package com.tcm.service;

import com.tcm.config.AuthUtils;
import com.tcm.model.*;
import com.tcm.repository.DeliveryRepository;
import com.tcm.repository.PrescriptionRepository;
import com.tcm.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

/** 配送：按波次派送、签收、超时自动登记异常；自提/老人代取签收 */
@Service
public class DeliveryService {

    private final DeliveryRepository deliveries;
    private final PrescriptionRepository prescriptions;
    private final UserRepository users;
    private final ExceptionService exceptionService;

    public DeliveryService(DeliveryRepository deliveries, PrescriptionRepository prescriptions,
                           UserRepository users, ExceptionService exceptionService) {
        this.deliveries = deliveries;
        this.prescriptions = prescriptions;
        this.users = users;
        this.exceptionService = exceptionService;
    }

    public List<Delivery> list(User user) {
        AuthUtils.requireRole(user, Role.COURIER, Role.ADMIN, Role.PHARMACIST);
        if (user.getRole() == Role.COURIER) {
            return deliveries.findByCourierIdOrderByIdDesc(user.getId());
        }
        return deliveries.findAll();
    }

    /** 待派送池(所有角色可见，便于配送员认领) */
    public List<Delivery> pendingPool(User user) {
        AuthUtils.requireRole(user, Role.COURIER, Role.ADMIN, Role.PHARMACIST);
        return deliveries.findByStatusOrderByIdDesc(DeliveryStatus.PENDING);
    }

    @Transactional
    public Delivery dispatch(Long deliveryId, Long courierId, User user) {
        AuthUtils.requireRole(user, Role.COURIER, Role.ADMIN);
        Delivery d = get(deliveryId);
        if (d.getStatus() != DeliveryStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "仅待配送订单可派送");
        }
        User courier;
        if (courierId != null) {
            courier = users.findById(courierId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "配送员不存在"));
        } else if (user.getRole() == Role.COURIER) {
            courier = user;
        } else {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请指定配送员");
        }
        d.setCourier(courier);
        d.setStatus(DeliveryStatus.DELIVERING);
        d.setDispatchedAt(LocalDateTime.now());
        Prescription p = d.getPrescription();
        p.setStatus(PrescriptionStatus.DELIVERING);
        prescriptions.save(p);
        return deliveries.save(d);
    }

    @Transactional
    public Delivery sign(Long deliveryId, String signedBy, User user) {
        AuthUtils.requireRole(user, Role.COURIER, Role.ADMIN);
        Delivery d = get(deliveryId);
        if (d.getStatus() != DeliveryStatus.DELIVERING) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "仅配送中的订单可签收");
        }
        if (signedBy == null || signedBy.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请填写签收人");
        }
        d.setSignedBy(signedBy);
        d.setSignedAt(LocalDateTime.now());
        d.setStatus(DeliveryStatus.SIGNED);
        // 签收时仍超时 → 登记配送超时异常
        if (Boolean.TRUE.equals(d.getTimeout()) || isOverdue(d)) {
            d.setTimeout(true);
            exceptionService.auto(d.getPrescription(), ExceptionType.DELIVERY_TIMEOUT,
                    "配送超过 " + d.getTimeoutHours() + " 小时时限(波次 " + d.getWaveNo() + ")", user);
        }
        Prescription p = d.getPrescription();
        p.setStatus(PrescriptionStatus.SIGNED);
        prescriptions.save(p);
        return deliveries.save(d);
    }

    /** 自提 / 老人代取 窗口签收 */
    @Transactional
    public Prescription pickupSign(Long prescriptionId, String signedBy, User user) {
        AuthUtils.requireRole(user, Role.PHARMACIST, Role.ADMIN);
        Prescription p = prescriptions.findById(prescriptionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "处方不存在"));
        if (p.getStatus() != PrescriptionStatus.READY_PICKUP) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "该处方不在待取药状态");
        }
        p.setStatus(PrescriptionStatus.SIGNED);
        return prescriptions.save(p);
    }

    private boolean isOverdue(Delivery d) {
        return d.getDispatchedAt() != null
                && d.getDispatchedAt().plusHours(d.getTimeoutHours()).isBefore(LocalDateTime.now());
    }

    /** 定时巡检：配送超时自动标记并登记异常 */
    @Scheduled(fixedDelay = 60_000)
    @Transactional
    public void scanTimeouts() {
        for (Delivery d : deliveries.findByStatus(DeliveryStatus.DELIVERING)) {
            if (isOverdue(d) && !Boolean.TRUE.equals(d.getTimeout())) {
                d.setTimeout(true);
                deliveries.save(d);
                exceptionService.auto(d.getPrescription(), ExceptionType.DELIVERY_TIMEOUT,
                        "配送超过 " + d.getTimeoutHours() + " 小时未签收(波次 " + d.getWaveNo() + ")", d.getCourier());
            }
        }
    }

    private Delivery get(Long id) {
        return deliveries.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "配送单不存在"));
    }
}
