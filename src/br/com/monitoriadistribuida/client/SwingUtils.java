package br.com.monitoriadistribuida.client;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.RenderingHints;
import java.awt.Toolkit;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.plaf.FontUIResource;

public final class SwingUtils {

    public static final Color BACKGROUND_TOP = new Color(245, 247, 252);
    public static final Color BACKGROUND_BOTTOM = new Color(224, 232, 240);
    public static final Color CARD_BACKGROUND = Color.WHITE;
    public static final Color PRIMARY = new Color(37, 99, 235);
    public static final Color PRIMARY_DARK = new Color(29, 78, 216);
    public static final Color SECONDARY = new Color(226, 232, 240);
    public static final Color TEXT = new Color(15, 23, 42);
    public static final Color MUTED = new Color(71, 85, 105);
    public static final Color SUCCESS = new Color(22, 163, 74);
    public static final Color WARNING = new Color(217, 119, 6);
    public static final Color DANGER = new Color(220, 38, 38);

    private SwingUtils() {
    }

    public static void installModernTheme() {
        try {
            UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel");
        } catch (Exception ignored) {
            // Fallback to the default LAF if Nimbus is unavailable.
        }

        Color base = new Color(37, 99, 235);
        Color dark = new Color(29, 78, 216);
        UIManager.put("control", new Color(248, 250, 252));
        UIManager.put("info", new Color(239, 246, 255));
        UIManager.put("nimbusBase", base);
        UIManager.put("nimbusBlueGrey", new Color(100, 116, 139));
        UIManager.put("nimbusLightBackground", Color.WHITE);
        UIManager.put("text", TEXT);
        UIManager.put("textText", TEXT);
        UIManager.put("controlText", TEXT);
        UIManager.put("Button.font", new FontUIResource("SansSerif", Font.BOLD, 14));
        UIManager.put("Label.font", new FontUIResource("SansSerif", Font.PLAIN, 14));
        UIManager.put("TextField.font", new FontUIResource("SansSerif", Font.PLAIN, 14));
        UIManager.put("PasswordField.font", new FontUIResource("SansSerif", Font.PLAIN, 14));
        UIManager.put("ComboBox.font", new FontUIResource("SansSerif", Font.PLAIN, 14));
        UIManager.put("TextArea.font", new FontUIResource("SansSerif", Font.PLAIN, 14));
        UIManager.put("Button.select", dark);
    }

    public static void configureFrame(JFrame frame, String title, int width, int height) {
        frame.setTitle(title);
        frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        frame.setSize(width, height);
        frame.setMinimumSize(new Dimension(width, height));
        frame.setLocationRelativeTo(null);
    }

    public static JPanel createRootPanel() {
        return new GradientPanel();
    }

    public static JPanel createCardPanel() {
        JPanel panel = new JPanel();
        panel.setBackground(CARD_BACKGROUND);
        panel.setBorder(new CompoundCardBorder());
        return panel;
    }

    public static JLabel createTitle(String text) {
        JLabel label = new JLabel(text);
        label.setForeground(TEXT);
        label.setFont(label.getFont().deriveFont(java.awt.Font.BOLD, 24f));
        return label;
    }

    public static JLabel createSubtitle(String text) {
        JLabel label = new JLabel(text);
        label.setForeground(MUTED);
        label.setFont(label.getFont().deriveFont(14f));
        return label;
    }

    public static JLabel createSectionLabel(String text) {
        JLabel label = new JLabel(text);
        label.setForeground(TEXT);
        label.setFont(label.getFont().deriveFont(java.awt.Font.BOLD, 13.5f));
        return label;
    }

