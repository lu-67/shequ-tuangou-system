package com.community.groupbuy.service;

import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Map;

public interface StatsService {
    Map<String, Object> getOverviewStats();
    List<Map<String, Object>> listOrders();
    void exportOrders(HttpServletResponse response);
}