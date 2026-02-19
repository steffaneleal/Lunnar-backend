package com.steffaneleal.lunnar.controllers;

import com.steffaneleal.lunnar.dto.CustomerDTO;
import com.steffaneleal.lunnar.dto.CustomerReportDTO;
import com.steffaneleal.lunnar.dto.CustomerUpdateDTO;
import com.steffaneleal.lunnar.models.Customer;
import com.steffaneleal.lunnar.models.User;
import com.steffaneleal.lunnar.models.UserRole;
import com.steffaneleal.lunnar.repositories.CustomerRepository;
import com.steffaneleal.lunnar.services.CustomerReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Comparator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

@RestController
@RequestMapping("/customers")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerRepository customerRepository;
    private final CustomerReportService customerReportService;

    // Lista todos os clientes (CRM) - Apenas ADMIN
    @GetMapping
    public ResponseEntity<List<CustomerDTO>> listAll(@AuthenticationPrincipal User user) {
        if (user.getRole() != UserRole.ADMIN) {
            return ResponseEntity.status(403).build();
        }
        List<CustomerDTO> list = customerRepository.findAll().stream()
                .sorted(Comparator.comparing(c -> c.getUser().getName(), String.CASE_INSENSITIVE_ORDER))
                .map(this::toDTO)
                .toList();
        return ResponseEntity.ok(list);
    }

    // Busca cliente por id
    // Admin: qualquer um // Cliente: apenas o próprio (por userId)
    @GetMapping("/{id}")
    public ResponseEntity<CustomerDTO> getById(@PathVariable UUID id, @AuthenticationPrincipal User user) {
        return customerRepository.findById(id)
                .filter(c -> user.getRole() == UserRole.ADMIN || c.getUser().getId().equals(user.getId()))
                .map(this::toDTO)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // Retorna o perfil de cliente do usuário logado e cria registro Customer se não existir (para USER)
    @GetMapping("/me")
    public ResponseEntity<CustomerDTO> getMyProfile(@AuthenticationPrincipal User user) {
        if (user.getRole() != UserRole.USER) {
            return ResponseEntity.status(403).body(null);
        }
        Customer customer = customerRepository.findByUserId(user.getId()).orElseGet(() -> {
            Customer newCustomer = new Customer();
            newCustomer.setUser(user);
            return customerRepository.save(newCustomer);
        });
        return ResponseEntity.ok(toDTO(customer));
    }

    // Relatório do cliente: compras, valores, categorias, tendência por dia da semana -> APENAS ADMIN
    @GetMapping("/{id}/report")
    public ResponseEntity<?> getReport(@PathVariable UUID id, @AuthenticationPrincipal User user) {
        if (user.getRole() != UserRole.ADMIN) {
            return ResponseEntity.status(403).build();
        }
        try {
            CustomerReportDTO report = customerReportService.generateReport(id);
            return ResponseEntity.ok(report);
        } catch (NoSuchElementException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // Atualiza dados de relacionamento do cliente (empresa, notas, último contato) -> APENAS ADMIN
    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable UUID id, @RequestBody CustomerUpdateDTO dto, @AuthenticationPrincipal User user) {
        if (user.getRole() != UserRole.ADMIN) {
            return ResponseEntity.status(403).build();
        }
        return customerRepository.findById(id)
                .map(c -> {
                    if (dto.companyName() != null) c.setCompanyName(dto.companyName());
                    if (dto.notes() != null) c.setNotes(dto.notes());
                    if (dto.lastContactAt() != null) c.setLastContactAt(dto.lastContactAt());
                    c.setUpdatedAt(java.time.LocalDateTime.now());
                    return ResponseEntity.ok(toDTO(customerRepository.save(c)));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    private CustomerDTO toDTO(Customer c) {
        User u = c.getUser();
        return new CustomerDTO(
                c.getId(),
                u.getId(),
                u.getName(),
                u.getEmail(),
                u.getPhoneNumber(),
                c.getCompanyName(),
                c.getNotes(),
                c.getLastContactAt(),
                c.getCreatedAt()
        );
    }
}
