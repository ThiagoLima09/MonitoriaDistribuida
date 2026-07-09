package br.com.monitoriadistribuida.network;

import java.awt.image.BufferedImage;

public interface OuvinteQuadroVideo {

    void aoReceberQuadro(BufferedImage quadro);

    void aoOcorrerErroVideo(String mensagem);
}
