package com.campus.express.pricing;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("t_price_rule")
public class PriceRule {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String ruleNo;
    private Long stationId;
    private Integer orderType;
    private BigDecimal baseFee;
    private Integer freeWeightGram;
    private Integer weightStepGram;
    private BigDecimal weightStepFee;
    private Integer freeDistanceM;
    private Integer distanceStepM;
    private BigDecimal distanceStepFee;
    private BigDecimal peakRate;
    private String peakWindows;
    private BigDecimal typeExtraFee;
    private BigDecimal insureRate;
    private BigDecimal insureMinFee;
    private BigDecimal minFee;
    private BigDecimal maxFee;
    private LocalDate effectiveFrom;
    private LocalDate effectiveTo;
    private Integer status;
    private Integer version;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    @TableLogic
    private Integer isDeleted;
}
