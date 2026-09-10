package com.lucdev.orcamento.service;

import com.lucdev.orcamento.dto.ResumoResponse;
import com.lucdev.orcamento.dto.TransacaoRequest;
import com.lucdev.orcamento.dto.TransacaoResponse;
import com.lucdev.orcamento.exception.RecursoNaoEncontradoException;
import com.lucdev.orcamento.model.Categoria;
import com.lucdev.orcamento.model.TipoTransacao;
import com.lucdev.orcamento.model.Transacao;
import com.lucdev.orcamento.repository.TransacaoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class TransacaoService {

    private final TransacaoRepository repository;

    public TransacaoService(TransacaoRepository repository) {
        this.repository = repository;
    }

    public List<TransacaoResponse> listar() {
        return repository.findAllByOrderByDataHoraDesc().stream()
                .map(TransacaoResponse::de)
                .toList();
    }

    public TransacaoResponse buscar(Long id) {
        return TransacaoResponse.de(buscarEntidade(id));
    }

    @Transactional
    public TransacaoResponse criar(TransacaoRequest request) {
        Transacao transacao = new Transacao(
                request.descricao().trim(), request.valor(), request.categoria());
        return TransacaoResponse.de(repository.save(transacao));
    }

    @Transactional
    public TransacaoResponse atualizar(Long id, TransacaoRequest request) {
        Transacao transacao = buscarEntidade(id);
        transacao.setDescricao(request.descricao().trim());
        transacao.setValor(request.valor());
        transacao.setCategoria(request.categoria());
        return TransacaoResponse.de(repository.save(transacao));
    }

    @Transactional
    public void apagar(Long id) {
        repository.delete(buscarEntidade(id));
    }

    public ResumoResponse resumo() {
        List<Transacao> transacoes = repository.findAll();

        BigDecimal receitas = somar(transacoes, TipoTransacao.RECEITA);
        BigDecimal despesas = somar(transacoes, TipoTransacao.DESPESA);

        Map<Categoria, BigDecimal> totalPorCategoria = new HashMap<>();
        for (Transacao transacao : transacoes) {
            if (transacao.getTipo() == TipoTransacao.DESPESA) {
                Categoria categoria = transacao.getCategoria();
                BigDecimal acumulado = totalPorCategoria.getOrDefault(categoria, BigDecimal.ZERO);
                totalPorCategoria.put(categoria, acumulado.add(transacao.getValor()));
            }
        }

        List<ResumoResponse.GastoPorCategoria> gastos = new ArrayList<>();
        for (Map.Entry<Categoria, BigDecimal> item : totalPorCategoria.entrySet()) {
            Categoria categoria = item.getKey();
            gastos.add(new ResumoResponse.GastoPorCategoria(
                    categoria.name(), categoria.getRotulo(), item.getValue()));
        }

        gastos.sort((a, b) -> b.total().compareTo(a.total()));

        return new ResumoResponse(receitas, despesas, receitas.subtract(despesas), gastos);
    }

    private BigDecimal somar(List<Transacao> transacoes, TipoTransacao tipo) {
        BigDecimal total = BigDecimal.ZERO;
        for (Transacao transacao : transacoes) {
            if (transacao.getTipo() == tipo) {
                total = total.add(transacao.getValor());
            }
        }
        return total;
    }

    private Transacao buscarEntidade(Long id) {
        return repository.findById(id).orElseThrow(
                () -> new RecursoNaoEncontradoException("Transação %d não encontrada.".formatted(id)));
    }
}
