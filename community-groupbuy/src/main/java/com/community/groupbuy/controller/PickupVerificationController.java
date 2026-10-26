package com.community.groupbuy.controller;

import com.community.groupbuy.common.Result;
import com.community.groupbuy.common.SecurityUtils;
import com.community.groupbuy.dto.VerifyDTO;
import com.community.groupbuy.service.PickupVerificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * M5 自提核销接口
 *
 * POST /api/pickup/verify  body: { verificationCode, pickupPointId } —— 仅 ADMIN / HEAD
 */
@RestController
@RequestMapping("/api/pickup")
@RequiredArgsConstructor
public class PickupVerificationController {

    private final PickupVerificationService pickupVerificationService;

    @PostMapping("/verify")
    public Result<Long> verify(@Valid @RequestBody VerifyDTO dto) {
        return Result.success(pickupVerificationService.verify(dto, SecurityUtils.getRole()));
    }
}
