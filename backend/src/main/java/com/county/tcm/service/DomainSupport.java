package com.county.tcm.service;

import com.county.tcm.domain.*;
import com.county.tcm.repo.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** 领域通用小工具:单号生成、事件留痕、目录快照、异常抛出、视图装配 */
@Component
public class DomainSupport {

    private static final DateTimeFormatter DAY_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final PrescriptionRepository rxRepo;
    private final FulfillmentEventRepository eventRepo;
    private final HerbRepository herbRepo;

    public DomainSupport(PrescriptionRepository rxRepo, FulfillmentEventRepository eventRepo, HerbRepository herbRepo) {
        this.rxRepo = rxRepo;
        this.eventRepo = eventRepo;
        this.herbRepo = herbRepo;
    }

    public synchronized String nextRxNo() {
        return "RX" + LocalDateTime.now().format(DAY_FMT) + "-"
                + String.format("%04d", rxRepo.count() + 1);
    }

    public synchronized String nextIssueNo(long count) {
        return "IS" + LocalDateTime.now().format(DAY_FMT) + "-" + String.format("%04d", count + 1);
    }

    public synchronized String nextInvoiceNo(long count) {
        return "INV" + LocalDateTime.now().format(DAY_FMT) + "-" + String.format("%04d", count + 1);
    }

    public FulfillmentEvent event(Prescription rx, Stage stage, String action, String detail, String operator) {
        return eventRepo.save(new FulfillmentEvent(rx, stage, action, detail, operator));
    }

    public Map<String, Herb> catalog() {
        return herbRepo.findAll().stream()
                .collect(Collectors.toMap(Herb::getName, Function.identity(), (a, b) -> a, LinkedHashMap::new));
    }

