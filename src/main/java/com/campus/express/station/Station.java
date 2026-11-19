package com.campus.express.station;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("t_station")
public class Station {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private Integer brand;
    private String campus;
    private String address;
    private BigDecimal lat;
    private BigDecimal lng;
    private String openTime;
    private String closeTime;
    private Integer status;
    private Integer sort;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    @TableLogic
    private Integer isDeleted;
}
