package com.steffaneleal.lunnar.controllers;

import com.steffaneleal.lunnar.dto.ProductCategoryDTO;
import com.steffaneleal.lunnar.dto.ProductDTO;
import com.steffaneleal.lunnar.dto.ProductRequestDTO;
import com.steffaneleal.lunnar.dto.StockUpdateDTO;
import com.steffaneleal.lunnar.models.Category;
import com.steffaneleal.lunnar.models.Product;
import com.steffaneleal.lunnar.repositories.CategoryRepository;
import com.steffaneleal.lunnar.repositories.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    @GetMapping
    public ResponseEntity<List<ProductDTO>> listAll(
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false) String search
    ) {
        List<Product> list;
        if (categoryId != null && search != null && !search.isBlank()) {
            list = productRepository.findByCategoryId(categoryId).stream()
                    .filter(p -> p.getName() != null && p.getName().toLowerCase().contains(search.trim().toLowerCase()))
                    .toList();
        } else if (categoryId != null) {
            list = productRepository.findByCategoryId(categoryId);
        } else if (search != null && !search.isBlank()) {
            list = productRepository.findByNameContainingIgnoreCase(search.trim());
        } else {
            list = productRepository.findAll();
        }
        return ResponseEntity.ok(list.stream().map(this::toDTO).toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductDTO> getById(@PathVariable UUID id) {
        return productRepository.findById(id)
                .map(this::toDTO)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody ProductRequestDTO dto) {
        Product p = new Product();
        p.setName(dto.name());
        p.setDescription(dto.description());
        p.setPrice(dto.price());
        p.setStockQuantity(dto.stockQuantity() != null ? dto.stockQuantity() : 0);
        p.setImageUrl(dto.imageUrl());
        p.setCategories(resolveCategories(dto.categoryIds()));
        return ResponseEntity.status(HttpStatus.CREATED).body(toDTO(productRepository.save(p)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable UUID id, @RequestBody ProductRequestDTO dto) {
        return productRepository.findById(id)
                .map(p -> {
                    p.setName(dto.name());
                    p.setDescription(dto.description());
                    p.setPrice(dto.price());
                    if (dto.stockQuantity() != null) p.setStockQuantity(dto.stockQuantity());
                    p.setImageUrl(dto.imageUrl());
                    p.setCategories(resolveCategories(dto.categoryIds()));
                    return ResponseEntity.ok(toDTO(productRepository.save(p)));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PatchMapping("/{id}/stock")
    public ResponseEntity<?> updateStock(@PathVariable UUID id, @RequestBody StockUpdateDTO dto) {
        return productRepository.findById(id)
                .map(p -> {
                    p.setStockQuantity(dto.stockQuantity());
                    return ResponseEntity.ok(toDTO(productRepository.save(p)));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        if (!productRepository.existsById(id)) return ResponseEntity.notFound().build();
        productRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    private Set<Category> resolveCategories(List<UUID> ids) {
        if (ids == null || ids.isEmpty()) return new HashSet<>();
        return new HashSet<>(categoryRepository.findAllById(ids));
    }

    private ProductDTO toDTO(Product p) {
        List<ProductCategoryDTO> cats = p.getCategories() == null ? List.of() :
                p.getCategories().stream()
                        .map(c -> new ProductCategoryDTO(c.getId(), c.getName()))
                        .toList();
        return new ProductDTO(p.getId(), p.getName(), p.getDescription(),
                p.getPrice(), p.getStockQuantity(), p.getImageUrl(), cats);
    }
}