package com.tcm.controller;

import com.tcm.config.AuthUtils;
import com.tcm.model.ExceptionEvent;
import com.tcm.model.ExceptionStatus;
import com.tcm.service.ExceptionService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/exceptions")
public class ExceptionController {

    private final ExceptionService service;

    public ExceptionController(ExceptionService service) {
        this.service = service;
    }

    @GetMapping
    public List<ExceptionEvent> list(@RequestParam(required = false) ExceptionStatus status) {
        return service.list(AuthUtils.current(), status);
    }

    @PostMapping
    public ExceptionEvent create(@RequestBody ExceptionService.CreateReq req) {
        return service.create(req, AuthUtils.current());
    }

    @PostMapping("/{id}/resolve")
    public ExceptionEvent resolve(@PathVariable Long id, @RequestBody ExceptionService.ResolveReq req) {
        return service.resolve(id, req, AuthUtils.current());
    }
}
