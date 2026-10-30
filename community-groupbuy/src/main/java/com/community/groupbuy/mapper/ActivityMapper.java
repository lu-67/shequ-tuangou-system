package com.community.groupbuy.mapper;

import com.community.groupbuy.dto.ActivityQueryDTO;
import com.community.groupbuy.entity.GroupActivity;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 团购活动 Mapper
 *
 * M3 参团下单依赖：selectById / deductStockCAS / incrementStock（组长实现）
 * M2 开团活动（B 同学）补充：insert / updateStatusCAS / selectByCondition / countByCondition / selectOngoing / selectExpired
 */
@Mapper
public interface ActivityMapper {

    @Select("SELECT * FROM group_activity WHERE id = #{id}")
    GroupActivity selectById(@Param("id") Long id);

    /**
     * 库存扣减（DB CAS，保证不超卖）：
     * 仅当剩余库存 >= 购买量时才扣减，受影响行数=1 表示成功。
     */
    @Update("UPDATE group_activity SET stock = stock - #{qty} WHERE id = #{activityId} AND stock >= #{qty}")
    int deductStockCAS(@Param("activityId") Long activityId, @Param("qty") Integer qty);

    /** 库存回补（退款/取消时使用，M4 支付成团（B 同学）会用到） */
    @Update("UPDATE group_activity SET stock = stock + #{qty} WHERE id = #{activityId}")
    int incrementStock(@Param("activityId") Long activityId, @Param("qty") Integer qty);

    /** M2：创建开团活动，主键回填到 id */
    @Insert("INSERT INTO group_activity(product_id, creator_id, title, origin_price, group_price, total_stock, stock, per_limit, status, start_time, end_time) "
            + "VALUES(#{productId}, #{creatorId}, #{title}, #{originPrice}, #{groupPrice}, #{totalStock}, #{stock}, #{perLimit}, #{status}, #{startTime}, #{endTime})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(GroupActivity activity);

    /**
     * M2：状态流转（CAS）。
     * 带上原状态作为条件，并发下只有一方能更新成功，受影响行数=0 表示状态已被其他操作变更。
     */
    @Update("UPDATE group_activity SET status = #{to} WHERE id = #{id} AND status = #{from}")
    int updateStatusCAS(@Param("id") Long id, @Param("from") String from, @Param("to") String to);

    /** M2：动态条件分页查询，见 ActivityMapper.xml */
    List<GroupActivity> selectByCondition(ActivityQueryDTO query);

    /** M2：动态条件统计总数，见 ActivityMapper.xml */
    long countByCondition(ActivityQueryDTO query);

    /** M2：进行中的活动列表（首页用），按结束时间正序（快结束的排前面） */
    List<GroupActivity> selectOngoing();

    /** 团长/管理员：查询我创建的活动（全部状态），按创建时间倒序 */
    @Select("SELECT * FROM group_activity WHERE creator_id = #{creatorId} ORDER BY id DESC")
    List<GroupActivity> selectByCreator(@Param("creatorId") Long creatorId);

    /** M8：已到期但仍处于 ONGOING 的活动，供定时任务判定成团/失败 */
    List<GroupActivity> selectExpired(@Param("now") LocalDateTime now);

    /**
     * M8：进行中且「已参团份数」已达到 total_stock 的活动，由定时任务批量筛出候选。
     * 已参团份数 = 该活动已支付（PAID）订单的数量汇总，不新增字段（约定：不改表结构）。
     * 这里只做批量筛选，真正的状态流转仍走 ActivityService.markSuccessIfReached 的 CAS。
     */
    @Select("SELECT a.* FROM group_activity a WHERE a.status = 'ONGOING' AND "
            + "COALESCE((SELECT SUM(o.quantity) FROM group_order o "
            + "WHERE o.activity_id = a.id AND o.status = 'PAID'), 0) >= a.total_stock")
    List<GroupActivity> selectReachedPaidTotal();
}
