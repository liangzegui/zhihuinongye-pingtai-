package com.example.agribackend.mapper;

import com.example.agribackend.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Update;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface UserMapper {
    // 根据用户名查询用户
    @Select("SELECT * FROM user WHERE username = #{username}")
    User findByUsername(String username);

    // 根据ID查询用户
    @Select("SELECT id, username, password, role, create_time AS createTime FROM user WHERE id = #{id}")
    User findById(@Param("id") Integer id);

    // 查询全部用户（包含密码字段，供服务层内部使用）
    @Select("SELECT id, username, password, role, create_time AS createTime FROM user ORDER BY id DESC")
    List<User> findAll();

    // 插入新用户
    @Insert("INSERT INTO user (username, password, role, create_time) VALUES (#{username}, #{password}, #{role}, #{createTime})")
    int insert(User user);

    // 更新用户密码
    @Update("UPDATE user SET password = #{newPassword} WHERE username = #{username}")
    int updatePassword(String username, String newPassword);

    // 按ID更新用户名和角色
    @Update("UPDATE user SET username = #{username}, role = #{role} WHERE id = #{id}")
    int updateBasicById(@Param("id") Integer id, @Param("username") String username, @Param("role") String role);

    // 按ID更新密码
    @Update("UPDATE user SET password = #{newPassword} WHERE id = #{id}")
    int updatePasswordById(@Param("id") Integer id, @Param("newPassword") String newPassword);

    // 按ID删除用户
    @Delete("DELETE FROM user WHERE id = #{id}")
    int deleteById(@Param("id") Integer id);

    // 统计某角色用户数量
    @Select("SELECT COUNT(1) FROM user WHERE role = #{role}")
    int countByRole(@Param("role") String role);
}