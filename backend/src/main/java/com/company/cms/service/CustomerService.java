package com.company.cms.service;

import com.company.cms.dto.CustomerDtos.*;
import com.company.cms.entity.Customer;
import com.company.cms.exception.ConflictException;
import com.company.cms.exception.ResourceNotFoundException;
import com.company.cms.repository.CustomerFeedbackRepository;
import com.company.cms.repository.CustomerRepository;
import com.company.cms.repository.SalesOrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final SalesOrderRepository salesOrderRepository;
    private final CustomerFeedbackRepository customerFeedbackRepository;

    public CustomerService(
            CustomerRepository customerRepository,
            SalesOrderRepository salesOrderRepository,
            CustomerFeedbackRepository customerFeedbackRepository
    ) {
        this.customerRepository = customerRepository;
        this.salesOrderRepository = salesOrderRepository;
        this.customerFeedbackRepository = customerFeedbackRepository;
    }

    public List<CustomerResponse> getAllCustomers(String region) {
        List<Customer> list = (region != null && !region.isBlank() && !region.equalsIgnoreCase("ALL"))
                ? customerRepository.findByRegionIgnoreCase(region)
                : customerRepository.findAll();

        return list.stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    public CustomerResponse getCustomerById(Long id) {
        Customer c = customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id: " + id));
        return mapToResponse(c);
    }

    @Transactional
    public CustomerResponse createCustomer(CustomerRequest request) {
        if (customerRepository.existsByEmail(request.getEmail())) {
            throw new ConflictException("Customer with email " + request.getEmail() + " already exists.");
        }
        Customer c = new Customer();
        c.setCompanyName(request.getCompanyName());
        c.setContactName(request.getContactName());
        c.setEmail(request.getEmail());
        c.setPhone(request.getPhone());
        c.setRegion(request.getRegion().toUpperCase());

        c = customerRepository.save(c);
        return mapToResponse(c);
    }

    @Transactional
    public CustomerResponse updateCustomer(Long id, CustomerRequest request) {
        Customer c = customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id: " + id));

        if (!c.getEmail().equalsIgnoreCase(request.getEmail()) && customerRepository.existsByEmail(request.getEmail())) {
            throw new ConflictException("Customer with email " + request.getEmail() + " already exists.");
        }

        c.setCompanyName(request.getCompanyName());
        c.setContactName(request.getContactName());
        c.setEmail(request.getEmail());
        c.setPhone(request.getPhone());
        c.setRegion(request.getRegion().toUpperCase());

        c = customerRepository.save(c);
        return mapToResponse(c);
    }

    @Transactional
    public void deleteCustomer(Long id) {
        Customer c = customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id: " + id));

        var orders = salesOrderRepository.findByCustomerId(id);
        if (!orders.isEmpty()) {
            throw new ConflictException("Cannot delete customer '" + c.getCompanyName() + "' because they have " + orders.size() + " active sales orders. Please cancel or reassign orders first.");
        }

        // Clean up any linked customer feedback
        var feedbacks = customerFeedbackRepository.findByCustomerId(id);
        if (!feedbacks.isEmpty()) {
            customerFeedbackRepository.deleteAll(feedbacks);
        }

        customerRepository.delete(c);
    }

    private CustomerResponse mapToResponse(Customer c) {
        return new CustomerResponse(c.getId(), c.getCompanyName(), c.getContactName(), c.getEmail(), c.getPhone(), c.getRegion());
    }
}
