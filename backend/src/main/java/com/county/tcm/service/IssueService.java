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
public class IssueService {

    private final IssueRepository issueRepo;
    private final DecoctTaskRepository decoctRepo;
    private final DeliveryTaskRepository deliveryRepo;
    private final MedicationReminderRepository reminderRepo;
    private final DomainSupport support;

    public IssueService(IssueRepository issueRepo, DecoctTaskRepository decoctRepo,
                        DeliveryTaskRepository deliveryRepo, MedicationReminderRepository reminderRepo,
                        DomainSupport support) {
        this.issueRepo = issueRepo;
        this.decoctRepo = decoctRepo;
        this.deliveryRepo = deliveryRepo;
        this.reminderRepo = reminderRepo;
        this.support = support;
    }

    private Stage ownerOf(IssueType type) {
        return switch (type) {
            case SHORTAGE -> Stage.PHARMACIST;
            case ADDRESS_CHANGE, DELIVERY_TIMEOUT -> Stage.DELIVERY;
            case SPECIAL_MISSED, BAG_DAMAGED -> Stage.DECOCTION;
            case DISCOMFORT -> Stage.CLINIC;
            case INVOICE -> Stage.FINANCE;
        };
    }

    /** 任一角色在同一条处方履约记录上报异常;自动附带锅号/包装人/配送员定位信息 */
    @Transactional
    public java.util.Map<String, Object> report(Long rxId, Dtos.IssueCreateRequest req, UserAccount reporter) {
        Prescription rx = support.requireRx(rxId);
        IssueType type = IssueType.valueOf(req.type());
        Issue issue = new Issue();
        issue.setIssueNo(support.nextIssueNo(issueRepo.count()));
        issue.setPrescription(rx);
        issue.setType(type);
        issue.setOwnerStage(ownerOf(type));
        issue.setDescription(req.description() == null ? type.getLabel() : req.description());
        issue.setReporter(reporter);
        issue.setReportedAt(LocalDateTime.now());

        String pot = decoctRepo.findByPrescriptionId(rxId).map(t ->
                "锅号:" + t.getPotNo() + ",包装人:" + nz(t.getPacker()) + ",复核人:" + nz(t.getReviewer())
        ).orElse("尚未进入煎药环节");
        String courier = deliveryRepo.findByPrescriptionId(rxId).map(t ->
                "波次:" + nz(t.getWave()) + ",配送员:" + (t.getCourier() == null ? "未指派" : t.getCourier().getDisplayName())
        ).orElse("尚未进入配送环节");
        issue.setLocateInfo(pot + ";" + courier);

        if (type == IssueType.SPECIAL_MISSED) {
            long specials = rx.getHerbs().stream()
                    .filter(h -> h.getSpecialMethod() != SpecialMethod.NORMAL).count();
            issue.setDescription(issue.getDescription()
                    + "(该方含 " + specials + " 味需特殊煎法:"
                    + rx.getHerbs().stream().filter(h -> h.getSpecialMethod() != SpecialMethod.NORMAL)
                        .map(h -> h.getHerbName() + h.getSpecialMethod().getLabel()).reduce((a, b) -> a + "," + b).orElse("无")
                    + ")");
        }

        issueRepo.save(issue);
        support.event(rx, switch (reporter.getRole()) {
            case PATIENT -> Stage.PATIENT;
            case CLINIC -> Stage.CLINIC;
            case PHARMACIST -> Stage.PHARMACIST;
            case DECOCTOR -> Stage.DECOCTION;
            case COURIER -> Stage.DELIVERY;
            default -> Stage.FINANCE;
        }, "上报异常[" + type.getLabel() + "]",
                issue.getIssueNo() + " " + issue.getDescription() + " | 定位:" + issue.getLocateInfo(),
                reporter.getDisplayName());
        return support.issueView(issue);
    }

    /** 处理异常:赔付金额影响药房赔付台账;不适类处理结果联动用药提醒;评分在复盘模块统计 */
    @Transactional
    public java.util.Map<String, Object> resolve(Long issueId, Dtos.IssueResolveRequest req, UserAccount resolver) {
        Issue issue = issueRepo.findById(issueId)
                .orElseThrow(() -> DomainSupport.badRequest("工单不存在: " + issueId));
        if (issue.getStatus() == IssueStatus.RESOLVED) {
            throw DomainSupport.conflict("工单已解决");
        }
        issue.setStatus(IssueStatus.RESOLVED);
        issue.setResolution(req.resolution());
        issue.setCompensation(req.compensation() == null ? BigDecimal.ZERO : req.compensation());
        issue.setResolver(resolver);
        issue.setResolvedAt(LocalDateTime.now());
        issueRepo.save(issue);

        support.event(issue.getPrescription(), issue.getOwnerStage(), "异常工单解决",
                issue.getIssueNo() + " 处理结果:" + nz(req.resolution())
                        + (issue.getCompensation().signum() > 0 ? ",药房赔付 " + issue.getCompensation() + " 元" : ""),
                resolver.getDisplayName());

        if (issue.getType() == IssueType.DISCOMFORT) {
            reminderRepo.findByPrescriptionId(issue.getPrescription().getId()).ifPresent(r -> {
                r.setNote("不适工单已闭环:" + nz(req.resolution()));
                // 经医生研判可继续用药时由提醒接口手动恢复,此处保持暂停
            });
        }
        return support.issueView(issue);
    }

    @Transactional(readOnly = true)
    public List<java.util.Map<String, Object>> listViews() {
        return listAll().stream().map(support::issueView).toList();
    }

    @Transactional(readOnly = true)
    public List<Issue> listAll() {
        return issueRepo.findAllByOrderByReportedAtDesc();
    }

    @Transactional
    public java.util.Map<String, Object> processing(Long issueId, UserAccount resolver) {
        Issue issue = issueRepo.findById(issueId)
                .orElseThrow(() -> DomainSupport.badRequest("工单不存在: " + issueId));
        issue.setStatus(IssueStatus.PROCESSING);
        issue.setResolver(resolver);
        issueRepo.save(issue);
        support.event(issue.getPrescription(), issue.getOwnerStage(), "工单受理处理中",
                issue.getIssueNo(), resolver.getDisplayName());
        return support.issueView(issue);
    }

    private static String nz(String s) { return s == null ? "无" : s; }
}
