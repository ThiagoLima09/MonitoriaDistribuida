package br.com.monitoriadistribuida.client.controller;

import br.com.monitoriadistribuida.client.SessionContext;
import br.com.monitoriadistribuida.network.ResultadoLogin;
import br.com.monitoriadistribuida.network.ServerConnection;

import java.io.IOException;

public class LoginController {

    public SessionContext autenticar(String email, String senha) throws IOException {
        String emailNormalizado = normalizar(email);
        String senhaNormalizada = normalizar(senha);

        if (emailNormalizado.isEmpty()) {
            throw new IllegalArgumentException("Informe o e-mail.");
        }

        if (senhaNormalizada.isEmpty()) {
            throw new IllegalArgumentException("Informe a senha.");
        }

        ServerConnection conexaoServidor = new ServerConnection();

        try {
            ResultadoLogin resultado = conexaoServidor.login(emailNormalizado, senhaNormalizada);
            return new SessionContext(resultado.getNome(), emailNormalizado, resultado.getTipoUsuario(), conexaoServidor);
        } catch (IOException | RuntimeException e) {
            conexaoServidor.close();
            throw e;
        }
    }

    private String normalizar(String value) {
        return value == null ? "" : value.trim();
    }
}
