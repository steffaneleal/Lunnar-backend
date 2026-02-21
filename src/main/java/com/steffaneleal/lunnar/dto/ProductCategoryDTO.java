// DTO simples de categoria usado dentro de ProductDTOpackage com.steffaneleal.lunnar.dto;
package com.steffaneleal.lunnar.dto;
import java.util.UUID;

public record ProductCategoryDTO(UUID id, String name) {
}