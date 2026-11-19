package com.campus.express.courier;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("t_courier")
public class Courier {

    public static final int AUDIT_PENDING = 1;
    public static final int AUDIT_APPROVED = 2;
    public static final int AUDIT_REJECTED = 3;

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String realName;
    private String studentNo;
    private String idCardMask;
    private String studentCardUrl;
    private Integer auditStatus;
    private String auditRemark;
    private Integer workStatus;
    private String serviceStart;
    private String serviceEnd;
    private Integer maxWeightGram;
    private String stationIds;
    private Integer level;
    private BigDecimal score;
    private Integer finishedCnt;
    private Integer violationCnt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    @TableLogic
    private Integer isDeleted;
}
