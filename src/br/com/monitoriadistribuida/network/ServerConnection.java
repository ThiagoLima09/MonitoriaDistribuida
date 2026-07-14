package br.com.monitoriadistribuida.network;

import br.com.monitoriadistribuida.server.model.Disciplina;
import br.com.monitoriadistribuida.server.model.StatusMonitor;
import br.com.monitoriadistribuida.server.model.TipoUsuario;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ServerConnection implements Closeable {

    public static final String HOST_PADRAO = "localhost";
    public static final int PORTA_PADRAO = 5001;
    private static final int TEMPO_LIMITE_CONEXAO_MS = 5000;

    private final Socket conexaoSocket;
    private final BufferedReader entrada;
    private final PrintWriter saida;
    private final String mensagemBoasVindas;

    public ServerConnection() throws IOException {
        this(HOST_PADRAO, PORTA_PADRAO);
    }

    public ServerConnection(String endereco, int porta) throws IOException {
        this(endereco, porta, TEMPO_LIMITE_CONEXAO_MS);
    }

    public ServerConnection(String endereco, int porta, int tempoLimiteMs) throws IOException {
        validarPorta(porta);

        conexaoSocket = new Socket();
        conexaoSocket.connect(new InetSocketAddress(endereco, porta), tempoLimiteMs);
        entrada = new BufferedReader(new InputStreamReader(conexaoSocket.getInputStream(), StandardCharsets.UTF_8));
        saida = new PrintWriter(new OutputStreamWriter(conexaoSocket.getOutputStream(), StandardCharsets.UTF_8), true);
        mensagemBoasVindas = entrada.readLine();
    }

    public String getMensagemBoasVindas() {
        return mensagemBoasVindas;
    }

    public ResultadoLogin login(String email, String senha) throws IOException {
        String resposta = enviarComando("LOGIN;" + normalizar(email) + ";" + normalizar(senha));
        String[] partes = separarResposta(resposta);

        if (partes.length < 4 || !"OK".equals(partes[0])) {
            throw new IOException("Resposta inválida para LOGIN: " + resposta);
        }

        return new ResultadoLogin(TipoUsuario.fromTexto(partes[2]), partes[3]);
    }

    public void cadastrar(String nome, String email, String senha, TipoUsuario tipoUsuario) throws IOException {
        if (tipoUsuario == null) {
            throw new IllegalArgumentException("Tipo de usuário não pode ser nulo.");
        }

        enviarComando("CADASTRO;"
                + normalizar(nome) + ";"
                + normalizar(email) + ";"
                + normalizar(senha) + ";"
                + tipoUsuario.name());
    }

    public List<InformacoesMonitor> listarMonitores(Disciplina disciplina) throws IOException {
        if (disciplina == null) {
            throw new IllegalArgumentException("Disciplina não pode ser nula.");
        }

        String resposta = enviarComando("LISTAR_MONITORES;" + disciplina.name());
        String[] partes = separarResposta(resposta);

        if (partes.length < 2 || !"OK".equals(partes[0]) || !"MONITORES".equals(partes[1])) {
            return Collections.emptyList();
        }

        List<InformacoesMonitor> monitores = new ArrayList<>();

        for (int i = 2; i < partes.length; i++) {
            monitores.add(converterItemListaMonitores(partes[i]));
        }

        return monitores;
    }

    public InformacoesMonitor solicitarAtendimento(String emailAluno, String emailMonitor) throws IOException {
        String resposta = enviarComando("SOLICITAR_ATENDIMENTO;"
                + normalizar(emailAluno) + ";"
                + normalizar(emailMonitor));
        String[] partes = separarResposta(resposta);

        if (partes.length < 9 || !"OK".equals(partes[0]) || !"ATENDIMENTO".equals(partes[1])) {
            throw new IOException("Resposta inválida para SOLICITAR_ATENDIMENTO: " + resposta);
        }

        int portaChat = converterInteiro(partes[6], "portaChat");
        int portaVideo = converterInteiro(partes[7], "portaVideo");
        int portaAudio = converterInteiro(partes[8], "portaAudio");

        return new InformacoesMonitor(partes[2], partes[3], partes[4], partes[5], portaChat, portaVideo, portaAudio);
    }

    public void atualizarStatus(String email,
                                StatusMonitor status,
                                Disciplina disciplina,
                                int portaChat,
                                int portaVideo,
                                int portaAudio) throws IOException {
        if (status == null) {
            throw new IllegalArgumentException("Status não pode ser nulo.");
        }

        if (disciplina == null) {
            throw new IllegalArgumentException("Disciplina não pode ser nula.");
        }

        validarPortaServidor(portaChat);
        validarPortaServidor(portaVideo);
        validarPortaServidor(portaAudio);

        enviarComando("ATUALIZAR_STATUS;"
                + normalizar(email) + ";"
                + status.name() + ";"
                + disciplina.name() + ";"
                + portaChat + ";"
                + portaVideo + ";"
                + portaAudio);
    }

    public void atualizarStatus(String email, StatusMonitor status, Disciplina disciplina, int portaChat)
            throws IOException {
        atualizarStatus(email, status, disciplina, portaChat, 0, 0);
    }

    public synchronized String enviarComando(String comando) throws IOException {
        if (comando == null || comando.trim().isEmpty()) {
            throw new IllegalArgumentException("Comando não pode ser vazio.");
        }

        if (conexaoSocket.isClosed()) {
            throw new IOException("Conexão com servidor central está fechada.");
        }

        saida.println(comando);

        if (saida.checkError()) {
            throw new IOException("Falha ao enviar comando ao servidor central.");
        }

        String resposta = entrada.readLine();

        if (resposta == null) {
            throw new IOException("Servidor central encerrou a conexão.");
        }

        if (resposta.startsWith("ERRO;")) {
            throw new IOException(resposta.substring("ERRO;".length()));
        }

        return resposta;
    }

    @Override
    public void close() {
        try {
            if (!conexaoSocket.isClosed()) {
                saida.println("SAIR");
            }
        } catch (RuntimeException ignored) {
            // A conexão pode já estar fechada; nesse caso não há servidor para notificar.
        }

        saida.close();

        try {
            entrada.close();
        } catch (IOException ignored) {
            // O socket pode ter caído antes do fechamento manual da entrada.
        }

        try {
            conexaoSocket.close();
        } catch (IOException ignored) {
            // O socket pode já ter sido fechado pelo sistema ou pelo servidor.
        }
    }

    private InformacoesMonitor converterItemListaMonitores(String item) throws IOException {
        String[] campos = item.split(",", -1);

        if (campos.length < 5) {
            throw new IOException("Item de monitor inválido: " + item);
        }

        int portaChat = converterInteiro(campos[4], "portaChat");
        int portaVideo = campos.length > 5 ? converterInteiro(campos[5], "portaVideo") : 0;
        int portaAudio = campos.length > 6 ? converterInteiro(campos[6], "portaAudio") : 0;

        return new InformacoesMonitor(campos[0], campos[1], campos[2], campos[3], portaChat, portaVideo, portaAudio);
    }

    private String[] separarResposta(String resposta) {
        return resposta.split(";", -1);
    }

    private int converterInteiro(String valor, String nomeCampo) throws IOException {
        try {
            return Integer.parseInt(valor);
        } catch (NumberFormatException e) {
            throw new IOException("Campo numérico inválido " + nomeCampo + ": " + valor);
        }
    }

    private String normalizar(String valor) {
        return valor == null ? "" : valor.trim();
    }

    private void validarPorta(int porta) {
        if (porta < 1 || porta > 65535) {
            throw new IllegalArgumentException("Porta inválida: " + porta);
        }
    }

    private void validarPortaServidor(int porta) {
        if (porta < 0 || porta > 65535) {
            throw new IllegalArgumentException("Porta inválida: " + porta);
        }
    }

}
