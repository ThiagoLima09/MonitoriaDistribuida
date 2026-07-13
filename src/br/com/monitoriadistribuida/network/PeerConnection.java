package br.com.monitoriadistribuida.network;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;

public class PeerConnection implements Closeable {

    private static final int TEMPO_LIMITE_CONEXAO_MS = 5000;

    private final MessageListener ouvinte;
    private final Object travaEnvio = new Object();

    private volatile boolean executando;
    private volatile boolean conectado;

    private ServerSocket socketServidor;
    private Socket socket;
    private BufferedReader entrada;
    private PrintWriter saida;
    private Thread threadAceite;
    private Thread threadLeitura;

    public PeerConnection(String endereco, int porta, MessageListener ouvinte) throws IOException {
        this(endereco, porta, ouvinte, TEMPO_LIMITE_CONEXAO_MS);
    }

    public PeerConnection(String endereco, int porta, MessageListener ouvinte, int tempoLimiteMs) throws IOException {
        validarOuvinte(ouvinte);
        validarPorta(porta);

        this.ouvinte = ouvinte;
        this.executando = true;

        Socket socketPar = new Socket();
        socketPar.connect(new InetSocketAddress(endereco, porta), tempoLimiteMs);
        inicializarSocket(socketPar);
    }

    public PeerConnection(int porta, MessageListener ouvinte) throws IOException {
        validarOuvinte(ouvinte);
        validarPortaServidor(porta);

        this.ouvinte = ouvinte;
        this.executando = true;
        this.socketServidor = new ServerSocket(porta);
        iniciarThreadAceite();
    }

    public static PeerConnection conectarAoPar(String endereco, int porta, MessageListener ouvinte) throws IOException {
        return new PeerConnection(endereco, porta, ouvinte);
    }

    public static PeerConnection aguardarPar(int porta, MessageListener ouvinte) throws IOException {
        return new PeerConnection(porta, ouvinte);
    }

    public int getPortaLocal() {
        if (socketServidor != null && !socketServidor.isClosed()) {
            return socketServidor.getLocalPort();
        }
        if (socket != null && !socket.isClosed()) {
            return socket.getLocalPort();
        }
        return -1;
    }

    public boolean estaConectado() {
        return conectado && socket != null && socket.isConnected() && !socket.isClosed();
    }

    public String getEnderecoPar() {
        if (socket == null || socket.getInetAddress() == null) {
            return null;
        }

        return socket.getInetAddress().getHostAddress();
    }

    public void sendMessage(String mensagem) throws IOException {
        enviarMensagem(mensagem);
    }

    public void enviarMensagem(String mensagem) throws IOException {
        if (mensagem == null) {
            throw new IllegalArgumentException("Mensagem não pode ser nula.");
        }

        synchronized (travaEnvio) {
            if (!estaConectado() || saida == null) {
                throw new IOException("Nenhum par conectado para envio.");
            }

            saida.println(mensagem);

            if (saida.checkError()) {
                throw new IOException("Falha ao enviar mensagem para o par.");
            }
        }
    }

    @Override
    public void close() {
        executando = false;
        conectado = false;

        fecharSilenciosamente(entrada);

        if (saida != null) {
            saida.close();
        }

        fecharSilenciosamente(socket);
        fecharSilenciosamente(socketServidor);

        interromperThread(threadLeitura);
        interromperThread(threadAceite);
    }

    private void iniciarThreadAceite() {
        threadAceite = new Thread(() -> {
            while (true) {
                if (!executando) {
                    break;
                }

                try {
                    Socket socketAceito = socketServidor.accept();
                    inicializarSocket(socketAceito);
                    break;
                } catch (SocketException e) {
                    if (executando) {
                        notificarOuvinte("ERRO;Falha no socket de escuta: " + e.getMessage());
                    }
                    break;
                } catch (IOException e) {
                    if (executando) {
                        notificarOuvinte("ERRO;Falha ao aceitar conexão P2P: " + e.getMessage());
                    }
                    break;
                }
            }
        }, "thread-aceite-par");

        threadAceite.setDaemon(true);
        threadAceite.start();
    }

    private synchronized void inicializarSocket(Socket socketPar) throws IOException {
        if (!executando) {
            fecharSilenciosamente(socketPar);
            return;
        }

        fecharSocketParExistente();

        socket = socketPar;
        entrada = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
        saida = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);
        conectado = true;

        iniciarThreadLeitura();
        notificarOuvinte("CONTROLE;CONEXAO_ESTABELECIDA");
    }

    private void iniciarThreadLeitura() {
        threadLeitura = new Thread(() -> {
            while (true) {
                if (!executando) {
                    break;
                }

                try {
                    String mensagem = entrada.readLine();

                    if (mensagem == null) {
                        conectado = false;
                        if (executando) {
                            notificarOuvinte("CONTROLE;CONEXAO_ENCERRADA");
                        }
                        break;
                    }

                    notificarOuvinte(mensagem);
                } catch (SocketException e) {
                    if (executando) {
                        notificarOuvinte("ERRO;Conexão P2P interrompida: " + e.getMessage());
                    }
                    conectado = false;
                    break;
                } catch (IOException e) {
                    if (executando) {
                        notificarOuvinte("ERRO;Falha ao ler mensagem P2P: " + e.getMessage());
                    }
                    conectado = false;
                    break;
                }
            }
        }, "thread-leitura-par");

        threadLeitura.setDaemon(true);
        threadLeitura.start();
    }

    private void notificarOuvinte(String mensagem) {
        if (ouvinte != null) {
            ouvinte.onMessageReceived(mensagem);
        }
    }

    private synchronized void fecharSocketParExistente() {
        conectado = false;
        fecharSilenciosamente(entrada);

        if (saida != null) {
            saida.close();
            saida = null;
        }

        fecharSilenciosamente(socket);
        entrada = null;
        socket = null;
    }

    private void fecharSilenciosamente(Closeable fechavel) {
        if (fechavel == null) {
            return;
        }

        try {
            fechavel.close();
        } catch (IOException ignored) {
            // Fechamento em shutdown: melhor esforço.
        }
    }

    private void fecharSilenciosamente(Socket socketFechavel) {
        if (socketFechavel == null) {
            return;
        }

        try {
            socketFechavel.close();
        } catch (IOException ignored) {
            // Fechamento em shutdown: melhor esforço.
        }
    }

    private void fecharSilenciosamente(ServerSocket socketServidorFechavel) {
        if (socketServidorFechavel == null) {
            return;
        }

        try {
            socketServidorFechavel.close();
        } catch (IOException ignored) {
            // Fechamento em shutdown: melhor esforço.
        }
    }

    private void interromperThread(Thread thread) {
        if (thread != null && thread.isAlive()) {
            thread.interrupt();
        }
    }

    private void validarOuvinte(MessageListener ouvinte) {
        if (ouvinte == null) {
            throw new IllegalArgumentException("MessageListener não pode ser nulo.");
        }
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
