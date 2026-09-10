package com.lucdev.orcamento.controller;

import com.lucdev.orcamento.dto.ResumoResponse;
import com.lucdev.orcamento.dto.TransacaoRequest;
import com.lucdev.orcamento.dto.TransacaoResponse;
import com.lucdev.orcamento.model.Categoria;
import com.lucdev.orcamento.service.TransacaoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/api")
public class TransacaoController {

    private final TransacaoService service;

    public TransacaoController(TransacaoService service) {
        this.service = service;
    }

    @GetMapping("/transacoes")
    public ResponseEntity<List<TransacaoResponse>> listar() {
        return ResponseEntity.ok(service.listar());
    }

    @GetMapping("/transacoes/{id}")
    public ResponseEntity<TransacaoResponse> buscar(@PathVariable Long id) {
        return ResponseEntity.ok(service.buscar(id));
    }

    @PostMapping("/transacoes")
    public ResponseEntity<TransacaoResponse> criar(@RequestBody @Valid TransacaoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.criar(request));
    }

    @PutMapping("/transacoes/{id}")
    public ResponseEntity<TransacaoResponse> atualizar(@PathVariable Long id,
                                                       @RequestBody @Valid TransacaoRequest request) {
        return ResponseEntity.ok(service.atualizar(id, request));
    }

    @DeleteMapping("/transacoes/{id}")
    public ResponseEntity<Void> apagar(@PathVariable Long id) {
        service.apagar(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/resumo")
    public ResponseEntity<ResumoResponse> resumo() {
        return ResponseEntity.ok(service.resumo());
    }

    @GetMapping("/categorias")
    public ResponseEntity<List<CategoriaResponse>> categorias() {
        return ResponseEntity.ok(Arrays.stream(Categoria.values())
                .map(c -> new CategoriaResponse(c.name(), c.getRotulo(), c.getTipo().name()))
                .toList());
    }

    public record CategoriaResponse(String nome, String rotulo, String tipo) {
    }
}
