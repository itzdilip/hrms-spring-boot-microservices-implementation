package com.niks.employeeservice.service;

import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;
import com.netflix.hystrix.contrib.javanica.annotation.HystrixProperty;
import com.niks.employeeservice.repository.EmployeeRepository;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.niks.employeeservice.constants.ErrorMessageConstants;
import com.niks.employeeservice.model.builder.EmployeeBuilder;
import com.niks.employeeservice.model.db.Employee;
import com.niks.employeeservice.request.employee.EmployeeCreateRequest;
import com.niks.employeeservice.request.employee.EmployeeSearchRequest;
import com.niks.employeeservice.request.employee.EmployeeUpdateRequest;
import com.niks.employeeservice.service.exception.BadRequestException;
import com.niks.employeeservice.service.exception.EntityAlreadyExistsException;
import com.niks.employeeservice.service.exception.EntityNotFoundException;

@Service
public class EmployeeService {

  @Autowired
  EmployeeRepository employeeRepository;

  @Autowired
  EmployeeBuilder employeeBuilder;

  @Autowired
  OrganizationServiceClient organizationServiceClient;

  public static final Logger LOGGER = LoggerFactory.getLogger(EmployeeService.class);

  public Employee getEmployeeById(Long id) throws EntityNotFoundException {
    return employeeRepository.findById(id)
        .orElseThrow(
            () -> new EntityNotFoundException(ErrorMessageConstants.EMPLOYEE_BY_ID_NOT_FOUND));
  }


  public Employee getEmployeeByEmployeeId(String employeeId) throws EntityNotFoundException {
    Employee employee = employeeRepository.findByEmpId(employeeId);
    if (employee == null) {
      throw new EntityNotFoundException(ErrorMessageConstants.EMPLOYEE_BY_ID_NOT_FOUND);
    } else {
      return employee;
    }
  }

  public Employee createEmployee(EmployeeCreateRequest employeeCreateRequest)
      throws EntityAlreadyExistsException, BadRequestException, EntityNotFoundException {
    List<Employee> employees = employeeRepository
        .findByEmpIdOrEmail(employeeCreateRequest.getEmpId(), employeeCreateRequest.getEmail());
    Optional<Employee> reportTo = getReportsToEmployee(employeeCreateRequest.getReportsTo());
    organizationServiceClient.getOrganizationById(employeeCreateRequest.getOrganizationId());
    if (employees.isEmpty()) {
      Employee employeeToCreate = employeeBuilder
          .buildFromRequest(employeeCreateRequest, reportTo.isPresent() ? reportTo.get() : null);
      employeeToCreate = employeeRepository.save(employeeToCreate);
      return employeeToCreate;
    } else {
      throw new EntityAlreadyExistsException(ErrorMessageConstants.EMPLOYEE_DUPLICATE_EMAIL);
    }
  }

  @Transactional
  public Employee updateEmployee(final Long id, EmployeeUpdateRequest employeeUpdateRequest)
      throws EntityNotFoundException, BadRequestException {
    Employee employeeToUpdate = employeeRepository.findById(id)
        .orElseThrow(
            () -> new EntityNotFoundException(ErrorMessageConstants.EMPLOYEE_BY_ID_NOT_FOUND));
    Optional<Employee> reportTo = getReportsToEmployee(employeeUpdateRequest.getReportsTo());
    Employee updatedEmployee = employeeBuilder
        .buildFromRequest(employeeToUpdate, employeeUpdateRequest,
            reportTo.isPresent() ? reportTo.get() : null);
    return employeeRepository.save(updatedEmployee);
  }

  private Optional<Employee> getReportsToEmployee(Long reportsTo) {
    Optional<Employee> reportTo = Optional.empty();
    if (reportsTo != null) {
      reportTo = employeeRepository.findById(reportsTo);
    }
    return reportTo;
  }

  public void deleteEmployeeById(Long id) throws EntityNotFoundException {
    Employee employeeToDelete = getEmployeeById(id);
    employeeRepository.deleteById(employeeToDelete.getId());
  }

  public List<Employee> getAllEmployees() {
    return employeeRepository.findAll();
  }

  @Transactional
  public List<Employee> searchEmployee(final EmployeeSearchRequest employeeSearchRequest) {
    return employeeRepository.searchEmployee(employeeSearchRequest);
  }

  private Employee createEmployee_FallBack(EmployeeCreateRequest employeeCreateRequest, Throwable temp) {

    System.out.println("Organization Service is down!!! fallback route enabled...");

    return new Employee();
  }
}