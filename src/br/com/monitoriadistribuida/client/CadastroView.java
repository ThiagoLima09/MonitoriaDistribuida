package br.com.monitoriadistribuida.client;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
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
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

import br.com.monitoriadistribuida.server.model.TipoUsuario;

public class CadastroView extends JFrame {

    private final JFrame loginFrame;
    private final CadastroController controller = new CadastroController();
    private final JTextField nomeField = SwingUtils.createTextField("Nome completo");
    private final JTextField emailField = SwingUtils.createTextField("E-mail");
    private final JPasswordField senhaField = SwingUtils.createPasswordField("Senha");
    private final JPasswordField confirmacaoField = SwingUtils.createPasswordField("Confirmação de senha");
    private final JComboBox<TipoUsuario> tipoCombo = new JComboBox<>(TipoUsuario.values());

    public CadastroView(JFrame loginFrame) {
        this.loginFrame = loginFrame;
        buildView();
    }

    private void buildView() {
        SwingUtils.configureFrame(this, "Monitoria Distribuida - Cadastro", 960, 640);
        setContentPane(SwingUtils.createRootPanel());

        JPanel hero = createHeroPanel();
        JPanel card = createCard();

        JPanel content = new JPanel(new BorderLayout(24, 24));
        content.setOpaque(false);
        content.add(hero, BorderLayout.WEST);
        content.add(card, BorderLayout.CENTER);

        getContentPane().add(content, BorderLayout.CENTER);
    }

    private JPanel createHeroPanel() {
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(42, 24, 42, 24));
        panel.setPreferredSize(new Dimension(380, 0));

        JLabel badge = new JLabel("Novo usuário", SwingConstants.LEFT);
        badge.setOpaque(true);
        badge.setBackground(new Color(220, 252, 231));
        badge.setForeground(new Color(22, 101, 52));
        badge.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
        badge.setAlignmentX(LEFT_ALIGNMENT);

        JLabel title = SwingUtils.createTitle("Criar conta");
        title.setAlignmentX(LEFT_ALIGNMENT);

        JLabel subtitle = new JLabel(
                "<html><div style='width:290px'>Cadastro simples com nome, e-mail, senha e tipo de usuário. Sem campos extras, porque o servidor central não pede mais do que isso.</div></html>");
        subtitle.setForeground(SwingUtils.MUTED);
        subtitle.setFont(subtitle.getFont().deriveFont(16f));
        subtitle.setAlignmentX(LEFT_ALIGNMENT);

        panel.add(badge);
        panel.add(Box.createVerticalStrut(22));
        panel.add(title);
        panel.add(Box.createVerticalStrut(12));
        panel.add(subtitle);
        panel.add(Box.createVerticalStrut(28));
        panel.add(createFeatureLabel("Nome completo"));
        panel.add(createFeatureLabel("E-mail e senha"));
        panel.add(createFeatureLabel("Aluno ou monitor"));
        panel.add(Box.createVerticalGlue());

        return panel;
    }

    private JLabel createFeatureLabel(String text) {
        JLabel label = new JLabel("• " + text);
        label.setForeground(SwingUtils.TEXT);
        label.setFont(label.getFont().deriveFont(Font.PLAIN, 15f));
        label.setAlignmentX(LEFT_ALIGNMENT);
        label.setBorder(BorderFactory.createEmptyBorder(8, 0, 0, 0));
        return label;
    }

    private JPanel createCard() {
        JPanel card = SwingUtils.createCardPanel();
        card.setLayout(new GridBagLayout());
        SwingUtils.styleComboBox(tipoCombo);
        JPanel inner = new JPanel(new GridBagLayout());
        inner.setOpaque(false);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(0, 0, 12, 0);

        JLabel title = SwingUtils.createSectionLabel("Cadastro");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 20f));

        JLabel helper = new JLabel("Os dados abaixo correspondem ao que o servidor central cadastra hoje.");
        helper.setForeground(SwingUtils.MUTED);

        JButton cadastrarButton = SwingUtils.createPrimaryButton("Cadastrar");
        JButton voltarButton = SwingUtils.createGhostButton("Voltar ao login");

        gbc.gridy = 0;
        inner.add(title, gbc);
        gbc.gridy++;
        inner.add(helper, gbc);
        gbc.gridy++;
        inner.add(labeledField("Nome completo", nomeField), gbc);
        gbc.gridy++;
        inner.add(labeledField("E-mail", emailField), gbc);
        gbc.gridy++;
        inner.add(labeledField("Senha", senhaField), gbc);
        gbc.gridy++;
        inner.add(labeledField("Confirmação de senha", confirmacaoField), gbc);
        gbc.gridy++;
        inner.add(labeledField("Tipo de usuário", tipoCombo), gbc);
        gbc.gridy++;
        inner.add(cadastrarButton, gbc);
        gbc.gridy++;
        inner.add(voltarButton, gbc);
        gbc.gridy++;
        gbc.weighty = 1.0;
        inner.add(Box.createVerticalStrut(1), gbc);

        card.add(inner, new GridBagConstraints());

        cadastrarButton.addActionListener(e -> handleCadastro());
        voltarButton.addActionListener(e -> voltarLogin());
        return card;
    }

    private JPanel labeledField(String labelText, java.awt.Component field) {
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        JLabel label = new JLabel(labelText);
        label.setForeground(SwingUtils.TEXT);
        label.setFont(label.getFont().deriveFont(Font.BOLD, 13f));
        field.setPreferredSize(new Dimension(320, 40));
        panel.add(label);
        panel.add(Box.createVerticalStrut(6));
        panel.add(field);
        return panel;
    }

    private void handleCadastro() {
        String nome = nomeField.getText();
        String email = emailField.getText();
        String senha = new String(senhaField.getPassword());
        String confirmacao = new String(confirmacaoField.getPassword());
        TipoUsuario tipoUsuario = (TipoUsuario) tipoCombo.getSelectedItem();

        Thread threadCadastro = new Thread(() -> {
            try {
                controller.cadastrar(nome, email, senha, confirmacao, tipoUsuario);
                SwingUtilities.invokeLater(() -> {
                    SwingUtils.showInfo(this, "Cadastro", "Cadastro realizado com sucesso.");
                    voltarLogin();
                });
            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> SwingUtils.showError(this, "Cadastro", ex.getMessage()));
            }
        }, "thread-cadastro-servidor");

        threadCadastro.setDaemon(true);
        threadCadastro.start();
    }

    private void voltarLogin() {
        dispose();
        if (loginFrame != null) {
            SwingUtils.exibirCentralizado(loginFrame);
        }
    }
}