    public static JTextField createTextField(String placeholder) {
        JTextField field = new JTextField();
        field.setColumns(20);
        field.setToolTipText(placeholder);
        field.setFont(new Font("SansSerif", Font.PLAIN, 14));
        field.setBackground(Color.WHITE);
        field.setForeground(TEXT);
        field.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(new Color(191, 201, 216), 1, true),
                new EmptyBorder(11, 14, 11, 14)));
        return field;
    }

    public static JPasswordField createPasswordField(String placeholder) {
        JPasswordField field = new JPasswordField();
        field.setColumns(20);
        field.setToolTipText(placeholder);
        field.setFont(new Font("SansSerif", Font.PLAIN, 14));
        field.setBackground(Color.WHITE);
        field.setForeground(TEXT);
        field.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(new Color(191, 201, 216), 1, true),
                new EmptyBorder(11, 14, 11, 14)));
        return field;
    }

    public static void styleComboBox(JComboBox<?> comboBox) {
        comboBox.setFont(new Font("SansSerif", Font.PLAIN, 14));
        comboBox.setBackground(Color.WHITE);
        comboBox.setForeground(TEXT);
        comboBox.setOpaque(true);
        comboBox.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(new Color(191, 201, 216), 1, true),
                new EmptyBorder(6, 10, 6, 10)));
    }

    public static JTextArea createTextArea() {
        JTextArea area = new JTextArea();
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setEditable(false);
        area.setBackground(new Color(248, 250, 252));
        area.setFont(new Font("SansSerif", Font.PLAIN, 14));
        area.setForeground(TEXT);
        area.setBorder(new EmptyBorder(12, 12, 12, 12));
        return area;
    }

    public static JScrollPane createScrollPane(Component component) {
        JScrollPane scrollPane = new JScrollPane(component);
        scrollPane.setBorder(new LineBorder(new Color(226, 232, 240), 1, true));
        scrollPane.getVerticalScrollBar().setUnitIncrement(14);
        return scrollPane;
    }

    public static JButton createPrimaryButton(String text) {
        return createButton(text, PRIMARY, Color.WHITE);
    }

    public static JButton createSecondaryButton(String text) {
        return createButton(text, SECONDARY, TEXT);
    }

    public static JButton createGhostButton(String text) {
        JButton button = createButton(text, new Color(241, 245, 249), TEXT);
        button.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(new Color(203, 213, 225), 1, true),
                new EmptyBorder(10, 14, 10, 14)));
        return button;
    }

    public static void showInfo(Component parent, String title, String message) {
        JOptionPane.showMessageDialog(parent, message, title, JOptionPane.INFORMATION_MESSAGE);
    }

    public static void showWarning(Component parent, String title, String message) {
        JOptionPane.showMessageDialog(parent, message, title, JOptionPane.WARNING_MESSAGE);
    }

    public static void showError(Component parent, String title, String message) {
        JOptionPane.showMessageDialog(parent, message, title, JOptionPane.ERROR_MESSAGE);
    }

    public static void centerOnScreen(JFrame frame) {
        frame.setLocationRelativeTo(null);
    }

    public static void exibirCentralizado(JFrame frame) {
        if (frame == null) {
            return;
        }

        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    public static Dimension screenSize() {
        return Toolkit.getDefaultToolkit().getScreenSize();
    }

    private static JButton createButton(String text, Color background, Color foreground) {
        JButton button = new JButton(text);
        button.setFont(new Font("SansSerif", Font.BOLD, 14));
        button.setFocusPainted(false);
        button.setBackground(background);
        button.setForeground(foreground);
        button.setOpaque(true);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(background.darker(), 1, true),
                new EmptyBorder(11, 18, 11, 18)));
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                button.setBackground(background.darker());
            }

            @Override
            public void mouseExited(MouseEvent e) {
                button.setBackground(background);
            }
        });
        return button;
    }

    private static final class GradientPanel extends JPanel {
        private GradientPanel() {
            setLayout(new java.awt.BorderLayout());
            setBorder(new EmptyBorder(24, 24, 24, 24));
            setBackground(BACKGROUND_TOP);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int width = getWidth();
            int height = getHeight();
            GradientPaint paint = new GradientPaint(0, 0, BACKGROUND_TOP, width, height, BACKGROUND_BOTTOM);
            g2.setPaint(paint);
            g2.fillRect(0, 0, width, height);
            g2.dispose();
        }
    }

    private static final class CompoundCardBorder extends javax.swing.border.CompoundBorder {
        private CompoundCardBorder() {
            super(
                    new LineBorder(new Color(226, 232, 240), 1, true),
                    new EmptyBorder(24, 24, 24, 24));
        }
    }
}
