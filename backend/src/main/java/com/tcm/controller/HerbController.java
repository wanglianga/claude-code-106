package com.tcm.controller;

import com.tcm.config.AuthUtils;
import com.tcm.model.Herb;
import com.tcm.model.Role;
import com.tcm.repository.HerbRepository;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/herbs")
public class HerbController {

    private final HerbRepository herbs;

    public HerbController(HerbRepository herbs) {
        this.herbs = herbs;
    }

    @GetMapping
    public List<Herb> list() {
        return herbs.findAllByOrderByNameAsc();
    }

    public record StockReq(Integer stockGram) {
    }

    /** 补货/库存调整 */
    @PutMapping("/{id}/stock")
    public Herb adjustStock(@PathVariable Long id, @RequestBody StockReq req) {
        AuthUtils.requireRole(AuthUtils.current(), Role.ADMIN, Role.PHARMACIST);
        Herb herb = herbs.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "药材不存在"));
        if (req.stockGram() == null || req.stockGram() < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "库存须为非负整数");
        }
        herb.setStockGram(req.stockGram());
        return herbs.save(herb);
    }
}
