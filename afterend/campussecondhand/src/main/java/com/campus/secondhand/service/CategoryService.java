package com.campus.secondhand.service;
import com.campus.secondhand.common.ApiResponse;
import com.campus.secondhand.repository.*;
import com.campus.secondhand.entity.Product;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service
public class CategoryService {
    private final CategoryRepository categories;
    private final ProductRepository products;
    public CategoryService(CategoryRepository categories, ProductRepository products) { this.categories = categories; this.products = products; }
    @Transactional
    public ApiResponse<?> delete(Long id) {
        var category = categories.lockById(id);
        if (category == null) return ApiResponse.error(404, "分类不存在");
        if (products.selectCount(new QueryWrapper<Product>().eq("category", category.getName())) > 0)
            return ApiResponse.error(409, "该分类下还有商品，不能删除");
        categories.deleteById(id);
        return ApiResponse.success("删除成功", null);
    }
}
