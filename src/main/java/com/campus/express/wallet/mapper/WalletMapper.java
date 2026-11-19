package com.campus.express.wallet.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.express.wallet.Wallet;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.math.BigDecimal;

@Mapper
public interface WalletMapper extends BaseMapper<Wallet> {

    /** 乐观锁扣款：余额不足或版本过期返回 0，由上层重试或报错 */
    @Update("UPDATE t_wallet SET balance = balance - #{amount}, total_expense = total_expense + #{amount}, "
            + "version = version + 1, updated_at = CURRENT_TIMESTAMP "
            + "WHERE id = #{id} AND version = #{version} AND balance >= #{amount} AND is_deleted = 0")
    int debit(@Param("id") Long id, @Param("amount") BigDecimal amount, @Param("version") Integer version);

    @Update("UPDATE t_wallet SET balance = balance + #{amount}, total_income = total_income + #{amount}, "
            + "version = version + 1, updated_at = CURRENT_TIMESTAMP "
            + "WHERE id = #{id} AND version = #{version} AND is_deleted = 0")
    int credit(@Param("id") Long id, @Param("amount") BigDecimal amount, @Param("version") Integer version);
}
