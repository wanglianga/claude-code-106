package com.tcm.service;

import com.tcm.config.AuthUtils;
import com.tcm.model.*;
import com.tcm.repository.*;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

/** 用药回访：影响用药提醒、药房赔付(经异常)与诊所合作评分 */
@Service
public class FollowUpService {

    private final FollowUpRepository followUps;
    private final PrescriptionRepository prescriptions;
    private final ClinicRepository clinics;
    private final ReminderRepository reminders;
    private final ExceptionEventRepository exceptions;
    private final ExceptionService exceptionService;

    public FollowUpService(FollowUpRepository followUps, PrescriptionRepository prescriptions,
                           ClinicRepository clinics, ReminderRepository reminders,
                           ExceptionEventRepository exceptions, ExceptionService exceptionService) {
        this.followUps = followUps;
        this.prescriptions = prescriptions;
        this.clinics = clinics;
        this.reminders = reminders;
        this.exceptions = exceptions;
        this.exceptionService = exceptionService;
    }

    public record FollowUpReq(Long prescriptionId, FollowUpResult result, String discomfortDesc,
                              Integer satisfaction, Boolean needReminder, String note) {
    }

    @Transactional
    public FollowUp create(FollowUpReq req, User user) {
        AuthUtils.requireRole(user, Role.PHARMACIST, Role.ADMIN);
        Prescription p = prescriptions.findById(req.prescriptionId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "处方不存在"));
        if (p.getStatus() != PrescriptionStatus.SIGNED && p.getStatus() != PrescriptionStatus.FOLLOWED_UP) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "仅已签收处方可回访");
        }
        FollowUp f = new FollowUp();
        f.setPrescription(p);
        f.setResult(req.result() != null ? req.result() : FollowUpResult.NORMAL);
        f.setDiscomfortDesc(req.discomfortDesc());
        f.setSatisfaction(req.satisfaction());
        f.setNeedReminder(Boolean.TRUE.equals(req.needReminder()));
        f.setNote(req.note());
        f.setCreatedBy(user);
        followUps.save(f);

        // 服药不适 → 自动登记异常，进入赔付处理流程
        if (f.getResult() == FollowUpResult.DISCOMFORT) {
            exceptionService.auto(p, ExceptionType.DISCOMFORT,
                    "患者服药后不适：" + (req.discomfortDesc() != null ? req.discomfortDesc() : "未说明"), user);
        }
        // 用药提醒：后续 3 天每日早晚两次
        if (Boolean.TRUE.equals(req.needReminder())) {
            for (int day = 1; day <= 3; day++) {
                for (int hour : new int[]{8, 18}) {
                    Reminder r = new Reminder();
                    r.setPrescription(p);
                    r.setMessage("请按时温服中药(每日2次，饭后半小时)，如有不适及时联系药房 "
                            + p.getPatientName() + " " + p.getPatientPhone());
                    r.setRemindAt(LocalDateTime.now().plusDays(day).withHour(hour).withMinute(0).withSecond(0).withNano(0));
                    reminders.save(r);
                }
            }
        }
        // 满意度影响诊所合作评分
        if (req.satisfaction() != null && p.getClinic() != null) {
            Clinic clinic = p.getClinic();
            double delta = switch (req.satisfaction()) {
                case 1, 2 -> -3.0;
                case 3 -> -1.0;
                case 5 -> 1.0;
                default -> 0.0;
            };
            clinic.setCooperationScore(Math.max(0, Math.min(100, clinic.getCooperationScore() + delta)));
            clinics.save(clinic);
        }
        // 有未结异常 → 已回访；无异常 → 履约完成
        boolean anyOpen = exceptions.existsByPrescriptionIdAndStatusNot(p.getId(), ExceptionStatus.RESOLVED);
        p.setStatus(anyOpen ? PrescriptionStatus.FOLLOWED_UP : PrescriptionStatus.COMPLETED);
        prescriptions.save(p);
        return f;
    }

    public List<FollowUp> list(User user) {
        AuthUtils.requireRole(user, Role.PHARMACIST, Role.ADMIN);
        return followUps.findAll(Sort.by(Sort.Direction.DESC, "createdAt"));
    }

    /** 待回访：已签收但未回访的处方 */
    public List<Prescription> pending(User user) {
        AuthUtils.requireRole(user, Role.PHARMACIST, Role.ADMIN);
        return prescriptions.findByStatusOrderByCreatedAtDesc(PrescriptionStatus.SIGNED);
    }

    public List<Reminder> reminderList(User user) {
        AuthUtils.requireRole(user, Role.PHARMACIST, Role.ADMIN);
        return reminders.findAllByOrderByRemindAtDesc();
    }

    /** 到期提醒自动置为已发送 */
    @Scheduled(fixedDelay = 60_000)
    @Transactional
    public void markDueReminders() {
        List<Reminder> due = reminders.findByStatusAndRemindAtBefore(ReminderStatus.PENDING, LocalDateTime.now());
        for (Reminder r : due) {
            r.setStatus(ReminderStatus.SENT);
            reminders.save(r);
        }
    }
}
