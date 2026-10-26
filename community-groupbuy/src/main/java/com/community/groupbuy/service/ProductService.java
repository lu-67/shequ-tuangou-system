package com.community.groupbuy.service;

import com.community.groupbuy.common.BusinessException;
import com.community.groupbuy.common.Constants;
import com.community.groupbuy.common.ResultCode;
import com.community.groupbuy.dto.PageResult;
import com.community.groupbuy.dto.ProductDTO;
import com.community.groupbuy.dto.ProductQueryDTO;
import com.community.groupbuy.entity.Product;
import com.community.groupbuy.mapper.ProductMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * M1 商品管理：CRUD + 动态条件分页 + 上架商品列表
 *
 * 约定：
 * - 写操作仅 ADMIN / HEAD，权限在 Service 内断言（与 ActivityService 一致，不依赖 M0 落地）；
 * - 被团购活动引用的商品禁止物理删除，避免活动详情出现空商品；
 * - 商品删除/更新走单表事务，跨表校验只读 group_activity，不改活动模块任何代码。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProductService {

    private static final int STATUS_ON_SHELF = 1;
    private static final int STATUS_OFF_SHELF = 0;

    private final ProductMapper productMapper;

    /** 新增商品，默认上架 */
    @Transactional(rollbackFor = Exception.class)
    public Long create(ProductDTO dto, String operatorRole) {
        assertAdminOrHead(operatorRole);
        validate(dto);
        Product product = buildProduct(dto);
        productMapper.insert(product);
        log.info("商品创建成功: id={}, name={}, operator={}", product.getId(), product.getName(), operatorRole);
        return product.getId();
    }

    /** 修改商品（全字段覆盖，create_time 不变） */
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, ProductDTO dto, String operatorRole) {
        assertAdminOrHead(operatorRole);
        validate(dto);
        requireProduct(id);
        Product product = buildProduct(dto);
        product.setId(id);
        int rows = productMapper.update(product);
        if (rows != 1) {
            throw new BusinessException(ResultCode.PRODUCT_NOT_FOUND, "商品已被删除，更新失败");
        }
        log.info("商品更新成功: id={}, name={}, operator={}", id, product.getName(), operatorRole);
    }

    /** 上架 / 下架 */
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Long id, Integer status, String operatorRole) {
        assertAdminOrHead(operatorRole);
        if (status == null || (status != STATUS_ON_SHELF && status != STATUS_OFF_SHELF)) {
            throw new BusinessException(ResultCode.PRODUCT_STATUS_ERROR, "商品状态只能为 1（上架）或 0（下架）");
        }
        requireProduct(id);
        int rows = productMapper.updateStatus(id, status);
        if (rows != 1) {
            throw new BusinessException(ResultCode.PRODUCT_NOT_FOUND, "商品已被删除，操作失败");
        }
        log.info("商品上下架: id={}, status={}, operator={}", id, status, operatorRole);
    }

    /** 删除商品：被活动引用时拒绝 */
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id, String operatorRole) {
        assertAdminOrHead(operatorRole);
        requireProduct(id);
        long refs = productMapper.countActivityRef(id);
        if (refs > 0) {
            throw new BusinessException(ResultCode.PRODUCT_REFERENCED,
                    "该商品已被 " + refs + " 个团购活动引用，请改为下架");
        }
        productMapper.deleteById(id);
        log.info("商品删除成功: id={}, operator={}", id, operatorRole);
    }

    public Product getById(Long id) {
        return requireProduct(id);
    }

    /** 动态条件分页查询（管理后台） */
    public PageResult<Product> page(ProductQueryDTO query) {
        long total = productMapper.countByCondition(query);
        List<Product> list = total == 0 ? List.of() : productMapper.selectByCondition(query);
        return PageResult.of(total,
                query.getPageNum() == null ? 1 : query.getPageNum(),
                query.getPageSize() == null ? 10 : query.getPageSize(),
                list);
    }

    /** 上架商品（居民端首页 / 开团弹窗下拉） */
    public List<Product> listOnShelf() {
        return productMapper.selectOnShelf();
    }

    private Product requireProduct(Long id) {
        Product product = productMapper.selectById(id);
        if (product == null) {
            throw new BusinessException(ResultCode.PRODUCT_NOT_FOUND);
        }
        return product;
    }

    private void assertAdminOrHead(String role) {
        if (!Constants.ROLE_ADMIN.equals(role) && !Constants.ROLE_HEAD.equals(role)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "仅管理员或团长可管理商品");
        }
    }

    /** 跨字段校验：团购价不得高于原价 */
    private void validate(ProductDTO dto) {
        if (dto.getName() == null || dto.getName().trim().isEmpty()) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "商品名称不能为空");
        }
        if (dto.getPrice() == null || dto.getGroupPrice() == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "原价与团购价不能为空");
        }
        if (dto.getGroupPrice().compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "团购价不能为负数");
        }
        if (dto.getGroupPrice().compareTo(dto.getPrice()) > 0) {
            throw new BusinessException(ResultCode.PRODUCT_PRICE_ERROR);
        }
        if (dto.getStock() == null || dto.getStock() < 0) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "库存不能为负数");
        }
    }

    private Product buildProduct(ProductDTO dto) {
        Product product = new Product();
        product.setName(dto.getName().trim());
        product.setCategory(dto.getCategory() == null ? "" : dto.getCategory());
        product.setSpec(dto.getSpec() == null ? "" : dto.getSpec());
        product.setUnit(dto.getUnit() == null || dto.getUnit().isEmpty() ? "份" : dto.getUnit());
        product.setPrice(dto.getPrice());
        product.setGroupPrice(dto.getGroupPrice());
        product.setStock(dto.getStock());
        product.setImageUrl(dto.getImageUrl() == null ? "" : dto.getImageUrl());
        product.setDescription(dto.getDescription() == null ? "" : dto.getDescription());
        product.setStatus(dto.getStatus() == null ? STATUS_ON_SHELF : dto.getStatus());
        return product;
    }
}
