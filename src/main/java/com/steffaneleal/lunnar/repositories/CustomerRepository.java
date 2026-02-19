// Acessa a tabela tb_customer para listagem de clientes
package com.steffaneleal.lunnar.repositories;

import com.steffaneleal.lunnar.models.Customer;
import com.steffaneleal.lunnar.models.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CustomerRepository extends JpaRepository<Customer, UUID> {
    Optional<Customer> findByUser(User user);
    Optional<Customer> findByUserId(UUID userId);
    List<Customer> findAll();
}
