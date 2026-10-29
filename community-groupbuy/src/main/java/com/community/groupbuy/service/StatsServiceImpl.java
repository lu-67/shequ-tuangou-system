package com.community.groupbuy.service;

import com.alibaba.excel.EasyExcel;
import com.community.groupbuy.mapper.StatsMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class StatsServiceImpl implements StatsService {

    @Autowired
    private StatsMapper statsMapper;

    @Override
    public Map<String, Object> getOverviewStats() {
        return statsMapper.getOverviewStats(null);
    }

    @Override
    public void exportOrders(HttpServletResponse response) {
        try {
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setCharacterEncoding("utf-8");
            String fileName = URLEncoder.encode("社区团购订单数据", StandardCharsets.UTF_8).replaceAll("\\+", "%20");
            response.setHeader("Content-disposition", "attachment;filename*=utf-8''" + fileName + ".xlsx");

            List<Map<String, Object>> orderList = statsMapper.getOrderListForExport(null);
            if (orderList == null) orderList = new ArrayList<>();

            List<List<String>> head = new ArrayList<>();
            head.add(List.of("订单号"));
            head.add(List.of("下单用户ID"));
            head.add(List.of("支付金额"));
            head.add(List.of("订单状态"));
            head.add(List.of("下单时间"));

            EasyExcel.write(response.getOutputStream())
                    .head(head)
                    .sheet("订单数据")
                    .doWrite(orderList);
        } catch (IOException e) {
            throw new RuntimeException("导出Excel失败", e);
        }
    }
}