package br.com.monitoriadistribuida.client;

import br.com.monitoriadistribuida.network.MessageListener;
import br.com.monitoriadistribuida.network.PeerConnection;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;

public class ChatView extends JFrame implements MessageListener {

    private final JFrame telaAnterior;
    private final SessionContext session;
    private final String nomePar;
    private final JTextArea areaMensagens = SwingUtils.createTextArea();
    private final JTextField campoMensagem = SwingUtils.createTextField("Mensagem");
    private final JLabel rotuloStatus = new JLabel();
    private volatile PeerConnection conexaoPar;

    public ChatView(JFrame telaAnterior, SessionContext session, String nomePar, String endereco, int porta) {
        this.telaAnterior = telaAnterior;
        this.session = session;
        this.nomePar = normalizarNomePar(nomePar);
        montarTela("Conectando em " + endereco + ":" + porta);
        conectarAoPar(endereco, porta);
    }

    public ChatView(JFrame telaAnterior, SessionContext session, String nomePar, int portaEscuta) {
        this.telaAnterior = telaAnterior;
        this.session = session;
        this.nomePar = normalizarNomePar(nomePar);
        montarTela("Abrindo porta P2P...");
        aguardarPar(portaEscuta);
    }

    public int getPortaLocal() {
        if (conexaoPar == null) {
            return -1;
        }

        return conexaoPar.getPortaLocal();
    }

    @Override
    public void onMessageReceived(String mensagem) {
        SwingUtilities.invokeLater(() -> {
            if (mensagem != null && mensagem.startsWith("ERRO;")) {
                definirStatus(mensagem.substring("ERRO;".length()), SwingUtils.DANGER);
                return;
            }

            adicionarMensagem(nomePar, mensagem);
        });
    }

    @Override
    public void dispose() {
        fecharConexaoPar();
        super.dispose();
    }

