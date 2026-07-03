package br.com.monitoriadistribuida.client;

import br.com.monitoriadistribuida.server.model.Disciplina;

import java.util.ArrayList;
import java.util.List;

public class AlunoController {

    public List<Disciplina> listarDisciplinas() {
        List<Disciplina> disciplinas = new ArrayList<>();
        for (Disciplina disciplina : Disciplina.values()) {
            disciplinas.add(disciplina);
        }
        return disciplinas;
    }

    public String listarMonitoresPorDisciplina(Disciplina disciplina) {
        return "Consulta simulada para " + disciplina.getNomeExibicao()
                + "\n\nNenhum monitor foi integrado ao backend ainda, mas esta tela ja acompanha o fluxo."
                + "\nQuando o servidor retornar dados reais, eles podem ser exibidos aqui.";
    }

    public String solicitarAtendimento(String emailAluno, String emailMonitor) {
        return "Solicitacao simulada enviada."
                + "\nAluno: " + emailAluno
                + "\nMonitor: " + emailMonitor;
    }
}
