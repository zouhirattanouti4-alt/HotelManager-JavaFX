package ma.ensao.hotel.util;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;

import java.util.Optional;

/**
 * Fabrique centralisée de boîtes de dialogue {@link Alert}.
 * <p>
 * Centraliser ces appels garantit une présentation homogène des messages
 * (information, erreur, confirmation) dans toute l'application.
 *
 * @author ENSAO GI3
 */
public final class AlertUtil {

    private AlertUtil() {
        // Classe utilitaire : pas d'instanciation
    }

    public static void info(String titre, String message) {
        afficher(Alert.AlertType.INFORMATION, titre, message);
    }

    public static void erreur(String titre, String message) {
        afficher(Alert.AlertType.ERROR, titre, message);
    }

    public static void avertissement(String titre, String message) {
        afficher(Alert.AlertType.WARNING, titre, message);
    }

    /**
     * Affiche une demande de confirmation OUI / NON.
     *
     * @return {@code true} si l'utilisateur a confirmé.
     */
    public static boolean confirmer(String titre, String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(titre);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.getButtonTypes().setAll(ButtonType.YES, ButtonType.NO);
        Optional<ButtonType> resultat = alert.showAndWait();
        return resultat.isPresent() && resultat.get() == ButtonType.YES;
    }

    private static void afficher(Alert.AlertType type, String titre, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(titre);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
