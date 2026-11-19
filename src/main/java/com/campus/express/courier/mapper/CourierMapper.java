package com.campus.express.courier.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.express.courier.Courier;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface CourierMapper extends BaseMapper<Courier> {

    @Update("UPDATE t_courier SET finished_cnt = finished_cnt + 1, score = LEAST(5.00, score + 0.01), "
            + "updated_at = CURRENT_TIMESTAMP WHERE id = #{id} AND is_deleted = 0")
    int increaseFinished(@Param("id") Long id);

    @Update("UPDATE t_courier SET violation_cnt = violation_cnt + 1, score = GREATEST(1.00, score - 0.10), "
            + "updated_at = CURRENT_TIMESTAMP WHERE id = #{id} AND is_deleted = 0")
    int increaseViolation(@Param("id") Long id);
}
