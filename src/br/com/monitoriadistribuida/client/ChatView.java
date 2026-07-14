package br.com.monitoriadistribuida.client;

import br.com.monitoriadistribuida.network.MessageListener;
import br.com.monitoriadistribuida.network.OuvinteTransferenciaArquivo;
import br.com.monitoriadistribuida.network.PeerConnection;
import br.com.monitoriadistribuida.network.ServicoTransferenciaArquivo;
import br.com.monitoriadistribuida.server.model.TipoUsuario;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;

public class ChatView extends JFrame implements MessageListener, OuvinteTransferenciaArquivo {

    private static final String CONTROLE = "CONTROLE;";
    private static final String CONTROLE_CONEXAO_ESTABELECIDA = "CONTROLE;CONEXAO_ESTABELECIDA";
    private static final String CONTROLE_CONEXAO_ENCERRADA = "CONTROLE;CONEXAO_ENCERRADA";
    private static final String CONTROLE_ARQUIVO_PORTA = "CONTROLE;ARQUIVO_PORTA;";
    private static final String CONTROLE_CHAMADA_MIDIA = "CONTROLE;CHAMADA_MIDIA;";
    private static final String CONTROLE_CHAMADA_ENCERRADA = "CONTROLE;CHAMADA_ENCERRADA";
    private static final String CONTROLE_CHAT_ENCERRADO = "CONTROLE;CHAT_ENCERRADO";

    private final JFrame telaAnterior;
    private final SessionContext session;
    private final String nomePar;
    private final String disciplina;
    private final JTextArea areaMensagens = SwingUtils.createTextArea();
    private final JTextField campoMensagem = SwingUtils.createTextField("Mensagem");
    private final JLabel rotuloStatus = new JLabel();
    private final DefaultListModel<ArquivoTransferido> arquivosModel = new DefaultListModel<>();
    private final JList<ArquivoTransferido> listaArquivos = new JList<>(arquivosModel);
    private final ServicoTransferenciaArquivo servicoTransferenciaArquivo = new ServicoTransferenciaArquivo(this);
    private volatile PeerConnection conexaoPar;
    private volatile int portaArquivoLocal = -1;
    private volatile int portaArquivoPar = -1;
    private volatile String enderecoArquivoPar;
    private volatile ChamadaMidiaView chamadaMidia;
    private volatile boolean portaArquivoAnunciada;
    private volatile boolean chatEncerrado;
    private volatile boolean recursosFechando;

    public ChatView(JFrame telaAnterior, SessionContext session, String nomePar, String endereco, int porta) {
        this(telaAnterior, session, nomePar, "Monitoria", endereco, porta);
    }

    public ChatView(JFrame telaAnterior,
                    SessionContext session,
                    String nomePar,
                    String disciplina,
                    String endereco,
                    int porta) {
        this.telaAnterior = telaAnterior;
        this.session = session;
        this.nomePar = normalizarNomePar(nomePar);
        this.disciplina = normalizarDisciplina(disciplina);
        montarTela("Conectando em " + endereco + ":" + porta);
        iniciarReceptorArquivo();
        conectarAoPar(endereco, porta);
    }

    public ChatView(JFrame telaAnterior, SessionContext session, String nomePar, int portaEscuta) {
        this(telaAnterior, session, nomePar, portaEscuta, "Monitoria");
    }

