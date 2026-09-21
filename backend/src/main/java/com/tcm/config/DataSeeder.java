package com.tcm.config;

import com.tcm.model.*;
import com.tcm.repository.*;
import com.tcm.service.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/** 首次启动初始化基础数据与演示数据 */
@Component
public class DataSeeder implements CommandLineRunner {

    private final UserRepository users;
    private final ClinicRepository clinics;
    private final DoctorRepository doctors;
    private final HerbRepository herbs;
    private final HerbConflictRepository conflicts;
    private final DecoctTaskRepository decoctTasks;
    private final DeliveryRepository deliveries;
    private final PasswordEncoder encoder;
    private final PrescriptionService prescriptionService;
    private final ReviewService reviewService;
    private final DecoctService decoctService;
    private final DeliveryService deliveryService;
    private final FollowUpService followUpService;
    private final ExceptionService exceptionService;

    public DataSeeder(UserRepository users, ClinicRepository clinics, DoctorRepository doctors,
                      HerbRepository herbs, HerbConflictRepository conflicts,
                      DecoctTaskRepository decoctTasks, DeliveryRepository deliveries,
                      PasswordEncoder encoder, PrescriptionService prescriptionService,
                      ReviewService reviewService, DecoctService decoctService,
                      DeliveryService deliveryService, FollowUpService followUpService,
                      ExceptionService exceptionService) {
        this.users = users;
        this.clinics = clinics;
        this.doctors = doctors;
        this.herbs = herbs;
        this.conflicts = conflicts;
        this.decoctTasks = decoctTasks;
        this.deliveries = deliveries;
        this.encoder = encoder;
        this.prescriptionService = prescriptionService;
        this.reviewService = reviewService;
        this.decoctService = decoctService;
        this.deliveryService = deliveryService;
        this.followUpService = followUpService;
        this.exceptionService = exceptionService;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (users.count() > 0) {
            return;
        }
        seedBase();
        seedDemo();
    }

    // ---------- 基础数据 ----------

    private User admin, pharmacist, decocter, courier, courier2, finance, clinic1User, clinic2User, patient1, patient2;
    private Clinic clinic1, clinic2, clinic3;
    private Doctor docChen, docLiu, docZhou, docWu;

    private void seedBase() {
        clinic1 = clinic("康民诊所", "陈立", "0558-6201001", "城关镇人民路88号", 96.0);
        clinic2 = clinic("仁和社区诊所", "周远", "0558-6201002", "仁和街道春晖路6号", 100.0);
        clinic3 = clinic("李桥中医馆", "吴一针", "0558-6201003", "李桥镇老街21号", 88.0);

        docChen = doctor("陈立", "主治医师", clinic1);
        docLiu = doctor("刘芳", "执业医师", clinic1);
        docZhou = doctor("周远", "主治医师", clinic2);
        docWu = doctor("吴一针", "主任中医师", clinic3);

        admin = user("admin", "admin123", "药房管理员", "0558-6200000", Role.ADMIN, null);
        pharmacist = user("pharmacist", "123456", "王药师", "13900000001", Role.PHARMACIST, null);
        decocter = user("decocter", "123456", "李煎药", "13900000002", Role.DECOCTER, null);
        courier = user("courier", "123456", "赵配送", "13900000003", Role.COURIER, null);
        courier2 = user("courier2", "123456", "孙配送", "13900000004", Role.COURIER, null);
        finance = user("finance", "123456", "钱会计", "13900000005", Role.FINANCE, null);
        clinic1User = user("clinic1", "123456", "康民诊所", "0558-6201001", Role.CLINIC, clinic1);
        clinic2User = user("clinic2", "123456", "仁和社区诊所", "0558-6201002", Role.CLINIC, clinic2);
        patient1 = user("patient1", "123456", "张建国", "13800000001", Role.PATIENT, null);
        patient2 = user("patient2", "123456", "王秀兰", "13800000002", Role.PATIENT, null);

        seedHerbs();
        seedConflicts();
    }

