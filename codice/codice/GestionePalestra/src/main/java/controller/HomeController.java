package controller;

import db.dao.AbbonamentoDAO;
import db.dao.ConsulenzaDAO;
import db.dao.ConsulenzaDAO.ConsulenzaDettaglio;
import model.Abbonamento;
import model.Cliente;
import view.HomeView;
import view.LoginView;
import view.PrenotaConsulenzaView;
import view.SelezionaAbbonamentoView;
import view.ThemedDialog;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

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

    /** Apertura schermata prenotazione consulenza (chiude la Home) */
    public void handlePrenotaConsulenza() {
        view.dispose(); // chiudo l’area personale

        PrenotaConsulenzaView v = new PrenotaConsulenzaView(cliente);
        new PrenotaConsulenzaController(v, cliente);
        v.setVisible(true);
    }

    /** Mostra tutte le consulenze del cliente (future e passate) */
    public void handleVediConsulenza() {
        try {
            List<ConsulenzaDettaglio> lista =
                    ConsulenzaDAO.findByCliente(cliente.getIdCliente());

            if (lista.isEmpty()) {
                view.mostraMessaggioInfo(
                        "Non hai ancora prenotato alcuna consulenza.");
                return;
            }

            LocalDate oggi = LocalDate.now();
            LocalTime adesso = LocalTime.now();

            StringBuilder futureSb = new StringBuilder();
            StringBuilder pastSb   = new StringBuilder();

            for (ConsulenzaDettaglio c : lista) {
                int durata = ConsulenzaDAO.getDurataMinuti(c.tipo);
                LocalTime fine = c.ora.plusMinutes(durata);

                boolean giaPassata =
                        c.data.isBefore(oggi) ||
                        (c.data.isEqual(oggi) && fine.isBefore(adesso));

                String nota = (c.note != null && !c.note.isEmpty())
                        ? c.note
                        : "Nessuna nota.";

                String blocco =
                        "- " + c.data + " ore " + c.ora + "\n" +
                        "  Tipo: " + c.tipo + "\n" +
                        "  Professionista: " + c.nomeDipendente +
                        " (" + c.ruoloDipendente + ")\n" +
                        "  Durata stimata: " + durata + " minuti\n" +
                        "  Note: " + nota + "\n\n";

                if (giaPassata) {
                    pastSb.append(blocco);
                } else {
                    futureSb.append(blocco);
                }
            }

            StringBuilder testo = new StringBuilder();

            if (futureSb.length() > 0) {
                testo.append("CONSULENZE FUTURE / PROGRAMMATE\n\n");
                testo.append(futureSb);
            }

            if (pastSb.length() > 0) {
                if (testo.length() > 0) {
                    testo.append("\n------------------------------\n\n");
                }
                testo.append("CONSULENZE PASSATE\n\n");
                testo.append(pastSb);
            }

            view.mostraDettaglioConsulenza(testo.toString());

        } catch (Exception e) {
            e.printStackTrace();
            view.mostraMessaggioErrore(
                    "Si è verificato un errore nel caricamento delle consulenze.");
        }
    }

    public void handleVediAbbonamento() {
        Abbonamento abb = cliente.getAbbonamento();
        if (abb == null) {
            view.mostraMessaggioInfo(
                    "Non hai ancora un abbonamento attivo.\n" +
                    "Vai nella sezione 'Acquista abbonamento' per sottoscriverne uno.");
            return;
        }

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

            SelezionaAbbonamentoView sView = new SelezionaAbbonamentoView(cliente);
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
