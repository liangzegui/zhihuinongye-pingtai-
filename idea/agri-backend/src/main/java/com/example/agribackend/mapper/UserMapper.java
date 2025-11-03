package com.example.agribackend.mapper;

import com.example.agribackend.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Insert;

@Mapper
public interface UserMapper {
    // 根据用户名查询用户
    @Select("SELECT * FROM user WHERE username = #{username}")
    User findByUsername(String username);

    // 插入新用户
    @Insert("INSERT INTO user (username, password, role, create_time) VALUES (#{username}, #{password}, #{role}, #{createTime})")
    int insert(User user);
}