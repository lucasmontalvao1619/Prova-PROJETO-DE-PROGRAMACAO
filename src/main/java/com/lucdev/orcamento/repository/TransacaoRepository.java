package com.lucdev.orcamento.repository;

import com.lucdev.orcamento.model.Transacao;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TransacaoRepository extends JpaRepository<Transacao, Long> {

    List<Transacao> findAllByOrderByDataHoraDesc();
}
