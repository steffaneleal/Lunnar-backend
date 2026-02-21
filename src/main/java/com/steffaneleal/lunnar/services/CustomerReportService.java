// Monta o relatório analítico sobre os clientes
package com.steffaneleal.lunnar.services;

import com.steffaneleal.lunnar.dto.*;
import com.steffaneleal.lunnar.models.*;
import com.steffaneleal.lunnar.repositories.CustomerRepository;
import com.steffaneleal.lunnar.repositories.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class CustomerReportService {

    private final CustomerRepository customerRepository;
    private final OrderRepository orderRepository;

    private static final String[] DAY_NAMES = {"Segunda", "Terça", "Quarta", "Quinta", "Sexta", "Sábado", "Domingo"};

    public CustomerReportDTO generateReport(UUID customerId) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new NoSuchElementException("Cliente não encontrado"));
        UUID userId = customer.getUser().getId();
        List<Order> orders = orderRepository.findByUserIdOrderByCreatedAtDesc(userId);

        int totalOrders = orders.size();
        BigDecimal totalSpent = orders.stream()
                .map(Order::getTotalPrice)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal averageOrderValue = totalOrders > 0 && totalSpent != null
                ? totalSpent.divide(BigDecimal.valueOf(totalOrders), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        LocalDateTime firstPurchaseAt = orders.isEmpty() ? null : orders.get(orders.size() - 1).getCreatedAt();
        LocalDateTime lastPurchaseAt = orders.isEmpty() ? null : orders.get(0).getCreatedAt();

        List<OrderSummaryDTO> ordersSummary = orders.stream()
                .map(o -> new OrderSummaryDTO(
                        o.getId(),
                        o.getCreatedAt(),
                        o.getTotalPrice(),
                        o.getStatus()
                ))
                .toList();

        // Por categoria — compatível com @ManyToMany (getCategories())
        Map<String, int[]> orderCountByCat = new HashMap<>();
        Map<String, Integer> qtyByCat = new HashMap<>();
        Map<String, BigDecimal> valueByCat = new HashMap<>();

        for (Order order : orders) {
            if (order.getItems() == null) continue;
            Set<String> catsInThisOrder = new HashSet<>();

            for (OrderItem item : order.getItems()) {
                if (item.getProduct() == null) continue;

                // Suporte a múltiplas categorias (@ManyToMany)
                Set<Category> productCategories = item.getProduct().getCategories();
                List<String> catNames = (productCategories == null || productCategories.isEmpty())
                        ? List.of("Sem categoria")
                        : productCategories.stream().map(Category::getName).toList();

                int qty = item.getQuantity() != null ? item.getQuantity() : 0;
                BigDecimal itemValue = item.getUnitPrice() != null && item.getQuantity() != null
                        ? item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity()))
                        : BigDecimal.ZERO;

                for (String catName : catNames) {
                    catsInThisOrder.add(catName);
                    qtyByCat.merge(catName, qty, Integer::sum);
                    valueByCat.merge(catName, itemValue, BigDecimal::add);
                }
            }
            for (String c : catsInThisOrder) {
                orderCountByCat.merge(c, new int[]{1}, (a, b) -> new int[]{a[0] + b[0]});
            }
        }

        List<CategorySummaryDTO> byCategory = qtyByCat.keySet().stream()
                .map(catName -> new CategorySummaryDTO(
                        catName,
                        orderCountByCat.getOrDefault(catName, new int[]{0})[0],
                        qtyByCat.getOrDefault(catName, 0),
                        valueByCat.getOrDefault(catName, BigDecimal.ZERO)
                ))
                .sorted(Comparator.comparing(CategorySummaryDTO::totalValue).reversed())
                .toList();

        // Por dia da semana (Java: Monday=1 .. Sunday=7)
        int[] dayCounts = new int[7];
        for (Order order : orders) {
            LocalDateTime dt = order.getCreatedAt();
            if (dt != null) {
                DayOfWeek d = dt.getDayOfWeek();
                dayCounts[d.getValue() - 1]++;
            }
        }
        List<DayOfWeekSummaryDTO> byDayOfWeek = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            byDayOfWeek.add(new DayOfWeekSummaryDTO(DAY_NAMES[i], i + 1, dayCounts[i]));
        }

        User u = customer.getUser();
        return new CustomerReportDTO(
                customer.getId(),
                u.getId(),
                u.getName(),
                u.getEmail(),
                customer.getCompanyName(),
                totalOrders,
                totalSpent != null ? totalSpent : BigDecimal.ZERO,
                averageOrderValue,
                firstPurchaseAt,
                lastPurchaseAt,
                ordersSummary,
                byCategory,
                byDayOfWeek
        );
    }
}