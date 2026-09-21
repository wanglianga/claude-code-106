package com.county.tcm.service;

import com.county.tcm.domain.*;
import com.county.tcm.repo.*;
import com.county.tcm.web.dto.Dtos;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class PrescriptionService {

    private final PrescriptionRepository rxRepo;
    private final HerbRepository herbRepo;
    private final DoctorRepository doctorRepo;
    private final CompatibilityRuleRepository ruleRepo;
    private final ReviewRecordRepository reviewRepo;
    private final DispenseRecordRepository dispenseRepo;
    private final DecoctTaskRepository decoctRepo;
    private final DomainSupport support;

    public PrescriptionService(PrescriptionRepository rxRepo, HerbRepository herbRepo,
                               DoctorRepository doctorRepo, CompatibilityRuleRepository ruleRepo,
                               ReviewRecordRepository reviewRepo, DispenseRecordRepository dispenseRepo,
                               DecoctTaskRepository decoctRepo, DomainSupport support) {
        this.rxRepo = rxRepo;
        this.herbRepo = herbRepo;
        this.doctorRepo = doctorRepo;
        this.ruleRepo = ruleRepo;
        this.reviewRepo = reviewRepo;
        this.dispenseRepo = dispenseRepo;
        this.decoctRepo = decoctRepo;
        this.support = support;
    }

    // ---------------- 提交处方 ----------------

    @Transactional
    public Map<String, Object> create(Dtos.RxCreateRequest req, UserAccount submitter) {
        Prescription rx = new Prescription();
        rx.setRxNo(support.nextRxNo());
        rx.setSubmitter(submitter);
        rx.setSubmittedAt(LocalDateTime.now());
        fillRx(rx, req, submitter);
        rx = rxRepo.save(rx);
        support.event(rx, submitter.getRole() == Role.CLINIC ? Stage.CLINIC : Stage.PATIENT,
                "提交处方", "处方提交,共 " + rx.getDoses() + " 剂,取药方式:" + rx.getPickupMethod().getLabel()
                        + (rx.isNightUrgent() ? ",夜间急煎" : ""), submitter.getDisplayName());
        return support.rxView(rx);
    }

    @Transactional
    public List<Map<String, Object>> createBatch(Dtos.BatchCreateRequest req, UserAccount clinic) {
        if (clinic.getRole() != Role.CLINIC) {
            throw DomainSupport.badRequest("仅诊所账号可提交批量处方");
        }
        String batchNo = "BATCH-" + LocalDateTime.now().toString().substring(0, 10).replace("-", "")
                + "-" + (rxRepo.count() + 1);
        List<Map<String, Object>> saved = new ArrayList<>();
        for (Dtos.RxCreateRequest one : req.prescriptions()) {
            Prescription rx = new Prescription();
            rx.setRxNo(support.nextRxNo());
            rx.setSubmitter(clinic);
            rx.setSubmittedAt(LocalDateTime.now());
            rx.setBatchNo(batchNo);
            fillRx(rx, one, clinic);
            rx = rxRepo.save(rx);
            support.event(rx, Stage.CLINIC, "批量处方提交", "批次 " + batchNo, clinic.getDisplayName());
            saved.add(support.rxView(rx));
        }
        return saved;
    }

    private void fillRx(Prescription rx, Dtos.RxCreateRequest req, UserAccount submitter) {
        rx.setPatientName(req.patientName());
        rx.setPatientPhone(req.patientPhone());
        rx.setDoses(req.doses());
        rx.setSpecialDecoctionNote(req.specialDecoctionNote());
        rx.setAddSugar(req.addSugar());
        rx.setPickupMethod(PickupMethod.valueOf(req.pickupMethod()));
        rx.setAddress(req.address());
        rx.setContactName(req.contactName() != null ? req.contactName() : req.patientName());
        rx.setContactPhone(req.contactPhone() != null ? req.contactPhone() : req.patientPhone());
        rx.setContraindications(req.contraindications());
        rx.setSettlementType(SettlementType.valueOf(req.settlementType()));
        rx.setNightUrgent(req.nightUrgent());
        if (req.batchNo() != null) rx.setBatchNo(req.batchNo());

        if (submitter.getRole() == Role.PATIENT) {
            rx.setPatientUser(submitter);
            rx.setPatientName(submitter.getDisplayName());
        }
        if (submitter.getRole() == Role.CLINIC) {
            rx.setClinicName(submitter.getOrganization());
        } else if (req.clinicName() != null) {
            rx.setClinicName(req.clinicName());
        }

        if (req.doctorId() != null) {
            Doctor doctor = doctorRepo.findById(req.doctorId())
                    .orElseThrow(() -> DomainSupport.badRequest("医生不存在: " + req.doctorId()));
            rx.setDoctor(doctor);
            rx.setDoctorName(doctor.getName());
        } else {
            rx.setDoctorName(req.doctorName());
        }

        Map<String, Herb> catalog = support.catalog();
        for (Dtos.HerbItemDto h : req.herbs()) {
            PrescriptionHerb ph = new PrescriptionHerb();
            ph.setHerbName(h.name());
            ph.setHerb(catalog.get(h.name()));
            ph.setDosePerPacket(h.dosePerPacket());
            if (h.specialMethod() != null && !h.specialMethod().isBlank()) {
                ph.setSpecialMethod(SpecialMethod.valueOf(h.specialMethod()));
            }
            ph.setNote(h.note());
            rx.addHerb(ph);
        }
    }

    // ---------------- 列表与权限 ----------------

    @Transactional(readOnly = true)
    public List<Map<String, Object>> listViewsFor(UserAccount user) {
        return listFor(user).stream().map(support::rxView).toList();
    }

    @Transactional(readOnly = true)
    public List<Prescription> listFor(UserAccount user) {
        return switch (user.getRole()) {
            case PATIENT -> rxRepo.findAllByOrderBySubmittedAtDesc().stream()
                    .filter(rx -> rx.getPatientUser() != null && rx.getPatientUser().getId().equals(user.getId()))
                    .toList();
            case CLINIC -> rxRepo.findAllByOrderBySubmittedAtDesc().stream()
                    .filter(rx -> user.getOrganization() != null
                            && user.getOrganization().equals(rx.getClinicName()))
                    .toList();
            default -> rxRepo.findAllByOrderBySubmittedAtDesc();
        };
    }

    public Prescription getRx(Long id) {
        return support.requireRx(id);
    }

    public void assertCanView(UserAccount user, Prescription rx) {
        if (user.getRole() == Role.PATIENT
                && (rx.getPatientUser() == null || !rx.getPatientUser().getId().equals(user.getId()))) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.FORBIDDEN, "无权查看该处方");
        }
        if (user.getRole() == Role.CLINIC
                && !Objects.equals(user.getOrganization(), rx.getClinicName())) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.FORBIDDEN, "无权查看该处方");
        }
    }

    // ---------------- 审方预览(规则引擎实时结果) ----------------

    @Transactional(readOnly = true)
    public Map<String, Object> previewReview(Long rxId) {
        Prescription rx = support.requireRx(rxId);
        var check = ReviewEngine.check(rx, ruleRepo.findAll(), support.catalog());
        return Map.of(
                "findings", check.findings(),
                "blockingFindings", check.blockingFindings(),
                "shortageSuggest", check.shortageSuggest(),
                "insuranceWarnings", check.insuranceWarnings(),
                "blocked", check.blocked());
    }

    // ---------------- 药师审方 ----------------

    @Transactional
    public ReviewRecord review(Long rxId, Dtos.ReviewSubmitRequest req, UserAccount pharmacist) {
        Prescription rx = support.requireRx(rxId);
        if (rx.getStatus() != RxStatus.PENDING_REVIEW) {
            throw DomainSupport.conflict("处方当前状态[" + rx.getStatus().getLabel() + "]不可审方");
        }
        ReviewConclusion conclusion = ReviewConclusion.valueOf(req.conclusion());
        var check = ReviewEngine.check(rx, ruleRepo.findAll(), support.catalog());

        ReviewRecord record = new ReviewRecord();
        record.setPrescription(rx);
        record.setPharmacist(pharmacist);
        record.setReviewedAt(LocalDateTime.now());
        record.setFindings(new ArrayList<>(check.findings()));
        record.setInsuranceNote(String.join(";", check.insuranceWarnings())
                + (req.insuranceNote() == null ? "" : ";" + req.insuranceNote()));
        record.setRemark(req.remark());

        if (conclusion == ReviewConclusion.REJECT) {
            record.setConclusion(ReviewConclusion.REJECT);
            reviewRepo.save(record);
            rx.setStatus(RxStatus.REJECTED);
            rx.setFinishedAt(LocalDateTime.now());
            support.event(rx, Stage.PHARMACIST, "审方驳回",
                    String.join(" | ", check.findings()) + "|" + nz(req.remark()), pharmacist.getDisplayName());
            return record;
        }

        // 通过路径:硬性禁忌/剂量阻断
        if (check.blocked()) {
            throw DomainSupport.conflict("存在阻断性问题,不能通过审方:" + String.join(";", check.blockingFindings()));
        }

        // 缺药替代
        Map<String, Herb> catalog = support.catalog();
        List<String> subLogs = new ArrayList<>();
        if (!check.shortageSuggest().isEmpty()) {
            if (!req.applySuggestions()) {
                throw DomainSupport.conflict("存在缺药且未确认替代方案,请勾选应用缺药替代或驳回处方");
            }
            for (PrescriptionHerb ph : rx.getHerbs()) {
                String target = check.shortageSuggest().get(ph.getHerbName());
                if (target != null) {
                    Herb sub = catalog.get(target);
                    subLogs.add(ph.getHerbName() + "→" + target);
                    ph.setSubstitutedFrom(ph.getHerbName());
                    ph.setHerbName(target);
                    ph.setHerb(sub);
                }
            }
            conclusion = ReviewConclusion.PASS_WITH_SUBSTITUTE;
        }
        record.setSubstitutions(String.join(";", subLogs));
        record.setConclusion(conclusion);

        // 费用结算
        var fees = ReviewEngine.fees(rx, catalog,
                new BigDecimal("3.00"), new BigDecimal("6.00"), new BigDecimal("20.00"));
        rx.setHerbAmount(fees.herbAmount());
        rx.setDecoctFee(fees.decoctFee());
        rx.setDeliveryFee(fees.deliveryFee());
        rx.setNightSurcharge(fees.nightSurcharge());
        rx.setTotalAmount(fees.total());
        rx.setInsurancePaid(fees.insurancePaid());
        rx.setSelfPaid(fees.selfPaid());
        rx.setSettled(true);

        reviewRepo.save(record);
        rx.setStatus(RxStatus.APPROVED);
        support.event(rx, Stage.PHARMACIST, "药师审方通过",
                "结论:" + conclusion.getLabel()
                        + (subLogs.isEmpty() ? "" : ",替代:" + String.join(",", subLogs))
                        + ",总额 " + fees.total() + " 元(医保 " + fees.insurancePaid()
                        + "/自付 " + fees.selfPaid() + ")",
                pharmacist.getDisplayName());
        support.event(rx, Stage.FINANCE, "收费结算",
                rx.getSettlementType().getLabel() + " 总额 " + fees.total()
                        + " 元,医保 " + fees.insurancePaid() + ",自付 " + fees.selfPaid(),
                pharmacist.getDisplayName());
        return record;
    }

    // ---------------- 抓药(扣库存) ----------------

    @Transactional
    public DispenseRecord dispense(Long rxId, Dtos.DispenseRequest req, UserAccount pharmacist) {
        Prescription rx = support.requireRx(rxId);
        if (rx.getStatus() != RxStatus.APPROVED) {
            throw DomainSupport.conflict("仅[审方通过待抓药]状态可抓药");
        }
        Map<String, Herb> catalog = support.catalog();
        for (PrescriptionHerb ph : rx.getHerbs()) {
            Herb herb = catalog.get(ph.getHerbName());
            if (herb == null) {
                throw DomainSupport.conflict("药材 " + ph.getHerbName() + " 不在目录,无法抓药");
            }
            BigDecimal need = ph.getDosePerPacket().multiply(BigDecimal.valueOf(rx.getDoses()));
            if (herb.getStock().compareTo(need) < 0) {
                throw DomainSupport.conflict("药材 " + ph.getHerbName() + " 库存不足,需 " + need + " 存 " + herb.getStock());
            }
        }
        for (PrescriptionHerb ph : rx.getHerbs()) {
            Herb herb = catalog.get(ph.getHerbName());
            BigDecimal need = ph.getDosePerPacket().multiply(BigDecimal.valueOf(rx.getDoses()));
            herb.setStock(herb.getStock().subtract(need));
        }

        DispenseRecord dr = new DispenseRecord();
        dr.setPrescription(rx);
        dr.setPharmacist(pharmacist);
        dr.setHerbCount(rx.getHerbs().size());
        dr.setNote(req == null ? null : req.note());
        dr.setDispensedAt(LocalDateTime.now());
        dispenseRepo.save(dr);

        if (rx.getPickupMethod() == PickupMethod.SELF_PICKUP) {
            rx.setStatus(RxStatus.READY_PICKUP);
            support.event(rx, Stage.PHARMACIST, "抓药完成",
                    "已抓 " + rx.getHerbs().size() + " 味,等待患者到店自取", pharmacist.getDisplayName());
        } else {
            DecoctTask task = new DecoctTask();
            task.setPrescription(rx);
            task.setQueuedAt(LocalDateTime.now());
            task.setStatus(DecoctStatus.QUEUED);
            decoctRepo.save(task);
            rx.setStatus(RxStatus.DISPENSED);
            support.event(rx, Stage.PHARMACIST, "抓药完成",
                    "已抓 " + rx.getHerbs().size() + " 味并移交煎药房代煎", pharmacist.getDisplayName());
        }
        return dr;
    }

    private static String nz(String s) { return s == null ? "" : s; }
}
