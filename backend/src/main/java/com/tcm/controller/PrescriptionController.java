package com.tcm.controller;

import com.tcm.config.AuthUtils;
import com.tcm.model.Prescription;
import com.tcm.model.PrescriptionStatus;
import com.tcm.model.User;
import com.tcm.service.PrescriptionService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/prescriptions")
public class PrescriptionController {

    private final PrescriptionService service;

    public PrescriptionController(PrescriptionService service) {
        this.service = service;
    }

    @PostMapping
    public Prescription create(@RequestBody PrescriptionService.CreateReq req) {
        return service.create(req, AuthUtils.current());
    }

    @GetMapping
    public List<Prescription> list(@RequestParam(required = false) PrescriptionStatus status,
                                   @RequestParam(required = false) String batchNo) {
        return service.list(AuthUtils.current(), status, batchNo);
    }

    @GetMapping("/{id}")
    public Map<String, Object> detail(@PathVariable Long id) {
        return service.detail(id, AuthUtils.current());
    }

    /** 抓药完成 → 生成煎药任务并进入代煎排队 */
    @PostMapping("/{id}/dispense")
    public Object dispense(@PathVariable Long id) {
        return service.dispense(id, AuthUtils.current());
    }
}
