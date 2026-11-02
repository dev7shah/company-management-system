package com.company.cms.dto;

import com.company.cms.entity.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class AuthDtos {

    public static class LoginRequest {
        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        private String email;

        @NotBlank(message = "Password is required")
        private String password;

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
    }

    public static class LoginResponse {
        private String token;
        private String email;
        private Role role;
        private String fullName;
        private Long employeeId;

        public LoginResponse(String token, String email, Role role, String fullName, Long employeeId) {
            this.token = token;
            this.email = email;
            this.role = role;
            this.fullName = fullName;
            this.employeeId = employeeId;
        }

        public String getToken() { return token; }
        public String getEmail() { return email; }
        public Role getRole() { return role; }
        public String getFullName() { return fullName; }
        public Long getEmployeeId() { return employeeId; }
    }

    public static class ChangePasswordRequest {
        @NotBlank(message = "Current password is required")
        private String currentPassword;

        @NotBlank(message = "New password is required")
        @Size(min = 6, message = "Password must be at least 6 characters")
        private String newPassword;

        public String getCurrentPassword() { return currentPassword; }
        public void setCurrentPassword(String currentPassword) { this.currentPassword = currentPassword; }
        public String getNewPassword() { return newPassword; }
        public void setNewPassword(String newPassword) { this.newPassword = newPassword; }
    }

    public static class CreateUserRequest {
        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        private String email;

        @NotBlank(message = "Password is required")
        @Size(min = 6, message = "Password must be at least 6 characters")
        private String password;

        @NotNull(message = "Role is required")
        private Role role;

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
        public Role getRole() { return role; }
        public void setRole(Role role) { this.role = role; }
    }

    public static class UserResponse {
        private Long id;
        private String email;
        private Role role;
        private boolean active;

        public UserResponse(Long id, String email, Role role, boolean active) {
            this.id = id;
            this.email = email;
            this.role = role;
            this.active = active;
        }

        public Long getId() { return id; }
        public String getEmail() { return email; }
        public Role getRole() { return role; }
        public boolean isActive() { return active; }
    }

    public static class UpdateUserRequest {
        @Email(message = "Invalid email format")
        private String email;

        private Role role;

        private Boolean active;

        @Size(min = 6, message = "Password must be at least 6 characters")
        private String password;

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public Role getRole() { return role; }
        public void setRole(Role role) { this.role = role; }
        public Boolean getActive() { return active; }
        public void setActive(Boolean active) { this.active = active; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
    }
}
