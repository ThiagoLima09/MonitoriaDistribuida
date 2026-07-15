package br.com.monitoriadistribuida.client.view;

import br.com.monitoriadistribuida.client.SessionContext;
import br.com.monitoriadistribuida.client.controller.AlunoController;
import br.com.monitoriadistribuida.network.InformacoesMonitor;
import br.com.monitoriadistribuida.server.model.Disciplina;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;

import java.util.ArrayList;
import java.util.List;

public class MenuAlunoView extends JFrame {

    private final JFrame loginFrame;
    private final SessionContext session;
    private final AlunoController controller = new AlunoController();
    private final JComboBox<Disciplina> disciplinaCombo = new JComboBox<>();
    private final JTextArea respostaArea = SwingUtils.createTextArea();
    private final JPanel resultadoPanel = new JPanel(new BorderLayout());
    private volatile List<InformacoesMonitor> monitoresExibidos = new ArrayList<>();
    private volatile boolean solicitacaoEmAndamento;

    public MenuAlunoView(JFrame loginFrame, SessionContext session) {
        this.loginFrame = loginFrame;
        this.session = session;
        buildView();
    }

    private void buildView() {
        SwingUtils.configureFrame(this, "Monitoria Distribuida - Aluno", 1060, 700);
        setContentPane(SwingUtils.createRootPanel());

        preencherDisciplinas();
        SwingUtils.styleComboBox(disciplinaCombo);

        JPanel header = createHeader();
        JPanel actions = createActionCard();
        JPanel preview = createPreviewCard();

        JPanel body = new JPanel(new GridLayout(1, 2, 24, 24));
        body.setOpaque(false);
        body.add(actions);
        body.add(preview);

        JPanel content = new JPanel(new BorderLayout(0, 24));
        content.setOpaque(false);
        content.add(header, BorderLayout.NORTH);
        content.add(body, BorderLayout.CENTER);

        getContentPane().add(content, BorderLayout.CENTER);
    }

