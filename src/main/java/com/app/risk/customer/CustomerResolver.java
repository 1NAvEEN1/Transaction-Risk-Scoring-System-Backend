package com.app.risk.customer;

import lombok.RequiredArgsConstructor;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class CustomerResolver {

    private final CustomerRepository customerRepository;

    @QueryMapping
    public List<Customer> customers() {
        return customerRepository.findAll();
    }
}
