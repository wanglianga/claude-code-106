package com.tcm.service;

import com.tcm.config.AuthUtils;
import com.tcm.model.*;
import com.tcm.repository.*;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 管理复盘：按医生、药味、锅号、配送员统计异常 */
@Service
public class StatsService {

    private final PrescriptionRepository prescriptions;
    private final PrescriptionItemRepository items;
    private final DecoctTaskRepository decoctTasks;
    private final DeliveryRepository deliveries;
    private final ExceptionEventRepository exceptions;
    private final ClinicRepository clinics;

    public StatsService(PrescriptionRepository prescriptions, PrescriptionItemRepository items,
                        DecoctTaskRepository decoctTasks, DeliveryRepository deliveries,
                        ExceptionEventRepository exceptions, ClinicRepository clinics) {
        this.prescriptions = prescriptions;
        this.items = items;
        this.decoctTasks = decoctTasks;
        this.deliveries = deliveries;
        this.exceptions = exceptions;
        this.clinics = clinics;
    }

    public Map<String, Object> dashboard(User user) {
        Map<String, Object> map = new LinkedHashMap<>();
        Map<String, Long> statusCounts = new LinkedHashMap<>();
        for (PrescriptionStatus s : PrescriptionStatus.values()) {
            statusCounts.put(s.name(), prescriptions.countByStatus(s));
        }
        map.put("statusCounts", statusCounts);
        map.put("todayCount", prescriptions.countByCreatedAtAfter(LocalDate.now().atStartOfDay()));
        map.put("openExceptions", exceptions.countByStatus(ExceptionStatus.OPEN)
                + exceptions.countByStatus(ExceptionStatus.PROCESSING));
        map.put("decoctQueued", decoctTasks.countByStatus(DecoctStatus.QUEUED));
        map.put("decocting", decoctTasks.countByStatus(DecoctStatus.IN_PROGRESS));
        map.put("delivering", deliveries.countByStatus(DeliveryStatus.DELIVERING));
        map.put("pendingFollowUp", prescriptions.countByStatus(PrescriptionStatus.SIGNED));
        BigDecimal compensation = exceptions.findAll().stream()
                .map(ExceptionEvent::getCompensation)
                .filter(c -> c != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        map.put("totalCompensation", compensation);
        return map;
    }

    /** 异常复盘：dim = doctor | herb | pot | courier */
    public List<Map<String, Object>> exceptionsBy(String dim, User user) {
        AuthUtils.requireRole(user, Role.ADMIN, Role.PHARMACIST);
        Map<String, long[]> counts = new LinkedHashMap<>();
        Map<String, BigDecimal> comps = new LinkedHashMap<>();
        for (ExceptionEvent e : exceptions.findAll()) {
            List<String> keys = keysOf(e, dim);
            for (String key : keys) {
                counts.computeIfAbsent(key, k -> new long[1])[0]++;
                comps.merge(key, e.getCompensation() != null ? e.getCompensation() : BigDecimal.ZERO,
                        BigDecimal::add);
            }
        }
        List<Map<String, Object>> result = new ArrayList<>();
        counts.forEach((k, v) -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("name", k);
            row.put("count", v[0]);
            row.put("compensation", comps.getOrDefault(k, BigDecimal.ZERO));
            result.add(row);
        });
        result.sort((a, b) -> Long.compare((long) b.get("count"), (long) a.get("count")));
        return result;
    }

    private List<String> keysOf(ExceptionEvent e, String dim) {
        Prescription p = e.getPrescription();
        switch (dim) {
            case "doctor":
                return List.of(p.getDoctor() != null
                        ? p.getDoctor().getName() + (p.getDoctor().getClinic() != null
                        ? "(" + p.getDoctor().getClinic().getName() + ")" : "")
                        : "无医生(患者直投)");
            case "herb": {
                List<String> names = new ArrayList<>();
                for (PrescriptionItem it : items.findByPrescriptionId(p.getId())) {
                    names.add(it.getHerb().getName());
                }
                return names.isEmpty() ? List.of("无药味") : names;
            }
            case "pot":
                return List.of(decoctTasks.findByPrescriptionId(p.getId())
                        .map(t -> "锅号 " + t.getPotNo()).orElse("未入煎"));
            case "courier":
                return List.of(deliveries.findByPrescriptionId(p.getId())
                        .map(d -> d.getCourier() != null ? d.getCourier().getName() : "未分配配送员")
                        .orElse("未配送"));
            default:
                return List.of("未知维度");
        }
    }

    public List<Map<String, Object>> clinicScores(User user) {
        AuthUtils.requireRole(user, Role.ADMIN, Role.PHARMACIST);
        List<Map<String, Object>> result = new ArrayList<>();
        for (Clinic c : clinics.findAll()) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("clinic", c);
            row.put("prescriptionCount", prescriptions.findByClinicIdOrderByCreatedAtDesc(c.getId()).size());
            result.add(row);
        }
        return result;
    }
}
