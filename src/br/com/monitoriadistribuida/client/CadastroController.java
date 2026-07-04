package br.com.monitoriadistribuida.client;

import br.com.monitoriadistribuida.server.model.TipoUsuario;

public class CadastroController {

    public void validateCadastro(String nome,
                                 String email,
                                 String senha,
                                 String confirmacao,
                                 TipoUsuario tipoUsuario) {
        if (isBlank(nome)) {
            throw new IllegalArgumentException("Informe o nome completo.");
        }
        if (isBlank(email)) {
            throw new IllegalArgumentException("Informe o e-mail.");
        }
        if (isBlank(senha)) {
            throw new IllegalArgumentException("Informe a senha.");
        }
        if (isBlank(confirmacao)) {
            throw new IllegalArgumentException("Confirme a senha.");
        }
        if (!senha.equals(confirmacao)) {
            throw new IllegalArgumentException("A senha e a confirmacao nao conferem.");
        }
        if (tipoUsuario == null) {
            throw new IllegalArgumentException("Selecione o tipo de usuario.");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
