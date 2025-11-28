package db.dao;

import model.Abbonamento;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import db.GestioneDB;

import java.sql.*;
import java.util.Date;

public class AbbonamentoDAO {

    private static final Logger logger =
            LogManager.getLogger(AbbonamentoDAO.class);

    /**
     * Restituisce l'abbonamento associato a un cliente (se esiste),
     * altrimenti null.
     *
     * Usa la colonna TIPO della tabella ABBONAMENTO (BASICO / COMPLETO / CORSI),
     * non il prefisso dell'ID.
     */
    public static Abbonamento getAbbonamentoByClienteId(int idCliente) {
        String sql =
                "SELECT ID_ABBONAMENTO, TIPO, SCADENZA, " +
                "       FASCIA_ORARIA_CONSENTITA, PREZZO " +
                "FROM ABBONAMENTO " +
                "WHERE ID_CLIENTE = ?";

        try (Connection conn = GestioneDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, idCliente);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {

                    int idAbbonamentoInt = rs.getInt("ID_ABBONAMENTO");
                    String tipo = rs.getString("TIPO");   // BASICO / COMPLETO / CORSI

                    Date scadenza =
                            rs.getDate("SCADENZA") != null
                                    ? new Date(rs.getDate("SCADENZA").getTime())
                                    : null;
                    String fascia = rs.getString("FASCIA_ORARIA_CONSENTITA");
                    int prezzo = rs.getInt("PREZZO");

                    // Crea la sottoclasse giusta a partire dal tipo
                    Abbonamento abbonamento = Abbonamento.creaDaTipo(tipo, idCliente);
                    if (abbonamento == null) {
                        logger.warn("Tipo abbonamento sconosciuto '{}' per cliente {}: ritorno null",
                                tipo, idCliente);
                        return null;
                    }

                    // ID auto-increment (in DB è INT, nel model lo salviamo come String)
                    abbonamento.setIdAbbonamento(String.valueOf(idAbbonamentoInt));
                    abbonamento.setPrezzo(prezzo);
                    abbonamento.setFasciaOrariaConsentita(fascia);
                    abbonamento.setScadenza(scadenza);

                    return abbonamento;
                }
            }

        } catch (SQLException e) {
            logger.error("Errore nel recupero abbonamento per cliente {}", idCliente, e);
        }

        return null;
    }

    /**
     * Salva su DB un nuovo abbonamento associato a un cliente.
     * Usa ID_ABBONAMENTO INT AUTO_INCREMENT generato dal database.
     */
    public static void salvaAbbonamento(Abbonamento abbonamento, int idCliente) {
        if (abbonamento == null) {
            return;
        }

        // Il tipo deve essere qualcosa come "BASICO", "COMPLETO", "CORSI"
        String tipo = abbonamento.getTipo();
        if (tipo == null || tipo.isEmpty()) {
            logger.warn("Tentativo di salvare un abbonamento senza tipo per cliente {}", idCliente);
            return;
        }

        String sql =
                "INSERT INTO ABBONAMENTO " +
                "(TIPO, SCADENZA, ID_SPOGLIATOIO, ID_CLIENTE, " +
                " FASCIA_ORARIA_CONSENTITA, PREZZO) " +
                "VALUES (?, ?, NULL, ?, ?, ?)";

        try (Connection conn = GestioneDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            java.sql.Date dataSql = null;
            if (abbonamento.getScadenza() != null) {
                dataSql = new java.sql.Date(abbonamento.getScadenza().getTime());
            }

            ps.setString(1, tipo);
            ps.setDate(2, dataSql);
            ps.setInt(3, idCliente);
            ps.setString(4, abbonamento.getFasciaOrariaConsentita());
            ps.setInt(5, abbonamento.getPrezzo());

            int rows = ps.executeUpdate();
            if (rows > 0) {
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        int idGen = rs.getInt(1);
                        abbonamento.setIdAbbonamento(String.valueOf(idGen));
                        logger.info("Abbonamento {} (tipo {}) salvato per cliente {}",
                                idGen, tipo, idCliente);
                    }
                }
            } else {
                logger.warn("Nessun abbonamento salvato per cliente {}", idCliente);
            }

        } catch (SQLException e) {
            logger.error("Errore nel salvataggio abbonamento per cliente {}", idCliente, e);
        }
    }

    /**
     * Disdice l'abbonamento di un cliente / ID_CLIENTE esplicito.
     * Elimina anche pagamenti, iscrizioni ai corsi e consulenze
     * collegate a quel cliente.
     */
    public static void disdiciAbbonamentoPerCliente(int idCliente) {

        // elimina iscrizioni corsi del cliente
        String sqlDeleteIscrizioniCorsi =
                "DELETE FROM ISCRIZIONE_CORSO WHERE ID_CLIENTE = ?";

        // elimina consulenze del cliente
        String sqlDeleteConsulenze =
                "DELETE FROM CONSULENZA WHERE ID_CLIENTE = ?";

        // elimina pagamenti legati ai suoi abbonamenti
        String sqlDeletePagamenti =
                "DELETE FROM PAGAMENTO WHERE ID_CLIENTE = ? " +
                "AND ID_ABBONAMENTO IN (SELECT ID_ABBONAMENTO FROM ABBONAMENTO WHERE ID_CLIENTE = ?)";

        // elimina abbonamenti del cliente
        String sqlDeleteAbbonamenti =
                "DELETE FROM ABBONAMENTO WHERE ID_CLIENTE = ?";

        try (Connection conn = GestioneDB.getConnection()) {
            conn.setAutoCommit(false);

            try (PreparedStatement psCorsi = conn.prepareStatement(sqlDeleteIscrizioniCorsi);
                 PreparedStatement psCons  = conn.prepareStatement(sqlDeleteConsulenze);
                 PreparedStatement ps1     = conn.prepareStatement(sqlDeletePagamenti);
                 PreparedStatement ps2     = conn.prepareStatement(sqlDeleteAbbonamenti)) {

                // 1) cancello tutte le iscrizioni ai corsi del cliente
                psCorsi.setInt(1, idCliente);
                psCorsi.executeUpdate();

                // 2) cancello tutte le consulenze del cliente
                psCons.setInt(1, idCliente);
                psCons.executeUpdate();

                // 3) cancello i pagamenti legati ai suoi abbonamenti
                ps1.setInt(1, idCliente);
                ps1.setInt(2, idCliente);
                ps1.executeUpdate();

                // 4) cancello gli abbonamenti del cliente
                ps2.setInt(1, idCliente);
                ps2.executeUpdate();

                conn.commit();
                logger.info("Abbonamento, pagamenti, corsi e consulenze disdetti per cliente {}",
                        idCliente);

            } catch (SQLException e) {
                conn.rollback();
                logger.error("Errore durante la disdetta abbonamento per cliente {}", idCliente, e);
            } finally {
                conn.setAutoCommit(true);
            }

        } catch (SQLException e) {
            logger.error("Errore di connessione in disdiciAbbonamentoPerCliente", e);
        }
    }

    /**
     * Disdice l'abbonamento partendo dall'USERNAME del cliente.
     * Elimina anche pagamenti, iscrizioni ai corsi e consulenze
     * collegate a quel cliente.
     */
    public static void disdiciAbbonamentoPerUsername(String username) {

        // elimina iscrizioni ai corsi del cliente
        String sqlDeleteIscrizioniCorsi =
                "DELETE FROM ISCRIZIONE_CORSO " +
                "WHERE ID_CLIENTE IN (" +
                "   SELECT ID_CLIENTE FROM CLIENTE WHERE USERNAME = ?" +
                ")";

        // elimina consulenze del cliente
        String sqlDeleteConsulenze =
                "DELETE FROM CONSULENZA " +
                "WHERE ID_CLIENTE IN (" +
                "   SELECT ID_CLIENTE FROM CLIENTE WHERE USERNAME = ?" +
                ")";

        // elimina pagamenti legati agli abbonamenti del cliente
        String sqlDeletePagamenti =
                "DELETE FROM PAGAMENTO " +
                "WHERE ID_ABBONAMENTO IN (" +
                "   SELECT A.ID_ABBONAMENTO " +
                "   FROM ABBONAMENTO A " +
                "   JOIN CLIENTE C ON A.ID_CLIENTE = C.ID_CLIENTE " +
                "   WHERE C.USERNAME = ?" +
                ")";

        // elimina abbonamenti del cliente
        String sqlDeleteAbbonamenti =
                "DELETE FROM ABBONAMENTO " +
                "WHERE ID_CLIENTE IN (" +
                "   SELECT ID_CLIENTE FROM CLIENTE WHERE USERNAME = ?" +
                ")";

        try (Connection conn = GestioneDB.getConnection()) {
            conn.setAutoCommit(false);

            try (PreparedStatement psCorsi = conn.prepareStatement(sqlDeleteIscrizioniCorsi);
                 PreparedStatement psCons  = conn.prepareStatement(sqlDeleteConsulenze);
                 PreparedStatement ps1     = conn.prepareStatement(sqlDeletePagamenti);
                 PreparedStatement ps2     = conn.prepareStatement(sqlDeleteAbbonamenti)) {

                // 1) iscrizioni corsi
                psCorsi.setString(1, username);
                psCorsi.executeUpdate();

                // 2) consulenze
                psCons.setString(1, username);
                psCons.executeUpdate();

                // 3) pagamenti
                ps1.setString(1, username);
                ps1.executeUpdate();

                // 4) abbonamenti
                ps2.setString(1, username);
                ps2.executeUpdate();

                conn.commit();
                logger.info("Abbonamento (e pagamenti, corsi, consulenze) disdetti per username {}",
                        username);

            } catch (SQLException e) {
                conn.rollback();
                logger.error("Errore durante disdiciAbbonamentoPerUsername per {}", username, e);
            } finally {
                conn.setAutoCommit(true);
            }

        } catch (SQLException e) {
            logger.error("Errore di connessione in disdiciAbbonamentoPerUsername", e);
        }
    }
}
