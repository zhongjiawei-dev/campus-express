package com.campus.express.message;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_message")
public class Message {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Integer msgType;
    private String title;
    private String content;
    private String bizNo;
    private Integer isRead;
    private LocalDateTime readTime;
    private Integer channel;
    private Integer sendStatus;
    private LocalDateTime createdAt;
    @TableLogic
    private Integer isDeleted;
}
