package br.com.monitoriadistribuida.network;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.Closeable;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.nio.file.Paths;

public class ServicoTransferenciaArquivo implements Closeable {

    private static final int TAMANHO_BUFFER = 8192;
    private static final int TEMPO_LIMITE_CONEXAO_MS = 5000;

    private final OuvinteTransferenciaArquivo ouvinte;
    private volatile boolean executando;
    private ServerSocket socketServidor;
    private Thread threadRecepcao;

    public ServicoTransferenciaArquivo(OuvinteTransferenciaArquivo ouvinte) {
        if (ouvinte == null) {
            throw new IllegalArgumentException("OuvinteTransferenciaArquivo não pode ser nulo.");
        }

        this.ouvinte = ouvinte;
    }

    public int iniciarReceptor(int porta, File diretorioDestino) throws IOException {
        validarPortaServidor(porta);
        validarDiretorioDestino(diretorioDestino);

        close();
        executando = true;
        socketServidor = new ServerSocket(porta);
        threadRecepcao = new Thread(() -> loopRecepcao(diretorioDestino), "thread-recepcao-arquivo");
        threadRecepcao.setDaemon(true);
        threadRecepcao.start();

        return socketServidor.getLocalPort();
    }

    public void enviarArquivo(String endereco, int porta, File arquivo) throws IOException {
        validarPorta(porta);
        validarArquivoLeitura(arquivo);

        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(endereco, porta), TEMPO_LIMITE_CONEXAO_MS);

            try (DataOutputStream saida = new DataOutputStream(
                    new BufferedOutputStream(socket.getOutputStream()));
                 FileInputStream entradaArquivo = new FileInputStream(arquivo)) {

                saida.writeUTF(arquivo.getName());
                saida.writeLong(arquivo.length());

                byte[] buffer = new byte[TAMANHO_BUFFER];
                int lidos;

                while ((lidos = entradaArquivo.read(buffer)) != -1) {
                    saida.write(buffer, 0, lidos);
                }

                saida.flush();
            }
        }
    }

    @Override
    public void close() {
        executando = false;
        fecharSilenciosamente(socketServidor);

        if (threadRecepcao != null && threadRecepcao.isAlive()) {
            threadRecepcao.interrupt();
        }
    }

    private void loopRecepcao(File diretorioDestino) {
        while (true) {
            if (!executando) {
                break;
            }

            try (Socket socket = socketServidor.accept()) {
                File arquivoRecebido = receberArquivo(socket, diretorioDestino);
                ouvinte.aoReceberArquivo(arquivoRecebido);
            } catch (SocketException e) {
                if (executando) {
                    ouvinte.aoOcorrerErroTransferencia("Falha no socket de arquivos: " + e.getMessage());
                }
                break;
            } catch (IOException e) {
                if (executando) {
                    ouvinte.aoOcorrerErroTransferencia("Falha ao receber arquivo: " + e.getMessage());
                }
            }
        }
    }

    private File receberArquivo(Socket socket, File diretorioDestino) throws IOException {
        try (DataInputStream entrada = new DataInputStream(
                new BufferedInputStream(socket.getInputStream()))) {

            String nomeOriginal = entrada.readUTF();
            long tamanho = entrada.readLong();

            if (tamanho < 0) {
                throw new IOException("Tamanho de arquivo inválido.");
            }

            String nomeSeguro = sanitizarNomeArquivo(nomeOriginal);
            File arquivoSaida = resolverArquivoDisponivel(diretorioDestino, nomeSeguro);

            try (FileOutputStream saida = new FileOutputStream(arquivoSaida)) {
                byte[] buffer = new byte[TAMANHO_BUFFER];
                long restante = tamanho;

                while (restante > 0) {
                    int maximoLeitura = (int) Math.min(buffer.length, restante);
                    int lidos = entrada.read(buffer, 0, maximoLeitura);

                    if (lidos == -1) {
                        throw new IOException("Conexao encerrada antes do fim do arquivo.");
                    }

                    saida.write(buffer, 0, lidos);
                    restante -= lidos;
                }
            }

            return arquivoSaida;
        }
    }

    private File resolverArquivoDisponivel(File diretorioDestino, String nomeArquivo) {
        File candidato = new File(diretorioDestino, nomeArquivo);

        if (!candidato.exists()) {
            return candidato;
        }

        int indicePonto = nomeArquivo.lastIndexOf('.');
        String nomeBase = indicePonto > 0 ? nomeArquivo.substring(0, indicePonto) : nomeArquivo;
        String extensao = indicePonto > 0 ? nomeArquivo.substring(indicePonto) : "";
        int contador = 1;

        while (candidato.exists()) {
            candidato = new File(diretorioDestino, nomeBase + "-" + contador + extensao);
            contador++;
        }

        return candidato;
    }

    private String sanitizarNomeArquivo(String nomeArquivo) throws IOException {
        if (nomeArquivo == null || nomeArquivo.trim().isEmpty()) {
            throw new IOException("Nome de arquivo inválido.");
        }

        java.nio.file.Path caminho = Paths.get(nomeArquivo).getFileName();

        if (caminho == null || caminho.toString().trim().isEmpty()) {
            throw new IOException("Nome de arquivo inválido.");
        }

        return caminho.toString();
    }

    private void validarArquivoLeitura(File arquivo) {
        if (arquivo == null || !arquivo.isFile() || !arquivo.canRead()) {
            throw new IllegalArgumentException("Arquivo inválido ou sem permissão de leitura.");
        }
    }

    private void validarDiretorioDestino(File diretorioDestino) {
        if (diretorioDestino == null || !diretorioDestino.isDirectory() || !diretorioDestino.canWrite()) {
            throw new IllegalArgumentException("Diretório de destino inválido ou sem permissão de escrita.");
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

    private void fecharSilenciosamente(ServerSocket socketServidorFechavel) {
        if (socketServidorFechavel == null) {
            return;
        }

        try {
            socketServidorFechavel.close();
        } catch (IOException ignored) {
            // O receptor de arquivos pode já ter sido encerrado junto com o chat.
        }
    }
}
