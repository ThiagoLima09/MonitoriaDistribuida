package br.com.monitoriadistribuida.server; 

import br.com.monitoriadistribuida.server.model.TipoUsuario;
import br.com.monitoriadistribuida.server.model.Usuario; 
import br.com.monitoriadistribuida.server.service.UserService; 

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.List;

public class ClientHandler implements Runnable {

    private Socket clientSocket;

    private UserService userService; 

    public ClientHandler(Socket clientSocket, UserService userService) { 
        this.clientSocket = clientSocket; 
        this.userService = userService;
    } 

    @Override 
    public void run() {

        try (
            BufferedReader entrada = new BufferedReader(
                new InputStreamReader(clientSocket.getInputStream()) 
            );

            PrintWriter saida = new PrintWriter( 
                clientSocket.getOutputStream(), 
                true 
            ) 
        ) { 

            saida.println("OK;Conectado ao servidor central");

            String mensagemRecebida;

            while ((mensagemRecebida = entrada.readLine()) != null) {

                System.out.println("Cliente enviou: " + mensagemRecebida); 

                String resposta = processarComando(mensagemRecebida);

                saida.println(resposta);

                
                if (mensagemRecebida.equalsIgnoreCase("SAIR")) {
                    break; 
                }

            }

        } catch (IOException e) { 

            System.out.println("Cliente desconectado ou erro de comunicação: " + e.getMessage());

        } finally {
            try {
                clientSocket.close();
            } catch (IOException e) {
                System.out.println("Erro ao fechar conexão do cliente: " + e.getMessage());
            } 

        }

    }

    private String processarComando(String mensagem) { 

        if (mensagem == null || mensagem.trim().isEmpty()) { 
            return "ERRO;Comando vazio"; 
        }

        String[] partes = mensagem.split(";");

        String comando = partes[0].toUpperCase();

        if (comando.equals("CADASTRO")) {
            return cadastrarUsuario(partes); 
        }

        if (comando.equals("LOGIN")) {
            return realizarLogin(partes);
        } 

        if (comando.equals("LISTAR_USUARIOS")) {
            return listarUsuarios();
        }

        if (comando.equals("SAIR")) { 
            return "OK;Conexão encerrada";
        }

        return "ERRO;Comando inválido";
    }

    private String cadastrarUsuario(String[] partes) {

        if (partes.length != 5) { 
            return "ERRO;Formato correto: CADASTRO;nome;email;senha;tipo"; 
        } 

        try {

            String nome = partes[1];
            String email = partes[2];
            String senha = partes[3];
            TipoUsuario tipo = TipoUsuario.valueOf(partes[4].toUpperCase());

            Usuario usuario = new Usuario(nome, email, senha, tipo);

            boolean cadastrado = userService.cadastrar(usuario);

            if (cadastrado) {
                return "OK;Usuário cadastrado com sucesso"; 
            } else {
                return "ERRO;Email já cadastrado"; 
            } 

        } catch (IllegalArgumentException e) { 
            return "ERRO;Tipo de usuário inválido. Use ALUNO ou MONITOR";
        } 

    }

    private String realizarLogin(String[] partes) {

        if (partes.length != 3) {
            return "ERRO;Formato correto: LOGIN;email;senha";
        }

        String email = partes[1];

        String senha = partes[2];

        Usuario usuario = userService.login(email, senha);

        if (usuario == null) {
            return "ERRO;Email ou senha inválidos";
        }

        return "OK;Login realizado;" + usuario.getTipo() + ";" + usuario.getNome();
    }

    private String listarUsuarios() {

    List<Usuario> usuarios = userService.listarUsuarios();

    if (usuarios.isEmpty()) {
        return "OK;Nenhum usuário cadastrado";
    }

    StringBuilder resposta = new StringBuilder();

    resposta.append("OK;USUARIOS");

    for (Usuario usuario : usuarios) {
        resposta.append(";")
                .append(usuario.getNome())
                .append(",")
                .append(usuario.getEmail())
                .append(",")
                .append(usuario.getTipo());
    }

    return resposta.toString();
}
} 