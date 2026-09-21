package com.tcm.service;

import com.tcm.config.AuthUtils;
import com.tcm.model.*;
import com.tcm.repository.ExceptionEventRepository;
import com.tcm.repository.InvoiceRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 财务：发票开具/补开、结算与赔付汇总 */
@Service
public class FinanceService {

    private final InvoiceRepository invoices;
    private final ExceptionEventRepository exceptions;

    public FinanceService(InvoiceRepository invoices, ExceptionEventRepository exceptions) {
        this.invoices = invoices;
        this.exceptions = exceptions;
    }

    public List<Invoice> list(User user) {
        List<Invoice> all = invoices.findAll();
        if (user.getRole() == Role.CLINIC) {
            return all.stream().filter(i -> i.getPrescription().getClinic() != null
                    && i.getPrescription().getClinic().getId().equals(user.getClinic().getId())).toList();
        }
        if (user.getRole() == Role.PATIENT) {
            return all.stream().filter(i -> i.getPrescription().getSubmittedBy().getId().equals(user.getId())).toList();
        }
        AuthUtils.requireRole(user, Role.FINANCE, Role.ADMIN, Role.PHARMACIST);
        return all;
    }

    /** 补开发票(诊所要求补开 → 财务补开 → 关联异常自动完结) */
    @Transactional
    public Invoice reissue(Long invoiceId, String reason, User user) {
        AuthUtils.requireRole(user, Role.FINANCE, Role.ADMIN);
        Invoice inv = invoices.findById(invoiceId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "发票不存在"));
        if (inv.getStatus() == InvoiceStatus.REISSUED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "该发票已补开");
        }
        inv.setStatus(InvoiceStatus.REISSUED);
        inv.setReissueReason(reason);
        inv.setReissuedAt(LocalDateTime.now());
        inv.setInvoiceNo(inv.getInvoiceNo() + "-R");
        invoices.save(inv);

        Prescription p = inv.getPrescription();
        for (ExceptionEvent e : exceptions.findByPrescriptionIdAndTypeAndStatusNot(
                p.getId(), ExceptionType.INVOICE_REISSUE, ExceptionStatus.RESOLVED)) {
            e.setStatus(ExceptionStatus.RESOLVED);
            e.setHandler(user);
            e.setResolution("财务已补开发票：" + inv.getInvoiceNo());
            e.setResolvedAt(LocalDateTime.now());
            exceptions.save(e);
        }
        return inv;
    }

    public Map<String, Object> summary(User user) {
        AuthUtils.requireRole(user, Role.FINANCE, Role.ADMIN);
        BigDecimal invoiced = BigDecimal.ZERO;
        BigDecimal insurance = BigDecimal.ZERO;
        BigDecimal selfPay = BigDecimal.ZERO;
        for (Invoice i : invoices.findAll()) {
            invoiced = invoiced.add(i.getAmount());
            Prescription p = i.getPrescription();
            if (p.getInsuranceAmount() != null) {
                insurance = insurance.add(p.getInsuranceAmount());
            }
            if (p.getSelfPayAmount() != null) {
                selfPay = selfPay.add(p.getSelfPayAmount());
            }
        }
        BigDecimal compensation = exceptions.findAll().stream()
                .map(ExceptionEvent::getCompensation)
                .filter(c -> c != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("invoiceCount", invoices.count());
        map.put("invoicedAmount", invoiced);
        map.put("insuranceAmount", insurance);
        map.put("selfPayAmount", selfPay);
        map.put("compensationAmount", compensation);
        return map;
    }
}
