package br.com.monitoriadistribuida.client;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import br.com.monitoriadistribuida.server.model.Disciplina;
import br.com.monitoriadistribuida.server.model.StatusMonitor;

public class MenuMonitorView extends JFrame {

    private final JFrame loginFrame;
    private final SessionContext session;
    private final MonitorController controller = new MonitorController();
    private final JComboBox<Disciplina> disciplinaCombo = new JComboBox<>();
    private final JTextField portaField = SwingUtils.createTextField("Porta do monitor");
    private final JTextArea respostaArea = SwingUtils.createTextArea();
    private final JLabel statusAtualLabel = new JLabel();
    private StatusMonitor statusAtual = StatusMonitor.OFFLINE;

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

        JLabel badge = new JLabel("Sessao: MONITOR");
        badge.setOpaque(true);
        badge.setBackground(new Color(254, 243, 199));
        badge.setForeground(new Color(146, 64, 14));
        badge.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));

        panel.add(left, BorderLayout.WEST);
        panel.add(badge, BorderLayout.EAST);
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

        JLabel section = SwingUtils.createSectionLabel("Atualizacao do monitor");
        section.setFont(section.getFont().deriveFont(Font.BOLD, 20f));

        JButton disponivelButton = SwingUtils.createPrimaryButton("Ficar disponivel");
        JButton ocupadoButton = SwingUtils.createSecondaryButton("Ficar ocupado");
        JButton offlineButton = SwingUtils.createGhostButton("Ficar offline");
        JButton sairButton = SwingUtils.createGhostButton("Sair da conta");

        disponivelButton.addActionListener(e -> atualizarStatus(StatusMonitor.DISPONIVEL));
        ocupadoButton.addActionListener(e -> atualizarStatus(StatusMonitor.OCUPADO));
        offlineButton.addActionListener(e -> atualizarStatus(StatusMonitor.OFFLINE));
        sairButton.addActionListener(e -> sairConta());

        gbc.gridy = 0;
        inner.add(section, gbc);
        gbc.gridy++;
        inner.add(labeledField("Disciplina", disciplinaCombo), gbc);
        gbc.gridy++;
        inner.add(createStatusPanel(), gbc);
        gbc.gridy++;
        inner.add(labeledField("Porta", portaField), gbc);
        gbc.gridy++;
        inner.add(disponivelButton, gbc);
        gbc.gridy++;
        inner.add(ocupadoButton, gbc);
        gbc.gridy++;
        inner.add(offlineButton, gbc);
        gbc.gridy++;
        inner.add(sairButton, gbc);
        gbc.gridy++;
        gbc.weighty = 1.0;
        inner.add(Box.createVerticalStrut(1), gbc);

        card.add(inner, new GridBagConstraints());
        return card;
    }

    private JPanel createStatusPanel() {
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        JLabel label = new JLabel("Status atual");
        label.setForeground(SwingUtils.TEXT);
        label.setFont(label.getFont().deriveFont(Font.BOLD, 13f));

        statusAtualLabel.setOpaque(true);
        statusAtualLabel.setHorizontalAlignment(JLabel.CENTER);
        statusAtualLabel.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));

        panel.add(label);
        panel.add(Box.createVerticalStrut(6));
        panel.add(statusAtualLabel);
        atualizarPillStatus();
        return panel;
    }

    private JPanel createPreviewCard() {
        JPanel card = SwingUtils.createCardPanel();
        card.setLayout(new BorderLayout(0, 16));

        JLabel section = SwingUtils.createSectionLabel("Resultado");
        section.setFont(section.getFont().deriveFont(Font.BOLD, 20f));

        respostaArea.setText("A atualização de status aparece aqui.");
        respostaArea.setRows(18);

        JPanel chips = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        chips.setOpaque(false);
        chips.add(createChip("Status"));
        chips.add(createChip("Disciplina"));
        chips.add(createChip("Porta"));

        card.add(section, BorderLayout.NORTH);
        card.add(new JScrollPane(respostaArea), BorderLayout.CENTER);
        card.add(chips, BorderLayout.SOUTH);
        return card;
    }

    private JLabel createChip(String text) {
        JLabel chip = new JLabel(text);
        chip.setOpaque(true);
        chip.setBackground(new Color(241, 245, 249));
        chip.setForeground(SwingUtils.TEXT);
        chip.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
        return chip;
    }

    private JPanel labeledField(String labelText, java.awt.Component field) {
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        JLabel label = new JLabel(labelText);
        label.setForeground(SwingUtils.TEXT);
        label.setFont(label.getFont().deriveFont(Font.BOLD, 13f));
        field.setPreferredSize(new Dimension(300, 40));
        panel.add(label);
        panel.add(Box.createVerticalStrut(6));
        panel.add(field);
        return panel;
    }

    private void atualizarStatus(StatusMonitor status) {
        Disciplina disciplina = (Disciplina) disciplinaCombo.getSelectedItem();
        if (disciplina == null) {
            SwingUtils.showWarning(this, "Status", "Selecione uma disciplina.");
            return;
        }

        int porta;
        try {
            porta = Integer.parseInt(portaField.getText().trim());
        } catch (NumberFormatException ex) {
            SwingUtils.showWarning(this, "Status", "Informe uma porta valida.");
            return;
        }

        statusAtual = status;
        atualizarPillStatus();
        respostaArea.setText(controller.atualizarStatus(session.getLogin(), status, disciplina, porta));
        SwingUtils.showInfo(this, "Status", "Status preparado para o backend.");
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

    private void sairConta() {
        dispose();
        if (loginFrame != null) {
            loginFrame.setVisible(true);
        }
    }
}