    public ChatView(JFrame telaAnterior,
                    SessionContext session,
                    String nomePar,
                    int portaEscuta,
                    String disciplina) {
        this.telaAnterior = telaAnterior;
        this.session = session;
        this.nomePar = normalizarNomePar(nomePar);
        this.disciplina = normalizarDisciplina(disciplina);
        montarTela("Abrindo porta P2P...");
        iniciarReceptorArquivo();
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
            if (processarMensagemControle(mensagem)) {
                return;
            }

            if (mensagem != null && mensagem.startsWith("ERRO;")) {
                definirStatus(mensagem.substring("ERRO;".length()), SwingUtils.DANGER);
                return;
            }

            adicionarMensagem(nomePar, mensagem);
        });
    }

    @Override
    public void dispose() {
        if (!chatEncerrado) {
            chatEncerrado = true;
            fecharChamadaMidiaNaEdt();
            fecharRecursosChatEmSegundoPlano();
        }

        super.dispose();
    }

    @Override
    public void aoReceberArquivo(File arquivo) {
        SwingUtilities.invokeLater(() -> {
            adicionarMensagem("Sistema", "Arquivo recebido: " + arquivo.getAbsolutePath());
            registrarArquivoTrocado("Recebido", arquivo);
            definirStatus("Arquivo recebido", SwingUtils.SUCCESS);
        });
    }

    @Override
    public void aoOcorrerErroTransferencia(String mensagem) {
        SwingUtilities.invokeLater(() -> {
            adicionarMensagem("Sistema", mensagem);
            definirStatus("Falha em arquivo", SwingUtils.DANGER);
        });
    }

    private void montarTela(String statusInicial) {
        SwingUtils.configureFrame(this, "Monitoria Distribuida - " + tituloMonitoria(), 960, 600);
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
        conteudo.add(criarPainelCentral(), BorderLayout.CENTER);
        conteudo.add(criarCompositor(), BorderLayout.SOUTH);

        getContentPane().add(conteudo, BorderLayout.CENTER);
    }

    private JPanel criarCabecalho(String statusInicial) {
        JPanel cabecalho = new JPanel(new BorderLayout(16, 0));
        cabecalho.setOpaque(false);

        JPanel grupoTitulo = new JPanel();
        grupoTitulo.setOpaque(false);
        grupoTitulo.setLayout(new BoxLayout(grupoTitulo, BoxLayout.Y_AXIS));

        JLabel titulo = SwingUtils.createTitle(tituloMonitoria());
        JLabel subtitulo = new JLabel("Usuário: " + session.getNome() + " | " + rotuloPessoaPar() + ": " + nomePar);
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

    private JPanel criarPainelCentral() {
        JPanel painel = new JPanel(new BorderLayout(14, 0));
        painel.setOpaque(false);
        painel.add(criarPainelMensagens(), BorderLayout.CENTER);
        painel.add(criarPainelArquivos(), BorderLayout.EAST);
        return painel;
    }

    private JScrollPane criarPainelMensagens() {
        areaMensagens.setRows(16);
        areaMensagens.setText("");
        return SwingUtils.createScrollPane(areaMensagens);
    }

    private JPanel criarPainelArquivos() {
        JPanel painel = new JPanel(new BorderLayout(0, 10));
        painel.setOpaque(false);
        painel.setPreferredSize(new Dimension(240, 0));

        JLabel titulo = SwingUtils.createSectionLabel("Arquivos trocados");
        titulo.setFont(titulo.getFont().deriveFont(Font.BOLD, 15f));

        listaArquivos.setBackground(new Color(248, 250, 252));
        listaArquivos.setForeground(SwingUtils.TEXT);
        listaArquivos.setFixedCellHeight(48);
        listaArquivos.setCellRenderer(new ArquivoTransferidoRenderer());
        listaArquivos.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    abrirArquivoSelecionado();
                }
            }
        });

        JButton abrirButton = SwingUtils.createSecondaryButton("Abrir");
        abrirButton.addActionListener(e -> abrirArquivoSelecionado());

        painel.add(titulo, BorderLayout.NORTH);
        painel.add(SwingUtils.createScrollPane(listaArquivos), BorderLayout.CENTER);
        painel.add(abrirButton, BorderLayout.SOUTH);
        return painel;
    }

    private JPanel criarCompositor() {
        JPanel compositor = new JPanel(new BorderLayout(10, 0));
        compositor.setOpaque(false);

        JButton botaoEnviar = SwingUtils.createPrimaryButton("Enviar");
        JButton botaoArquivo = SwingUtils.createSecondaryButton("Enviar arquivo");
        JButton botaoChamada = SwingUtils.createSecondaryButton("Chamada");
        JButton botaoEncerrar = SwingUtils.createGhostButton("Encerrar");

        botaoEnviar.addActionListener(e -> enviarMensagemAtual());
        botaoArquivo.addActionListener(e -> enviarArquivo());
        botaoChamada.addActionListener(e -> iniciarChamadaMidia());
        botaoEncerrar.addActionListener(e -> encerrarChat());
        campoMensagem.addActionListener(e -> enviarMensagemAtual());

        JPanel acoes = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        acoes.setOpaque(false);
        acoes.add(botaoArquivo);
        acoes.add(botaoChamada);
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
                enderecoArquivoPar = endereco;
                anunciarPortaArquivo();
                SwingUtilities.invokeLater(() -> definirStatus("Conectado", SwingUtils.SUCCESS));
            } catch (IOException | IllegalArgumentException e) {
                SwingUtilities.invokeLater(() -> {
                    definirStatus("Falha na conexão", SwingUtils.DANGER);
                    SwingUtils.showError(
                            this,
                            "Chat",
                            "Não foi possível conectar ao atendimento.\n\nDetalhes: " + e.getMessage());
                });
            }
        }, "thread-conexao-chat");

        threadConexao.setDaemon(true);
        threadConexao.start();
    }

    private void aguardarPar(int portaEscuta) {
        try {
            conexaoPar = new PeerConnection(portaEscuta, this);
            definirStatus("Aguardando Aluno", SwingUtils.WARNING);
        } catch (IOException | IllegalArgumentException e) {
            definirStatus("Falha ao abrir porta", SwingUtils.DANGER);
            SwingUtils.showError(
                    this,
                    "Chat",
                    "Não foi possível preparar o chat.\n\nDetalhes: " + e.getMessage());
        }
    }

    private void enviarMensagemAtual() {
        String texto = campoMensagem.getText().trim();

        if (texto.isEmpty()) {
            return;
        }

        if (conexaoPar == null || !conexaoPar.estaConectado()) {
            SwingUtils.showWarning(this, "Chat", "A conexão ainda não está ativa.");
            return;
        }

        try {
            conexaoPar.enviarMensagem(texto);
            adicionarMensagem("Eu", texto);
            campoMensagem.setText("");
            definirStatus("Conectado", SwingUtils.SUCCESS);
        } catch (IOException e) {
            definirStatus("Falha no envio", SwingUtils.DANGER);
            SwingUtils.showError(
                    this,
                    "Chat",
                    "Não foi possível enviar a mensagem.\n\nDetalhes: " + e.getMessage());
        }
    }

    private void enviarArquivo() {
        if (!validarConexaoAtiva()) {
            return;
        }

        if (portaArquivoPar <= 0) {
            SwingUtils.showWarning(this, "Arquivo", "A transferência de arquivos ainda não está pronta.");
            anunciarPortaArquivo();
            return;
        }

        JFileChooser seletorArquivo = new JFileChooser();
        int resultado = seletorArquivo.showOpenDialog(this);

        if (resultado != JFileChooser.APPROVE_OPTION) {
            return;
        }

        File arquivo = seletorArquivo.getSelectedFile();
        String enderecoDestino = resolverEnderecoPar();

        Thread threadEnvio = new Thread(() -> {
            try {
                servicoTransferenciaArquivo.enviarArquivo(enderecoDestino, portaArquivoPar, arquivo);
                SwingUtilities.invokeLater(() -> {
                    adicionarMensagem("Eu", "Arquivo enviado: " + arquivo.getName());
                    registrarArquivoTrocado("Enviado", arquivo);
                    definirStatus("Arquivo enviado", SwingUtils.SUCCESS);
                });
            } catch (Exception e) {
                SwingUtilities.invokeLater(() -> {
                    definirStatus("Falha no arquivo", SwingUtils.DANGER);
                    SwingUtils.showError(
                            this,
                            "Arquivo",
                            "Não foi possível enviar o arquivo.\n\nDetalhes: " + e.getMessage());
                });
            }
        }, "thread-envio-arquivo");

        threadEnvio.setDaemon(true);
        threadEnvio.start();
    }

    private void iniciarChamadaMidia() {
        if (!validarConexaoAtiva()) {
            return;
        }

        if (chamadaMidia != null && chamadaMidia.isDisplayable()) {
            chamadaMidia.toFront();
            return;
        }

        ChamadaMidiaView chamada = new ChamadaMidiaView(this, session, nomePar, 0, 0,
                this::notificarEncerramentoChamadaLocal);
        int portaVideo = chamada.getPortaLocalVideo();
        int portaAudio = chamada.getPortaLocalAudio();

        if (portaVideo <= 0 || portaAudio <= 0) {
            chamada.dispose();
            SwingUtils.showError(this, "Chamada", "Não foi possível iniciar a chamada.");
            return;
        }

        chamadaMidia = chamada;
        SwingUtils.exibirCentralizado(chamadaMidia);
        enviarControle(CONTROLE_CHAMADA_MIDIA + portaVideo + ";" + portaAudio);
        adicionarMensagem("Sistema", "Chamada iniciada. Use os botões da janela de chamada para áudio e tela.");
    }

    private boolean processarMensagemControle(String mensagem) {
        if (mensagem == null || !mensagem.startsWith(CONTROLE)) {
            return false;
        }

        if (CONTROLE_CONEXAO_ESTABELECIDA.equals(mensagem)) {
            if (conexaoPar != null && conexaoPar.getEnderecoPar() != null) {
                enderecoArquivoPar = conexaoPar.getEnderecoPar();
            }
            definirStatus("Conectado", SwingUtils.SUCCESS);
            anunciarPortaArquivo();
            return true;
        }

        if (mensagem.startsWith(CONTROLE_ARQUIVO_PORTA)) {
            String valorPorta = mensagem.substring(CONTROLE_ARQUIVO_PORTA.length());
            try {
                portaArquivoPar = Integer.parseInt(valorPorta);
                if (conexaoPar != null && conexaoPar.getEnderecoPar() != null) {
                    enderecoArquivoPar = conexaoPar.getEnderecoPar();
                }
                definirStatus("Online", SwingUtils.SUCCESS);
                anunciarPortaArquivo();
            } catch (NumberFormatException e) {
                definirStatus("Porta de arquivo inválida", SwingUtils.DANGER);
            }
            return true;
        }

        if (mensagem.startsWith(CONTROLE_CHAMADA_MIDIA)) {
            aceitarChamadaMidia(mensagem.substring(CONTROLE_CHAMADA_MIDIA.length()));
            return true;
        }

        if (CONTROLE_CHAMADA_ENCERRADA.equals(mensagem)) {
            encerrarChamadaRemota();
            return true;
        }

        if (CONTROLE_CHAT_ENCERRADO.equals(mensagem) || CONTROLE_CONEXAO_ENCERRADA.equals(mensagem)) {
            encerrarChatRemoto();
            return true;
        }

        return true;
    }

    private void aceitarChamadaMidia(String dadosPortas) {
        String[] partes = dadosPortas.split(";", -1);

        if (partes.length != 2) {
            definirStatus("Convite de chamada inválido", SwingUtils.DANGER);
            return;
        }

        try {
            int portaVideo = Integer.parseInt(partes[0]);
            int portaAudio = Integer.parseInt(partes[1]);
            String enderecoPar = resolverEnderecoPar();

            if (chamadaMidia != null && chamadaMidia.isDisplayable()) {
                chamadaMidia.dispose();
            }

            chamadaMidia = new ChamadaMidiaView(this, session, nomePar, enderecoPar, portaVideo, portaAudio,
                    this::notificarEncerramentoChamadaLocal);
            SwingUtils.exibirCentralizado(chamadaMidia);
            adicionarMensagem("Sistema", "Chamada recebida. Use os botões da janela de chamada para áudio e tela.");
        } catch (NumberFormatException e) {
            definirStatus("Portas de chamada inválidas", SwingUtils.DANGER);
        }
    }

    private void iniciarReceptorArquivo() {
        File diretorioDestino = new File(System.getProperty("user.home"), "Downloads/MonitoriaDistribuida");

        if (!diretorioDestino.exists() && !diretorioDestino.mkdirs()) {
            adicionarMensagem("Sistema", "Não foi possível criar diretório de arquivos recebidos.");
            return;
        }

        try {
            portaArquivoLocal = servicoTransferenciaArquivo.iniciarReceptor(0, diretorioDestino);
        } catch (IOException | IllegalArgumentException e) {
            portaArquivoLocal = -1;
            adicionarMensagem("Sistema", "Não foi possível iniciar receptor de arquivos: " + e.getMessage());
        }
    }

    private void anunciarPortaArquivo() {
        if (portaArquivoAnunciada || portaArquivoLocal <= 0 || conexaoPar == null || !conexaoPar.estaConectado()) {
            return;
        }

        enviarControle(CONTROLE_ARQUIVO_PORTA + portaArquivoLocal);
        portaArquivoAnunciada = true;
    }

    private void enviarControle(String mensagemControle) {
        try {
            if (conexaoPar != null && conexaoPar.estaConectado()) {
                conexaoPar.enviarMensagem(mensagemControle);
            }
        } catch (IOException e) {
            definirStatus("Falha no controle P2P", SwingUtils.DANGER);
        }
    }

    private boolean validarConexaoAtiva() {
        if (conexaoPar == null || !conexaoPar.estaConectado()) {
            SwingUtils.showWarning(this, "Chat", "A conexão ainda não está ativa.");
            return false;
        }

        return true;
    }

    private String resolverEnderecoPar() {
        if (enderecoArquivoPar != null && !enderecoArquivoPar.trim().isEmpty()) {
            return enderecoArquivoPar;
        }

        if (conexaoPar != null && conexaoPar.getEnderecoPar() != null) {
            return conexaoPar.getEnderecoPar();
        }

        return "127.0.0.1";
    }

    private void adicionarMensagem(String autor, String mensagem) {
        areaMensagens.append(autor + ": " + mensagem + "\n");
        areaMensagens.setCaretPosition(areaMensagens.getDocument().getLength());
    }

    private void registrarArquivoTrocado(String direcao, File arquivo) {
        if (arquivo == null) {
            return;
        }

        arquivosModel.add(0, new ArquivoTransferido(direcao, arquivo));
        if (!arquivosModel.isEmpty()) {
            listaArquivos.setSelectedIndex(0);
        }
    }

    private void abrirArquivoSelecionado() {
        ArquivoTransferido arquivo = listaArquivos.getSelectedValue();
        if (arquivo == null) {
            SwingUtils.showWarning(this, "Arquivo", "Escolha um arquivo da lista para abrir.");
            return;
        }

        File destino = arquivo.getArquivo();
        if (!destino.exists()) {
            SwingUtils.showWarning(this, "Arquivo", "Esse arquivo não está mais disponível no computador.");
            return;
        }

        if (!Desktop.isDesktopSupported()) {
            SwingUtils.showWarning(this, "Arquivo", "Não foi possível abrir arquivos automaticamente neste sistema.");
            return;
        }

        try {
            Desktop.getDesktop().open(destino);
        } catch (IOException | IllegalArgumentException e) {
            SwingUtils.showError(
                    this,
                    "Arquivo",
                    "Não foi possível abrir o arquivo.\n\nDetalhes: " + e.getMessage());
        }
    }

    private void definirStatus(String texto, Color cor) {
        rotuloStatus.setText(texto);
        rotuloStatus.setForeground(Color.WHITE);
        rotuloStatus.setBackground(cor);
        rotuloStatus.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
    }

    private void encerrarChat() {
        encerrarChat(true);
    }

    private void encerrarChat(boolean notificarPar) {
        if (chatEncerrado) {
            return;
        }

        chatEncerrado = true;

        fecharChamadaMidiaNaEdt();
        setVisible(false);
        super.dispose();

        if (telaAnterior != null) {
            SwingUtils.exibirCentralizado(telaAnterior);
            notificarTelaAnteriorChatEncerrado();
        }

        fecharRecursosChatEmSegundoPlano(notificarPar ? CONTROLE_CHAT_ENCERRADO : null);
    }

    private void encerrarChatRemoto() {
        if (chatEncerrado) {
            return;
        }

        chatEncerrado = true;
        setVisible(false);
        super.dispose();

        if (telaAnterior != null) {
            SwingUtils.exibirCentralizado(telaAnterior);
            notificarTelaAnteriorChatEncerrado();
            SwingUtils.showInfo(telaAnterior, "Chat", "Conexão encerrada.");
        }

        fecharChamadaMidiaNaEdt();
        fecharRecursosChatEmSegundoPlano();
    }

    private void notificarTelaAnteriorChatEncerrado() {
        if (telaAnterior instanceof MenuMonitorView) {
            ((MenuMonitorView) telaAnterior).aoChatAtendimentoEncerrado();
        } else if (telaAnterior instanceof MenuAlunoView) {
            ((MenuAlunoView) telaAnterior).aoChatAtendimentoEncerrado();
        }
    }

    private void fecharRecursosChat() {
        fecharTransferenciaArquivo();
        fecharConexaoPar();
    }

    private void fecharRecursosChatEmSegundoPlano() {
        fecharRecursosChatEmSegundoPlano(null);
    }

    private void fecharRecursosChatEmSegundoPlano(String mensagemControleAntesDeFechar) {
        if (recursosFechando) {
            return;
        }

        recursosFechando = true;

        Thread thread = new Thread(() -> {
            if (mensagemControleAntesDeFechar != null) {
                enviarControleSemAtualizarTela(mensagemControleAntesDeFechar);
            }

            fecharRecursosChat();
        }, "thread-fechamento-chat-p2p");
        thread.setDaemon(true);
        thread.start();
    }

    private void fecharConexaoPar() {
        if (conexaoPar != null) {
            conexaoPar.close();
            conexaoPar = null;
        }
    }

    private void fecharTransferenciaArquivo() {
        servicoTransferenciaArquivo.close();
    }

    private void enviarControleSemAtualizarTela(String mensagemControle) {
        try {
            if (conexaoPar != null && conexaoPar.estaConectado()) {
                conexaoPar.enviarMensagem(mensagemControle);
            }
        } catch (IOException ignored) {
            // Encerramento do chat: se o par já caiu, apenas seguimos fechando localmente.
        }
    }

    private void fecharChamadaMidia() {
        if (chamadaMidia != null) {
            chamadaMidia.dispose();
            chamadaMidia = null;
        }
    }

    private void fecharChamadaMidiaNaEdt() {
        if (SwingUtilities.isEventDispatchThread()) {
            fecharChamadaMidia();
            return;
        }

        SwingUtilities.invokeLater(this::fecharChamadaMidia);
    }

    private void notificarEncerramentoChamadaLocal() {
        enviarControle(CONTROLE_CHAMADA_ENCERRADA);
        chamadaMidia = null;
        adicionarMensagem("Sistema", "Chamada encerrada.");
        definirStatus("Chamada encerrada", SwingUtils.WARNING);
    }

    private void encerrarChamadaRemota() {
        fecharChamadaMidia();
        adicionarMensagem("Sistema", "O par encerrou a chamada.");
        definirStatus("Chamada encerrada", SwingUtils.WARNING);
    }

    private String normalizarNomePar(String valor) {
        if (valor == null || valor.trim().isEmpty()) {
            return "Par";
        }

        return valor.trim();
    }

    private String normalizarDisciplina(String valor) {
        if (valor == null || valor.trim().isEmpty()) {
            return "Monitoria";
        }

        return valor.trim();
    }

    private String tituloMonitoria() {
        if ("Monitoria".equalsIgnoreCase(disciplina)) {
            return "Monitoria";
        }

        return "Monitoria de " + disciplina;
    }

    private String rotuloPessoaPar() {
        if (session.getTipoUsuario() == TipoUsuario.ALUNO) {
            return "Monitor";
        }

        return "Aluno";
    }

    private static final class ArquivoTransferido {
        private final String direcao;
        private final File arquivo;

        private ArquivoTransferido(String direcao, File arquivo) {
            this.direcao = direcao;
            this.arquivo = arquivo;
        }

        private File getArquivo() {
            return arquivo;
        }

        @Override
        public String toString() {
            return direcao + " - " + arquivo.getName();
        }
    }

    private static final class ArquivoTransferidoRenderer extends JPanel
            implements javax.swing.ListCellRenderer<ArquivoTransferido> {

        private final JLabel nomeLabel = new JLabel();
        private final JLabel caminhoLabel = new JLabel();

        private ArquivoTransferidoRenderer() {
            setLayout(new GridLayout(2, 1, 0, 2));
            setBorder(new EmptyBorder(6, 8, 6, 8));
            nomeLabel.setFont(nomeLabel.getFont().deriveFont(Font.BOLD, 13f));
            caminhoLabel.setFont(caminhoLabel.getFont().deriveFont(Font.PLAIN, 11f));
            add(nomeLabel);
            add(caminhoLabel);
        }

        @Override
        public Component getListCellRendererComponent(JList<? extends ArquivoTransferido> list,
                                                      ArquivoTransferido value,
                                                      int index,
                                                      boolean isSelected,
                                                      boolean cellHasFocus) {
            if (value == null) {
                nomeLabel.setText("");
                caminhoLabel.setText("");
            } else {
                nomeLabel.setText(value.toString());
                String pasta = value.getArquivo().getParent();
                caminhoLabel.setText(pasta == null ? "" : pasta);
            }

            Color background = isSelected ? new Color(219, 234, 254) : new Color(248, 250, 252);
            setBackground(background);
            nomeLabel.setForeground(SwingUtils.TEXT);
            caminhoLabel.setForeground(SwingUtils.MUTED);
            return this;
        }
    }
}
