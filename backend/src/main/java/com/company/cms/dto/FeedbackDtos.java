package com.company.cms.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

public class FeedbackDtos {

    public static class FeedbackRequest {
        @NotNull(message = "Customer ID is required")
        private Long customerId;

        private Long productId;

        @NotNull(message = "Rating is required")
        @Min(value = 1, message = "Rating must be between 1 and 5")
        @Max(value = 5, message = "Rating must be between 1 and 5")
        private Integer rating;

        @NotBlank(message = "Review text is required")
        private String reviewText;

        public Long getCustomerId() { return customerId; }
        public void setCustomerId(Long customerId) { this.customerId = customerId; }
        public Long getProductId() { return productId; }
        public void setProductId(Long productId) { this.productId = productId; }
        public Integer getRating() { return rating; }
        public void setRating(Integer rating) { this.rating = rating; }
        public String getReviewText() { return reviewText; }
        public void setReviewText(String reviewText) { this.reviewText = reviewText; }
    }

    public static class FeedbackResponse {
        private Long id;
        private Long customerId;
        private String customerName;
        private Long productId;
        private String productName;
        private Integer rating;
        private String reviewText;
        private String sentimentLabel;
        private Double sentimentScore;
        private LocalDateTime createdAt;

        public FeedbackResponse() {}

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public Long getCustomerId() { return customerId; }
        public void setCustomerId(Long customerId) { this.customerId = customerId; }
        public String getCustomerName() { return customerName; }
        public void setCustomerName(String customerName) { this.customerName = customerName; }
        public Long getProductId() { return productId; }
        public void setProductId(Long productId) { this.productId = productId; }
        public String getProductName() { return productName; }
        public void setProductName(String productName) { this.productName = productName; }
        public Integer getRating() { return rating; }
        public void setRating(Integer rating) { this.rating = rating; }
        public String getReviewText() { return reviewText; }
        public void setReviewText(String reviewText) { this.reviewText = reviewText; }
        public String getSentimentLabel() { return sentimentLabel; }
        public void setSentimentLabel(String sentimentLabel) { this.sentimentLabel = sentimentLabel; }
        public Double getSentimentScore() { return sentimentScore; }
        public void setSentimentScore(Double sentimentScore) { this.sentimentScore = sentimentScore; }
        public LocalDateTime getCreatedAt() { return createdAt; }
        public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    }

    public static class PublicFeedbackOptions {
        private java.util.List<CustomerOption> customers;
        private java.util.List<ProductOption> products;

        public PublicFeedbackOptions() {}

        public PublicFeedbackOptions(java.util.List<CustomerOption> customers, java.util.List<ProductOption> products) {
            this.customers = customers;
            this.products = products;
        }

        public java.util.List<CustomerOption> getCustomers() { return customers; }
        public void setCustomers(java.util.List<CustomerOption> customers) { this.customers = customers; }
        public java.util.List<ProductOption> getProducts() { return products; }
        public void setProducts(java.util.List<ProductOption> products) { this.products = products; }
    }

    public static class CustomerOption {
        private Long id;
        private String companyName;

        public CustomerOption() {}

        public CustomerOption(Long id, String companyName) {
            this.id = id;
            this.companyName = companyName;
        }

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getCompanyName() { return companyName; }
        public void setCompanyName(String companyName) { this.companyName = companyName; }
    }

    public static class ProductOption {
        private Long id;
        private String name;

        public ProductOption() {}

        public ProductOption(Long id, String name) {
            this.id = id;
            this.name = name;
        }

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
    }
}
