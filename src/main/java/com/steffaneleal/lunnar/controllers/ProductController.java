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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    @GetMapping
    public ResponseEntity<List<ProductDTO>> listAll(
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String sortBy
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

        if ("price_asc".equals(sortBy)) {
            list = list.stream().sorted(Comparator.comparing(Product::getPrice)).toList();
        } else if ("price_desc".equals(sortBy)) {
            list = list.stream().sorted(Comparator.comparing(Product::getPrice).reversed()).toList();
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
        // Validação backend: nome, descrição e preço são obrigatórios
        if (dto.name() == null || dto.name().isBlank())
            return ResponseEntity.badRequest().body("O nome do produto é obrigatório.");
        if (dto.description() == null || dto.description().isBlank())
            return ResponseEntity.badRequest().body("A descrição do produto é obrigatória.");
        if (dto.price() == null || dto.price().signum() <= 0)
            return ResponseEntity.badRequest().body("O preço do produto deve ser maior que zero.");

        Product p = new Product();
        p.setName(dto.name().trim());
        p.setDescription(dto.description().trim());
        p.setPrice(dto.price());
        p.setStockQuantity(dto.stockQuantity() != null ? dto.stockQuantity() : 0);
        p.setImageUrl(dto.imageUrl());
        p.setCategories(resolveCategories(dto.categoryIds()));
        return ResponseEntity.status(HttpStatus.CREATED).body(toDTO(productRepository.save(p)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable UUID id, @RequestBody ProductRequestDTO dto) {
        if (dto.name() == null || dto.name().isBlank())
            return ResponseEntity.badRequest().body("O nome do produto é obrigatório.");
        if (dto.description() == null || dto.description().isBlank())
            return ResponseEntity.badRequest().body("A descrição do produto é obrigatória.");
        if (dto.price() == null || dto.price().signum() <= 0)
            return ResponseEntity.badRequest().body("O preço do produto deve ser maior que zero.");

        return productRepository.findById(id)
                .map(p -> {
                    p.setName(dto.name().trim());
                    p.setDescription(dto.description().trim());
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
    public ResponseEntity<?> delete(@PathVariable UUID id) {
        if (!productRepository.existsById(id)) return ResponseEntity.notFound().build();
        try {
            productRepository.deleteById(id);
            return ResponseEntity.noContent().build();
        } catch (DataIntegrityViolationException e) {
            // Produto está referenciado em pedidos — não pode ser excluído
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body("Este produto possui pedidos associados e não pode ser excluído.");
        }
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