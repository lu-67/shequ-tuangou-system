package com.community.groupbuy.controller;

import com.alibaba.excel.EasyExcel;
import com.community.groupbuy.mapper.StatsMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/stats")
public class StatsController {

    @Autowired
    private StatsMapper statsMapper;

    @GetMapping("/overview")
    @PreAuthorize("hasAnyRole('ADMIN', 'HEAD')")
    public Map<String, Object> overview() {
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "success");
        result.put("data", statsMapper.getOverviewStats());
        return result;
    }

    @GetMapping("/orders/export")
    @PreAuthorize("hasAnyRole('ADMIN', 'HEAD')")
    public void exportOrders(HttpServletResponse response) throws IOException {
        // 1. 设置响应头
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        String fileName = URLEncoder.encode("社区团购订单数据", StandardCharsets.UTF_8).replaceAll("\\+", "%20");
        response.setHeader("Content-disposition", "attachment;filename*=utf-8''" + fileName + ".xlsx");

        // 2. 查库
        List<Map<String, Object>> orderList = statsMapper.getOrderListForExport();

        // 3. 在控制台打印条数（关键！）
        System.out.println("============== 准备导出数据，查到的条数：" + (orderList == null ? 0 : orderList.size()) + " ==============");

        // 4. 如果为空，给个兜底提示，防止空指针
        if (orderList == null) {
            orderList = new ArrayList<>();
        }

        // 5. 组装表头
        List<List<String>> head = new ArrayList<>();
        head.add(List.of("订单号"));
        head.add(List.of("下单用户ID"));
        head.add(List.of("支付金额"));
        head.add(List.of("订单状态"));
        head.add(List.of("下单时间"));

        // 6. 写入 Excel
        EasyExcel.write(response.getOutputStream())
                .head(head)
                .sheet("订单数据")
                .doWrite(orderList);
    }
}