package br.com.monitoriadistribuida.server.service;

import br.com.monitoriadistribuida.server.model.Usuario;
import br.com.monitoriadistribuida.server.repository.UsuarioRepositorioMySql;

import java.util.List;
import java.sql.SQLException;

public class UserService {

    private final UsuarioRepositorioMySql usuarioRepositorio;

    public UserService() {
        try {
            usuarioRepositorio = new UsuarioRepositorioMySql();
        } catch (SQLException e) {
            throw new IllegalStateException("Não foi possível conectar ao banco de dados MySQL.", e);
        }
    }

    public boolean cadastrar(Usuario usuario) {

        if (usuario == null) {
            return false;
        }

        String email = usuario.getEmail();

        if (email == null || email.trim().isEmpty()) {
            return false;
        }

        try {
            return usuarioRepositorio.cadastrar(usuario);
        } catch (SQLException e) {
            throw new IllegalStateException("Falha ao cadastrar usuário no banco de dados.", e);
        }
    }

    public Usuario login(String email, String senha) {

        if (email == null || senha == null) {
            return null;
        }

        try {
            return usuarioRepositorio.login(email, senha);
        } catch (SQLException e) {
            throw new IllegalStateException("Falha ao consultar login no banco de dados.", e);
        }
    }

    public List<Usuario> listarUsuarios() {
        try {
            return usuarioRepositorio.listarUsuarios();
        } catch (SQLException e) {
            throw new IllegalStateException("Falha ao listar usuários no banco de dados.", e);
        }
    }

    public Usuario buscarPorEmail(String email) {
        if (email == null) {
            return null;
        }

        try {
            return usuarioRepositorio.buscarPorEmail(email);
        } catch (SQLException e) {
            throw new IllegalStateException("Falha ao buscar usuário no banco de dados.", e);
        }
    }
}
