package br.com.monitoriadistribuida.network;

public class InformacoesMonitor {

    private final String nome;
    private final String email;
    private final String disciplina;
    private final String ip;
    private final int portaChat;
    private final int portaVideo;
    private final int portaAudio;

    public InformacoesMonitor(String nome,
                       String email,
                       String disciplina,
                       String ip,
                       int portaChat,
                       int portaVideo,
                       int portaAudio) {
        this.nome = nome;
        this.email = email;
        this.disciplina = disciplina;
        this.ip = ip;
        this.portaChat = portaChat;
        this.portaVideo = portaVideo;
        this.portaAudio = portaAudio;
    }

    public String getNome() {
        return nome;
    }

    public String getEmail() {
        return email;
    }

    public String getDisciplina() {
        return disciplina;
    }

    public String getIp() {
        return ip;
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
}
