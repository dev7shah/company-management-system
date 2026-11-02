package com.company.cms.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class CustomerDtos {

    public static class CustomerRequest {
        @NotBlank(message = "Company name is required")
        private String companyName;

        private String contactName;

        @NotBlank(message = "Email is required")
        @Email(message = "Valid email is required")
        private String email;

        private String phone;

        @NotBlank(message = "Region is required")
        @Pattern(regexp = "(?i)^(NORTH|SOUTH|EAST|WEST|CENTRAL)$", message = "Region must be NORTH, SOUTH, EAST, WEST, or CENTRAL")
        private String region;

        public String getCompanyName() { return companyName; }
        public void setCompanyName(String companyName) { this.companyName = companyName; }
        public String getContactName() { return contactName; }
        public void setContactName(String contactName) { this.contactName = contactName; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getPhone() { return phone; }
        public void setPhone(String phone) { this.phone = phone; }
        public String getRegion() { return region; }
        public void setRegion(String region) { this.region = region; }
    }

    public static class CustomerResponse {
        private Long id;
        private String companyName;
        private String contactName;
        private String email;
        private String phone;
        private String region;

        public CustomerResponse(Long id, String companyName, String contactName, String email, String phone, String region) {
            this.id = id;
            this.companyName = companyName;
            this.contactName = contactName;
            this.email = email;
            this.phone = phone;
            this.region = region;
        }

        public Long getId() { return id; }
        public String getCompanyName() { return companyName; }
        public String getContactName() { return contactName; }
        public String getEmail() { return email; }
        public String getPhone() { return phone; }
        public String getRegion() { return region; }
    }
}
