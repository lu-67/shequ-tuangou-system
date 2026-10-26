package com.community.groupbuy.service;

import com.community.groupbuy.common.BusinessException;
import com.community.groupbuy.common.Constants;
import com.community.groupbuy.common.ResultCode;
import com.community.groupbuy.dto.PageResult;
import com.community.groupbuy.dto.ProductDTO;
import com.community.groupbuy.dto.ProductQueryDTO;
import com.community.groupbuy.entity.Product;
import com.community.groupbuy.mapper.ProductMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * M1 商品管理 Service 单元测试（Mockito，不启动 Spring 容器，不依赖数据库）
 *
 * 覆盖：新增成功/默认上架/居民越权/名称为空/团购价高于原价、
 *       修改成功/商品不存在/并发删除、上下架非法状态、删除被活动引用/删除成功、
 *       分页无数据、上架列表。
 */
@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductMapper productMapper;

    /** M7 依赖：商品详情缓存（C 同学） */
    @Mock
    private CacheService cacheService;

    @InjectMocks
    private ProductService productService;

    private ProductDTO dto() {
        ProductDTO dto = new ProductDTO();
        dto.setName("有机小白菜");
        dto.setCategory("蔬菜");
        dto.setSpec("500g/份");
        dto.setPrice(new BigDecimal("6.80"));
        dto.setGroupPrice(new BigDecimal("4.90"));
        dto.setStock(200);
        return dto;
    }

    private Product productOf(Long id) {
        Product p = new Product();
        p.setId(id);
        p.setName("有机小白菜");
        p.setCategory("蔬菜");
        p.setPrice(new BigDecimal("6.80"));
        p.setGroupPrice(new BigDecimal("4.90"));
        p.setStock(200);
        p.setStatus(1);
        return p;
    }

    /** 新增成功：回填主键、默认上架、单位默认「份」 */
    @Test
    void create_success_returnsIdAndDefaults() {
        doAnswer(invocation -> {
            Product arg = invocation.getArgument(0);
            arg.setId(101L);
            return 1;
        }).when(productMapper).insert(any(Product.class));

        Long id = productService.create(dto(), Constants.ROLE_ADMIN);

        assertThat(id).isEqualTo(101L);
        ArgumentCaptor<Product> captor = ArgumentCaptor.forClass(Product.class);
        verify(productMapper).insert(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(1);
        assertThat(captor.getValue().getUnit()).isEqualTo("份");
    }

    /** 越权：居民新增商品应被拒绝 */
    @Test
    void create_residentForbidden_throws403() {
        assertThatThrownBy(() -> productService.create(dto(), Constants.ROLE_RESIDENT))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("code", ResultCode.FORBIDDEN.getCode());
        verify(productMapper, never()).insert(any(Product.class));
    }

    /** 异常分支：商品名称为空白 */
    @Test
    void create_blankName_throwsParamError() {
        ProductDTO dto = dto();
        dto.setName("   ");
        assertThatThrownBy(() -> productService.create(dto, Constants.ROLE_ADMIN))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("code", ResultCode.PARAM_ERROR.getCode());
    }

    /** 异常分支：团购价高于原价 */
    @Test
    void create_groupPriceHigherThanPrice_throws1102() {
        ProductDTO dto = dto();
        dto.setGroupPrice(new BigDecimal("9.90"));
        assertThatThrownBy(() -> productService.create(dto, Constants.ROLE_ADMIN))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("code", ResultCode.PRODUCT_PRICE_ERROR.getCode());
    }

    /** 修改成功 */
    @Test
    void update_success() {
        when(productMapper.selectById(1L)).thenReturn(productOf(1L));
        when(productMapper.update(any(Product.class))).thenReturn(1);

        productService.update(1L, dto(), Constants.ROLE_HEAD);

        ArgumentCaptor<Product> captor = ArgumentCaptor.forClass(Product.class);
        verify(productMapper).update(captor.capture());
        assertThat(captor.getValue().getId()).isEqualTo(1L);
        assertThat(captor.getValue().getName()).isEqualTo("有机小白菜");
    }

    /** 异常分支：修改不存在的商品 */
    @Test
    void update_notFound_throws1101() {
        when(productMapper.selectById(99L)).thenReturn(null);
        assertThatThrownBy(() -> productService.update(99L, dto(), Constants.ROLE_ADMIN))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("code", ResultCode.PRODUCT_NOT_FOUND.getCode());
    }

    /** 并发场景：查询时存在、更新时已被删除（影响行数 0） */
    @Test
    void update_deletedConcurrently_throws1101() {
        when(productMapper.selectById(1L)).thenReturn(productOf(1L));
        when(productMapper.update(any(Product.class))).thenReturn(0);
        assertThatThrownBy(() -> productService.update(1L, dto(), Constants.ROLE_ADMIN))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("code", ResultCode.PRODUCT_NOT_FOUND.getCode());
    }

    /** 上下架：状态值非法 */
    @Test
    void updateStatus_invalidValue_throws1103() {
        assertThatThrownBy(() -> productService.updateStatus(1L, 2, Constants.ROLE_ADMIN))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("code", ResultCode.PRODUCT_STATUS_ERROR.getCode());
        verify(productMapper, never()).updateStatus(anyLong(), any());
    }

    /** 上下架成功 */
    @Test
    void updateStatus_success() {
        when(productMapper.selectById(1L)).thenReturn(productOf(1L));
        when(productMapper.updateStatus(1L, 0)).thenReturn(1);
        productService.updateStatus(1L, 0, Constants.ROLE_ADMIN);
        verify(productMapper).updateStatus(1L, 0);
    }

    /** 异常分支：商品已被团购活动引用，禁止删除 */
    @Test
    void delete_referenced_throws1104() {
        when(productMapper.selectById(1L)).thenReturn(productOf(1L));
        when(productMapper.countActivityRef(1L)).thenReturn(3L);
        assertThatThrownBy(() -> productService.delete(1L, Constants.ROLE_ADMIN))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("code", ResultCode.PRODUCT_REFERENCED.getCode());
        verify(productMapper, never()).deleteById(anyLong());
    }

    /** 删除成功 */
    @Test
    void delete_success() {
        when(productMapper.selectById(1L)).thenReturn(productOf(1L));
        when(productMapper.countActivityRef(1L)).thenReturn(0L);
        when(productMapper.deleteById(1L)).thenReturn(1);
        productService.delete(1L, Constants.ROLE_ADMIN);
        verify(productMapper).deleteById(1L);
    }

    /** 分页：总数为 0 时不再查明细（避免无效 SQL） */
    @Test
    void page_totalZero_skipsDetailQuery() {
        ProductQueryDTO query = new ProductQueryDTO();
        when(productMapper.countByCondition(query)).thenReturn(0L);

        PageResult<Product> result = productService.page(query);

        assertThat(result.getTotal()).isZero();
        assertThat(result.getList()).isEmpty();
        verify(productMapper, never()).selectByCondition(any());
    }

    /** 分页：正常返回总数与列表 */
    @Test
    void page_withData_returnsTotalAndList() {
        ProductQueryDTO query = new ProductQueryDTO();
        when(productMapper.countByCondition(query)).thenReturn(15L);
        when(productMapper.selectByCondition(query)).thenReturn(List.of(productOf(1L), productOf(2L)));

        PageResult<Product> result = productService.page(query);

        assertThat(result.getTotal()).isEqualTo(15L);
        assertThat(result.getList()).hasSize(2);
        assertThat(result.getPages()).isEqualTo(2);
    }

    /** 上架商品列表（首页） */
    @Test
    void listOnShelf_returnsMapperResult() {
        when(productMapper.selectOnShelf()).thenReturn(List.of(productOf(1L)));
        assertThat(productService.listOnShelf()).hasSize(1);
    }
}
