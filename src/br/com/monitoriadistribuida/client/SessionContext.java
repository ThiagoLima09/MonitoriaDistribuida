package br.com.monitoriadistribuida.client;

import br.com.monitoriadistribuida.network.ServerConnection;
import br.com.monitoriadistribuida.server.model.TipoUsuario;

public class SessionContext {

    private final String nome;
    private final String login;
    private final TipoUsuario tipoUsuario;
    private final ServerConnection conexaoServidor;

    public SessionContext(String nome, String login, TipoUsuario tipoUsuario) {
        this(nome, login, tipoUsuario, null);
    }

    public SessionContext(String nome, String login, TipoUsuario tipoUsuario, ServerConnection conexaoServidor) {
        this.nome = nome;
        this.login = login;
        this.tipoUsuario = tipoUsuario;
        this.conexaoServidor = conexaoServidor;
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

    public ServerConnection getConexaoServidor() {
        return conexaoServidor;
    }

    public void fecharConexaoServidor() {
        if (conexaoServidor != null) {
            conexaoServidor.close();
        }
    }
}
