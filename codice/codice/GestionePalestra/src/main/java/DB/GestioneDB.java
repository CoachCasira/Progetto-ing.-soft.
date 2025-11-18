package DB;


import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class GestioneDB {
	
	 // Il file del DB verrà creato nella cartella "data" del progetto
    private static final String JDBC_URL = "jdbc:h2:./data/palestra;AUTO_SERVER=TRUE";
    private static final String JDBC_USER = "sa";
    private static final String JDBC_PASSWORD = "";

    static {
        try {
            // Carica il driver H2
            Class.forName("org.h2.Driver");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Impossibile caricare il driver H2", e);
        }
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(JDBC_URL, JDBC_USER, JDBC_PASSWORD);
    }

}
