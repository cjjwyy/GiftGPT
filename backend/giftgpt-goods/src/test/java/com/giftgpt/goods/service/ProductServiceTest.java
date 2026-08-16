package com.giftgpt.goods.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.giftgpt.goods.dto.ProductSearchRequest;
import com.giftgpt.goods.entity.Product;
import com.giftgpt.goods.mapper.ProductMapper;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ProductServiceTest {

    @Test
    @SuppressWarnings("unchecked")
    void mergedSearchTotalShouldUseExternalSizePlusDbTotal() {
        ProductMapper productMapper = mock(ProductMapper.class);
        CommerceService commerceService = mock(CommerceService.class);

        Product external = new Product();
        external.setName("蓝牙耳机");
        external.setPrice(new BigDecimal("199.00"));
        external.setPlatform("拼多多");
        external.setStatus(1);

        Product cached = new Product();
        cached.setName("耳机收纳盒");
        cached.setPrice(new BigDecimal("29.90"));
        cached.setPlatform("拼多多");
        cached.setStatus(1);

        when(commerceService.searchAcrossPlatforms("耳机", 1, 10)).thenReturn(List.of(external));

        Page<Product> dbPage = new Page<>(1, 10);
        dbPage.setRecords(List.of(cached));
        dbPage.setTotal(5);
        when(productMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(dbPage);

        ProductService service = new ProductService(productMapper, commerceService);
        ProductSearchRequest request = new ProductSearchRequest();
        request.setKeyword("耳机");

        Page<Product> result = service.search(request, 1, 10);

        assertEquals(6L, result.getTotal());
        assertEquals(2, result.getRecords().size());
    }
}
