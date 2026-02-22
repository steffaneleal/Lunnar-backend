package com.steffaneleal.lunnar.controllers;

import com.steffaneleal.lunnar.dto.AddressDTO;
import com.steffaneleal.lunnar.dto.CustomerDTO;
import com.steffaneleal.lunnar.dto.CustomerReportDTO;
import com.steffaneleal.lunnar.dto.CustomerUpdateDTO;
import com.steffaneleal.lunnar.models.Address;
import com.steffaneleal.lunnar.models.Customer;
import com.steffaneleal.lunnar.models.User;
import com.steffaneleal.lunnar.models.UserRole;
import com.steffaneleal.lunnar.repositories.CustomerRepository;
import com.steffaneleal.lunnar.repositories.OrderRepository;
import com.steffaneleal.lunnar.repositories.UserRepository;
import com.steffaneleal.lunnar.services.CustomerReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
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
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final CustomerReportService customerReportService;

    // Lista todos os clientes (CRM) - Apenas ADMIN
    @GetMapping
    public ResponseEntity<List<CustomerDTO>> listAll(@AuthenticationPrincipal User user) {
        if (user.getRole() != UserRole.ADMIN) return ResponseEntity.status(403).build();
        List<CustomerDTO> list = customerRepository.findAll().stream()
                .filter(c -> c.getUser().getRole() == UserRole.USER)
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
            return ResponseEntity.noContent().build();
        }
        Customer customer = customerRepository.findByUserId(user.getId()).orElseGet(() -> {
            Customer newCustomer = new Customer();
            newCustomer.setUser(user);
            return customerRepository.save(newCustomer);
        });
        return ResponseEntity.ok(toDTO(customer));
    }

    @PutMapping("/me")
    @Transactional
    public ResponseEntity<CustomerDTO> updateMyProfile(@RequestBody CustomerUpdateDTO dto, @AuthenticationPrincipal User authenticatedUser) {
        if (authenticatedUser.getRole() != UserRole.USER) return ResponseEntity.status(403).build();
        User user = userRepository.findById(authenticatedUser.getId())
                .orElseThrow(() -> new IllegalStateException("Usuario nao encontrado."));
        Customer customer = customerRepository.findByUserId(user.getId()).orElseGet(() -> {
            Customer c = new Customer();
            c.setUser(user);
            return c;
        });
        if (dto.companyName() != null) customer.setCompanyName(dto.companyName());
        if (dto.phoneNumber() != null) user.setPhoneNumber(dto.phoneNumber());
        customer.setUpdatedAt(java.time.LocalDateTime.now());
        return ResponseEntity.ok(toDTO(customerRepository.save(customer)));
    }

    // Relatório do cliente: compras, valores, categorias, tendência por dia da semana -> APENAS ADMIN
    @GetMapping("/{id}/report")
    public ResponseEntity<?> getReport(@PathVariable UUID id, @AuthenticationPrincipal User user) {
        if (user.getRole() != UserRole.ADMIN) return ResponseEntity.status(403).build();
        try {
            return ResponseEntity.ok(customerReportService.generateReport(id));
        } catch (NoSuchElementException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // Atualiza dados de relacionamento do cliente (empresa, notas, último contato) -> APENAS ADMIN
    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable UUID id, @RequestBody CustomerUpdateDTO dto, @AuthenticationPrincipal User user) {
        if (user.getRole() != UserRole.ADMIN) return ResponseEntity.status(403).build();
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

    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<Void> delete(@PathVariable UUID id, @AuthenticationPrincipal User user) {
        if (user.getRole() != UserRole.ADMIN) return ResponseEntity.status(403).build();
        return customerRepository.findById(id)
                .map(c -> {
                    customerRepository.delete(c);
                    return ResponseEntity.noContent().<Void>build();
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // Admin nao tem enderecos - retorna lista vazia em vez de erro
    @GetMapping("/me/addresses")
    public ResponseEntity<List<AddressDTO>> getMyAddresses(@AuthenticationPrincipal User user) {
        if (user.getRole() != UserRole.USER) return ResponseEntity.ok(List.of());
        return customerRepository.findByUserId(user.getId())
                .map(c -> c.getAddresses().stream().map(this::toAddressDTO).toList())
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.ok(List.of()));
    }

    @PostMapping("/me/addresses")
    public ResponseEntity<AddressDTO> addMyAddress(@AuthenticationPrincipal User user, @RequestBody AddressDTO dto) {
        Customer customer = customerRepository.findByUserId(user.getId()).orElseGet(() -> {
            Customer c = new Customer();
            c.setUser(user);
            return customerRepository.save(c);
        });
        Address address = new Address();
        address.setStreet(dto.street());
        address.setNumber(dto.number());
        address.setComplement(dto.complement());
        address.setNeighborhood(dto.neighborhood());
        address.setCity(dto.city());
        address.setState(dto.state());
        address.setZipCode(dto.zip_code());
        customer.getAddresses().add(address);
        customerRepository.save(customer);
        Address saved = customer.getAddresses().get(customer.getAddresses().size() - 1);
        return ResponseEntity.status(201).body(toAddressDTO(saved));
    }

    @DeleteMapping("/me/addresses/{addressId}")
    public ResponseEntity<Void> deleteMyAddress(@PathVariable UUID addressId, @AuthenticationPrincipal User user) {
        if (orderRepository.existsByShippingAddressId(addressId)) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .header("X-Error-Message", "Este endereço não pode ser excluído pois está associado a um ou mais pedidos.")
                    .build();
        }

        return customerRepository.findByUserId(user.getId())
                .map(c -> {
                    boolean removed = c.getAddresses().removeIf(a -> a.getId().equals(addressId));
                    if (removed) {
                        customerRepository.save(c);
                        return ResponseEntity.noContent().<Void>build();
                    }
                    return ResponseEntity.notFound().<Void>build();
                })
                .orElse(ResponseEntity.notFound().build());
    }

    private CustomerDTO toDTO(Customer c) {
        User u = c.getUser();
        return new CustomerDTO(c.getId(), u.getId(), u.getName(), u.getEmail(),
                u.getPhoneNumber(), c.getCompanyName(), c.getNotes(), c.getLastContactAt(), c.getCreatedAt());
    }

    private AddressDTO toAddressDTO(Address a) {
        return new AddressDTO(a.getId(), a.getStreet(), a.getNumber(),
                a.getComplement(), a.getNeighborhood(), a.getCity(), a.getState(), a.getZipCode());
    }
}