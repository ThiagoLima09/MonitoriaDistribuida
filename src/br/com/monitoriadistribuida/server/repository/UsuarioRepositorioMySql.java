package br.com.monitoriadistribuida.server.repository;

import br.com.monitoriadistribuida.server.database.ConexaoBancoDados;
import br.com.monitoriadistribuida.server.model.TipoUsuario;
import br.com.monitoriadistribuida.server.model.Usuario;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class UsuarioRepositorioMySql {

    public UsuarioRepositorioMySql() throws SQLException {
        try (Connection conexao = ConexaoBancoDados.abrirConexao()) {
            garantirEstruturaTabela(conexao);
        }
    }

    public boolean cadastrar(Usuario usuario) throws SQLException {
        if (usuario == null || estaVazio(usuario.getEmail())) {
            return false;
        }

        if (buscarPorEmail(usuario.getEmail()) != null) {
            return false;
        }

        try (Connection conexao = ConexaoBancoDados.abrirConexao();
             PreparedStatement comando = conexao.prepareStatement(criarSqlCadastro())) {
            comando.setString(1, normalizarNome(usuario));
            comando.setString(2, usuario.getEmail());
            comando.setString(3, usuario.getSenha());
            comando.setString(4, usuario.getTipo().getCodigoBanco());

            comando.executeUpdate();
            return true;
        } catch (SQLIntegrityConstraintViolationException e) {
            return false;
        }
    }

    public Usuario login(String email, String senha) throws SQLException {
        if (estaVazio(email) || senha == null) {
            return null;
        }

        String sql = criarSqlConsultaUsuario("WHERE email = ? AND senha = ? LIMIT 1");

        try (Connection conexao = ConexaoBancoDados.abrirConexao();
             PreparedStatement comando = conexao.prepareStatement(sql)) {
            comando.setString(1, email.trim());
            comando.setString(2, senha);

            try (ResultSet resultado = comando.executeQuery()) {
                if (!resultado.next()) {
                    return null;
                }

                return converterUsuario(resultado);
            }
        }
    }

    public Usuario buscarPorEmail(String email) throws SQLException {
        if (estaVazio(email)) {
            return null;
        }

        String sql = criarSqlConsultaUsuario("WHERE email = ? LIMIT 1");

        try (Connection conexao = ConexaoBancoDados.abrirConexao();
             PreparedStatement comando = conexao.prepareStatement(sql)) {
            comando.setString(1, email.trim());

            try (ResultSet resultado = comando.executeQuery()) {
                if (!resultado.next()) {
                    return null;
                }

                return converterUsuario(resultado);
            }
        }
    }

    public List<Usuario> listarUsuarios() throws SQLException {
        List<Usuario> usuarios = new ArrayList<>();
        String sql = criarSqlConsultaUsuario("ORDER BY nome");

        try (Connection conexao = ConexaoBancoDados.abrirConexao();
             PreparedStatement comando = conexao.prepareStatement(sql);
             ResultSet resultado = comando.executeQuery()) {
            while (resultado.next()) {
                usuarios.add(converterUsuario(resultado));
            }
        }

        return usuarios;
    }

    private void garantirEstruturaTabela(Connection conexao) throws SQLException {
        try (Statement comando = conexao.createStatement()) {
            comando.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS usuario (
                        idUsuario INT NOT NULL AUTO_INCREMENT,
                        nome VARCHAR(160) NOT NULL,
                        email VARCHAR(160) NOT NULL,
                        senha VARCHAR(120) NOT NULL,
                        tipoUsuario ENUM('M', 'A') NOT NULL,
                        PRIMARY KEY (idUsuario)
                    )
                    """);

            if (!existeColuna(conexao, "usuario", "nome")) {
                comando.executeUpdate("ALTER TABLE usuario ADD COLUMN nome VARCHAR(160) NULL AFTER idUsuario");
            }

            if (!existeColuna(conexao, "usuario", "email")) {
                comando.executeUpdate("ALTER TABLE usuario ADD COLUMN email VARCHAR(160) NULL AFTER nome");

                if (existeColuna(conexao, "usuario", "nome")) {
                    comando.executeUpdate("UPDATE usuario SET email = nome WHERE email IS NULL OR TRIM(email) = ''");
                }
            }

            comando.executeUpdate("UPDATE usuario SET nome = email WHERE nome IS NULL OR TRIM(nome) = ''");
        }
    }

    private String criarSqlCadastro() {
        return "INSERT INTO usuario (nome, email, senha, tipoUsuario) VALUES (?, ?, ?, ?)";
    }

    private String criarSqlConsultaUsuario(String complemento) {
        return "SELECT nome, email, senha, tipoUsuario FROM usuario " + complemento;
    }

    private boolean existeColuna(Connection conexao, String tabela, String coluna) throws SQLException {
        DatabaseMetaData metaData = conexao.getMetaData();

        try (ResultSet colunas = metaData.getColumns(conexao.getCatalog(), null, tabela, coluna)) {
            if (colunas.next()) {
                return true;
            }
        }

        try (ResultSet colunas = metaData.getColumns(conexao.getCatalog(), null, tabela.toUpperCase(), coluna)) {
            if (colunas.next()) {
                return true;
            }
        }

        try (ResultSet colunas = metaData.getColumns(conexao.getCatalog(), null, tabela, coluna.toUpperCase())) {
            return colunas.next();
        }
    }

    private Usuario converterUsuario(ResultSet resultado) throws SQLException {
        String nome = resultado.getString("nome");
        String email = resultado.getString("email");
        String senha = resultado.getString("senha");
        TipoUsuario tipo = TipoUsuario.fromTexto(resultado.getString("tipoUsuario"));

        return new Usuario(nome, email, senha, tipo);
    }

    private boolean estaVazio(String valor) {
        return valor == null || valor.trim().isEmpty();
    }

    private String normalizarNome(Usuario usuario) {
        if (!estaVazio(usuario.getNome())) {
            return usuario.getNome().trim();
        }

        return usuario.getEmail().trim();
    }
}
