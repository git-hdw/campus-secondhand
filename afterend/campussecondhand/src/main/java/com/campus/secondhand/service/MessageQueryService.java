package com.campus.secondhand.service;

import com.campus.secondhand.common.PageResult;
import com.campus.secondhand.entity.Message;
import com.campus.secondhand.repository.MessageRepository;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import java.util.*;

@Service
public class MessageQueryService {
    private final JdbcTemplate jdbc;
    private final MessageRepository messages;
    private final MessageService details;
    public MessageQueryService(JdbcTemplate jdbc, MessageRepository messages, MessageService details) {
        this.jdbc = jdbc; this.messages = messages; this.details = details;
    }
    public PageResult<Message> page(long userId, boolean received, Integer page, Integer size) {
        var result = messages.selectPage(new Page<Message>(PageResult.number(page), PageResult.size(size)),
                new QueryWrapper<Message>().eq(received ? "receiver_id" : "sender_id", userId).orderByDesc("id"));
        details.enrich(result.getRecords());
        return new PageResult<>(result.getRecords(), result.getTotal(), result.getCurrent(), result.getSize());
    }
    public Map<String,Object> history(long userId, long peer, Long before, Long after) {
        if (peer <= 0 || (before != null && before <= 0) || (after != null && after <= 0) || (before != null && after != null))
            throw new IllegalArgumentException("消息游标不正确");
        var query = new QueryWrapper<Message>().and(w -> w.eq("sender_id", userId).eq("receiver_id", peer)
                .or().eq("sender_id", peer).eq("receiver_id", userId));
        if (before != null) query.lt("id", before);
        if (after != null) query.gt("id", after).orderByAsc("id"); else query.orderByDesc("id");
        List<Message> rows = messages.selectList(query.last("LIMIT 51"));
        boolean more = rows.size() > 50;
        if (more) rows = new ArrayList<>(rows.subList(0,50));
        rows.sort(Comparator.comparing(Message::getId));
        details.enrich(rows);
        var readIds = jdbc.queryForList("SELECT id FROM messages WHERE sender_id=? AND receiver_id=? AND is_read=1 ORDER BY id DESC LIMIT 50", Long.class, userId, peer);
        return Map.of("items", rows, "hasMore", more, "readIds", readIds);
    }
    public PageResult<Map<String,Object>> conversations(long userId, Integer page, Integer size, String search, boolean unread) {
        String keyword = search == null ? "" : search.trim();
        if (keyword.length() > 100) throw new IllegalArgumentException("搜索内容过长");
        String like = "%" + keyword.replace("!", "!!").replace("%", "!%").replace("_", "!_") + "%";
        String from = " FROM (SELECT CASE WHEN sender_id=? THEN receiver_id ELSE sender_id END AS peer,MAX(id) AS latest,SUM(receiver_id=? AND is_read=0) AS unreadCount FROM messages WHERE sender_id=? OR receiver_id=? GROUP BY peer) g JOIN messages m ON m.id=g.latest LEFT JOIN users u ON u.id=g.peer WHERE 1=1";
        List<Object> params = new ArrayList<>(List.of(userId,userId,userId,userId));
        if (unread) from += " AND g.unreadCount>0";
        if (!keyword.isEmpty()) {
            from += " AND (u.username LIKE ? ESCAPE '!' OR EXISTS (SELECT 1 FROM messages h LEFT JOIN products p ON p.id=h.product_id WHERE ((h.sender_id=? AND h.receiver_id=g.peer) OR (h.receiver_id=? AND h.sender_id=g.peer)) AND (h.content LIKE ? ESCAPE '!' OR p.title LIKE ? ESCAPE '!')))";
            params.addAll(List.of(like,userId,userId,like,like));
        }
        long total = jdbc.queryForObject("SELECT COUNT(*)" + from, Long.class, params.toArray());
        int current = PageResult.number(page), limit = PageResult.size(size);
        params.add(limit); params.add((long)(current-1)*limit);
        var rows = jdbc.queryForList("SELECT g.peer AS userId,COALESCE(u.username,'已注销用户') AS username,COALESCE(u.avatar,'') AS avatar,g.unreadCount,m.content AS lastMessage,m.created_time AS lastMessageTime,m.sender_id AS lastSenderId" + from + " ORDER BY m.id DESC LIMIT ? OFFSET ?", params.toArray());
        rows.forEach(row -> row.put("lastSent", ((Number)row.remove("lastSenderId")).longValue() == userId));
        return new PageResult<>(rows,total,current,limit);
    }
}
