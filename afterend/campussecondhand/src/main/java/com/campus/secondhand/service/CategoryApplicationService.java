package com.campus.secondhand.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.campus.secondhand.common.ApiResponse;
import com.campus.secondhand.entity.Category;
import com.campus.secondhand.entity.User;
import com.campus.secondhand.repository.CategoryRepository;
import com.campus.secondhand.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Optional;



@org.springframework.stereotype.Service
public class CategoryApplicationService {

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private UserRepository userRepository;


    private Optional<User> getUserFromToken(String authHeader) {
        return com.campus.secondhand.config.CurrentUser.get();
    }

    public ApiResponse<?> getAllCategories() {
        List<Category> categories = categoryRepository.selectList(null);
        return (ApiResponse.success("获取成功", categories));
    }

    public ApiResponse<?> getCategoryById(Long id) {
        Category category = categoryRepository.selectById(id);
        if (category != null) {
            return (ApiResponse.success("获取成功", category));
        }
        return (ApiResponse.error(404, "分类不存在"));
    }

    public ApiResponse<?> createCategory(
            String authHeader,
            Category category) {
        Optional<User> userOpt = getUserFromToken(authHeader);
        if (userOpt.isEmpty()) {
            return (ApiResponse.error(401, "未登录或登录已过期"));
        }
        return (ApiResponse.error(400, "请在管理端维护分类"));
    }

    public ApiResponse<?> updateCategory(
            String authHeader,
            Long id, 
            Category category) {
        Optional<User> userOpt = getUserFromToken(authHeader);
        if (userOpt.isEmpty()) {
            return (ApiResponse.error(401, "未登录或登录已过期"));
        }
        return (ApiResponse.error(400, "请在管理端维护分类"));
    }

    public ApiResponse<?> deleteCategory(
            String authHeader,
            Long id) {
        Optional<User> userOpt = getUserFromToken(authHeader);
        if (userOpt.isEmpty()) {
            return (ApiResponse.error(401, "未登录或登录已过期"));
        }
        return (ApiResponse.error(400, "请在管理端维护分类"));
    }

    public ApiResponse<?> getCategoriesByParentId(Long parentId) {
        QueryWrapper<Category> wrapper = new QueryWrapper<>();
        wrapper.eq("parent_id", parentId);
        List<Category> categories = categoryRepository.selectList(wrapper);
        return (ApiResponse.success("获取成功", categories));
    }
}