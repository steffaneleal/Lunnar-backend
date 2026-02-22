package com.steffaneleal.lunnar.controllers;

import com.steffaneleal.lunnar.dto.*;
import com.steffaneleal.lunnar.models.*;
import com.steffaneleal.lunnar.repositories.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
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
    private final CustomerRepository customerRepository;
    private final UserRepository userRepository;

    @GetMapping
    @Transactional(readOnly = true)
    public ResponseEntity<List<OrderResponseDTO>> list(
            @AuthenticationPrincipal User user,
            @RequestParam(required = false) String status) {

        List<Order> orders;

        if (user.getRole() == UserRole.ADMIN) {
            if (status != null && !status.isBlank()) {
                try {
                    OrderStatus filteredStatus = OrderStatus.valueOf(status.toUpperCase());
                    orders = orderRepository.findByStatusOrderByCreatedAtDesc(filteredStatus);
                } catch (IllegalArgumentException e) {
                    return ResponseEntity.badRequest().build();
                }
            } else {
                orders = orderRepository.findAllByOrderByCreatedAtDesc();
            }
        } else {
            orders = orderRepository.findByUserIdOrderByCreatedAtDesc(user.getId());
        }

        return ResponseEntity.ok(orders.stream().map(this::toDTO).toList());
    }

    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    public ResponseEntity<OrderResponseDTO> getById(@PathVariable UUID id, @AuthenticationPrincipal User user) {
        return orderRepository.findById(id)
                .filter(o -> user.getRole() == UserRole.ADMIN || o.getUser().getId().equals(user.getId()))
                .map(this::toDTO)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @Transactional
    public ResponseEntity<?> create(@RequestBody OrderRequestDTO dto, @AuthenticationPrincipal User authenticatedUser) {
        if (dto.items() == null || dto.items().isEmpty()) {
            return ResponseEntity.badRequest().body("Pedido deve ter pelo menos um item.");
        }
        if (dto.addressId() == null) {
            return ResponseEntity.badRequest().body("Endereço de entrega é obrigatório.");
        }

        User user = userRepository.findById(authenticatedUser.getId())
                .orElseThrow(() -> new IllegalStateException("Usuário não encontrado."));

        Customer customer = customerRepository.findByUserId(user.getId()).orElseGet(() -> {
            Customer c = new Customer();
            c.setUser(user);
            return customerRepository.save(c);
        });

        Address shippingAddress = customer.getAddresses().stream()
                .filter(a -> a.getId().equals(dto.addressId()))
                .findFirst()
                .orElse(null);
        if (shippingAddress == null) {
            return ResponseEntity.badRequest().body("Endereço não encontrado. Cadastre um endereço antes de finalizar o pedido.");
        }

        // --- LOOP 1: VALIDAÇÃO DE ESTOQUE COM BLOQUEIO ---
        for (OrderItemRequestDTO itemDto : dto.items()) {
            Product product = productRepository.findByIdWithLock(itemDto.productId())
                    .orElseThrow(() -> new RuntimeException("Produto não encontrado: " + itemDto.productId()));
            if (product.getStockQuantity() < itemDto.quantity()) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body("Estoque insuficiente para: " + product.getName());
            }
        }

        // --- LOOP: CRIAÇÃO E ATUALIZAÇÃO ---
        Order order = new Order();
        List<OrderItem> items = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;

        for (OrderItemRequestDTO itemDto : dto.items()) {
            Product product = productRepository.findById(itemDto.productId()).get(); // Já validamos, então .get() é seguro

            BigDecimal unitPrice = product.getPrice();
            total = total.add(unitPrice.multiply(BigDecimal.valueOf(itemDto.quantity())));

            OrderItem item = new OrderItem();
            item.setProduct(product);
            item.setQuantity(itemDto.quantity());
            item.setUnitPrice(unitPrice);
            item.setOrder(order);
            items.add(item);

            // Atualiza o estoque
            product.setStockQuantity(product.getStockQuantity() - itemDto.quantity());
            productRepository.save(product);
        }

        order.setUser(user);
        order.setShippingAddress(shippingAddress);
        order.setTotalPrice(total);
        order.setStatus(OrderStatus.PENDENTE);
        order.setItems(items);
        
        Order savedOrder = orderRepository.save(order);

        return ResponseEntity.status(HttpStatus.CREATED).body(toDTO(savedOrder));
    }

    // Cancelamento pelo próprio cliente — apenas pedidos PENDENTE podem ser cancelados pelo cliente
    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<?> cancelByCustomer(@PathVariable UUID id, @AuthenticationPrincipal User authenticatedUser) {
        if (authenticatedUser.getRole() == UserRole.ADMIN) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Administradores devem usar o endpoint de status.");
        }

        return orderRepository.findById(id)
                .map(order -> {
                    if (!order.getUser().getId().equals(authenticatedUser.getId())) {
                        return ResponseEntity.status(HttpStatus.FORBIDDEN).<Object>body("Você não tem permissão para cancelar este pedido.");
                    }
                    if (order.getStatus() != OrderStatus.PENDENTE) {
                        return ResponseEntity.status(HttpStatus.CONFLICT)
                                .<Object>body("Apenas pedidos com status PENDENTE podem ser cancelados. Status atual: " + order.getStatus());
                    }
                    for (OrderItem item : order.getItems()) {
                        Product p = item.getProduct();
                        p.setStockQuantity(p.getStockQuantity() + item.getQuantity());
                        productRepository.save(p);
                    }
                    order.setStatus(OrderStatus.CANCELADO_PELO_CLIENTE);
                    return ResponseEntity.<Object>ok(toDTO(orderRepository.save(order)));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PatchMapping("/{id}/status")
    @Transactional
    public ResponseEntity<?> updateStatus(@PathVariable UUID id, @RequestBody OrderStatusRequestDTO dto, @AuthenticationPrincipal User user) {
        if (user.getRole() != UserRole.ADMIN) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();

        return orderRepository.findById(id)
                .map(order -> {
                    if (order.getStatus() == OrderStatus.CANCELADO_PELO_CLIENTE) {
                        return ResponseEntity.status(HttpStatus.CONFLICT)
                                .<Object>body("Este pedido foi cancelado pelo cliente e não pode ter seu status alterado.");
                    }
                    OrderStatus anterior = order.getStatus();
                    OrderStatus novo = dto.status();
                    if (novo == OrderStatus.CANCELADO_PELO_CLIENTE) {
                        return ResponseEntity.badRequest()
                                .<Object>body("O status CANCELADO_PELO_CLIENTE é exclusivo do cliente e não pode ser definido pelo administrador.");
                    }
                    if (novo == OrderStatus.CANCELADO && anterior != OrderStatus.CANCELADO) {
                        for (OrderItem item : order.getItems()) {
                            Product p = item.getProduct();
                            p.setStockQuantity(p.getStockQuantity() + item.getQuantity());
                            productRepository.save(p);
                        }
                    }
                    if (anterior == OrderStatus.CANCELADO && novo != OrderStatus.CANCELADO) {
                        for (OrderItem item : order.getItems()) {
                            Product p = item.getProduct();
                            if (p.getStockQuantity() < item.getQuantity()) {
                                throw new RuntimeException("Estoque insuficiente para reativar: " + p.getName());
                            }
                            p.setStockQuantity(p.getStockQuantity() - item.getQuantity());
                            productRepository.save(p);
                        }
                    }
                    order.setStatus(novo);
                    return ResponseEntity.<Object>ok(toDTO(orderRepository.save(order)));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    private OrderResponseDTO toDTO(Order order) {
        List<OrderItemResponseDTO> itemDtos = order.getItems() != null
                ? order.getItems().stream().map(i -> new OrderItemResponseDTO(
                i.getProduct().getId(),
                i.getProduct().getName(),
                i.getProduct().getImageUrl(),
                i.getQuantity(),
                i.getUnitPrice(),
                i.getUnitPrice().multiply(BigDecimal.valueOf(i.getQuantity()))
        )).toList()
                : List.of();

        AddressDTO addressDTO = null;
        if (order.getShippingAddress() != null) {
            Address a = order.getShippingAddress();
            addressDTO = new AddressDTO(a.getId(), a.getStreet(), a.getNumber(),
                    a.getComplement(), a.getNeighborhood(), a.getCity(), a.getState(), a.getZipCode());
        }

        return new OrderResponseDTO(
                order.getId(),
                order.getUser().getId(),
                order.getUser().getName(),
                order.getUser().getEmail(),
                order.getTotalPrice(),
                order.getStatus(),
                order.getCreatedAt(),
                itemDtos,
                addressDTO
        );
    }
}
