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
    public List<Map<String, Object>> listOrders() {
        List<Map<String, Object>> list = statsMapper.getOrderListForExport(null);
        return list == null ? new ArrayList<>() : list;
    }

    private static final Map<String, String> STATUS_MAP = Map.of(
            "UNPAID", "待支付",
            "PAID", "已支付",
            "CANCELLED", "已取消",
            "PICKED", "已核销"
    );

    @Override
    public void exportOrders(HttpServletResponse response) {
        try {
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setCharacterEncoding("utf-8");
            String fileName = URLEncoder.encode("社区团购订单数据", StandardCharsets.UTF_8).replaceAll("\\+", "%20");
            response.setHeader("Content-disposition", "attachment;filename*=utf-8''" + fileName + ".xlsx");

            List<Map<String, Object>> orderList = statsMapper.getOrderListForExport(null);
            if (orderList == null) orderList = new ArrayList<>();

            // 表头（与图片中 Excel 设计一致）
            List<List<String>> head = new ArrayList<>();
            head.add(List.of("订单号"));
            head.add(List.of("下单用户ID"));
            head.add(List.of("支付金额"));
            head.add(List.of("订单状态"));
            head.add(List.of("下单时间"));

            // 将 Map 列表转换为 EasyExcel 需要的二维数组格式
            List<List<Object>> dataList = new ArrayList<>();
            for (Map<String, Object> row : orderList) {
                List<Object> rowData = new ArrayList<>();
                // 1. 订单号
                rowData.add(row.get("orderNo"));
                // 2. 下单用户ID
                rowData.add(row.get("username"));
                // 3. 支付金额
                rowData.add(row.get("amount"));
                // 4. 订单状态：转换为中文
                String status = (String) row.get("status");
                rowData.add(STATUS_MAP.getOrDefault(status, status));
                // 5. 下单时间
                rowData.add(row.get("createTime"));
                dataList.add(rowData);
            }

            EasyExcel.write(response.getOutputStream())
                    .head(head)
                    .sheet("订单数据")
                    .doWrite(dataList);
        } catch (IOException e) {
            throw new RuntimeException("导出Excel失败", e);
        }
    }
}