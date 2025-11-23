package db.dao;

import db.GestioneDB;
import model.Consulenza;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Time;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

public class ConsulenzaDAO {

    /** DTO per mostrare i dettagli in Home */
    public static class ConsulenzaDettaglio {
        public final LocalDate data;
        public final LocalTime ora;
        public final String tipo;
        public final String note;
        public final String nomeDipendente;
        public final String ruoloDipendente;

        public ConsulenzaDettaglio(LocalDate data,
                                   LocalTime ora,
                                   String tipo,
                                   String note,
                                   String nomeDipendente,
                                   String ruoloDipendente) {
            this.data = data;
            this.ora = ora;
            this.tipo = tipo;
            this.note = note;
            this.nomeDipendente = nomeDipendente;
            this.ruoloDipendente = ruoloDipendente;
        }
    }

    /** Durata standard (in minuti) per ciascun tipo di consulenza */
    public static int getDurataMinuti(String tipo) {
        if (tipo == null) return 30;
        switch (tipo.toUpperCase()) {
            case "PERSONAL_TRAINER":
                return 45;
            case "NUTRIZIONISTA":
                return 45;
            case "ISTRUTTORE_CORSO":
                return 30;
            default:
                return 30;
        }
    }

    /**
     * Controlla se esiste un conflitto per il CLIENTE:
     * non può avere due consulenze che si sovrappongono
     * (anche con dipendenti / tipi diversi).
     */
    public static boolean esisteConflitto(int idCliente,
                                          int idDipendente,
                                          String tipoNuovo,
                                          LocalDate dataNuova,
                                          LocalTime oraNuova) throws Exception {

        String sql = "SELECT TIPO, DATA_CONSULENZA, ORA_CONSULENZA " +
                     "FROM CONSULENZA " +
                     "WHERE ID_CLIENTE = ? AND DATA_CONSULENZA = ?";

        int durataNuova = getDurataMinuti(tipoNuovo);
        LocalTime inizioNuova = oraNuova;
        LocalTime fineNuova   = oraNuova.plusMinutes(durataNuova);

        try (Connection conn = GestioneDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, idCliente);
            ps.setDate(2, Date.valueOf(dataNuova));

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String tipoEsistente   = rs.getString("TIPO");
                    LocalDate dataEsistente= rs.getDate("DATA_CONSULENZA").toLocalDate();
                    LocalTime oraEsistente = rs.getTime("ORA_CONSULENZA").toLocalTime();

                    int durataEsistente = getDurataMinuti(tipoEsistente);
                    LocalTime inizioEsistente = oraEsistente;
                    LocalTime fineEsistente   = oraEsistente.plusMinutes(durataEsistente);

                    // stesso giorno già garantito dalla WHERE, lo ricontrolliamo per chiarezza
                    if (!dataEsistente.equals(dataNuova)) {
                        continue;
                    }

                    // verifica sovrapposizione intervalli orari:
                    // [inizioNuova, fineNuova) si sovrappone a [inizioEsistente, fineEsistente)
                    boolean nonSiSovrappone =
                            fineNuova.compareTo(inizioEsistente) <= 0 ||
                            fineEsistente.compareTo(inizioNuova) <= 0;

                    if (!nonSiSovrappone) {
                        return true; // conflitto trovato
                    }
                }
            }
        }

        return false;
    }

    /** Inserisce una nuova consulenza nel DB */
    public static void inserisci(Consulenza c) throws Exception {
        String sql = "INSERT INTO CONSULENZA " +
                     "(ID_CLIENTE, ID_DIPENDENTE, TIPO, DATA_CONSULENZA, ORA_CONSULENZA, NOTE) " +
                     "VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection conn = GestioneDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, c.getIdCliente());
            ps.setInt(2, c.getIdDipendente());
            ps.setString(3, c.getTipo());
            ps.setDate(4, Date.valueOf(c.getData()));
            ps.setTime(5, Time.valueOf(c.getOra()));
            ps.setString(6, c.getNote());

            ps.executeUpdate();
        }
    }

    /**
     * Restituisce TUTTE le consulenze del cliente, con anche
     * nome e ruolo del dipendente, ordinate per data/ora.
     */
    public static List<ConsulenzaDettaglio> findByCliente(int idCliente) throws Exception {
        String sql =
            "SELECT c.TIPO, c.DATA_CONSULENZA, c.ORA_CONSULENZA, c.NOTE, " +
            "       d.NOME, d.COGNOME, d.RUOLO " +
            "FROM CONSULENZA c " +
            "JOIN DIPENDENTE d ON c.ID_DIPENDENTE = d.ID_DIPENDENTE " +
            "WHERE c.ID_CLIENTE = ? " +
            "ORDER BY c.DATA_CONSULENZA, c.ORA_CONSULENZA";

        List<ConsulenzaDettaglio> lista = new ArrayList<>();

        try (Connection conn = GestioneDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, idCliente);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String tipo   = rs.getString("TIPO");
                    LocalDate data = rs.getDate("DATA_CONSULENZA").toLocalDate();
                    LocalTime ora  = rs.getTime("ORA_CONSULENZA").toLocalTime();
                    String note    = rs.getString("NOTE");

                    String nome   = rs.getString("NOME");
                    String cognome= rs.getString("COGNOME");
                    String ruolo  = rs.getString("RUOLO");

                    String nomeCompleto = nome + " " + cognome;

                    lista.add(new ConsulenzaDettaglio(
                            data,
                            ora,
                            tipo,
                            note,
                            nomeCompleto,
                            ruolo
                    ));
                }
            }
        }

        return lista;
    }
}
