package com.steffaneleal.lunnar.dto;

import java.util.List;

public record OrderRequestDTO(List<OrderItemRequestDTO> items) {
}
