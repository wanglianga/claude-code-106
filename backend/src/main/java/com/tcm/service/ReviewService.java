package com.tcm.service;

import com.tcm.config.AuthUtils;
import com.tcm.model.*;
import com.tcm.repository.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 药师审方：十八反十九畏、剂量异常、缺药替代、医保/自费结算 */
@Service
public class ReviewService {

    private final PrescriptionRepository prescriptions;
    private final PrescriptionItemRepository items;
    private final HerbRepository herbs;
    private final HerbConflictRepository conflicts;
    private final ReviewRepository reviews;
    private final InvoiceRepository invoices;

    private final BigDecimal decoctFeePerDose;
    private final BigDecimal deliveryFee;

    public ReviewService(PrescriptionRepository prescriptions, PrescriptionItemRepository items,
                         HerbRepository herbs, HerbConflictRepository conflicts,
                         ReviewRepository reviews, InvoiceRepository invoices,
                         @Value("${app.decoct-fee-per-dose:3}") BigDecimal decoctFeePerDose,
                         @Value("${app.delivery-fee:5}") BigDecimal deliveryFee) {
        this.prescriptions = prescriptions;
        this.items = items;
        this.herbs = herbs;
        this.conflicts = conflicts;
        this.reviews = reviews;
        this.invoices = invoices;
        this.decoctFeePerDose = decoctFeePerDose;
        this.deliveryFee = deliveryFee;
    }

    public record Substitution(Long itemId, String note) {
    }

    public record ReviewReq(ReviewConclusion conclusion, SettlementType settlementType,
                            String note, List<Substitution> substitutions) {
    }

    /** 审方预检：自动核对配伍禁忌/剂量/库存/医保目录，供药师参考 */
    public Map<String, Object> precheck(Long prescriptionId, User user) {
        AuthUtils.requireRole(user, Role.PHARMACIST, Role.ADMIN);
        Prescription p = get(prescriptionId);
        return runChecks(p, items.findByPrescriptionId(prescriptionId));
    }

