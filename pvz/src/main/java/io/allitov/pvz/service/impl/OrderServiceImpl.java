package io.allitov.pvz.service.impl;

import io.allitov.pvz.model.Order;
import io.allitov.pvz.model.OrderFilter;
import io.allitov.pvz.model.OrderStatus;
import io.allitov.pvz.model.Report;
import io.allitov.pvz.repository.OrderRepository;
import io.allitov.pvz.exception.OrderCreationException;
import io.allitov.pvz.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.jdbi.v3.core.JdbiException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
class OrderServiceImpl implements OrderService {

    private static final Pattern LOOKS_LIKE_PHONE = Pattern.compile("^(?=(?:\\D*\\d){10})[\\d+\\-() ]+$");

    private static final List<String> CLOSED_STATUSES = List.of(
        OrderStatus.ISSUED.name().toLowerCase(Locale.ROOT),
        OrderStatus.CANCELLED.name().toLowerCase(Locale.ROOT)
    );

    private final OrderRepository orderRepository;

    @Override
    @Transactional
    public boolean issueOrder(String orderCode) {
        Optional<Order> found = orderRepository.findByCode(orderCode);
        if (found.isEmpty()) {
            return false;
        }
        String ready = OrderStatus.READY.name().toLowerCase(Locale.ROOT);
        if (!found.get().getStatus().equals(ready)) {
            return false;
        }
        int updated = orderRepository.updateStatusAndIssuedAt(
            orderCode,
            OrderStatus.ISSUED.name().toLowerCase(Locale.ROOT),
            Instant.now()
        );
        return updated > 0;
    }

    @Override
    @Transactional
    public boolean cancelOrder(String orderCode) {
        Optional<Order> found = orderRepository.findByCode(orderCode);
        if (found.isEmpty()) {
            return false;
        }
        String ready = OrderStatus.READY.name().toLowerCase(Locale.ROOT);
        if (!found.get().getStatus().equals(ready)) {
            return false;
        }
        int updated = orderRepository.updateStatus(
            orderCode,
            OrderStatus.CANCELLED.name().toLowerCase(Locale.ROOT)
        );
        return updated > 0;
    }

    @Override
    public Report buildReport() {
        return null;
    }

    @Override
    public List<Order> listOrders(String filter) {
        return listOrders(OrderFilter.of(filter));
    }

    private List<Order> listOrders(OrderFilter filter) {
        if (filter.getSearch() == null) {
            return orderRepository.findAll();
        }
        return orderRepository.findBySearchTerm("%" + filter.getSearch() + "%");
    }

    @Override
    public List<Order> listByStatus(String status) {
        if (status == null || status.isBlank()) {
            return orderRepository.findAll();
        }
        return orderRepository.findByStatus(status.trim());
    }

    @Override
    public int countClosedOrders() {
        return orderRepository.countByStatuses(CLOSED_STATUSES);
    }

    @Override
    @Transactional
    public int deleteClosedOrders() {
        return orderRepository.deleteByStatuses(CLOSED_STATUSES);
    }

    @Override
    @Transactional
    public String addOrder(String contact, String cell) {
        boolean isPhone = looksLikePhone(contact);
        String tempCode = UUID.randomUUID().toString().replace("-", "").substring(0, 30);
        try {
            long id = orderRepository.insert(tempCode, isPhone ? contact : null, isPhone ? null : contact, cell);
            String code = String.format("PVZ-%06d", id);
            orderRepository.updateCode(id, code);
            return code;
        } catch (JdbiException e) {
            throw new OrderCreationException("Failed to add order for contact '" + contact + "'", e);
        }
    }

    private boolean looksLikePhone(String contact) {
        return contact != null && LOOKS_LIKE_PHONE.matcher(contact).matches();
    }
}
