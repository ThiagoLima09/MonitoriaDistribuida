package br.com.monitoriadistribuida.network;

import java.awt.AWTException;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.io.IOException;

public class FonteQuadroTela implements FonteQuadroVideo {

    private final Robot robot;
    private final Rectangle areaCaptura;
    private final int larguraMaxima;

    public FonteQuadroTela() throws AWTException {
        this(new Rectangle(Toolkit.getDefaultToolkit().getScreenSize()), 640);
    }

    public FonteQuadroTela(Rectangle areaCaptura, int larguraMaxima) throws AWTException {
        if (areaCaptura == null || areaCaptura.width <= 0 || areaCaptura.height <= 0) {
            throw new IllegalArgumentException("Area de captura invalida.");
        }

        if (larguraMaxima <= 0) {
            throw new IllegalArgumentException("Largura maxima invalida.");
        }

        this.robot = new Robot();
        this.areaCaptura = new Rectangle(areaCaptura);
        this.larguraMaxima = larguraMaxima;
    }

    @Override
    public BufferedImage capturarQuadro() throws IOException {
        BufferedImage quadro = robot.createScreenCapture(areaCaptura);
        return redimensionarSeNecessario(quadro);
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
}
