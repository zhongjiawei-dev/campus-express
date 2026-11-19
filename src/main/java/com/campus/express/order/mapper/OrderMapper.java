package com.campus.express.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.express.order.Order;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Mapper
public interface OrderMapper extends BaseMapper<Order> {

    /**
     * 状态 CAS：只有当前状态仍等于 fromStatus 时才更新；接单场景同时要求 courier_id 为空，
     * 从数据库层面杜绝一单多接。
     */
    @Update("""
            UPDATE t_order
               SET status = #{toStatus},
                   courier_id = CASE WHEN #{toStatus} = 30 THEN #{courierId,jdbcType=BIGINT} ELSE courier_id END,
                   pay_time = CASE WHEN #{toStatus} = 20 THEN CURRENT_TIMESTAMP ELSE pay_time END,
                   accept_time = CASE WHEN #{toStatus} = 30 THEN CURRENT_TIMESTAMP ELSE accept_time END,
                   pick_time = CASE WHEN #{toStatus} = 40 THEN CURRENT_TIMESTAMP ELSE pick_time END,
                   deliver_time = CASE WHEN #{toStatus} = 50 THEN CURRENT_TIMESTAMP ELSE deliver_time END,
                   finish_time = CASE WHEN #{toStatus} = 60 OR #{toStatus} = 90 THEN CURRENT_TIMESTAMP ELSE finish_time END,
                   updated_at = CURRENT_TIMESTAMP,
                   version = version + 1
             WHERE order_no = #{orderNo}
               AND status = #{fromStatus}
               AND is_deleted = 0
               AND (#{toStatus} <> 30 OR courier_id IS NULL)
            """)
    int casUpdateStatus(@Param("orderNo") String orderNo,
                        @Param("fromStatus") int fromStatus,
                        @Param("toStatus") int toStatus,
                        @Param("courierId") Long courierId);

    @Update("UPDATE t_order SET pay_no = #{payNo}, pay_type = #{payType}, updated_at = CURRENT_TIMESTAMP "
            + "WHERE order_no = #{orderNo} AND status = 10 AND is_deleted = 0")
    int updatePayInfo(@Param("orderNo") String orderNo,
                      @Param("payNo") String payNo,
                      @Param("payType") Integer payType);

    @Update("UPDATE t_order SET pay_fee = #{fee}, updated_at = CURRENT_TIMESTAMP "
            + "WHERE order_no = #{orderNo} AND is_deleted = 0")
    int updatePayFee(@Param("orderNo") String orderNo, @Param("fee") BigDecimal fee);

    @Update("UPDATE t_order SET pickup_code_verified = 1, updated_at = CURRENT_TIMESTAMP "
            + "WHERE order_no = #{orderNo} AND pickup_code_verified = 0 AND is_deleted = 0")
    int markPickupVerified(@Param("orderNo") String orderNo);

    @Update("UPDATE t_order SET photo_pick_url = #{url}, updated_at = CURRENT_TIMESTAMP "
            + "WHERE order_no = #{orderNo} AND is_deleted = 0")
    int updatePickPhoto(@Param("orderNo") String orderNo, @Param("url") String url);

    @Update("UPDATE t_order SET photo_deliver_url = #{url}, updated_at = CURRENT_TIMESTAMP "
            + "WHERE order_no = #{orderNo} AND is_deleted = 0")
    int updateDeliverPhoto(@Param("orderNo") String orderNo, @Param("url") String url);

    @Update("UPDATE t_order SET cancel_role = #{role}, cancel_reason = #{reason}, updated_at = CURRENT_TIMESTAMP "
            + "WHERE order_no = #{orderNo} AND is_deleted = 0")
    int updateCancelInfo(@Param("orderNo") String orderNo,
                         @Param("role") Integer role,
                         @Param("reason") String reason);

    @Update("UPDATE t_order SET timeout_flag = 1, updated_at = CURRENT_TIMESTAMP "
            + "WHERE order_no = #{orderNo} AND is_deleted = 0")
    int markTimeout(@Param("orderNo") String orderNo);

    @Select("SELECT COUNT(*) FROM t_order WHERE is_deleted = 0 AND created_at >= #{start}")
    long countSince(@Param("start") LocalDateTime start);

    @Select("SELECT COALESCE(SUM(pay_fee), 0) FROM t_order WHERE is_deleted = 0 AND status = 60 AND finish_time >= #{start}")
    BigDecimal gmvSince(@Param("start") LocalDateTime start);

    @Select("SELECT status, COUNT(*) AS cnt FROM t_order WHERE is_deleted = 0 GROUP BY status")
    List<Map<String, Object>> countByStatus();

    @Select("SELECT COALESCE(SUM(pay_fee), 0) FROM t_order WHERE is_deleted = 0 AND status IN (20,30,40,50) AND created_at >= #{start}")
    BigDecimal pendingAmountSince(@Param("start") LocalDateTime start);

    @Select("SELECT c.real_name AS realName, COUNT(*) AS cnt FROM t_order o "
            + "JOIN t_courier c ON c.id = o.courier_id "
            + "WHERE o.is_deleted = 0 AND o.status = 60 AND o.finish_time >= #{start} "
            + "GROUP BY c.id, c.real_name ORDER BY cnt DESC LIMIT 5")
    List<Map<String, Object>> topCouriers(@Param("start") LocalDateTime start);

    @Select("SELECT COUNT(DISTINCT user_id) FROM t_order WHERE is_deleted = 0 AND created_at >= #{start}")
    long activeUsersSince(@Param("start") LocalDateTime start);

    @Select("SELECT COUNT(*) FROM t_order WHERE is_deleted = 0 AND courier_id = #{courierId} AND status IN (30,40,50)")
    long countActiveByCourier(@Param("courierId") Long courierId);
}
