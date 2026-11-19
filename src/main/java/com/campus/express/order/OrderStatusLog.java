package com.campus.express.order;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_order_status_log")
public class OrderStatusLog {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String orderNo;
    private Integer fromStatus;
    private Integer toStatus;
    private String event;
    private Long operatorId;
    private String operatorRole;
    private String remark;
    private LocalDateTime createdAt;
}