    public Prescription requireRx(Long id) {
        return rxRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "处方不存在: " + id));
    }

    public static ResponseStatusException conflict(String msg) {
        return new ResponseStatusException(HttpStatus.CONFLICT, msg);
    }

    public static ResponseStatusException badRequest(String msg) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, msg);
    }

    public static Map<String, Object> userView(UserAccount u) {
        if (u == null) return null;
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", u.getId());
        m.put("username", u.getUsername());
        m.put("displayName", u.getDisplayName());
        m.put("role", u.getRole().name());
        m.put("roleLabel", u.getRole().getLabel());
        m.put("organization", u.getOrganization());
        m.put("phone", u.getPhone());
        return m;
    }

    public Map<String, Object> rxView(Prescription rx) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", rx.getId());
        m.put("rxNo", rx.getRxNo());
        m.put("patientName", rx.getPatientName());
        m.put("patientPhone", rx.getPatientPhone());
        m.put("patientUser", userView(rx.getPatientUser()));
        m.put("submitter", userView(rx.getSubmitter()));
        m.put("clinicName", rx.getClinicName());
        m.put("doctorId", rx.getDoctor() == null ? null : rx.getDoctor().getId());
        m.put("doctorName", rx.getDoctorName());
        m.put("doses", rx.getDoses());
        m.put("specialDecoctionNote", rx.getSpecialDecoctionNote());
        m.put("addSugar", rx.isAddSugar());
        m.put("pickupMethod", rx.getPickupMethod().name());
        m.put("pickupMethodLabel", rx.getPickupMethod().getLabel());
        m.put("address", rx.getAddress());
        m.put("contactName", rx.getContactName());
        m.put("contactPhone", rx.getContactPhone());
        m.put("contraindications", rx.getContraindications());
        m.put("settlementType", rx.getSettlementType().name());
        m.put("settlementLabel", rx.getSettlementType().getLabel());
        m.put("nightUrgent", rx.isNightUrgent());
        m.put("batchNo", rx.getBatchNo());
        m.put("status", rx.getStatus().name());
        m.put("statusLabel", rx.getStatus().getLabel());
        m.put("submittedAt", rx.getSubmittedAt());
        m.put("finishedAt", rx.getFinishedAt());

        List<Map<String, Object>> herbs = rx.getHerbs().stream().map(ph -> {
            Map<String, Object> h = new LinkedHashMap<>();
            h.put("id", ph.getId());
            h.put("herbName", ph.getHerbName());
            h.put("dosePerPacket", ph.getDosePerPacket());
            h.put("totalQuantity", ph.getDosePerPacket().multiply(java.math.BigDecimal.valueOf(rx.getDoses())));
            h.put("specialMethod", ph.getSpecialMethod().name());
            h.put("specialLabel", ph.getSpecialMethod().getLabel());
            h.put("note", ph.getNote());
            h.put("substitutedFrom", ph.getSubstitutedFrom());
            h.put("unitPrice", ph.getHerb() == null ? null : ph.getHerb().getUnitPrice());
            h.put("insuranceCovered", ph.getHerb() == null ? null : ph.getHerb().isInsuranceCovered());
            return h;
        }).toList();
        m.put("herbs", herbs);

        Map<String, Object> fees = new LinkedHashMap<>();
        fees.put("herbAmount", rx.getHerbAmount());
        fees.put("decoctFee", rx.getDecoctFee());
        fees.put("deliveryFee", rx.getDeliveryFee());
        fees.put("nightSurcharge", rx.getNightSurcharge());
        fees.put("totalAmount", rx.getTotalAmount());
        fees.put("insurancePaid", rx.getInsurancePaid());
        fees.put("selfPaid", rx.getSelfPaid());
        fees.put("settled", rx.isSettled());
        m.put("fees", fees);
        return m;
    }

    public Map<String, Object> decoctView(DecoctTask t) {
        Map<String, Object> x = new LinkedHashMap<>();
        x.put("rxId", t.getPrescription().getId());
        x.put("rxNo", t.getPrescription().getRxNo());
        x.put("patientName", t.getPrescription().getPatientName());
        x.put("doses", t.getPrescription().getDoses());
        x.put("nightUrgent", t.getPrescription().isNightUrgent());
        x.put("potNo", t.getPotNo());
        x.put("soakMinutes", t.getSoakMinutes());
        x.put("boilTimes", t.getBoilTimes());
        x.put("bagCount", t.getBagCount());
        x.put("deliveryWave", t.getDeliveryWave());
        x.put("status", t.getStatus().name());
        x.put("statusLabel", t.getStatus().getLabel());
        x.put("decoctor", t.getDecoctor() == null ? null : t.getDecoctor().getDisplayName());
        x.put("scannedBagCode", t.getScannedBagCode());
        x.put("startedAt", t.getStartedAt());
        x.put("endedAt", t.getEndedAt());
        x.put("missingBags", t.getMissingBags());
        x.put("damagedBags", t.getDamagedBags());
        x.put("packer", t.getPacker());
        x.put("reviewer", t.getReviewer());
        return x;
    }

    public Map<String, Object> deliveryView(DeliveryTask t) {
        Map<String, Object> x = new LinkedHashMap<>();
        x.put("rxId", t.getPrescription().getId());
        x.put("rxNo", t.getPrescription().getRxNo());
        x.put("patientName", t.getPrescription().getPatientName());
        x.put("pickupMethod", t.getPrescription().getPickupMethod().name());
        x.put("wave", t.getWave());
        x.put("courier", t.getCourier() == null ? null : t.getCourier().getDisplayName());
        x.put("status", t.getStatus().name());
        x.put("statusLabel", t.getStatus().getLabel());
        x.put("address", t.getAddress());
        x.put("proxyName", t.getProxyName());
        x.put("promisedAt", t.getPromisedAt());
        x.put("signedAt", t.getSignedAt());
        x.put("signedBy", t.getSignedBy());
        x.put("timeoutMinutes", t.getTimeoutMinutes());
        return x;
    }

    public Map<String, Object> issueView(Issue i) {
        Map<String, Object> x = new LinkedHashMap<>();
        x.put("id", i.getId());
        x.put("issueNo", i.getIssueNo());
        x.put("rxId", i.getPrescription().getId());
        x.put("rxNo", i.getPrescription().getRxNo());
        x.put("patientName", i.getPrescription().getPatientName());
        x.put("type", i.getType().name());
        x.put("typeLabel", i.getType().getLabel());
        x.put("status", i.getStatus().name());
        x.put("statusLabel", i.getStatus().getLabel());
        x.put("ownerStage", i.getOwnerStage().name());
        x.put("ownerStageLabel", i.getOwnerStage().getLabel());
        x.put("description", i.getDescription());
        x.put("locateInfo", i.getLocateInfo());
        x.put("resolution", i.getResolution());
        x.put("compensation", i.getCompensation());
        x.put("reporter", i.getReporter() == null ? null : i.getReporter().getDisplayName());
        x.put("resolver", i.getResolver() == null ? null : i.getResolver().getDisplayName());
        x.put("reportedAt", i.getReportedAt());
        x.put("resolvedAt", i.getResolvedAt());
        return x;
    }

    public Map<String, Object> invoiceView(Invoice i) {
        Map<String, Object> x = new LinkedHashMap<>();
        x.put("id", i.getId());
        x.put("invoiceNo", i.getInvoiceNo());
        x.put("rxNo", i.getPrescription().getRxNo());
        x.put("title", i.getTitle());
        x.put("amount", i.getAmount());
        x.put("kind", i.getKind());
        x.put("issuedAt", i.getIssuedAt());
        return x;
    }

    public Map<String, Object> reminderView(MedicationReminder r) {
        Map<String, Object> x = new LinkedHashMap<>();
        x.put("id", r.getId());
        x.put("rxNo", r.getPrescription().getRxNo());
        x.put("patientName", r.getPatientName());
        x.put("patientPhone", r.getPatientPhone());
        x.put("state", r.getState());
        x.put("timesPerDay", r.getTimesPerDay());
        x.put("nextRemindAt", r.getNextRemindAt());
        x.put("note", r.getNote());
        return x;
    }
}
