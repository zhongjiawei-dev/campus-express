package com.campus.express.complaint;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("t_complaint")
public class Complaint {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String complaintNo;
    private String orderNo;
    private Long userId;
    private Long courierId;
    private Integer type;
    private String description;
    private String images;
    private String demand;
    private Integer status;
    private Long handlerId;
    private String result;
    private BigDecimal compensation;
    private Integer responsibility;
    private LocalDateTime deadline;
    private LocalDateTime closeTime;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    @TableLogic
    private Integer isDeleted;
}
