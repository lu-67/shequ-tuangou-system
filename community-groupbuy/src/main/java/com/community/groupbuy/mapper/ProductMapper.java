package com.community.groupbuy.mapper;

import com.community.groupbuy.dto.ProductQueryDTO;
import com.community.groupbuy.entity.Product;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 商品 Mapper（M1 商品管理，C 同学）
 *
 * 动态条件查询 / 插入 / 更新见 ProductMapper.xml；
 * 本文件只保留单表简单语句（注解方式），与 ActivityMapper 的风格保持一致。
 */
@Mapper
public interface ProductMapper {

    @Select("SELECT * FROM product WHERE id = #{id}")
    Product selectById(@Param("id") Long id);

    @Delete("DELETE FROM product WHERE id = #{id}")
    int deleteById(@Param("id") Long id);

    /** M1：上架 / 下架（只改 status 一列，避免整行覆盖） */
    @Update("UPDATE product SET status = #{status} WHERE id = #{id}")
    int updateStatus(@Param("id") Long id, @Param("status") Integer status);

    /** M1：新增商品，主键回填（XML insert，useGeneratedKeys） */
    int insert(Product product);

    /** M1：全字段更新（XML update） */
    int update(Product product);

    /** M1：动态条件分页查询（XML 手写动态 SQL） */
    List<Product> selectByCondition(ProductQueryDTO query);

    /** M1：动态条件统计总数，与查询共用同一套条件，保证分页 total 一致 */
    long countByCondition(ProductQueryDTO query);

    /** M1：上架商品列表（居民端首页，不分页） */
    List<Product> selectOnShelf();

    /**
     * M1：统计引用该商品的团购活动数量（只读 group_activity，不修改 B 的任何代码）。
     * 被活动引用的商品不允许物理删除，避免活动详情出现空商品。
     */
    long countActivityRef(@Param("productId") Long productId);
}