    private Prescription get(Long id) {
        return prescriptions.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "处方不存在"));
    }

    private Map<String, Object> runChecks(Prescription p, List<PrescriptionItem> itemList) {
        List<String> conflictMsgs = new ArrayList<>();
        List<HerbConflict> all = conflicts.findAll();
        for (int i = 0; i < itemList.size(); i++) {
            for (int j = i + 1; j < itemList.size(); j++) {
                Herb a = itemList.get(i).getHerb();
                Herb b = itemList.get(j).getHerb();
                for (HerbConflict c : all) {
                    boolean hit = (c.getHerbA().getId().equals(a.getId()) && c.getHerbB().getId().equals(b.getId()))
                            || (c.getHerbA().getId().equals(b.getId()) && c.getHerbB().getId().equals(a.getId()));
                    if (hit) {
                        conflictMsgs.add(a.getName() + " 与 " + b.getName() + " 属【" + c.getConflictType()
                                + "】配伍禁忌：" + c.getDescription());
                    }
                }
            }
        }
        List<String> doseMsgs = new ArrayList<>();
        List<String> shortageMsgs = new ArrayList<>();
        List<String> notInCatalog = new ArrayList<>();
        for (PrescriptionItem it : itemList) {
            Herb h = it.getHerb();
            if (it.getDosageG() > h.getMaxDailyDoseGram()) {
                doseMsgs.add(h.getName() + " 单剂 " + it.getDosageG() + "g 超过常规最大剂量 "
                        + h.getMaxDailyDoseGram() + "g");
            }
            int need = it.getDosageG() * p.getDoses();
            if (need > h.getStockGram()) {
                shortageMsgs.add(h.getName() + " 需 " + need + "g，库存仅 " + h.getStockGram() + "g，建议缺药替代或补货");
            }
            if (!Boolean.TRUE.equals(h.getInInsurance())) {
                notInCatalog.add(h.getName() + " 不在医保目录，医保结算时该味需自费");
            }
        }
        boolean hasSpecial = itemList.stream().anyMatch(i -> i.getSpecialHandling() != SpecialHandling.NONE);
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("conflicts", conflictMsgs);
        map.put("doseIssues", doseMsgs);
        map.put("shortages", shortageMsgs);
        map.put("notInCatalog", notInCatalog);
        map.put("hasSpecialHandling", hasSpecial);
        return map;
    }

    @Transactional
    public Review review(Long prescriptionId, ReviewReq req, User pharmacist) {
        AuthUtils.requireRole(pharmacist, Role.PHARMACIST, Role.ADMIN);
        Prescription p = get(prescriptionId);
        if (p.getStatus() != PrescriptionStatus.PENDING_REVIEW) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "当前状态不可审方");
        }
        if (req.conclusion() == null || req.settlementType() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "审方结论与结算方式必填");
        }
        List<PrescriptionItem> itemList = items.findByPrescriptionId(prescriptionId);
        Map<String, Object> checks = runChecks(p, itemList);

        // 缺药替代
        if (req.substitutions() != null) {
            for (Substitution s : req.substitutions()) {
                PrescriptionItem it = itemList.stream()
                        .filter(x -> x.getId().equals(s.itemId())).findFirst()
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "药味不存在: " + s.itemId()));
                it.setSubstituted(true);
                it.setSubstituteNote(s.note());
                items.save(it);
            }
        }

        p.setSettlementType(req.settlementType());
        computeAmounts(p, itemList);

        Review review = new Review();
        review.setPrescription(p);
        review.setPharmacist(pharmacist);
        review.setConflictResult(join(checks.get("conflicts")));
        review.setDoseResult(join(checks.get("doseIssues")));
        review.setShortageResult(join(checks.get("shortages")));
        review.setInsuranceResult(join(checks.get("notInCatalog")));
        review.setConclusion(req.conclusion());
        review.setNote(req.note());
        reviews.save(review);

        if (req.conclusion() == ReviewConclusion.REJECTED) {
            p.setStatus(PrescriptionStatus.REVIEW_REJECTED);
        } else {
            // 审方通过 → 扣减库存(抓药备药) → 开具发票 → 进入抓药环节
            for (PrescriptionItem it : itemList) {
                Herb h = it.getHerb();
                int need = it.getDosageG() * p.getDoses();
                h.setStockGram(Math.max(0, h.getStockGram() - need));
                herbs.save(h);
            }
            p.setStatus(PrescriptionStatus.DISPENSING);
            Invoice invoice = new Invoice();
            invoice.setPrescription(p);
            invoice.setInvoiceNo("INV-" + p.getRxNo());
            invoice.setAmount(p.getTotalAmount());
            invoice.setType(req.settlementType());
            invoices.save(invoice);
        }
        prescriptions.save(p);
        return review;
    }

    @SuppressWarnings("unchecked")
    private String join(Object list) {
        List<String> l = (List<String>) list;
        return l.isEmpty() ? "未见异常" : String.join("；", l);
    }

    /** 费用 = 药费 + 代煎费(剂数×单价) + 配送费；医保仅承担目录内药味与代煎费 */
    private void computeAmounts(Prescription p, List<PrescriptionItem> itemList) {
        BigDecimal herbsTotal = BigDecimal.ZERO;
        BigDecimal catalogTotal = BigDecimal.ZERO;
        for (PrescriptionItem it : itemList) {
            BigDecimal line = it.getUnitPrice()
                    .multiply(BigDecimal.valueOf(it.getDosageG()))
                    .multiply(BigDecimal.valueOf(p.getDoses()));
            herbsTotal = herbsTotal.add(line);
            if (Boolean.TRUE.equals(it.getHerb().getInInsurance())) {
                catalogTotal = catalogTotal.add(line);
            }
        }
        BigDecimal decoctFee = decoctFeePerDose.multiply(BigDecimal.valueOf(p.getDoses()));
        BigDecimal delivery = p.getPickupMethod() == PickupMethod.DELIVERY ? deliveryFee : BigDecimal.ZERO;
        BigDecimal total = herbsTotal.add(decoctFee).add(delivery);
        BigDecimal insurance = p.getSettlementType() == SettlementType.MEDICAL_INSURANCE
                ? catalogTotal.add(decoctFee) : BigDecimal.ZERO;
        p.setTotalAmount(total);
        p.setInsuranceAmount(insurance);
        p.setSelfPayAmount(total.subtract(insurance));
    }
}
