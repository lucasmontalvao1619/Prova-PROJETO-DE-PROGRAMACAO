package com.lucdev.orcamento.model;

public enum Categoria {

    ALIMENTACAO("Alimentação", TipoTransacao.DESPESA),
    TRANSPORTE("Transporte", TipoTransacao.DESPESA),
    MORADIA("Moradia", TipoTransacao.DESPESA),
    LAZER("Lazer", TipoTransacao.DESPESA),
    SAUDE("Saúde", TipoTransacao.DESPESA),

    SALARIO("Salário", TipoTransacao.RECEITA),
    PRESENTE("Presente", TipoTransacao.RECEITA),
    EXTRA("Renda extra", TipoTransacao.RECEITA);

    private final String rotulo;
    private final TipoTransacao tipo;

    Categoria(String rotulo, TipoTransacao tipo) {
        this.rotulo = rotulo;
        this.tipo = tipo;
    }

    public String getRotulo() {
        return rotulo;
    }

    public TipoTransacao getTipo() {
        return tipo;
    }
}
