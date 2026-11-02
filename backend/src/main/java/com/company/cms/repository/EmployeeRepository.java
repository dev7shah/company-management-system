package com.company.cms.repository;

import com.company.cms.entity.Employee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, Long> {
    Optional<Employee> findByEmployeeCode(String employeeCode);
    Optional<Employee> findByEmail(String email);
    Optional<Employee> findByUserId(Long userId);
    boolean existsByEmployeeCode(String employeeCode);
    boolean existsByEmail(String email);
    long countByDepartmentId(Long departmentId);
    List<Employee> findByDepartmentId(Long departmentId);
    List<Employee> findByManagerId(Long managerId);

    @Query("SELECT e FROM Employee e WHERE " +
           "(:deptId IS NULL OR e.department.id = :deptId) AND " +
           "(:status IS NULL OR e.status = :status) AND " +
           "(:search IS NULL OR " +
           "LOWER(e.firstName) LIKE CONCAT('%', :search, '%') OR " +
           "LOWER(e.lastName) LIKE CONCAT('%', :search, '%') OR " +
           "LOWER(e.employeeCode) LIKE CONCAT('%', :search, '%'))")
    List<Employee> searchEmployees(
        @Param("deptId") Long deptId,
        @Param("status") String status,
        @Param("search") String search
    );
}
