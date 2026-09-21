package com.tcm.service;

import com.tcm.config.AuthUtils;
import com.tcm.model.*;
import com.tcm.repository.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class PrescriptionService {

    private final PrescriptionRepository prescriptions;
    private final PrescriptionItemRepository items;
    private final HerbRepository herbs;
    private final ClinicRepository clinics;
    private final DoctorRepository doctors;
    private final ReviewRepository reviews;
    private final DecoctTaskRepository decoctTasks;
    private final DecoctRecordRepository decoctRecords;
    private final DeliveryRepository deliveries;
    private final ExceptionEventRepository exceptions;
    private final FollowUpRepository followUps;
    private final InvoiceRepository invoices;
    private final ReminderRepository reminders;

    public PrescriptionService(PrescriptionRepository prescriptions, PrescriptionItemRepository items,
                               HerbRepository herbs, ClinicRepository clinics, DoctorRepository doctors,
                               ReviewRepository reviews, DecoctTaskRepository decoctTasks,
                               DecoctRecordRepository decoctRecords, DeliveryRepository deliveries,
                               ExceptionEventRepository exceptions, FollowUpRepository followUps,
                               InvoiceRepository invoices, ReminderRepository reminders) {
        this.prescriptions = prescriptions;
        this.items = items;
        this.herbs = herbs;
        this.clinics = clinics;
        this.doctors = doctors;
        this.reviews = reviews;
        this.decoctTasks = decoctTasks;
        this.decoctRecords = decoctRecords;
        this.deliveries = deliveries;
        this.exceptions = exceptions;
        this.followUps = followUps;
        this.invoices = invoices;
        this.reminders = reminders;
    }

    public record ItemReq(Long herbId, Integer dosageG, SpecialHandling specialHandling) {
    }

    public record CreateReq(String patientName, String patientPhone, Integer patientAge, String patientGender,
                            String contraindications, Long clinicId, Long doctorId, Integer doses,
                            String specialDecoction, Boolean addSugar, PickupMethod pickupMethod,
                            String deliveryAddress, String proxyName, String proxyPhone,
                            Boolean urgent, String batchNo, List<ItemReq> items) {
    }

    @Transactional
    public Prescription create(CreateReq req, User user) {
        AuthUtils.requireRole(user, Role.PATIENT, Role.CLINIC, Role.ADMIN);
        if (req.items() == null || req.items().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "处方至少包含一味药");
        }
        if (req.doses() == null || req.doses() < 1 || req.doses() > 30) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "剂数须在 1-30 之间");
        }
        PickupMethod pm = req.pickupMethod() != null ? req.pickupMethod() : PickupMethod.DELIVERY;
        if (pm == PickupMethod.DELIVERY && (req.deliveryAddress() == null || req.deliveryAddress().isBlank())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "配送到家须填写配送地址");
        }
        if (pm == PickupMethod.PROXY_PICKUP && (req.proxyName() == null || req.proxyName().isBlank())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "老人代取须填写代取人信息");
        }

        Prescription p = new Prescription();
        p.setRxNo(genRxNo());
        p.setPatientName(req.patientName());
        p.setPatientPhone(req.patientPhone());
        p.setPatientAge(req.patientAge());
        p.setPatientGender(req.patientGender());
        p.setContraindications(req.contraindications());
        if (user.getRole() == Role.CLINIC) {
            p.setClinic(user.getClinic());
        } else if (req.clinicId() != null) {
            p.setClinic(clinics.findById(req.clinicId()).orElse(null));
        }
        if (req.doctorId() != null) {
            p.setDoctor(doctors.findById(req.doctorId()).orElse(null));
        }
        p.setSubmittedBy(user);
        p.setDoses(req.doses());
        p.setSpecialDecoction(req.specialDecoction());
        p.setAddSugar(Boolean.TRUE.equals(req.addSugar()));
        p.setPickupMethod(pm);
        p.setDeliveryAddress(req.deliveryAddress());
        p.setProxyName(req.proxyName());
        p.setProxyPhone(req.proxyPhone());
        p.setUrgent(Boolean.TRUE.equals(req.urgent()));
        p.setBatchNo(req.batchNo());
        p.setStatus(PrescriptionStatus.PENDING_REVIEW);
        prescriptions.save(p);

        for (ItemReq ir : req.items()) {
            if (ir.herbId() == null || ir.dosageG() == null || ir.dosageG() < 1) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "药味与剂量填写不完整");
            }
            Herb herb = herbs.findById(ir.herbId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "药材不存在: " + ir.herbId()));
            PrescriptionItem item = new PrescriptionItem();
            item.setPrescription(p);
            item.setHerb(herb);
            item.setDosageG(ir.dosageG());
            item.setSpecialHandling(ir.specialHandling() != null ? ir.specialHandling() : SpecialHandling.NONE);
            item.setSubstituted(false);
            item.setUnitPrice(herb.getUnitPrice());
            items.save(item);
        }
        return p;
    }

    private String genRxNo() {
        String prefix = "RX" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyMMdd"));
        long seq = prescriptions.countByRxNoStartingWith(prefix) + 1;
        return prefix + "-" + String.format("%04d", seq);
    }

    public List<Prescription> list(User user, PrescriptionStatus status, String batchNo) {
        if (batchNo != null && !batchNo.isBlank()) {
            return prescriptions.findByBatchNoOrderByCreatedAtAsc(batchNo);
        }
        if (user.getRole() == Role.PATIENT) {
            return prescriptions.findBySubmittedByIdOrderByCreatedAtDesc(user.getId());
        }
        if (user.getRole() == Role.CLINIC) {
            return prescriptions.findByClinicIdOrderByCreatedAtDesc(user.getClinic().getId());
        }
        if (status != null) {
            return prescriptions.findByStatusOrderByCreatedAtDesc(status);
        }
        return prescriptions.findAllByOrderByCreatedAtDesc();
    }

    public Prescription get(Long id) {
        return prescriptions.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "处方不存在"));
    }

    /** 加载并校验数据权限：患者仅见本人提交，诊所仅见本诊所 */
    public Prescription getScoped(Long id, User user) {
        Prescription p = get(id);
        if (user.getRole() == Role.PATIENT && !p.getSubmittedBy().getId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "无权查看该处方");
        }
        if (user.getRole() == Role.CLINIC
                && (p.getClinic() == null || !p.getClinic().getId().equals(user.getClinic().getId()))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "无权查看该处方");
        }
        return p;
    }

    /** 完整履约记录：处方 + 药味 + 审方 + 煎药 + 配送 + 异常 + 回访 + 发票 + 提醒 */
    public Map<String, Object> detail(Long id, User user) {
        Prescription p = getScoped(id, user);
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("prescription", p);
        map.put("items", items.findByPrescriptionId(id));
        map.put("review", reviews.findByPrescriptionId(id).orElse(null));
        DecoctTask task = decoctTasks.findByPrescriptionId(id).orElse(null);
        map.put("decoctTask", task);
        map.put("decoctRecords", task != null ? decoctRecords.findByTaskId(task.getId()) : List.of());
        map.put("delivery", deliveries.findByPrescriptionId(id).orElse(null));
        map.put("exceptions", exceptions.findByPrescriptionIdOrderByCreatedAtDesc(id));
        map.put("followUps", followUps.findByPrescriptionIdOrderByCreatedAtDesc(id));
        map.put("invoice", invoices.findByPrescriptionId(id).orElse(null));
        map.put("reminders", reminders.findByPrescriptionIdOrderByRemindAtAsc(id));
        return map;
    }

    /** 抓药完成 → 生成煎药任务(锅号/浸泡/煎煮次数/袋数/波次)，进入代煎排队 */
    @Transactional
    public DecoctTask dispense(Long id, User user) {
        AuthUtils.requireRole(user, Role.PHARMACIST, Role.ADMIN);
        Prescription p = get(id);
        if (p.getStatus() != PrescriptionStatus.DISPENSING) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "当前状态不可抓药(须审方通过)");
        }
        p.setStatus(PrescriptionStatus.DECOCT_QUEUED);

        DecoctTask t = new DecoctTask();
        t.setPrescription(p);
        t.setPotNo("G" + String.format("%02d", (decoctTasks.count() % 8) + 1));
        t.setSoakMinutes(30);
        t.setDecoctTimes(2);
        t.setBagCount(p.getDoses() * 2);
        t.setWaveNo(computeWave(p));
        t.setQueuePosition((int) decoctTasks.countByStatus(DecoctStatus.QUEUED) + 1);
        t.setUrgent(Boolean.TRUE.equals(p.getUrgent()));
        decoctTasks.save(t);
        prescriptions.save(p);
        return t;
    }

    private String computeWave(Prescription p) {
        if (Boolean.TRUE.equals(p.getUrgent())) {
            return "W0-夜间急煎";
        }
        int hour = LocalDateTime.now().getHour();
        if (hour < 11) {
            return "W1-上午波次";
        }
        if (hour < 16) {
            return "W2-下午波次";
        }
        return "W3-晚间波次";
    }
}
