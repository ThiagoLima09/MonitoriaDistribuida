package br.com.monitoriadistribuida.client;

import br.com.monitoriadistribuida.server.model.TipoUsuario;

public class SessionContext {

    private final String nome;
    private final String login;
    private final TipoUsuario tipoUsuario;

    public SessionContext(String nome, String login, TipoUsuario tipoUsuario) {
        this.nome = nome;
        this.login = login;
        this.tipoUsuario = tipoUsuario;
    }

    public String getNome() {
        return nome;
    }

    public String getLogin() {
        return login;
    }

    public TipoUsuario getTipoUsuario() {
        return tipoUsuario;
    }
}
