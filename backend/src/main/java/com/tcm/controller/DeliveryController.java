package com.tcm.controller;

import com.tcm.config.AuthUtils;
import com.tcm.model.Delivery;
import com.tcm.model.Prescription;
import com.tcm.service.DeliveryService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class DeliveryController {

    private final DeliveryService service;

    public DeliveryController(DeliveryService service) {
        this.service = service;
    }

    @GetMapping("/deliveries")
    public List<Delivery> list() {
        return service.list(AuthUtils.current());
    }

    @GetMapping("/deliveries/pending")
    public List<Delivery> pendingPool() {
        return service.pendingPool(AuthUtils.current());
    }

    public record DispatchReq(Long courierId) {
    }

    @PostMapping("/deliveries/{id}/dispatch")
    public Delivery dispatch(@PathVariable Long id, @RequestBody(required = false) DispatchReq req) {
        return service.dispatch(id, req != null ? req.courierId() : null, AuthUtils.current());
    }

    public record SignReq(String signedBy) {
    }

    @PostMapping("/deliveries/{id}/sign")
    public Delivery sign(@PathVariable Long id, @RequestBody SignReq req) {
        return service.sign(id, req.signedBy(), AuthUtils.current());
    }

    /** 自提/老人代取 窗口签收 */
    @PostMapping("/prescriptions/{id}/pickup-sign")
    public Prescription pickupSign(@PathVariable Long id, @RequestBody SignReq req) {
        return service.pickupSign(id, req.signedBy(), AuthUtils.current());
    }
}
