package br.com.monitoriadistribuida.client.controller;

import br.com.monitoriadistribuida.network.ServerConnection;
import br.com.monitoriadistribuida.server.model.TipoUsuario;

import java.io.IOException;

public class CadastroController {

    public void cadastrar(String nome,
                          String email,
                          String senha,
                          String confirmacao,
                          TipoUsuario tipoUsuario) throws IOException {
        validarCadastro(nome, email, senha, confirmacao, tipoUsuario);

        try (ServerConnection conexaoServidor = new ServerConnection()) {
            conexaoServidor.cadastrar(nome.trim(), email.trim(), senha.trim(), tipoUsuario);
        }
    }

    private void validarCadastro(String nome,
                                 String email,
                                 String senha,
                                 String confirmacao,
                                 TipoUsuario tipoUsuario) {
        if (estaVazio(nome)) {
            throw new IllegalArgumentException("Informe o nome completo.");
        }
        if (estaVazio(email)) {
            throw new IllegalArgumentException("Informe o e-mail.");
        }
        if (estaVazio(senha)) {
            throw new IllegalArgumentException("Informe a senha.");
        }
        if (estaVazio(confirmacao)) {
            throw new IllegalArgumentException("Confirme a senha.");
        }
        if (!senha.equals(confirmacao)) {
            throw new IllegalArgumentException("A senha e a confirmação não conferem.");
        }
        if (tipoUsuario == null) {
            throw new IllegalArgumentException("Selecione o tipo de usuário.");
        }
    }

    private boolean estaVazio(String valor) {
        return valor == null || valor.trim().isEmpty();
    }
}
