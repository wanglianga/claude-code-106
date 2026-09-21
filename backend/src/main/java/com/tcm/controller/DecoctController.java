package com.tcm.controller;

import com.tcm.config.AuthUtils;
import com.tcm.model.DecoctRecord;
import com.tcm.service.DecoctService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/decoct")
public class DecoctController {

    private final DecoctService service;

    public DecoctController(DecoctService service) {
        this.service = service;
    }

    @GetMapping("/queue")
    public List<Map<String, Object>> queue() {
        return service.queue(AuthUtils.current());
    }

    @PostMapping("/tasks/{id}/scan")
    public DecoctRecord scan(@PathVariable Long id, @RequestParam String code) {
        return service.scan(id, code, AuthUtils.current());
    }

    @PostMapping("/tasks/{id}/start")
    public DecoctRecord start(@PathVariable Long id) {
        return service.start(id, AuthUtils.current());
    }

    @PostMapping("/tasks/{id}/finish")
    public DecoctRecord finish(@PathVariable Long id, @RequestBody DecoctService.FinishReq req) {
        return service.finish(id, req, AuthUtils.current());
    }
}
