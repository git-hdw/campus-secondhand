package com.campus.secondhand.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.secondhand.entity.Product;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import java.util.List;

@Mapper
public interface ProductRepository extends BaseMapper<Product> {

    @Select("SELECT * FROM products WHERE user_id = #{userId} ORDER BY created_time DESC")
    List<Product> findByUserId(@Param("userId") Long userId);

    @Select("SELECT * FROM products WHERE id = #{id}")
    Product findById(@Param("id") Long id);

    @Select("SELECT * FROM products WHERE id = #{id} FOR UPDATE")
    Product lockById(@Param("id") Long id);

    @Update("UPDATE products SET view_count = IFNULL(view_count, 0) + 1 WHERE id = #{id}")
    int increaseViewCount(@Param("id") Long id);
}
