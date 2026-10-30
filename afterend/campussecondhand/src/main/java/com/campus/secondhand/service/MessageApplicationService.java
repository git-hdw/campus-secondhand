package com.campus.secondhand.service;

import com.campus.secondhand.common.ApiResponse;
import com.campus.secondhand.entity.Message;
import com.campus.secondhand.entity.Product;
import com.campus.secondhand.entity.User;
import com.campus.secondhand.repository.UserRepository;
import com.campus.secondhand.service.MessageService;
import com.campus.secondhand.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.List;
import java.util.Map;
import java.util.Optional;



@org.springframework.stereotype.Service
public class MessageApplicationService {

    @Autowired
    private MessageService messageService;

    @Autowired
    private ProductService productService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private com.campus.secondhand.service.IdempotencyService idempotency;

    @Autowired private MessageQueryService queries;
    private long actor() { return com.campus.secondhand.config.CurrentUser.get().orElseThrow().getId(); }
    public ApiResponse<?> receivedMessages(String authHeader, Integer page, Integer pageSize) {
        return ApiResponse.success("获取成功", queries.page(actor(), true, page, pageSize));
    }
    public ApiResponse<?> sentMessages(String authHeader, Integer page, Integer pageSize) {
        return ApiResponse.success("获取成功", queries.page(actor(), false, page, pageSize));
    }
    public ApiResponse<?> conversation(Long otherUserId, String authHeader, Long before, Long after) {
        return ApiResponse.success("获取成功", queries.history(actor(), otherUserId, before, after));
    }
    public ApiResponse<?> conversations(Integer page, Integer pageSize, String search, boolean unread) {
        var result = queries.conversations(actor(), page, pageSize, search, unread);
        return ApiResponse.success("获取成功", Map.of("items", result.items(), "total", result.total(), "page", result.page(), "pageSize", result.pageSize(), "unreadTotal", messageService.countUnread(actor())));
    }

    public ApiResponse<?> markConversationRead(
            Long otherUserId,
            String authHeader) {
        try {
            Optional<User> userOpt = com.campus.secondhand.config.CurrentUser.get();
            if (userOpt.isEmpty()) {
                return (ApiResponse.error(401, "用户不存在"));
            }
            int count = messageService.markConversationRead(userOpt.get().getId(), otherUserId);
            return (ApiResponse.success("已读", Map.of("count", count)));
        } catch (Exception e) {
            return (ApiResponse.error(500, "操作失败"));
        }
    }

    public ApiResponse<?> unreadCount(String authHeader) {
        try {
            Optional<User> userOpt = com.campus.secondhand.config.CurrentUser.get();
            if (userOpt.isEmpty()) {
                return (ApiResponse.error(401, "用户不存在"));
            }
            int count = messageService.countUnread(userOpt.get().getId());
            return (ApiResponse.success("获取成功", Map.of("count", count)));
        } catch (Exception e) {
            throw new IllegalStateException("消息查询失败", e);
        }
    }

    public ApiResponse<?> send(
            String authHeader,
            String requestKey,
            Map<String, Object> params) {
        try {
            Optional<User> userOpt = com.campus.secondhand.config.CurrentUser.get();
            if (userOpt.isEmpty()) {
                return (ApiResponse.error(401, "用户不存在"));
            }

            Message message = new Message();
            message.setSenderId(userOpt.get().getId());
            message.setReceiverId(com.campus.secondhand.util.RequestValue.positiveId(params.get("receiverId")));
            if (message.getSenderId().equals(message.getReceiverId())) {
                return (ApiResponse.error(400, "不能给自己发消息"));
            }
            User receiver = userRepository.selectById(message.getReceiverId());
            if (receiver == null || (receiver.getDeleted() != null && receiver.getDeleted() == 1)) {
                return (ApiResponse.error(400, "对方不存在"));
            }
            if (receiver.getStatus() != null && receiver.getStatus() == 0) {
                return (ApiResponse.error(400, "对方账号不可用"));
            }
            if (params.get("productId") != null && !params.get("productId").toString().isBlank()) {
                Long productId = com.campus.secondhand.util.RequestValue.positiveId(params.get("productId"));
                Product product = productService.findById(productId);
                if (product == null) {
                    return (ApiResponse.error(400, "商品不存在"));
                }
                Long ownerId = product.getUserId();
                if (!ownerId.equals(message.getSenderId()) && !ownerId.equals(message.getReceiverId())) {
                    return (ApiResponse.error(400, "这件商品不是这次对话里的商品"));
                }
                message.setProductId(productId);
            }
            String content = params.get("content") == null ? "" : params.get("content").toString().trim();
            if (content.isEmpty()) {
                return (ApiResponse.error(400, "消息不能为空"));
            }
            if (content.length() > 500) {
                return (ApiResponse.error(400, "消息不能超过 500 字"));
            }
            message.setContent(content);

            return (idempotency.execute(userOpt.get().getId(), "message", requestKey, params, () -> ApiResponse.success("发送成功", messageService.send(message))));
        } catch (Exception e) {
            if (e instanceof IllegalArgumentException invalid) throw invalid;
            throw new IllegalStateException("消息发送失败", e);
        }
    }

    public ApiResponse<?> markAsRead(
            Long id,
            String authHeader) {
        try {
            Optional<User> userOpt = com.campus.secondhand.config.CurrentUser.get();
            if (userOpt.isEmpty()) {
                return (ApiResponse.error(401, "用户不存在"));
            }

            com.campus.secondhand.entity.Message message = messageService.findById(id);
            if (message == null) {
                return (ApiResponse.error(404, "消息不存在"));
            }
            if (!userOpt.get().getId().equals(message.getReceiverId())) {
                return (ApiResponse.error(403, "只能把自己收到的消息标为已读"));
            }
            messageService.markAsRead(id, userOpt.get().getId());
            return (ApiResponse.success("已标记为已读", null));
        } catch (Exception e) {
            return (ApiResponse.error(500, "操作失败"));
        }
    }

    public ApiResponse<?> markAllAsRead(String authHeader) {
        try {
            Optional<User> userOpt = com.campus.secondhand.config.CurrentUser.get();
            if (userOpt.isEmpty()) {
                return (ApiResponse.error(401, "用户不存在"));
            }

            messageService.markAllAsRead(userOpt.get().getId());
            return (ApiResponse.success("已全部标记为已读", null));
        } catch (Exception e) {
            return (ApiResponse.error(500, "操作失败"));
        }
    }

    public ApiResponse<?> delete(
            Long id,
            String authHeader) {
        try {
            Optional<User> userOpt = com.campus.secondhand.config.CurrentUser.get();
            if (userOpt.isEmpty()) {
                return (ApiResponse.error(401, "用户不存在"));
            }

            Message message = messageService.findById(id);
            if (message == null) {
                return (ApiResponse.error(404, "消息不存在"));
            }
            Long userId = userOpt.get().getId();
            if (!message.getSenderId().equals(userId) && !message.getReceiverId().equals(userId)) {
                return (ApiResponse.error(403, "无权操作"));
            }

            int deleted = messageService.delete(id);
            if (deleted > 0) {
                return (ApiResponse.success("删除成功", null));
            }
            return (ApiResponse.error(404, "消息不存在"));
        } catch (Exception e) {
            return (ApiResponse.error(500, "删除失败"));
        }
    }
}
