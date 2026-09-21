package com.tcm.service;

import com.tcm.config.AuthUtils;
import com.tcm.model.*;
import com.tcm.repository.DeliveryRepository;
import com.tcm.repository.ExceptionEventRepository;
import com.tcm.repository.InvoiceRepository;
import com.tcm.repository.PrescriptionRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** 处方履约异常：缺药/改地址/特殊煎法遗漏/包装破损/配送超时/服药不适/补开发票 */
@Service
public class ExceptionService {

    private final ExceptionEventRepository exceptions;
    private final PrescriptionRepository prescriptions;
    private final DeliveryRepository deliveries;
    private final InvoiceRepository invoices;

    public ExceptionService(ExceptionEventRepository exceptions, PrescriptionRepository prescriptions,
                            DeliveryRepository deliveries, InvoiceRepository invoices) {
        this.exceptions = exceptions;
        this.prescriptions = prescriptions;
        this.deliveries = deliveries;
        this.invoices = invoices;
    }

    public record CreateReq(Long prescriptionId, ExceptionType type, String description, String newAddress) {
    }

    public record ResolveReq(String resolution, BigDecimal compensation) {
    }

    /** 系统内部自动登记异常(漏袋/超时/不适等)，同类型未结异常不重复登记 */
    @Transactional
    public ExceptionEvent auto(Prescription p, ExceptionType type, String description, User reporter) {
        boolean exists = exceptions.existsByPrescriptionIdAndTypeAndStatusNot(
                p.getId(), type, ExceptionStatus.RESOLVED);
        if (exists) {
            return null;
        }
        ExceptionEvent e = new ExceptionEvent();
        e.setPrescription(p);
        e.setType(type);
        e.setDescription(description);
        e.setReportedBy(reporter);
        e.setStatus(ExceptionStatus.OPEN);
        return exceptions.save(e);
    }

    @Transactional
    public ExceptionEvent create(CreateReq req, User user) {
        Prescription p = prescriptions.findById(req.prescriptionId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "处方不存在"));
        // 患者/诊所只能就自己的处方上报
        if (user.getRole() == Role.PATIENT && !p.getSubmittedBy().getId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "无权操作该处方");
        }
        if (user.getRole() == Role.CLINIC
                && (p.getClinic() == null || !p.getClinic().getId().equals(user.getClinic().getId()))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "无权操作该处方");
        }
        String desc = req.description() != null ? req.description() : "";

        if (req.type() == ExceptionType.ADDRESS_CHANGE) {
            // 患者临时改地址：同步到处方与未签收配送单
            if (req.newAddress() == null || req.newAddress().isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "改地址须填写新地址");
            }
            p.setDeliveryAddress(req.newAddress());
            prescriptions.save(p);
            deliveries.findByPrescriptionId(p.getId()).ifPresent(d -> {
                if (d.getStatus() != DeliveryStatus.SIGNED) {
                    d.setAddress(req.newAddress());
                    deliveries.save(d);
                }
            });
            desc = desc + " 新地址：" + req.newAddress();
        }
        if (req.type() == ExceptionType.INVOICE_REISSUE) {
            // 诊所要求补开发票：发票进入补开申请
            invoices.findByPrescriptionId(p.getId()).ifPresent(inv -> {
                if (inv.getStatus() == InvoiceStatus.ISSUED) {
                    inv.setStatus(InvoiceStatus.REISSUE_REQUESTED);
                    invoices.save(inv);
                }
            });
        }

        ExceptionEvent e = new ExceptionEvent();
        e.setPrescription(p);
        e.setType(req.type());
        e.setDescription(desc);
        e.setReportedBy(user);
        e.setStatus(ExceptionStatus.OPEN);
        return exceptions.save(e);
    }

    @Transactional
    public ExceptionEvent resolve(Long id, ResolveReq req, User user) {
        AuthUtils.requireRole(user, Role.ADMIN, Role.PHARMACIST);
        ExceptionEvent e = exceptions.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "异常不存在"));
        if (e.getStatus() == ExceptionStatus.RESOLVED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "该异常已处理完结");
        }
        e.setStatus(ExceptionStatus.RESOLVED);
        e.setHandler(user);
        e.setResolution(req.resolution());
        e.setCompensation(req.compensation() != null ? req.compensation() : BigDecimal.ZERO);
        e.setResolvedAt(LocalDateTime.now());
        exceptions.save(e);

        // 该处方所有异常已结且已回访 → 履约完成
        Prescription p = e.getPrescription();
        boolean anyOpen = exceptions.existsByPrescriptionIdAndStatusNot(p.getId(), ExceptionStatus.RESOLVED);
        if (!anyOpen && p.getStatus() == PrescriptionStatus.FOLLOWED_UP) {
            p.setStatus(PrescriptionStatus.COMPLETED);
            prescriptions.save(p);
        }
        return e;
    }

    public List<ExceptionEvent> list(User user, ExceptionStatus status) {
        AuthUtils.requireRole(user, Role.ADMIN, Role.PHARMACIST, Role.DECOCTER, Role.COURIER, Role.FINANCE);
        if (status != null) {
            return exceptions.findByStatusOrderByCreatedAtDesc(status);
        }
        return exceptions.findAllByOrderByCreatedAtDesc();
    }
}
