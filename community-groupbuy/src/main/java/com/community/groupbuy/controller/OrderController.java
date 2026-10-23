package com.community.groupbuy.controller;

import com.community.groupbuy.common.Result;
import com.community.groupbuy.common.SecurityUtils;
import com.community.groupbuy.dto.CreateOrderDTO;
import com.community.groupbuy.entity.GroupOrder;
import com.community.groupbuy.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * M3 参团下单（核心并发）
 */
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    public Result<Long> create(@Valid @RequestBody CreateOrderDTO dto) {
        Long userId = SecurityUtils.getUserId();
        return Result.success(orderService.createOrder(userId, dto));
    }

    /**
     * 我的订单（居民端）：按当前登录用户查询
     */
    @GetMapping("/my")
    public Result<java.util.List<GroupOrder>> myOrders() {
        Long userId = SecurityUtils.getUserId();
        return Result.success(orderService.listMyOrders(userId));
    }

    @GetMapping("/{id}")
    public Result<GroupOrder> get(@PathVariable Long id) {
        return Result.success(orderService.getOrder(id));
    }
}
