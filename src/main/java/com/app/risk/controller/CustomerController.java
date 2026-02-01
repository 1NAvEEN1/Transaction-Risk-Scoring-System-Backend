package com.app.risk.controller;

import com.app.risk.model.Customer;
import com.app.risk.service.CustomerService;
import lombok.RequiredArgsConstructor;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService customerService;

    @QueryMapping
    public List<Customer> customers() {
        return customerService.findAll();
    }
}

