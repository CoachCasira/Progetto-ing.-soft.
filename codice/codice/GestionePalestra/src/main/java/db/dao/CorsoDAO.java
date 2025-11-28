package db.dao;

import db.GestioneDB;

import java.sql.*;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class CorsoDAO {

    // ===================== DTO CORSI =====================
    public static class CorsoInfo {
        public final int idCorso;
        public final String nome;
        public final String descrizione;
        public final int durataMinuti;

        public CorsoInfo(int idCorso, String nome, String descrizione, int durataMinuti) {
            this.idCorso = idCorso;
            this.nome = nome;
            this.descrizione = descrizione;
            this.durataMinuti = durataMinuti;
        }
    }

    // ===================== DTO LEZIONI =====================
    public static class LezioneInfo {
        public final int idLezione;
        public final int idCorso;
        public final LocalDate data;
        public final LocalTime ora;
        public final int durataMinuti;
        public final int postiTotali;
        public final int postiPrenotati;
        public final String nomeIstruttore;  // "Nome Cognome"

        public LezioneInfo(int idLezione,
                           int idCorso,
                           LocalDate data,
                           LocalTime ora,
                           int durataMinuti,
                           int postiTotali,
                           int postiPrenotati,
                           String nomeIstruttore) {
            this.idLezione = idLezione;
            this.idCorso = idCorso;
            this.data = data;
            this.ora = ora;
            this.durataMinuti = durataMinuti;
            this.postiTotali = postiTotali;
            this.postiPrenotati = postiPrenotati;
            this.nomeIstruttore = nomeIstruttore;
        }

        public int postiDisponibili() {
            return postiTotali - postiPrenotati;
        }

        public LocalDateTime getInizio() {
            return LocalDateTime.of(data, ora);
        }

        public LocalDateTime getFine() {
            return getInizio().plusMinutes(durataMinuti);
        }
    }

    // ===================== DTO ISCRIZIONI PER DISDETTA =====================
    public static class IscrizioneInfo {
        public final int idLezione;
        public final int idCorso;
        public final String nomeCorso;
        public final LocalDate data;
        public final LocalTime ora;
        public final int durataMinuti;
        public final String nomeIstruttore;

        public IscrizioneInfo(int idLezione,
                              int idCorso,
                              String nomeCorso,
                              LocalDate data,
                              LocalTime ora,
                              int durataMinuti,
                              String nomeIstruttore) {
            this.idLezione = idLezione;
            this.idCorso = idCorso;
            this.nomeCorso = nomeCorso;
            this.data = data;
            this.ora = ora;
            this.durataMinuti = durataMinuti;
            this.nomeIstruttore = nomeIstruttore;
        }

        public LocalDateTime getInizio() {
            return LocalDateTime.of(data, ora);
        }
    }

    // ===================== UTILITY PER DESCRIZIONE PROGRAMMAZIONE =====================

    private static String abbreviazioneGiorno(DayOfWeek d) {
        switch (d) {
            case MONDAY:    return "Lun";
            case TUESDAY:   return "Mar";
            case WEDNESDAY: return "Mer";
            case THURSDAY:  return "Gio";
            case FRIDAY:    return "Ven";
            case SATURDAY:  return "Sab";
            case SUNDAY:    return "Dom";
            default:        return d.toString();
        }
    }

    /**
     * Costruisce una descrizione compatta della programmazione di un corso,
     * del tipo:
     *    "Lun-Mer-Ven 18:00"
     * oppure, se ci sono orari diversi:
     *    "Lun-Mer 18:00 / Ven 19:00"
     *
     * Legge la tabella LEZIONE_CORSO, quindi è sempre coerente
     * con le lezioni effettivamente prenotabili.
     */
    public static String buildDescrizioneProgrammazioneCorso(int idCorso) throws Exception {
        String sql =
                "SELECT DATA_LEZIONE, ORA_LEZIONE " +
                "FROM LEZIONE_CORSO " +
                "WHERE ID_CORSO = ? " +
                "ORDER BY DATA_LEZIONE, ORA_LEZIONE";

        // mappa: ora -> lista di abbreviazioni dei giorni (Lun, Mer, ...)
        Map<LocalTime, List<String>> byOra = new LinkedHashMap<>();

        try (Connection conn = GestioneDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, idCorso);

            try (ResultSet rs = ps.executeQuery()) {
                boolean any = false;

                while (rs.next()) {
                    any = true;

                    LocalDate data = rs.getDate("DATA_LEZIONE").toLocalDate();
                    LocalTime ora  = rs.getTime("ORA_LEZIONE").toLocalTime();
                    DayOfWeek dow  = data.getDayOfWeek();

                    String giorno = abbreviazioneGiorno(dow);

                    byOra.computeIfAbsent(ora, k -> new ArrayList<>())
                         .add(giorno);
                }

                if (!any) {
                    return "Nessuna lezione programmata.";
                }
            }
        }

        StringBuilder sb = new StringBuilder();
        boolean firstTime = true;

        for (Map.Entry<LocalTime, List<String>> entry : byOra.entrySet()) {
            if (!firstTime) {
                sb.append(" / ");   // separatore tra orari diversi
            }
            firstTime = false;

            LocalTime ora = entry.getKey();
            List<String> giorni = entry.getValue();

            // elimino eventuali duplicati mantenendo l'ordine
            Set<String> unique = new LinkedHashSet<>(giorni);
            String giorniStr = String.join("-", unique);

            // formatto ora come HH:MM
            String oraStr = ora.toString();
            if (oraStr.length() > 5) {
                oraStr = oraStr.substring(0, 5); // es. "18:00:00" -> "18:00"
            }

            sb.append(giorniStr).append(" ").append(oraStr);
        }

        return sb.toString();
    }

    // ===================== CORSI E LEZIONI =====================

    /** Restituisce tutti i corsi disponibili. */
    public static List<CorsoInfo> getTuttiICorsi() throws Exception {
        String sql = "SELECT ID_CORSO, NOME, DESCRIZIONE, DURATA_MINUTI FROM CORSO ORDER BY NOME";

        List<CorsoInfo> result = new ArrayList<>();

        try (Connection conn = GestioneDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                int id = rs.getInt("ID_CORSO");
                String nome = rs.getString("NOME");
                String descr = rs.getString("DESCRIZIONE");
                int durata = rs.getInt("DURATA_MINUTI");

                result.add(new CorsoInfo(id, nome, descr, durata));
            }
        }

        return result;
    }

    /** Restituisce SOLO le lezioni future previste per un corso. */
    public static List<LezioneInfo> getLezioniPerCorso(int idCorso) throws Exception {
        String sql =
                "SELECT L.ID_LEZIONE, L.ID_CORSO, L.DATA_LEZIONE, L.ORA_LEZIONE, " +
                "       L.POSTI_TOTALI, L.POSTI_PRENOTATI, " +
                "       C.DURATA_MINUTI, D.NOME, D.COGNOME " +
                "FROM LEZIONE_CORSO L " +
                "JOIN CORSO C ON L.ID_CORSO = C.ID_CORSO " +
                "JOIN DIPENDENTE D ON L.ID_ISTRUTTORE = D.ID_DIPENDENTE " +
                "WHERE L.ID_CORSO = ? " +
                "ORDER BY L.DATA_LEZIONE, L.ORA_LEZIONE";

        List<LezioneInfo> result = new ArrayList<>();
        LocalDate oggi = LocalDate.now();

        try (Connection conn = GestioneDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, idCorso);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int idLezione = rs.getInt("ID_LEZIONE");
                    LocalDate data = rs.getDate("DATA_LEZIONE").toLocalDate();
                    LocalTime ora  = rs.getTime("ORA_LEZIONE").toLocalTime();
                    int postiTot   = rs.getInt("POSTI_TOTALI");
                    int postiPren  = rs.getInt("POSTI_PRENOTATI");
                    int durata     = rs.getInt("DURATA_MINUTI");
                    String nomeIstr = rs.getString("NOME") + " " + rs.getString("COGNOME");

                    // Se la data è nel passato o oggi, la porto avanti di settimane
                    // finché non è strettamente > oggi, mantenendo il giorno della settimana.
                    while (!data.isAfter(oggi)) {
                        data = data.plusWeeks(1);
                    }

                    result.add(new LezioneInfo(
                            idLezione, idCorso, data, ora,
                            durata, postiTot, postiPren, nomeIstr
                    ));
                }
            }
        }

        return result;
    }
    
    /**
     * Porta in avanti le date delle lezioni finché non sono nel presente/futuro.
     * Mantiene lo stesso giorno della settimana, aggiungendo settimane.
     *
     * Esempio: una lezione del 2025-11-18 (Martedì) diventa 2025-11-25,
     * poi 2025-12-02, ... finché DATA_LEZIONE >= oggi.
     */
    public static void aggiornaDateLezioniAllaSettimanaCorrente() throws Exception {
        String sqlSelect = "SELECT ID_LEZIONE, DATA_LEZIONE FROM LEZIONE_CORSO";
        String sqlUpdate = "UPDATE LEZIONE_CORSO SET DATA_LEZIONE = ? WHERE ID_LEZIONE = ?";

        LocalDate oggi = LocalDate.now();

        try (Connection conn = GestioneDB.getConnection();
             PreparedStatement psSel = conn.prepareStatement(sqlSelect);
             ResultSet rs = psSel.executeQuery();
             PreparedStatement psUpd = conn.prepareStatement(sqlUpdate)) {

            while (rs.next()) {
                int idLez = rs.getInt("ID_LEZIONE");
                LocalDate dataOriginale = rs.getDate("DATA_LEZIONE").toLocalDate();
                LocalDate dataNuova = dataOriginale;

                // finché è nel passato, sposto avanti di 1 settimana
                while (dataNuova.isBefore(oggi)) {
                    dataNuova = dataNuova.plusWeeks(1);
                }

                if (!dataNuova.equals(dataOriginale)) {
                    psUpd.setDate(1, Date.valueOf(dataNuova));
                    psUpd.setInt(2, idLez);
                    psUpd.addBatch();
                }
            }

            psUpd.executeBatch();
        }
    }


    // ===================== CONFLITTI E ISCRIZIONI =====================

    /**
     * Controlla se per un cliente esiste già un corso in conflitto
     * con la lezione che si vuole prenotare (stessa data e intervalli sovrapposti).
     */
    public static boolean esisteConflittoPerCliente(int idCliente,
                                                    LocalDate dataNuova,
                                                    LocalTime oraNuova,
                                                    int durataNuovaMin) throws Exception {

        String sql =
                "SELECT L.DATA_LEZIONE, L.ORA_LEZIONE, C.DURATA_MINUTI " +
                "FROM ISCRIZIONE_CORSO I " +
                "JOIN LEZIONE_CORSO L ON I.ID_LEZIONE = L.ID_LEZIONE " +
                "JOIN CORSO C ON L.ID_CORSO = C.ID_CORSO " +
                "WHERE I.ID_CLIENTE = ? AND L.DATA_LEZIONE = ?";

        LocalDateTime inizioNuovo = LocalDateTime.of(dataNuova, oraNuova);
        LocalDateTime fineNuovo   = inizioNuovo.plusMinutes(durataNuovaMin);

        try (Connection conn = GestioneDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, idCliente);
            ps.setDate(2, Date.valueOf(dataNuova));

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    LocalDate dataEsistente = rs.getDate("DATA_LEZIONE").toLocalDate();
                    LocalTime oraEsistente  = rs.getTime("ORA_LEZIONE").toLocalTime();
                    int durataEsistente     = rs.getInt("DURATA_MINUTI");

                    LocalDateTime inizioEsistente = LocalDateTime.of(dataEsistente, oraEsistente);
                    LocalDateTime fineEsistente   = inizioEsistente.plusMinutes(durataEsistente);

                    boolean overlap =
                            !inizioNuovo.isAfter(fineEsistente) &&
                            !fineNuovo.isBefore(inizioEsistente);

                    if (overlap) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    /** Controlla se la lezione ha ancora posti liberi. */
    public static boolean haPostiDisponibili(int idLezione) throws Exception {
        String sql = "SELECT POSTI_TOTALI, POSTI_PRENOTATI FROM LEZIONE_CORSO WHERE ID_LEZIONE = ?";

        try (Connection conn = GestioneDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, idLezione);

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return false;
                int tot = rs.getInt("POSTI_TOTALI");
                int pren = rs.getInt("POSTI_PRENOTATI");
                return pren < tot;
            }
        }
    }

    /**
     * Iscrive un cliente ad una lezione:
     * - controlla i posti
     * - incrementa POSTI_PRENOTATI
     * - inserisce in ISCRIZIONE_CORSO
     */
    public static void iscriviClienteALezione(int idCliente, int idLezione) throws Exception {
        String sqlCheck =
                "SELECT POSTI_TOTALI, POSTI_PRENOTATI FROM LEZIONE_CORSO WHERE ID_LEZIONE = ? FOR UPDATE";
        String sqlUpdate =
                "UPDATE LEZIONE_CORSO SET POSTI_PRENOTATI = POSTI_PRENOTATI + 1 WHERE ID_LEZIONE = ?";
        String sqlInsert =
                "INSERT INTO ISCRIZIONE_CORSO (ID_CLIENTE, ID_LEZIONE) VALUES (?, ?)";

        try (Connection conn = GestioneDB.getConnection()) {
            conn.setAutoCommit(false);

            try (PreparedStatement psCheck = conn.prepareStatement(sqlCheck);
                 PreparedStatement psUpd   = conn.prepareStatement(sqlUpdate);
                 PreparedStatement psIns   = conn.prepareStatement(sqlInsert)) {

                // controllo posti
                psCheck.setInt(1, idLezione);
                try (ResultSet rs = psCheck.executeQuery()) {
                    if (!rs.next()) {
                        conn.rollback();
                        throw new Exception("Lezione inesistente.");
                    }
                    int tot  = rs.getInt("POSTI_TOTALI");
                    int pren = rs.getInt("POSTI_PRENOTATI");
                    if (pren >= tot) {
                        conn.rollback();
                        throw new Exception("Nessun posto disponibile per questa lezione.");
                    }
                }

                // incremento contatore posti
                psUpd.setInt(1, idLezione);
                psUpd.executeUpdate();

                // inserimento iscrizione
                psIns.setInt(1, idCliente);
                psIns.setInt(2, idLezione);
                psIns.executeUpdate();

                conn.commit();
            } catch (Exception ex) {
                conn.rollback();
                throw ex;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    // ===================== DISISCRIZIONE CORSI =====================

    /** Restituisce le iscrizioni future di un cliente (usato dal dialog di disdetta). */
    public static List<IscrizioneInfo> getIscrizioniFuturePerCliente(int idCliente) throws Exception {
        String sql =
                "SELECT L.ID_LEZIONE, L.ID_CORSO, L.DATA_LEZIONE, L.ORA_LEZIONE, " +
                "       C.NOME AS NOME_CORSO, C.DURATA_MINUTI, " +
                "       D.NOME, D.COGNOME " +
                "FROM ISCRIZIONE_CORSO I " +
                "JOIN LEZIONE_CORSO L ON I.ID_LEZIONE = L.ID_LEZIONE " +
                "JOIN CORSO C ON L.ID_CORSO = C.ID_CORSO " +
                "JOIN DIPENDENTE D ON L.ID_ISTRUTTORE = D.ID_DIPENDENTE " +
                "WHERE I.ID_CLIENTE = ? AND L.DATA_LEZIONE >= ? " +
                "ORDER BY L.DATA_LEZIONE, L.ORA_LEZIONE";

        List<IscrizioneInfo> result = new ArrayList<>();

        try (Connection conn = GestioneDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, idCliente);
            ps.setDate(2, Date.valueOf(LocalDate.now()));

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int idLezione = rs.getInt("ID_LEZIONE");
                    int idCorso   = rs.getInt("ID_CORSO");
                    LocalDate data = rs.getDate("DATA_LEZIONE").toLocalDate();
                    LocalTime ora  = rs.getTime("ORA_LEZIONE").toLocalTime();
                    int durata = rs.getInt("DURATA_MINUTI");
                    String nomeCorso = rs.getString("NOME_CORSO");
                    String nomeIstr = rs.getString("NOME") + " " + rs.getString("COGNOME");

                    result.add(new IscrizioneInfo(
                            idLezione, idCorso, nomeCorso,
                            data, ora, durata, nomeIstr
                    ));
                }
            }
        }

        return result;
    }

    /**
     * Disiscrive un cliente da una lezione:
     * - decrementa POSTI_PRENOTATI
     * - elimina riga da ISCRIZIONE_CORSO
     */
    public static void disiscriviClienteDaLezione(int idCliente, int idLezione) throws Exception {
        String sqlCheck =
                "SELECT POSTI_PRENOTATI FROM LEZIONE_CORSO WHERE ID_LEZIONE = ? FOR UPDATE";
        String sqlUpdate =
                "UPDATE LEZIONE_CORSO SET POSTI_PRENOTATI = POSTI_PRENOTATI - 1 WHERE ID_LEZIONE = ?";
        String sqlDelete =
                "DELETE FROM ISCRIZIONE_CORSO WHERE ID_CLIENTE = ? AND ID_LEZIONE = ?";

        try (Connection conn = GestioneDB.getConnection()) {
            conn.setAutoCommit(false);

            try (PreparedStatement psCheck = conn.prepareStatement(sqlCheck);
                 PreparedStatement psUpd   = conn.prepareStatement(sqlUpdate);
                 PreparedStatement psDel   = conn.prepareStatement(sqlDelete)) {

                // controllo che ci sia almeno un iscritto
                psCheck.setInt(1, idLezione);
                try (ResultSet rs = psCheck.executeQuery()) {
                    if (!rs.next()) {
                        conn.rollback();
                        throw new Exception("Lezione inesistente.");
                    }
                    int pren = rs.getInt("POSTI_PRENOTATI");
                    if (pren <= 0) {
                        conn.rollback();
                        throw new Exception("Nessuna iscrizione da rimuovere per questa lezione.");
                    }
                }

                // decremento contatore posti
                psUpd.setInt(1, idLezione);
                psUpd.executeUpdate();

                // rimozione iscrizione
                psDel.setInt(1, idCliente);
                psDel.setInt(2, idLezione);
                psDel.executeUpdate();

                conn.commit();
            } catch (Exception ex) {
                conn.rollback();
                throw ex;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    // ===================== DETTAGLIO CORSI PRENOTATI =====================

    public static String buildDettaglioIscrizioniPerCliente(int idCliente) throws Exception {
        String sql =
                "SELECT C.NOME, C.DESCRIZIONE, C.DURATA_MINUTI, " +
                "       L.DATA_LEZIONE, L.ORA_LEZIONE, " +
                "       L.POSTI_TOTALI, L.POSTI_PRENOTATI, " +
                "       D.NOME AS NOME_IST, D.COGNOME AS COGNOME_IST " +
                "FROM ISCRIZIONE_CORSO I " +
                "JOIN LEZIONE_CORSO L ON I.ID_LEZIONE = L.ID_LEZIONE " +
                "JOIN CORSO C ON L.ID_CORSO = C.ID_CORSO " +
                "JOIN DIPENDENTE D ON L.ID_ISTRUTTORE = D.ID_DIPENDENTE " +
                "WHERE I.ID_CLIENTE = ? " +
                "ORDER BY L.DATA_LEZIONE, L.ORA_LEZIONE";

        StringBuilder futureSb = new StringBuilder();
        StringBuilder pastSb   = new StringBuilder();

        LocalDateTime now = LocalDateTime.now();

        try (Connection conn = GestioneDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, idCliente);

            try (ResultSet rs = ps.executeQuery()) {
                boolean any = false;

                while (rs.next()) {
                    any = true;

                    String nomeCorso = rs.getString("NOME");
                    String descr = rs.getString("DESCRIZIONE");
                    int durata = rs.getInt("DURATA_MINUTI");
                    LocalDate data = rs.getDate("DATA_LEZIONE").toLocalDate();
                    LocalTime ora  = rs.getTime("ORA_LEZIONE").toLocalTime();
                    int tot  = rs.getInt("POSTI_TOTALI");
                    int pren = rs.getInt("POSTI_PRENOTATI");
                    String nomeIstr = rs.getString("NOME_IST") + " " + rs.getString("COGNOME_IST");

                    LocalDateTime inizio = LocalDateTime.of(data, ora);

                    StringBuilder target = inizio.isBefore(now) ? pastSb : futureSb;

                    target.append("- ").append(data).append(" ore ").append(ora)
                          .append("\nCorso: ").append(nomeCorso)
                          .append("\nIstruttore: ").append(nomeIstr)
                          .append("\nDurata: ").append(durata).append(" minuti")
                          .append("\nPosti: ").append(pren).append("/").append(tot)
                          .append("\nDescrizione: ").append(descr)
                          .append("\n\n");
                }

                if (!any) {
                    return "Non hai ancora nessuna iscrizione ai corsi.\n";
                }
            }
        }

        StringBuilder sb = new StringBuilder();

        if (futureSb.length() > 0) {
            sb.append("Corsi futuri:\n\n");
            sb.append(futureSb);
        }

        if (pastSb.length() > 0) {
            if (futureSb.length() > 0) {
                sb.append("\n"); // riga vuota di separazione
            }
            sb.append("Corsi già svolti:\n\n");
            sb.append(pastSb);
        }

        return sb.toString();
    }

    /** Ritorna true se esiste almeno un corso nel catalogo. */
    public static boolean esistonoCorsi() throws Exception {
        String sql = "SELECT COUNT(*) FROM CORSO";

        try (Connection conn = GestioneDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            if (!rs.next()) return false;
            return rs.getInt(1) > 0;
        }
    }

    /** Ritorna true se il cliente ha almeno una iscrizione futura. */
    public static boolean esistonoIscrizioniFuturePerCliente(int idCliente) throws Exception {
        String sql =
                "SELECT COUNT(*) FROM ISCRIZIONE_CORSO I " +
                "JOIN LEZIONE_CORSO L ON I.ID_LEZIONE = L.ID_LEZIONE " +
                "WHERE I.ID_CLIENTE = ? AND L.DATA_LEZIONE >= ?";

        try (Connection conn = GestioneDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, idCliente);
            ps.setDate(2, Date.valueOf(LocalDate.now()));

            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1) > 0;
            }
        }
    }
}
