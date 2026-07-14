package br.com.monitoriadistribuida.network;

import java.io.Closeable;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.TargetDataLine;

public class ConexaoAudioP2P implements Closeable {

    private static final int TEMPO_LIMITE_CONEXAO_MS = 5000;
    private static final int TAMANHO_PACOTE_AUDIO_BYTES = 1024;
    private static final int TAMANHO_MAXIMO_PACOTE_AUDIO_BYTES = 8192;

    private final OuvinteChamadaAudio ouvinte;
    private final Object travaSaida = new Object();
    private final AudioFormat formatoAudio = new AudioFormat(16000.0f, 16, 1, true, false);

    private volatile boolean executando;
    private volatile boolean conectado;
    private volatile boolean escutaSolicitada;
    private volatile boolean escutaExecutando;
    private volatile boolean microfoneSolicitado;
    private volatile boolean microfoneAtivo;

    private ServerSocket socketServidor;
    private Socket socket;
    private DataInputStream entrada;
    private DataOutputStream saida;
    private TargetDataLine linhaMicrofone;
    private SourceDataLine linhaAltoFalante;
    private Thread threadAceite;
    private Thread threadCaptura;
    private Thread threadReproducao;

    public ConexaoAudioP2P(String endereco, int porta, OuvinteChamadaAudio ouvinte) throws IOException {
        this(endereco, porta, ouvinte, TEMPO_LIMITE_CONEXAO_MS);
    }

    public ConexaoAudioP2P(String endereco, int porta, OuvinteChamadaAudio ouvinte, int tempoLimiteMs) throws IOException {
        validarOuvinte(ouvinte);
        validarPorta(porta);

        this.ouvinte = ouvinte;
        this.executando = true;

        Socket socketPar = new Socket();
        socketPar.connect(new InetSocketAddress(endereco, porta), tempoLimiteMs);
        inicializarSocket(socketPar);
    }

