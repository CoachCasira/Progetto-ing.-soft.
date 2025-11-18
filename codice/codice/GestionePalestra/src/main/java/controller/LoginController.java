package controller;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import view.LoginView;

public class LoginController {

    private static final Logger logger = LogManager.getLogger(LoginController.class);

    private final LoginView view;

    public LoginController(LoginView view) {
        this.view = view;
        this.view.setController(this);
    }

    // Gestisce il click su "Accedi"
    public void handleLogin(String username, String password) {
        logger.info("Tentativo di login per username: {}", username);

        // Per ora logica finta, giusto per provare la GUI:
        if (username.isEmpty() || password.isEmpty()) {
            view.mostraMessaggioErrore("Inserire sia username che password.");
            return;
        }

        // TODO: qui in futuro chiameremo ClienteDAO.login(...)
        if (username.equals("admin") && password.equals("admin")) {
            view.mostraMessaggioInfo("Login effettuato con successo!");
            logger.info("Login riuscito per {}", username);
            // qui poi apriremo la schermata principale dell'app
        } else {
            view.mostraMessaggioErrore("Credenziali non valide.");
            logger.warn("Login fallito per {}", username);
        }
    }

    // Gestisce il click su "Registrati"
    public void handleRegistrazione() {
        // Per ora solo un messaggio; poi apriremo una schermata di registrazione
        view.mostraMessaggioInfo("Funzionalità di registrazione non ancora implementata.");
        logger.info("Richiesta di registrazione utente");
    }
}
