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
        return "Comando preparado para backend:"
                + "\nEmail: " + email
                + "\nStatus: " + status
                + "\nDisciplina: " + disciplina.getNomeExibicao()
                + "\nPorta: " + porta;
    }
}
