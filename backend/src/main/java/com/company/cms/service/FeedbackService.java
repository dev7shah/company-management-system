package com.company.cms.service;

import com.company.cms.dto.FeedbackDtos.*;
import com.company.cms.entity.Customer;
import com.company.cms.entity.CustomerFeedback;
import com.company.cms.entity.Product;
import com.company.cms.exception.ResourceNotFoundException;
import com.company.cms.repository.CustomerFeedbackRepository;
import com.company.cms.repository.CustomerRepository;
import com.company.cms.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class FeedbackService {

    private final CustomerFeedbackRepository feedbackRepository;
    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;

    public FeedbackService(
            CustomerFeedbackRepository feedbackRepository,
            CustomerRepository customerRepository,
            ProductRepository productRepository
    ) {
        this.feedbackRepository = feedbackRepository;
        this.customerRepository = customerRepository;
        this.productRepository = productRepository;
    }

    public List<FeedbackResponse> getAllFeedback(Integer rating, Long productId) {
        List<CustomerFeedback> list;
        if (rating != null) {
            list = feedbackRepository.findByRating(rating);
        } else if (productId != null) {
            list = feedbackRepository.findByProductId(productId);
        } else {
            list = feedbackRepository.findAll();
        }
        return list.stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    public PublicFeedbackOptions getPublicFeedbackOptions() {
        List<CustomerOption> customers = customerRepository.findAll().stream()
                .map(c -> new CustomerOption(c.getId(), c.getCompanyName()))
                .sorted((a, b) -> a.getCompanyName().compareToIgnoreCase(b.getCompanyName()))
                .collect(Collectors.toList());

        List<ProductOption> products = productRepository.findByActiveTrue().stream()
                .map(p -> new ProductOption(p.getId(), p.getName()))
                .sorted((a, b) -> a.getName().compareToIgnoreCase(b.getName()))
                .collect(Collectors.toList());

        return new PublicFeedbackOptions(customers, products);
    }

    @Transactional
    public FeedbackResponse createFeedback(FeedbackRequest request) {
        Customer customer = customerRepository.findById(request.getCustomerId())
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id: " + request.getCustomerId()));

        Product product = null;
        if (request.getProductId() != null) {
            product = productRepository.findById(request.getProductId()).orElse(null);
        }

        CustomerFeedback fb = new CustomerFeedback();
        fb.setCustomer(customer);
        fb.setProduct(product);
        fb.setRating(request.getRating());
        fb.setReviewText(request.getReviewText());

        // Basic sentiment heuristics before ETL runs full NLP
        if (request.getRating() >= 4) {
            fb.setSentimentLabel("POSITIVE");
            fb.setSentimentScore(0.75);
        } else if (request.getRating() == 3) {
            fb.setSentimentLabel("NEUTRAL");
            fb.setSentimentScore(0.0);
        } else {
            fb.setSentimentLabel("NEGATIVE");
            fb.setSentimentScore(-0.65);
        }

        fb = feedbackRepository.save(fb);
        return mapToResponse(fb);
    }

    public FeedbackResponse getFeedbackById(Long id) {
        CustomerFeedback fb = feedbackRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Feedback not found with id: " + id));
        return mapToResponse(fb);
    }

    @Transactional
    public FeedbackResponse updateFeedback(Long id, FeedbackRequest request) {
        CustomerFeedback fb = feedbackRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Feedback not found with id: " + id));

        Customer customer = customerRepository.findById(request.getCustomerId())
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id: " + request.getCustomerId()));

        Product product = null;
        if (request.getProductId() != null) {
            product = productRepository.findById(request.getProductId()).orElse(null);
        }

        fb.setCustomer(customer);
        fb.setProduct(product);
        fb.setRating(request.getRating());
        fb.setReviewText(request.getReviewText());

        if (request.getRating() >= 4) {
            fb.setSentimentLabel("POSITIVE");
            fb.setSentimentScore(0.75);
        } else if (request.getRating() == 3) {
            fb.setSentimentLabel("NEUTRAL");
            fb.setSentimentScore(0.0);
        } else {
            fb.setSentimentLabel("NEGATIVE");
            fb.setSentimentScore(-0.65);
        }

        fb = feedbackRepository.save(fb);
        return mapToResponse(fb);
    }

    @Transactional
    public void deleteFeedback(Long id) {
        CustomerFeedback fb = feedbackRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Feedback not found with id: " + id));
        feedbackRepository.delete(fb);
    }

    private FeedbackResponse mapToResponse(CustomerFeedback fb) {
        FeedbackResponse resp = new FeedbackResponse();
        resp.setId(fb.getId());
        resp.setCustomerId(fb.getCustomer().getId());
        resp.setCustomerName(fb.getCustomer().getCompanyName());
        if (fb.getProduct() != null) {
            resp.setProductId(fb.getProduct().getId());
            resp.setProductName(fb.getProduct().getName());
        }
        resp.setRating(fb.getRating());
        resp.setReviewText(fb.getReviewText());
        resp.setSentimentLabel(fb.getSentimentLabel());
        resp.setSentimentScore(fb.getSentimentScore());
        resp.setCreatedAt(fb.getCreatedAt());
        return resp;
    }
}
