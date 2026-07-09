package br.com.monitoriadistribuida.server.model;

public class MonitorDisponivel {

    private String nome;
    private String email;
    private Disciplina disciplina;
    private String ip;
    private int portaChat;
    private int portaVideo;
    private int portaAudio;
    private StatusMonitor status;

    public MonitorDisponivel(String nome, String email, Disciplina disciplina, String ip, int porta, StatusMonitor status) {
        this(nome, email, disciplina, ip, porta, 0, 0, status);
    }

    public MonitorDisponivel(String nome,
                             String email,
                             Disciplina disciplina,
                             String ip,
                             int portaChat,
                             int portaVideo,
                             int portaAudio,
                             StatusMonitor status) {
        this.nome = nome;
        this.email = email;
        this.disciplina = disciplina;
        this.ip = ip;
        this.portaChat = portaChat;
        this.portaVideo = portaVideo;
        this.portaAudio = portaAudio;
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
        return portaChat;
    }

    public int getPortaChat() {
        return portaChat;
    }

    public int getPortaVideo() {
        return portaVideo;
    }

    public int getPortaAudio() {
        return portaAudio;
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
        this.portaChat = porta;
    }

    public void setPortaChat(int portaChat) {
        this.portaChat = portaChat;
    }

    public void setPortaVideo(int portaVideo) {
        this.portaVideo = portaVideo;
    }

    public void setPortaAudio(int portaAudio) {
        this.portaAudio = portaAudio;
    }
}
