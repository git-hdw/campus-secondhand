package com.campus.secondhand.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.secondhand.entity.Order;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface OrderRepository extends BaseMapper<Order> {

    @Select("SELECT id FROM orders WHERE product_id = #{productId} AND status = 3 FOR UPDATE")
    List<Long> lockCompletedIds(@Param("productId") Long productId);
}