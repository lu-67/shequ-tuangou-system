package com.community.groupbuy.service;

import jakarta.servlet.http.HttpServletResponse;
import java.util.Map;

public interface StatsService {
    Map<String, Object> getOverviewStats();
    void exportOrders(HttpServletResponse response);
}