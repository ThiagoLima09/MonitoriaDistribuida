package br.com.monitoriadistribuida.network;

import java.awt.AWTException;
import java.awt.Rectangle;
import java.io.Closeable;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import javax.imageio.ImageIO;

public class ServicoGravacaoTela implements Closeable {

    private final FonteQuadroTela fonteQuadro;
    private volatile boolean gravando;
    private Thread threadGravacao;

    public ServicoGravacaoTela() throws AWTException {
        this.fonteQuadro = new FonteQuadroTela();
    }

    public ServicoGravacaoTela(Rectangle areaCaptura, int larguraMaxima) throws AWTException {
        this.fonteQuadro = new FonteQuadroTela(areaCaptura, larguraMaxima);
    }

    public void iniciarGravacao(File diretorioSaida, int fps) throws IOException {
        if (gravando) {
            throw new IllegalStateException("Gravacao de tela ja esta em andamento.");
        }

        validarDiretorioSaida(diretorioSaida);

        if (fps < 1 || fps > 30) {
            throw new IllegalArgumentException("FPS deve ficar entre 1 e 30.");
        }

        escreverMetadados(diretorioSaida, fps);
        gravando = true;

        threadGravacao = new Thread(() -> loopGravacao(diretorioSaida, fps), "thread-gravacao-tela");
        threadGravacao.setDaemon(true);
        threadGravacao.start();
    }

    public void pararGravacao() {
        gravando = false;

        if (threadGravacao != null && threadGravacao.isAlive()) {
            threadGravacao.interrupt();
        }
    }

    @Override
    public void close() {
        pararGravacao();
    }

    private void loopGravacao(File diretorioSaida, int fps) {
        long intervaloMs = 1000L / fps;
        int indiceQuadro = 0;

        while (true) {
            if (!gravando) {
                break;
            }

            try {
                File arquivoQuadro = new File(diretorioSaida, String.format("quadro-%06d.jpg", indiceQuadro));
                ImageIO.write(fonteQuadro.capturarQuadro(), "jpg", arquivoQuadro);
                indiceQuadro++;
                Thread.sleep(intervaloMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (IOException e) {
                gravando = false;
                break;
            }
        }
    }

    private void validarDiretorioSaida(File diretorioSaida) throws IOException {
        if (diretorioSaida == null) {
            throw new IllegalArgumentException("Diretório de saída não pode ser nulo.");
        }

        if (!diretorioSaida.exists() && !diretorioSaida.mkdirs()) {
            throw new IOException("Não foi possível criar o diretório de gravação.");
        }

        if (!diretorioSaida.isDirectory() || !diretorioSaida.canWrite()) {
            throw new IOException("Diretório de gravação inválido ou sem permissão de escrita.");
        }
    }

    private void escreverMetadados(File diretorioSaida, int fps) throws IOException {
        File arquivoMetadados = new File(diretorioSaida, "gravacao-info.txt");

        try (PrintWriter escritor = new PrintWriter(new FileWriter(arquivoMetadados))) {
            escritor.println("formato=sequencia-jpeg");
            escritor.println("fps=" + fps);
            escritor.println("padrao=quadro-%06d.jpg");
        }
    }
}
