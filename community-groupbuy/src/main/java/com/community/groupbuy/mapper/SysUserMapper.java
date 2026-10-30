package com.community.groupbuy.mapper;

import com.community.groupbuy.entity.SysUser;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface SysUserMapper {

    // 根据用户名查用户（登录用）
    @Select("SELECT * FROM sys_user WHERE username = #{username}")
    SysUser selectByUsername(String username);

    // 插入新用户（注册用）
    @Insert("INSERT INTO sys_user(username, password, nickname, role, create_time) " +
            "VALUES(#{username}, #{password}, #{nickname}, #{role}, NOW())")
    int insert(SysUser user);
}