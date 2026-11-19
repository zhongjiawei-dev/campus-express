package com.campus.express.pricing.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.express.pricing.PriceRule;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface PriceRuleMapper extends BaseMapper<PriceRule> {

    @Select("SELECT * FROM t_price_rule WHERE status = 1 AND is_deleted = 0 "
            + "AND order_type = #{orderType} AND station_id = #{stationId} "
            + "ORDER BY version DESC LIMIT 1")
    PriceRule matchStation(@Param("stationId") Long stationId, @Param("orderType") Integer orderType);

    @Select("SELECT * FROM t_price_rule WHERE status = 1 AND is_deleted = 0 "
            + "AND order_type = #{orderType} AND station_id IS NULL "
            + "ORDER BY version DESC LIMIT 1")
    PriceRule matchGlobal(@Param("orderType") Integer orderType);
}
