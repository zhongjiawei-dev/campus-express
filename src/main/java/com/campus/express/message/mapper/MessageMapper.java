package com.campus.express.message.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.express.message.Message;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface MessageMapper extends BaseMapper<Message> {

    @Update("UPDATE t_message SET is_read = 1, read_time = CURRENT_TIMESTAMP "
            + "WHERE id = #{id} AND user_id = #{userId} AND is_read = 0")
    int markRead(@Param("id") Long id, @Param("userId") Long userId);
}
