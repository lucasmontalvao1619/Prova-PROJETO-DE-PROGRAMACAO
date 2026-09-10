package com.lucdev.orcamento.service;

import com.lucdev.orcamento.dto.ResumoResponse;
import com.lucdev.orcamento.dto.TransacaoRequest;
import com.lucdev.orcamento.dto.TransacaoResponse;
import com.lucdev.orcamento.exception.RecursoNaoEncontradoException;
import com.lucdev.orcamento.model.Categoria;
import com.lucdev.orcamento.model.Transacao;
import com.lucdev.orcamento.repository.TransacaoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransacaoServiceTest {

    @Mock
    private TransacaoRepository repository;

    @InjectMocks
    private TransacaoService service;

    @Test
    void criarSalvaComOTipoVindoDaCategoria() {
        when(repository.save(any())).thenAnswer(i -> i.getArgument(0));

        TransacaoResponse r = service.criar(
                new TransacaoRequest("Mercado", new BigDecimal("200.00"), Categoria.ALIMENTACAO));

        assertThat(r.descricao()).isEqualTo("Mercado");
        assertThat(r.tipo()).isEqualTo("DESPESA");
    }

    @Test
    void criarRemoveEspacoEmVoltaDaDescricao() {
        when(repository.save(any())).thenAnswer(i -> i.getArgument(0));

        assertThat(service.criar(new TransacaoRequest("  Uber  ", new BigDecimal("30"), Categoria.TRANSPORTE))
                .descricao()).isEqualTo("Uber");
    }

    @Test
    void buscarUmIdQueNaoExisteEstoura404() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.buscar(99L))
                .isInstanceOf(RecursoNaoEncontradoException.class)
                .hasMessageContaining("99");
    }

    @Test
    void atualizarTrocaOsCamposDoLancamentoExistente() {
        when(repository.findById(1L)).thenReturn(Optional.of(
                new Transacao("Mercado", new BigDecimal("200"), Categoria.ALIMENTACAO)));
        when(repository.save(any())).thenAnswer(i -> i.getArgument(0));

        TransacaoResponse r = service.atualizar(1L,
                new TransacaoRequest("Farmácia", new BigDecimal("45.50"), Categoria.SAUDE));

        assertThat(r.descricao()).isEqualTo("Farmácia");
        assertThat(r.valor()).isEqualByComparingTo("45.50");
        assertThat(r.categoria()).isEqualTo("SAUDE");
    }

    @Test
    void apagarRemoveOLancamentoEncontrado() {
        Transacao existente = new Transacao("Uber", new BigDecimal("30"), Categoria.TRANSPORTE);
        when(repository.findById(1L)).thenReturn(Optional.of(existente));

        service.apagar(1L);

        verify(repository).delete(existente);
    }

    @Test
    void oResumoSomaReceitasDespesasESaldo() {
        when(repository.findAll()).thenReturn(List.of(
                new Transacao("Salário", new BigDecimal("3000"), Categoria.SALARIO),
                new Transacao("Mercado", new BigDecimal("200"), Categoria.ALIMENTACAO),
                new Transacao("Uber", new BigDecimal("60"), Categoria.TRANSPORTE)));

        ResumoResponse resumo = service.resumo();

        assertThat(resumo.totalReceitas()).isEqualByComparingTo("3000");
        assertThat(resumo.totalDespesas()).isEqualByComparingTo("260");
        assertThat(resumo.saldo()).isEqualByComparingTo("2740");
    }

    @Test
    void oResumoAgrupaGastosDoMaiorParaOMenorEIgnoraReceitas() {
        when(repository.findAll()).thenReturn(List.of(
                new Transacao("Mercado", new BigDecimal("200"), Categoria.ALIMENTACAO),
                new Transacao("Padaria", new BigDecimal("50"), Categoria.ALIMENTACAO),
                new Transacao("Uber", new BigDecimal("60"), Categoria.TRANSPORTE),
                new Transacao("Salário", new BigDecimal("3000"), Categoria.SALARIO)));

        List<ResumoResponse.GastoPorCategoria> gastos = service.resumo().gastosPorCategoria();

        assertThat(gastos).hasSize(2);
        assertThat(gastos.get(0).categoria()).isEqualTo("ALIMENTACAO");
        assertThat(gastos.get(0).total()).isEqualByComparingTo("250");
        assertThat(gastos).noneMatch(g -> g.categoria().equals("SALARIO"));
    }

    @Test
    void oResumoDeUmOrcamentoVazioEZeroENaoUmErro() {
        when(repository.findAll()).thenReturn(List.of());

        assertThat(service.resumo().saldo()).isEqualByComparingTo("0");
        assertThat(service.resumo().gastosPorCategoria()).isEmpty();
    }
}
