package br.com.monitoriadistribuida.network;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.Iterator;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;

public class ConexaoVideoP2P implements Closeable {

    private static final int TEMPO_LIMITE_CONEXAO_MS = 5000;
    private static final int TAMANHO_MAXIMO_QUADRO_BYTES = 2 * 1024 * 1024;

    private final OuvinteQuadroVideo ouvinte;
    private final Object travaSaida = new Object();

    private volatile boolean executando;
    private volatile boolean conectado;
    private volatile boolean transmitindo;

    private ServerSocket socketServidor;
    private Socket socket;
    private DataInputStream entrada;
    private DataOutputStream saida;
    private Thread threadAceite;
    private Thread threadRecepcao;
    private Thread threadEnvio;

    public ConexaoVideoP2P(String endereco, int porta, OuvinteQuadroVideo ouvinte) throws IOException {
        this(endereco, porta, ouvinte, TEMPO_LIMITE_CONEXAO_MS);
    }

    public ConexaoVideoP2P(String endereco, int porta, OuvinteQuadroVideo ouvinte, int tempoLimiteMs) throws IOException {
        validarOuvinte(ouvinte);
        validarPorta(porta);

        this.ouvinte = ouvinte;
        this.executando = true;

        Socket socketPar = new Socket();
        socketPar.connect(new InetSocketAddress(endereco, porta), tempoLimiteMs);
        inicializarSocket(socketPar);
    }

    public ConexaoVideoP2P(int porta, OuvinteQuadroVideo ouvinte) throws IOException {
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

    public void iniciarTransmissao(FonteQuadroVideo fonteQuadro, int fps, float qualidadeJpeg) {
        if (fonteQuadro == null) {
            throw new IllegalArgumentException("FonteQuadroVideo não pode ser nulo.");
        }

        if (fps < 1 || fps > 30) {
            throw new IllegalArgumentException("FPS deve ficar entre 1 e 30.");
        }

        if (qualidadeJpeg <= 0.0f || qualidadeJpeg > 1.0f) {
            throw new IllegalArgumentException("Qualidade JPEG deve ficar entre 0.0 e 1.0.");
        }

        pararTransmissao();
        transmitindo = true;

        threadEnvio = new Thread(() -> {
            long intervaloMs = 1000L / fps;

            while (true) {
                if (!executando || !transmitindo) {
                    break;
                }

                try {
                    if (estaConectado()) {
                        enviarQuadro(fonteQuadro.capturarQuadro(), qualidadeJpeg);
                    }

                    Thread.sleep(intervaloMs);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (IOException | RuntimeException e) {
                    if (executando && transmitindo) {
                        ouvinte.aoOcorrerErroVideo("Falha ao enviar quadro de vídeo: " + e.getMessage());
                    }
                }
            }
        }, "thread-envio-video");

        threadEnvio.setDaemon(true);
        threadEnvio.start();
    }

    public void pararTransmissao() {
        transmitindo = false;

        if (threadEnvio != null && threadEnvio.isAlive()) {
            threadEnvio.interrupt();
        }
    }

    public void enviarQuadro(BufferedImage quadro, float qualidadeJpeg) throws IOException {
        if (quadro == null) {
            throw new IllegalArgumentException("Quadro não pode ser nulo.");
        }

        byte[] quadroCodificado = codificarJpeg(quadro, qualidadeJpeg);

        synchronized (travaSaida) {
            if (!estaConectado() || saida == null) {
                throw new IOException("Nenhum par de vídeo conectado.");
            }

            saida.writeInt(quadroCodificado.length);
            saida.write(quadroCodificado);
            saida.flush();
        }
    }

    @Override
    public void close() {
        executando = false;
        conectado = false;
        pararTransmissao();
        fecharSilenciosamente(entrada);
        fecharSilenciosamente(saida);
        fecharSilenciosamente(socket);
        fecharSilenciosamente(socketServidor);
        interromperThread(threadRecepcao);
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
                        ouvinte.aoOcorrerErroVideo("Falha no socket de vídeo: " + e.getMessage());
                    }
                    break;
                } catch (IOException e) {
                    if (executando) {
                        ouvinte.aoOcorrerErroVideo("Falha ao aceitar conexão de vídeo: " + e.getMessage());
                    }
                    break;
                }
            }
        }, "thread-aceite-video");

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

        iniciarThreadRecepcao();
    }

    private void iniciarThreadRecepcao() {
        threadRecepcao = new Thread(() -> {
            while (true) {
                if (!executando) {
                    break;
                }

                try {
                    int tamanhoQuadro = entrada.readInt();

                    if (tamanhoQuadro <= 0 || tamanhoQuadro > TAMANHO_MAXIMO_QUADRO_BYTES) {
                        throw new IOException("Tamanho de quadro inválido: " + tamanhoQuadro);
                    }

                    byte[] dados = new byte[tamanhoQuadro];
                    entrada.readFully(dados);

                    BufferedImage quadro = ImageIO.read(new ByteArrayInputStream(dados));

                    if (quadro != null) {
                        ouvinte.aoReceberQuadro(quadro);
                    }
                } catch (EOFException | SocketException e) {
                    conectado = false;
                    break;
                } catch (IOException e) {
                    conectado = false;

                    if (executando) {
                        ouvinte.aoOcorrerErroVideo("Falha ao receber vídeo: " + e.getMessage());
                    }
                    break;
                }
            }
        }, "thread-recepcao-video");

        threadRecepcao.setDaemon(true);
        threadRecepcao.start();
    }

    private byte[] codificarJpeg(BufferedImage quadro, float qualidade) throws IOException {
        BufferedImage quadroRgb = converterParaRgb(quadro);
        Iterator<ImageWriter> escritores = ImageIO.getImageWritersByFormatName("jpg");

        if (!escritores.hasNext()) {
            throw new IOException("Nenhum encoder JPEG disponível.");
        }

        ImageWriter escritor = escritores.next();

        try (ByteArrayOutputStream bytesSaida = new ByteArrayOutputStream();
             ImageOutputStream imagemSaida = ImageIO.createImageOutputStream(bytesSaida)) {

            ImageWriteParam parametros = escritor.getDefaultWriteParam();

            if (parametros.canWriteCompressed()) {
                parametros.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                parametros.setCompressionQuality(qualidade);
            }

            escritor.setOutput(imagemSaida);
            escritor.write(null, new IIOImage(quadroRgb, null, null), parametros);
            return bytesSaida.toByteArray();
        } finally {
            escritor.dispose();
        }
    }

    private BufferedImage converterParaRgb(BufferedImage origem) {
        if (origem.getType() == BufferedImage.TYPE_INT_RGB) {
            return origem;
        }

        BufferedImage rgb = new BufferedImage(origem.getWidth(), origem.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D graficos = rgb.createGraphics();
        graficos.drawImage(origem, 0, 0, null);
        graficos.dispose();
        return rgb;
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

    private void validarOuvinte(OuvinteQuadroVideo ouvinte) {
        if (ouvinte == null) {
            throw new IllegalArgumentException("OuvinteQuadroVideo não pode ser nulo.");
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
