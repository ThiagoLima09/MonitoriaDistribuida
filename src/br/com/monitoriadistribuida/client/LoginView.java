package br.com.monitoriadistribuida.client;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.RenderingHints;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

public class LoginView extends JFrame {

    private final LoginController controller = new LoginController();
    private final JTextField emailField = SwingUtils.createTextField("E-mail");
    private final JPasswordField senhaField = SwingUtils.createPasswordField("Senha");

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

        JLabel title = SwingUtils.createTitle("Acesse o Sistema");
        title.setAlignmentX(LEFT_ALIGNMENT);

        panel.add(badge);
        panel.add(Box.createVerticalStrut(22));
        panel.add(title);
        panel.add(Box.createVerticalGlue());

        return panel;
    }

    private JPanel createLoginCard() {
        JPanel card = createRoundedCardPanel();
        card.setPreferredSize(new Dimension(360, 0));
        card.setLayout(new GridBagLayout());

        JPanel inner = new JPanel(new GridBagLayout());
        inner.setOpaque(false);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(0, 0, 10, 0);

        JLabel title = SwingUtils.createSectionLabel("Entrar");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 20f));

        JLabel helper = new JLabel("Informe os dados para realizar o login");
        helper.setForeground(SwingUtils.MUTED);

        JButton entrarButton = SwingUtils.createPrimaryButton("Entrar");
        JButton cadastroButton = SwingUtils.createSecondaryButton("Cadastrar");

        gbc.gridy = 0;
        inner.add(title, gbc);

        gbc.gridy++;
        inner.add(helper, gbc);

        gbc.gridy++;
        inner.add(labeledField("E-mail", emailField), gbc);

        gbc.gridy++;
        inner.add(labeledField("Senha", senhaField), gbc);

        gbc.gridy++;
        inner.add(entrarButton, gbc);

        gbc.gridy++;
        inner.add(cadastroButton, gbc);

        gbc.gridy++;
        gbc.weighty = 1.0;
        inner.add(Box.createVerticalStrut(1), gbc);

        card.add(inner, new GridBagConstraints());

        entrarButton.addActionListener(e -> handleLogin());
        cadastroButton.addActionListener(e -> openCadastro());
        return card;
    }

    private JPanel createRoundedCardPanel() {
        JPanel panel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(SwingUtils.CARD_BACKGROUND);
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 28, 28);
                g2.dispose();
                super.paintComponent(g);
            }

            @Override
            protected void paintBorder(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(226, 232, 240));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 28, 28);
                g2.dispose();
            }
        };
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));
        return panel;
    }

    private JPanel labeledField(String labelText, java.awt.Component field) {
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel label = new JLabel(labelText);
        label.setForeground(SwingUtils.TEXT);
        label.setFont(label.getFont().deriveFont(Font.BOLD, 13f));
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        field.setPreferredSize(new Dimension(280, 40));
        if (field instanceof JComponent swingField) {
            swingField.setAlignmentX(Component.LEFT_ALIGNMENT);
        }
        panel.add(label);
        panel.add(Box.createVerticalStrut(6));
        panel.add(field);
        return panel;
    }

    private void handleLogin() {
        String email = emailField.getText();
        String senha = new String(senhaField.getPassword());

        Thread threadLogin = new Thread(() -> {
            try {
                SessionContext session = controller.autenticar(email, senha);
                SwingUtilities.invokeLater(() -> abrirMenuUsuario(session));
            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> SwingUtils.showError(
                        this,
                        "Login",
                        "Não foi possível entrar. Verifique seus dados e tente novamente.\n\nDetalhes: "
                                + ex.getMessage()));
            }
        }, "thread-login-servidor");

        threadLogin.setDaemon(true);
        threadLogin.start();
    }

    private void abrirMenuUsuario(SessionContext session) {
        setVisible(false);
        if (session.getTipoUsuario() == br.com.monitoriadistribuida.server.model.TipoUsuario.MONITOR) {
            SwingUtils.exibirCentralizado(new MenuMonitorView(this, session));
        } else {
            SwingUtils.exibirCentralizado(new MenuAlunoView(this, session));
        }
    }

    private void openCadastro() {
        setVisible(false);
        SwingUtils.exibirCentralizado(new CadastroView(this));
    }
}
