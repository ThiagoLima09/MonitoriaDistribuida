package br.com.monitoriadistribuida.client;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import br.com.monitoriadistribuida.server.model.Disciplina;
import br.com.monitoriadistribuida.server.model.StatusMonitor;

public class MenuMonitorView extends JFrame {

    private final JFrame loginFrame;
    private final SessionContext session;
    private final MonitorController controller = new MonitorController();
    private final JComboBox<Disciplina> disciplinaCombo = new JComboBox<>();
    private final JLabel statusAtualLabel = new JLabel();
    private StatusMonitor statusAtual = StatusMonitor.OFFLINE;
    private ChatView chatAtendimento;

    public MenuMonitorView(JFrame loginFrame, SessionContext session) {
        this.loginFrame = loginFrame;
        this.session = session;
        buildView();
    }

    private void buildView() {
        SwingUtils.configureFrame(this, "Monitoria Distribuida - Monitor", 1060, 700);
        setContentPane(SwingUtils.createRootPanel());
        preencherDisciplinas();
        SwingUtils.styleComboBox(disciplinaCombo);

        JPanel header = createHeader();
        JPanel actions = createActionCard();

        JPanel content = new JPanel(new BorderLayout(0, 24));
        content.setOpaque(false);
        content.add(header, BorderLayout.NORTH);
        content.add(actions, BorderLayout.CENTER);

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
            label.setBackground(isSelected ? new Color(254, 243, 199) : Color.WHITE);
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

        JLabel title = SwingUtils.createTitle("Menu do monitor");
        JLabel subtitle = new JLabel("Bem-vindo, " + session.getNome() + ". Atualize seu status e disciplina.");
        subtitle.setForeground(SwingUtils.MUTED);
        subtitle.setFont(subtitle.getFont().deriveFont(15f));

        left.add(title);
        left.add(Box.createVerticalStrut(4));
        left.add(subtitle);

        JLabel badge = new JLabel("Sessão: MONITOR");
        badge.setOpaque(true);
        badge.setBackground(new Color(254, 243, 199));
        badge.setForeground(new Color(146, 64, 14));
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
        card.setLayout(new GridBagLayout());

        JPanel inner = new JPanel(new GridBagLayout());
        inner.setOpaque(false);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(0, 0, 12, 0);

        JLabel section = SwingUtils.createSectionLabel("Ações do monitor");
        section.setFont(section.getFont().deriveFont(Font.BOLD, 20f));

        JButton disponivelButton = SwingUtils.createPrimaryButton("Ficar disponível");
        JButton offlineButton = SwingUtils.createGhostButton("Ficar offline");

        disponivelButton.addActionListener(e -> atualizarStatus(StatusMonitor.DISPONIVEL));
        offlineButton.addActionListener(e -> atualizarStatus(StatusMonitor.OFFLINE));

        gbc.gridy = 0;
        inner.add(section, gbc);
        gbc.gridy++;
        inner.add(labeledField("Disciplina", disciplinaCombo), gbc);
        gbc.gridy++;
        inner.add(createStatusPanel(), gbc);
        gbc.gridy++;
        inner.add(disponivelButton, gbc);
        gbc.gridy++;
        inner.add(offlineButton, gbc);
        gbc.gridy++;
        gbc.weighty = 1.0;
        inner.add(Box.createVerticalStrut(1), gbc);

        card.add(inner, new GridBagConstraints());
        return card;
    }

