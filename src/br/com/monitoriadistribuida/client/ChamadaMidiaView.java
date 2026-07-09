package br.com.monitoriadistribuida.client;

import br.com.monitoriadistribuida.network.OuvinteChamadaAudio;
import br.com.monitoriadistribuida.network.ConexaoAudioP2P;
import br.com.monitoriadistribuida.network.FonteQuadroTela;
import br.com.monitoriadistribuida.network.OuvinteQuadroVideo;
import br.com.monitoriadistribuida.network.ConexaoVideoP2P;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Image;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;

public class ChamadaMidiaView extends JFrame implements OuvinteQuadroVideo, OuvinteChamadaAudio {

    private static final int FPS_TELA_PADRAO = 8;
    private static final float QUALIDADE_JPEG_PADRAO = 0.45f;

    private final JFrame telaAnterior;
    private final SessionContext session;
    private final String nomePar;
    private final JLabel rotuloVideo = new JLabel("Aguardando vídeo", SwingConstants.CENTER);
    private final JLabel rotuloStatus = new JLabel();
    private volatile ConexaoVideoP2P conexaoVideo;
    private volatile ConexaoAudioP2P conexaoAudio;
    private boolean compartilhandoTela;
    private boolean audioAtivo;

    public ChamadaMidiaView(JFrame telaAnterior,
                         SessionContext session,
                         String nomePar,
                         String endereco,
                         int portaVideo,
                         int portaAudio) {
        this.telaAnterior = telaAnterior;
        this.session = session;
        this.nomePar = normalizarNomePar(nomePar);
        montarTela("Conectando mídia...");
        conectarAoPar(endereco, portaVideo, portaAudio);
    }

    public ChamadaMidiaView(JFrame telaAnterior,
                         SessionContext session,
                         String nomePar,
                         int portaEscutaVideo,
                         int portaEscutaAudio) {
        this.telaAnterior = telaAnterior;
        this.session = session;
        this.nomePar = normalizarNomePar(nomePar);
        montarTela("Abrindo portas de mídia...");
        aguardarPar(portaEscutaVideo, portaEscutaAudio);
    }

    public int getPortaLocalVideo() {
        if (conexaoVideo == null) {
            return -1;
        }

        return conexaoVideo.getPortaLocal();
    }

    public int getPortaLocalAudio() {
        if (conexaoAudio == null) {
            return -1;
        }

        return conexaoAudio.getPortaLocal();
    }

    @Override
    public void aoReceberQuadro(BufferedImage quadro) {
        SwingUtilities.invokeLater(() -> renderizarQuadro(quadro));
    }

    @Override
    public void aoOcorrerErroVideo(String mensagem) {
        SwingUtilities.invokeLater(() -> definirStatus(mensagem, SwingUtils.DANGER));
    }

    @Override
    public void aoOcorrerErroAudio(String mensagem) {
        SwingUtilities.invokeLater(() -> definirStatus(mensagem, SwingUtils.DANGER));
    }

    @Override
    public void dispose() {
        fecharMidia();
        super.dispose();
    }

