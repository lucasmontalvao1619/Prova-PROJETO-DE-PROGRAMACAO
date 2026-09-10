package com.lucdev.orcamento.dto;

import com.lucdev.orcamento.model.Categoria;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record TransacaoRequest(

        @NotBlank(message = "a descrição é obrigatória")
        String descricao,

        @NotNull(message = "o valor é obrigatório")
        @Positive(message = "o valor deve ser maior que zero")
        BigDecimal valor,

        @NotNull(message = "a categoria é obrigatória")
        Categoria categoria
) {
}
