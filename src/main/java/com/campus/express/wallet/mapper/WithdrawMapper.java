package com.campus.express.wallet.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.express.wallet.Withdraw;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Mapper
public interface WithdrawMapper extends BaseMapper<Withdraw> {

    @Select("SELECT COALESCE(SUM(amount), 0) FROM t_withdraw "
            + "WHERE user_id = #{userId} AND status IN (1, 2) AND created_at >= #{start} AND is_deleted = 0")
    BigDecimal sumSince(@Param("userId") Long userId, @Param("start") LocalDateTime start);
}
