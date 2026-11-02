package com.company.cms.service;

import com.company.cms.dto.SalesOrderDtos.*;
import com.company.cms.entity.*;
import com.company.cms.exception.ConflictException;
import com.company.cms.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SalesOrderServiceTest {

    @Mock
    private SalesOrderRepository salesOrderRepository;
    @Mock
    private CustomerRepository customerRepository;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private EmployeeRepository employeeRepository;
    @Mock
    private OrderStatusHistoryRepository statusHistoryRepository;
    @Mock
    private AnalyticsClientService analyticsClientService;

    @InjectMocks
    private SalesOrderService salesOrderService;

    private Customer sampleCustomer;
    private Employee sampleEmployee;
    private Product sampleProduct;

    @BeforeEach
    void setUp() {
        sampleCustomer = new Customer();
        sampleCustomer.setId(1L);
        sampleCustomer.setCompanyName("Acme Test Corp");
        sampleCustomer.setRegion("WEST");

        sampleEmployee = new Employee();
        sampleEmployee.setId(10L);
        sampleEmployee.setFirstName("Charlie");
        sampleEmployee.setLastName("Davis");
        sampleEmployee.setEmail("charlie@company.com");

        sampleProduct = new Product();
        sampleProduct.setId(100L);
        sampleProduct.setName("Enterprise Cloud ERP");
        sampleProduct.setSku("ERP-01");
        sampleProduct.setUnitPrice(new BigDecimal("1000.00"));
        sampleProduct.setUnitCost(new BigDecimal("400.00"));
        sampleProduct.setActive(true);
    }

    @Test
    @DisplayName("Should successfully calculate subtotal, discount, and total amount when creating an order")
    void testCreateOrderPriceAndDiscountCalculation() {
        OrderItemRequest itemReq = new OrderItemRequest();
        itemReq.setProductId(100L);
        itemReq.setQuantity(2);

        CreateOrderRequest request = new CreateOrderRequest();
        request.setCustomerId(1L);
        request.setSalesRepId(10L);
        request.setDiscountPercent(new BigDecimal("10.00")); // 10% discount
        request.setItems(List.of(itemReq));

        when(customerRepository.findById(1L)).thenReturn(Optional.of(sampleCustomer));
        when(employeeRepository.findById(10L)).thenReturn(Optional.of(sampleEmployee));
        when(productRepository.findById(100L)).thenReturn(Optional.of(sampleProduct));
        when(salesOrderRepository.save(any(SalesOrder.class))).thenAnswer(invocation -> {
            SalesOrder order = invocation.getArgument(0);
            order.setId(500L);
            return order;
        });

        SalesOrderResponse response = salesOrderService.createOrder(request, 10L, "admin@company.com");

        assertNotNull(response);
        assertEquals(OrderStatus.INQUIRY, response.getStatus());
        assertEquals(new BigDecimal("2000.00"), response.getSubtotalAmount());
        // 2000 - 10% = 1800.00
        assertEquals(new BigDecimal("1800.00"), response.getTotalAmount());
        verify(statusHistoryRepository, times(1)).save(any(OrderStatusHistory.class));
    }

    @Test
    @DisplayName("Should permit valid workflow transition INQUIRY -> QUOTED")
    void testValidStatusTransition() {
        SalesOrder order = new SalesOrder();
        order.setId(501L);
        order.setStatus(OrderStatus.INQUIRY);
        order.setCustomer(sampleCustomer);
        order.setSalesRep(sampleEmployee);

        when(salesOrderRepository.findById(501L)).thenReturn(Optional.of(order));
        when(salesOrderRepository.save(any(SalesOrder.class))).thenAnswer(i -> i.getArgument(0));

        SalesOrderResponse resp = salesOrderService.changeOrderStatus(501L, OrderStatus.QUOTED, "admin@company.com");

        assertEquals(OrderStatus.QUOTED, resp.getStatus());
        verify(statusHistoryRepository, times(1)).save(any(OrderStatusHistory.class));
    }

    @Test
    @DisplayName("Should throw ConflictException when trying to cancel an already DELIVERED order")
    void testInvalidStatusTransitionThrowsConflict() {
        SalesOrder order = new SalesOrder();
        order.setId(502L);
        order.setStatus(OrderStatus.DELIVERED);
        order.setCustomer(sampleCustomer);
        order.setSalesRep(sampleEmployee);

        when(salesOrderRepository.findById(502L)).thenReturn(Optional.of(order));

        assertThrows(ConflictException.class, () ->
                salesOrderService.changeOrderStatus(502L, OrderStatus.CANCELLED, "admin@company.com")
        );
    }
}
