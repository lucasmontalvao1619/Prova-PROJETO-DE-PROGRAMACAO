package com.lucdev.orcamento.dto;

import java.math.BigDecimal;
import java.util.List;

public record ResumoResponse(
        BigDecimal totalReceitas,
        BigDecimal totalDespesas,
        BigDecimal saldo,
        List<GastoPorCategoria> gastosPorCategoria
) {

    public record GastoPorCategoria(String categoria, String rotulo, BigDecimal total) {
    }
}
