package com.county.tcm.service;

import com.county.tcm.domain.*;
import com.county.tcm.repo.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

/** 药房管理者复盘:按医生、药味、锅号、配送员统计异常;诊所合作评分 */
@Service
public class AnalyticsService {

    private final PrescriptionRepository rxRepo;
    private final DecoctTaskRepository decoctRepo;
    private final DeliveryTaskRepository deliveryRepo;
    private final FollowUpRepository followRepo;
    private final IssueRepository issueRepo;

    public AnalyticsService(PrescriptionRepository rxRepo, DecoctTaskRepository decoctRepo,
                            DeliveryTaskRepository deliveryRepo, FollowUpRepository followRepo,
                            IssueRepository issueRepo) {
        this.rxRepo = rxRepo;
        this.decoctRepo = decoctRepo;
        this.deliveryRepo = deliveryRepo;
        this.followRepo = followRepo;
        this.issueRepo = issueRepo;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> overview() {
        List<Prescription> all = rxRepo.findAll();
        Map<String, Long> statusCount = all.stream()
                .collect(Collectors.groupingBy(r -> r.getStatus().name(), Collectors.counting()));
        BigDecimal revenue = all.stream().filter(Prescription::isSettled)
                .map(r -> r.getSelfPaid() == null ? BigDecimal.ZERO : r.getSelfPaid())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal insurance = all.stream().filter(Prescription::isSettled)
                .map(r -> r.getInsurancePaid() == null ? BigDecimal.ZERO : r.getInsurancePaid())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal compensation = issueRepo.findAll().stream()
                .map(i -> i.getCompensation() == null ? BigDecimal.ZERO : i.getCompensation())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        List<FollowUp> fus = followRepo.findAll();
        double avgScore = fus.stream().filter(f -> f.getSatisfactionScore() != null)
                .collect(Collectors.averagingInt(FollowUp::getSatisfactionScore));
        long discomfort = fus.stream().filter(f -> f.getResult() == FollowUpResult.DISCOMFORT).count();

        Map<String, Object> m = new LinkedHashMap<>();
        m.put("rxTotal", all.size());
        m.put("statusCount", statusCount);
        m.put("openIssues", issueRepo.countByStatus(IssueStatus.OPEN)
                + issueRepo.countByStatus(IssueStatus.PROCESSING));
        m.put("selfPayRevenue", revenue);
        m.put("insuranceRevenue", insurance);
        m.put("compensation", compensation);
        m.put("avgSatisfaction", round(avgScore));
        m.put("discomfortCount", discomfort);
        return m;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> byDoctor() {
        List<Issue> issues = issueRepo.findAll();
        List<FollowUp> fus = followRepo.findAll();
        Map<String, List<Prescription>> grouped = rxRepo.findAll().stream()
                .filter(r -> r.getDoctorName() != null)
                .collect(Collectors.groupingBy(Prescription::getDoctorName, LinkedHashMap::new, Collectors.toList()));
        List<Map<String, Object>> out = new ArrayList<>();
        for (var e : grouped.entrySet()) {
            Set<Long> rxIds = e.getValue().stream().map(Prescription::getId).collect(Collectors.toSet());
            long issueCnt = issues.stream().filter(i -> rxIds.contains(i.getPrescription().getId())).count();
            double avg = fus.stream()
                    .filter(f -> rxIds.contains(f.getPrescription().getId()) && f.getSatisfactionScore() != null)
                    .collect(Collectors.averagingInt(FollowUp::getSatisfactionScore));
            long discomfort = fus.stream()
                    .filter(f -> rxIds.contains(f.getPrescription().getId())
                            && f.getResult() == FollowUpResult.DISCOMFORT).count();
            long night = e.getValue().stream().filter(Prescription::isNightUrgent).count();
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("doctorName", e.getKey());
            row.put("rxCount", e.getValue().size());
            row.put("issueCount", issueCnt);
            row.put("discomfortCount", discomfort);
            row.put("nightUrgentCount", night);
            row.put("avgSatisfaction", round(avg));
            row.put("cooperationScore", doctorScore(e.getValue().size(), issueCnt, avg));
            out.add(row);
        }
        return out;
    }

    /** 药味复盘:出现次数、替代次数、特殊煎法遗漏关联 */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> byHerb() {
        List<Issue> issues = issueRepo.findAll();
        Map<String, long[]> agg = new LinkedHashMap<>(); // [出现次数, 替代次数]
        for (Prescription rx : rxRepo.findAll()) {
            for (PrescriptionHerb ph : rx.getHerbs()) {
                long[] a = agg.computeIfAbsent(ph.getHerbName(), k -> new long[3]);
                a[0]++;
                if (ph.getSubstitutedFrom() != null) a[1]++;
            }
        }
        List<Map<String, Object>> out = new ArrayList<>();
        for (var e : agg.entrySet()) {
            String herb = e.getKey();
            long specialMissed = issues.stream()
                    .filter(i -> i.getType() == IssueType.SPECIAL_MISSED
                            && i.getDescription() != null && i.getDescription().contains(herb))
                    .count();
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("herbName", herb);
            row.put("usedCount", e.getValue()[0]);
            row.put("substitutedCount", e.getValue()[1]);
            row.put("specialMissedCount", specialMissed);
            out.add(row);
        }
        out.sort((a, b) -> Long.compare((long) b.get("usedCount"), (long) a.get("usedCount")));
        return out;
    }

    /** 锅号复盘:任务数、漏袋、破损、异常气味、超时关联 */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> byPot() {
        Map<String, List<DecoctTask>> grouped = decoctRepo.findAll().stream()
                .filter(t -> t.getPotNo() != null)
                .collect(Collectors.groupingBy(DecoctTask::getPotNo, LinkedHashMap::new, Collectors.toList()));
        List<Map<String, Object>> out = new ArrayList<>();
        for (var e : grouped.entrySet()) {
            Set<Long> rxIds = e.getValue().stream().map(t -> t.getPrescription().getId()).collect(Collectors.toSet());
            long timeout = deliveryRepo.findAll().stream()
                    .filter(t -> rxIds.contains(t.getPrescription().getId())
                            && t.getTimeoutMinutes() != null).count();
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("potNo", e.getKey());
            row.put("taskCount", e.getValue().size());
            row.put("missingBags", e.getValue().stream().mapToInt(t -> nz(t.getMissingBags())).sum());
            row.put("damagedBags", e.getValue().stream().mapToInt(t -> nz(t.getDamagedBags())).sum());
            row.put("abnormalSmellCount", e.getValue().stream()
                    .filter(t -> t.getAbnormalSmell() != null && !t.getAbnormalSmell().isBlank()).count());
            row.put("relatedTimeout", timeout);
            out.add(row);
        }
        return out;
    }

    /** 配送员复盘:单量、超时次数、平均超时分钟 */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> byCourier() {
        Map<UserAccount, List<DeliveryTask>> grouped = deliveryRepo.findAll().stream()
                .filter(t -> t.getCourier() != null)
                .collect(Collectors.groupingBy(DeliveryTask::getCourier, LinkedHashMap::new, Collectors.toList()));
        List<Map<String, Object>> out = new ArrayList<>();
        for (var e : grouped.entrySet()) {
            long timeout = e.getValue().stream().filter(t -> t.getTimeoutMinutes() != null).count();
            double avgLate = e.getValue().stream().filter(t -> t.getTimeoutMinutes() != null)
                    .collect(Collectors.averagingInt(DeliveryTask::getTimeoutMinutes));
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("courierName", e.getKey().getDisplayName());
            row.put("phone", e.getKey().getPhone());
            row.put("deliveryCount", e.getValue().size());
            row.put("timeoutCount", timeout);
            row.put("avgTimeoutMinutes", round(avgLate));
            out.add(row);
        }
        return out;
    }

    /** 诊所合作评分:处方量、异常率、不适反馈、满意度综合 0-100 */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> clinicScores() {
        List<Issue> issues = issueRepo.findAll();
        List<FollowUp> fus = followRepo.findAll();
        Map<String, List<Prescription>> grouped = rxRepo.findAll().stream()
                .filter(r -> r.getClinicName() != null)
                .collect(Collectors.groupingBy(Prescription::getClinicName, LinkedHashMap::new, Collectors.toList()));
        List<Map<String, Object>> out = new ArrayList<>();
        for (var e : grouped.entrySet()) {
            Set<Long> rxIds = e.getValue().stream().map(Prescription::getId).collect(Collectors.toSet());
            long issueCnt = issues.stream().filter(i -> rxIds.contains(i.getPrescription().getId())).count();
            long openCnt = issues.stream().filter(i -> rxIds.contains(i.getPrescription().getId())
                    && i.getStatus() != IssueStatus.RESOLVED).count();
            long discomfort = fus.stream().filter(f -> rxIds.contains(f.getPrescription().getId())
                    && f.getResult() == FollowUpResult.DISCOMFORT).count();
            double avg = fus.stream().filter(f -> rxIds.contains(f.getPrescription().getId())
                    && f.getSatisfactionScore() != null)
                    .collect(Collectors.averagingInt(FollowUp::getSatisfactionScore));
            double issueRate = e.getValue().isEmpty() ? 0 : (double) issueCnt / e.getValue().size();
            double score = 100 - issueRate * 20 - discomfort * 8 + (avg - 3) * 8 - openCnt * 3;
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("clinicName", e.getKey());
            row.put("rxCount", e.getValue().size());
            row.put("issueCount", issueCnt);
            row.put("openIssueCount", openCnt);
            row.put("discomfortCount", discomfort);
            row.put("avgSatisfaction", round(avg));
            row.put("cooperationScore", Math.max(0, Math.min(100, round(score))));
            out.add(row);
        }
        return out;
    }

    private static int nz(Integer i) { return i == null ? 0 : i; }
    private static double round(double v) {
        return BigDecimal.valueOf(v).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    private double doctorScore(int rxCount, long issues, double avgSatisfaction) {
        double issueRate = rxCount == 0 ? 0 : (double) issues / rxCount;
        double score = 100 - issueRate * 25 + (avgSatisfaction == 0 ? 0 : (avgSatisfaction - 3) * 6);
        return Math.max(0, Math.min(100, round(score)));
    }
}
