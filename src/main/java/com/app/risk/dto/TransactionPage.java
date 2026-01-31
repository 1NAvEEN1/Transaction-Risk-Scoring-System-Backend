package com.app.risk.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionPage {
    private List<TransactionDTO> content;
    private Integer page;
    private Integer size;
    private Long totalElements;
    private Integer totalPages;
}

