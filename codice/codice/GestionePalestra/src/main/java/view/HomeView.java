package view;

import controller.HomeController;
import model.Abbonamento;
import model.Cliente;

import javax.swing.*;
import java.awt.*;

public class HomeView extends JFrame {

    private static final long serialVersionUID = 1L;

    private static final Color DARK_BG   = new Color(20, 20, 20);
    private static final Color CARD_BG   = new Color(30, 30, 30);
    private static final Color ORANGE    = new Color(255, 140, 0);
    private static final Color ORANGE_HO = new Color(255, 170, 40);
    private static final Color TEXT_GRAY = new Color(200, 200, 200);

    private final Cliente cliente;
    private HomeController controller;

    private JButton btnVediAbbonamento;
    private JButton btnPrenotaCorso;
    private JButton btnDisdiciAbbonamento;
    private JButton btnLogout;

    public HomeView(Cliente cliente) {
        this.cliente = cliente;
        initUI();
    }

    public void setController(HomeController controller) {
        this.controller = controller;
    }

    private void initUI() {
        setTitle("GestionePalestra - Home");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(700, 450);
        setLocationRelativeTo(null);
        setResizable(false);

        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(DARK_BG);
        setContentPane(mainPanel);

        // --------- HEADER ---------
        JPanel header = new JPanel();
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.setBackground(DARK_BG);
        header.setBorder(BorderFactory.createEmptyBorder(20, 30, 10, 30));

        JLabel lblArea = new JLabel("Area personale");
        lblArea.setFont(new Font("SansSerif", Font.BOLD, 28));
        lblArea.setForeground(ORANGE);
        lblArea.setAlignmentX(Component.LEFT_ALIGNMENT);

        String benvenutoTxt =
                "Benvenuto, " + cliente.getNome() + " " + cliente.getCognome();
        JLabel lblWelcome = new JLabel(benvenutoTxt);
        lblWelcome.setFont(new Font("SansSerif", Font.PLAIN, 18));
        lblWelcome.setForeground(Color.WHITE);
        lblWelcome.setAlignmentX(Component.LEFT_ALIGNMENT);

        header.add(lblArea);
        header.add(Box.createVerticalStrut(5));
        header.add(lblWelcome);

        mainPanel.add(header, BorderLayout.NORTH);

        // --------- CARD CENTRALE ---------
        JPanel card = new JPanel();
        card.setBackground(CARD_BG);
        card.setBorder(BorderFactory.createEmptyBorder(40, 80, 40, 80));
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));

        JLabel lblScelta = new JLabel("Scegli un'operazione:");
        lblScelta.setFont(new Font("SansSerif", Font.PLAIN, 18));
        lblScelta.setForeground(TEXT_GRAY);
        lblScelta.setAlignmentX(Component.CENTER_ALIGNMENT);

        card.add(lblScelta);
        card.add(Box.createVerticalStrut(30));

        btnVediAbbonamento    = creaBottoneArancione("Vedi abbonamento");
        btnPrenotaCorso       = creaBottoneArancione("Prenota corso");
        btnDisdiciAbbonamento = creaBottoneSoloBordo("Disdici abbonamento");
        btnLogout             = creaBottoneSoloBordo("Logout");

        btnVediAbbonamento.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnPrenotaCorso.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnDisdiciAbbonamento.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnLogout.setAlignmentX(Component.CENTER_ALIGNMENT);

        card.add(btnVediAbbonamento);
        card.add(Box.createVerticalStrut(15));
        card.add(btnPrenotaCorso);
        card.add(Box.createVerticalStrut(15));
        card.add(btnDisdiciAbbonamento);
        card.add(Box.createVerticalStrut(25));
        card.add(btnLogout);

        JPanel centerWrapper = new JPanel(new GridBagLayout());
        centerWrapper.setBackground(DARK_BG);
        centerWrapper.add(card);

        mainPanel.add(centerWrapper, BorderLayout.CENTER);

        // --------- FOOTER ---------
        JLabel lblFooter = new JLabel(
                "GestionePalestra – versione demo progetto",
                SwingConstants.CENTER);
        lblFooter.setForeground(new Color(160, 160, 160));
        lblFooter.setFont(new Font("SansSerif", Font.PLAIN, 11));
        lblFooter.setBorder(BorderFactory.createEmptyBorder(8, 5, 8, 5));

        JPanel footerPanel = new JPanel(new BorderLayout());
        footerPanel.setBackground(DARK_BG);
        footerPanel.add(lblFooter, BorderLayout.CENTER);
        mainPanel.add(footerPanel, BorderLayout.SOUTH);

        // ---- visibilità prenota corso in base all'abbonamento ----
        aggiornaVisibilitaPrenotaCorso();

        // --------- LISTENER ---------
        btnVediAbbonamento.addActionListener(e -> {
            if (controller != null) controller.handleVediAbbonamento();
        });

        btnPrenotaCorso.addActionListener(e -> {
            if (controller != null) controller.handlePrenotaCorso();
        });

        btnDisdiciAbbonamento.addActionListener(e -> {
            if (controller != null) controller.handleDisdiciAbbonamento();
        });

        btnLogout.addActionListener(e -> {
            if (controller != null) controller.handleLogout();
        });
    }

    /**
     * Mostra o nasconde il bottone "Prenota corso" a seconda
     * del tipo di abbonamento del cliente.
     * - visibile SOLO se abbonamento != null e tipo == "CORSI"
     */
    private void aggiornaVisibilitaPrenotaCorso() {
        Abbonamento abb = cliente != null ? cliente.getAbbonamento() : null;
        boolean haCorsi = abb != null &&
                abb.getTipo() != null &&
                abb.getTipo().equalsIgnoreCase("CORSI");

        btnPrenotaCorso.setVisible(haCorsi);
    }

    // ========= Factory bottoni (stesso stile del login) =========
    private JButton creaBottoneArancione(String testo) {
        JButton b = new JButton(testo);
        b.setPreferredSize(new Dimension(260, 50));
        b.setMaximumSize(new Dimension(280, 50));
        b.setBackground(ORANGE);
        b.setForeground(Color.BLACK);
        b.setFocusPainted(false);
        b.setBorder(BorderFactory.createEmptyBorder(8, 20, 8, 20));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setOpaque(true);

        b.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent e) {
                b.setBackground(ORANGE_HO);
            }
            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                b.setBackground(ORANGE);
            }
        });
        return b;
    }

    private JButton creaBottoneSoloBordo(String testo) {
        JButton b = new JButton(testo);
        b.setPreferredSize(new Dimension(260, 46));
        b.setMaximumSize(new Dimension(280, 46));
        b.setBackground(DARK_BG);
        b.setForeground(ORANGE);
        b.setFocusPainted(false);
        b.setBorder(BorderFactory.createLineBorder(ORANGE));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setOpaque(true);

        b.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent e) {
                b.setBackground(new Color(40, 40, 40));
            }
            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                b.setBackground(DARK_BG);
            }
        });
        return b;
    }

    // ======================================================
    // Finestra custom per i dettagli dell’abbonamento
    // ======================================================
    public void mostraDettaglioAbbonamento(Abbonamento abb) {
        JDialog dialog = new JDialog(this, "Dettagli abbonamento", true);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        dialog.setSize(500, 320);
        dialog.setLocationRelativeTo(this);
        dialog.setResizable(false);

        JPanel main = new JPanel(new BorderLayout());
        main.setBackground(DARK_BG);
        dialog.setContentPane(main);

        JPanel header = new JPanel();
        header.setBackground(DARK_BG);
        header.setBorder(BorderFactory.createEmptyBorder(15, 20, 10, 20));
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));

        JLabel lblTitle = new JLabel("Dettagli abbonamento");
        lblTitle.setForeground(ORANGE);
        lblTitle.setFont(new Font("SansSerif", Font.BOLD, 20));
        lblTitle.setAlignmentX(Component.LEFT_ALIGNMENT);

        header.add(lblTitle);
        main.add(header, BorderLayout.NORTH);

        JTextArea txt = new JTextArea();
        txt.setEditable(false);
        txt.setLineWrap(true);
        txt.setWrapStyleWord(true);
        txt.setBackground(CARD_BG);
        txt.setForeground(Color.WHITE);
        txt.setFont(new Font("SansSerif", Font.PLAIN, 13));
        txt.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        txt.setText(abb.getDescrizioneCompleta());

        JScrollPane scroll = new JScrollPane(txt);
        scroll.setBorder(BorderFactory.createLineBorder(new Color(70, 70, 70)));
        scroll.getViewport().setBackground(CARD_BG);

        JPanel center = new JPanel(new BorderLayout());
        center.setBackground(DARK_BG);
        center.setBorder(BorderFactory.createEmptyBorder(0, 20, 10, 20));
        center.add(scroll, BorderLayout.CENTER);
        main.add(center, BorderLayout.CENTER);

        JPanel footer = new JPanel();
        footer.setBackground(DARK_BG);
        footer.setBorder(BorderFactory.createEmptyBorder(10, 0, 15, 0));

        JButton btnChiudi = creaBottoneSoloBordo("Chiudi");
        btnChiudi.addActionListener(e -> dialog.dispose());
        footer.add(btnChiudi);

        main.add(footer, BorderLayout.SOUTH);

        dialog.setVisible(true);
    }

    // =================== messaggi info/errore ===================
    public void mostraMessaggioInfo(String msg) {
        ThemedDialog.showMessage(this, "Info", msg, false);
    }

    public void mostraMessaggioErrore(String msg) {
        ThemedDialog.showMessage(this, "Errore", msg, true);
    }

    public Cliente getCliente() {
        return cliente;
    }
}
