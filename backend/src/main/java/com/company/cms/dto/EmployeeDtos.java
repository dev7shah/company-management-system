package com.company.cms.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.LocalDate;

public class EmployeeDtos {

    public static class EmployeeRequest {
        @NotBlank(message = "Employee code is required")
        private String employeeCode;

        @NotBlank(message = "First name is required")
        private String firstName;

        @NotBlank(message = "Last name is required")
        private String lastName;

        @NotBlank(message = "Email is required")
        @Email(message = "Valid email is required")
        private String email;

        private String phone;

        @NotBlank(message = "Job title is required")
        private String jobTitle;

        @NotNull(message = "Hire date is required")
        private LocalDate hireDate;

        @NotNull(message = "Salary is required")
        @Positive(message = "Salary must be positive")
        private BigDecimal salary;

        private LocalDate dateOfBirth;

        @NotNull(message = "Department ID is required")
        private Long departmentId;

        private Long managerId;

        private String password;

        private com.company.cms.entity.Role role;

        public String getEmployeeCode() { return employeeCode; }
        public void setEmployeeCode(String employeeCode) { this.employeeCode = employeeCode; }
        public String getFirstName() { return firstName; }
        public void setFirstName(String firstName) { this.firstName = firstName; }
        public String getLastName() { return lastName; }
        public void setLastName(String lastName) { this.lastName = lastName; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getPhone() { return phone; }
        public void setPhone(String phone) { this.phone = phone; }
        public String getJobTitle() { return jobTitle; }
        public void setJobTitle(String jobTitle) { this.jobTitle = jobTitle; }
        public LocalDate getHireDate() { return hireDate; }
        public void setHireDate(LocalDate hireDate) { this.hireDate = hireDate; }
        public BigDecimal getSalary() { return salary; }
        public void setSalary(BigDecimal salary) { this.salary = salary; }
        public LocalDate getDateOfBirth() { return dateOfBirth; }
        public void setDateOfBirth(LocalDate dateOfBirth) { this.dateOfBirth = dateOfBirth; }
        public Long getDepartmentId() { return departmentId; }
        public void setDepartmentId(Long departmentId) { this.departmentId = departmentId; }
        public Long getManagerId() { return managerId; }
        public void setManagerId(Long managerId) { this.managerId = managerId; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
        public com.company.cms.entity.Role getRole() { return role; }
        public void setRole(com.company.cms.entity.Role role) { this.role = role; }
    }

    public static class EmployeeResponse {
        private Long id;
        private String employeeCode;
        private String firstName;
        private String lastName;
        private String fullName;
        private String email;
        private String phone;
        private String jobTitle;
        private LocalDate hireDate;
        private BigDecimal salary; // null if not Admin
        private LocalDate dateOfBirth; // null if not Admin
        private String status;
        private Long departmentId;
        private String departmentName;
        private Long managerId;
        private String managerName;
        private Long userId;
        private com.company.cms.entity.Role userRole;
        private boolean hasUserAccount;

        public EmployeeResponse() {}

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getEmployeeCode() { return employeeCode; }
        public void setEmployeeCode(String employeeCode) { this.employeeCode = employeeCode; }
        public String getFirstName() { return firstName; }
        public void setFirstName(String firstName) { this.firstName = firstName; }
        public String getLastName() { return lastName; }
        public void setLastName(String lastName) { this.lastName = lastName; }
        public String getFullName() { return fullName; }
        public void setFullName(String fullName) { this.fullName = fullName; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getPhone() { return phone; }
        public void setPhone(String phone) { this.phone = phone; }
        public String getJobTitle() { return jobTitle; }
        public void setJobTitle(String jobTitle) { this.jobTitle = jobTitle; }
        public LocalDate getHireDate() { return hireDate; }
        public void setHireDate(LocalDate hireDate) { this.hireDate = hireDate; }
        public BigDecimal getSalary() { return salary; }
        public void setSalary(BigDecimal salary) { this.salary = salary; }
        public LocalDate getDateOfBirth() { return dateOfBirth; }
        public void setDateOfBirth(LocalDate dateOfBirth) { this.dateOfBirth = dateOfBirth; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public Long getDepartmentId() { return departmentId; }
        public void setDepartmentId(Long departmentId) { this.departmentId = departmentId; }
        public String getDepartmentName() { return departmentName; }
        public void setDepartmentName(String departmentName) { this.departmentName = departmentName; }
        public Long getManagerId() { return managerId; }
        public void setManagerId(Long managerId) { this.managerId = managerId; }
        public String getManagerName() { return managerName; }
        public void setManagerName(String managerName) { this.managerName = managerName; }
        public Long getUserId() { return userId; }
        public void setUserId(Long userId) { this.userId = userId; }
        public com.company.cms.entity.Role getUserRole() { return userRole; }
        public void setUserRole(com.company.cms.entity.Role userRole) { this.userRole = userRole; }
        public boolean isHasUserAccount() { return hasUserAccount; }
        public void setHasUserAccount(boolean hasUserAccount) { this.hasUserAccount = hasUserAccount; }
    }
}
