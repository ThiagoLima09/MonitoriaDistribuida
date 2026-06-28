package br.com.monitoriadistribuida.server;

import java.io.BufferedReader; 
import java.io.IOException; 
import java.io.InputStreamReader; 
import java.io.PrintWriter; 
import java.net.Socket; 


public class ClientHandler implements Runnable{
    
    private Socket clientSocket;

    public ClientHandler(Socket clientSocket) {
        this.clientSocket = clientSocket;
    }

    @Override
    public void run() {
        try (
            BufferedReader entrada = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

            PrintWriter saida = new PrintWriter( 
                clientSocket.getOutputStream(),
                true 
            )

        ) { 

            saida.println("Conectado ao servidor central!");

            String mensagemRecebida;

            while ((mensagemRecebida = entrada.readLine()) != null) {

                System.out.println("Cliente enviou: " + mensagemRecebida);

                if (mensagemRecebida.equalsIgnoreCase("SAIR")) { 
                    saida.println("Conexão encerrada pelo servidor."); 
                    break;
                } 

                saida.println("Servidor recebeu: " + mensagemRecebida);

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
}