    private Clinic clinic(String name, String contact, String phone, String addr, double score) {
        Clinic c = new Clinic();
        c.setName(name);
        c.setContactPerson(contact);
        c.setPhone(phone);
        c.setAddress(addr);
        c.setCooperationScore(score);
        return clinics.save(c);
    }

    private Doctor doctor(String name, String title, Clinic clinic) {
        Doctor d = new Doctor();
        d.setName(name);
        d.setTitle(title);
        d.setClinic(clinic);
        return doctors.save(d);
    }

    private User user(String username, String pwd, String name, String phone, Role role, Clinic clinic) {
        User u = new User();
        u.setUsername(username);
        u.setPassword(encoder.encode(pwd));
        u.setName(name);
        u.setPhone(phone);
        u.setRole(role);
        u.setClinic(clinic);
        return users.save(u);
    }

    private void herb(String code, String name, String category, double price, int stock, int maxDose, boolean insurance) {
        Herb h = new Herb();
        h.setCode(code);
        h.setName(name);
        h.setCategory(category);
        h.setUnitPrice(BigDecimal.valueOf(price));
        h.setStockGram(stock);
        h.setMaxDailyDoseGram(maxDose);
        h.setInInsurance(insurance);
        herbs.save(h);
    }

    private void seedHerbs() {
        herb("H001", "黄芪", "补气药", 0.08, 5000, 30, true);
        herb("H002", "党参", "补气药", 0.10, 4000, 30, true);
        herb("H003", "白术", "补气药", 0.09, 4000, 30, true);
        herb("H004", "茯苓", "利水渗湿药", 0.07, 5000, 30, true);
        herb("H005", "当归", "补血药", 0.12, 4000, 15, true);
        herb("H006", "川芎", "活血化瘀药", 0.10, 3000, 10, true);
        herb("H007", "白芍", "补血药", 0.09, 4000, 30, true);
        herb("H008", "熟地黄", "补血药", 0.08, 4000, 30, true);
        herb("H009", "桂枝", "解表药", 0.06, 3000, 10, true);
        herb("H010", "柴胡", "解表药", 0.10, 3000, 10, true);
        herb("H011", "黄芩", "清热药", 0.08, 3000, 15, true);
        herb("H012", "陈皮", "理气药", 0.05, 3000, 10, true);
        herb("H013", "金银花", "清热药", 0.15, 3000, 20, true);
        herb("H014", "连翘", "清热药", 0.12, 3000, 15, true);
        herb("H015", "薄荷", "解表药", 0.06, 2000, 6, true);
        herb("H016", "桔梗", "化痰止咳药", 0.08, 3000, 10, true);
        herb("H017", "苦杏仁", "化痰止咳药", 0.09, 2000, 10, true);
        herb("H018", "麦冬", "补阴药", 0.12, 3000, 20, true);
        herb("H019", "五味子", "收涩药", 0.15, 2000, 10, true);
        herb("H020", "丹参", "活血化瘀药", 0.10, 3000, 20, true);
        herb("H021", "红花", "活血化瘀药", 0.20, 2000, 10, true);
        herb("H022", "桃仁", "活血化瘀药", 0.12, 2000, 10, true);
        herb("H023", "生姜", "解表药", 0.03, 5000, 15, true);
        herb("H024", "大枣", "补气药", 0.04, 5000, 30, true);
        herb("H025", "炙甘草", "补气药", 0.07, 4000, 10, true);
        herb("H026", "甘草", "补气药", 0.06, 4000, 10, true);
        herb("H027", "半夏", "化痰药", 0.15, 2000, 10, true);
        herb("H028", "瓜蒌", "化痰药", 0.10, 2000, 20, true);
        herb("H029", "川贝母", "化痰药", 0.80, 500, 10, true);
        herb("H030", "白及", "止血药", 0.30, 1000, 15, true);
        herb("H031", "细辛", "解表药", 0.15, 1000, 3, true);
        herb("H032", "郁金", "活血化瘀药", 0.10, 2000, 15, true);
        herb("H033", "肉桂", "温里药", 0.12, 2000, 5, true);
        herb("H034", "附子", "温里药", 0.20, 2000, 15, true);
        herb("H035", "海藻", "化痰药", 0.08, 1500, 15, true);
        herb("H036", "白蔹", "清热药", 0.20, 1000, 15, true);
        // 医保目录外(自费)药材
        herb("H101", "人参", "补气药", 2.50, 500, 10, false);
        herb("H102", "甘遂", "峻下逐水药", 0.30, 0, 3, false);
        herb("H103", "藜芦", "涌吐药", 0.25, 500, 3, false);
        herb("H104", "硫黄", "攻毒药", 0.10, 500, 3, false);
        herb("H105", "朴硝", "泻下药", 0.05, 1000, 10, false);
        herb("H106", "巴豆", "峻下逐水药", 0.40, 200, 1, false);
        herb("H107", "牵牛子", "泻下药", 0.08, 1000, 6, false);
        herb("H108", "五灵脂", "活血化瘀药", 0.15, 1000, 10, false);
        herb("H109", "赤石脂", "收涩药", 0.06, 1000, 15, false);
        herb("H110", "丁香", "温里药", 0.15, 1000, 5, false);
    }

