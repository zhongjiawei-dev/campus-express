package com.campus.express.order;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("t_order")
public class Order {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String orderNo;
    private String parentOrderNo;
    private Long userId;
    private Long courierId;
    private Long stationId;
    private Integer orderType;
    private Integer status;
    private Integer weightGram;
    private Integer actualWeightGram;
    @JsonIgnore
    private String pickupCode;
    private String pickupCodeMask;
    private Integer pickupCodeVerified;
    private LocalDateTime expectStart;
    private LocalDateTime expectEnd;
    private Long addressId;
    @JsonIgnore
    private String addressSnapshot;
    private Integer distanceM;
    private BigDecimal baseFee;
    private BigDecimal weightFee;
    private BigDecimal distanceFee;
    private BigDecimal peakFee;
    private BigDecimal typeExtraFee;
    private BigDecimal insureFee;
    private BigDecimal discountAmount;
    private BigDecimal totalFee;
    private BigDecimal payFee;
    private String priceSnapshot;
    private Integer payType;
    private String payNo;
    private LocalDateTime payTime;
    private LocalDateTime acceptTime;
    private LocalDateTime pickTime;
    private LocalDateTime deliverTime;
    private LocalDateTime finishTime;
    private Integer cancelRole;
    private String cancelReason;
    private Integer insureFlag;
    private BigDecimal declaredValue;
    private String photoPickUrl;
    private String photoDeliverUrl;
    private Integer timeoutFlag;
    private Integer urgent;
    private String remark;
    private Integer version;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    @TableLogic
    private Integer isDeleted;
}