    private void montarTela(String statusInicial) {
        SwingUtils.configureFrame(this, "Monitoria Distribuida - Chamada P2P", 900, 640);
        setContentPane(SwingUtils.createRootPanel());
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                encerrarChamada();
            }
        });

        JPanel conteudo = SwingUtils.createCardPanel();
        conteudo.setLayout(new BorderLayout(0, 18));
        conteudo.add(criarCabecalho(statusInicial), BorderLayout.NORTH);
        conteudo.add(criarPainelVideo(), BorderLayout.CENTER);
        conteudo.add(criarControles(), BorderLayout.SOUTH);

        getContentPane().add(conteudo, BorderLayout.CENTER);
    }

    private JPanel criarCabecalho(String statusInicial) {
        JPanel cabecalho = new JPanel(new BorderLayout(16, 0));
        cabecalho.setOpaque(false);

        JPanel grupoTitulo = new JPanel();
        grupoTitulo.setOpaque(false);
        grupoTitulo.setLayout(new BoxLayout(grupoTitulo, BoxLayout.Y_AXIS));

        JLabel titulo = SwingUtils.createTitle("Chamada P2P");
        JLabel subtitulo = new JLabel("Usuário: " + session.getNome() + " | Par: " + nomePar);
        subtitulo.setForeground(SwingUtils.MUTED);
        subtitulo.setFont(subtitulo.getFont().deriveFont(Font.PLAIN, 14f));

        rotuloStatus.setOpaque(true);
        rotuloStatus.setHorizontalAlignment(JLabel.CENTER);
        rotuloStatus.setBorder(new EmptyBorder(8, 12, 8, 12));
        definirStatus(statusInicial, SwingUtils.WARNING);

        grupoTitulo.add(titulo);
        grupoTitulo.add(Box.createVerticalStrut(4));
        grupoTitulo.add(subtitulo);

        cabecalho.add(grupoTitulo, BorderLayout.CENTER);
        cabecalho.add(rotuloStatus, BorderLayout.EAST);
        return cabecalho;
    }

    private JPanel criarPainelVideo() {
        JPanel painel = new JPanel(new BorderLayout());
        painel.setBackground(Color.BLACK);
        painel.setBorder(BorderFactory.createLineBorder(new Color(15, 23, 42), 1));

        rotuloVideo.setOpaque(true);
        rotuloVideo.setBackground(Color.BLACK);
        rotuloVideo.setForeground(Color.WHITE);
        rotuloVideo.setPreferredSize(new Dimension(760, 420));

        painel.add(rotuloVideo, BorderLayout.CENTER);
        return painel;
    }

    private JPanel criarControles() {
        JPanel controles = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        controles.setOpaque(false);

        JButton botaoIniciarTela = SwingUtils.createPrimaryButton("Compartilhar tela");
        JButton botaoPararTela = SwingUtils.createSecondaryButton("Parar tela");
        JButton botaoIniciarAudio = SwingUtils.createPrimaryButton("Ligar áudio");
        JButton botaoPararAudio = SwingUtils.createSecondaryButton("Desligar áudio");
        JButton botaoEncerrar = SwingUtils.createGhostButton("Encerrar");

        botaoIniciarTela.addActionListener(e -> iniciarCompartilhamentoTela());
        botaoPararTela.addActionListener(e -> pararCompartilhamentoTela());
        botaoIniciarAudio.addActionListener(e -> iniciarAudio());
        botaoPararAudio.addActionListener(e -> pararAudio());
        botaoEncerrar.addActionListener(e -> encerrarChamada());

        controles.add(botaoIniciarTela);
        controles.add(botaoPararTela);
        controles.add(botaoIniciarAudio);
        controles.add(botaoPararAudio);
        controles.add(botaoEncerrar);
        return controles;
    }

    private void conectarAoPar(String endereco, int portaVideo, int portaAudio) {
        Thread threadConexao = new Thread(() -> {
            ConexaoVideoP2P video = null;
            ConexaoAudioP2P audio = null;

            try {
                video = new ConexaoVideoP2P(endereco, portaVideo, this);
                audio = new ConexaoAudioP2P(endereco, portaAudio, this);
                conexaoVideo = video;
                conexaoAudio = audio;
                SwingUtilities.invokeLater(() -> definirStatus("Mídia conectada", SwingUtils.SUCCESS));
            } catch (IOException | IllegalArgumentException e) {
                if (video != null) {
                    video.close();
                }

                if (audio != null) {
                    audio.close();
                }

                SwingUtilities.invokeLater(() -> {
                    definirStatus("Falha na mídia", SwingUtils.DANGER);
                    SwingUtils.showError(this, "Chamada", "Não foi possível conectar a mídia: " + e.getMessage());
                });
            }
        }, "thread-conexao-midia");

        threadConexao.setDaemon(true);
        threadConexao.start();
    }

    private void aguardarPar(int portaEscutaVideo, int portaEscutaAudio) {
        try {
            conexaoVideo = new ConexaoVideoP2P(portaEscutaVideo, this);
            conexaoAudio = new ConexaoAudioP2P(portaEscutaAudio, this);
            definirStatus("Vídeo " + conexaoVideo.getPortaLocal() + " | Áudio " + conexaoAudio.getPortaLocal(),
                    SwingUtils.WARNING);
        } catch (IOException | IllegalArgumentException e) {
            definirStatus("Falha ao abrir mídia", SwingUtils.DANGER);
            SwingUtils.showError(this, "Chamada", "Não foi possível abrir portas de mídia: " + e.getMessage());
        }
    }

    private void iniciarCompartilhamentoTela() {
        if (conexaoVideo == null) {
            SwingUtils.showWarning(this, "Chamada", "Canal de vídeo não foi inicializado.");
            return;
        }

        try {
            conexaoVideo.iniciarTransmissao(new FonteQuadroTela(), FPS_TELA_PADRAO, QUALIDADE_JPEG_PADRAO);
            compartilhandoTela = true;
            definirStatus("Compartilhando tela", SwingUtils.SUCCESS);
        } catch (Exception e) {
            compartilhandoTela = false;
            definirStatus("Falha na captura", SwingUtils.DANGER);
            SwingUtils.showError(this, "Chamada", "Não foi possível capturar a tela: " + e.getMessage());
        }
    }

    private void pararCompartilhamentoTela() {
        if (conexaoVideo != null) {
            conexaoVideo.pararTransmissao();
        }

        compartilhandoTela = false;
        definirStatus(audioAtivo ? "Áudio ativo" : "Tela pausada", SwingUtils.WARNING);
    }

    private void iniciarAudio() {
        if (conexaoAudio == null) {
            SwingUtils.showWarning(this, "Chamada", "Canal de áudio não foi inicializado.");
            return;
        }

        conexaoAudio.iniciarAudio();
        audioAtivo = true;
        definirStatus(compartilhandoTela ? "Tela e áudio ativos" : "Áudio ativo", SwingUtils.SUCCESS);
    }

    private void pararAudio() {
        if (conexaoAudio != null) {
            conexaoAudio.pararAudio();
        }

        audioAtivo = false;
        definirStatus(compartilhandoTela ? "Compartilhando tela" : "Áudio desligado", SwingUtils.WARNING);
    }

    private void renderizarQuadro(BufferedImage quadro) {
        if (quadro == null) {
            return;
        }

        int larguraDestino = rotuloVideo.getWidth();
        int alturaDestino = rotuloVideo.getHeight();

        if (larguraDestino <= 0 || alturaDestino <= 0) {
            rotuloVideo.setIcon(new ImageIcon(quadro));
            rotuloVideo.setText("");
            return;
        }

        double escala = Math.min(larguraDestino / (double) quadro.getWidth(), alturaDestino / (double) quadro.getHeight());
        int largura = Math.max(1, (int) (quadro.getWidth() * escala));
        int altura = Math.max(1, (int) (quadro.getHeight() * escala));
        Image redimensionada = quadro.getScaledInstance(largura, altura, Image.SCALE_FAST);

        rotuloVideo.setIcon(new ImageIcon(redimensionada));
        rotuloVideo.setText("");
    }

    private void definirStatus(String texto, Color cor) {
        rotuloStatus.setText(texto);
        rotuloStatus.setForeground(Color.WHITE);
        rotuloStatus.setBackground(cor);
        rotuloStatus.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
    }

    private void encerrarChamada() {
        fecharMidia();
        dispose();

        if (telaAnterior != null) {
            telaAnterior.setVisible(true);
        }
    }

    private void fecharMidia() {
        if (conexaoVideo != null) {
            conexaoVideo.close();
            conexaoVideo = null;
        }

        if (conexaoAudio != null) {
            conexaoAudio.close();
            conexaoAudio = null;
        }
    }

    private String normalizarNomePar(String valor) {
        if (valor == null || valor.trim().isEmpty()) {
            return "Par";
        }

        return valor.trim();
    }
}