    private void conflict(String a, String b, String type, String desc) {
        HerbConflict c = new HerbConflict();
        c.setHerbA(herbs.findByName(a).orElseThrow());
        c.setHerbB(herbs.findByName(b).orElseThrow());
        c.setConflictType(type);
        c.setDescription(desc);
        conflicts.save(c);
    }

    private void seedConflicts() {
        // 十八反
        conflict("甘草", "甘遂", "十八反", "甘草反甘遂，同用增毒性");
        conflict("甘草", "海藻", "十八反", "甘草反海藻");
        conflict("附子", "半夏", "十八反", "乌头类反半夏");
        conflict("附子", "瓜蒌", "十八反", "乌头类反瓜蒌");
        conflict("附子", "川贝母", "十八反", "乌头类反贝母");
        conflict("附子", "白及", "十八反", "乌头类反白及");
        conflict("附子", "白蔹", "十八反", "乌头类反白蔹");
        conflict("藜芦", "人参", "十八反", "藜芦反人参");
        conflict("藜芦", "丹参", "十八反", "藜芦反丹参");
        conflict("藜芦", "细辛", "十八反", "藜芦反细辛");
        conflict("藜芦", "白芍", "十八反", "藜芦反芍药");
        // 十九畏
        conflict("丁香", "郁金", "十九畏", "丁香莫与郁金见");
        conflict("肉桂", "赤石脂", "十九畏", "官桂善能调冷气，若逢石脂便相欺");
        conflict("人参", "五灵脂", "十九畏", "人参最怕五灵脂");
        conflict("硫黄", "朴硝", "十九畏", "硫黄原是火中精，朴硝一见便相争");
        conflict("巴豆", "牵牛子", "十九畏", "巴豆性烈最为上，偏与牵牛不顺情");
    }

    // ---------- 演示处方 ----------

    private PrescriptionService.ItemReq it(String name, int g) {
        return it(name, g, SpecialHandling.NONE);
    }

    private PrescriptionService.ItemReq it(String name, int g, SpecialHandling sh) {
        Herb h = herbs.findByName(name).orElseThrow(() -> new IllegalStateException("药材未初始化: " + name));
        return new PrescriptionService.ItemReq(h.getId(), g, sh);
    }

    private Prescription createRx(User submitter, String patientName, String phone, Integer age, String gender,
                                  String contra, Long doctorId, int doses, String special, boolean sugar,
                                  PickupMethod pm, String addr, String proxyName, String proxyPhone,
                                  boolean urgent, String batchNo, List<PrescriptionService.ItemReq> items) {
        return prescriptionService.create(new PrescriptionService.CreateReq(
                patientName, phone, age, gender, contra, null, doctorId, doses, special, sugar,
                pm, addr, proxyName, proxyPhone, urgent, batchNo, items), submitter);
    }

