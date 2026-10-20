package io.allitov.pvz.service;

import io.allitov.pvz.model.Order;
import io.allitov.pvz.model.Report;

import java.util.List;

public interface OrderService {

    boolean issueOrder(String orderCode);

    boolean cancelOrder(String orderCode);

    Report buildReport();

    List<Order> listOrders(String filter);

    List<Order> listByStatus(String status);

    int countClosedOrders();

    int deleteClosedOrders();

    String addOrder(String contact, String cell);
}
