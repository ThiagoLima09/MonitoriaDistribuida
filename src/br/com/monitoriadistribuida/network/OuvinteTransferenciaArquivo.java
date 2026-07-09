package br.com.monitoriadistribuida.network;

import java.io.File;

public interface OuvinteTransferenciaArquivo {

    void aoReceberArquivo(File arquivo);

    void aoOcorrerErroTransferencia(String mensagem);
}