    private void pass(Prescription p, SettlementType st, String note) {
        reviewService.review(p.getId(), new ReviewService.ReviewReq(ReviewConclusion.PASS, st, note, null), pharmacist);
    }

    private void decoctAll(Prescription p, boolean abnormal, int leaked, boolean specialConfirmed) {
        prescriptionService.dispense(p.getId(), pharmacist);
        DecoctTask t = decoctTasks.findByPrescriptionId(p.getId()).orElseThrow();
        decoctService.scan(t.getId(), p.getRxNo(), decocter);
        decoctService.start(t.getId(), decocter);
        decoctService.finish(t.getId(), new DecoctService.FinishReq(
                abnormal, abnormal ? "有焦糊味" : null, leaked, "王药师", specialConfirmed, null), decocter);
    }

    private void deliverAll(Prescription p) {
        Delivery d = deliveries.findByPrescriptionId(p.getId()).orElseThrow();
        deliveryService.dispatch(d.getId(), null, courier);
        deliveryService.sign(d.getId(), p.getPatientName(), courier);
    }

    private void seedDemo() {
        String batch = "B" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyMMdd")) + "-01";

        // 1) 待审方：含十八反(甘草-甘遂) + 甘遂缺药，患者直投
        createRx(patient1, "张建国", "13800000001", 67, "男", "高血压病史，忌大量甘草",
                null, 3, null, false, PickupMethod.DELIVERY, "城关镇建设路12号院3单元", null, null,
                false, null, List.of(it("甘草", 6), it("甘遂", 3), it("茯苓", 10)));

        // 2-4) 待审方：康民诊所批量处方(同批次3张)
        createRx(clinic1User, "刘桂英", "13800001001", 58, "女", "糖尿病史",
                docChen.getId(), 5, null, true, PickupMethod.DELIVERY, "城关镇人民路幸福小区2栋", null, null,
                false, batch, List.of(it("黄芪", 15), it("当归", 10), it("党参", 12), it("白术", 10), it("茯苓", 10), it("炙甘草", 6)));
        createRx(clinic1User, "赵德柱", "13800001002", 45, "男", null,
                docLiu.getId(), 7, null, false, PickupMethod.SELF_PICKUP, null, null, null,
                false, batch, List.of(it("柴胡", 10), it("黄芩", 9), it("半夏", 9), it("白芍", 12), it("陈皮", 6), it("桔梗", 6)));
        createRx(clinic1User, "孙丽", "13800001003", 32, "女", "哺乳期",
                docChen.getId(), 3, "薄荷后下", false, PickupMethod.DELIVERY, "城关镇文昌街15号", null, null,
                false, batch, List.of(it("金银花", 12), it("连翘", 10), it("薄荷", 6, SpecialHandling.LATER_ADD), it("桔梗", 6)));

        // 5) 待审方：夜间急煎(附子先煎)
        createRx(patient2, "王秀兰", "13800000002", 71, "女", "心功能不全",
                null, 7, "附子先煎40分钟，余药后下", false, PickupMethod.SELF_PICKUP, null, null, null,
                true, null, List.of(it("附子", 10, SpecialHandling.PRE_DECOCT), it("桂枝", 10), it("白术", 12), it("炙甘草", 6)));

        // 6) 审方通过 → 代煎排队
        Prescription rx6 = createRx(clinic2User, "吴国强", "13800002001", 62, "男", null,
                docZhou.getId(), 5, null, false, PickupMethod.DELIVERY, "仁和街道春晖路9号", null, null,
                false, null, List.of(it("党参", 15), it("麦冬", 10), it("五味子", 6)));
        pass(rx6, SettlementType.MEDICAL_INSURANCE, "生脉饮加减，配伍正常");
        prescriptionService.dispense(rx6.getId(), pharmacist);

        // 7) 煎药中
        Prescription rx7 = createRx(clinic2User, "郑海燕", "13800002002", 50, "女", null,
                docZhou.getId(), 7, null, false, PickupMethod.DELIVERY, "仁和街道滨河路3号", null, null,
                false, null, List.of(it("丹参", 15), it("红花", 6), it("桃仁", 10), it("川芎", 10)));
        pass(rx7, SettlementType.SELF_PAY, "活血化瘀方，自费结算");
        prescriptionService.dispense(rx7.getId(), pharmacist);
        DecoctTask t7 = decoctTasks.findByPrescriptionId(rx7.getId()).orElseThrow();
        decoctService.scan(t7.getId(), rx7.getRxNo(), decocter);
        decoctService.start(t7.getId(), decocter);

        // 8) 配送中(煎药漏袋1袋 → 自动登记包装破损异常)
        Prescription rx8 = createRx(clinic1User, "马长顺", "13800001004", 66, "男", null,
                docChen.getId(), 5, null, false, PickupMethod.DELIVERY, "城关镇解放路27号", null, null,
                false, null, List.of(it("茯苓", 15), it("白术", 12), it("党参", 12), it("炙甘草", 6), it("陈皮", 6)));
        pass(rx8, SettlementType.MEDICAL_INSURANCE, "四君子汤加减");
        decoctAll(rx8, false, 1, true);
        Delivery d8 = deliveries.findByPrescriptionId(rx8.getId()).orElseThrow();
        deliveryService.dispatch(d8.getId(), null, courier);

        // 9) 已签收待回访；诊所要求补开发票
        Prescription rx9 = createRx(clinic1User, "高玉梅", "13800001005", 55, "女", null,
                docChen.getId(), 7, null, true, PickupMethod.DELIVERY, "城关镇建设路8号", null, null,
                false, null, List.of(it("黄芪", 20), it("当归", 12), it("熟地黄", 15), it("白芍", 12)));
        pass(rx9, SettlementType.MEDICAL_INSURANCE, "补气养血方");
        decoctAll(rx9, false, 0, true);
        deliverAll(rx9);
        exceptionService.create(new ExceptionService.CreateReq(
                rx9.getId(), ExceptionType.INVOICE_REISSUE, "诊所月底统一结账，需补开发票", null), clinic1User);

        // 10) 老人代取，待取药
        Prescription rx10 = createRx(clinic1User, "周老太", "13800001006", 80, "女", "行动不便，子女代取",
                docLiu.getId(), 3, null, true, PickupMethod.PROXY_PICKUP, null, "李小军", "13900001006",
                false, null, List.of(it("苦杏仁", 10), it("桔梗", 8), it("陈皮", 6), it("黄芩", 9)));
        pass(rx10, SettlementType.MEDICAL_INSURANCE, "止咳化痰方");
        decoctAll(rx10, false, 0, true);

        // 11) 已回访：服药不适(自动异常+赔付流程) + 用药提醒 + 诊所评分下降
        Prescription rx11 = createRx(clinic2User, "何建军", "13800002003", 48, "男", null,
                docZhou.getId(), 5, "人参另煎兑服", false, PickupMethod.DELIVERY, "仁和街道胜利路12号", null, null,
                false, null, List.of(it("人参", 6, SpecialHandling.SEPARATE), it("黄芪", 15), it("白术", 10)));
        pass(rx11, SettlementType.SELF_PAY, "含自费药味人参，自费结算");
        decoctAll(rx11, false, 0, true);
        deliverAll(rx11);
        followUpService.create(new FollowUpService.FollowUpReq(
                rx11.getId(), FollowUpResult.DISCOMFORT, "服药后胃部不适、轻微腹泻", 2, true,
                "建议饭后温服，已登记不适并跟进"), pharmacist);

        // 12) 审方驳回：细辛超量
        Prescription rx12 = createRx(clinic2User, "宋小宝", "13800002004", 39, "男", null,
                docZhou.getId(), 3, null, false, PickupMethod.SELF_PICKUP, null, null, null,
                false, null, List.of(it("细辛", 8), it("川芎", 6)));
        reviewService.review(rx12.getId(), new ReviewService.ReviewReq(
                ReviewConclusion.REJECTED, SettlementType.SELF_PAY, "细辛8g超过常规最大剂量3g，驳回请医生改方", null), pharmacist);
    }
}
