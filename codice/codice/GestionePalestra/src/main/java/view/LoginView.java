package view;

import javax.swing.*;
import java.awt.*;
import controller.LoginController;

public class LoginView extends JFrame {

    private JTextField txtUsername;
    private JPasswordField txtPassword;
    private JButton btnLogin;
    private JButton btnRegistrati;

    private LoginController controller;

    public LoginView() {
        initUI();
    }

    // Il controller verrà impostato dal main
    public void setController(LoginController controller) {
        this.controller = controller;
    }

    private void initUI() {
        setTitle("Gestione Palestra - Login");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(400, 250);
        setLocationRelativeTo(null); // centra la finestra

        // Layout semplice
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel lblTitolo = new JLabel("Benvenuto in Gestione Palestra", SwingConstants.CENTER);
        lblTitolo.setFont(new Font("Arial", Font.BOLD, 16));

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        panel.add(lblTitolo, gbc);

        gbc.gridwidth = 1;

        // Username
        gbc.gridx = 0;
        gbc.gridy = 1;
        panel.add(new JLabel("Username:"), gbc);

        gbc.gridx = 1;
        txtUsername = new JTextField(15);
        panel.add(txtUsername, gbc);

        // Password
        gbc.gridx = 0;
        gbc.gridy = 2;
        panel.add(new JLabel("Password:"), gbc);

        gbc.gridx = 1;
        txtPassword = new JPasswordField(15);
        panel.add(txtPassword, gbc);

        // Bottoni
        gbc.gridx = 0;
        gbc.gridy = 3;
        btnLogin = new JButton("Accedi");
        panel.add(btnLogin, gbc);

        gbc.gridx = 1;
        btnRegistrati = new JButton("Registrati");
        panel.add(btnRegistrati, gbc);

        // Listener dei bottoni
        btnLogin.addActionListener(e -> onLoginClicked());
        btnRegistrati.addActionListener(e -> onRegistratiClicked());

        setContentPane(panel);
    }

    private void onLoginClicked() {
        if (controller == null) {
            JOptionPane.showMessageDialog(this, "Controller non impostato!", "Errore", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String username = txtUsername.getText().trim();
        String password = new String(txtPassword.getPassword());

        controller.handleLogin(username, password);
    }

    private void onRegistratiClicked() {
        if (controller == null) {
            JOptionPane.showMessageDialog(this, "Controller non impostato!", "Errore", JOptionPane.ERROR_MESSAGE);
            return;
        }

        controller.handleRegistrazione();
    }

    // Metodi di comodo per mostrare messaggi dal controller
    public void mostraMessaggioInfo(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Info", JOptionPane.INFORMATION_MESSAGE);
    }

    public void mostraMessaggioErrore(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Errore", JOptionPane.ERROR_MESSAGE);
    }
}