    private JPanel createStatusPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 6));
        panel.setOpaque(false);

        JLabel label = new JLabel("Status atual");
        label.setForeground(SwingUtils.TEXT);
        label.setFont(label.getFont().deriveFont(Font.BOLD, 13f));

        statusAtualLabel.setOpaque(true);
        statusAtualLabel.setHorizontalAlignment(JLabel.CENTER);
        statusAtualLabel.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));
        statusAtualLabel.setPreferredSize(new Dimension(300, 40));

        panel.add(label, BorderLayout.NORTH);
        panel.add(statusAtualLabel, BorderLayout.CENTER);
        atualizarPillStatus();
        return panel;
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

    private void atualizarStatus(StatusMonitor status) {
        Disciplina disciplina = (Disciplina) disciplinaCombo.getSelectedItem();
        if (disciplina == null) {
            SwingUtils.showWarning(this, "Status", "Escolha uma disciplina antes de ficar disponível.");
            return;
        }

        ChatView novoChat = null;
        int portaChat = 0;

        if (status == StatusMonitor.DISPONIVEL) {
            novoChat = abrirChatDeEspera(disciplina);
            if (novoChat == null) {
                return;
            }
            portaChat = novoChat.getPortaLocal();
        }

        ChatView chatParaAtivar = novoChat;
        int portaFinal = portaChat;

        executarEmSegundoPlano("thread-atualizar-status-monitor", () -> {
            controller.atualizarStatus(
                    session.getConexaoServidor(),
                    session.getLogin(),
                    status,
                    disciplina,
                    portaFinal);
            SwingUtilities.invokeLater(() -> concluirAtualizacaoStatus(status, chatParaAtivar));
        }, chatParaAtivar);
    }

    private ChatView abrirChatDeEspera(Disciplina disciplina) {
        ChatView chat = new ChatView(this, session, "Aluno", 0, disciplina.getNomeExibicao());

        if (chat.getPortaLocal() <= 0) {
            chat.dispose();
            SwingUtils.showError(this, "Chat", "Não foi possível preparar o chat para atendimento.");
            return null;
        }

        return chat;
    }

    private void concluirAtualizacaoStatus(StatusMonitor status, ChatView novoChat) {
        if (status != StatusMonitor.DISPONIVEL) {
            fecharChatAtendimento();
        } else {
            fecharChatAtendimento();
            chatAtendimento = novoChat;
            setVisible(false);
            SwingUtils.exibirCentralizado(chatAtendimento);
        }

        statusAtual = status;
        atualizarPillStatus();
        SwingUtils.showInfo(this, "Status", "Status atualizado.");
    }

    private void executarEmSegundoPlano(String nomeThread, AcaoServidor acaoServidor, ChatView chatParaFecharEmErro) {
        Thread thread = new Thread(() -> {
            try {
                acaoServidor.executar();
            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> {
                    if (chatParaFecharEmErro != null) {
                        chatParaFecharEmErro.dispose();
                    }
                    SwingUtils.showError(
                            this,
                            "Servidor",
                            "Não foi possível atualizar seu status.\n\nDetalhes: " + ex.getMessage());
                });
            }
        }, nomeThread);

        thread.setDaemon(true);
        thread.start();
    }

    private void atualizarPillStatus() {
        statusAtualLabel.setText(statusAtual.name());
        switch (statusAtual) {
            case DISPONIVEL:
                statusAtualLabel.setBackground(new Color(220, 252, 231));
                statusAtualLabel.setForeground(new Color(22, 101, 52));
                break;
            case OCUPADO:
                statusAtualLabel.setBackground(new Color(254, 243, 199));
                statusAtualLabel.setForeground(new Color(146, 64, 14));
                break;
            case OFFLINE:
            default:
                statusAtualLabel.setBackground(new Color(254, 226, 226));
                statusAtualLabel.setForeground(new Color(153, 27, 27));
                break;
        }
    }

    public void aoChatAtendimentoEncerrado() {
        chatAtendimento = null;
        statusAtual = StatusMonitor.OFFLINE;
        atualizarPillStatus();
        atualizarStatusOfflineServidor();
    }

    private void atualizarStatusOfflineServidor() {
        Disciplina disciplina = (Disciplina) disciplinaCombo.getSelectedItem();
        if (disciplina == null || session.getConexaoServidor() == null) {
            return;
        }

        executarEmSegundoPlano("thread-status-offline-chat-encerrado", () -> controller.atualizarStatus(
                session.getConexaoServidor(),
                session.getLogin(),
                StatusMonitor.OFFLINE,
                disciplina,
                0), null);
    }

    private void sairConta() {
        fecharChatAtendimento();
        session.fecharConexaoServidor();
        dispose();
        if (loginFrame != null) {
            SwingUtils.exibirCentralizado(loginFrame);
        }
    }

    private void fecharChatAtendimento() {
        if (chatAtendimento != null) {
            chatAtendimento.dispose();
            chatAtendimento = null;
        }
    }

    private interface AcaoServidor {
        void executar() throws Exception;
    }
}
