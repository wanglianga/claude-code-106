package com.county.tcm.service;

import com.county.tcm.domain.*;
import com.county.tcm.repo.*;
import com.county.tcm.web.dto.Dtos;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class DecoctService {

    private final DecoctTaskRepository decoctRepo;
    private final PrescriptionRepository rxRepo;
    private final IssueRepository issueRepo;
    private final DomainSupport support;

    public DecoctService(DecoctTaskRepository decoctRepo, PrescriptionRepository rxRepo,
                         IssueRepository issueRepo, DomainSupport support) {
        this.decoctRepo = decoctRepo;
        this.rxRepo = rxRepo;
        this.issueRepo = issueRepo;
        this.support = support;
    }

    @Transactional(readOnly = true)
    public List<java.util.Map<String, Object>> queueViews() {
        return queue().stream().map(support::decoctView).toList();
    }

    @Transactional(readOnly = true)
    public List<DecoctTask> queue() {
        return decoctRepo.findAll().stream()
                .sorted((a, b) -> {
                    boolean urgentA = a.getPrescription().isNightUrgent();
                    boolean urgentB = b.getPrescription().isNightUrgent();
                    if (urgentA != urgentB) return urgentA ? -1 : 1;
                    return a.getId().compareTo(b.getId());
                })
                .toList();
    }

    /** 煎药员安排任务:锅号、浸泡时间、煎煮次数、包装袋数、配送波次 */
    @Transactional
    public DecoctTask schedule(Long rxId, Dtos.DecoctScheduleRequest req, UserAccount decoctor) {
        Prescription rx = support.requireRx(rxId);
        if (rx.getStatus() != RxStatus.DISPENSED) {
            throw DomainSupport.conflict("仅[已抓药待煎煮]状态可安排代煎");
        }
        DecoctTask task = decoctRepo.findByPrescriptionId(rxId)
                .orElseGet(() -> {
                    DecoctTask t = new DecoctTask();
                    t.setPrescription(rx);
                    t.setQueuedAt(LocalDateTime.now());
                    return t;
                });
        task.setPotNo(req.potNo());
        task.setSoakMinutes(req.soakMinutes());
        task.setBoilTimes(req.boilTimes());
        task.setBagCount(req.bagCount());
        task.setDeliveryWave(req.deliveryWave());
        task.setDecoctor(decoctor);
        task.setStatus(DecoctStatus.QUEUED);
        decoctRepo.save(task);
        support.event(rx, Stage.DECOCTION, "代煎排产",
                "锅号 " + req.potNo() + ",浸泡 " + req.soakMinutes() + " 分钟,煎煮 "
                        + req.boilTimes() + " 次,包装袋 " + req.bagCount() + " 个,波次 "
                        + (req.deliveryWave() == null ? "待定" : req.deliveryWave())
                        + (rx.isNightUrgent() ? ",【夜间急煎优先】" : ""),
                decoctor.getDisplayName());
        return task;
    }

    /** 扫描药包(药包条码约定为 RXNO-RX编号) */
    @Transactional
    public DecoctTask scan(Long rxId, Dtos.ScanRequest req, UserAccount decoctor) {
        Prescription rx = support.requireRx(rxId);
        DecoctTask task = mustTask(rxId);
        String expected = "RXNO-" + rx.getId();
        if (!expected.equals(req.bagCode()) && !rx.getRxNo().equals(req.bagCode())) {
            throw DomainSupport.badRequest("药包条码不匹配,扫描值[" + req.bagCode()
                    + "]不属于处方 " + rx.getRxNo() + "(应收码 " + expected + ")");
        }
        task.setScannedBagCode(req.bagCode());
        task.setDecoctor(decoctor);
        decoctRepo.save(task);
        support.event(rx, Stage.DECOCTION, "扫描药包", "药包码 " + req.bagCode() + " 核对一致", decoctor.getDisplayName());
        return task;
    }

    @Transactional
    public DecoctTask start(Long rxId, UserAccount decoctor) {
        Prescription rx = support.requireRx(rxId);
        DecoctTask task = mustTask(rxId);
        if (task.getPotNo() == null) {
            throw DomainSupport.conflict("请先安排锅号/波次再开始煎煮");
        }
        task.setStatus(DecoctStatus.DECOCTING);
        task.setStartedAt(LocalDateTime.now());
        task.setDecoctor(decoctor);
        decoctRepo.save(task);
        rx.setStatus(RxStatus.DECOCTING);
        support.event(rx, Stage.DECOCTION, "煎煮开始",
                "锅号 " + task.getPotNo() + " 开始煎煮", decoctor.getDisplayName());
        return task;
    }

    /** 煎煮结束:异常气味、漏袋、破损、包装人、复核人;自动登记漏袋/破损异常工单 */
    @Transactional
    public DecoctTask end(Long rxId, Dtos.DecoctEndRequest req, UserAccount decoctor) {
        Prescription rx = support.requireRx(rxId);
        DecoctTask task = mustTask(rxId);
        if (task.getStatus() != DecoctStatus.DECOCTING) {
            throw DomainSupport.conflict("任务尚未在煎煮中,无法结束");
        }
        task.setStatus(DecoctStatus.PACKAGED);
        task.setEndedAt(LocalDateTime.now());
        task.setAbnormalSmell(req.abnormalSmell());
        task.setMissingBags(req.missingBags() == null ? 0 : req.missingBags());
        task.setDamagedBags(req.damagedBags() == null ? 0 : req.damagedBags());
        task.setPacker(req.packer());
        task.setReviewer(req.reviewer());
        task.setRemark(req.remark());
        decoctRepo.save(task);

        support.event(rx, Stage.DECOCTION, "煎煮完成包装复核",
                "锅号 " + task.getPotNo() + ",包装人 " + req.packer() + ",复核人 " + req.reviewer()
                        + ",袋数 " + task.getBagCount() + ",漏袋 " + task.getMissingBags()
                        + ",破损 " + task.getDamagedBags()
                        + (req.abnormalSmell() == null || req.abnormalSmell().isBlank()
                        ? "" : ",异常气味:" + req.abnormalSmell()),
                decoctor.getDisplayName());

        if (task.getMissingBags() > 0 || task.getDamagedBags() > 0) {
            Issue issue = new Issue();
            issue.setIssueNo(support.nextIssueNo(issueRepo.count()));
            issue.setPrescription(rx);
            issue.setType(IssueType.BAG_DAMAGED);
            issue.setOwnerStage(Stage.DECOCTION);
            issue.setDescription("煎煮结束发现漏袋 " + task.getMissingBags() + " 个、包装袋破损 "
                    + task.getDamagedBags() + " 个");
            issue.setLocateInfo("锅号:" + task.getPotNo() + ",包装人:" + task.getPacker()
                    + ",复核人:" + task.getReviewer());
            issue.setReporter(decoctor);
            issue.setReportedAt(LocalDateTime.now());
            issueRepo.save(issue);
            support.event(rx, Stage.DECOCTION, "登记异常工单",
                    issue.getIssueNo() + " 漏袋/破损,定位:" + issue.getLocateInfo(), decoctor.getDisplayName());
        }

        rx.setStatus(RxStatus.DECOCTED);
        return task;
    }

    private DecoctTask mustTask(Long rxId) {
        return decoctRepo.findByPrescriptionId(rxId)
                .orElseThrow(() -> DomainSupport.conflict("该处方暂无代煎任务"));
    }
}
