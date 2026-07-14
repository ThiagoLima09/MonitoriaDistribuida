package br.com.monitoriadistribuida.network;

import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Java2DFrameConverter;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.Closeable;
import java.io.IOException;

public class FonteQuadroWebcam implements FonteQuadroVideo, Closeable {

    private static final int LARGURA_MAXIMA_PADRAO = 640;
    private static final int LARGURA_CAPTURA_PADRAO = 640;
    private static final int ALTURA_PADRAO = 480;
    private static final String FPS_PRINCIPAL_MAC = "30";
    private static final String FPS_RESERVA_MAC = "15";

    static {
        configurarPlataformaMacArm();
    }

    private final FFmpegFrameGrabber capturador;
    private final Java2DFrameConverter conversor = new Java2DFrameConverter();
    private final int larguraMaxima;

    public FonteQuadroWebcam() throws IOException {
        this(LARGURA_MAXIMA_PADRAO);
    }

    public FonteQuadroWebcam(int larguraMaxima) throws IOException {
        this(0, larguraMaxima);
    }

    public FonteQuadroWebcam(int indiceCamera, int larguraMaxima) throws IOException {
        if (larguraMaxima <= 0) {
            throw new IllegalArgumentException("Largura máxima inválida.");
        }

        if (indiceCamera < 0) {
            throw new IllegalArgumentException("Índice da câmera inválido.");
        }

        this.larguraMaxima = larguraMaxima;
        this.capturador = abrirCapturador(indiceCamera);
    }

    @Override
    public BufferedImage capturarQuadro() throws IOException {
        Frame quadroCapturado;

        try {
            quadroCapturado = capturador.grab();
        } catch (Exception e) {
            throw new IOException("Falha ao capturar imagem da webcam.", e);
        }

        if (quadroCapturado == null) {
            throw new IOException("A webcam não retornou imagem.");
        }

        BufferedImage quadro = conversor.convert(quadroCapturado);

        if (quadro == null) {
            throw new IOException("Não foi possível converter a imagem da webcam.");
        }

        return redimensionarSeNecessario(quadro);
    }

    @Override
    public void close() {
        try {
            capturador.stop();
            capturador.release();
        } catch (Exception ignored) {
            // A webcam pode já ter sido liberada pelo driver ao encerrar a chamada.
        }
    }

    private BufferedImage redimensionarSeNecessario(BufferedImage quadro) {
        if (quadro.getWidth() <= larguraMaxima) {
            return quadro;
        }

        int largura = larguraMaxima;
        int altura = Math.max(1, (int) (quadro.getHeight() * (largura / (double) quadro.getWidth())));

        BufferedImage redimensionada = new BufferedImage(largura, altura, BufferedImage.TYPE_INT_RGB);
        Graphics2D graficos = redimensionada.createGraphics();
        graficos.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        graficos.drawImage(quadro, 0, 0, largura, altura, null);
        graficos.dispose();

        return redimensionada;
    }

    private FFmpegFrameGrabber abrirCapturador(int indiceCamera) throws IOException {
        String sistema = System.getProperty("os.name", "").toLowerCase();

        if (sistema.contains("mac")) {
            return abrirCapturadorMac(indiceCamera);
        }

        FFmpegFrameGrabber novoCapturador = criarCapturador(indiceCamera, LARGURA_CAPTURA_PADRAO, ALTURA_PADRAO, null);

        try {
            novoCapturador.start();
            return novoCapturador;
        } catch (Exception e) {
            fecharCapturador(novoCapturador);
            throw new IOException("Não foi possível abrir a webcam.", e);
        }
    }

    private FFmpegFrameGrabber abrirCapturadorMac(int indiceCamera) throws IOException {
        String[] taxasQuadros = {FPS_PRINCIPAL_MAC, FPS_RESERVA_MAC};
        Exception ultimaFalha = null;

        for (String taxaQuadros : taxasQuadros) {
            FFmpegFrameGrabber novoCapturador = criarCapturador(
                    indiceCamera,
                    LARGURA_CAPTURA_PADRAO,
                    ALTURA_PADRAO,
                    taxaQuadros);

            try {
                novoCapturador.start();
                return novoCapturador;
            } catch (Exception e) {
                ultimaFalha = e;
                fecharCapturador(novoCapturador);
            }
        }

        throw new IOException("Não foi possível abrir a webcam em 640x480 com 30 FPS ou 15 FPS.", ultimaFalha);
    }

    private FFmpegFrameGrabber criarCapturador(int indiceCamera, int largura, int altura, String taxaQuadros) {
        String sistema = System.getProperty("os.name", "").toLowerCase();
        FFmpegFrameGrabber novoCapturador;

        if (sistema.contains("mac")) {
            novoCapturador = new FFmpegFrameGrabber(String.valueOf(indiceCamera));
            novoCapturador.setFormat("avfoundation");
        } else if (sistema.contains("linux")) {
            novoCapturador = new FFmpegFrameGrabber("/dev/video" + indiceCamera);
            novoCapturador.setFormat("video4linux2");
        } else {
            novoCapturador = new FFmpegFrameGrabber(String.valueOf(indiceCamera));
        }

        novoCapturador.setImageWidth(largura);
        novoCapturador.setImageHeight(altura);
        novoCapturador.setOption("video_size", largura + "x" + altura);

        if (taxaQuadros != null) {
            novoCapturador.setFrameRate(Double.parseDouble(taxaQuadros));
            novoCapturador.setOption("framerate", taxaQuadros);
        }

        return novoCapturador;
    }

    private void fecharCapturador(FFmpegFrameGrabber capturadorParaFechar) {
        if (capturadorParaFechar == null) {
            return;
        }

        try {
            capturadorParaFechar.close();
        } catch (Exception ignored) {
            // Se uma câmera de fallback falhar ao abrir, tentamos liberar o capturador parcial.
        }
    }

    private static void configurarPlataformaMacArm() {
        String plataformaConfigurada = System.getProperty("org.bytedeco.javacpp.platform");

        if (plataformaConfigurada != null && !plataformaConfigurada.trim().isEmpty()) {
            return;
        }

        String sistema = System.getProperty("os.name", "").toLowerCase();
        String arquitetura = System.getProperty("os.arch", "").toLowerCase();

        if (sistema.contains("mac") && (arquitetura.contains("aarch64") || arquitetura.contains("arm64"))) {
            System.setProperty("org.bytedeco.javacpp.platform", "macosx-arm64");
        }
    }
}
