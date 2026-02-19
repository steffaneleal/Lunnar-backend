package com.steffaneleal.lunnar.controllers;

import com.steffaneleal.lunnar.dto.*;
import com.steffaneleal.lunnar.models.*;
import com.steffaneleal.lunnar.repositories.OrderRepository;
import com.steffaneleal.lunnar.repositories.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;

    // Cliente: lista apenas seus pedidos // Admin: lista todos os pedidos.
    @GetMapping
    public ResponseEntity<List<OrderResponseDTO>> list(@AuthenticationPrincipal User user) {
        List<Order> orders;
        if (user.getRole() == UserRole.ADMIN) {
            orders = orderRepository.findAllByOrderByCreatedAtDesc();
        } else {
            orders = orderRepository.findByUserIdOrderByCreatedAtDesc(user.getId());
        }
        List<OrderResponseDTO> dtos = orders.stream().map(this::toDTO).toList();
        return ResponseEntity.ok(dtos);
    }

    // Busca pedido por id. Cliente só vê o próprio; admin vê qualquer um.
    @GetMapping("/{id}")
    public ResponseEntity<OrderResponseDTO> getById(@PathVariable UUID id, @AuthenticationPrincipal User user) {
        return orderRepository.findById(id)
                .filter(order -> user.getRole() == UserRole.ADMIN || order.getUser().getId().equals(user.getId()))
                .map(this::toDTO)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // Cliente: cria pedido (venda) // Desconta do estoque
    @PostMapping
    public ResponseEntity<?> create(@RequestBody OrderRequestDTO dto, @AuthenticationPrincipal User user) {
        if (dto.items() == null || dto.items().isEmpty()) {
            return ResponseEntity.badRequest().body("Pedido deve ter pelo menos um item.");
        }
        List<OrderItem> items = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;

        for (OrderItemRequestDTO itemDto : dto.items()) {
            Product product = productRepository.findById(itemDto.productId())
                    .orElseThrow(() -> new RuntimeException("Produto não encontrado: " + itemDto.productId()));
            if (product.getStockQuantity() < itemDto.quantity()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("Estoque insuficiente para o produto: " + product.getName());
            }
            BigDecimal unitPrice = product.getPrice();
            BigDecimal subtotal = unitPrice.multiply(BigDecimal.valueOf(itemDto.quantity()));
            total = total.add(subtotal);

            OrderItem item = new OrderItem();
            item.setProduct(product);
            item.setQuantity(itemDto.quantity());
            item.setUnitPrice(unitPrice);
            items.add(item);
        }

        Order order = new Order();
        order.setUser(user);
        order.setTotalPrice(total);
        order.setStatus(OrderStatus.PENDENTE);
        for (OrderItem item : items) {
            item.setOrder(order);
        }
        order.setItems(items);
        order = orderRepository.save(order);

        for (OrderItem item : items) {
            Product p = item.getProduct();
            p.setStockQuantity(p.getStockQuantity() - item.getQuantity());
            productRepository.save(p);
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(toDTO(order));
    }

    // Atualiza status do pedido -> APENAS ADMIN
    @PatchMapping("/{id}/status")
    public ResponseEntity<?> updateStatus(@PathVariable UUID id, @RequestBody OrderStatusRequestDTO dto, @AuthenticationPrincipal User user) {
        if (user.getRole() != UserRole.ADMIN) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return orderRepository.findById(id)
                .map(order -> {
                    order.setStatus(dto.status());
                    return ResponseEntity.ok(toDTO(orderRepository.save(order)));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    private OrderResponseDTO toDTO(Order order) {
        List<OrderItemResponseDTO> itemDtos = order.getItems() != null ? order.getItems().stream()
                .map(i -> new OrderItemResponseDTO(
                        i.getProduct().getId(),
                        i.getProduct().getName(),
                        i.getQuantity(),
                        i.getUnitPrice(),
                        i.getUnitPrice().multiply(BigDecimal.valueOf(i.getQuantity()))
                ))
                .toList() : List.of();
        return new OrderResponseDTO(
                order.getId(),
                order.getUser().getId(),
                order.getUser().getName(),
                order.getUser().getEmail(),
                order.getTotalPrice(),
                order.getStatus(),
                order.getCreatedAt(),
                itemDtos
        );
    }
}
