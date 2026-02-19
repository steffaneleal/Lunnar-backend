// Acessa os produtos com suporte a filtro e busca por nome
package com.steffaneleal.lunnar.repositories;

import com.steffaneleal.lunnar.models.Product;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ProductRepository extends JpaRepository<Product, UUID> {
    List<Product> findByCategoryId(UUID categoryId);
    List<Product> findByNameContainingIgnoreCase(String name);
}