    private void montarTela(String statusInicial) {
        SwingUtils.configureFrame(this, "Monitoria Distribuida - Chat", 780, 560);
        setContentPane(SwingUtils.createRootPanel());
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                encerrarChat();
            }
        });

        JPanel conteudo = SwingUtils.createCardPanel();
        conteudo.setLayout(new BorderLayout(0, 18));
        conteudo.add(criarCabecalho(statusInicial), BorderLayout.NORTH);
        conteudo.add(criarPainelMensagens(), BorderLayout.CENTER);
        conteudo.add(criarCompositor(), BorderLayout.SOUTH);

        getContentPane().add(conteudo, BorderLayout.CENTER);
    }

    private JPanel criarCabecalho(String statusInicial) {
        JPanel cabecalho = new JPanel(new BorderLayout(16, 0));
        cabecalho.setOpaque(false);

        JPanel grupoTitulo = new JPanel();
        grupoTitulo.setOpaque(false);
        grupoTitulo.setLayout(new BoxLayout(grupoTitulo, BoxLayout.Y_AXIS));

        JLabel titulo = SwingUtils.createTitle("Chat P2P");
        JLabel subtitulo = new JLabel("Usuário: " + session.getNome() + " | Par: " + nomePar);
        subtitulo.setForeground(SwingUtils.MUTED);
        subtitulo.setFont(subtitulo.getFont().deriveFont(Font.PLAIN, 14f));

        rotuloStatus.setOpaque(true);
        rotuloStatus.setBorder(new EmptyBorder(8, 12, 8, 12));
        rotuloStatus.setHorizontalAlignment(JLabel.CENTER);
        definirStatus(statusInicial, SwingUtils.WARNING);

        grupoTitulo.add(titulo);
        grupoTitulo.add(Box.createVerticalStrut(4));
        grupoTitulo.add(subtitulo);

        cabecalho.add(grupoTitulo, BorderLayout.CENTER);
        cabecalho.add(rotuloStatus, BorderLayout.EAST);
        return cabecalho;
    }

    private JScrollPane criarPainelMensagens() {
        areaMensagens.setRows(16);
        areaMensagens.setText("");
        return SwingUtils.createScrollPane(areaMensagens);
    }

    private JPanel criarCompositor() {
        JPanel compositor = new JPanel(new BorderLayout(10, 0));
        compositor.setOpaque(false);

        JButton botaoEnviar = SwingUtils.createPrimaryButton("Enviar");
        JButton botaoEncerrar = SwingUtils.createGhostButton("Encerrar");

        botaoEnviar.addActionListener(e -> enviarMensagemAtual());
        botaoEncerrar.addActionListener(e -> encerrarChat());
        campoMensagem.addActionListener(e -> enviarMensagemAtual());

        JPanel acoes = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        acoes.setOpaque(false);
        acoes.add(botaoEnviar);
        acoes.add(botaoEncerrar);

        compositor.add(campoMensagem, BorderLayout.CENTER);
        compositor.add(acoes, BorderLayout.EAST);
        return compositor;
    }

    private void conectarAoPar(String endereco, int porta) {
        Thread threadConexao = new Thread(() -> {
            try {
                PeerConnection conexao = new PeerConnection(endereco, porta, this);
                conexaoPar = conexao;
                SwingUtilities.invokeLater(() -> definirStatus("Conectado", SwingUtils.SUCCESS));
            } catch (IOException | IllegalArgumentException e) {
                SwingUtilities.invokeLater(() -> {
                    definirStatus("Falha na conexão", SwingUtils.DANGER);
                    SwingUtils.showError(this, "Chat", "Não foi possível conectar ao par: " + e.getMessage());
                });
            }
        }, "thread-conexao-chat");

        threadConexao.setDaemon(true);
        threadConexao.start();
    }

    private void aguardarPar(int portaEscuta) {
        try {
            conexaoPar = new PeerConnection(portaEscuta, this);
            definirStatus("Aguardando na porta " + conexaoPar.getPortaLocal(), SwingUtils.WARNING);
        } catch (IOException | IllegalArgumentException e) {
            definirStatus("Falha ao abrir porta", SwingUtils.DANGER);
            SwingUtils.showError(this, "Chat", "Não foi possível abrir a porta P2P: " + e.getMessage());
        }
    }

    private void enviarMensagemAtual() {
        String texto = campoMensagem.getText().trim();

        if (texto.isEmpty()) {
            return;
        }

        if (conexaoPar == null || !conexaoPar.estaConectado()) {
            SwingUtils.showWarning(this, "Chat", "Nenhum par conectado.");
            return;
        }

        try {
            conexaoPar.enviarMensagem(texto);
            adicionarMensagem("Eu", texto);
            campoMensagem.setText("");
            definirStatus("Conectado", SwingUtils.SUCCESS);
        } catch (IOException e) {
            definirStatus("Falha no envio", SwingUtils.DANGER);
            SwingUtils.showError(this, "Chat", "Não foi possível enviar a mensagem: " + e.getMessage());
        }
    }

    private void adicionarMensagem(String autor, String mensagem) {
        areaMensagens.append(autor + ": " + mensagem + "\n");
        areaMensagens.setCaretPosition(areaMensagens.getDocument().getLength());
    }

    private void definirStatus(String texto, Color cor) {
        rotuloStatus.setText(texto);
        rotuloStatus.setForeground(Color.WHITE);
        rotuloStatus.setBackground(cor);
        rotuloStatus.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
    }

    private void encerrarChat() {
        fecharConexaoPar();
        dispose();

        if (telaAnterior != null) {
            telaAnterior.setVisible(true);
        }
    }

    private void fecharConexaoPar() {
        if (conexaoPar != null) {
            conexaoPar.close();
            conexaoPar = null;
        }
    }

    private String normalizarNomePar(String valor) {
        if (valor == null || valor.trim().isEmpty()) {
            return "Par";
        }

        return valor.trim();
    }
}
