package br.com.monitoriadistribuida.client;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class TestClient {

    public static void main(String[] args) throws Exception {
        Socket socket = new Socket("localhost", 5000);

        BufferedReader entrada = new BufferedReader(
                new InputStreamReader(socket.getInputStream()));

        PrintWriter saida = new PrintWriter(socket.getOutputStream(), true);

        System.out.println(entrada.readLine());

        saida.println("Olá, Lucas Aqui!");

        System.out.println(entrada.readLine());

        saida.println("SAIR");

        System.out.println(entrada.readLine());

        socket.close();
    }
}