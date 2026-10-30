package com.campus.secondhand.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.secondhand.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Optional;

@Mapper
public interface UserRepository extends BaseMapper<User> {
    @Select("SELECT * FROM users WHERE username = #{username} AND deleted = 0")
    Optional<User> findByUsername(@Param("username") String username);

    @Select("SELECT * FROM users WHERE student_no = #{studentNo} AND deleted = 0")
    Optional<User> findByStudentNo(@Param("studentNo") String studentNo);
}


