package br.com.monitoriadistribuida.server.model;

import java.text.Normalizer;

public enum Disciplina {

    PROGRAMACAO("Programação"),
    ESTRUTURA_DE_DADOS("Estrutura de dados"),
    CALCULO_1("Cálculo 1"),
    BANCO_DE_DADOS("Banco de dados"),
    ENGENHARIA_DE_SOFTWARE("Engenharia de software");

    private String nomeExibicao;

    Disciplina(String nomeExibicao) {
        this.nomeExibicao = nomeExibicao;
    }

    public String getNomeExibicao() {
        return nomeExibicao;
    }

    public static Disciplina fromTexto(String texto) {
        String normalizado = Normalizer.normalize(texto.trim(), Normalizer.Form.NFD);
        normalizado = normalizado.replaceAll("\\p{M}", "");
        normalizado = normalizado.toUpperCase();
        normalizado = normalizado.replace(" ", "_");

        return Disciplina.valueOf(normalizado);
    }
}