package controller;

import db.GestioneDB;
import db.dao.AbbonamentoDAO;
import model.Abbonamento;
import model.Cliente;
import view.HomeView;
import view.LoginView;
import view.RegistrazioneView;
import view.SelezionaAbbonamentoView;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.*;

public class LoginController {

    private static final Logger logger =
            LogManager.getLogger(LoginController.class);

    private final LoginView view;

    public LoginController(LoginView view) {
        this.view = view;
        this.view.setController(this);
    }

    // Gestisce il click su "Accedi"
    public void handleLogin(String username, String password) {
        logger.info("Tentativo di login per username: {}", username);

        if (username.isEmpty() || password.isEmpty()) {
            view.mostraMessaggioErrore("Inserire sia username che password.");
            return;
        }

        String sql = "SELECT * FROM CLIENTE WHERE USERNAME = ? AND PASSWORD = ?";

        try (Connection conn = GestioneDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, username);
            ps.setString(2, password);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    // Costruisco l'oggetto Cliente a partire dal DB
                    Cliente cliente = new Cliente();
                    cliente.setIdCliente(rs.getInt("ID_CLIENTE"));
                    cliente.setUsername(rs.getString("USERNAME"));
                    cliente.setNome(rs.getString("NOME"));
                    cliente.setCognome(rs.getString("COGNOME"));
                    cliente.setCF(rs.getString("CF"));
                    cliente.setLuogoNascita(rs.getString("LUOGO_NASCITA"));
                    cliente.setPassword(rs.getString("PASSWORD"));
                    cliente.setIban(rs.getString("IBAN"));
                    Date dataNascita = rs.getDate("DATA_NASCITA");
                    if (dataNascita != null) {
                        cliente.setDataNascita(new java.util.Date(dataNascita.getTime()));
                    }

                    logger.info("Login riuscito per {}", username);

                    // Verifico se esiste già un abbonamento su DB
                    Abbonamento abb = AbbonamentoDAO.getAbbonamentoByClienteId(cliente.getIdCliente());

                    if (abb != null) {
                        // Il cliente ha già un abbonamento → saltiamo la scelta e andiamo direttamente alla Home
                        cliente.setAbbonamento(abb);
                        view.mostraMessaggioInfo("Benvenuto, accesso effettuato. Abbonamento attivo trovato.");
                        view.dispose();

                        HomeView hView = new HomeView(cliente);
                        HomeController hController = new HomeController(hView, cliente);
                        hView.setVisible(true);

                    } else {
                        // Nessun abbonamento → mandiamo alla schermata di selezione abbonamento
                        view.mostraMessaggioInfo("Accesso effettuato. Nessun abbonamento attivo, selezionane uno.");
                        view.dispose();

                        SelezionaAbbonamentoView sView = new SelezionaAbbonamentoView(cliente);
                        SelezionaAbbonamentoController sController =
                                new SelezionaAbbonamentoController(sView, cliente);
                        sView.setVisible(true);
                    }

                } else {
                    view.mostraMessaggioErrore("Credenziali non valide.");
                    logger.warn("Login fallito per {}", username);
                }
            }

        } catch (SQLException e) {
            logger.error("Errore durante il login di {}", username, e);
            view.mostraMessaggioErrore("Errore di database durante il login.");
        }
    }

 // Gestisce il click su "Registrati"
    public void handleRegistrazione() {
        logger.info("Apertura finestra di registrazione utente");

        // chiudo la schermata di login
        view.dispose();

        // apro la schermata di registrazione
        RegistrazioneView regView = new RegistrazioneView();
        new RegistrazioneController(regView); // collega controller e view
        regView.setVisible(true);
    }

}
