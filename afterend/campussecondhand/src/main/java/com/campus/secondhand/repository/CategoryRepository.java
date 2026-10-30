package com.campus.secondhand.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.secondhand.entity.Category;

public interface CategoryRepository extends BaseMapper<Category> {
    @org.apache.ibatis.annotations.Select("SELECT * FROM categories WHERE name = #{name} FOR UPDATE")
    Category lockByName(@org.apache.ibatis.annotations.Param("name") String name);
    @org.apache.ibatis.annotations.Select("SELECT * FROM categories WHERE id = #{id} FOR UPDATE")
    Category lockById(@org.apache.ibatis.annotations.Param("id") Long id);
}