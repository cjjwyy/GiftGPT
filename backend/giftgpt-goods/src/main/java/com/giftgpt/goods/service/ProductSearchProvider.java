package com.giftgpt.goods.service;

import com.giftgpt.goods.entity.Product;

import java.util.List;

/** 可插拔的商品搜索来源。新增平台时只需实现该接口。 */
public interface ProductSearchProvider {

    String platformName();

    boolean isConfigured();

    List<Product> search(String keyword, int page, int size);
}
