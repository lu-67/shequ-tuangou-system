package com.community.groupbuy.mapper;

import com.community.groupbuy.entity.PickupPoint;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 自提点 Mapper（M5 自提点与核销，C 同学）
 *
 * 动态条件查询与新增放在 PickupPointMapper.xml，简单单表语句用注解。
 */
@Mapper
public interface PickupPointMapper {

    @Select("SELECT * FROM pickup_point WHERE id = #{id}")
    PickupPoint selectById(@Param("id") Long id);

    @Delete("DELETE FROM pickup_point WHERE id = #{id}")
    int deleteById(@Param("id") Long id);

    @Update("UPDATE pickup_point SET name = #{name}, address = #{address}, manager = #{manager}, phone = #{phone} "
            + "WHERE id = #{id}")
    int update(PickupPoint point);

    /** M5：新增自提点，主键回填（XML insert） */
    int insert(PickupPoint point);

    /** M5：按名称模糊的条件查询（XML 动态 SQL），居民端首页不传条件即全量 */
    List<PickupPoint> selectByCondition(@Param("name") String name);

    /**
     * M5：统计引用该自提点的核销记录数（只读 pickup_verification）。
     * 已有核销记录的自提点不允许删除，避免核销记录变成孤儿数据。
     */
    @Select("SELECT COUNT(*) FROM pickup_verification WHERE pickup_point_id = #{pointId}")
    long countVerificationRef(@Param("pointId") Long pointId);
}
