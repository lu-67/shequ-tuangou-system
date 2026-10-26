package com.community.groupbuy.controller;

import com.community.groupbuy.common.Result;
import com.community.groupbuy.common.SecurityUtils;
import com.community.groupbuy.dto.PickupPointDTO;
import com.community.groupbuy.entity.PickupPoint;
import com.community.groupbuy.service.PickupPointService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * M5 自提点接口
 *
 * - GET /api/pickup/points 返回数组（居民端首页、我的订单、自提核销下拉都用它）
 * - POST/PUT/DELETE 仅 ADMIN / HEAD
 */
@RestController
@RequestMapping("/api/pickup/points")
@RequiredArgsConstructor
public class PickupPointController {

    private final PickupPointService pickupPointService;

    @GetMapping
    public Result<List<PickupPoint>> list(@RequestParam(required = false) String name) {
        return Result.success(pickupPointService.list(name));
    }

    @GetMapping("/{id}")
    public Result<PickupPoint> get(@PathVariable Long id) {
        return Result.success(pickupPointService.getById(id));
    }

    @PostMapping
    public Result<Long> create(@Valid @RequestBody PickupPointDTO dto) {
        return Result.success(pickupPointService.create(dto, SecurityUtils.getRole()));
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody PickupPointDTO dto) {
        pickupPointService.update(id, dto, SecurityUtils.getRole());
        return Result.success("自提点已更新");
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        pickupPointService.delete(id, SecurityUtils.getRole());
        return Result.success("自提点已删除");
    }
}
