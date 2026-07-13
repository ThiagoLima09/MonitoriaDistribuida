package br.com.monitoriadistribuida.client;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class TestClient {

    public static void main(String[] args) throws Exception {
        Socket socket = new Socket("localhost", 5001);

        BufferedReader entrada = new BufferedReader(
                new InputStreamReader(socket.getInputStream()));

        PrintWriter saida = new PrintWriter(socket.getOutputStream(), true);

        System.out.println(entrada.readLine());

        saida.println("CADASTRO;Joao;joao@email.com;123;MONITOR");
        System.out.println(entrada.readLine());

        saida.println("CADASTRO;Lucas;lucas@email.com;123;ALUNO");
        System.out.println(entrada.readLine());

        saida.println("LOGIN;joao@email.com;123");
        System.out.println(entrada.readLine());

        saida.println("LISTAR_DISCIPLINAS");
        System.out.println(entrada.readLine());

        saida.println("ATUALIZAR_STATUS;joao@email.com;DISPONIVEL;PROGRAMACAO;6001;6002;6003");
        System.out.println(entrada.readLine());

        saida.println("LISTAR_MONITORES;PROGRAMACAO");
        System.out.println(entrada.readLine());

        saida.println("SAIR");
        System.out.println(entrada.readLine());
        socket.close();

        socket = new Socket("localhost", 5001);
        entrada = new BufferedReader(
                new InputStreamReader(socket.getInputStream()));
        saida = new PrintWriter(socket.getOutputStream(), true);

        System.out.println(entrada.readLine());

        saida.println("LOGIN;lucas@email.com;123");
        System.out.println(entrada.readLine());

        saida.println("SOLICITAR_ATENDIMENTO;lucas@email.com;joao@email.com");
        System.out.println(entrada.readLine());

        saida.println("LISTAR_MONITORES;PROGRAMACAO");
        System.out.println(entrada.readLine());

        socket.close();
    }
}
