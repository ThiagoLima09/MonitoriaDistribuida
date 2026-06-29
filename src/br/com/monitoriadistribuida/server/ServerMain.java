package br.com.monitoriadistribuida.server; 

import br.com.monitoriadistribuida.server.service.UserService; 
import java.io.IOException; 
import java.net.ServerSocket; 
import java.net.Socket; 

public class ServerMain { 

    private static final int PORTA = 5000; 

    public static void main(String[] args) { 

        UserService userService = new UserService(); 

        System.out.println("Iniciando servidor central..."); 

        try (ServerSocket serverSocket = new ServerSocket(PORTA)) { 

            System.out.println("Servidor iniciado na porta " + PORTA);

            while (true) {

                Socket clientSocket = serverSocket.accept(); 
                System.out.println("Novo cliente conectado: " + clientSocket.getInetAddress());

                ClientHandler clientHandler = new ClientHandler(clientSocket, userService); 

                Thread threadCliente = new Thread(clientHandler); 

                threadCliente.start();
            }

        } catch (IOException e) { 
            System.out.println("Erro no servidor: " + e.getMessage()); // Exibe a mensagem de erro.
        } 
    } 
} 