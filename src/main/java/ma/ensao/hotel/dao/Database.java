package ma.ensao.hotel.dao;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Gestionnaire central de la connexion MySQL (JDBC).
 * <p>
 * Implémenté en <b>Singleton</b> : une seule connexion partagée est maintenue
 * pendant toute la durée de vie de l'application, ce qui est adapté à une
 * application de bureau mono-utilisateur. Les paramètres de connexion sont lus
 * depuis le fichier {@code /database.properties} (classpath), avec des valeurs
 * de repli si le fichier est absent.
 *
 * @author ENSAO GI3
 */
public final class Database {

    private static Database instance;

    private Connection connection;
    private String url;
    private String user;
    private String password;

    private Database() {
        chargerConfiguration();
    }

    /** Point d'accès unique au gestionnaire de base de données. */
    public static synchronized Database getInstance() {
        if (instance == null) {
            instance = new Database();
        }
        return instance;
    }

    /** Charge les identifiants depuis database.properties (ou valeurs par défaut). */
    private void chargerConfiguration() {
        Properties props = new Properties();
        try (InputStream in = Database.class.getResourceAsStream("/database.properties")) {
            if (in != null) {
                props.load(in);
            } else {
                System.err.println("[Database] database.properties introuvable, utilisation des valeurs par défaut.");
            }
        } catch (IOException e) {
            System.err.println("[Database] Erreur de lecture de database.properties : " + e.getMessage());
        }

        this.url = props.getProperty("db.url",
                "jdbc:mysql://localhost:3306/hotel_db"
                + "?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true&characterEncoding=UTF-8");
        this.user = props.getProperty("db.user", "root");
        this.password = props.getProperty("db.password", "");
    }

    /**
     * Renvoie la connexion active, en la (ré)ouvrant si nécessaire.
     *
     * @throws SQLException si la connexion ne peut être établie
     */
    public Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            try {
                Class.forName("com.mysql.cj.jdbc.Driver");
            } catch (ClassNotFoundException e) {
                throw new SQLException("Driver JDBC MySQL introuvable. "
                        + "Vérifiez la dépendance mysql-connector-j.", e);
            }
            connection = DriverManager.getConnection(url, user, password);
        }
        return connection;
    }

    /** Teste la disponibilité de la base (utile au démarrage). */
    public boolean testerConnexion() {
        try {
            return getConnection() != null && getConnection().isValid(3);
        } catch (SQLException e) {
            System.err.println("[Database] Connexion impossible : " + e.getMessage());
            return false;
        }
    }

    /** Ferme proprement la connexion (appelé à l'arrêt de l'application). */
    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException ignored) {
            // Rien à faire : on quitte l'application
        }
    }
}
