package com.lucdev.orcamento.dto;

import com.lucdev.orcamento.model.Transacao;

import java.math.BigDecimal;
import java.time.LocalDateTime;
//(o método de(Transacao) converte entidade em resposta)
public record TransacaoResponse(
        Long id,
        String descricao,
        BigDecimal valor,
        String categoria,
        String categoriaRotulo,
        String tipo,
        LocalDateTime dataHora
) {

    public static TransacaoResponse de(Transacao t) {
        return new TransacaoResponse(
                t.getId(),
                t.getDescricao(),
                t.getValor(),
                t.getCategoria().name(),
                t.getCategoria().getRotulo(),
                t.getTipo().name(),
                t.getDataHora());
    }
}
