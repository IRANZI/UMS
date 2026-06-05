package com.java.Utility.Billing.System.service;

import com.java.Utility.Billing.System.dto.request.CustomerRequest;
import com.java.Utility.Billing.System.dto.response.CustomerResponse;
import com.java.Utility.Billing.System.dto.response.PageResponse;
import com.java.Utility.Billing.System.entity.Customer;
import com.java.Utility.Billing.System.entity.User;
import com.java.Utility.Billing.System.enums.CustomerStatus;
import com.java.Utility.Billing.System.exception.BadRequestException;
import com.java.Utility.Billing.System.exception.ResourceNotFoundException;
import com.java.Utility.Billing.System.mapper.EntityMapper;
import com.java.Utility.Billing.System.repository.CustomerRepository;
import com.java.Utility.Billing.System.repository.UserRepository;
import com.java.Utility.Billing.System.util.PageMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Customer CRUD — stores billing profile (name, national ID, email, phone, address, status).
 * Link a userId so the customer can log in and view their own bills.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final UserRepository userRepository;

    @Transactional
    public CustomerResponse create(CustomerRequest request) {
        validateUnique(request, null);
        Customer customer = buildCustomerFromRequest(request);
        customer = customerRepository.save(customer);
        log.info("Customer created: {} - {}", customer.getNationalId(), customer.getFullNames());
        return EntityMapper.toCustomerResponse(customer);
    }

    public CustomerResponse getById(Long id) {
        return EntityMapper.toCustomerResponse(findCustomer(id));
    }

    public PageResponse<CustomerResponse> getAll(Pageable pageable) {
        Page<CustomerResponse> page = customerRepository.findAll(pageable).map(EntityMapper::toCustomerResponse);
        return PageMapper.toPageResponse(page);
    }

    public PageResponse<CustomerResponse> getByStatus(CustomerStatus status, Pageable pageable) {
        Page<CustomerResponse> page = customerRepository.findByStatus(status, pageable)
                .map(EntityMapper::toCustomerResponse);
        return PageMapper.toPageResponse(page);
    }

    public PageResponse<CustomerResponse> search(String query, Pageable pageable) {
        Page<CustomerResponse> page = customerRepository.search(query, pageable).map(EntityMapper::toCustomerResponse);
        return PageMapper.toPageResponse(page);
    }

    @Transactional
    public CustomerResponse update(Long id, CustomerRequest request) {
        Customer customer = findCustomer(id);
        validateUnique(request, id);
        customer.setFullNames(request.getFullNames());
        customer.setNationalId(request.getNationalId());
        customer.setEmail(request.getEmail());
        customer.setPhoneNumber(request.getPhoneNumber());
        customer.setAddress(request.getAddress());
        customer.setStatus(request.getStatus() != null ? request.getStatus() : customer.getStatus());
        customer.setUser(resolveUser(request.getUserId()));
        customer = customerRepository.save(customer);
        log.info("Customer updated: {} - status={}", customer.getNationalId(), customer.getStatus());
        return EntityMapper.toCustomerResponse(customer);
    }

    @Transactional
    public CustomerResponse updateStatus(Long id, CustomerStatus status) {
        Customer customer = findCustomer(id);
        customer.setStatus(status);
        customer = customerRepository.save(customer);
        log.info("Customer {} status changed to {}", customer.getNationalId(), status);
        return EntityMapper.toCustomerResponse(customer);
    }

    @Transactional
    public void delete(Long id) {
        Customer customer = findCustomer(id);
        customerRepository.delete(customer);
        log.info("Customer deleted: {}", customer.getNationalId());
    }

    public Customer findCustomer(Long id) {
        return customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id: " + id));
    }

    public void ensureCustomerCanReceiveBills(Customer customer) {
        if (customer.getStatus() == CustomerStatus.INACTIVE) {
            throw new BadRequestException("Inactive customers cannot receive bills");
        }
    }

    private Customer buildCustomerFromRequest(CustomerRequest request) {
        return Customer.builder()
                .fullNames(request.getFullNames())
                .nationalId(request.getNationalId())
                .email(request.getEmail())
                .phoneNumber(request.getPhoneNumber())
                .address(request.getAddress())
                .status(request.getStatus() != null ? request.getStatus() : CustomerStatus.ACTIVE)
                .user(resolveUser(request.getUserId()))
                .build();
    }

    private void validateUnique(CustomerRequest request, Long excludeId) {
        customerRepository.findByNationalId(request.getNationalId()).ifPresent(existing -> {
            if (excludeId == null || !existing.getId().equals(excludeId)) {
                throw new BadRequestException("Customer with National ID '" + request.getNationalId() + "' already exists");
            }
        });
        customerRepository.findByEmail(request.getEmail()).ifPresent(existing -> {
            if (excludeId == null || !existing.getId().equals(excludeId)) {
                throw new BadRequestException("Customer with email '" + request.getEmail() + "' already exists");
            }
        });
    }

    // Optional — connects this customer record to a login account (ROLE_CUSTOMER)
    private User resolveUser(Long userId) {
        if (userId == null) {
            return null;
        }
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
    }
}
