package com.county.tcm.service;

import com.county.tcm.domain.*;
import com.county.tcm.repo.*;
import com.county.tcm.web.dto.Dtos;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class FinanceService {

    private final InvoiceRepository invoiceRepo;
    private final IssueRepository issueRepo;
    private final MedicationReminderRepository reminderRepo;
    private final DomainSupport support;

    public FinanceService(InvoiceRepository invoiceRepo, IssueRepository issueRepo,
                          MedicationReminderRepository reminderRepo, DomainSupport support) {
        this.invoiceRepo = invoiceRepo;
        this.issueRepo = issueRepo;
        this.reminderRepo = reminderRepo;
        this.support = support;
    }

    /** 正常开票(结算后) */
    @Transactional
    public java.util.Map<String, Object> issueInvoice(Long rxId, Dtos.InvoiceRequest req, UserAccount finance) {
        Prescription rx = support.requireRx(rxId);
        if (!rx.isSettled()) {
            throw DomainSupport.conflict("处方尚未结算,不能开票");
        }
        Invoice inv = new Invoice();
        inv.setInvoiceNo(support.nextInvoiceNo(invoiceRepo.count()));
        inv.setPrescription(rx);
        inv.setTitle(req == null || req.title() == null ? rx.getPatientName() : req.title());
        inv.setAmount(rx.getTotalAmount());
        inv.setKind("NORMAL");
        inv.setFinanceUser(finance);
        inv.setIssuedAt(LocalDateTime.now());
        invoiceRepo.save(inv);
        support.event(rx, Stage.FINANCE, "开具发票",
                inv.getInvoiceNo() + " 抬头:" + inv.getTitle() + " 金额:" + inv.getAmount(), finance.getDisplayName());
        return support.invoiceView(inv);
    }

    /** 诊所/患者提出补开发票需求(挂到同一条履约记录的工单) */
    @Transactional
    public java.util.Map<String, Object> requestReissue(Long rxId, Dtos.InvoiceRequest req, UserAccount requester) {
        Prescription rx = support.requireRx(rxId);
        Issue issue = new Issue();
        issue.setIssueNo(support.nextIssueNo(issueRepo.count()));
        issue.setPrescription(rx);
        issue.setType(IssueType.INVOICE);
        issue.setOwnerStage(Stage.FINANCE);
        issue.setDescription("诊所要求补开发票,抬头:" + (req == null || req.title() == null ? rx.getClinicName() : req.title()));
        issue.setReporter(requester);
        issue.setReportedAt(LocalDateTime.now());
        issueRepo.save(issue);
        support.event(rx, requester.getRole() == Role.CLINIC ? Stage.CLINIC : Stage.PATIENT,
                "申请补开发票", issue.getIssueNo() + " " + issue.getDescription(), requester.getDisplayName());
        return support.issueView(issue);
    }

    /** 财务补开发票并闭环发票工单 */
    @Transactional
    public java.util.Map<String, Object> reissue(Long issueId, Dtos.InvoiceRequest req, UserAccount finance) {
        Issue issue = issueRepo.findById(issueId)
                .orElseThrow(() -> DomainSupport.badRequest("工单不存在: " + issueId));
        if (issue.getType() != IssueType.INVOICE) {
            throw DomainSupport.badRequest("该工单不是补开发票工单");
        }
        Prescription rx = issue.getPrescription();
        Invoice inv = new Invoice();
        inv.setInvoiceNo(support.nextInvoiceNo(invoiceRepo.count()));
        inv.setPrescription(rx);
        inv.setTitle(req == null || req.title() == null ? rx.getClinicName() : req.title());
        inv.setAmount(rx.getTotalAmount());
        inv.setKind("REISSUED");
        inv.setIssue(issue);
        inv.setFinanceUser(finance);
        inv.setIssuedAt(LocalDateTime.now());
        invoiceRepo.save(inv);

        issue.setStatus(IssueStatus.RESOLVED);
        issue.setResolution("已补开发票 " + inv.getInvoiceNo());
        issue.setResolver(finance);
        issue.setResolvedAt(LocalDateTime.now());
        issueRepo.save(issue);

        support.event(rx, Stage.FINANCE, "补开发票",
                "工单 " + issue.getIssueNo() + " 已补开 " + inv.getInvoiceNo()
                        + ",抬头:" + inv.getTitle(), finance.getDisplayName());
        return support.invoiceView(inv);
    }

    @Transactional(readOnly = true)
    public List<java.util.Map<String, Object>> invoiceViews() {
        return invoices().stream().map(support::invoiceView).toList();
    }

    @Transactional(readOnly = true)
    public List<Invoice> invoices() {
        return invoiceRepo.findAllByOrderByIssuedAtDesc();
    }

    @Transactional(readOnly = true)
    public List<java.util.Map<String, Object>> reminderViews() {
        return reminders().stream().map(support::reminderView).toList();
    }

    @Transactional(readOnly = true)
    public List<MedicationReminder> reminders() {
        return reminderRepo.findAllByOrderByNextRemindAtAsc();
    }

    @Transactional
    public java.util.Map<String, Object> updateReminder(Long reminderId, Dtos.ReminderStateRequest req, UserAccount operator) {
        MedicationReminder r = reminderRepo.findById(reminderId)
                .orElseThrow(() -> DomainSupport.badRequest("提醒不存在: " + reminderId));
        r.setState(req.state());
        if (req.note() != null) r.setNote(req.note());
        reminderRepo.save(r);
        support.event(r.getPrescription(), Stage.PHARMACIST, "调整用药提醒",
                "状态改为 " + req.state() + ":" + (req.note() == null ? "" : req.note()), operator.getDisplayName());
        return support.reminderView(r);
    }

    /** 赔付台账:全部已决赔付工单汇总 */
    @Transactional(readOnly = true)
    public java.util.Map<String, Object> compensationLedger() {
        List<Issue> paid = issueRepo.findAll().stream()
                .filter(i -> i.getCompensation() != null && i.getCompensation().signum() > 0)
                .toList();
        BigDecimal total = paid.stream().map(Issue::getCompensation).reduce(BigDecimal.ZERO, BigDecimal::add);
        return java.util.Map.of("totalCompensation", total, "count", paid.size(), "items", paid.stream().map(i -> {
            java.util.Map<String, Object> m = new java.util.LinkedHashMap<>();
            m.put("issueNo", i.getIssueNo());
            m.put("rxNo", i.getPrescription().getRxNo());
            m.put("type", i.getType().getLabel());
            m.put("amount", i.getCompensation());
            m.put("resolvedAt", i.getResolvedAt());
            return m;
        }).toList());
    }
}
