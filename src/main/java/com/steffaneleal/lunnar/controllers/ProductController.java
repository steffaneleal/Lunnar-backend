package com.steffaneleal.lunnar.controllers;

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

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    // Lista todos os produtos e filtro por categoria e/ou busca por nome (integrados)
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
        List<ProductDTO> dtos = list.stream().map(this::toDTO).toList();
        return ResponseEntity.ok(dtos);
    }

    // Busca produto por id
    @GetMapping("/{id}")
    public ResponseEntity<ProductDTO> getById(@PathVariable UUID id) {
        return productRepository.findById(id)
                .map(this::toDTO)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // Cria produto -> APENAS ADMIN
    @PostMapping
    public ResponseEntity<?> create(@RequestBody ProductRequestDTO dto) {
        Category category = null;
        if (dto.categoryId() != null) {
            category = categoryRepository.findById(dto.categoryId()).orElse(null);
        }
        Product p = new Product();
        p.setName(dto.name());
        p.setDescription(dto.description());
        p.setPrice(dto.price());
        p.setStockQuantity(dto.stockQuantity() != null ? dto.stockQuantity() : 0);
        p.setCategory(category);
        p = productRepository.save(p);
        return ResponseEntity.status(HttpStatus.CREATED).body(toDTO(p));
    }

    // Atualiza produto -> APENAS ADMIN
    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable UUID id, @RequestBody ProductRequestDTO dto) {
        return productRepository.findById(id)
                .map(p -> {
                    p.setName(dto.name());
                    p.setDescription(dto.description());
                    p.setPrice(dto.price());
                    if (dto.stockQuantity() != null) {
                        p.setStockQuantity(dto.stockQuantity());
                    }
                    if (dto.categoryId() != null) {
                        categoryRepository.findById(dto.categoryId()).ifPresent(p::setCategory);
                    } else {
                        p.setCategory(null);
                    }
                    return ResponseEntity.ok(toDTO(productRepository.save(p)));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // Atualiza apenas estoque -> APENAS ADMIN
    @PatchMapping("/{id}/stock")
    public ResponseEntity<?> updateStock(@PathVariable UUID id, @RequestBody StockUpdateDTO dto) {
        return productRepository.findById(id)
                .map(p -> {
                    p.setStockQuantity(dto.stockQuantity());
                    return ResponseEntity.ok(toDTO(productRepository.save(p)));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // Remove produto -> APENAS ADMIN
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        if (!productRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        productRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    private ProductDTO toDTO(Product p) {
        return new ProductDTO(
                p.getId(),
                p.getName(),
                p.getDescription(),
                p.getPrice(),
                p.getStockQuantity(),
                p.getCategory() != null ? p.getCategory().getId() : null,
                p.getCategory() != null ? p.getCategory().getName() : null,
                p.getImageUrl()
        );
    }
}
