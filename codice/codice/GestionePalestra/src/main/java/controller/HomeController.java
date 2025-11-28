package controller;

import db.dao.AbbonamentoDAO;
import db.dao.CorsoDAO;
import db.dao.CorsoDAO.CorsoInfo;
import db.dao.ConsulenzaDAO;
import db.dao.PalestraDAO;
import db.dao.PalestraDAO.MacchinarioInfo;
import db.dao.PalestraDAO.SalaCorsoInfo;
import model.Abbonamento;
import model.Cliente;

import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import view.*;
import view.dialog.*;

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
        view.dispose();

        PrenotaConsulenzaView v = new PrenotaConsulenzaView(cliente);
        new PrenotaConsulenzaController(v, cliente);
        v.setVisible(true);
    }

    /** Apertura schermata prenotazione corso (solo abbonamento CORSI). */
    public void handlePrenotaCorso() {
        try {
            if (!CorsoDAO.esistonoCorsi()) {
                view.mostraMessaggioErrore(
                        "Al momento non sono presenti corsi a catalogo.\n" +
                        "Contatta la palestra per maggiori informazioni.");
                return;
            }
        } catch (Exception e) {
            e.printStackTrace();
            view.mostraMessaggioErrore(
                    "Si è verificato un errore nel caricamento dei corsi.\n" +
                    "Riprova più tardi.");
            return;
        }

        view.dispose();

        PrenotaCorsoView v = new PrenotaCorsoView(cliente);
        new PrenotaCorsoController(v, cliente);
        v.setVisible(true);
    }

    /** Vedi dettaglio abbonamento. */
    public void handleVediAbbonamento() {
        Abbonamento abb = cliente.getAbbonamento();
        if (abb == null) {
            view.mostraMessaggioInfo(
                    "Non hai ancora un abbonamento attivo.\n" +
                    "Vai nella sezione 'Acquista abbonamento' per sottoscriverne uno.");
            return;
        }

        // nascondo la Home mentre è aperta la finestra di dettaglio
        view.setVisible(false);
        view.mostraDettaglioAbbonamento(abb);
        // quando il dialog viene chiuso, torno a mostrare la Home
        view.setVisible(true);
    }

    /** Vedi corsi prenotati */
    public void handleVediCorsi() {
        try {
            String testo = CorsoDAO.buildDettaglioIscrizioniPerCliente(cliente.getIdCliente());

            // Nascondo la Home mentre è aperta la finestra "Dettagli corsi"
            view.setVisible(false);

            // Questo metodo apre un JDialog MODALE: ritorna solo quando il dialog viene chiuso
            view.mostraDettaglioCorsi(testo);

            // Quando il dialog è stato chiuso (tasto "Chiudi" o disiscrizione), ri-mosto la Home
            view.setVisible(true);

        } catch (Exception e) {
            e.printStackTrace();
            view.mostraMessaggioErrore("Errore nel caricamento dei corsi prenotati.");
        }
    }


    /** Vedi consulenze prenotate */
    public void handleVediConsulenza() {
        try {
            String dettaglio = ConsulenzaDAO
                    .buildDettaglioConsulenzePerCliente(cliente.getIdCliente());

            // nascondo la Home mentre il dialog è aperto
            view.setVisible(false);

            // dialog modale: ritorna qui solo quando viene chiuso
            view.mostraDettaglioConsulenza(dettaglio);

            // quando il dialog viene chiuso, ri-mosto la Home
            view.setVisible(true);

        } catch (Exception e) {
            e.printStackTrace();
            view.mostraMessaggioErrore("Errore nel caricamento delle consulenze.");
        }
    }

    /** Disdetta abbonamento (bloccata se ci sono consulenze o corsi futuri) */
    public void handleDisdiciAbbonamento() {
        Abbonamento abb = cliente.getAbbonamento();
        if (abb == null) {
            view.mostraMessaggioInfo("Non hai alcun abbonamento attivo da disdire.");
            return;
        }

        try {
            if (ConsulenzaDAO.esistonoConsulenzeFuturePerCliente(cliente.getIdCliente())) {
                view.mostraMessaggioErrore(
                        "Non puoi disdire l'abbonamento perché hai consulenze future prenotate.");
                return;
            }

            if (CorsoDAO.esistonoIscrizioniFuturePerCliente(cliente.getIdCliente())) {
                view.mostraMessaggioErrore(
                        "Non puoi disdire l'abbonamento perché hai corsi futuri prenotati.");
                return;
            }

        } catch (Exception e) {
            e.printStackTrace();
            view.mostraMessaggioErrore(
                    "Errore nel controllo di consulenze/corsi futuri.\n" +
                    "Riprova più tardi.");
            return;
        }

        boolean conferma = ThemedDialog.showConfirm(
                view,
                "Conferma disdetta",
                "Sei sicuro di voler disdire l’abbonamento attivo?"
        );

        if (!conferma) return;

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

    /** Apertura dialog disdetta consulenza */
    public void handleApriDisdettaConsulenza() {
        try {
            // se non ci sono consulenze future → solo messaggio, niente dialog
            if (!ConsulenzaDAO.esistonoConsulenzeFuturePerCliente(cliente.getIdCliente())) {
                view.mostraMessaggioInfo(
                        "Non hai consulenze future prenotate da poter disdire.");
                return;
            }
        } catch (Exception e) {
            e.printStackTrace();
            view.mostraMessaggioErrore(
                    "Errore nel caricamento delle consulenze future.\n" +
                    "Riprova più tardi.");
            return;
        }

        DisdiciConsulenzaDialog dialog = new DisdiciConsulenzaDialog(view, cliente);
        dialog.setVisible(true);
    }

    /** Apertura dialog disdetta corso */
    public void handleApriDisdettaCorso() {
        try {
            // analogo controllo per i corsi futuri
            if (!CorsoDAO.esistonoIscrizioniFuturePerCliente(cliente.getIdCliente())) {
                view.mostraMessaggioInfo(
                        "Non hai corsi futuri prenotati da poter disdire.");
                return;
            }
        } catch (Exception e) {
            e.printStackTrace();
            view.mostraMessaggioErrore(
                    "Errore nel caricamento dei corsi prenotati.\n" +
                    "Riprova più tardi.");
            return;
        }

        DisdiciCorsoDialog dialog = new DisdiciCorsoDialog(view, cliente);
        dialog.setVisible(true);
    }

    // ==========================================================
    //  NUOVA FUNZIONALITÀ: VISIONE / PANORAMICA PALESTRA
    // ==========================================================
    public void handlePanoramicaPalestra() {
        Abbonamento abb = cliente.getAbbonamento();
        if (abb == null) {
            view.mostraMessaggioInfo(
                    "Per visualizzare la panoramica della palestra devi avere\n" +
                    "un abbonamento attivo.");
            return;
        }

        String tipo = abb.getTipo();
        if (tipo == null) {
            view.mostraMessaggioErrore("Tipo di abbonamento non riconosciuto.");
            return;
        }

        String t = tipo.trim().toUpperCase();

        // Mappatura come da specifica:
        // BASE     -> solo sala pesi
        // COMPLETO -> sala pesi + SPA
        // CORSI    -> solo sale corsi
        boolean canSalaPesi = false;
        boolean canSpa      = false;
        boolean canCorsi    = false;

        switch (t) {
            case "BASE":
                canSalaPesi = true;
                break;
            case "COMPLETO":
                canSalaPesi = true;
                canSpa = true;
                break;
            case "CORSI":
                canCorsi = true;
                break;
            default:
                view.mostraMessaggioErrore("Tipo di abbonamento '" + tipo + "' non supportato.");
                return;
        }

        StringBuilder sb = new StringBuilder();

        try {
            // ===== SALA PESI =====
            if (canSalaPesi) {
                PalestraDAO.SalaPesiInfo info = PalestraDAO.getSalaPesiInfo();

                sb.append("=== SALA PESI ===\n\n");

                if (info == null) {
                    sb.append("La sala pesi non è al momento configurata nel sistema.\n\n");
                } else {
                    sb.append("Orari apertura: ").append(info.orariApertura).append("\n");
                    sb.append("Capienza massima: ").append(info.capienza).append(" persone\n");
                    sb.append("Metratura: ").append(info.metratura).append(" m²\n");
                    sb.append("Numero macchinari: ").append(info.numMacchinari).append("\n");
                    sb.append("Numero panche: ").append(info.numPanche).append("\n");
                    sb.append("Pesi liberi disponibili: ").append(info.numPesiLiberi).append("\n");
                    sb.append("Disponibilità attuale: ")
                      .append(info.disponibilita ? "aperta" : "chiusa")
                      .append("\n\n");
                }

                // elenco macchinari
                sb.append("Macchinari disponibili:\n");
                List<MacchinarioInfo> macchinari = PalestraDAO.getMacchinariSalaPesi();
                if (macchinari.isEmpty()) {
                    sb.append("- Nessun macchinario presente a catalogo.\n\n");
                } else {
                    for (MacchinarioInfo m : macchinari) {
                        sb.append("- ").append(m.descrizione()).append("\n");
                    }
                    sb.append("\n");
                }

                sb.append("\n");
            }

            // ===== SPA =====
            if (canSpa) {
                PalestraDAO.SpaInfo spa = PalestraDAO.getSpaInfo();

                sb.append("=== SPA ===\n\n");

                if (spa == null) {
                    sb.append("La SPA non è al momento configurata nel sistema.\n\n");
                } else {
                    sb.append("Orari apertura: ").append(spa.orariApertura).append("\n");
                    sb.append("Capienza massima: ").append(spa.capienza).append(" persone\n");
                    sb.append("Numero saune: ").append(spa.numSaune).append("\n");
                    sb.append("Numero piscine: ").append(spa.numPiscine).append("\n");
                    sb.append("Disponibilità attuale: ")
                      .append(spa.disponibilita ? "aperta" : "chiusa")
                      .append("\n\n");

                    sb.append("La SPA offre aree relax dedicate con saune e piscine,\n")
                      .append("pensate per il recupero post-allenamento e il benessere generale.\n\n");
                }

                sb.append("\n");
            }

            // ===== SALE CORSI =====
            if (canCorsi) {
                List<SalaCorsoInfo> saleCorsi = PalestraDAO.getSaleCorsiInfo();

                sb.append("=== SALE CORSI ===\n\n");

                if (saleCorsi.isEmpty()) {
                    sb.append("Al momento non sono configurate sale corsi.\n\n");
                } else {
                    int idx = 1;
                    for (SalaCorsoInfo s : saleCorsi) {
                        sb.append("Sala corsi #").append(idx++).append("\n");
                        sb.append("Orari apertura sala: ").append(s.orariApertura).append("\n");
                        sb.append("Capienza: ").append(s.capienza).append(" persone\n");

                        // qui usiamo l'orarioCorso presente in SALA_CORSI
                        if (s.orarioCorso != null && !s.orarioCorso.isEmpty()) {
                            sb.append("Programmazione corso: ")
                              .append(s.orarioCorso)
                              .append("\n");
                        } else {
                            sb.append("Programmazione corso: non specificata a sistema\n");
                        }

                        sb.append("Disponibilità: ")
                          .append(s.disponibilita ? "attiva" : "non attiva")
                          .append("\n\n");
                    }
                }

                // opzionale: elenco corsi dal catalogo, con descrizione
                try {
                    List<CorsoInfo> corsiCatalogo = CorsoDAO.getTuttiICorsi();
                    if (!corsiCatalogo.isEmpty()) {
                        sb.append("Corsi a catalogo:\n\n");
                        for (CorsoInfo c : corsiCatalogo) {
                            sb.append("- ").append(c.nome).append("\n");
                            sb.append("  Durata: ").append(c.durataMinuti).append(" minuti\n");
                            sb.append("  Descrizione: ").append(c.descrizione).append("\n\n");
                        }
                    }
                } catch (Exception ignore) {
                    // se fallisce, non blocchiamo tutta la panoramica
                }

                sb.append("\n");
            }

        } catch (Exception e) {
            logger.error("Errore nel caricamento panoramica palestra per utente {}",
                    cliente.getUsername(), e);
            view.mostraMessaggioErrore(
                    "Si è verificato un errore nel caricamento della panoramica palestra.\n" +
                    "Riprova più tardi.");
            return;
        }

        if (sb.length() == 0) {
            view.mostraMessaggioInfo(
                    "Per il tuo tipo di abbonamento non è disponibile alcuna area da visualizzare.");
            return;
        }

        // Nascondo la Home mentre è aperto il dialog
        view.setVisible(false);
        view.mostraPanoramicaPalestra(sb.toString());
        view.setVisible(true);
    }
}
