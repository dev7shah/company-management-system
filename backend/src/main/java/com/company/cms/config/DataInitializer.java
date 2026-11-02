package com.company.cms.config;

import com.company.cms.entity.*;
import com.company.cms.repository.*;
import com.company.cms.service.AnalyticsClientService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final EmployeeRepository employeeRepository;
    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;
    private final SalesOrderRepository salesOrderRepository;
    private final CustomerFeedbackRepository feedbackRepository;
    private final ProjectRepository projectRepository;
    private final PasswordEncoder passwordEncoder;
    private final AnalyticsClientService analyticsClientService;

    public DataInitializer(
            UserRepository userRepository,
            DepartmentRepository departmentRepository,
            EmployeeRepository employeeRepository,
            CustomerRepository customerRepository,
            ProductRepository productRepository,
            SalesOrderRepository salesOrderRepository,
            CustomerFeedbackRepository feedbackRepository,
            ProjectRepository projectRepository,
            PasswordEncoder passwordEncoder,
            AnalyticsClientService analyticsClientService
    ) {
        this.userRepository = userRepository;
        this.departmentRepository = departmentRepository;
        this.employeeRepository = employeeRepository;
        this.customerRepository = customerRepository;
        this.productRepository = productRepository;
        this.salesOrderRepository = salesOrderRepository;
        this.feedbackRepository = feedbackRepository;
        this.projectRepository = projectRepository;
        this.passwordEncoder = passwordEncoder;
        this.analyticsClientService = analyticsClientService;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (userRepository.count() > 0) {
            log.info("Database already contains users. Checking employee login parity and syncing warehouse...");
            ensureAllEmployeesHaveUserLogins();
            syncAllOrdersToAnalyticsWarehouse();
            return;
        }

        log.info("Seeding initial corporate demo data for Company Management System...");

        // 1. Seed Departments
        Department deptEng = departmentRepository.save(new Department("Engineering & Architecture", "Software Engineering, Cloud Infrastructure, and Technical R&D"));
        Department deptSales = departmentRepository.save(new Department("Global Sales & Accounts", "Enterprise client acquisition, strategic accounts, and partner relations"));
        Department deptHR = departmentRepository.save(new Department("People Operations & HR", "Talent management, organizational development, and employee success"));
        Department deptFinance = departmentRepository.save(new Department("Finance & Operations", "Fiscal reporting, compliance, and corporate business operations"));

        // 2. Seed Users
        User adminUser = userRepository.save(new User("admin@company.com", passwordEncoder.encode("Admin@123"), Role.ADMIN));
        User managerUser = userRepository.save(new User("manager@company.com", passwordEncoder.encode("Manager@123"), Role.MANAGER));
        User employeeUser = userRepository.save(new User("employee@company.com", passwordEncoder.encode("Employee@123"), Role.EMPLOYEE));

        // 3. Seed Employees
        Employee empAdmin = new Employee();
        empAdmin.setEmployeeCode("EMP-1001");
        empAdmin.setFirstName("Alice");
        empAdmin.setLastName("Walker");
        empAdmin.setEmail("admin@company.com");
        empAdmin.setPhone("+1-555-0100");
        empAdmin.setJobTitle("Chief Executive & General Manager");
        empAdmin.setHireDate(LocalDate.of(2021, 1, 15));
        empAdmin.setDateOfBirth(LocalDate.of(1985, 6, 20));
        empAdmin.setSalary(new BigDecimal("185000.00"));
        empAdmin.setDepartment(deptEng);
        empAdmin.setUser(adminUser);
        empAdmin = employeeRepository.save(empAdmin);

        Employee empManager = new Employee();
        empManager.setEmployeeCode("EMP-1002");
        empManager.setFirstName("Robert");
        empManager.setLastName("Sterling");
        empManager.setEmail("manager@company.com");
        empManager.setPhone("+1-555-0102");
        empManager.setJobTitle("VP of Commercial Sales");
        empManager.setHireDate(LocalDate.of(2022, 3, 1));
        empManager.setDateOfBirth(LocalDate.of(1988, 11, 12));
        empManager.setSalary(new BigDecimal("145000.00"));
        empManager.setDepartment(deptSales);
        empManager.setManager(empAdmin);
        empManager.setUser(managerUser);
        empManager = employeeRepository.save(empManager);

        Employee empSales = new Employee();
        empSales.setEmployeeCode("EMP-1003");
        empSales.setFirstName("Charlie");
        empSales.setLastName("Davis");
        empSales.setEmail("employee@company.com");
        empSales.setPhone("+1-555-0105");
        empSales.setJobTitle("Senior Enterprise Account Executive");
        empSales.setHireDate(LocalDate.of(2023, 5, 10));
        empSales.setDateOfBirth(LocalDate.of(1992, 4, 18));
        empSales.setSalary(new BigDecimal("98000.00"));
        empSales.setDepartment(deptSales);
        empSales.setManager(empManager);
        empSales.setUser(employeeUser);
        empSales = employeeRepository.save(empSales);

        User architectUser = userRepository.save(new User("diana.prince@company.com", passwordEncoder.encode("Employee@123"), Role.EMPLOYEE));

        Employee empArchitect = new Employee();
        empArchitect.setEmployeeCode("EMP-1004");
        empArchitect.setFirstName("Diana");
        empArchitect.setLastName("Prince");
        empArchitect.setEmail("diana.prince@company.com");
        empArchitect.setPhone("+1-555-0108");
        empArchitect.setJobTitle("Principal Cloud Architect");
        empArchitect.setHireDate(LocalDate.of(2022, 8, 15));
        empArchitect.setDateOfBirth(LocalDate.of(1990, 9, 5));
        empArchitect.setSalary(new BigDecimal("160000.00"));
        empArchitect.setDepartment(deptEng);
        empArchitect.setManager(empAdmin);
        empArchitect.setUser(architectUser);
        empArchitect = employeeRepository.save(empArchitect);

        // Guarantee that EVERY employee in the repository ALWAYS has a linked system user login
        for (Employee e : employeeRepository.findAll()) {
            if (e.getUser() == null) {
                User u = userRepository.findByEmail(e.getEmail()).orElseGet(() ->
                        userRepository.save(new User(e.getEmail(), passwordEncoder.encode("Employee@123"), Role.EMPLOYEE))
                );
                e.setUser(u);
                employeeRepository.save(e);
            }
        }

        // 4. Seed Products
        Product pErp = new Product();
        pErp.setSku("PROD-ERP-01");
        pErp.setName("Enterprise Cloud ERP Suite");
        pErp.setCategory("Software");
        pErp.setUnitPrice(new BigDecimal("25000.00"));
        pErp.setUnitCost(new BigDecimal("8000.00"));
        pErp = productRepository.save(pErp);

        Product pAi = new Product();
        pAi.setSku("PROD-AI-02");
        pAi.setName("AI Business Analytics & ML Engine");
        pAi.setCategory("Analytics");
        pAi.setUnitPrice(new BigDecimal("15000.00"));
        pAi.setUnitCost(new BigDecimal("4500.00"));
        pAi = productRepository.save(pAi);

        Product pCrm = new Product();
        pCrm.setSku("PROD-CRM-03");
        pCrm.setName("Omnichannel CRM & Customer Hub");
        pCrm.setCategory("Software");
        pCrm.setUnitPrice(new BigDecimal("12000.00"));
        pCrm.setUnitCost(new BigDecimal("3600.00"));
        pCrm = productRepository.save(pCrm);

        Product pSec = new Product();
        pSec.setSku("PROD-SEC-04");
        pSec.setName("Zero-Trust Cyber Defense Shield");
        pSec.setCategory("Security");
        pSec.setUnitPrice(new BigDecimal("18000.00"));
        pSec.setUnitCost(new BigDecimal("6000.00"));
        pSec = productRepository.save(pSec);

        Product pHrm = new Product();
        pHrm.setSku("PROD-HRM-05");
        pHrm.setName("Automated People & Payroll Pro");
        pHrm.setCategory("HR Tech");
        pHrm.setUnitPrice(new BigDecimal("9500.00"));
        pHrm.setUnitCost(new BigDecimal("2800.00"));
        pHrm = productRepository.save(pHrm);

        // 5. Seed Customers
        Customer c1 = new Customer();
        c1.setCompanyName("Acme Industrial Corp");
        c1.setContactName("John Miller");
        c1.setEmail("jmiller@acme-ind.com");
        c1.setPhone("+1-800-555-011");
        c1.setRegion("NORTH");
        c1 = customerRepository.save(c1);

        Customer c2 = new Customer();
        c2.setCompanyName("GlobalTech Solutions Inc");
        c2.setContactName("Sarah Chen");
        c2.setEmail("schen@globaltech-solutions.com");
        c2.setPhone("+1-800-555-022");
        c2.setRegion("WEST");
        c2 = customerRepository.save(c2);

        Customer c3 = new Customer();
        c3.setCompanyName("Apex Financial Systems");
        c3.setContactName("Michael Brown");
        c3.setEmail("mbrown@apexfin.com");
        c3.setPhone("+1-800-555-033");
        c3.setRegion("EAST");
        c3 = customerRepository.save(c3);

        Customer c4 = new Customer();
        c4.setCompanyName("InnoCorp Technologies");
        c4.setContactName("Priya Sharma");
        c4.setEmail("priya@innocorp-tech.com");
        c4.setPhone("+1-800-555-044");
        c4.setRegion("SOUTH");
        c4 = customerRepository.save(c4);

        Customer c5 = new Customer();
        c5.setCompanyName("Nexus Retail Group");
        c5.setContactName("David Lee");
        c5.setEmail("dlee@nexusretail.com");
        c5.setPhone("+1-800-555-055");
        c5.setRegion("CENTRAL");
        c5 = customerRepository.save(c5);

        // 6. Seed Projects
        Project prj1 = new Project();
        prj1.setName("Next-Gen Cloud ERP Rollout");
        prj1.setDescription("Enterprise-wide migration to modern cloud microservices architecture with high availability.");
        prj1.setStartDate(LocalDate.now().minusMonths(3));
        prj1.setEndDate(LocalDate.now().plusMonths(6));
        prj1.setStatus("ACTIVE");
        prj1.setDepartment(deptEng);
        prj1.getAssignedEmployees().add(empArchitect);
        prj1.getAssignedEmployees().add(empAdmin);
        projectRepository.save(prj1);

        Project prj2 = new Project();
        prj2.setName("Q4 Global Client Expansion");
        prj2.setDescription("Scaling commercial accounts across North American and European enterprise sectors.");
        prj2.setStartDate(LocalDate.now().minusMonths(1));
        prj2.setEndDate(LocalDate.now().plusMonths(5));
        prj2.setStatus("ACTIVE");
        prj2.setDepartment(deptSales);
        prj2.getAssignedEmployees().add(empManager);
        prj2.getAssignedEmployees().add(empSales);
        projectRepository.save(prj2);

        // 7. Seed Sales Orders
        createOrder(c1, empSales, "NORTH", OrderStatus.DELIVERED,
                List.of(new ItemSpec(pErp, 1), new ItemSpec(pAi, 2)), new BigDecimal("5.00"));

        createOrder(c2, empSales, "WEST", OrderStatus.PAID,
                List.of(new ItemSpec(pCrm, 2), new ItemSpec(pSec, 1)), new BigDecimal("10.00"));

        createOrder(c3, empSales, "EAST", OrderStatus.CONFIRMED,
                List.of(new ItemSpec(pAi, 1), new ItemSpec(pErp, 1)), BigDecimal.ZERO);

        createOrder(c4, empManager, "SOUTH", OrderStatus.QUOTED,
                List.of(new ItemSpec(pSec, 2)), new BigDecimal("8.00"));

        createOrder(c5, empSales, "CENTRAL", OrderStatus.INQUIRY,
                List.of(new ItemSpec(pHrm, 3)), BigDecimal.ZERO);

        // 8. Seed Customer Feedback
        createFeedback(c1, pErp, 5, "Outstanding ERP platform! It has streamlined our supply chain and financial operations seamlessly.", "POSITIVE", 0.88);
        createFeedback(c2, pAi, 5, "The AI analytics forecasting and segmentation models provided invaluable leadership visibility.", "POSITIVE", 0.92);
        createFeedback(c3, pCrm, 4, "High quality CRM suite with very intuitive UI and strong performance.", "POSITIVE", 0.65);
        createFeedback(c4, pSec, 3, "Solid security architecture, looking forward to expanded third-party integrations.", "NEUTRAL", 0.05);
        createFeedback(c5, pHrm, 5, "Excellent HR automated payroll workflows and great response time from tech support.", "POSITIVE", 0.85);

        log.info("Corporate seed data initialization completed successfully.");
        syncAllOrdersToAnalyticsWarehouse();
    }

    private void syncAllOrdersToAnalyticsWarehouse() {
        try {
            var orders = salesOrderRepository.findAll();
            analyticsClientService.syncAllOrdersToAnalytics(orders);
            log.info("Synchronized {} operational sales orders to Analytics Data Warehouse.", orders.size());
        } catch (Exception e) {
            log.warn("Initial sync of sales orders to analytics failed: {}", e.getMessage());
        }
    }

    private void createOrder(Customer customer, Employee salesRep, String region, OrderStatus status, List<ItemSpec> itemSpecs, BigDecimal discountPct) {
        SalesOrder order = new SalesOrder();
        order.setOrderNumber("SO-2026-" + (1000 + salesOrderRepository.count() + 1));
        order.setCustomer(customer);
        order.setSalesRep(salesRep);
        order.setRegion(region);
        order.setStatus(status);
        order.setDiscountPercent(discountPct);
        order.setOrderDate(LocalDateTime.now().minusDays((salesOrderRepository.count() + 1) * 3));

        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal totalCost = BigDecimal.ZERO;

        for (ItemSpec spec : itemSpecs) {
            SalesOrderItem item = new SalesOrderItem();
            item.setOrder(order);
            item.setProduct(spec.product);
            item.setQuantity(spec.qty);
            item.setUnitPrice(spec.product.getUnitPrice());
            item.setUnitCost(spec.product.getUnitCost());

            BigDecimal lineTotal = spec.product.getUnitPrice().multiply(BigDecimal.valueOf(spec.qty));
            BigDecimal lineCost = spec.product.getUnitCost().multiply(BigDecimal.valueOf(spec.qty));
            item.setLineTotal(lineTotal);

            order.getItems().add(item);
            subtotal = subtotal.add(lineTotal);
            totalCost = totalCost.add(lineCost);
        }

        BigDecimal discountMultiplier = BigDecimal.ONE.subtract(discountPct.divide(new BigDecimal("100")));
        BigDecimal totalAmount = subtotal.multiply(discountMultiplier);

        order.setSubtotalAmount(subtotal);
        order.setTotalAmount(totalAmount);
        order.setTotalCost(totalCost);

        OrderStatusHistory history = new OrderStatusHistory(order, null, status, salesRep.getEmail());
        order.getStatusHistories().add(history);

        salesOrderRepository.save(order);
    }

    private void createFeedback(Customer customer, Product product, int rating, String review, String sentiment, double score) {
        CustomerFeedback fb = new CustomerFeedback();
        fb.setCustomer(customer);
        fb.setProduct(product);
        fb.setRating(rating);
        fb.setReviewText(review);
        fb.setSentimentLabel(sentiment);
        fb.setSentimentScore(score);
        feedbackRepository.save(fb);
    }

    private void ensureAllEmployeesHaveUserLogins() {
        for (Employee e : employeeRepository.findAll()) {
            if (e.getUser() == null) {
                User u = userRepository.findByEmail(e.getEmail()).orElseGet(() ->
                        userRepository.save(new User(e.getEmail(), passwordEncoder.encode("Employee@123"), Role.EMPLOYEE))
                );
                e.setUser(u);
                employeeRepository.save(e);
                log.info("Auto-provisioned system login for employee: {}", e.getEmail());
            }
        }
    }

    private record ItemSpec(Product product, int qty) {}
}
