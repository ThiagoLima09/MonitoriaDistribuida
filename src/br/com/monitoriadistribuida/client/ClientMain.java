package br.com.monitoriadistribuida.client;

import javax.swing.SwingUtilities;

public class ClientMain {

    public static void main(String[] args) {
        SwingUtils.installModernTheme();
        SwingUtilities.invokeLater(() -> SwingUtils.exibirCentralizado(new LoginView()));
    }
}
