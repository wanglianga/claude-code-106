package com.county.tcm.service;

import com.county.tcm.domain.*;
import com.county.tcm.repo.*;
import com.county.tcm.web.dto.Dtos;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class FollowUpService {

    private final FollowUpRepository followRepo;
    private final IssueRepository issueRepo;
    private final MedicationReminderRepository reminderRepo;
    private final DomainSupport support;

    public FollowUpService(FollowUpRepository followRepo, IssueRepository issueRepo,
                           MedicationReminderRepository reminderRepo, DomainSupport support) {
        this.followRepo = followRepo;
        this.issueRepo = issueRepo;
        this.reminderRepo = reminderRepo;
        this.support = support;
    }

    /** 用药回访;不适结果自动开工单并暂停用药提醒,回访结果驱动后续提醒/赔付/评分 */
    @Transactional
    public FollowUp followUp(Long rxId, Dtos.FollowUpRequest req, UserAccount operator) {
        Prescription rx = support.requireRx(rxId);
        if (rx.getStatus() != RxStatus.SIGNED && rx.getStatus() != RxStatus.FOLLOWED_UP) {
            throw DomainSupport.conflict("仅已签收处方可回访");
        }
        FollowUpResult result = FollowUpResult.valueOf(req.result());

        FollowUp fu = new FollowUp();
        fu.setPrescription(rx);
        fu.setOperator(operator);
        fu.setResult(result);
        fu.setSymptoms(req.symptoms());
        fu.setAdvice(req.advice());
        fu.setSatisfactionScore(req.satisfactionScore());
        fu.setFollowedAt(LocalDateTime.now());

        if (result == FollowUpResult.DISCOMFORT) {
            Issue issue = new Issue();
            issue.setIssueNo(support.nextIssueNo(issueRepo.count()));
            issue.setPrescription(rx);
            issue.setType(IssueType.DISCOMFORT);
            issue.setOwnerStage(Stage.CLINIC);
            issue.setDescription("患者服药后不适:" + nz(req.symptoms())
                    + ";回访建议:" + nz(req.advice()));
            issue.setLocateInfo("医生:" + rx.getDoctorName() + ",诊所:" + rx.getClinicName()
                    + ",锅号:" + supportPot(rx));
            issue.setReporter(operator);
            issue.setReportedAt(LocalDateTime.now());
            issueRepo.save(issue);
            fu.setIssue(issue);
            reminderRepo.findByPrescriptionId(rxId).ifPresent(r -> {
                r.setState("PAUSED");
                r.setNote("患者反馈不适,用药提醒已暂停,待诊所/药师确认");
            });
            support.event(rx, Stage.PATIENT, "回访发现不适",
                    "已登记 " + issue.getIssueNo() + ",用药提醒暂停,等待医生研判与赔付评估", operator.getDisplayName());
        } else if (result == FollowUpResult.GOOD) {
            reminderRepo.findByPrescriptionId(rxId).ifPresent(r -> {
                if ("PAUSED".equals(r.getState())) {
                    r.setState("ACTIVE");
                    r.setNote("复诊确认无异常,提醒恢复");
                }
            });
            support.event(rx, Stage.PATIENT, "用药回访",
                    "患者反馈服药正常" + (req.satisfactionScore() != null ? ",满意度 " + req.satisfactionScore() + " 分" : ""),
                    operator.getDisplayName());
        } else {
            support.event(rx, Stage.PATIENT, "用药回访", "回访结果:" + result.getLabel(), operator.getDisplayName());
        }

        followRepo.save(fu);
        rx.setStatus(RxStatus.FOLLOWED_UP);
        return fu;
    }

    private String supportPot(Prescription rx) {
        return "见煎药记录";
    }

    private static String nz(String s) { return s == null ? "" : s; }
}
