package br.com.monitoriadistribuida.server;

import br.com.monitoriadistribuida.server.model.Disciplina;
import br.com.monitoriadistribuida.server.model.MonitorDisponivel;
import br.com.monitoriadistribuida.server.model.StatusMonitor;
import br.com.monitoriadistribuida.server.model.TipoUsuario;
import br.com.monitoriadistribuida.server.model.Usuario;
import br.com.monitoriadistribuida.server.service.MonitorService;
import br.com.monitoriadistribuida.server.service.UserService;

import java.util.List;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.List;

public class ClientHandler implements Runnable {

    private Socket clientSocket;
    private UserService userService;
    private MonitorService monitorService;

    public ClientHandler(Socket clientSocket, UserService userService, MonitorService monitorService) {
        this.clientSocket = clientSocket;
        this.userService = userService;
        this.monitorService = monitorService;
    }

    @Override
    public void run() {

        try (
                BufferedReader entrada = new BufferedReader(
                        new InputStreamReader(clientSocket.getInputStream()));

                PrintWriter saida = new PrintWriter(
                        clientSocket.getOutputStream(),
                        true)) {

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

        if (comando.equals("LISTAR_DISCIPLINAS")) {
            return listarDisciplinas();
        }

        if (comando.equals("ATUALIZAR_STATUS")) {
            return atualizarStatusMonitor(partes);
        }

        if (comando.equals("LISTAR_MONITORES")) {
            return listarMonitores(partes);
        }

        if (comando.equals("SOLICITAR_ATENDIMENTO")) {
            return solicitarAtendimento(partes);
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

    private String listarDisciplinas() {
        StringBuilder resposta = new StringBuilder();

        resposta.append("OK;DISCIPLINAS");

        for (Disciplina disciplina : Disciplina.values()) {
            resposta.append(";")
                    .append(disciplina.name())
                    .append(",")
                    .append(disciplina.getNomeExibicao());
        }

        return resposta.toString();
    }

    private String atualizarStatusMonitor(String[] partes) {
        if (partes.length != 5) {
            return "ERRO;Formato correto: ATUALIZAR_STATUS;email;status;disciplina;porta";
        }

        try {
            String email = partes[1];

            StatusMonitor status = StatusMonitor.valueOf(partes[2].toUpperCase());

            Disciplina disciplina = Disciplina.fromTexto(partes[3]);

            int porta = Integer.parseInt(partes[4]);

            Usuario usuario = userService.buscarPorEmail(email);

            if (usuario == null) {
                return "ERRO;Usuário não encontrado";
            }

            if (usuario.getTipo() != TipoUsuario.MONITOR) {
                return "ERRO;Usuário não é monitor";
            }

            String ip = clientSocket.getInetAddress().getHostAddress();

            boolean atualizado = monitorService.atualizarStatus(usuario, status, disciplina, ip, porta);

            if (!atualizado) {
                return "ERRO;Não foi possível atualizar status do monitor";
            }

            return "OK;Status do monitor atualizado";

        } catch (IllegalArgumentException e) {
            return "ERRO;Status, disciplina ou porta inválida";
        }
    }

    private String listarMonitores(String[] partes) {
        if (partes.length != 2) {
            return "ERRO;Formato correto: LISTAR_MONITORES;disciplina";
        }

        try {
            Disciplina disciplina = Disciplina.fromTexto(partes[1]);

            List<MonitorDisponivel> monitores = monitorService.listarPorDisciplina(disciplina);

            if (monitores.isEmpty()) {
                return "ERRO;Nenhum monitor disponível para essa disciplina";
            }

            StringBuilder resposta = new StringBuilder();

            resposta.append("OK;MONITORES");

            for (MonitorDisponivel monitor : monitores) {
                resposta.append(";")
                        .append(monitor.getNome())
                        .append(",")
                        .append(monitor.getEmail())
                        .append(",")
                        .append(monitor.getDisciplina().getNomeExibicao())
                        .append(",")
                        .append(monitor.getIp())
                        .append(",")
                        .append(monitor.getPorta());
            }

            return resposta.toString();

        } catch (IllegalArgumentException e) {
            return "ERRO;Disciplina inválida";
        }
    }

    private String solicitarAtendimento(String[] partes) {
        if (partes.length != 3) {
            return "ERRO;Formato correto: SOLICITAR_ATENDIMENTO;emailAluno;emailMonitor";
        }

        String emailAluno = partes[1];

        String emailMonitor = partes[2];

        Usuario aluno = userService.buscarPorEmail(emailAluno);

        if (aluno == null) {
            return "ERRO;Aluno não encontrado";
        }

        if (aluno.getTipo() != TipoUsuario.ALUNO) {
            return "ERRO;Usuário solicitante não é aluno";
        }

        MonitorDisponivel monitor = monitorService.solicitarAtendimento(emailMonitor);

        if (monitor == null) {
            return "ERRO;Monitor não disponível";
        }

        return "OK;ATENDIMENTO;"
                + monitor.getNome() + ";"
                + monitor.getEmail() + ";"
                + monitor.getDisciplina().getNomeExibicao() + ";"
                + monitor.getIp() + ";"
                + monitor.getPorta();
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