package com.county.tcm.service;

import com.county.tcm.domain.*;
import com.county.tcm.repo.*;
import com.county.tcm.web.dto.Dtos;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class DeliveryService {

    private final DeliveryTaskRepository deliveryRepo;
    private final IssueRepository issueRepo;
    private final MedicationReminderRepository reminderRepo;
    private final UserRepository userRepo;
    private final DecoctTaskRepository decoctRepo;
    private final DomainSupport support;

    public DeliveryService(DeliveryTaskRepository deliveryRepo, IssueRepository issueRepo,
                           MedicationReminderRepository reminderRepo, UserRepository userRepo,
                           DecoctTaskRepository decoctRepo, DomainSupport support) {
        this.deliveryRepo = deliveryRepo;
        this.issueRepo = issueRepo;
        this.reminderRepo = reminderRepo;
        this.userRepo = userRepo;
        this.decoctRepo = decoctRepo;
        this.support = support;
    }

    @Transactional(readOnly = true)
    public List<java.util.Map<String, Object>> listViews() {
        return list().stream().map(support::deliveryView).toList();
    }

    @Transactional(readOnly = true)
    public List<DeliveryTask> list() {
        return deliveryRepo.findAll().stream()
                .sorted((a, b) -> b.getAssignedAt().compareTo(a.getAssignedAt()))
                .toList();
    }

    /** 调度派单:指定配送员、波次、承诺送达时长(分钟) */
    @Transactional
    public DeliveryTask assign(Long rxId, Dtos.DeliveryAssignRequest req, UserAccount operator) {
        Prescription rx = support.requireRx(rxId);
        if (rx.getStatus() != RxStatus.DECOCTED) {
            throw DomainSupport.conflict("仅[煎煮完成待配送]状态可派单");
        }
        if (rx.getPickupMethod() == PickupMethod.SELF_PICKUP) {
            throw DomainSupport.conflict("自取处方无需配送");
        }
        UserAccount courier = null;
        if (req.courierId() != null) {
            courier = userRepo.findById(req.courierId())
                    .filter(u -> u.getRole() == Role.COURIER)
                    .orElseThrow(() -> DomainSupport.badRequest("配送员不存在: " + req.courierId()));
        }

        DeliveryTask task = deliveryRepo.findByPrescriptionId(rxId).orElseGet(DeliveryTask::new);
        task.setPrescription(rx);
        task.setCourier(courier);
        task.setWave(req.wave() == null ? "WAVE-DEFAULT" : req.wave());
        task.setAddress(rx.getAddress());
        task.setStatus(DeliveryStatus.ASSIGNED);
        task.setAssignedAt(LocalDateTime.now());
        int minutes = req.promisedMinutes() == null ? 60 : req.promisedMinutes();
        task.setPromisedAt(LocalDateTime.now().plusMinutes(minutes));
        deliveryRepo.save(task);
        support.event(rx, Stage.DELIVERY, "配送派单",
                "波次 " + task.getWave() + ",承诺 " + minutes + " 分钟内送达,地址:" + task.getAddress()
                        + (rx.getPickupMethod() == PickupMethod.ELDER_PROXY ? ",老人代取" : ""),
                operator.getDisplayName());
        return task;
    }

    /** 派单后指定/更换配送员 */
    @Transactional
    public DeliveryTask assignCourier(Long rxId, UserAccount courier, UserAccount operator) {
        DeliveryTask task = deliveryRepo.findByPrescriptionId(rxId)
                .orElseThrow(() -> DomainSupport.conflict("该处方尚无配送任务"));
        task.setCourier(courier);
        deliveryRepo.save(task);
        support.event(task.getPrescription(), Stage.DELIVERY, "指定配送员",
                courier.getDisplayName() + "(" + courier.getPhone() + ")", operator.getDisplayName());
        return task;
    }

    @Transactional
    public DeliveryTask outbound(Long rxId, UserAccount courier) {
        DeliveryTask task = mustTask(rxId);
        if (task.getStatus() != DeliveryStatus.ASSIGNED) {
            throw DomainSupport.conflict("仅已接单任务可出库配送");
        }
        task.setStatus(DeliveryStatus.DELIVERING);
        task.setOutboundAt(LocalDateTime.now());
        if (task.getCourier() == null) task.setCourier(courier);
        deliveryRepo.save(task);
        task.getPrescription().setStatus(RxStatus.DELIVERING);
        support.event(task.getPrescription(), Stage.DELIVERY, "出库配送",
                "配送员 " + task.getCourier().getDisplayName() + " 携药出库", courier.getDisplayName());
        return task;
    }

    /** 患者签收(含老人代取签收);超时自动开单;签收后自动建立后续用药提醒 */
    @Transactional
    public DeliveryTask sign(Long rxId, Dtos.SignRequest req, UserAccount operator) {
        Prescription rx = support.requireRx(rxId);
        DeliveryTask task = mustTask(rxId);
        if (task.getStatus() == DeliveryStatus.SIGNED) {
            throw DomainSupport.conflict("该处方已签收");
        }
        LocalDateTime now = LocalDateTime.now();
        task.setStatus(DeliveryStatus.SIGNED);
        task.setSignedAt(now);
        task.setSignedBy(req.signedBy() == null || req.signedBy().isBlank() ? rx.getPatientName() : req.signedBy());
        if (req.address() != null && !req.address().isBlank()) task.setAddress(req.address());
        if (rx.getPickupMethod() == PickupMethod.ELDER_PROXY && req.proxyName() != null) {
            task.setProxyName(req.proxyName());
        }
        if (task.getPromisedAt() != null && now.isAfter(task.getPromisedAt())) {
            int late = (int) java.time.Duration.between(task.getPromisedAt(), now).toMinutes();
            task.setTimeoutMinutes(late);
            openTimeoutIssue(rx, task, late, operator);
        }
        deliveryRepo.save(task);

        rx.setStatus(RxStatus.SIGNED);
        rx.setFinishedAt(now);
        support.event(rx, Stage.DELIVERY, "患者签收",
                "签收人 " + task.getSignedBy()
                        + (task.getProxyName() != null ? "(老人代取:" + task.getProxyName() + ")" : "")
                        + (task.getTimeoutMinutes() != null ? ",配送超时 " + task.getTimeoutMinutes() + " 分钟" : ""),
                operator.getDisplayName());

        if (reminderRepo.findByPrescriptionId(rxId).isEmpty()) {
            MedicationReminder reminder = new MedicationReminder();
            reminder.setPrescription(rx);
            reminder.setPatientName(rx.getPatientName());
            reminder.setPatientPhone(rx.getPatientPhone());
            reminder.setTimesPerDay(2);
            reminder.setNextRemindAt(now.plusHours(12));
            reminder.setState("ACTIVE");
            reminder.setCreatedAt(now);
            reminder.setNote("早晚温服,每剂 2 袋" + (rx.isAddSugar() ? ",可加糖" : ""));
            reminderRepo.save(reminder);
            support.event(rx, Stage.PATIENT, "建立用药提醒",
                    "每日 2 次,首次提醒 " + reminder.getNextRemindAt(), operator.getDisplayName());
        }
        return task;
    }

    /** 患者临时改地址:在同一条履约记录上挂 ADDRESS_CHANGE 工单并同步配送地址 */
    @Transactional
    public java.util.Map<String, Object> changeAddress(Long rxId, Dtos.AddressChangeRequest req, UserAccount reporter) {
        Prescription rx = support.requireRx(rxId);
        if (deliveryRepo.findByPrescriptionId(rxId).map(t -> t.getStatus() == DeliveryStatus.SIGNED).orElse(false)) {
            throw DomainSupport.conflict("已签收处方不可改地址");
        }
        String old = rx.getAddress();
        rx.setAddress(req.newAddress());
        deliveryRepo.findByPrescriptionId(rxId).ifPresent(t -> t.setAddress(req.newAddress()));

        Issue issue = new Issue();
        issue.setIssueNo(support.nextIssueNo(issueRepo.count()));
        issue.setPrescription(rx);
        issue.setType(IssueType.ADDRESS_CHANGE);
        issue.setOwnerStage(Stage.DELIVERY);
        issue.setDescription("患者临时改地址:[" + old + "] → [" + req.newAddress() + "]");
        String courier = deliveryRepo.findByPrescriptionId(rxId)
                .map(t -> t.getCourier() == null ? "待派单" : t.getCourier().getDisplayName())
                .orElse("尚未派单");
        issue.setLocateInfo("配送员:" + courier + ",波次:"
                + deliveryRepo.findByPrescriptionId(rxId).map(DeliveryTask::getWave).orElse("无"));
        issue.setReporter(reporter);
        issue.setReportedAt(LocalDateTime.now());
        issueRepo.save(issue);
        support.event(rx, reporter.getRole() == Role.PATIENT ? Stage.PATIENT : Stage.CLINIC,
                "申请改地址", issue.getDescription() + ";" + issue.getLocateInfo(), reporter.getDisplayName());
        return support.issueView(issue);
    }

    /** 定时扫描:超过承诺时间仍在途的配送单,自动登记配送超时工单(幂等) */
    @Scheduled(fixedRate = 60_000)
    @Transactional
    public void scanOverdue() {
        LocalDateTime now = LocalDateTime.now();
        for (DeliveryTask task : deliveryRepo.findAll()) {
            if (task.getStatus() == DeliveryStatus.SIGNED || task.getPromisedAt() == null) continue;
            if (!now.isAfter(task.getPromisedAt())) continue;
            Prescription rx = task.getPrescription();
            boolean exists = issueRepo.findByPrescriptionIdOrderByReportedAtAsc(rx.getId()).stream()
                    .anyMatch(i -> i.getType() == IssueType.DELIVERY_TIMEOUT && i.getStatus() != IssueStatus.RESOLVED);
            if (!exists) {
                int late = (int) java.time.Duration.between(task.getPromisedAt(), now).toMinutes();
                openTimeoutIssue(rx, task, late, task.getCourier());
            }
        }
    }

    private void openTimeoutIssue(Prescription rx, DeliveryTask task, int lateMinutes, UserAccount reporter) {
        Issue issue = new Issue();
        issue.setIssueNo(support.nextIssueNo(issueRepo.count()));
        issue.setPrescription(rx);
        issue.setType(IssueType.DELIVERY_TIMEOUT);
        issue.setOwnerStage(Stage.DELIVERY);
        issue.setDescription("配送超时 " + lateMinutes + " 分钟(承诺 " + task.getPromisedAt() + " 前送达)");
        issue.setLocateInfo("波次:" + task.getWave() + ",配送员:"
                + (task.getCourier() == null ? "未指派" : task.getCourier().getDisplayName())
                + ",锅号:" + decoctRepo.findByPrescriptionId(rx.getId())
                        .map(DecoctTask::getPotNo).orElse("无"));
        issue.setReporter(reporter);
        issue.setReportedAt(LocalDateTime.now());
        issueRepo.save(issue);
        support.event(rx, Stage.DELIVERY, "登记配送超时",
                issue.getIssueNo() + " 超时 " + lateMinutes + " 分钟",
                reporter == null ? "系统" : reporter.getDisplayName());
    }

    private DeliveryTask mustTask(Long rxId) {
        return deliveryRepo.findByPrescriptionId(rxId)
                .orElseThrow(() -> DomainSupport.conflict("该处方尚无配送任务"));
    }
}