    private void preencherDisciplinas() {
        disciplinaCombo.removeAllItems();
        for (Disciplina disciplina : controller.listarDisciplinas()) {
            disciplinaCombo.addItem(disciplina);
        }
        disciplinaCombo.setRenderer((list, value, index, isSelected, cellHasFocus) -> {
            JLabel label = new JLabel(value == null ? "" : value.getNomeExibicao());
            label.setOpaque(true);
            if (isSelected) {
                label.setBackground(new Color(219, 234, 254));
            } else {
                label.setBackground(Color.WHITE);
            }
            label.setForeground(SwingUtils.TEXT);
            label.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));
            return label;
        });
    }

    private JPanel createHeader() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);

        JPanel left = new JPanel();
        left.setOpaque(false);
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));

        JLabel title = SwingUtils.createTitle("Menu do aluno");
        JLabel subtitle = new JLabel("Bem-vindo, " + session.getNome() + ". Aqui você consulta disciplinas e solicita monitor.");
        subtitle.setForeground(SwingUtils.MUTED);
        subtitle.setFont(subtitle.getFont().deriveFont(15f));

        left.add(title);
        left.add(Box.createVerticalStrut(4));
        left.add(subtitle);

        JLabel badge = new JLabel("Sessão: ALUNO");
        badge.setOpaque(true);
        badge.setBackground(new Color(219, 234, 254));
        badge.setForeground(SwingUtils.PRIMARY_DARK);
        badge.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));

        JButton encerrarSessaoButton = SwingUtils.createDangerButton("Encerrar Sessão");
        encerrarSessaoButton.setPreferredSize(new Dimension(
                encerrarSessaoButton.getPreferredSize().width,
                badge.getPreferredSize().height));
        encerrarSessaoButton.addActionListener(e -> sairConta());

        JPanel sessionActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        sessionActions.setOpaque(false);
        sessionActions.add(badge);
        sessionActions.add(encerrarSessaoButton);

        panel.add(left, BorderLayout.WEST);
        panel.add(sessionActions, BorderLayout.EAST);
        return panel;
    }

    private JPanel createActionCard() {
        JPanel card = SwingUtils.createCardPanel();
        card.setLayout(new BorderLayout());

        JPanel inner = new JPanel(new GridBagLayout());
        inner.setOpaque(false);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.insets = new Insets(0, 0, 12, 0);

        JLabel section = SwingUtils.createSectionLabel("Ações do aluno");
        section.setFont(section.getFont().deriveFont(Font.BOLD, 20f));

        JButton buscarMonitoresButton = SwingUtils.createPrimaryButton("Buscar monitores");

        buscarMonitoresButton.addActionListener(e -> buscarMonitores());

        gbc.gridy = 0;
        inner.add(section, gbc);
        gbc.gridy++;
        inner.add(labeledField("Disciplina", disciplinaCombo), gbc);
        gbc.gridy++;
        inner.add(buscarMonitoresButton, gbc);

        card.add(inner, BorderLayout.NORTH);
        return card;
    }

    private JPanel createPreviewCard() {
        JPanel card = SwingUtils.createCardPanel();
        card.setLayout(new BorderLayout(0, 16));

        JLabel section = SwingUtils.createSectionLabel("Monitores Disponíveis");
        section.setFont(section.getFont().deriveFont(Font.BOLD, 20f));

        respostaArea.setRows(18);
        resultadoPanel.setBackground(new Color(248, 250, 252));
        mostrarTextoResultado("");

        card.add(section, BorderLayout.NORTH);
        card.add(SwingUtils.createScrollPane(resultadoPanel), BorderLayout.CENTER);
        return card;
    }

    private JPanel labeledField(String labelText, java.awt.Component field) {
        JPanel panel = new JPanel(new BorderLayout(0, 6));
        panel.setOpaque(false);
        JLabel label = new JLabel(labelText);
        label.setForeground(SwingUtils.TEXT);
        label.setFont(label.getFont().deriveFont(Font.BOLD, 13f));
        field.setPreferredSize(new Dimension(300, 40));
        field.setMinimumSize(new Dimension(300, 40));
        panel.add(label, BorderLayout.NORTH);
        panel.add(field, BorderLayout.CENTER);
        return panel;
    }

    private void buscarMonitores() {
        Disciplina disciplina = (Disciplina) disciplinaCombo.getSelectedItem();
        if (disciplina == null) {
            SwingUtils.showWarning(this, "Busca", "Escolha uma disciplina para buscar monitores.");
            return;
        }

        mostrarTextoResultado("Buscando monitores disponíveis...");

        executarEmSegundoPlano("thread-busca-monitores", () -> {
            List<InformacoesMonitor> monitores = controller.listarMonitoresPorDisciplina(
                    session.getConexaoServidor(),
                    disciplina);
            SwingUtilities.invokeLater(() -> mostrarMonitores(monitores));
        });
    }

    private void solicitarAtendimento(InformacoesMonitor monitor) {
        if (monitor == null) {
            return;
        }

        if (solicitacaoEmAndamento) {
            SwingUtils.showWarning(this, "Atendimento", "Já existe uma solicitação em andamento.");
            return;
        }

        solicitacaoEmAndamento = true;
        mostrarTextoResultado("Solicitando atendimento para " + monitor.getNome() + "...");

        Thread thread = new Thread(() -> {
            try {
                InformacoesMonitor atendimento = controller.solicitarAtendimento(
                    session.getConexaoServidor(),
                    session.getLogin(),
                    monitor.getEmail());
                SwingUtilities.invokeLater(() -> {
                    solicitacaoEmAndamento = false;
                    abrirChatComMonitor(atendimento);
                });
            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> {
                    solicitacaoEmAndamento = false;
                    SwingUtils.showError(
                            this,
                            "Atendimento",
                            "Não foi possível iniciar o atendimento.\n\nDetalhes: " + ex.getMessage());
                    mostrarMonitores(monitoresExibidos);
                });
            }
        }, "thread-solicitar-atendimento");

        thread.setDaemon(true);
        thread.start();
    }

    private void mostrarMonitores(List<InformacoesMonitor> monitores) {
        if (monitores == null || monitores.isEmpty()) {
            monitoresExibidos = new ArrayList<>();
            mostrarTextoResultado("Nenhum monitor disponível para a disciplina selecionada.");
            return;
        }

        monitoresExibidos = new ArrayList<>(monitores);

        JPanel lista = new JPanel();
        lista.setOpaque(false);
        lista.setLayout(new BoxLayout(lista, BoxLayout.Y_AXIS));

        for (InformacoesMonitor monitor : monitores) {
            lista.add(criarCardMonitor(monitor));
            lista.add(Box.createVerticalStrut(10));
        }

        resultadoPanel.removeAll();
        resultadoPanel.add(lista, BorderLayout.NORTH);
        atualizarResultado();
    }

    private void abrirChatComMonitor(InformacoesMonitor monitor) {
        if (monitor.getPortaChat() <= 0) {
            SwingUtils.showError(this, "Atendimento", "O chat do monitor ainda não está disponível.");
            return;
        }

        mostrarTextoResultado("Atendimento iniciado com " + monitor.getNome()
                + "\nIP: " + monitor.getIp()
                + "\nPorta chat: " + monitor.getPortaChat());

        setVisible(false);
        SwingUtils.exibirCentralizado(new ChatView(
                this,
                session,
                monitor.getNome(),
                monitor.getDisciplina(),
                monitor.getIp(),
                monitor.getPortaChat()));
    }

    private void executarEmSegundoPlano(String nomeThread, AcaoServidor acaoServidor) {
        Thread thread = new Thread(() -> {
            try {
                acaoServidor.executar();
            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> SwingUtils.showError(
                        this,
                        "Servidor",
                        "Não foi possível se comunicar com o servidor.\n\nDetalhes: " + ex.getMessage()));
            }
        }, nomeThread);

        thread.setDaemon(true);
        thread.start();
    }

    private JPanel criarCardMonitor(InformacoesMonitor monitor) {
        JPanel card = new JPanel(new BorderLayout(14, 0));
        card.setOpaque(true);
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(new Color(203, 213, 225), 1, true),
                new EmptyBorder(14, 16, 14, 16)));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 112));

        JPanel textos = new JPanel();
        textos.setOpaque(false);
        textos.setLayout(new BoxLayout(textos, BoxLayout.Y_AXIS));

        JLabel nomeLabel = new JLabel(monitor.getNome());
        nomeLabel.setForeground(SwingUtils.TEXT);
        nomeLabel.setFont(nomeLabel.getFont().deriveFont(Font.BOLD, 16f));

        JLabel disciplinaLabel = new JLabel(monitor.getDisciplina());
        disciplinaLabel.setForeground(SwingUtils.MUTED);
        disciplinaLabel.setFont(disciplinaLabel.getFont().deriveFont(Font.PLAIN, 13f));

        JLabel emailLabel = new JLabel(monitor.getEmail());
        emailLabel.setForeground(SwingUtils.MUTED);
        emailLabel.setFont(emailLabel.getFont().deriveFont(Font.PLAIN, 13f));

        textos.add(nomeLabel);
        textos.add(Box.createVerticalStrut(4));
        textos.add(disciplinaLabel);
        textos.add(Box.createVerticalStrut(3));
        textos.add(emailLabel);

        JLabel acaoLabel = new JLabel("Solicitar");
        acaoLabel.setOpaque(true);
        acaoLabel.setBackground(new Color(219, 234, 254));
        acaoLabel.setForeground(SwingUtils.PRIMARY_DARK);
        acaoLabel.setFont(acaoLabel.getFont().deriveFont(Font.BOLD, 13f));
        acaoLabel.setBorder(new EmptyBorder(8, 12, 8, 12));

        MouseAdapter cliqueCard = new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                solicitarAtendimento(monitor);
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                card.setBackground(new Color(248, 250, 252));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                card.setBackground(Color.WHITE);
            }
        };

        card.add(textos, BorderLayout.CENTER);
        card.add(acaoLabel, BorderLayout.EAST);
        aplicarCliqueCard(card, cliqueCard);
        return card;
    }

    private void aplicarCliqueCard(Component componente, MouseAdapter cliqueCard) {
        componente.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        componente.addMouseListener(cliqueCard);

        if (componente instanceof Container) {
            Component[] filhos = ((Container) componente).getComponents();

            for (Component filho : filhos) {
                aplicarCliqueCard(filho, cliqueCard);
            }
        }
    }

    private void mostrarTextoResultado(String texto) {
        respostaArea.setText(texto);
        resultadoPanel.removeAll();
        resultadoPanel.add(respostaArea, BorderLayout.CENTER);
        atualizarResultado();
    }

    private void atualizarResultado() {
        resultadoPanel.revalidate();
        resultadoPanel.repaint();
    }

    public void aoChatAtendimentoEncerrado() {
        solicitacaoEmAndamento = false;
        monitoresExibidos = new ArrayList<>();
        mostrarTextoResultado("");
    }

    private void sairConta() {
        session.fecharConexaoServidor();
        dispose();
        if (loginFrame != null) {
            SwingUtils.exibirCentralizado(loginFrame);
        }
    }

    private interface AcaoServidor {
        void executar() throws Exception;
    }
}
