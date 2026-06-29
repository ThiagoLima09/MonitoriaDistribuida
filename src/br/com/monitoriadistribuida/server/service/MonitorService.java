package br.com.monitoriadistribuida.server.service;

import br.com.monitoriadistribuida.server.model.Disciplina;
import br.com.monitoriadistribuida.server.model.MonitorDisponivel;
import br.com.monitoriadistribuida.server.model.StatusMonitor;
import br.com.monitoriadistribuida.server.model.TipoUsuario;
import br.com.monitoriadistribuida.server.model.Usuario;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class MonitorService {

    private Map<String, MonitorDisponivel> monitoresDisponiveis = new ConcurrentHashMap<>();

    public boolean atualizarStatus(Usuario usuario, StatusMonitor status, Disciplina disciplina, String ip, int porta) {
        if (usuario == null) {
            return false;
        }

        if (usuario.getTipo() != TipoUsuario.MONITOR) {
            return false;
        }

        if (status == StatusMonitor.OFFLINE) {
            monitoresDisponiveis.remove(usuario.getEmail());
            return true;
        }

        MonitorDisponivel monitor = new MonitorDisponivel(
                usuario.getNome(),
                usuario.getEmail(),
                disciplina,
                ip,
                porta,
                status
        );

        monitoresDisponiveis.put(usuario.getEmail(), monitor);

        return true;
    }

    public List<MonitorDisponivel> listarPorDisciplina(Disciplina disciplina) {
        List<MonitorDisponivel> resultado = new ArrayList<>();

        for (MonitorDisponivel monitor : monitoresDisponiveis.values()) {
            if (monitor.getDisciplina() == disciplina && monitor.getStatus() == StatusMonitor.DISPONIVEL) {
                resultado.add(monitor);
            }
        }

        return resultado;
    }

    public MonitorDisponivel buscarMonitorDisponivel(String emailMonitor) {
        MonitorDisponivel monitor = monitoresDisponiveis.get(emailMonitor);

        if (monitor == null) {
            return null;
        }

        if (monitor.getStatus() != StatusMonitor.DISPONIVEL) {
            return null;
        }

        return monitor;
    }

    public MonitorDisponivel solicitarAtendimento(String emailMonitor) {
        MonitorDisponivel monitor = buscarMonitorDisponivel(emailMonitor);

        if (monitor == null) {
            return null;
        }

        monitor.setStatus(StatusMonitor.OCUPADO);

        return monitor;
    }
}