package br.com.monitoriadistribuida.client;

import br.com.monitoriadistribuida.network.OuvinteChamadaAudio;
import br.com.monitoriadistribuida.network.ConexaoAudioP2P;
import br.com.monitoriadistribuida.network.FonteQuadroTela;
import br.com.monitoriadistribuida.network.FonteQuadroWebcam;
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
    private final Runnable aoEncerrarChamada;
    private final JLabel rotuloVideo = new JLabel("Aguardando vídeo", SwingConstants.CENTER);
    private final JLabel rotuloStatus = new JLabel();
    private volatile ConexaoVideoP2P conexaoVideo;
    private volatile ConexaoAudioP2P conexaoAudio;
    private volatile FonteQuadroWebcam fonteWebcam;
    private boolean transmitindoVideo;
    private boolean microfoneAtivo = true;
    private boolean chamadaEncerrada;
    private volatile boolean recursosFechando;

    public ChamadaMidiaView(JFrame telaAnterior,
                         SessionContext session,
                         String nomePar,
                         String endereco,
                         int portaVideo,
                         int portaAudio) {
        this(telaAnterior, session, nomePar, endereco, portaVideo, portaAudio, null);
    }

    public ChamadaMidiaView(JFrame telaAnterior,
                         SessionContext session,
                         String nomePar,
                         String endereco,
                         int portaVideo,
                         int portaAudio,
                         Runnable aoEncerrarChamada) {
        this.telaAnterior = telaAnterior;
        this.session = session;
        this.nomePar = normalizarNomePar(nomePar);
        this.aoEncerrarChamada = aoEncerrarChamada;
        montarTela("Conectando mídia...");
        conectarAoPar(endereco, portaVideo, portaAudio);
    }

    public ChamadaMidiaView(JFrame telaAnterior,
                         SessionContext session,
                         String nomePar,
                         int portaEscutaVideo,
                         int portaEscutaAudio) {
        this(telaAnterior, session, nomePar, portaEscutaVideo, portaEscutaAudio, null);
    }

    public ChamadaMidiaView(JFrame telaAnterior,
                         SessionContext session,
                         String nomePar,
                         int portaEscutaVideo,
                         int portaEscutaAudio,
                         Runnable aoEncerrarChamada) {
        this.telaAnterior = telaAnterior;
        this.session = session;
        this.nomePar = normalizarNomePar(nomePar);
        this.aoEncerrarChamada = aoEncerrarChamada;
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
        chamadaEncerrada = true;
        fecharMidiaEmSegundoPlano();
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
        JLabel subtitulo = new JLabel("Usuário: " + session.getNome() + " | Com: " + nomePar);
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
        JButton botaoIniciarWebcam = SwingUtils.createPrimaryButton("Compartilhar webcam");
        JButton botaoPararVideo = SwingUtils.createSecondaryButton("Parar vídeo");
        JButton botaoMicrofone = SwingUtils.createSecondaryButton("Mutar microfone");
        JButton botaoEncerrar = SwingUtils.createGhostButton("Encerrar");

        botaoIniciarTela.addActionListener(e -> iniciarCompartilhamentoTela());
        botaoIniciarWebcam.addActionListener(e -> iniciarCompartilhamentoWebcam());
        botaoPararVideo.addActionListener(e -> pararTransmissaoVideo());
        botaoMicrofone.addActionListener(e -> alternarMicrofone(botaoMicrofone));
        botaoEncerrar.addActionListener(e -> encerrarChamada());

        controles.add(botaoIniciarTela);
        controles.add(botaoIniciarWebcam);
        controles.add(botaoPararVideo);
        controles.add(botaoMicrofone);
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
                conexaoAudio.iniciarEscuta();
                conexaoAudio.ativarMicrofone();
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
            conexaoAudio.iniciarEscuta();
            conexaoAudio.ativarMicrofone();
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
            fecharFonteWebcam();
            conexaoVideo.iniciarTransmissao(new FonteQuadroTela(), FPS_TELA_PADRAO, QUALIDADE_JPEG_PADRAO);
            transmitindoVideo = true;
            definirStatus("Compartilhando tela", SwingUtils.SUCCESS);
        } catch (Exception e) {
            transmitindoVideo = false;
            definirStatus("Falha na captura", SwingUtils.DANGER);
            SwingUtils.showError(this, "Chamada", "Não foi possível capturar a tela: " + e.getMessage());
        }
    }

    private void iniciarCompartilhamentoWebcam() {
        if (conexaoVideo == null) {
            SwingUtils.showWarning(this, "Chamada", "Canal de vídeo não foi inicializado.");
            return;
        }

        try {
            fecharFonteWebcam();
            fonteWebcam = new FonteQuadroWebcam();
            conexaoVideo.iniciarTransmissao(fonteWebcam, FPS_TELA_PADRAO, QUALIDADE_JPEG_PADRAO);
            transmitindoVideo = true;
            definirStatus("Compartilhando webcam", SwingUtils.SUCCESS);
        } catch (Exception e) {
            fecharFonteWebcam();
            transmitindoVideo = false;
            definirStatus("Falha na webcam", SwingUtils.DANGER);
            SwingUtils.showError(this, "Chamada", "Não foi possível abrir a webcam: " + e.getMessage());
        }
    }

    private void pararTransmissaoVideo() {
        if (conexaoVideo != null) {
            conexaoVideo.pararTransmissao();
        }

        fecharFonteWebcam();
        transmitindoVideo = false;
        definirStatus("Vídeo pausado", SwingUtils.WARNING);
    }

    private void alternarMicrofone(JButton botaoMicrofone) {
        if (conexaoAudio == null) {
            SwingUtils.showWarning(this, "Chamada", "Canal de áudio não foi inicializado.");
            return;
        }

        if (microfoneAtivo) {
            conexaoAudio.silenciarMicrofone();
            microfoneAtivo = false;
            botaoMicrofone.setText("Ativar microfone");
            definirStatus("Microfone mutado", SwingUtils.WARNING);
        } else {
            conexaoAudio.ativarMicrofone();
            microfoneAtivo = true;
            botaoMicrofone.setText("Mutar microfone");
            definirStatus(transmitindoVideo ? "Vídeo e microfone ativos" : "Microfone ativo", SwingUtils.SUCCESS);
        }
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
        if (chamadaEncerrada) {
            return;
        }

        chamadaEncerrada = true;

        if (aoEncerrarChamada != null) {
            aoEncerrarChamada.run();
        }

        fecharMidiaEmSegundoPlano();
        setVisible(false);
        super.dispose();

        if (telaAnterior != null) {
            SwingUtils.exibirCentralizado(telaAnterior);
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

        fecharFonteWebcam();
    }

    private void fecharMidiaEmSegundoPlano() {
        if (recursosFechando) {
            return;
        }

        recursosFechando = true;

        Thread thread = new Thread(this::fecharMidia, "thread-fechamento-midia-p2p");
        thread.setDaemon(true);
        thread.start();
    }

    private void fecharFonteWebcam() {
        if (fonteWebcam != null) {
            fonteWebcam.close();
            fonteWebcam = null;
        }
    }

    private String normalizarNomePar(String valor) {
        if (valor == null || valor.trim().isEmpty()) {
            return "Par";
        }

        return valor.trim();
    }
}
