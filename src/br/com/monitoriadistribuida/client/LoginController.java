package br.com.monitoriadistribuida.client;

import br.com.monitoriadistribuida.server.model.TipoUsuario;

public class LoginController {

    public SessionContext authenticate(String email, String senha, TipoUsuario tipoUsuario) {
        String normalizedEmail = normalize(email);
        String normalizedSenha = normalize(senha);

        if (normalizedEmail.isEmpty()) {
            throw new IllegalArgumentException("Informe o e-mail.");
        }

        if (normalizedSenha.isEmpty()) {
            throw new IllegalArgumentException("Informe a senha.");
        }

        if (tipoUsuario == null) {
            throw new IllegalArgumentException("Selecione o tipo de usuario.");
        }

        return new SessionContext(buildDisplayName(normalizedEmail), normalizedEmail, tipoUsuario);
    }

    private String buildDisplayName(String email) {
        int atIndex = email.indexOf('@');
        String base = atIndex > 0 ? email.substring(0, atIndex) : email;
        base = base.replace('.', ' ').replace('_', ' ').trim();
        if (base.isEmpty()) {
            return "Usuario";
        }
        return Character.toUpperCase(base.charAt(0)) + base.substring(1);
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
