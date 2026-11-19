package com.campus.express.evaluation;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_evaluation")
public class Evaluation {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String orderNo;
    private Long userId;
    private Long courierId;
    private Integer score;
    private String tags;
    private String content;
    private Integer anonymous;
    private String reply;
    private Integer status;
    private LocalDateTime createdAt;
    @JsonIgnore
    @TableLogic
    private Integer isDeleted;
}
