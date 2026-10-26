package com.community.groupbuy.controller;

import com.community.groupbuy.common.Result;
import com.community.groupbuy.common.SecurityUtils;
import com.community.groupbuy.dto.PageResult;
import com.community.groupbuy.dto.ProductDTO;
import com.community.groupbuy.dto.ProductQueryDTO;
import com.community.groupbuy.entity.Product;
import com.community.groupbuy.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * M1 商品管理接口
 *
 * 路径约定（与前端保持一致）：
 * - GET  /api/products        分页 + 动态条件（管理后台）：返回 { total, pageNum, pageSize, list }
 * - GET  /api/products/list   上架商品数组（居民端首页 / 开团弹窗下拉）
 * - GET  /api/products/{id}   商品详情
 * - POST/PUT/DELETE           写接口，仅 ADMIN / HEAD
 */
@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    /** 分页 + 动态条件查询（管理后台） */
    @GetMapping
    public Result<PageResult<Product>> page(ProductQueryDTO query) {
        return Result.success(productService.page(query));
    }

    /** 上架商品列表（居民端首页） */
    @GetMapping("/list")
    public Result<List<Product>> list() {
        return Result.success(productService.listOnShelf());
    }

    @GetMapping("/{id}")
    public Result<Product> get(@PathVariable Long id) {
        return Result.success(productService.getById(id));
    }

    @PostMapping
    public Result<Long> create(@Valid @RequestBody ProductDTO dto) {
        return Result.success(productService.create(dto, SecurityUtils.getRole()));
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody ProductDTO dto) {
        productService.update(id, dto, SecurityUtils.getRole());
        return Result.success("商品已更新");
    }

    /** 上架 / 下架：status 走 URL 参数，与活动状态流转接口保持一致 */
    @PutMapping("/{id}/status")
    public Result<Void> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        productService.updateStatus(id, status, SecurityUtils.getRole());
        return Result.success("商品状态已更新");
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        productService.delete(id, SecurityUtils.getRole());
        return Result.success("商品已删除");
    }
}
