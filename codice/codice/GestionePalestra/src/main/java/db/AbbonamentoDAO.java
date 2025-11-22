package db;

import model.Abbonamento;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.*;
import java.util.Date;

public class AbbonamentoDAO {

    private static final Logger logger =
            LogManager.getLogger(AbbonamentoDAO.class);

    /**
     * Restituisce l'abbonamento associato a un cliente (se esiste),
     * altrimenti null.
     */
    public static Abbonamento getAbbonamentoByClienteId(int idCliente) {
        String sql = "SELECT ID_ABBONAMENTO, SCADENZA, FASCIA_ORARIA_CONSENTITA, PREZZO " +
                     "FROM ABBONAMENTO WHERE ID_CLIENTE = ?";

        try (Connection conn = GestioneDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, idCliente);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String idAbbonamento = rs.getString("ID_ABBONAMENTO");
                    Date scadenza =
                            rs.getDate("SCADENZA") != null
                                    ? new Date(rs.getDate("SCADENZA").getTime())
                                    : null;
                    String fascia = rs.getString("FASCIA_ORARIA_CONSENTITA");
                    int prezzo = rs.getInt("PREZZO");

                    String tipo = null;
                    if (idAbbonamento != null && idAbbonamento.contains("_")) {
                        tipo = idAbbonamento.substring(0, idAbbonamento.indexOf("_"));
                    }

                    Abbonamento abbonamento = Abbonamento.creaDaTipo(tipo, idCliente);
                    if (abbonamento == null) {
                        // dati corrotti / prefisso sconosciuto -> meglio loggare e restituire null
                        logger.warn("Tipo abbonamento sconosciuto ({}) per ID {}: ritorno null",
                                    tipo, idAbbonamento);
                        return null;
                    }

                    abbonamento.setIdAbbonamento(idAbbonamento);
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
     * Si occupa solo della tabella ABBONAMENTO (non delle sottoclassi specifiche).
     */
    public static void salvaAbbonamento(Abbonamento abbonamento, int idCliente) {
        if (abbonamento == null) {
            return;
        }

        if (abbonamento.getIdAbbonamento() == null ||
            abbonamento.getIdAbbonamento().isEmpty()) {

            String prefix = abbonamento.getTipo() != null
                    ? abbonamento.getTipo().toUpperCase()
                    : "ABB";
            String generatedId = prefix + "_" + System.currentTimeMillis();
            abbonamento.setIdAbbonamento(generatedId);
        }

        String sql = "INSERT INTO ABBONAMENTO " +
                     "(ID_ABBONAMENTO, SCADENZA, ID_SPOGLIATOIO, ID_CLIENTE, FASCIA_ORARIA_CONSENTITA, PREZZO) " +
                     "VALUES (?, ?, NULL, ?, ?, ?)";

        try (Connection conn = GestioneDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            java.sql.Date dataSql = null;
            if (abbonamento.getScadenza() != null) {
                dataSql = new java.sql.Date(abbonamento.getScadenza().getTime());
            }

            ps.setString(1, abbonamento.getIdAbbonamento());
            ps.setDate(2, dataSql);
            ps.setInt(3, idCliente);
            ps.setString(4, abbonamento.getFasciaOrariaConsentita());
            ps.setInt(5, abbonamento.getPrezzo());

            int rows = ps.executeUpdate();
            if (rows > 0) {
                logger.info("Abbonamento {} salvato per cliente {}",
                        abbonamento.getIdAbbonamento(), idCliente);
            } else {
                logger.warn("Nessun abbonamento salvato per cliente {}", idCliente);
            }

        } catch (SQLException e) {
            logger.error("Errore nel salvataggio abbonamento per cliente {}", idCliente, e);
        }
    }

    /**
     * Disdice l'abbonamento di un cliente / ID_CLIENTE esplicito.
     * (rimane per eventuale uso futuro)
     */
    public static void disdiciAbbonamentoPerCliente(int idCliente) {
        String sqlDeletePagamenti =
                "DELETE FROM PAGAMENTO WHERE ID_CLIENTE = ? " +
                "AND ID_ABBONAMENTO IN (SELECT ID_ABBONAMENTO FROM ABBONAMENTO WHERE ID_CLIENTE = ?)";
        String sqlDeleteAbbonamenti =
                "DELETE FROM ABBONAMENTO WHERE ID_CLIENTE = ?";

        try (Connection conn = GestioneDB.getConnection()) {
            conn.setAutoCommit(false);

            try (PreparedStatement ps1 = conn.prepareStatement(sqlDeletePagamenti);
                 PreparedStatement ps2 = conn.prepareStatement(sqlDeleteAbbonamenti)) {

                ps1.setInt(1, idCliente);
                ps1.setInt(2, idCliente);
                ps1.executeUpdate();

                ps2.setInt(1, idCliente);
                ps2.executeUpdate();

                conn.commit();
                logger.info("Abbonamento e pagamenti disdetti per cliente {}", idCliente);

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
     * Utile perché nel modello Cliente non hai l'ID_CLIENTE.
     */
    public static void disdiciAbbonamentoPerUsername(String username) {
        String sqlDeletePagamenti =
                "DELETE FROM PAGAMENTO " +
                "WHERE ID_ABBONAMENTO IN (" +
                "   SELECT A.ID_ABBONAMENTO " +
                "   FROM ABBONAMENTO A " +
                "   JOIN CLIENTE C ON A.ID_CLIENTE = C.ID_CLIENTE " +
                "   WHERE C.USERNAME = ?" +
                ")";

        String sqlDeleteAbbonamenti =
                "DELETE FROM ABBONAMENTO " +
                "WHERE ID_CLIENTE IN (" +
                "   SELECT ID_CLIENTE FROM CLIENTE WHERE USERNAME = ?" +
                ")";

        try (Connection conn = GestioneDB.getConnection()) {
            conn.setAutoCommit(false);

            try (PreparedStatement ps1 = conn.prepareStatement(sqlDeletePagamenti);
                 PreparedStatement ps2 = conn.prepareStatement(sqlDeleteAbbonamenti)) {

                ps1.setString(1, username);
                ps1.executeUpdate();

                ps2.setString(1, username);
                ps2.executeUpdate();

                conn.commit();
                logger.info("Abbonamento (e pagamenti) disdetti per username {}", username);

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
