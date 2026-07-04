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
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JComboBox;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

import br.com.monitoriadistribuida.server.model.TipoUsuario;

public class LoginView extends JFrame {

    private final LoginController controller = new LoginController();
    private final JTextField emailField = SwingUtils.createTextField("E-mail");
    private final JPasswordField senhaField = SwingUtils.createPasswordField("Senha");
    private final JComboBox<TipoUsuario> tipoCombo = new JComboBox<>(TipoUsuario.values());

    public LoginView() {
        buildView();
    }

    private void buildView() {
        SwingUtils.configureFrame(this, "Monitoria Distribuida - Login", 940, 600);
        setContentPane(SwingUtils.createRootPanel());

        JPanel hero = createHeroPanel();
        JPanel card = createLoginCard();

        JPanel content = new JPanel(new BorderLayout(24, 24));
        content.setOpaque(false);
        content.add(hero, BorderLayout.CENTER);
        content.add(card, BorderLayout.EAST);

        getContentPane().add(content, BorderLayout.CENTER);
    }

    private JPanel createHeroPanel() {
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(42, 24, 42, 24));

        JLabel badge = new JLabel("Monitoria Distribuida", SwingConstants.LEFT);
        badge.setOpaque(true);
        badge.setBackground(new Color(219, 234, 254));
        badge.setForeground(SwingUtils.PRIMARY_DARK);
        badge.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
        badge.setAlignmentX(LEFT_ALIGNMENT);

        JLabel title = SwingUtils.createTitle("Acesse o sistema");
        title.setAlignmentX(LEFT_ALIGNMENT);

        JLabel subtitle = new JLabel(
                "<html><div style='width:330px'>Entre com seu e-mail e senha para acessar as telas já suportadas pelo backend: cadastro, consulta de disciplinas, busca de monitores e atualização de status.</div></html>");
        subtitle.setForeground(SwingUtils.MUTED);
        subtitle.setFont(subtitle.getFont().deriveFont(16f));
        subtitle.setAlignmentX(LEFT_ALIGNMENT);

        panel.add(badge);
        panel.add(Box.createVerticalStrut(22));
        panel.add(title);
        panel.add(Box.createVerticalStrut(12));
        panel.add(subtitle);
        panel.add(Box.createVerticalStrut(28));
        panel.add(createFeatureLabel("Login por e-mail e senha"));
        panel.add(createFeatureLabel("Cadastro de aluno ou monitor"));
        panel.add(createFeatureLabel("Menus alinhados ao backend"));
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

    private JPanel createLoginCard() {
        JPanel card = SwingUtils.createCardPanel();
        card.setPreferredSize(new Dimension(360, 0));
        card.setLayout(new GridBagLayout());
        SwingUtils.styleComboBox(tipoCombo);

        JPanel inner = new JPanel(new GridBagLayout());
        inner.setOpaque(false);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(0, 0, 10, 0);

        JLabel title = SwingUtils.createSectionLabel("Entrar");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 20f));

        JLabel helper = new JLabel("Use o mesmo e-mail cadastrado no backend.");
        helper.setForeground(SwingUtils.MUTED);

        JLabel profileHint = new JLabel("Selecione seu perfil de acesso antes de entrar.");
        profileHint.setForeground(new Color(146, 64, 14));
        profileHint.setOpaque(true);
        profileHint.setBackground(new Color(255, 247, 237));
        profileHint.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));

        JButton entrarButton = SwingUtils.createPrimaryButton("Entrar");
        JButton cadastroButton = SwingUtils.createSecondaryButton("Cadastrar");

        gbc.gridy = 0;
        inner.add(title, gbc);

        gbc.gridy++;
        inner.add(helper, gbc);

        gbc.gridy++;
        inner.add(profileHint, gbc);

        gbc.gridy++;
        inner.add(labeledField("E-mail", emailField), gbc);

        gbc.gridy++;
        inner.add(labeledField("Senha", senhaField), gbc);

        gbc.gridy++;
        inner.add(labeledField("Tipo de usuario", tipoCombo), gbc);

        gbc.gridy++;
        inner.add(entrarButton, gbc);

        gbc.gridy++;
        inner.add(cadastroButton, gbc);

        gbc.gridy++;
        gbc.weighty = 1.0;
        inner.add(Box.createVerticalStrut(1), gbc);

        card.add(inner, new GridBagConstraints());

        tipoCombo.setSelectedIndex(-1);
        entrarButton.addActionListener(e -> handleLogin());
        cadastroButton.addActionListener(e -> openCadastro());
        return card;
    }

    private JPanel labeledField(String labelText, java.awt.Component field) {
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        JLabel label = new JLabel(labelText);
        label.setForeground(SwingUtils.TEXT);
        label.setFont(label.getFont().deriveFont(Font.BOLD, 13f));
        field.setPreferredSize(new Dimension(280, 40));
        panel.add(label);
        panel.add(Box.createVerticalStrut(6));
        panel.add(field);
        return panel;
    }

    private void handleLogin() {
        try {
            SessionContext session = controller.authenticate(
                    emailField.getText(),
                    new String(senhaField.getPassword()),
                    (TipoUsuario) tipoCombo.getSelectedItem());

            setVisible(false);
            if (session.getTipoUsuario() == br.com.monitoriadistribuida.server.model.TipoUsuario.MONITOR) {
                new MenuMonitorView(this, session).setVisible(true);
            } else {
                new MenuAlunoView(this, session).setVisible(true);
            }
        } catch (IllegalArgumentException ex) {
            SwingUtils.showError(this, "Login", ex.getMessage());
        }
    }

    private void openCadastro() {
        setVisible(false);
        new CadastroView(this).setVisible(true);
    }
}
