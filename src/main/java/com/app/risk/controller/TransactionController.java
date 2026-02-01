package com.app.risk.controller;

import com.app.risk.dto.TransactionDTO;
import com.app.risk.dto.TransactionInput;
import com.app.risk.dto.TransactionPage;
import com.app.risk.service.TransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;

    @QueryMapping
    public TransactionPage transactions(@Argument Integer page, @Argument Integer size, @Argument String status) {
        return transactionService.getTransactions(page, size, status);
    }

    @QueryMapping
    public TransactionDTO transaction(@Argument Long id) {
        return transactionService.getTransaction(id);
    }

    @MutationMapping
    public TransactionDTO submitTransaction(@Argument @Valid TransactionInput input) {
        return transactionService.submitTransaction(input);
    }
}

