package com.tcm.controller;

import com.tcm.config.AuthUtils;
import com.tcm.model.Invoice;
import com.tcm.service.FinanceService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class FinanceController {

    private final FinanceService service;

    public FinanceController(FinanceService service) {
        this.service = service;
    }

    @GetMapping("/invoices")
    public List<Invoice> list() {
        return service.list(AuthUtils.current());
    }

    public record ReissueReq(String reason) {
    }

    /** 补开发票 */
    @PostMapping("/invoices/{id}/reissue")
    public Invoice reissue(@PathVariable Long id, @RequestBody ReissueReq req) {
        return service.reissue(id, req.reason(), AuthUtils.current());
    }

    @GetMapping("/finance/summary")
    public Map<String, Object> summary() {
        return service.summary(AuthUtils.current());
    }
}
