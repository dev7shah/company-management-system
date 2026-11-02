package com.company.cms.service;

import com.company.cms.entity.SalesOrder;
import com.company.cms.entity.SalesOrderItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AnalyticsClientService {

    private static final Logger log = LoggerFactory.getLogger(AnalyticsClientService.class);

    private final RestClient restClient;

    @Value("${app.analytics.url:http://localhost:8000}")
    private String analyticsServiceUrl;

    @Value("${app.analytics.internal-key:cms-internal-analytics-key-2026}")
    private String internalApiKey;

    public AnalyticsClientService(RestClient restClient) {
        this.restClient = restClient;
    }

    public Object triggerEtl() {
        return post("/etl/run");
    }

    public Object getEtlStatus() {
        return get("/etl/status");
    }

    public Object getKpiMetrics(String region, String fromDate, String toDate) {
        String uri = "/analytics/kpis?region=" + (region != null ? region : "ALL");
        return get(uri);
    }

    public Object getSalesTrend(String region, String category) {
        String uri = "/analytics/sales/trend?region=" + (region != null ? region : "ALL") +
                "&category=" + (category != null ? category : "ALL");
        return get(uri);
    }

    public Object getRegionalComparison(String category) {
        String uri = "/analytics/regional?category=" + (category != null ? category : "ALL");
        return get(uri);
    }

    public Object getSalesFunnel(String region) {
        String uri = "/analytics/funnel?region=" + (region != null ? region : "ALL");
        return get(uri);
    }

    public Object getForecast(int horizon, String region) {
        String uri = "/analytics/forecast?horizon=" + horizon + "&region=" + (region != null ? region : "ALL");
        return get(uri);
    }

    public Object getSegments(int k) {
        String uri = "/analytics/segments?k=" + k;
        return get(uri);
    }

    public Object getSentiment(Long productId) {
        String uri = productId != null ? "/analytics/sentiment?productId=" + productId : "/analytics/sentiment";
        return get(uri);
    }

    public Object getUnifiedReport(String department) {
        String uri = "/analytics/unified-report" + (department != null ? "?department=" + department : "");
        return get(uri);
    }

    public Object getAdvancedPlots() {
        return get("/analytics/advanced-plots");
    }

    private Object get(String path) {
        int maxRetries = 2;
        Exception lastException = null;
        for (int attempt = 0; attempt < maxRetries; attempt++) {
            try {
                return restClient.get()
                        .uri(analyticsServiceUrl + path)
                        .header("x-internal-key", internalApiKey)
                        .accept(MediaType.APPLICATION_JSON)
                        .retrieve()
                        .body(Object.class);
            } catch (Exception e) {
                lastException = e;
                log.warn("Analytics GET attempt {}/{} failed for path: {}. Error: {}",
                        attempt + 1, maxRetries, path, e.getMessage());
                if (attempt < maxRetries - 1) {
                    try { Thread.sleep(5000); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
                }
            }
        }
        log.error("Analytics service unavailable after {} retries for path: {}. Last error: {}",
                maxRetries, path, lastException != null ? lastException.getMessage() : "unknown");
        return Map.of(
                "status", "UNAVAILABLE",
                "message", "Analytics engine temporarily unavailable. The service may be starting up — please retry in 30 seconds."
        );
    }

    private Object post(String path) {
        return post(path, null);
    }

    private Object post(String path, Object body) {
        try {
            var spec = restClient.post()
                    .uri(analyticsServiceUrl + path)
                    .header("x-internal-key", internalApiKey)
                    .accept(MediaType.APPLICATION_JSON);
            if (body != null) {
                spec.contentType(MediaType.APPLICATION_JSON).body(body);
            }
            return spec.retrieve().body(Object.class);
        } catch (Exception e) {
            log.warn("Analytics POST failed for {}. Error: {}", path, e.getMessage());
            return Map.of(
                    "status", "UNAVAILABLE",
                    "message", "Unable to reach analytics engine: " + e.getMessage()
            );
        }
    }

    public void syncOrderToAnalytics(SalesOrder order) {
        if (order == null) return;
        try {
            Map<String, Object> payload = mapOrderToSyncPayload(order);
            post("/analytics/sync-order", payload);
            log.info("Dispatched order sync to analytics warehouse: {}", order.getOrderNumber());
        } catch (Exception e) {
            log.warn("Sync order {} to analytics warehouse failed: {}", order.getOrderNumber(), e.getMessage());
        }
    }

    public void syncAllOrdersToAnalytics(List<SalesOrder> orders) {
        if (orders == null || orders.isEmpty()) return;
        try {
            List<Map<String, Object>> payloads = orders.stream()
                    .map(this::mapOrderToSyncPayload)
                    .collect(Collectors.toList());
            post("/analytics/sync-all-orders", payloads);
            log.info("Dispatched bulk sync of {} operational orders to analytics warehouse", orders.size());
        } catch (Exception e) {
            log.warn("Bulk sync orders to analytics warehouse failed: {}", e.getMessage());
        }
    }

    private Map<String, Object> mapOrderToSyncPayload(SalesOrder order) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", order.getId());
        map.put("orderNumber", order.getOrderNumber());
        map.put("orderDate", order.getOrderDate() != null ? order.getOrderDate().toString() : LocalDateTime.now().toString());
        map.put("customerId", order.getCustomer() != null ? order.getCustomer().getId() : 1L);
        map.put("customerName", order.getCustomer() != null ? order.getCustomer().getCompanyName() : "Enterprise Client");
        map.put("salesRepId", order.getSalesRep() != null ? order.getSalesRep().getId() : 1L);
        map.put("salesRepName", order.getSalesRep() != null ? order.getSalesRep().getFullName() : "Alice Walker");
        map.put("region", order.getRegion() != null ? order.getRegion() : "NORTH");
        map.put("status", order.getStatus() != null ? order.getStatus().name() : "INQUIRY");
        map.put("subtotalAmount", order.getSubtotalAmount() != null ? order.getSubtotalAmount().doubleValue() : 0.0);
        map.put("discountPercent", order.getDiscountPercent() != null ? order.getDiscountPercent().doubleValue() : 0.0);
        map.put("totalAmount", order.getTotalAmount() != null ? order.getTotalAmount().doubleValue() : 0.0);
        map.put("totalCost", order.getTotalCost() != null ? order.getTotalCost().doubleValue() : 0.0);

        List<Map<String, Object>> itemsList = new ArrayList<>();
        if (order.getItems() != null) {
            for (SalesOrderItem item : order.getItems()) {
                Map<String, Object> itemMap = new HashMap<>();
                itemMap.put("productId", item.getProduct() != null ? item.getProduct().getId() : 1L);
                itemMap.put("productSku", item.getProduct() != null ? item.getProduct().getSku() : "PROD-GEN");
                itemMap.put("productName", item.getProduct() != null ? item.getProduct().getName() : "Enterprise Software");
                itemMap.put("category", item.getProduct() != null && item.getProduct().getCategory() != null ? item.getProduct().getCategory() : "Enterprise Software");
                itemMap.put("quantity", item.getQuantity());
                itemMap.put("unitPrice", item.getUnitPrice() != null ? item.getUnitPrice().doubleValue() : 0.0);
                itemMap.put("unitCost", item.getUnitCost() != null ? item.getUnitCost().doubleValue() : 0.0);
                itemMap.put("lineTotal", item.getLineTotal() != null ? item.getLineTotal().doubleValue() : 0.0);
                itemsList.add(itemMap);
            }
        }
        map.put("items", itemsList);
        return map;
    }
}
