package com.company.cms.controller;

import com.company.cms.dto.SalesOrderDtos.*;
import com.company.cms.entity.Employee;
import com.company.cms.entity.OrderStatus;
import com.company.cms.repository.EmployeeRepository;
import com.company.cms.security.UserPrincipal;
import com.company.cms.service.SalesOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
@Tag(name = "Sales Orders", description = "Endpoints for enterprise order generation, calculation, and status progression")
public class SalesOrderController {

    private final SalesOrderService salesOrderService;
    private final EmployeeRepository employeeRepository;

    public SalesOrderController(SalesOrderService salesOrderService, EmployeeRepository employeeRepository) {
        this.salesOrderService = salesOrderService;
        this.employeeRepository = employeeRepository;
    }

    @GetMapping
    @Operation(summary = "List sales orders (Employees see only their own orders)")
    public ResponseEntity<List<SalesOrderResponse>> getOrders(
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(required = false) String region,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        Long employeeId = employeeRepository.findByUserId(userPrincipal.getId())
                .map(Employee::getId)
                .orElse(null);
        return ResponseEntity.ok(salesOrderService.getOrders(userPrincipal.getRole(), employeeId, status, region));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get order details by ID")
    public ResponseEntity<SalesOrderResponse> getOrderById(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        Long employeeId = employeeRepository.findByUserId(userPrincipal.getId())
                .map(Employee::getId)
                .orElse(null);
        return ResponseEntity.ok(salesOrderService.getOrderById(id, userPrincipal.getRole(), employeeId));
    }

    @PostMapping
    @Operation(summary = "Create a sales order (Server computes unit prices, line totals, and final discount)")
    public ResponseEntity<SalesOrderResponse> createOrder(
            @Valid @RequestBody CreateOrderRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        Long employeeId = employeeRepository.findByUserId(userPrincipal.getId())
                .map(Employee::getId)
                .orElse(null);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(salesOrderService.createOrder(request, employeeId, userPrincipal.getUsername()));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Admin/Manager: Advance or change order status according to workflow rules")
    public ResponseEntity<SalesOrderResponse> changeOrderStatus(
            @PathVariable Long id,
            @Valid @RequestBody ChangeStatusRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        return ResponseEntity.ok(salesOrderService.changeOrderStatus(id, request.getNewStatus(), userPrincipal.getUsername()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Admin/Manager: Delete or cancel sales order")
    public ResponseEntity<Void> deleteOrder(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        salesOrderService.deleteOrder(id, userPrincipal.getRole());
        return ResponseEntity.noContent().build();
    }
}
