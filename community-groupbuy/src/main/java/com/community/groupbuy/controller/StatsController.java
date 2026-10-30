package com.community.groupbuy.controller;

import com.community.groupbuy.service.StatsService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/stats")
public class StatsController {

    @Autowired
    private StatsService statsService;

    @GetMapping("/overview")
    @PreAuthorize("hasAnyRole('ADMIN', 'HEAD')")
    public Map<String, Object> overview() {
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "success");
        result.put("data", statsService.getOverviewStats());
        return result;
    }

    @GetMapping("/orders/export")
    @PreAuthorize("hasAnyRole('ADMIN', 'HEAD')")
    public void exportOrders(HttpServletResponse response) {
        statsService.exportOrders(response);
    }

    @GetMapping("/orders")
    @PreAuthorize("hasAnyRole('ADMIN', 'HEAD')")
    public Map<String, Object> listOrders() {
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "success");
        result.put("data", statsService.listOrders());
        return result;
    }
}