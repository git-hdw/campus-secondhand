package com.campus.secondhand.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("messages")
public class Message {
    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("sender_id")
    private Long senderId;

    @TableField("receiver_id")
    private Long receiverId;

    @TableField("product_id")
    private Long productId;

    @TableField("content")
    private String content;

    @TableField("is_read")
    private Integer isRead = 0;

    @TableField("created_time")
    private LocalDateTime createdTime;

    @TableField(exist = false)
    private Object sender;

    @TableField(exist = false)
    private Object receiver;

    @TableField(exist = false)
    private Object product;

    public Message() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getSenderId() { return senderId; }
    public void setSenderId(Long senderId) { this.senderId = senderId; }

    public Long getReceiverId() { return receiverId; }
    public void setReceiverId(Long receiverId) { this.receiverId = receiverId; }

    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public Integer getIsRead() { return isRead; }
    public void setIsRead(Integer isRead) { this.isRead = isRead; }

    public LocalDateTime getCreatedTime() { return createdTime; }
    public void setCreatedTime(LocalDateTime createdTime) { this.createdTime = createdTime; }

    public Object getSender() { return sender; }
    public void setSender(Object sender) { this.sender = sender; }

    public Object getReceiver() { return receiver; }
    public void setReceiver(Object receiver) { this.receiver = receiver; }

    public Object getProduct() { return product; }
    public void setProduct(Object product) { this.product = product; }
}
