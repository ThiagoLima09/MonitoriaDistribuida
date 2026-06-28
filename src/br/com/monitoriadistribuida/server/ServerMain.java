package br.com.monitoriadistribuida.server;

import java.io.IOException; 
import java.net.ServerSocket; 
import java.net.Socket; 

public class ServerMain {
    private static final int PORTA = 5000;

    public static void main(String[] args) {
        System.err.println("Iniciando servidor central...");

        try (ServerSocket serverSocket = new ServerSocket(PORTA)){

            System.out.println("Servidor iniciando na porta " + PORTA);

            while (true) {
                Socket clientSocket = serverSocket.accept();

                System.out.println("Novo Cliente conectado: " + clientSocket.getInetAddress());

                ClientHandler clientHandler = new ClientHandler(clientSocket);

                Thread threadCliente = new Thread(clientHandler);

                threadCliente.start();
            }
            
        } catch (IOException e) {
            System.out.println("Erro no servidor: " + e.getMessage());
        }
    }
}
