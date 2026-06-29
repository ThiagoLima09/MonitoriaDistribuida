package br.com.monitoriadistribuida.server.service; 

import br.com.monitoriadistribuida.server.model.Usuario;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class UserService { 

    private Map<String, Usuario> usuarios = new ConcurrentHashMap<>();

    public boolean cadastrar(Usuario usuario) {

        if (usuario == null) { 
            return false;
        }

        String email = usuario.getEmail();

        if (email == null || email.trim().isEmpty()) {
            return false; 
        }

        Usuario usuarioExistente = usuarios.putIfAbsent(email, usuario);

        return usuarioExistente == null; 
    } 

    public Usuario login(String email, String senha) { 

        if (email == null || senha == null) {
            return null; 
        } 

        Usuario usuario = usuarios.get(email); 

        if (usuario == null) { 
            return null; 
        }

        if (!usuario.getSenha().equals(senha)) {
            return null; 
        } 

        return usuario; 
    }

    public List<Usuario> listarUsuarios() {
        return new ArrayList<>(usuarios.values());
    }

} 