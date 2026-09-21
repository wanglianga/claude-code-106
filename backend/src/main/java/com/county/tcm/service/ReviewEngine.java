package com.county.tcm.service;

import com.county.tcm.domain.*;

import java.math.BigDecimal;
import java.util.*;

/**
 * 审方规则引擎:十八反十九畏、剂量异常、缺药与替代建议、医保目录限制。
 * 纯计算,不落库,供药师审方界面实时预览与正式审方共用。
 */
public final class ReviewEngine {

    /** 演示用缺药替替代表(原药 → 替代药) */
    private static final Map<String, String> SUBSTITUTE_TABLE = Map.of(
            "金银花", "忍冬藤",
            "砂仁", "白豆蔻",
            "郁金", "香附"
    );

    public record CheckResult(List<String> findings,
                              List<String> blockingFindings,
                              Map<String, String> shortageSuggest,
                              List<String> insuranceWarnings,
                              boolean blocked) {}

    public static CheckResult check(Prescription rx,
                                    List<CompatibilityRule> rules,
                                    Map<String, Herb> catalog) {
        List<String> findings = new ArrayList<>();
        List<String> blocking = new ArrayList<>();
        List<String> insuranceWarnings = new ArrayList<>();
        Map<String, String> shortage = new LinkedHashMap<>();

        List<PrescriptionHerb> items = rx.getHerbs();
        List<String> names = items.stream().map(PrescriptionHerb::getHerbName).toList();

        // 1) 十八反 / 十九畏
        for (int i = 0; i < items.size(); i++) {
            for (int j = i + 1; j < items.size(); j++) {
                String a = items.get(i).getHerbName();
                String b = items.get(j).getHerbName();
                for (CompatibilityRule rule : rules) {
                    if (rule.involves(a, b)) {
                        String msg = "【" + rule.getRuleGroup() + "】" + a + " 与 " + b
                                + " 配伍禁忌" + (rule.getDescription() == null ? "" : "(" + rule.getDescription() + ")");
                        findings.add(msg);
                        blocking.add(msg);
                    }
                }
            }
        }

        // 2) 剂量异常(单剂超安全上限)
        for (PrescriptionHerb ph : items) {
            Herb herb = catalog.get(ph.getHerbName());
            if (herb != null && herb.getMaxDosePerPacket() != null
                    && ph.getDosePerPacket().compareTo(herb.getMaxDosePerPacket()) > 0) {
                String msg = "【剂量异常】" + ph.getHerbName() + " 单剂 " + ph.getDosePerPacket()
                        + herb.getUnit() + " 超过安全上限 " + herb.getMaxDosePerPacket() + herb.getUnit();
                findings.add(msg);
                blocking.add(msg);
            }
        }

        // 3) 缺药检查(总需求 = 单剂 × 剂数)
        for (PrescriptionHerb ph : items) {
            Herb herb = catalog.get(ph.getHerbName());
            if (herb == null) {
                String msg = "【目录外药材】" + ph.getHerbName() + " 不在药房目录,无法调配";
                findings.add(msg);
                blocking.add(msg);
                continue;
            }
            BigDecimal need = ph.getDosePerPacket().multiply(BigDecimal.valueOf(rx.getDoses()));
            if (herb.getStock().compareTo(need) < 0) {
                String suggest = SUBSTITUTE_TABLE.get(ph.getHerbName());
                String msg = "【缺药】" + ph.getHerbName() + " 需 " + need + herb.getUnit()
                        + ",库存仅 " + herb.getStock() + herb.getUnit()
                        + (suggest != null && catalog.containsKey(suggest) ? ",建议替代:" + suggest : ",需等待补货");
                findings.add(msg);
                if (suggest != null && catalog.containsKey(suggest)) {
                    shortage.put(ph.getHerbName(), suggest);
                } else {
                    blocking.add(msg);
                }
            }
        }

        // 4) 医保目录限制
        if (rx.getSettlementType() == SettlementType.INSURANCE) {
            if (rx.getDoctor() != null && !rx.getDoctor().isInsuranceQualified()) {
                insuranceWarnings.add("医生 " + rx.getDoctor().getName() + " 无医保处方资质,医保可能拒付");
            }
            for (PrescriptionHerb ph : items) {
                Herb herb = catalog.get(ph.getHerbName());
                if (herb != null && !herb.isInsuranceCovered()) {
                    insuranceWarnings.add(ph.getHerbName() + " 不在医保目录内,需全额自费");
                }
            }
        }

        return new CheckResult(findings, blocking, shortage, insuranceWarnings, !blocking.isEmpty());
    }

    /** 费用试算:药费按克单价 × 总用量;代煎按剂;配送费;夜间急煎附加费;医保按目录内药费 60% 报销 */
    public record FeeBreakdown(BigDecimal herbAmount, BigDecimal coveredHerbAmount,
                               BigDecimal decoctFee, BigDecimal deliveryFee,
                               BigDecimal nightSurcharge, BigDecimal total,
                               BigDecimal insurancePaid, BigDecimal selfPaid) {}

    public static FeeBreakdown fees(Prescription rx, Map<String, Herb> catalog,
                                    BigDecimal decoctPerDose, BigDecimal deliveryFee,
                                    BigDecimal nightSurcharge) {
        BigDecimal herb = BigDecimal.ZERO;
        BigDecimal covered = BigDecimal.ZERO;
        for (PrescriptionHerb ph : rx.getHerbs()) {
            Herb h = catalog.get(ph.getHerbName());
            if (h == null) continue;
            BigDecimal amount = h.getUnitPrice()
                    .multiply(ph.getDosePerPacket())
                    .multiply(BigDecimal.valueOf(rx.getDoses()));
            herb = herb.add(amount);
            if (h.isInsuranceCovered()) covered = covered.add(amount);
        }
        boolean decoct = rx.getPickupMethod() != PickupMethod.SELF_PICKUP;
        BigDecimal dFee = decoct ? decoctPerDose.multiply(BigDecimal.valueOf(rx.getDoses())) : BigDecimal.ZERO;
        BigDecimal ship = decoct ? deliveryFee : BigDecimal.ZERO;
        BigDecimal night = rx.isNightUrgent() ? nightSurcharge : BigDecimal.ZERO;
        BigDecimal total = herb.add(dFee).add(ship).add(night)
                .setScale(2, java.math.RoundingMode.HALF_UP);

        BigDecimal insurance = BigDecimal.ZERO;
        if (rx.getSettlementType() == SettlementType.INSURANCE) {
            insurance = covered.multiply(BigDecimal.valueOf(0.6))
                    .setScale(2, java.math.RoundingMode.HALF_UP)
                    .min(total);
        }
        BigDecimal self = total.subtract(insurance).setScale(2, java.math.RoundingMode.HALF_UP);
        return new FeeBreakdown(herb.setScale(2, java.math.RoundingMode.HALF_UP),
                covered.setScale(2, java.math.RoundingMode.HALF_UP), dFee, ship, night, total,
                insurance, self);
    }
}
