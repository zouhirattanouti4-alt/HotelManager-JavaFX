package ma.ensao.hotel;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import ma.ensao.hotel.dao.Database;
import ma.ensao.hotel.util.AlertUtil;
import ma.ensao.hotel.util.ThemeManager;

/**
 * Point d'entrée de l'application <b>HotelManager</b>.
 * <p>
 * Charge la vue principale (FXML), applique la feuille de style et le thème,
 * teste la connexion MySQL au démarrage et ferme proprement la connexion à
 * l'arrêt.
 *
 * @author ENSAO GI3
 */
public class MainApp extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        // Vérification de la connexion à la base au lancement
        if (!Database.getInstance().testerConnexion()) {
            AlertUtil.avertissement("Connexion à la base de données",
                    "Impossible de se connecter à MySQL.\n\n"
                    + "Vérifiez que :\n"
                    + "• le serveur MySQL est démarré ;\n"
                    + "• la base « hotel_db » a été créée (script init_db.sql) ;\n"
                    + "• les identifiants dans database.properties sont corrects.\n\n"
                    + "L'application va s'ouvrir, mais les données ne s'afficheront pas.");
        }

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/ma/ensao/hotel/view/MainView.fxml"));
        Parent root = loader.load();

        Scene scene = new Scene(root, 1240, 780);
        scene.getStylesheets().add(
                getClass().getResource("/ma/ensao/hotel/css/styles.css").toExternalForm());

        // Thème par défaut
        ThemeManager.appliquerAccent(root, ThemeManager.ACCENT_DEFAUT);

        stage.setTitle("HotelManager — Gestion Hôtelière | ENSAO GI3");
        stage.setScene(scene);
        stage.setMinWidth(1080);
        stage.setMinHeight(700);
        stage.show();
    }

    @Override
    public void stop() {
        // Libère la connexion MySQL à la fermeture
        Database.getInstance().close();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
