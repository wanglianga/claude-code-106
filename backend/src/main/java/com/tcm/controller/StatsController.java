package com.tcm.controller;

import com.tcm.config.AuthUtils;
import com.tcm.service.StatsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/stats")
public class StatsController {

    private final StatsService service;

    public StatsController(StatsService service) {
        this.service = service;
    }

    @GetMapping("/dashboard")
    public Map<String, Object> dashboard() {
        return service.dashboard(AuthUtils.current());
    }

    /** 异常复盘：dim = doctor | herb | pot | courier */
    @GetMapping("/exceptions")
    public List<Map<String, Object>> exceptionsBy(@RequestParam(defaultValue = "doctor") String dim) {
        return service.exceptionsBy(dim, AuthUtils.current());
    }

    @GetMapping("/clinics")
    public List<Map<String, Object>> clinicScores() {
        return service.clinicScores(AuthUtils.current());
    }
}
