package com.company.cms.repository;

import com.company.cms.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {
    Optional<Customer> findByEmail(String email);
    boolean existsByEmail(String email);
    List<Customer> findByRegion(String region);
    List<Customer> findByRegionIgnoreCase(String region);
}
