package com.county.tcm.service;

import com.county.tcm.domain.*;
import com.county.tcm.repo.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/** 完整履约聚合:审方→抓药→代煎→配送→签收→回访 的同一条处方视图 */
@Service
public class FulfillmentService {

    private final PrescriptionRepository rxRepo;
    private final ReviewRecordRepository reviewRepo;
    private final DecoctTaskRepository decoctRepo;
    private final DeliveryTaskRepository deliveryRepo;
    private final FollowUpRepository followRepo;
    private final IssueRepository issueRepo;
    private final FulfillmentEventRepository eventRepo;
    private final InvoiceRepository invoiceRepo;
    private final MedicationReminderRepository reminderRepo;
    private final DomainSupport support;

    public FulfillmentService(PrescriptionRepository rxRepo, ReviewRecordRepository reviewRepo,
                              DecoctTaskRepository decoctRepo, DeliveryTaskRepository deliveryRepo,
                              FollowUpRepository followRepo, IssueRepository issueRepo,
                              FulfillmentEventRepository eventRepo, InvoiceRepository invoiceRepo,
                              MedicationReminderRepository reminderRepo, DomainSupport support) {
        this.rxRepo = rxRepo;
        this.reviewRepo = reviewRepo;
        this.decoctRepo = decoctRepo;
        this.deliveryRepo = deliveryRepo;
        this.followRepo = followRepo;
        this.issueRepo = issueRepo;
        this.eventRepo = eventRepo;
        this.invoiceRepo = invoiceRepo;
        this.reminderRepo = reminderRepo;
        this.support = support;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> detail(Long rxId) {
        Prescription rx = rxRepo.findById(rxId)
                .orElseThrow(() -> DomainSupport.badRequest("处方不存在: " + rxId));
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("prescription", support.rxView(rx));

        reviewRepo.findFirstByPrescriptionIdOrderByReviewedAtDesc(rxId).ifPresent(rv -> {
            Map<String, Object> r = new LinkedHashMap<>();
            r.put("pharmacist", rv.getPharmacist().getDisplayName());
            r.put("conclusion", rv.getConclusion().name());
            r.put("conclusionLabel", rv.getConclusion().getLabel());
            r.put("findings", rv.getFindings());
            r.put("substitutions", rv.getSubstitutions());
            r.put("insuranceNote", rv.getInsuranceNote());
            r.put("remark", rv.getRemark());
            r.put("reviewedAt", rv.getReviewedAt());
            m.put("review", r);
        });

        decoctRepo.findByPrescriptionId(rxId).ifPresent(t -> {
            Map<String, Object> d = new LinkedHashMap<>();
            d.put("potNo", t.getPotNo());
            d.put("soakMinutes", t.getSoakMinutes());
            d.put("boilTimes", t.getBoilTimes());
            d.put("bagCount", t.getBagCount());
            d.put("deliveryWave", t.getDeliveryWave());
            d.put("status", t.getStatus().name());
            d.put("statusLabel", t.getStatus().getLabel());
            d.put("decoctor", t.getDecoctor() == null ? null : t.getDecoctor().getDisplayName());
            d.put("scannedBagCode", t.getScannedBagCode());
            d.put("queuedAt", t.getQueuedAt());
            d.put("startedAt", t.getStartedAt());
            d.put("endedAt", t.getEndedAt());
            d.put("abnormalSmell", t.getAbnormalSmell());
            d.put("missingBags", t.getMissingBags());
            d.put("damagedBags", t.getDamagedBags());
            d.put("packer", t.getPacker());
            d.put("reviewer", t.getReviewer());
            d.put("remark", t.getRemark());
            m.put("decoct", d);
        });

        deliveryRepo.findByPrescriptionId(rxId).ifPresent(t -> {
            Map<String, Object> d = new LinkedHashMap<>();
            d.put("wave", t.getWave());
            d.put("courier", t.getCourier() == null ? null : t.getCourier().getDisplayName());
            d.put("courierPhone", t.getCourier() == null ? null : t.getCourier().getPhone());
            d.put("status", t.getStatus().name());
            d.put("statusLabel", t.getStatus().getLabel());
            d.put("address", t.getAddress());
            d.put("proxyName", t.getProxyName());
            d.put("assignedAt", t.getAssignedAt());
            d.put("outboundAt", t.getOutboundAt());
            d.put("promisedAt", t.getPromisedAt());
            d.put("signedAt", t.getSignedAt());
            d.put("signedBy", t.getSignedBy());
            d.put("timeoutMinutes", t.getTimeoutMinutes());
            m.put("delivery", d);
        });

        m.put("followUps", followRepo.findByPrescriptionIdOrderByFollowedAtAsc(rxId).stream().map(f -> {
            Map<String, Object> f0 = new LinkedHashMap<>();
            f0.put("result", f.getResult().name());
            f0.put("resultLabel", f.getResult().getLabel());
            f0.put("symptoms", f.getSymptoms());
            f0.put("advice", f.getAdvice());
            f0.put("satisfactionScore", f.getSatisfactionScore());
            f0.put("operator", f.getOperator().getDisplayName());
            f0.put("followedAt", f.getFollowedAt());
            return f0;
        }).toList());

        m.put("issues", issueRepo.findByPrescriptionIdOrderByReportedAtAsc(rxId).stream().map(i -> {
            Map<String, Object> i0 = new LinkedHashMap<>();
            i0.put("id", i.getId());
            i0.put("issueNo", i.getIssueNo());
            i0.put("type", i.getType().name());
            i0.put("typeLabel", i.getType().getLabel());
            i0.put("status", i.getStatus().name());
            i0.put("statusLabel", i.getStatus().getLabel());
            i0.put("ownerStage", i.getOwnerStage().name());
            i0.put("ownerStageLabel", i.getOwnerStage().getLabel());
            i0.put("description", i.getDescription());
            i0.put("locateInfo", i.getLocateInfo());
            i0.put("resolution", i.getResolution());
            i0.put("compensation", i.getCompensation());
            i0.put("reportedAt", i.getReportedAt());
            i0.put("resolvedAt", i.getResolvedAt());
            return i0;
        }).toList());

        m.put("events", eventRepo.findByPrescriptionIdOrderByOccurredAtAsc(rxId).stream().map(e -> {
            Map<String, Object> e0 = new LinkedHashMap<>();
            e0.put("stage", e.getStage().name());
            e0.put("stageLabel", e.getStage().getLabel());
            e0.put("action", e.getAction());
            e0.put("detail", e.getDetail());
            e0.put("operator", e.getOperatorName());
            e0.put("occurredAt", e.getOccurredAt());
            return e0;
        }).toList());

        m.put("invoices", invoiceRepo.findAll().stream()
                .filter(i -> i.getPrescription().getId().equals(rxId))
                .map(i -> Map.of("invoiceNo", i.getInvoiceNo(), "title", i.getTitle(),
                        "amount", i.getAmount(), "kind", i.getKind(), "issuedAt", i.getIssuedAt()))
                .toList());

        reminderRepo.findByPrescriptionId(rxId).ifPresent(r -> {
            Map<String, Object> r0 = new LinkedHashMap<>();
            r0.put("id", r.getId());
            r0.put("state", r.getState());
            r0.put("timesPerDay", r.getTimesPerDay());
            r0.put("nextRemindAt", r.getNextRemindAt());
            r0.put("note", r.getNote());
            m.put("reminder", r0);
        });

        return m;
    }
}
