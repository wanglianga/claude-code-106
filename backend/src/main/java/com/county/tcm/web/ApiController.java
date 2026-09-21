package com.county.tcm.web;

import com.county.tcm.domain.*;
import com.county.tcm.repo.*;
import com.county.tcm.security.CurrentUserProvider;
import com.county.tcm.service.*;
import com.county.tcm.web.dto.Dtos;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class ApiController {

    private final CurrentUserProvider currentUser;
    private final PrescriptionService rxService;
    private final DecoctService decoctService;
    private final DeliveryService deliveryService;
    private final FollowUpService followUpService;
    private final IssueService issueService;
    private final FinanceService financeService;
    private final AnalyticsService analyticsService;
    private final FulfillmentService fulfillmentService;
    private final DoctorRepository doctorRepo;
    private final HerbRepository herbRepo;
    private final UserRepository userRepo;

    public ApiController(CurrentUserProvider currentUser,
                         PrescriptionService rxService, DecoctService decoctService,
                         DeliveryService deliveryService, FollowUpService followUpService,
                         IssueService issueService, FinanceService financeService,
                         AnalyticsService analyticsService, FulfillmentService fulfillmentService,
                         DoctorRepository doctorRepo, HerbRepository herbRepo, UserRepository userRepo) {
        this.currentUser = currentUser;
        this.rxService = rxService;
        this.decoctService = decoctService;
        this.deliveryService = deliveryService;
        this.followUpService = followUpService;
        this.issueService = issueService;
        this.financeService = financeService;
        this.analyticsService = analyticsService;
        this.fulfillmentService = fulfillmentService;
        this.doctorRepo = doctorRepo;
        this.herbRepo = herbRepo;
        this.userRepo = userRepo;
    }

    private UserAccount me() { return currentUser.require(); }

    private void requireRole(Role... roles) {
        Role r = me().getRole();
        for (Role allowed : roles) if (allowed == r) return;
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "当前角色[" + r.getLabel() + "]无权执行该操作");
    }

    // ---------------- 基础元数据 ----------------

    @GetMapping("/meta")
    public Map<String, Object> meta() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("doctors", doctorRepo.findAll().stream().map(d -> {
            Map<String, Object> x = new LinkedHashMap<>();
            x.put("id", d.getId());
            x.put("name", d.getName());
            x.put("title", d.getTitle());
            x.put("clinicName", d.getClinicName());
            x.put("insuranceQualified", d.isInsuranceQualified());
            return x;
        }).toList());
        m.put("herbs", herbRepo.findAll().stream().map(h -> {
            Map<String, Object> x = new LinkedHashMap<>();
            x.put("id", h.getId());
            x.put("name", h.getName());
            x.put("unitPrice", h.getUnitPrice());
            x.put("stock", h.getStock());
            x.put("insuranceCovered", h.isInsuranceCovered());
            x.put("maxDosePerPacket", h.getMaxDosePerPacket());
            return x;
        }).toList());
        m.put("couriers", userRepo.findAll().stream()
                .filter(u -> u.getRole() == Role.COURIER)
                .map(DomainSupport::userView).toList());
        m.put("enums", Map.of(
                "pickupMethod", enumMeta(PickupMethod.values()),
                "settlementType", enumMeta(SettlementType.values()),
                "specialMethod", enumMeta(SpecialMethod.values()),
                "issueType", enumMeta(IssueType.values())));
        return m;
    }

    private List<Map<String, Object>> enumMeta(Enum<?>[] values) {
        return Arrays.stream(values).map(e -> {
            Map<String, Object> x = new LinkedHashMap<>();
            x.put("value", e.name());
            try {
                x.put("label", e.getClass().getMethod("getLabel").invoke(e));
            } catch (Exception ex) {
                x.put("label", e.name());
            }
            return x;
        }).toList();
    }

    // ---------------- 处方 ----------------

    @PostMapping("/prescriptions")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> create(@Valid @RequestBody Dtos.RxCreateRequest req) {
        requireRole(Role.PATIENT, Role.CLINIC);
        return rxService.create(req, me());
    }

    @PostMapping("/prescriptions/batch")
    @ResponseStatus(HttpStatus.CREATED)
    public List<Map<String, Object>> createBatch(@Valid @RequestBody Dtos.BatchCreateRequest req) {
        requireRole(Role.CLINIC);
        return rxService.createBatch(req, me());
    }

    @GetMapping("/prescriptions")
    public List<Map<String, Object>> list() {
        return rxService.listViewsFor(me());
    }

    @GetMapping("/prescriptions/{id}")
    public Map<String, Object> detail(@PathVariable Long id) {
        UserAccount u = me();
        Prescription rx = rxService.getRx(id);
        rxService.assertCanView(u, rx);
        return fulfillmentService.detail(id);
    }

    // ---------------- 审方 ----------------

    @GetMapping("/prescriptions/{id}/review-preview")
    public Map<String, Object> reviewPreview(@PathVariable Long id) {
        requireRole(Role.PHARMACIST, Role.ADMIN);
        return rxService.previewReview(id);
    }

    @PostMapping("/prescriptions/{id}/review")
    public Map<String, Object> review(@PathVariable Long id, @Valid @RequestBody Dtos.ReviewSubmitRequest req) {
        requireRole(Role.PHARMACIST);
        rxService.review(id, req, me());
        return fulfillmentService.detail(id);
    }

    @PostMapping("/prescriptions/{id}/dispense")
    public Map<String, Object> dispense(@PathVariable Long id, @RequestBody(required = false) Dtos.DispenseRequest req) {
        requireRole(Role.PHARMACIST);
        rxService.dispense(id, req, me());
        return fulfillmentService.detail(id);
    }

    // ---------------- 煎药 ----------------

    @GetMapping("/decoct/queue")
    public List<Map<String, Object>> decoctQueue() {
        requireRole(Role.DECOCTOR, Role.PHARMACIST, Role.ADMIN);
        return decoctService.queueViews();
    }

    @PostMapping("/prescriptions/{id}/decoct/schedule")
    public Map<String, Object> schedule(@PathVariable Long id, @Valid @RequestBody Dtos.DecoctScheduleRequest req) {
        requireRole(Role.DECOCTOR);
        decoctService.schedule(id, req, me());
        return fulfillmentService.detail(id);
    }

    @PostMapping("/prescriptions/{id}/decoct/scan")
    public Map<String, Object> scan(@PathVariable Long id, @Valid @RequestBody Dtos.ScanRequest req) {
        requireRole(Role.DECOCTOR);
        decoctService.scan(id, req, me());
        return fulfillmentService.detail(id);
    }

    @PostMapping("/prescriptions/{id}/decoct/start")
    public Map<String, Object> start(@PathVariable Long id) {
        requireRole(Role.DECOCTOR);
        decoctService.start(id, me());
        return fulfillmentService.detail(id);
    }

    @PostMapping("/prescriptions/{id}/decoct/end")
    public Map<String, Object> end(@PathVariable Long id, @Valid @RequestBody Dtos.DecoctEndRequest req) {
        requireRole(Role.DECOCTOR);
        decoctService.end(id, req, me());
        return fulfillmentService.detail(id);
    }

    // ---------------- 配送 ----------------

    @GetMapping("/delivery/tasks")
    public List<Map<String, Object>> deliveryTasks() {
        requireRole(Role.COURIER, Role.PHARMACIST, Role.ADMIN, Role.FINANCE);
        return deliveryService.listViews();
    }

    @PostMapping("/prescriptions/{id}/delivery/assign")
    public Map<String, Object> assign(@PathVariable Long id, @Valid @RequestBody Dtos.DeliveryAssignRequest req) {
        requireRole(Role.PHARMACIST, Role.ADMIN, Role.COURIER);
        deliveryService.assign(id, req, me());
        return fulfillmentService.detail(id);
    }

    @PostMapping("/prescriptions/{id}/delivery/outbound")
    public Map<String, Object> outbound(@PathVariable Long id) {
        requireRole(Role.COURIER);
        deliveryService.outbound(id, me());
        return fulfillmentService.detail(id);
    }

    @PostMapping("/prescriptions/{id}/delivery/sign")
    public Map<String, Object> sign(@PathVariable Long id, @RequestBody(required = false) Dtos.SignRequest req) {
        requireRole(Role.COURIER, Role.PATIENT, Role.ADMIN);
        deliveryService.sign(id, req == null ? new Dtos.SignRequest(null, null, null) : req, me());
        return fulfillmentService.detail(id);
    }

    @PostMapping("/prescriptions/{id}/address-change")
    public Map<String, Object> changeAddress(@PathVariable Long id, @Valid @RequestBody Dtos.AddressChangeRequest req) {
        requireRole(Role.PATIENT, Role.CLINIC, Role.COURIER);
        deliveryService.changeAddress(id, req, me());
        return fulfillmentService.detail(id);
    }

    // ---------------- 回访 ----------------

    @PostMapping("/prescriptions/{id}/follow-up")
    public Map<String, Object> followUp(@PathVariable Long id, @Valid @RequestBody Dtos.FollowUpRequest req) {
        requireRole(Role.PHARMACIST, Role.ADMIN, Role.CLINIC);
        followUpService.followUp(id, req, me());
        return fulfillmentService.detail(id);
    }

    // ---------------- 异常工单 ----------------

    @GetMapping("/issues")
    public List<Map<String, Object>> issues() {
        return issueService.listViews();
    }

    @PostMapping("/prescriptions/{id}/issues")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> reportIssue(@PathVariable Long id, @Valid @RequestBody Dtos.IssueCreateRequest req) {
        requireRole(Role.PATIENT, Role.CLINIC, Role.PHARMACIST, Role.DECOCTOR, Role.COURIER, Role.ADMIN, Role.FINANCE);
        return issueService.report(id, req, me());
    }

    @PostMapping("/issues/{issueId}/processing")
    public Map<String, Object> processing(@PathVariable Long issueId) {
        return issueService.processing(issueId, me());
    }

    @PostMapping("/issues/{issueId}/resolve")
    public Map<String, Object> resolve(@PathVariable Long issueId, @RequestBody(required = false) Dtos.IssueResolveRequest req) {
        return issueService.resolve(issueId,
                req == null ? new Dtos.IssueResolveRequest(null, null) : req, me());
    }

    // ---------------- 财务/发票 ----------------

    @PostMapping("/prescriptions/{id}/invoice")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> invoice(@PathVariable Long id, @RequestBody(required = false) Dtos.InvoiceRequest req) {
        requireRole(Role.FINANCE);
        return financeService.issueInvoice(id, req, me());
    }

    @PostMapping("/prescriptions/{id}/invoice/reissue-request")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> reissueRequest(@PathVariable Long id,
                                              @RequestBody(required = false) Dtos.InvoiceRequest req) {
        requireRole(Role.CLINIC, Role.PATIENT);
        return financeService.requestReissue(id, req, me());
    }

    @PostMapping("/issues/{issueId}/reissue")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> reissue(@PathVariable Long issueId,
                                       @RequestBody(required = false) Dtos.InvoiceRequest req) {
        requireRole(Role.FINANCE);
        return financeService.reissue(issueId, req, me());
    }

    @GetMapping("/invoices")
    public List<Map<String, Object>> invoices() {
        requireRole(Role.FINANCE, Role.ADMIN, Role.CLINIC);
        return financeService.invoiceViews();
    }

    @GetMapping("/reminders")
    public List<Map<String, Object>> reminders() {
        requireRole(Role.PHARMACIST, Role.ADMIN, Role.FINANCE);
        return financeService.reminderViews();
    }

    @PatchMapping("/reminders/{reminderId}")
    public Map<String, Object> updateReminder(@PathVariable Long reminderId,
                                              @Valid @RequestBody Dtos.ReminderStateRequest req) {
        requireRole(Role.PHARMACIST, Role.ADMIN);
        return financeService.updateReminder(reminderId, req, me());
    }

    @GetMapping("/finance/compensation")
    public Map<String, Object> compensation() {
        requireRole(Role.FINANCE, Role.ADMIN);
        return financeService.compensationLedger();
    }

    // ---------------- 管理复盘 ----------------

    @GetMapping("/analytics/overview")
    public Map<String, Object> overview() {
        requireRole(Role.ADMIN, Role.PHARMACIST);
        return analyticsService.overview();
    }

    @GetMapping("/analytics/by-doctor")
    public List<Map<String, Object>> byDoctor() {
        requireRole(Role.ADMIN, Role.PHARMACIST);
        return analyticsService.byDoctor();
    }

    @GetMapping("/analytics/by-herb")
    public List<Map<String, Object>> byHerb() {
        requireRole(Role.ADMIN, Role.PHARMACIST);
        return analyticsService.byHerb();
    }

    @GetMapping("/analytics/by-pot")
    public List<Map<String, Object>> byPot() {
        requireRole(Role.ADMIN, Role.PHARMACIST, Role.DECOCTOR);
        return analyticsService.byPot();
    }

    @GetMapping("/analytics/by-courier")
    public List<Map<String, Object>> byCourier() {
        requireRole(Role.ADMIN, Role.PHARMACIST);
        return analyticsService.byCourier();
    }

    @GetMapping("/analytics/clinics")
    public List<Map<String, Object>> clinics() {
        requireRole(Role.ADMIN, Role.PHARMACIST);
        return analyticsService.clinicScores();
    }
}
