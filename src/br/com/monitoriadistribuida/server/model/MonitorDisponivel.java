package br.com.monitoriadistribuida.server.model;

public class MonitorDisponivel {

    private String nome;
    private String email;
    private Disciplina disciplina;
    private String ip;
    private int porta;
    private StatusMonitor status;

    public MonitorDisponivel(String nome, String email, Disciplina disciplina, String ip, int porta, StatusMonitor status) {
        this.nome = nome;
        this.email = email;
        this.disciplina = disciplina;
        this.ip = ip;
        this.porta = porta;
        this.status = status;
    }

    public String getNome() {
        return nome;
    }

    public String getEmail() {
        return email;
    }

    public Disciplina getDisciplina() {
        return disciplina;
    }

    public String getIp() {
        return ip;
    }

    public int getPorta() {
        return porta;
    }

    public StatusMonitor getStatus() {
        return status;
    }

    public void setStatus(StatusMonitor status) {
        this.status = status;
    }

    public void setDisciplina(Disciplina disciplina) {
        this.disciplina = disciplina;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public void setPorta(int porta) {
        this.porta = porta;
    }
}