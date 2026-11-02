package com.company.cms.service;

import com.company.cms.dto.DepartmentDtos.*;
import com.company.cms.entity.Department;
import com.company.cms.exception.ConflictException;
import com.company.cms.exception.ResourceNotFoundException;
import com.company.cms.repository.DepartmentRepository;
import com.company.cms.repository.EmployeeRepository;
import com.company.cms.repository.ProjectRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class DepartmentService {

    private final DepartmentRepository departmentRepository;
    private final EmployeeRepository employeeRepository;
    private final ProjectRepository projectRepository;

    public DepartmentService(
            DepartmentRepository departmentRepository,
            EmployeeRepository employeeRepository,
            ProjectRepository projectRepository
    ) {
        this.departmentRepository = departmentRepository;
        this.employeeRepository = employeeRepository;
        this.projectRepository = projectRepository;
    }

    public List<DepartmentResponse> getAllDepartments() {
        return departmentRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public DepartmentResponse getDepartmentById(Long id) {
        Department dept = departmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Department not found with id: " + id));
        return mapToResponse(dept);
    }

    @Transactional
    public DepartmentResponse createDepartment(DepartmentRequest request) {
        if (departmentRepository.existsByName(request.getName())) {
            throw new ConflictException("Department with name '" + request.getName() + "' already exists.");
        }
        Department dept = new Department(request.getName(), request.getDescription());
        dept = departmentRepository.save(dept);
        return mapToResponse(dept);
    }

    @Transactional
    public DepartmentResponse updateDepartment(Long id, DepartmentRequest request) {
        Department dept = departmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Department not found with id: " + id));

        if (!dept.getName().equalsIgnoreCase(request.getName()) && departmentRepository.existsByName(request.getName())) {
            throw new ConflictException("Department with name '" + request.getName() + "' already exists.");
        }

        dept.setName(request.getName());
        dept.setDescription(request.getDescription());
        dept = departmentRepository.save(dept);
        return mapToResponse(dept);
    }

    @Transactional
    public void deleteDepartment(Long id) {
        Department dept = departmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Department not found with id: " + id));

        long employeeCount = employeeRepository.countByDepartmentId(id);
        if (employeeCount > 0) {
            throw new ConflictException("Cannot delete department '" + dept.getName() + "' because it has " + employeeCount + " assigned employees. Please reassign staff first.");
        }

        var projects = projectRepository.findByDepartmentId(id);
        if (!projects.isEmpty()) {
            throw new ConflictException("Cannot delete department '" + dept.getName() + "' because it has " + projects.size() + " active projects. Please reassign or delete projects first.");
        }

        departmentRepository.delete(dept);
    }

    private DepartmentResponse mapToResponse(Department dept) {
        long count = employeeRepository.countByDepartmentId(dept.getId());
        return new DepartmentResponse(dept.getId(), dept.getName(), dept.getDescription(), count, dept.getCreatedAt());
    }
}
