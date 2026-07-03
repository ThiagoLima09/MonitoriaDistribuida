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

public class MenuAlunoView extends JFrame {

    private final JFrame loginFrame;
    private final SessionContext session;
    private final AlunoController controller = new AlunoController();
    private final JComboBox<Disciplina> disciplinaCombo = new JComboBox<>();
    private final JTextField emailMonitorField = SwingUtils.createTextField("E-mail do monitor");
    private final JTextArea respostaArea = SwingUtils.createTextArea();

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
        JLabel subtitle = new JLabel("Bem-vindo, " + session.getNome() + ". Aqui voce consulta disciplinas e solicita monitor.");
        subtitle.setForeground(SwingUtils.MUTED);
        subtitle.setFont(subtitle.getFont().deriveFont(15f));

        left.add(title);
        left.add(Box.createVerticalStrut(4));
        left.add(subtitle);

        JLabel badge = new JLabel("Sessao: ALUNO");
        badge.setOpaque(true);
        badge.setBackground(new Color(219, 234, 254));
        badge.setForeground(SwingUtils.PRIMARY_DARK);
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

        JLabel section = SwingUtils.createSectionLabel("Acoes do aluno");
        section.setFont(section.getFont().deriveFont(Font.BOLD, 20f));

        JButton listarDisciplinasButton = SwingUtils.createSecondaryButton("Listar disciplinas");
        JButton buscarMonitoresButton = SwingUtils.createPrimaryButton("Buscar monitores");
        JButton solicitarButton = SwingUtils.createPrimaryButton("Solicitar atendimento");
        JButton sairButton = SwingUtils.createGhostButton("Sair da conta");

        listarDisciplinasButton.addActionListener(e -> mostrarDisciplinas());
        buscarMonitoresButton.addActionListener(e -> buscarMonitores());
        solicitarButton.addActionListener(e -> solicitarAtendimento());
        sairButton.addActionListener(e -> sairConta());

        gbc.gridy = 0;
        inner.add(section, gbc);
        gbc.gridy++;
        inner.add(labeledField("Disciplina", disciplinaCombo), gbc);
        gbc.gridy++;
        inner.add(labeledField("E-mail do monitor", emailMonitorField), gbc);
        gbc.gridy++;
        inner.add(listarDisciplinasButton, gbc);
        gbc.gridy++;
        inner.add(buscarMonitoresButton, gbc);
        gbc.gridy++;
        inner.add(solicitarButton, gbc);
        gbc.gridy++;
        inner.add(sairButton, gbc);
        gbc.gridy++;
        gbc.weighty = 1.0;
        inner.add(Box.createVerticalStrut(1), gbc);

        card.add(inner, new GridBagConstraints());
        return card;
    }

    private JPanel createPreviewCard() {
        JPanel card = SwingUtils.createCardPanel();
        card.setLayout(new BorderLayout(0, 16));

        JLabel section = SwingUtils.createSectionLabel("Resultado");
        section.setFont(section.getFont().deriveFont(Font.BOLD, 20f));

        respostaArea.setText("As respostas dos comandos do backend aparecerão aqui.");
        respostaArea.setRows(18);

        JPanel chips = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        chips.setOpaque(false);
        chips.add(createChip("Disciplinas"));
        chips.add(createChip("Monitores"));
        chips.add(createChip("Atendimento"));

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
        panel.add(label);
        panel.add(Box.createVerticalStrut(6));
        panel.add(field);
        return panel;
    }

    private void mostrarDisciplinas() {
        StringBuilder builder = new StringBuilder("Disciplinas retornadas pelo backend:\n\n");
        for (Disciplina disciplina : controller.listarDisciplinas()) {
            builder.append("• ").append(disciplina.getNomeExibicao()).append('\n');
        }
        respostaArea.setText(builder.toString());
    }

    private void buscarMonitores() {
        Disciplina disciplina = (Disciplina) disciplinaCombo.getSelectedItem();
        if (disciplina == null) {
            SwingUtils.showWarning(this, "Busca", "Selecione uma disciplina.");
            return;
        }
        respostaArea.setText(controller.listarMonitoresPorDisciplina(disciplina));
    }

    private void solicitarAtendimento() {
        String emailMonitor = emailMonitorField.getText().trim();
        if (emailMonitor.isEmpty()) {
            SwingUtils.showWarning(this, "Atendimento", "Informe o e-mail do monitor.");
            return;
        }
        respostaArea.setText(controller.solicitarAtendimento(session.getLogin(), emailMonitor));
        SwingUtils.showInfo(this, "Atendimento", "Solicitacao preparada.");
    }

    private void sairConta() {
        dispose();
        if (loginFrame != null) {
            loginFrame.setVisible(true);
        }
    }
}
