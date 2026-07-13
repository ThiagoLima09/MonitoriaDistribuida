package br.com.monitoriadistribuida.client;

import br.com.monitoriadistribuida.network.InformacoesMonitor;
import br.com.monitoriadistribuida.network.ServerConnection;
import br.com.monitoriadistribuida.server.model.Disciplina;

import java.io.IOException;
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

    public List<InformacoesMonitor> listarMonitoresPorDisciplina(ServerConnection conexaoServidor,
                                                                 Disciplina disciplina) throws IOException {
        try {
            validarConexao(conexaoServidor);
            return conexaoServidor.listarMonitores(disciplina);
        } catch (IOException e) {
            if (e.getMessage() != null && e.getMessage().contains("Nenhum monitor disponível")) {
                return new ArrayList<>();
            }
            throw e;
        }
    }

    public InformacoesMonitor solicitarAtendimento(ServerConnection conexaoServidor,
                                                   String emailAluno,
                                                   String emailMonitor) throws IOException {
        validarConexao(conexaoServidor);
        return conexaoServidor.solicitarAtendimento(emailAluno, emailMonitor);
    }

    private void validarConexao(ServerConnection conexaoServidor) {
        if (conexaoServidor == null) {
            throw new IllegalStateException("Sessão sem conexão com o servidor central.");
        }
    }
}
