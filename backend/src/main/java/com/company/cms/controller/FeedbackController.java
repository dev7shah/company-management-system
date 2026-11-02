package com.company.cms.controller;

import com.company.cms.dto.FeedbackDtos.*;
import com.company.cms.service.FeedbackService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/feedback")
@Tag(name = "Customer Feedback", description = "Endpoints for retrieving, submitting, and moderating customer reviews and sentiment")
public class FeedbackController {

    private final FeedbackService feedbackService;

    public FeedbackController(FeedbackService feedbackService) {
        this.feedbackService = feedbackService;
    }

    @GetMapping("/public/options")
    @Operation(summary = "Public: Retrieve verified customer and product options for customer review intake")
    public ResponseEntity<PublicFeedbackOptions> getPublicFeedbackOptions() {
        return ResponseEntity.ok(feedbackService.getPublicFeedbackOptions());
    }

    @PostMapping("/public")
    @Operation(summary = "Public: Customer submission of review, rating, and feedback")
    public ResponseEntity<FeedbackResponse> submitPublicFeedback(@Valid @RequestBody FeedbackRequest request) {
        FeedbackResponse response = feedbackService.createFeedback(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "List customer feedback with optional filters by rating or product")
    public ResponseEntity<List<FeedbackResponse>> getAllFeedback(
            @RequestParam(required = false) Integer rating,
            @RequestParam(required = false) Long productId
    ) {
        return ResponseEntity.ok(feedbackService.getAllFeedback(rating, productId));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Get customer feedback details by ID")
    public ResponseEntity<FeedbackResponse> getFeedbackById(@PathVariable Long id) {
        return ResponseEntity.ok(feedbackService.getFeedbackById(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Submit new customer feedback")
    public ResponseEntity<FeedbackResponse> createFeedback(@Valid @RequestBody FeedbackRequest request) {
        FeedbackResponse response = feedbackService.createFeedback(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Update customer feedback review or rating")
    public ResponseEntity<FeedbackResponse> updateFeedback(
            @PathVariable Long id,
            @Valid @RequestBody FeedbackRequest request
    ) {
        return ResponseEntity.ok(feedbackService.updateFeedback(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Admin: Delete customer feedback entry")
    public ResponseEntity<Void> deleteFeedback(@PathVariable Long id) {
        feedbackService.deleteFeedback(id);
        return ResponseEntity.noContent().build();
    }
}
