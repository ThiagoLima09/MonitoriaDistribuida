package br.com.monitoriadistribuida.server.model;

public enum TipoUsuario {
    MONITOR("M", "Monitor"),
    ALUNO("A", "Aluno");

    private final String codigoBanco;
    private final String descricao;

    TipoUsuario(String codigoBanco, String descricao) {
        this.codigoBanco = codigoBanco;
        this.descricao = descricao;
    }

    public String getCodigoBanco() {
        return codigoBanco;
    }

    public String getDescricao() {
        return descricao;
    }

    public static TipoUsuario fromTexto(String texto) {
        if (texto == null || texto.trim().isEmpty()) {
            throw new IllegalArgumentException("Tipo de usuário vazio.");
        }

        String valorNormalizado = texto.trim().toUpperCase();

        for (TipoUsuario tipoUsuario : values()) {
            if (tipoUsuario.name().equals(valorNormalizado)
                    || tipoUsuario.codigoBanco.equals(valorNormalizado)
                    || tipoUsuario.descricao.toUpperCase().equals(valorNormalizado)) {
                return tipoUsuario;
            }
        }

        throw new IllegalArgumentException("Tipo de usuário inválido: " + texto);
    }
}
