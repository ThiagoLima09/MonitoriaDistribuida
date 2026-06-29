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

        saida.println("CADASTRO;Lucas;lucas@email.com;123;ALUNO");
        System.out.println(entrada.readLine());

        saida.println("LOGIN;lucas@email.com;123");
        System.out.println(entrada.readLine());

        saida.println("LISTAR_USUARIOS");
        System.out.println(entrada.readLine());

        saida.println("SAIR");
        System.out.println(entrada.readLine());

        socket.close();
    }
}