package br.com.monitoriadistribuida.client.controller;

import br.com.monitoriadistribuida.network.ServerConnection;
import br.com.monitoriadistribuida.server.model.Disciplina;
import br.com.monitoriadistribuida.server.model.StatusMonitor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MonitorController {

    public List<Disciplina> listarDisciplinas() {
        List<Disciplina> disciplinas = new ArrayList<>();
        for (Disciplina disciplina : Disciplina.values()) {
            disciplinas.add(disciplina);
        }
        return disciplinas;
    }

    public String atualizarStatus(ServerConnection conexaoServidor,
                                  String email,
                                  StatusMonitor status,
                                  Disciplina disciplina,
                                  int porta) throws IOException {
        return atualizarStatus(conexaoServidor, email, status, disciplina, porta, 0, 0);
    }

    public String atualizarStatus(ServerConnection conexaoServidor,
                                  String email,
                                  StatusMonitor status,
                                  Disciplina disciplina,
                                  int portaChat,
                                  int portaVideo,
                                  int portaAudio) throws IOException {
        if (conexaoServidor == null) {
            throw new IllegalStateException("Sessão sem conexão com o servidor central.");
        }

        conexaoServidor.atualizarStatus(email, status, disciplina, portaChat, portaVideo, portaAudio);

        return "Status atualizado no servidor central:"
                + "\nEmail: " + email
                + "\nStatus: " + status
                + "\nDisciplina: " + disciplina.getNomeExibicao()
                + "\nPorta chat: " + portaChat
                + "\nPorta vídeo: " + portaVideo
                + "\nPorta áudio: " + portaAudio;
    }
}
