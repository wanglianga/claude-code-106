package com.tcm.controller;

import com.tcm.config.AuthUtils;
import com.tcm.model.Review;
import com.tcm.service.ReviewService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/prescriptions/{id}")
public class ReviewController {

    private final ReviewService service;

    public ReviewController(ReviewService service) {
        this.service = service;
    }

    /** 审方预检：自动核对十八反十九畏/剂量/库存/医保目录 */
    @GetMapping("/precheck")
    public Map<String, Object> precheck(@PathVariable Long id) {
        return service.precheck(id, AuthUtils.current());
    }

    @PostMapping("/review")
    public Review review(@PathVariable Long id, @RequestBody ReviewService.ReviewReq req) {
        return service.review(id, req, AuthUtils.current());
    }
}
