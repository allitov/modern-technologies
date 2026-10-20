package io.allitov.pvz.service.impl;

import io.allitov.pvz.model.Order;
import io.allitov.pvz.model.Report;
import io.allitov.pvz.repository.OrderRepository;
import io.allitov.pvz.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;

    @Override
    public boolean issueOrder(String orderCode) {
        return false;
    }

    @Override
    public boolean cancelOrder(String orderCode) {
        return false;
    }

    @Override
    public Report buildReport() {
        return null;
    }

    @Override
    public List<Order> listOrders(String filter) {
        return List.of();
    }

    @Override
    public List<Order> listByStatus(String status) {
        return List.of();
    }

    @Override
    public int countClosedOrders() {
        return 0;
    }

    @Override
    public int deleteClosedOrders() {
        return 0;
    }

    @Override
    public String addOrder(String contact, String cell) {
        return "";
    }
}
