package com.steffaneleal.lunnar.controllers;

import com.steffaneleal.lunnar.dto.CategoryDTO;
import com.steffaneleal.lunnar.dto.CategoryRequestDTO;
import com.steffaneleal.lunnar.models.Category;
import com.steffaneleal.lunnar.repositories.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryRepository categoryRepository;

    // Lista todas as categorias, clientes e admin
    @GetMapping
    public ResponseEntity<List<CategoryDTO>> listAll() {
        List<CategoryDTO> list = categoryRepository.findAll().stream()
                .map(this::toDTO)
                .toList();
        return ResponseEntity.ok(list);
    }

    // Busca categoria por id
    @GetMapping("/{id}")
    public ResponseEntity<CategoryDTO> getById(@PathVariable UUID id) {
        return categoryRepository.findById(id)
                .map(this::toDTO)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // Cria categoria -> APENAS ADMIN
    @PostMapping
    public ResponseEntity<?> create(@RequestBody CategoryRequestDTO dto) {
        if (categoryRepository.existsByNameIgnoreCase(dto.name())) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Categoria já existe com esse nome.");
        }
        Category cat = new Category();
        cat.setName(dto.name());
        cat.setDescription(dto.description());
        cat = categoryRepository.save(cat);
        return ResponseEntity.status(HttpStatus.CREATED).body(toDTO(cat));
    }

    // Atualiza categoria -> APENAS ADMIN
    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable UUID id, @RequestBody CategoryRequestDTO dto) {
        return categoryRepository.findById(id)
                .map(cat -> {
                    cat.setName(dto.name());
                    cat.setDescription(dto.description());
                    return ResponseEntity.ok(toDTO(categoryRepository.save(cat)));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // Remove categoria -> APENAS ADMIN
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        if (!categoryRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        categoryRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    private CategoryDTO toDTO(Category c) {
        return new CategoryDTO(c.getId(), c.getName(), c.getDescription());
    }
}
