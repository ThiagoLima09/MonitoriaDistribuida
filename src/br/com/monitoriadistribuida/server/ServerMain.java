package br.com.monitoriadistribuida.server;

import br.com.monitoriadistribuida.server.database.ConexaoBancoDados;
import br.com.monitoriadistribuida.server.service.MonitorService;
import br.com.monitoriadistribuida.server.service.UserService;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class ServerMain {

    private static final int PORTA = 5001;

    public static void main(String[] args) {

        UserService userService;

        try {
            userService = new UserService();
            System.out.println("Banco de dados conectado: " + ConexaoBancoDados.getUrl());
        } catch (IllegalStateException e) {
            System.out.println("Erro ao iniciar persistência de usuários: " + e.getMessage());
            System.out.println("Verifique se o MySQL está ativo e se o schema monitoria existe.");
            return;
        }

        MonitorService monitorService = new MonitorService();

        SessionManager sessionManager = new SessionManager();

        System.out.println("Iniciando servidor central...");

        try (ServerSocket serverSocket = new ServerSocket(PORTA)) {

            System.out.println("Servidor iniciado na porta " + PORTA);

            while (true) {

                Socket clientSocket = serverSocket.accept();

                System.out.println("Novo cliente conectado: " + clientSocket.getInetAddress());

                ClientHandler clientHandler = new ClientHandler(clientSocket, userService, monitorService, sessionManager);

                Thread threadCliente = new Thread(clientHandler);

                threadCliente.start();
            }

        } catch (IOException e) {
            System.out.println("Erro no servidor: " + e.getMessage());
        }
    }
}
