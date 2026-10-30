package com.campus.secondhand.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.secondhand.entity.Message;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import java.util.List;

@Mapper
public interface MessageRepository extends BaseMapper<Message> {

    @Select("SELECT COUNT(*) FROM messages WHERE receiver_id = #{userId} AND is_read = 0")
    int countUnread(@Param("userId") Long userId);

    @Update("UPDATE messages SET is_read = 1 WHERE receiver_id = #{userId} AND sender_id = #{otherUserId} AND is_read = 0")
    int markConversationRead(@Param("userId") Long userId, @Param("otherUserId") Long otherUserId);

    @Update("UPDATE messages SET is_read = 1 WHERE id = #{id} AND receiver_id = #{userId} AND is_read = 0")
    int markReadByReceiver(@Param("id") Long id, @Param("userId") Long userId);

    @Update("UPDATE messages SET is_read = 1 WHERE receiver_id = #{userId} AND is_read = 0")
    int markAllRead(@Param("userId") Long userId);
}
