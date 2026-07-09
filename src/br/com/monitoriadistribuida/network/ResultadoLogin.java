package br.com.monitoriadistribuida.network;

import br.com.monitoriadistribuida.server.model.TipoUsuario;

public class ResultadoLogin {

    private final TipoUsuario tipoUsuario;
    private final String nome;

    public ResultadoLogin(TipoUsuario tipoUsuario, String nome) {
        this.tipoUsuario = tipoUsuario;
        this.nome = nome;
    }

    public TipoUsuario getTipoUsuario() {
        return tipoUsuario;
    }

    public String getNome() {
        return nome;
    }
}
