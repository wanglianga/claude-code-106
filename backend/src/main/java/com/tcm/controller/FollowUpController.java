package com.tcm.controller;

import com.tcm.config.AuthUtils;
import com.tcm.model.FollowUp;
import com.tcm.model.Prescription;
import com.tcm.model.Reminder;
import com.tcm.service.FollowUpService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class FollowUpController {

    private final FollowUpService service;

    public FollowUpController(FollowUpService service) {
        this.service = service;
    }

    @PostMapping("/followups")
    public FollowUp create(@RequestBody FollowUpService.FollowUpReq req) {
        return service.create(req, AuthUtils.current());
    }

    @GetMapping("/followups")
    public List<FollowUp> list() {
        return service.list(AuthUtils.current());
    }

    @GetMapping("/followups/pending")
    public List<Prescription> pending() {
        return service.pending(AuthUtils.current());
    }

    @GetMapping("/reminders")
    public List<Reminder> reminders() {
        return service.reminderList(AuthUtils.current());
    }
}
