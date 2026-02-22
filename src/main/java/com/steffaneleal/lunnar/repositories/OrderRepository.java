// Acessa os pedidos por ordem de mais recente, status (todos os pendentes, por exemplo) ou data para o admin
package com.steffaneleal.lunnar.repositories;

import com.steffaneleal.lunnar.models.Order;
import com.steffaneleal.lunnar.models.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface OrderRepository extends JpaRepository<Order, UUID> {
    List<Order> findByUserIdOrderByCreatedAtDesc(UUID userId);
    List<Order> findByStatus(OrderStatus status);
    List<Order> findByStatusOrderByCreatedAtDesc(OrderStatus status);
    List<Order> findAllByOrderByCreatedAtDesc();
    boolean existsByShippingAddressId(UUID addressId);
}