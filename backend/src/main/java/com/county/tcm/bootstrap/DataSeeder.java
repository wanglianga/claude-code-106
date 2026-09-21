package com.county.tcm.bootstrap;

import com.county.tcm.domain.*;
import com.county.tcm.repo.*;
import com.county.tcm.service.ReviewEngine;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** 演示种子数据:7 类角色账号、药材库存与医保目录、十八反十九畏、覆盖全履约状态的处方 */
@Component
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepo;
    private final DoctorRepository doctorRepo;
    private final HerbRepository herbRepo;
    private final CompatibilityRuleRepository ruleRepo;
    private final PrescriptionRepository rxRepo;
    private final ReviewRecordRepository reviewRepo;
    private final DispenseRecordRepository dispenseRepo;
    private final DecoctTaskRepository decoctRepo;
    private final DeliveryTaskRepository deliveryRepo;
    private final IssueRepository issueRepo;
    private final InvoiceRepository invoiceRepo;
    private final MedicationReminderRepository reminderRepo;
    private final FulfillmentEventRepository eventRepo;
    private final PasswordEncoder encoder;

    public DataSeeder(UserRepository userRepo, DoctorRepository doctorRepo, HerbRepository herbRepo,
                      CompatibilityRuleRepository ruleRepo, PrescriptionRepository rxRepo,
                      ReviewRecordRepository reviewRepo, DispenseRecordRepository dispenseRepo,
                      DecoctTaskRepository decoctRepo, DeliveryTaskRepository deliveryRepo,
                      IssueRepository issueRepo, InvoiceRepository invoiceRepo,
                      MedicationReminderRepository reminderRepo, FulfillmentEventRepository eventRepo,
                      PasswordEncoder encoder) {
        this.userRepo = userRepo;
        this.doctorRepo = doctorRepo;
        this.herbRepo = herbRepo;
        this.ruleRepo = ruleRepo;
        this.rxRepo = rxRepo;
        this.reviewRepo = reviewRepo;
        this.dispenseRepo = dispenseRepo;
        this.decoctRepo = decoctRepo;
        this.deliveryRepo = deliveryRepo;
        this.issueRepo = issueRepo;
        this.invoiceRepo = invoiceRepo;
        this.reminderRepo = reminderRepo;
        this.eventRepo = eventRepo;
        this.encoder = encoder;
    }

    private UserAccount u(String username, String name, Role role, String org, String phone) {
        return userRepo.save(new UserAccount(username, encoder.encode("123456"), name, role, org, phone));
    }

    private void herb(String name, String price, String stock, boolean insured, String maxDose) {
        herbRepo.save(new Herb(name, "", new BigDecimal(price), new BigDecimal(stock),
                insured, maxDose == null ? null : new BigDecimal(maxDose)));
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (userRepo.count() > 0) return;

        // ---------- 账号(密码统一 123456) ----------
        UserAccount patient = u("patient", "张伟", Role.PATIENT, null, "13800000001");
        UserAccount patient2 = u("patient2", "李桂兰", Role.PATIENT, null, "13800000002");
        UserAccount clinic = u("clinic", "济世堂账号", Role.CLINIC, "济世堂中医诊所", "13800000010");
        UserAccount clinic2 = u("clinic2", "同仁和账号", Role.CLINIC, "同仁和中医馆", "13800000011");
        UserAccount pharmacist = u("pharmacist", "王药师", Role.PHARMACIST, "同济堂中药房", "13800000020");
        UserAccount decoctor = u("decoctor", "赵师傅", Role.DECOCTOR, "同济堂煎药中心", "13800000030");
        UserAccount courier = u("courier", "刘小军", Role.COURIER, "县城速配一班", "13800000040");
        UserAccount courier2 = u("courier2", "周大勇", Role.COURIER, "县城速配二班", "13800000041");
        UserAccount finance = u("finance", "陈会计", Role.FINANCE, "同济堂中药房", "13800000050");
        UserAccount admin = u("admin", "药房管理员", Role.ADMIN, "同济堂中药房", "13800000060");

        // ---------- 医生 ----------
        Doctor li = doctorRepo.save(new Doctor("李建国", "主治中医师", "济世堂中医诊所", "13900000001", true));
        Doctor sun = doctorRepo.save(new Doctor("孙尚礼", "中医师", "同仁和中医馆", "13900000002", false));

        // ---------- 药材(单价元/克,库存克,医保,单剂上限) ----------
        herb("黄芪", "0.020", "8000", true, "60");
        herb("党参", "0.030", "6000", true, "30");
        herb("白术", "0.025", "6000", true, "30");
        herb("茯苓", "0.015", "9000", true, "30");
        herb("甘草", "0.012", "9000", true, "10");
        herb("当归", "0.050", "5000", true, "20");
        herb("川芎", "0.035", "4000", true, "15");
        herb("白芍", "0.028", "5000", true, "30");
        herb("熟地黄", "0.040", "5000", true, "30");
        herb("桂枝", "0.018", "5000", true, "15");
        herb("柴胡", "0.022", "5000", true, "15");
        herb("黄芩", "0.030", "5000", true, "15");
        herb("麻黄", "0.016", "3000", true, "9");
        herb("石膏", "0.006", "8000", true, "60");
        herb("大黄", "0.025", "4000", true, "15");
        herb("附子", "0.060", "2000", true, "15");
        herb("金银花", "0.080", "50", true, "30");
        herb("忍冬藤", "0.020", "4000", true, "30");
        herb("砂仁", "0.120", "3000", true, "10");
        herb("西洋参", "0.350", "2000", false, "10");
        herb("川乌", "0.090", "1000", true, "9");
        herb("半夏", "0.045", "3000", true, "12");
        herb("海藻", "0.020", "2000", true, "15");
        herb("丁香", "0.070", "1500", true, "6");
        herb("郁金", "0.032", "3000", true, "15");
        herb("防风", "0.030", "4000", true, "15");
        herb("山药", "0.018", "6000", true, "30");
        herb("山茱萸", "0.045", "3000", true, "20");
        // 注:"杏仁"故意不在目录,用于演示目录外药材阻断

        // ---------- 十八反十九畏 ----------
        ruleRepo.save(new CompatibilityRule("甘草", "海藻", "十八反", "甘草反海藻"));
        ruleRepo.save(new CompatibilityRule("甘草", "甘遂", "十八反", "甘草反甘遂"));
        ruleRepo.save(new CompatibilityRule("川乌", "半夏", "十八反", "乌头反半夏"));
        ruleRepo.save(new CompatibilityRule("藜芦", "党参", "十八反", "藜芦反人参/党参类"));
        ruleRepo.save(new CompatibilityRule("丁香", "郁金", "十九畏", "丁香畏郁金"));
        ruleRepo.save(new CompatibilityRule("人参", "五灵脂", "十九畏", "人参畏五灵脂"));

        Map<String, Herb> cat = herbRepo.findAll().stream()
                .collect(Collectors.toMap(Herb::getName, Function.identity()));
        LocalDateTime now = LocalDateTime.now();

        // ---------- A:待审方-常规(患者提交) ----------
        Prescription a = base("RX20260920-0001", patient, patient, "张伟", "13800000001",
                "济世堂中医诊所", li, 7, PickupMethod.DELIVERY, SettlementType.INSURANCE,
                "城关镇建设街 12 号", "无", false, null);
        a.setAddSugar(true);
        addHerb(a, cat, "党参", "15", SpecialMethod.NORMAL, null);
        addHerb(a, cat, "白术", "12", SpecialMethod.NORMAL, null);
        addHerb(a, cat, "茯苓", "15", SpecialMethod.NORMAL, null);
        addHerb(a, cat, "甘草", "6", SpecialMethod.NORMAL, null);
        addHerb(a, cat, "砂仁", "5", SpecialMethod.LATER_ADD, "后下 5 分钟");
        addHerb(a, cat, "金银花", "10", SpecialMethod.NORMAL, "库存不足,审方时建议忍冬藤替代");
        rxRepo.save(a);
        ev(a, Stage.PATIENT, "提交处方", "患者微信端提交,代煎配送到家,要求加糖", patient.getDisplayName());

        // ---------- B:待审方-十八反禁忌(诊所提交,应驳回) ----------
        Prescription b = base("RX20260920-0002", clinic, null, "王秀英", "13800000003",
                "济世堂中医诊所", li, 5, PickupMethod.DELIVERY, SettlementType.SELF_PAY,
                "城关镇老街 8 号", "高血压长期服药", false, null);
        addHerb(b, cat, "甘草", "6", SpecialMethod.NORMAL, null);
        addHerb(b, cat, "海藻", "12", SpecialMethod.NORMAL, null);
        addHerb(b, cat, "黄芪", "20", SpecialMethod.NORMAL, null);
        rxRepo.save(b);
        ev(b, Stage.CLINIC, "批量处方提交", "诊所代提交,处方含甘草与海藻", clinic.getDisplayName());

        // ---------- C:待审方-缺药+剂量+医保限制(夜间急煎) ----------
        Prescription c = base("RX20260920-0003", clinic2, null, "赵德厚", "13800000004",
                "同仁和中医馆", sun, 7, PickupMethod.ELDER_PROXY, SettlementType.INSURANCE,
                "城关镇滨河路 3 栋 201", "糖尿病;对鱼腥草过敏", true, null);
        addHerb(c, cat, "金银花", "10", SpecialMethod.NORMAL, "清热解毒");
        addHerb(c, cat, "麻黄", "12", SpecialMethod.NORMAL, "超常用量,医生已双签");
        addHerb(c, cat, "西洋参", "6", SpecialMethod.INFUSED, "另炖兑服");
        addHerb(c, cat, "杏仁", "10", SpecialMethod.NORMAL, null); // 目录外药材
        rxRepo.save(c);
        ev(c, Stage.CLINIC, "批量处方提交", "夜间急煎;老人代取;医保结算", clinic2.getDisplayName());

        // ---------- D:审方通过待抓药 ----------
        Prescription d = base("RX20260920-0004", patient, patient, "张伟", "13800000001",
                "济世堂中医诊所", li, 5, PickupMethod.DELIVERY, SettlementType.SELF_PAY,
                "城关镇建设街 12 号", "无", false, null);
        addHerb(d, cat, "黄芪", "20", SpecialMethod.NORMAL, null);
        addHerb(d, cat, "白术", "12", SpecialMethod.NORMAL, null);
        addHerb(d, cat, "防风", "10", SpecialMethod.NORMAL, null);
        rxRepo.save(d);
        approveSeed(d, pharmacist, cat, "审方通过,费用已结算");

        // ---------- E:已抓药待煎煮 ----------
        Prescription e = base("RX20260920-0005", clinic, null, "陈老汉", "13800000005",
                "济世堂中医诊所", li, 10, PickupMethod.DELIVERY, SettlementType.INSURANCE,
                "城关镇东关村 21 号", "胃溃疡", false, null);
        addHerb(e, cat, "黄芪", "30", SpecialMethod.NORMAL, null);
        addHerb(e, cat, "党参", "15", SpecialMethod.NORMAL, null);
        addHerb(e, cat, "当归", "10", SpecialMethod.NORMAL, null);
        addHerb(e, cat, "甘草", "6", SpecialMethod.NORMAL, null);
        rxRepo.save(e);
        approveSeed(e, pharmacist, cat, "审方通过");
        dispenseSeed(e, pharmacist);
        DecoctTask te = new DecoctTask();
        te.setPrescription(e);
        te.setQueuedAt(now.minusHours(2));
        te.setStatus(DecoctStatus.QUEUED);
        decoctRepo.save(te);

        // ---------- F:代煎中(已排锅已扫码已开始) ----------
        Prescription f = base("RX20260920-0006", clinic, null, "吴秀兰", "13800000006",
                "济世堂中医诊所", li, 7, PickupMethod.DELIVERY, SettlementType.INSURANCE,
                "城关镇西园小区 5-302", "孕妇禁用活血药已核", false, null);
        addHerb(f, cat, "石膏", "30", SpecialMethod.FIRST_DECOCT, "先煎 20 分钟");
        addHerb(f, cat, "知母", "12", SpecialMethod.NORMAL, null);
        addHerb(f, cat, "砂仁", "5", SpecialMethod.LATER_ADD, "后下");
        rxRepo.save(f);
        approveSeed(f, pharmacist, cat, "审方通过,注意先煎后下");
        dispenseSeed(f, pharmacist);
        DecoctTask tf = new DecoctTask();
        tf.setPrescription(f);
        tf.setQueuedAt(now.minusHours(3));
        tf.setPotNo("G-02");
        tf.setSoakMinutes(30);
        tf.setBoilTimes(2);
        tf.setBagCount(14);
        tf.setDeliveryWave("WAVE-PM-2");
        tf.setDecoctor(decoctor);
        tf.setScannedBagCode("RXNO-" + f.getId());
        tf.setStatus(DecoctStatus.DECOCTING);
        tf.setStartedAt(now.minusMinutes(40));
        decoctRepo.save(tf);
        f.setStatus(RxStatus.DECOCTING);
        ev(f, Stage.DECOCTION, "代煎排产", "锅号 G-02,浸泡 30 分钟,煎煮 2 次,14 袋,WAVE-PM-2", decoctor.getDisplayName());
        ev(f, Stage.DECOCTION, "扫描药包", "药包码 RXNO-" + f.getId() + " 核对一致", decoctor.getDisplayName());
        ev(f, Stage.DECOCTION, "煎煮开始", "锅号 G-02 开始煎煮,先煎石膏已投", decoctor.getDisplayName());

        // ---------- G:煎煮完成待配送(带一条改地址工单) ----------
        Prescription g = base("RX20260920-0007", patient2, patient2, "李桂兰", "13800000002",
                "济世堂中医诊所", li, 5, PickupMethod.ELDER_PROXY, SettlementType.INSURANCE,
                "城关镇幸福院 2 号房", "磺胺类过敏", false, null);
        addHerb(g, cat, "柴胡", "10", SpecialMethod.NORMAL, null);
        addHerb(g, cat, "黄芩", "10", SpecialMethod.NORMAL, null);
        addHerb(g, cat, "白芍", "12", SpecialMethod.NORMAL, null);
        rxRepo.save(g);
        approveSeed(g, pharmacist, cat, "审方通过,老人代取");
        dispenseSeed(g, pharmacist);
        DecoctTask tg = new DecoctTask();
        tg.setPrescription(g);
        tg.setQueuedAt(now.minusHours(5));
        tg.setPotNo("G-01");
        tg.setSoakMinutes(25);
        tg.setBoilTimes(2);
        tg.setBagCount(10);
        tg.setDeliveryWave("WAVE-AM-1");
        tg.setDecoctor(decoctor);
        tg.setScannedBagCode("RXNO-" + g.getId());
        tg.setStatus(DecoctStatus.PACKAGED);
        tg.setStartedAt(now.minusHours(4));
        tg.setEndedAt(now.minusHours(3));
        tg.setPacker("孙丽");
        tg.setReviewer("赵师傅");
        decoctRepo.save(tg);
        g.setStatus(RxStatus.DECOCTED);
        ev(g, Stage.DECOCTION, "煎煮完成包装复核", "锅号 G-01,包装人孙丽,复核人赵师傅,10 袋", decoctor.getDisplayName());
        Issue gIssue = issue(g, IssueType.ADDRESS_CHANGE, Stage.DELIVERY,
                "患者临时改地址:[城关镇幸福院 2 号房] → [城关镇幸福苑养老院 3 号楼]",
                "波次:WAVE-AM-1,配送员:未指派;锅号:G-01", patient2, now.minusHours(2));

        // ---------- H:配送中(已超时,定时任务/签收时会登记超时工单) ----------
        Prescription h = base("RX20260920-0008", clinic, null, "郑老根", "13800000007",
                "济世堂中医诊所", li, 7, PickupMethod.DELIVERY, SettlementType.SELF_PAY,
                "城关镇南关外 66 号", "无", false, null);
        addHerb(h, cat, "熟地黄", "15", SpecialMethod.NORMAL, null);
        addHerb(h, cat, "山药", "15", SpecialMethod.NORMAL, null);
        addHerb(h, cat, "山茱萸", "10", SpecialMethod.NORMAL, null);
        rxRepo.save(h);
        approveSeed(h, pharmacist, cat, "审方通过");
        dispenseSeed(h, pharmacist);
        DecoctTask th = new DecoctTask();
        th.setPrescription(h);
        th.setQueuedAt(now.minusHours(6));
        th.setPotNo("G-03");
        th.setSoakMinutes(30);
        th.setBoilTimes(2);
        th.setBagCount(14);
        th.setDeliveryWave("WAVE-PM-1");
        th.setDecoctor(decoctor);
        th.setStatus(DecoctStatus.PACKAGED);
        th.setStartedAt(now.minusHours(5));
        th.setEndedAt(now.minusHours(4));
        th.setPacker("孙丽");
        th.setReviewer("赵师傅");
        decoctRepo.save(th);
        h.setStatus(RxStatus.DECOCTED);
        DeliveryTask dh = new DeliveryTask();
        dh.setPrescription(h);
        dh.setWave("WAVE-PM-1");
        dh.setCourier(courier);
        dh.setStatus(DeliveryStatus.DELIVERING);
        dh.setAddress(h.getAddress());
        dh.setAssignedAt(now.minusHours(3));
        dh.setOutboundAt(now.minusHours(2).minusMinutes(40));
        dh.setPromisedAt(now.minusMinutes(35));
        deliveryRepo.save(dh);
        h.setStatus(RxStatus.DELIVERING);
        ev(h, Stage.DELIVERY, "配送派单", "波次 WAVE-PM-1,配送员刘小军", pharmacist.getDisplayName());
        ev(h, Stage.DELIVERY, "出库配送", "刘小军携药出库", courier.getDisplayName());

        // ---------- I:已签收(可回访;含历史赔付与补开发票) ----------
        Prescription ii = base("RX20260920-0009", patient, patient, "张伟", "13800000001",
                "济世堂中医诊所", li, 5, PickupMethod.DELIVERY, SettlementType.INSURANCE,
                "城关镇建设街 12 号", "无", false, null);
        addHerb(ii, cat, "党参", "15", SpecialMethod.NORMAL, null);
        addHerb(ii, cat, "茯苓", "15", SpecialMethod.NORMAL, null);
        addHerb(ii, cat, "甘草", "6", SpecialMethod.NORMAL, null);
        rxRepo.save(ii);
        approveSeed(ii, pharmacist, cat, "审方通过");
        dispenseSeed(ii, pharmacist);
        DecoctTask ti = new DecoctTask();
        ti.setPrescription(ii);
        ti.setQueuedAt(now.minusDays(1));
        ti.setPotNo("G-02");
        ti.setSoakMinutes(30);
        ti.setBoilTimes(2);
        ti.setBagCount(10);
        ti.setDeliveryWave("WAVE-AM-2");
        ti.setDecoctor(decoctor);
        ti.setStatus(DecoctStatus.PACKAGED);
        ti.setStartedAt(now.minusDays(1).minusHours(2));
        ti.setEndedAt(now.minusDays(1).minusHours(1));
        ti.setPacker("孙丽");
        ti.setReviewer("赵师傅");
        ti.setMissingBags(1);
        decoctRepo.save(ti);
        ii.setStatus(RxStatus.DECOCTED);
        ev(ii, Stage.DECOCTION, "代煎排产", "锅号 G-02,浸泡 30 分钟,煎煮 2 次,10 袋,WAVE-AM-2", decoctor.getDisplayName());
        ev(ii, Stage.DECOCTION, "扫描药包", "药包码 RXNO-" + ii.getId() + " 核对一致", decoctor.getDisplayName());
        ev(ii, Stage.DECOCTION, "煎煮开始", "锅号 G-02 开始煎煮", decoctor.getDisplayName());
        ev(ii, Stage.DECOCTION, "煎煮完成包装复核",
                "锅号 G-02,包装人孙丽,复核人赵师傅,10 袋,发现漏袋 1 个", decoctor.getDisplayName());
        DeliveryTask di = new DeliveryTask();
        di.setPrescription(ii);
        di.setWave("WAVE-AM-2");
        di.setCourier(courier2);
        di.setStatus(DeliveryStatus.SIGNED);
        di.setAddress(ii.getAddress());
        di.setAssignedAt(now.minusDays(1).minusMinutes(50));
        di.setOutboundAt(now.minusDays(1).minusMinutes(40));
        di.setPromisedAt(now.minusDays(1).plusMinutes(20));
        di.setSignedAt(now.minusDays(1).plusMinutes(10));
        di.setSignedBy("张伟本人");
        deliveryRepo.save(di);
        ii.setStatus(RxStatus.SIGNED);
        ii.setFinishedAt(di.getSignedAt());
        ev(ii, Stage.DELIVERY, "配送派单", "波次 WAVE-AM-2,配送员周大勇,承诺 60 分钟送达", pharmacist.getDisplayName());
        ev(ii, Stage.DELIVERY, "出库配送", "周大勇携药出库", courier2.getDisplayName());
        ev(ii, Stage.DELIVERY, "患者签收", "签收人张伟本人", courier2.getDisplayName());

        Issue bagIssue = issue(ii, IssueType.BAG_DAMAGED, Stage.DECOCTION,
                "患者开箱发现漏袋 1 个,实收 9 袋应 10 袋",
                "锅号:G-02,包装人:孙丽,复核人:赵师傅;波次:WAVE-AM-2,配送员:周大勇",
                patient, now.minusDays(1).plusMinutes(40));
        bagIssue.setStatus(IssueStatus.RESOLVED);
        bagIssue.setResolution("煎药房免费补煎 1 袋并配送,另赔付代金 30 元");
        bagIssue.setCompensation(new BigDecimal("30.00"));
        bagIssue.setResolver(admin);
        bagIssue.setResolvedAt(now.minusDays(1).plusHours(3));
        issueRepo.save(bagIssue);

        Issue invIssue = issue(ii, IssueType.INVOICE, Stage.FINANCE,
                "诊所要求补开发票,抬头:济世堂中医诊所", "财务", clinic, now.minusDays(1).plusHours(2));
        invIssue.setStatus(IssueStatus.RESOLVED);
        invIssue.setResolution("已补开发票");
        invIssue.setResolver(finance);
        invIssue.setResolvedAt(now.minusDays(1).plusHours(4));
        issueRepo.save(invIssue);

        invoice(ii, "张伟", "NORMAL", finance, now.minusDays(1).plusMinutes(20));
        Invoice re = invoice(ii, "济世堂中医诊所", "REISSUED", finance, now.minusDays(1).plusHours(4));
        re.setIssue(invIssue);
        invoiceRepo.save(re);

        MedicationReminder rem = new MedicationReminder();
        rem.setPrescription(ii);
        rem.setPatientName("张伟");
        rem.setPatientPhone("13800000001");
        rem.setTimesPerDay(2);
        rem.setNextRemindAt(now.plusHours(8));
        rem.setState("ACTIVE");
        rem.setCreatedAt(di.getSignedAt());
        rem.setNote("早晚温服,每剂 2 袋");
        reminderRepo.save(rem);

        // ---------- J:自取待取 ----------
        Prescription j = base("RX20260920-0010", patient2, patient2, "李桂兰", "13800000002",
                "同仁和中医馆", sun, 3, PickupMethod.SELF_PICKUP, SettlementType.SELF_PAY,
                "到店自取", "无", false, null);
        addHerb(j, cat, "黄芪", "15", SpecialMethod.NORMAL, null);
        addHerb(j, cat, "白术", "10", SpecialMethod.NORMAL, null);
        rxRepo.save(j);
        approveSeed(j, pharmacist, cat, "审方通过,自费自取");
        dispenseSeed(j, pharmacist);
        j.setStatus(RxStatus.READY_PICKUP);
        ev(j, Stage.PHARMACIST, "抓药完成", "饮片已配好,等待患者到店自取", pharmacist.getDisplayName());
    }

    // ---------- 辅助 ----------
    private Prescription base(String rxNo, UserAccount submitter, UserAccount patientUser,
                              String patientName, String phone, String clinicName, Doctor doctor,
                              int doses, PickupMethod pickup, SettlementType settle,
                              String address, String contra, boolean night, String batchNo) {
        Prescription rx = new Prescription();
        rx.setRxNo(rxNo);
        rx.setSubmitter(submitter);
        rx.setPatientUser(patientUser);
        rx.setPatientName(patientName);
        rx.setPatientPhone(phone);
        rx.setClinicName(clinicName);
        rx.setDoctor(doctor);
        rx.setDoctorName(doctor == null ? null : doctor.getName());
        rx.setDoses(doses);
        rx.setPickupMethod(pickup);
        rx.setSettlementType(settle);
        rx.setAddress(address);
        rx.setContactName(patientName);
        rx.setContactPhone(phone);
        rx.setContraindications(contra);
        rx.setNightUrgent(night);
        rx.setBatchNo(batchNo);
        rx.setSubmittedAt(LocalDateTime.now().minusHours(1));
        return rx;
    }

    private void addHerb(Prescription rx, Map<String, Herb> cat, String name, String dose,
                         SpecialMethod m, String note) {
        PrescriptionHerb ph = new PrescriptionHerb();
        ph.setHerbName(name);
        ph.setHerb(cat.get(name));
        ph.setDosePerPacket(new BigDecimal(dose));
        ph.setSpecialMethod(m);
        ph.setNote(note);
        rx.addHerb(ph);
    }

    private void approveSeed(Prescription rx, UserAccount pharmacist, Map<String, Herb> cat, String remark) {
        var fees = ReviewEngine.fees(rx, cat, new BigDecimal("3.00"), new BigDecimal("6.00"), new BigDecimal("20.00"));
        rx.setHerbAmount(fees.herbAmount());
        rx.setDecoctFee(fees.decoctFee());
        rx.setDeliveryFee(fees.deliveryFee());
        rx.setNightSurcharge(fees.nightSurcharge());
        rx.setTotalAmount(fees.total());
        rx.setInsurancePaid(fees.insurancePaid());
        rx.setSelfPaid(fees.selfPaid());
        rx.setSettled(true);
        rx.setStatus(RxStatus.APPROVED);
        ReviewRecord rv = new ReviewRecord();
        rv.setPrescription(rx);
        rv.setPharmacist(pharmacist);
        rv.setConclusion(ReviewConclusion.PASS);
        rv.setInsuranceNote("");
        rv.setRemark(remark);
        rv.setReviewedAt(LocalDateTime.now().minusMinutes(50));
        reviewRepo.save(rv);
        ev(rx, Stage.PHARMACIST, "药师审方通过", remark + ";总额 " + fees.total()
                + "(医保 " + fees.insurancePaid() + "/自付 " + fees.selfPaid() + ")", pharmacist.getDisplayName());
    }

    private void dispenseSeed(Prescription rx, UserAccount pharmacist) {
        for (PrescriptionHerb ph : rx.getHerbs()) {
            Herb h = ph.getHerb();
            if (h == null) continue;
            h.setStock(h.getStock().subtract(ph.getDosePerPacket().multiply(BigDecimal.valueOf(rx.getDoses()))));
        }
        DispenseRecord dr = new DispenseRecord();
        dr.setPrescription(rx);
        dr.setPharmacist(pharmacist);
        dr.setHerbCount(rx.getHerbs().size());
        dr.setDispensedAt(LocalDateTime.now().minusMinutes(40));
        dispenseRepo.save(dr);
        rx.setStatus(RxStatus.DISPENSED);
        ev(rx, Stage.PHARMACIST, "抓药完成", "已抓 " + rx.getHerbs().size() + " 味", pharmacist.getDisplayName());
    }

    private Issue issue(Prescription rx, IssueType type, Stage owner, String desc,
                        String locate, UserAccount reporter, LocalDateTime at) {
        Issue i = new Issue();
        i.setIssueNo("IS20260920-" + String.format("%04d", issueRepo.count() + 1));
        i.setPrescription(rx);
        i.setType(type);
        i.setOwnerStage(owner);
        i.setDescription(desc);
        i.setLocateInfo(locate);
        i.setReporter(reporter);
        i.setReportedAt(at);
        return issueRepo.save(i);
    }

    private Invoice invoice(Prescription rx, String title, String kind, UserAccount finance, LocalDateTime at) {
        Invoice inv = new Invoice();
        inv.setInvoiceNo("INV20260920-" + String.format("%04d", invoiceRepo.count() + 1));
        inv.setPrescription(rx);
        inv.setTitle(title);
        inv.setAmount(rx.getTotalAmount());
        inv.setKind(kind);
        inv.setFinanceUser(finance);
        inv.setIssuedAt(at);
        return invoiceRepo.save(inv);
    }

    private void ev(Prescription rx, Stage stage, String action, String detail, String operator) {
        eventRepo.save(new FulfillmentEvent(rx, stage, action, detail, operator));
    }
}
