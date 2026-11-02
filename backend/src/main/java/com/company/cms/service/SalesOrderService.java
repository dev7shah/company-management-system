package com.company.cms.service;

import com.company.cms.dto.SalesOrderDtos.*;
import com.company.cms.entity.*;
import com.company.cms.exception.BadRequestException;
import com.company.cms.exception.ConflictException;
import com.company.cms.exception.ForbiddenException;
import com.company.cms.exception.ResourceNotFoundException;
import com.company.cms.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class SalesOrderService {

    private final SalesOrderRepository salesOrderRepository;
    private final CustomerRepository customerRepository;
    private final EmployeeRepository employeeRepository;
    private final ProductRepository productRepository;
    private final OrderStatusHistoryRepository statusHistoryRepository;
    private final AnalyticsClientService analyticsClientService;

    public SalesOrderService(
            SalesOrderRepository salesOrderRepository,
            CustomerRepository customerRepository,
            EmployeeRepository employeeRepository,
            ProductRepository productRepository,
            OrderStatusHistoryRepository statusHistoryRepository,
            AnalyticsClientService analyticsClientService
    ) {
        this.salesOrderRepository = salesOrderRepository;
        this.customerRepository = customerRepository;
        this.employeeRepository = employeeRepository;
        this.productRepository = productRepository;
        this.statusHistoryRepository = statusHistoryRepository;
        this.analyticsClientService = analyticsClientService;
    }

    @Transactional(readOnly = true)
    public List<SalesOrderResponse> getOrders(Role role, Long currentEmployeeId, OrderStatus status, String region) {
        List<SalesOrder> orders;
        if (role == Role.EMPLOYEE) {
            if (currentEmployeeId == null) {
                return List.of();
            }
            orders = salesOrderRepository.findBySalesRepId(currentEmployeeId);
        } else if (status != null) {
            orders = salesOrderRepository.findByStatus(status);
        } else if (region != null && !region.isBlank() && !region.equalsIgnoreCase("ALL")) {
            orders = salesOrderRepository.findByRegionIgnoreCase(region);
        } else {
            orders = salesOrderRepository.findAll();
        }

        return orders.stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public SalesOrderResponse getOrderById(Long id, Role role, Long currentEmployeeId) {
        SalesOrder order = salesOrderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Sales Order not found with id: " + id));

        if (role == Role.EMPLOYEE && !order.getSalesRep().getId().equals(currentEmployeeId)) {
            throw new ForbiddenException("You can only view your own sales orders.");
        }

        return mapToResponse(order);
    }

    @Transactional
    public SalesOrderResponse createOrder(CreateOrderRequest request, Long currentEmployeeId, String username) {
        Customer customer = customerRepository.findById(request.getCustomerId())
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id: " + request.getCustomerId()));

        Long repId = request.getSalesRepId() != null ? request.getSalesRepId() : currentEmployeeId;
        if (repId == null) {
            throw new BadRequestException("Sales representative is required.");
        }

        Employee salesRep = employeeRepository.findById(repId)
                .orElseThrow(() -> new ResourceNotFoundException("Sales Representative not found with id: " + repId));

        SalesOrder order = new SalesOrder();
        order.setOrderNumber("ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        order.setOrderDate(LocalDateTime.now());
        order.setCustomer(customer);
        order.setSalesRep(salesRep);
        order.setRegion(customer.getRegion());
        order.setStatus(OrderStatus.INQUIRY);

        BigDecimal discount = request.getDiscountPercent() != null ? request.getDiscountPercent() : BigDecimal.ZERO;
        order.setDiscountPercent(discount);

        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal totalCost = BigDecimal.ZERO;
        List<SalesOrderItem> items = new ArrayList<>();

        for (OrderItemRequest itemReq : request.getItems()) {
            Product product = productRepository.findById(itemReq.getProductId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + itemReq.getProductId()));

            if (!product.isActive()) {
                throw new BadRequestException("Product " + product.getName() + " is currently inactive.");
            }

            BigDecimal lineTotal = product.getUnitPrice().multiply(BigDecimal.valueOf(itemReq.getQuantity()));
            BigDecimal lineCost = product.getUnitCost().multiply(BigDecimal.valueOf(itemReq.getQuantity()));

            subtotal = subtotal.add(lineTotal);
            totalCost = totalCost.add(lineCost);

            SalesOrderItem item = new SalesOrderItem();
            item.setOrder(order);
            item.setProduct(product);
            item.setQuantity(itemReq.getQuantity());
            item.setUnitPrice(product.getUnitPrice()); // Snapshot price
            item.setUnitCost(product.getUnitCost());   // Snapshot cost
            item.setLineTotal(lineTotal);
            items.add(item);
        }

        order.setSubtotalAmount(subtotal);
        order.setTotalCost(totalCost);

        BigDecimal discountMultiplier = BigDecimal.ONE.subtract(discount.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP));
        BigDecimal finalTotal = subtotal.multiply(discountMultiplier).setScale(2, RoundingMode.HALF_UP);
        order.setTotalAmount(finalTotal);
        order.setItems(items);

        order = salesOrderRepository.save(order);

        // Record initial status history
        OrderStatusHistory history = new OrderStatusHistory(order, null, OrderStatus.INQUIRY, username != null ? username : "System");
        statusHistoryRepository.save(history);

        // Real-time synchronization with Analytics Data Warehouse
        analyticsClientService.syncOrderToAnalytics(order);

        return mapToResponse(order);
    }

    @Transactional
    public SalesOrderResponse changeOrderStatus(Long id, OrderStatus newStatus, String username) {
        SalesOrder order = salesOrderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Sales Order not found with id: " + id));

        OrderStatus currentStatus = order.getStatus();

        if (!currentStatus.canTransitionTo(newStatus)) {
            throw new ConflictException("Invalid status transition: Cannot move order from " + currentStatus + " to " + newStatus);
        }

        order.setStatus(newStatus);
        order = salesOrderRepository.save(order);

        OrderStatusHistory history = new OrderStatusHistory(order, currentStatus, newStatus, username);
        statusHistoryRepository.save(history);

        // Real-time synchronization with Analytics Data Warehouse
        analyticsClientService.syncOrderToAnalytics(order);

        return mapToResponse(order);
    }

    @Transactional
    public void deleteOrder(Long id, Role role) {
        SalesOrder order = salesOrderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Sales Order not found with id: " + id));

        if (order.getStatus() == OrderStatus.DELIVERED) {
            throw new ConflictException("Cannot delete fulfilled order '" + order.getOrderNumber() + "'. Fulfilled orders are part of permanent financial audit trails.");
        }

        salesOrderRepository.delete(order);
    }

    private SalesOrderResponse mapToResponse(SalesOrder order) {
        SalesOrderResponse resp = new SalesOrderResponse();
        resp.setId(order.getId());
        resp.setOrderNumber(order.getOrderNumber());
        resp.setOrderDate(order.getOrderDate());
        resp.setCustomerId(order.getCustomer().getId());
        resp.setCustomerName(order.getCustomer().getCompanyName());
        resp.setSalesRepId(order.getSalesRep().getId());
        resp.setSalesRepName(order.getSalesRep().getFullName());
        resp.setRegion(order.getRegion());
        resp.setStatus(order.getStatus());
        resp.setSubtotalAmount(order.getSubtotalAmount());
        resp.setDiscountPercent(order.getDiscountPercent());
        resp.setTotalAmount(order.getTotalAmount());
        resp.setTotalCost(order.getTotalCost());

        if (order.getItems() != null) {
            resp.setItems(order.getItems().stream().map(i -> {
                OrderItemResponse ir = new OrderItemResponse();
                ir.setId(i.getId());
                ir.setProductId(i.getProduct().getId());
                ir.setProductSku(i.getProduct().getSku());
                ir.setProductName(i.getProduct().getName());
                ir.setQuantity(i.getQuantity());
                ir.setUnitPrice(i.getUnitPrice());
                ir.setUnitCost(i.getUnitCost());
                ir.setLineTotal(i.getLineTotal());
                return ir;
            }).collect(Collectors.toList()));
        }

        List<OrderStatusHistory> historyList = statusHistoryRepository.findByOrderIdOrderByChangedAtAsc(order.getId());
        resp.setStatusHistories(historyList.stream().map(h ->
                new StatusHistoryResponse(h.getId(), h.getPreviousStatus(), h.getNewStatus(), h.getChangedBy(), h.getChangedAt())
        ).collect(Collectors.toList()));

        return resp;
    }
}
