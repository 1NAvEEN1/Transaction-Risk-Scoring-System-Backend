package com.app.risk.service;

import com.app.risk.exception.NotFoundException;
import com.app.risk.entity.Customer;
import com.app.risk.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerRepository customerRepository;

    public Customer findById(Long id) {
        return customerRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Customer not found with id: " + id));
    }

    public List<Customer> findAll() {
        return customerRepository.findAll();
    }
}


