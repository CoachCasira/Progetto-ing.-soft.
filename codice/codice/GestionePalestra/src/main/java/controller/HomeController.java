package controller;

import db.AbbonamentoDAO;
import model.Cliente;
import model.Abbonamento;
import view.HomeView;
import view.LoginView;
import view.SelezionaAbbonamentoView;
import view.ThemedDialog;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;

public class HomeController {

    private static final Logger logger =
            LogManager.getLogger(HomeController.class);

    private final HomeView view;
    private final Cliente cliente;

    public HomeController(HomeView view, Cliente cliente) {
        this.view = view;
        this.cliente = cliente;
        this.view.setController(this);
    }

    public void handleVediAbbonamento() {
        Abbonamento abb = cliente.getAbbonamento();
        if (abb == null) {
            view.mostraMessaggioInfo(
                    "Non hai ancora un abbonamento attivo.\n" +
                    "Vai nella sezione 'Acquista abbonamento' per sottoscriverne uno.");
            return;
        }

        // usa la finestra personalizzata della HomeView
        view.mostraDettaglioAbbonamento(abb);
    }


    /** Simulazione prenotazione corsi */
    public void handlePrenotaCorso() {
        view.mostraMessaggioInfo(
                "Funzionalità prenotazione corsi non ancora implementata.\n" +
                "Nel progetto puoi simulare la prenotazione con un semplice messaggio.");
    }

    /** Disdetta completa dell’abbonamento e reindirizzamento alla scelta abbonamento */
    public void handleDisdiciAbbonamento() {
        Abbonamento abb = cliente.getAbbonamento();
        if (abb == null) {
            view.mostraMessaggioInfo("Non hai alcun abbonamento attivo da disdire.");
            return;
        }

        boolean conferma = ThemedDialog.showConfirm(
                view,
                "Conferma disdetta",
                "Sei sicuro di voler disdire l’abbonamento attivo?"
        );

        if (!conferma) {
            return;
        }

        try {
            AbbonamentoDAO.disdiciAbbonamentoPerUsername(cliente.getUsername());
            cliente.setAbbonamento(null);

            view.mostraMessaggioInfo("Abbonamento disdetto con successo.");
            logger.info("Abbonamento disdetto per utente {}", cliente.getUsername());

            // dopo la disdetta vai direttamente alla scelta abbonamento
            SelezionaAbbonamentoView sView = new SelezionaAbbonamentoView(cliente);
            SelezionaAbbonamentoController sController =
                    new SelezionaAbbonamentoController(sView, cliente);
            sView.setVisible(true);
            view.dispose();

        } catch (Exception e) {
            logger.error("Errore durante la disdetta per utente {}", cliente.getUsername(), e);
            view.mostraMessaggioErrore(
                    "Si è verificato un errore durante la disdetta dell’abbonamento.");
        }
    }


    /** Logout utente */
    public void handleLogout() {
        logger.info("Logout utente {}", cliente.getUsername());
        view.dispose();

        LoginView loginView = new LoginView();
        new LoginController(loginView);
        loginView.setVisible(true);
    }
}
