package com.community.groupbuy.controller;

import com.community.groupbuy.common.Result;
import com.community.groupbuy.common.SecurityUtils;
import com.community.groupbuy.dto.ActivityQueryDTO;
import com.community.groupbuy.dto.CreateActivityDTO;
import com.community.groupbuy.dto.PageResult;
import com.community.groupbuy.entity.GroupActivity;
import com.community.groupbuy.service.ActivityService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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
 * M2 开团活动接口
 */
@RestController
@RequestMapping("/api/activities")
@RequiredArgsConstructor
public class ActivityController {

    private final ActivityService activityService;

    /** 创建开团活动（团长 / 管理员），新建为 PENDING */
    @PostMapping
    public Result<Long> create(@Valid @RequestBody CreateActivityDTO dto) {
        Long operatorId = SecurityUtils.getUserId();
        String role = SecurityUtils.getRole();
        return Result.success(activityService.createActivity(dto, operatorId, role));
    }

    /** 活动状态流转：PENDING -> ONGOING -> SUCCESS / FAILED */
    @PutMapping("/{id}/status")
    public Result<Void> updateStatus(@PathVariable Long id, @RequestParam String status) {
        activityService.updateStatus(id, status, SecurityUtils.getRole());
        return Result.success("活动状态已更新");
    }

    /** 分页 + 动态条件查询（管理后台） */
    @GetMapping
    public Result<PageResult<GroupActivity>> page(ActivityQueryDTO query) {
        return Result.success(activityService.page(query));
    }

    /** 进行中活动列表（居民端首页） */
    @GetMapping("/ongoing")
    public Result<List<GroupActivity>> ongoing() {
        return Result.success(activityService.listOngoing());
    }

    @GetMapping("/{id}")
    public Result<GroupActivity> get(@PathVariable Long id) {
        return Result.success(activityService.getActivity(id));
    }
}
