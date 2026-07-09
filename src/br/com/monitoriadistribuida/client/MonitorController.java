package br.com.monitoriadistribuida.client;

import br.com.monitoriadistribuida.server.model.Disciplina;
import br.com.monitoriadistribuida.server.model.StatusMonitor;

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

    public String atualizarStatus(String email, StatusMonitor status, Disciplina disciplina, int porta) {
        return atualizarStatus(email, status, disciplina, porta, 0, 0);
    }

    public String atualizarStatus(String email,
                                  StatusMonitor status,
                                  Disciplina disciplina,
                                  int portaChat,
                                  int portaVideo,
                                  int portaAudio) {
        return "Comando preparado para o servidor central:"
                + "\nEmail: " + email
                + "\nStatus: " + status
                + "\nDisciplina: " + disciplina.getNomeExibicao()
                + "\nPorta chat: " + portaChat
                + "\nPorta vídeo: " + portaVideo
                + "\nPorta áudio: " + portaAudio;
    }
}
