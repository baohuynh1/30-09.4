package com.example.productservice;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

/** Service giả lập, dữ liệu cố định trong bộ nhớ — chỉ để kiểm tra định tuyến của Gateway. */
@RestController
@RequestMapping("/api/products")
public class ProductController {

    private static final List<Product> PRODUCTS = List.of(
            new Product(1L, "Laptop", new BigDecimal("25000000")),
            new Product(2L, "Chuột không dây", new BigDecimal("350000")),
            new Product(3L, "Bàn phím cơ", new BigDecimal("1200000"))
    );

    @GetMapping
    public List<Product> getAll() {
        return PRODUCTS;
    }

    @GetMapping("/{id}")
    public ResponseEntity<Product> getById(@PathVariable Long id) {
        return PRODUCTS.stream()
                .filter(p -> p.id().equals(id))
                .findFirst()
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
