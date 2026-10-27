package com.community.groupbuy.controller;

import com.community.groupbuy.common.JwtUtils;
import com.community.groupbuy.config.JwtUser;
import com.community.groupbuy.dto.LoginDTO;
import com.community.groupbuy.entity.SysUser;
import com.community.groupbuy.mapper.SysUserMapper;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private SysUserMapper sysUserMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtils jwtUtils;

    /**
     * 用户登录接口
     */
    @PostMapping("/login")
    public Map<String, Object> login(@Valid @RequestBody LoginDTO loginDTO) {
        // 1. 查数据库
        SysUser user = sysUserMapper.selectByUsername(loginDTO.getUsername());
        if (user == null) {
            throw new RuntimeException("用户名或密码错误");
        }

        // 2. 校验密码 (matches 会自动比对明文和数据库里的密文)
        if (!passwordEncoder.matches(loginDTO.getPassword(), user.getPassword())) {
            throw new RuntimeException("用户名或密码错误");
        }

        // 3. 生成 Token
        JwtUser jwtUser = new JwtUser(user.getId(), user.getUsername(), user.getRole(), user.getPassword());
        String token = jwtUtils.generateToken(jwtUser);

        // 4. 返回结果
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "登录成功");

        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        data.put("role", user.getRole());
        data.put("username", user.getUsername());
        result.put("data", data);

        return result;
    }

    /**
     * 用户注册接口
     */
    @PostMapping("/register")
    public Map<String, Object> register(@RequestBody SysUser user) {
        // 1. 检查用户名是否重复
        SysUser existUser = sysUserMapper.selectByUsername(user.getUsername());
        if (existUser != null) {
            throw new RuntimeException("该用户名已被注册");
        }

        // 2. 密码加密
        String encodedPassword = passwordEncoder.encode(user.getPassword());
        user.setPassword(encodedPassword);

        // 3. 设置默认角色（如果前端没传，默认是居民 RESIDENT）
        if (user.getRole() == null || user.getRole().isEmpty()) {
            user.setRole("RESIDENT");
        }
        if (user.getNickname() == null) {
            user.setNickname(user.getUsername());
        }

        // 4. 插入数据库
        sysUserMapper.insert(user);

        // 5. 返回结果
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "注册成功");
        return result;
    }
}