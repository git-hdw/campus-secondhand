package com.campus.secondhand.service;

import com.campus.secondhand.common.ApiResponse;
import com.campus.secondhand.entity.Order;
import com.campus.secondhand.entity.Product;
import com.campus.secondhand.entity.User;
import com.campus.secondhand.repository.OrderRepository;
import com.campus.secondhand.repository.ProductRepository;
import org.apache.ibatis.annotations.Select;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderTradeServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private OrderTradeService trade;

    @Test
    void cannotBuyOwnProduct() {
        Product product = sale(9L, 2L);
        when(productRepository.lockById(9L)).thenReturn(product);
        ApiResponse<?> result = trade.place(user(2L), 9L);
        assertEquals(400, result.getCode());
        assertEquals("不能购买自己的商品", result.getMessage());
        verify(orderRepository, never()).insert(any());
    }

    @Test
    void onlyOnePendingOrder() {
        when(productRepository.lockById(9L)).thenReturn(sale(9L, 3L));
        when(orderRepository.lockCompletedIds(9L)).thenReturn(List.of());
        when(orderRepository.selectCount(any())).thenReturn(1L);
        ApiResponse<?> result = trade.place(user(2L), 9L);
        assertEquals(400, result.getCode());
        assertEquals("已有待见面订单", result.getMessage());
        verify(orderRepository, never()).insert(any());
    }

    @Test
    void soldProductCannotBeBoughtAgain() {
        when(productRepository.lockById(9L)).thenReturn(sale(9L, 3L));
        when(orderRepository.lockCompletedIds(9L)).thenReturn(List.of(41L));
        ApiResponse<?> result = trade.place(user(2L), 9L);
        assertEquals(400, result.getCode());
        assertEquals("商品已成交，不能再购买", result.getMessage());
    }

    @Test
    void onlyBuyerCanConfirm() {
        Order order = pendingOrder();
        when(orderRepository.selectById(7L)).thenReturn(order);
        ApiResponse<?> result = trade.changeOrder(user(3L), 7L, 3);
        assertEquals(403, result.getCode());
        assertEquals("只有买家可以确认完成", result.getMessage());
        verify(productRepository, never()).lockById(any());
    }

    @Test
    void missingProductCanBeCancelledButNotConfirmed() {
        when(productRepository.lockById(9L)).thenReturn(null);
        ApiResponse<?> confirm = trade.finish(7L, 9L, 3);
        assertEquals(404, confirm.getCode());

        when(orderRepository.update(isNull(), any())).thenReturn(1);
        when(orderRepository.selectById(7L)).thenReturn(pendingOrder());
        ApiResponse<?> cancel = trade.finish(7L, 9L, 4);
        assertEquals(200, cancel.getCode());
        assertEquals("取消成功", cancel.getMessage());
    }

    @Test
    void pendingOrderBlocksStatusChange() {
        when(productRepository.lockById(9L)).thenReturn(sale(9L, 3L));
        when(orderRepository.selectCount(any())).thenReturn(1L);
        ApiResponse<?> result = trade.changeProductStatus(9L, 2);
        assertEquals(400, result.getCode());
        assertEquals("有待见面订单，不能修改商品状态", result.getMessage());
        verify(productRepository, never()).updateById(any());
    }

    @Test
    void soldProductCannotReturnToSaleOrReview() {
        Product sold = sale(9L, 3L);
        sold.setStatus(1);
        when(productRepository.lockById(9L)).thenReturn(sold);
        when(orderRepository.selectCount(any())).thenReturn(0L);
        assertEquals("已成交商品不能重新上架", trade.changeProductStatus(9L, 0).getMessage());
        assertEquals("已成交商品不能重新上架", trade.changeProductStatus(9L, 3).getMessage());
    }

    @Test
    void repositoriesLockRows() throws Exception {
        String productSql = ProductRepository.class.getMethod("lockById", Long.class).getAnnotation(Select.class).value()[0];
        String orderSql = OrderRepository.class.getMethod("lockCompletedIds", Long.class).getAnnotation(Select.class).value()[0];
        assertTrue(productSql.contains("FOR UPDATE"));
        assertTrue(orderSql.contains("FOR UPDATE"));
    }

    private static Product sale(Long id, Long sellerId) {
        Product product = new Product();
        product.setId(id);
        product.setUserId(sellerId);
        product.setStatus(0);
        return product;
    }

    private static User user(Long id) {
        User user = new User();
        user.setId(id);
        return user;
    }

    private static Order pendingOrder() {
        Order order = new Order();
        order.setId(7L);
        order.setProductId(9L);
        order.setBuyerId(2L);
        order.setSellerId(3L);
        order.setStatus(0);
        return order;
    }
}
