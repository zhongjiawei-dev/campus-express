package com.campus.express.wallet.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.express.wallet.WalletFlow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Mapper
public interface WalletFlowMapper extends BaseMapper<WalletFlow> {

    @Select("SELECT COALESCE(SUM(amount), 0) FROM t_wallet_flow "
            + "WHERE user_id = #{userId} AND direction = 1 AND biz_type IN (3, 6) AND created_at >= #{start}")
    BigDecimal incomeSince(@Param("userId") Long userId, @Param("start") LocalDateTime start);
}