    public ConexaoAudioP2P(int porta, OuvinteChamadaAudio ouvinte) throws IOException {
        validarOuvinte(ouvinte);
        validarPortaServidor(porta);

        this.ouvinte = ouvinte;
        this.executando = true;
        this.socketServidor = new ServerSocket(porta);
        iniciarThreadAceite();
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

    public synchronized void iniciarAudio() {
        iniciarEscuta();
        ativarMicrofone();
    }

    public synchronized void iniciarEscuta() {
        escutaSolicitada = true;

        if (!estaConectado() || escutaExecutando) {
            return;
        }

        escutaExecutando = true;
        iniciarThreadReproducao();
    }

    public synchronized void ativarMicrofone() {
        microfoneSolicitado = true;

        if (!estaConectado() || microfoneAtivo) {
            return;
        }

        microfoneAtivo = true;
        iniciarThreadCaptura();
    }

    public synchronized void silenciarMicrofone() {
        microfoneSolicitado = false;
        microfoneAtivo = false;
        fecharMicrofone();
        interromperThread(threadCaptura);
    }

    public synchronized void pararAudio() {
        escutaSolicitada = false;
        escutaExecutando = false;
        microfoneSolicitado = false;
        microfoneAtivo = false;
        fecharMicrofone();
        fecharAltoFalante();
        interromperThread(threadCaptura);
        interromperThread(threadReproducao);
    }

    @Override
    public void close() {
        executando = false;
        conectado = false;
        pararAudio();
        fecharSilenciosamente(entrada);
        fecharSilenciosamente(saida);
        fecharSilenciosamente(socket);
        fecharSilenciosamente(socketServidor);
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
                        ouvinte.aoOcorrerErroAudio("Falha no socket de áudio: " + e.getMessage());
                    }
                    break;
                } catch (IOException e) {
                    if (executando) {
                        ouvinte.aoOcorrerErroAudio("Falha ao aceitar conexão de áudio: " + e.getMessage());
                    }
                    break;
                }
            }
        }, "thread-aceite-audio");

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
        entrada = new DataInputStream(socket.getInputStream());
        saida = new DataOutputStream(socket.getOutputStream());
        conectado = true;

        if (escutaSolicitada) {
            iniciarEscuta();
        }

        if (microfoneSolicitado) {
            ativarMicrofone();
        }
    }

    private void iniciarThreadCaptura() {
        threadCaptura = new Thread(() -> {
            TargetDataLine linhaCaptura = null;

            try {
                linhaCaptura = abrirLinhaMicrofone();
                linhaMicrofone = linhaCaptura;

                byte[] buffer = new byte[TAMANHO_PACOTE_AUDIO_BYTES];

                while (true) {
                    if (!executando || !microfoneAtivo) {
                        break;
                    }

                    int lidos = linhaCaptura.read(buffer, 0, buffer.length);

                    if (lidos > 0) {
                        enviarPacoteAudio(buffer, lidos);
                    }
                }
            } catch (LineUnavailableException e) {
                ouvinte.aoOcorrerErroAudio("Microfone indisponível: " + e.getMessage());
            } catch (IOException | RuntimeException e) {
                if (executando && microfoneAtivo) {
                    ouvinte.aoOcorrerErroAudio("Falha ao capturar áudio: " + e.getMessage());
                }
            } finally {
                fecharMicrofone(linhaCaptura);
            }
        }, "thread-captura-audio");

        threadCaptura.setDaemon(true);
        threadCaptura.start();
    }

    private void iniciarThreadReproducao() {
        threadReproducao = new Thread(() -> {
            SourceDataLine linhaReproducao = null;

            try {
                linhaReproducao = abrirLinhaAltoFalante();
                linhaAltoFalante = linhaReproducao;

                while (true) {
                    if (!executando || !escutaExecutando) {
                        break;
                    }

                    int tamanhoPacote = entrada.readInt();

                    if (tamanhoPacote <= 0 || tamanhoPacote > TAMANHO_MAXIMO_PACOTE_AUDIO_BYTES) {
                        throw new IOException("Pacote de áudio inválido: " + tamanhoPacote);
                    }

                    byte[] buffer = new byte[tamanhoPacote];
                    entrada.readFully(buffer);
                    linhaReproducao.write(buffer, 0, buffer.length);
                }
            } catch (EOFException | SocketException e) {
                conectado = false;
            } catch (LineUnavailableException e) {
                ouvinte.aoOcorrerErroAudio("Saída de áudio indisponível: " + e.getMessage());
            } catch (IOException | RuntimeException e) {
                if (executando && escutaExecutando) {
                    ouvinte.aoOcorrerErroAudio("Falha ao reproduzir áudio: " + e.getMessage());
                }
            } finally {
                fecharAltoFalante(linhaReproducao);
            }
        }, "thread-reproducao-audio");

        threadReproducao.setDaemon(true);
        threadReproducao.start();
    }

    private void enviarPacoteAudio(byte[] buffer, int tamanho) throws IOException {
        synchronized (travaSaida) {
            if (!estaConectado() || saida == null) {
                throw new IOException("Nenhum par de áudio conectado.");
            }

            saida.writeInt(tamanho);
            saida.write(buffer, 0, tamanho);
            saida.flush();
        }
    }

    private TargetDataLine abrirLinhaMicrofone() throws LineUnavailableException {
        DataLine.Info informacao = new DataLine.Info(TargetDataLine.class, formatoAudio);
        TargetDataLine linha = (TargetDataLine) AudioSystem.getLine(informacao);
        linha.open(formatoAudio);
        linha.start();
        return linha;
    }

    private SourceDataLine abrirLinhaAltoFalante() throws LineUnavailableException {
        DataLine.Info informacao = new DataLine.Info(SourceDataLine.class, formatoAudio);
        SourceDataLine linha = (SourceDataLine) AudioSystem.getLine(informacao);
        linha.open(formatoAudio);
        linha.start();
        return linha;
    }

    private synchronized void fecharSocketParExistente() {
        conectado = false;
        fecharSilenciosamente(entrada);
        fecharSilenciosamente(saida);
        fecharSilenciosamente(socket);
        entrada = null;
        saida = null;
        socket = null;
    }

    private void fecharMicrofone() {
        TargetDataLine linha = linhaMicrofone;
        linhaMicrofone = null;
        fecharMicrofone(linha);
    }

    private void fecharMicrofone(TargetDataLine linha) {
        if (linha == null) {
            return;
        }

        if (linhaMicrofone == linha) {
            linhaMicrofone = null;
        }

        try {
            linha.stop();
        } catch (RuntimeException ignored) {
            // A linha do microfone pode já ter parado por falta de permissão ou encerramento da chamada.
        }

        linha.close();
    }

    private synchronized void fecharAltoFalante() {
        SourceDataLine linha = linhaAltoFalante;
        linhaAltoFalante = null;
        fecharAltoFalante(linha);
    }

    private synchronized void fecharAltoFalante(SourceDataLine linha) {
        if (linha == null) {
            return;
        }

        if (linhaAltoFalante == linha) {
            linhaAltoFalante = null;
        }

        try {
            linha.drain();
        } catch (RuntimeException ignored) {
            // Ao encerrar a chamada, a linha de saída pode já estar inválida.
        }

        try {
            linha.stop();
        } catch (RuntimeException ignored) {
            // Outra thread pode já ter parado o alto-falante durante o encerramento.
        }

        try {
            linha.close();
        } catch (RuntimeException ignored) {
            // O driver de áudio pode já ter liberado essa linha.
        }
    }

    private void fecharSilenciosamente(Closeable fechavel) {
        if (fechavel == null) {
            return;
        }

        try {
            fechavel.close();
        } catch (IOException ignored) {
            // Streams podem já estar fechados quando a chamada é encerrada.
        }
    }

    private void fecharSilenciosamente(Socket socketFechavel) {
        if (socketFechavel == null) {
            return;
        }

        try {
            socketFechavel.close();
        } catch (IOException ignored) {
            // O socket pode já ter sido fechado pela outra ponta da chamada.
        }
    }

    private void fecharSilenciosamente(ServerSocket socketServidorFechavel) {
        if (socketServidorFechavel == null) {
            return;
        }

        try {
            socketServidorFechavel.close();
        } catch (IOException ignored) {
            // O servidor local de áudio pode já ter parado junto com a janela de chamada.
        }
    }

    private void interromperThread(Thread thread) {
        if (thread != null && thread.isAlive()) {
            thread.interrupt();
        }
    }

    private void validarOuvinte(OuvinteChamadaAudio ouvinte) {
        if (ouvinte == null) {
            throw new IllegalArgumentException("OuvinteChamadaAudio não pode ser nulo.");
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
