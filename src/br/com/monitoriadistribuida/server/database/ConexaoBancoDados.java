package br.com.monitoriadistribuida.server.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public final class ConexaoBancoDados {

    private static final String URL_PADRAO =
            "jdbc:mysql://localhost:3306/monitoria?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&characterEncoding=utf8";
    private static final String USUARIO_PADRAO = "root";
    private static final String SENHA_PADRAO = "123456";

    private ConexaoBancoDados() {
    }

    public static Connection abrirConexao() throws SQLException {
        return DriverManager.getConnection(getUrl(), getUsuario(), getSenha());
    }

    public static String getUrl() {
        return lerConfiguracao("monitoria.db.url", "MONITORIA_DB_URL", URL_PADRAO);
    }

    public static String getUsuario() {
        return lerConfiguracao("monitoria.db.usuario", "MONITORIA_DB_USUARIO", USUARIO_PADRAO);
    }

    public static String getSenha() {
        return lerConfiguracao("monitoria.db.senha", "MONITORIA_DB_SENHA", SENHA_PADRAO);
    }

    private static String lerConfiguracao(String propriedade, String variavelAmbiente, String valorPadrao) {
        String valor = System.getProperty(propriedade);

        if (valor != null && !valor.trim().isEmpty()) {
            return valor.trim();
        }

        valor = System.getenv(variavelAmbiente);

        if (valor != null) {
            return valor;
        }

        return valorPadrao;
    }
}
