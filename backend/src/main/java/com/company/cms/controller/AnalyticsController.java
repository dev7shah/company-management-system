package com.company.cms.controller;

import com.company.cms.dto.SalesOrderDtos.SalesOrderResponse;
import com.company.cms.entity.Employee;
import com.company.cms.entity.Role;
import com.company.cms.entity.SalesOrder;
import com.company.cms.repository.EmployeeRepository;
import com.company.cms.repository.ProjectRepository;
import com.company.cms.repository.SalesOrderRepository;
import com.company.cms.security.UserPrincipal;
import com.company.cms.service.AnalyticsClientService;
import com.company.cms.service.SalesOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/analytics")
@Tag(name = "Analytics & Intelligence Engine", description = "Endpoints for proxying analytics KPIs, forecasting, segmentation, sentiment, and ETL pipeline")
public class AnalyticsController {

    private final AnalyticsClientService analyticsClientService;
    private final EmployeeRepository employeeRepository;
    private final SalesOrderRepository salesOrderRepository;
    private final ProjectRepository projectRepository;
    private final SalesOrderService salesOrderService;

    public AnalyticsController(
            AnalyticsClientService analyticsClientService,
            EmployeeRepository employeeRepository,
            SalesOrderRepository salesOrderRepository,
            ProjectRepository projectRepository,
            SalesOrderService salesOrderService
    ) {
        this.analyticsClientService = analyticsClientService;
        this.employeeRepository = employeeRepository;
        this.salesOrderRepository = salesOrderRepository;
        this.projectRepository = projectRepository;
        this.salesOrderService = salesOrderService;
    }

    @GetMapping("/internal/orders")
    @Operation(summary = "Internal: Extract all operational sales orders for ETL warehouse synchronization")
    public ResponseEntity<List<SalesOrderResponse>> getInternalOrders() {
        return ResponseEntity.ok(salesOrderService.getOrders(Role.ADMIN, null, null, null));
    }

    @PostMapping("/etl/run")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Admin: Trigger full warehouse ETL extraction, cleaning, and star-schema loading")
    public ResponseEntity<Object> runEtl() {
        return ResponseEntity.ok(analyticsClientService.triggerEtl());
    }

    @GetMapping("/etl/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Fetch the latest ETL pipeline execution status and metadata")
    public ResponseEntity<Object> getEtlStatus() {
        return ResponseEntity.ok(analyticsClientService.getEtlStatus());
    }

    @GetMapping("/kpis")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Get high-level business KPI metrics (Revenue, Profit, Margin, Order Volume)")
    public ResponseEntity<Object> getKpis(
            @RequestParam(required = false, defaultValue = "ALL") String region,
            @RequestParam(required = false) String fromDate,
            @RequestParam(required = false) String toDate
    ) {
        return ResponseEntity.ok(analyticsClientService.getKpiMetrics(region, fromDate, toDate));
    }

    @GetMapping("/sales/trend")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Get multi-year historical and operational sales revenue and profit trends")
    public ResponseEntity<Object> getSalesTrend(
            @RequestParam(required = false, defaultValue = "ALL") String region,
            @RequestParam(required = false, defaultValue = "ALL") String category
    ) {
        return ResponseEntity.ok(analyticsClientService.getSalesTrend(region, category));
    }

    @GetMapping("/regional")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Get regional breakdown comparison of revenue, orders, and market share")
    public ResponseEntity<Object> getRegional(
            @RequestParam(required = false, defaultValue = "ALL") String category
    ) {
        return ResponseEntity.ok(analyticsClientService.getRegionalComparison(category));
    }

    @GetMapping("/funnel")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Get order conversion funnel from Inquiry down to Delivered stage")
    public ResponseEntity<Object> getFunnel(
            @RequestParam(required = false, defaultValue = "ALL") String region
    ) {
        return ResponseEntity.ok(analyticsClientService.getSalesFunnel(region));
    }

    @GetMapping("/forecast")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Generate Holt-Winters exponential smoothing sales forecasts with confidence bands")
    public ResponseEntity<Object> getForecast(
            @RequestParam(required = false, defaultValue = "6") int horizon,
            @RequestParam(required = false, defaultValue = "ALL") String region
    ) {
        return ResponseEntity.ok(analyticsClientService.getForecast(horizon, region));
    }

    @GetMapping("/segments")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Run unsupervised K-Means customer RFM segmentation")
    public ResponseEntity<Object> getSegments(
            @RequestParam(required = false, defaultValue = "4") int k
    ) {
        return ResponseEntity.ok(analyticsClientService.getSegments(k));
    }

    @GetMapping("/sentiment")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Run NLP polarity analysis on customer reviews and feedback")
    public ResponseEntity<Object> getSentiment(
            @RequestParam(required = false) Long productId
    ) {
        return ResponseEntity.ok(analyticsClientService.getSentiment(productId));
    }

    @GetMapping("/unified")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Get unified cross-functional report (Sales + HR performance + Customer sentiment)")
    public ResponseEntity<Object> getUnifiedReport(
            @RequestParam(required = false) String department
    ) {
        return ResponseEntity.ok(analyticsClientService.getUnifiedReport(department));
    }

    @GetMapping("/advanced-plots")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Retrieve high-resolution statistical Seaborn/Matplotlib visualization figures")
    public ResponseEntity<Object> getAdvancedPlots() {
        return ResponseEntity.ok(analyticsClientService.getAdvancedPlots());
    }

    @GetMapping("/my-performance")
    @Operation(summary = "Retrieve personal performance scorecard for the logged-in employee")
    public ResponseEntity<Map<String, Object>> getMyPerformance(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        Map<String, Object> result = new HashMap<>();

        if (userPrincipal == null) {
            return ResponseEntity.ok(Map.of("message", "User session not found"));
        }

        Employee emp = employeeRepository.findByUserId(userPrincipal.getId()).orElse(null);
        if (emp == null) {
            result.put("name", userPrincipal.getUsername());
            result.put("role", userPrincipal.getRole().name());
            result.put("message", "No employee profile linked to this user account yet.");
            result.put("totalSales", BigDecimal.ZERO);
            result.put("ordersCount", 0);
            return ResponseEntity.ok(result);
        }

        List<SalesOrder> orders = salesOrderRepository.findBySalesRepId(emp.getId());
        BigDecimal totalSales = orders.stream()
                .map(SalesOrder::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long deliveredCount = orders.stream()
                .filter(o -> "DELIVERED".equalsIgnoreCase(o.getStatus().name()))
                .count();

        long activeProjectsCount = projectRepository.findProjectsAssignedToEmployee(emp.getId()).stream()
                .filter(p -> "ACTIVE".equalsIgnoreCase(p.getStatus()))
                .count();

        result.put("employeeId", emp.getId());
        result.put("employeeCode", emp.getEmployeeCode());
        result.put("fullName", emp.getFullName());
        result.put("jobTitle", emp.getJobTitle());
        result.put("department", emp.getDepartment() != null ? emp.getDepartment().getName() : "Unassigned");
        result.put("email", emp.getEmail());
        result.put("ordersCount", orders.size());
        result.put("deliveredCount", deliveredCount);
        result.put("totalSales", totalSales);
        result.put("activeProjectsCount", activeProjectsCount);

        return ResponseEntity.ok(result);
    }
}
