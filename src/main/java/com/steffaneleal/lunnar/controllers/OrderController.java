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
    public ResponseEntity<List<OrderResponseDTO>> list(@AuthenticationPrincipal User user) {
        List<Order> orders = user.getRole() == UserRole.ADMIN
                ? orderRepository.findAllByOrderByCreatedAtDesc()
                : orderRepository.findByUserIdOrderByCreatedAtDesc(user.getId());
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

        // Busca User gerenciado (evita detached entity)
        User user = userRepository.findById(authenticatedUser.getId())
                .orElseThrow(() -> new IllegalStateException("Usuário não encontrado."));

        // Garante que o Customer existe (cria se necessário)
        Customer customer = customerRepository.findByUserId(user.getId()).orElseGet(() -> {
            Customer c = new Customer();
            c.setUser(user);
            return customerRepository.save(c);
        });

        // Valida que o endereço pertence ao Customer deste usuário
        Address shippingAddress = customer.getAddresses().stream()
                .filter(a -> a.getId().equals(dto.addressId()))
                .findFirst()
                .orElse(null);
        if (shippingAddress == null) {
            return ResponseEntity.badRequest().body("Endereço não encontrado. Cadastre um endereço antes de finalizar o pedido.");
        }

        // Monta itens e calcula total
        List<OrderItem> items = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;
        for (OrderItemRequestDTO itemDto : dto.items()) {
            Product product = productRepository.findById(itemDto.productId())
                    .orElseThrow(() -> new RuntimeException("Produto não encontrado: " + itemDto.productId()));
            if (product.getStockQuantity() < itemDto.quantity()) {
                return ResponseEntity.badRequest().body("Estoque insuficiente para: " + product.getName());
            }
            BigDecimal unitPrice = product.getPrice();
            total = total.add(unitPrice.multiply(BigDecimal.valueOf(itemDto.quantity())));
            OrderItem item = new OrderItem();
            item.setProduct(product);
            item.setQuantity(itemDto.quantity());
            item.setUnitPrice(unitPrice);
            items.add(item);
        }

        Order order = new Order();
        order.setUser(user);
        order.setShippingAddress(shippingAddress);
        order.setTotalPrice(total);
        order.setStatus(OrderStatus.PENDENTE);
        for (OrderItem item : items) item.setOrder(order);
        order.setItems(items);
        order = orderRepository.save(order);

        // Desconta estoque após salvar
        for (OrderItem item : order.getItems()) {
            Product p = item.getProduct();
            p.setStockQuantity(p.getStockQuantity() - item.getQuantity());
            productRepository.save(p);
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(toDTO(order));
    }

    @PatchMapping("/{id}/status")
    @Transactional
    public ResponseEntity<?> updateStatus(@PathVariable UUID id, @RequestBody OrderStatusRequestDTO dto, @AuthenticationPrincipal User user) {
        if (user.getRole() != UserRole.ADMIN) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();

        return orderRepository.findById(id)
                .map(order -> {
                    OrderStatus anterior = order.getStatus();
                    OrderStatus novo = dto.status();

                    // Cancelamento: devolve estoque (apenas se não estava cancelado antes)
                    if (novo == OrderStatus.CANCELADO && anterior != OrderStatus.CANCELADO) {
                        for (OrderItem item : order.getItems()) {
                            Product p = item.getProduct();
                            p.setStockQuantity(p.getStockQuantity() + item.getQuantity());
                            productRepository.save(p);
                        }
                    }

                    // Reativação: desconta estoque novamente (sai de CANCELADO para outro status)
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
                    return ResponseEntity.ok(toDTO(orderRepository.save(order)));
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