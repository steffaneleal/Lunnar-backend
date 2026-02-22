package com.steffaneleal.lunnar.dto;

import java.util.List;
import java.util.UUID;

public record OrderRequestDTO(List<OrderItemRequestDTO> items, UUID addressId) {
}