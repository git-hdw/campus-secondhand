package com.campus.secondhand.service;

import com.campus.secondhand.entity.Message;
import com.campus.secondhand.entity.Product;
import com.campus.secondhand.entity.User;
import com.campus.secondhand.repository.MessageRepository;
import com.campus.secondhand.repository.ProductRepository;
import com.campus.secondhand.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class MessageService {

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProductRepository productRepository;

    public Message findById(Long id) {
        return messageRepository.selectById(id);
    }

    public int countUnread(Long userId) {
        return messageRepository.countUnread(userId);
    }

    @org.springframework.transaction.annotation.Transactional
    public Message send(Message message) {
        if (message.getProductId() != null && productRepository.lockById(message.getProductId()) == null)
            throw new IllegalArgumentException("商品不存在");
        message.setCreatedTime(LocalDateTime.now());
        message.setIsRead(0);
        messageRepository.insert(message);
        attachProducts(List.of(message));
        return message;
    }

    public void enrich(List<Message> messages) {
        var people = com.campus.secondhand.util.BatchLookup.users(userRepository, messages.stream().flatMap(m -> java.util.stream.Stream.of(m.getSenderId(), m.getReceiverId())).toList());
        messages.forEach(m -> { m.setSender(publicUser(people.get(m.getSenderId()))); m.setReceiver(publicUser(people.get(m.getReceiverId()))); });
        attachProducts(messages);
    }

    private void attachProducts(List<Message> messages) {
        var cache = com.campus.secondhand.util.BatchLookup.products(productRepository, messages.stream().map(Message::getProductId).toList());
        for (Message message : messages) {
            Long productId = message.getProductId();
            if (productId == null) {
                continue;
            }
            Product product = cache.get(productId);
            if (product == null) {
                continue;
            }
            Map<String, Object> brief = new HashMap<>();
            brief.put("id", product.getId());
            brief.put("title", product.getTitle());
            brief.put("price", product.getPrice());
            message.setProduct(brief);
        }
    }

    private Map<String, Object> publicUser(User row) {
        if (row == null) return null;
        Map<String, Object> brief = new HashMap<>();
        brief.put("id", row.getId());
        brief.put("username", row.getUsername());
        brief.put("avatar", row.getAvatar());
        return brief;
    }

    public int markConversationRead(Long userId, Long otherUserId) {
        messageRepository.markConversationRead(userId, otherUserId);
        return messageRepository.countUnread(userId);
    }

    public boolean markAsRead(Long id, Long userId) {
        return messageRepository.markReadByReceiver(id, userId) > 0
                || messageRepository.selectById(id) != null;
    }

    public boolean markAllAsRead(Long userId) {
        messageRepository.markAllRead(userId);
        return true;
    }

    public int delete(Long id) {
        return messageRepository.deleteById(id);
    }
}
