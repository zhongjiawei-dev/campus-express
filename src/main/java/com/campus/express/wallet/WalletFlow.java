package com.campus.express.wallet;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("t_wallet_flow")
public class WalletFlow {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String flowNo;
    private Long walletId;
    private Long userId;
    private Integer bizType;
    private Integer direction;
    private BigDecimal amount;
    private BigDecimal balanceAfter;
    private String bizNo;
    private Integer status;
    private String remark;
    private LocalDateTime createdAt;
}
