package br.com.monitoriadistribuida.server;

import br.com.monitoriadistribuida.server.model.TipoUsuario;
import br.com.monitoriadistribuida.server.model.Usuario;

import java.net.Socket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SessionManager {

    private final Map<String, SessaoUsuario> sessoesPorConexao = new ConcurrentHashMap<>();

    public void registrarSessao(Socket socket, Usuario usuario) {
        if (socket == null || usuario == null) {
            return;
        }

        sessoesPorConexao.put(chaveConexao(socket), new SessaoUsuario(usuario));
    }

    public SessaoUsuario buscarSessao(Socket socket) {
        if (socket == null) {
            return null;
        }

        return sessoesPorConexao.get(chaveConexao(socket));
    }

    public boolean existeSessaoAtiva(Socket socket) {
        return buscarSessao(socket) != null;
    }

    public void removerSessao(Socket socket) {
        if (socket == null) {
            return;
        }

        sessoesPorConexao.remove(chaveConexao(socket));
    }

    public int contarSessoesAtivas() {
        return sessoesPorConexao.size();
    }

    private String chaveConexao(Socket socket) {
        return socket.getRemoteSocketAddress().toString();
    }

    public static class SessaoUsuario {

        private final String nome;
        private final String email;
        private final TipoUsuario tipoUsuario;
        private final long criadaEm;

        private SessaoUsuario(Usuario usuario) {
            this.nome = usuario.getNome();
            this.email = usuario.getEmail();
            this.tipoUsuario = usuario.getTipo();
            this.criadaEm = System.currentTimeMillis();
        }

        public String getNome() {
            return nome;
        }

        public String getEmail() {
            return email;
        }

        public TipoUsuario getTipoUsuario() {
            return tipoUsuario;
        }

        public long getCriadaEm() {
            return criadaEm;
        }
    }
}
