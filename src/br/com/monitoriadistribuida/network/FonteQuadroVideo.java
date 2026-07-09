package br.com.monitoriadistribuida.network;

import java.awt.image.BufferedImage;
import java.io.IOException;

public interface FonteQuadroVideo {

    BufferedImage capturarQuadro() throws IOException;
}
