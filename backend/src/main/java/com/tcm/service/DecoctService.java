package com.tcm.service;

import com.tcm.config.AuthUtils;
import com.tcm.model.*;
import com.tcm.repository.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 煎药房：排队、扫描药包、煎煮起止、异常气味、漏袋、复核人 */
@Service
public class DecoctService {

    private final DecoctTaskRepository tasks;
    private final DecoctRecordRepository records;
    private final PrescriptionRepository prescriptions;
    private final PrescriptionItemRepository items;
    private final DeliveryRepository deliveries;
    private final ExceptionService exceptionService;
    private final int timeoutHours;

    public DecoctService(DecoctTaskRepository tasks, DecoctRecordRepository records,
                         PrescriptionRepository prescriptions, PrescriptionItemRepository items,
                         DeliveryRepository deliveries, ExceptionService exceptionService,
                         @Value("${app.delivery-timeout-hours:4}") int timeoutHours) {
        this.tasks = tasks;
        this.records = records;
        this.prescriptions = prescriptions;
        this.items = items;
        this.deliveries = deliveries;
        this.exceptionService = exceptionService;
        this.timeoutHours = timeoutHours;
    }

    public record FinishReq(Boolean abnormalSmell, String smellNote, Integer leakedBags,
                            String reviewerName, Boolean specialHandledConfirmed, String note) {
    }

    /** 煎药队列(急煎优先、先排先煎)，含每个任务的操作记录 */
    public List<Map<String, Object>> queue(User user) {
        AuthUtils.requireRole(user, Role.DECOCTER, Role.ADMIN, Role.PHARMACIST);
        List<DecoctTask> list = tasks.findByStatusInOrderByUrgentDescCreatedAtAsc(
                List.of(DecoctStatus.QUEUED, DecoctStatus.IN_PROGRESS));
        return list.stream().map(t -> {
            Map<String, Object> m = new LinkedHashMap<String, Object>();
            m.put("task", t);
            m.put("record", records.findFirstByTaskIdOrderByIdDesc(t.getId()).orElse(null));
            m.put("items", items.findByPrescriptionId(t.getPrescription().getId()));
            return m;
        }).toList();
    }

    private DecoctTask getTask(Long id) {
        return tasks.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "煎药任务不存在"));
    }

    /** 扫描药包：扫描码须与处方编号一致 */
    @Transactional
    public DecoctRecord scan(Long taskId, String code, User user) {
        AuthUtils.requireRole(user, Role.DECOCTER, Role.ADMIN);
        DecoctTask t = getTask(taskId);
        if (t.getStatus() == DecoctStatus.DONE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "该任务已完成");
        }
        if (code == null || !t.getPrescription().getRxNo().equals(code.trim())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "扫描码与处方编号不一致，应为 " + t.getPrescription().getRxNo());
        }
        DecoctRecord r = records.findFirstByTaskIdOrderByIdDesc(taskId).orElse(new DecoctRecord());
        r.setTask(t);
        r.setOperator(user);
        r.setScanCode(code.trim());
        r.setScanTime(LocalDateTime.now());
        return records.save(r);
    }

    /** 开始煎煮(须先扫描) */
    @Transactional
    public DecoctRecord start(Long taskId, User user) {
        AuthUtils.requireRole(user, Role.DECOCTER, Role.ADMIN);
        DecoctTask t = getTask(taskId);
        if (t.getStatus() != DecoctStatus.QUEUED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "仅排队中的任务可开始煎煮");
        }
        DecoctRecord r = records.findFirstByTaskIdOrderByIdDesc(taskId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "请先扫描药包再开始煎煮"));
        if (r.getScanCode() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请先扫描药包再开始煎煮");
        }
        r.setOperator(user);
        r.setStartTime(LocalDateTime.now());
        t.setStatus(DecoctStatus.IN_PROGRESS);
        Prescription p = t.getPrescription();
        p.setStatus(PrescriptionStatus.DECOCTING);
        prescriptions.save(p);
        tasks.save(t);
        return records.save(r);
    }

    /** 结束煎煮：记录异常气味/漏袋/复核人，自动生成异常与配送单 */
    @Transactional
    public DecoctRecord finish(Long taskId, FinishReq req, User user) {
        AuthUtils.requireRole(user, Role.DECOCTER, Role.ADMIN);
        DecoctTask t = getTask(taskId);
        if (t.getStatus() != DecoctStatus.IN_PROGRESS) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "仅煎药中的任务可结束");
        }
        if (req.reviewerName() == null || req.reviewerName().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "须填写复核人");
        }
        DecoctRecord r = records.findFirstByTaskIdOrderByIdDesc(taskId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "缺少煎药记录"));
        r.setOperator(user);
        r.setEndTime(LocalDateTime.now());
        r.setAbnormalSmell(Boolean.TRUE.equals(req.abnormalSmell()));
        r.setSmellNote(req.smellNote());
        r.setLeakedBags(req.leakedBags() != null ? req.leakedBags() : 0);
        r.setReviewerName(req.reviewerName());
        r.setNote(req.note());
        records.save(r);

        t.setStatus(DecoctStatus.DONE);
        tasks.save(t);
        Prescription p = t.getPrescription();

        // 异常气味 → 质量异常；漏袋 → 包装破损
        if (Boolean.TRUE.equals(req.abnormalSmell())) {
            exceptionService.auto(p, ExceptionType.QUALITY_ABNORMAL,
                    "煎煮过程出现异常气味：" + (req.smellNote() != null ? req.smellNote() : "未说明"), user);
        }
        if (req.leakedBags() != null && req.leakedBags() > 0) {
            exceptionService.auto(p, ExceptionType.BAG_DAMAGED,
                    "包装袋破损/漏袋 " + req.leakedBags() + " 袋(锅号 " + t.getPotNo() + ")", user);
        }
        // 特殊煎法遗漏检测：处方含特殊煎法药味但未确认执行
        boolean hasSpecial = items.findByPrescriptionId(p.getId()).stream()
                .anyMatch(i -> i.getSpecialHandling() != SpecialHandling.NONE);
        if (hasSpecial && !Boolean.TRUE.equals(req.specialHandledConfirmed())) {
            exceptionService.auto(p, ExceptionType.SPECIAL_MISSED,
                    "处方含先煎/后下等特殊煎法药味，煎药完成时未确认已执行特殊煎法", user);
        }

        if (p.getPickupMethod() == PickupMethod.DELIVERY) {
            Delivery d = new Delivery();
            d.setPrescription(p);
            d.setWaveNo(t.getWaveNo());
            d.setAddress(p.getDeliveryAddress());
            d.setReceiverName(p.getPatientName());
            d.setReceiverPhone(p.getPatientPhone());
            d.setTimeoutHours(timeoutHours);
            deliveries.save(d);
            p.setStatus(PrescriptionStatus.DECOCTED);
        } else {
            p.setStatus(PrescriptionStatus.READY_PICKUP);
        }
        prescriptions.save(p);
        return r;
    }
}
