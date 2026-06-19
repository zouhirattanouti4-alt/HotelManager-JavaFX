package ma.ensao.hotel.util;

import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.animation.TranslateTransition;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.stage.Popup;
import javafx.stage.Window;
import javafx.util.Duration;

/**
 * Notifications « toast » non bloquantes.
 * <p>
 * Contrairement aux {@link AlertUtil} (modales, qui interrompent l'utilisateur),
 * les toasts apparaissent en bas à droite de la fenêtre, restent visibles
 * quelques secondes puis disparaissent en fondu. Idéal pour confirmer une
 * action réussie sans casser le flux de travail.
 * <p>
 * Implémentation auto-portée via {@link Popup} : aucune modification des vues
 * FXML n'est nécessaire.
 *
 * @author ENSAO GI3
 */
public final class ToastUtil {

    private ToastUtil() {
    }

    /** Toast de succès (vert). */
    public static void succes(Node ancre, String message) {
        afficher(ancre, message, "#22c55e", "✓");
    }

    /** Toast d'information (bleu). */
    public static void info(Node ancre, String message) {
        afficher(ancre, message, "#38bdf8", "ℹ");
    }

    /** Toast d'erreur (rouge). */
    public static void erreur(Node ancre, String message) {
        afficher(ancre, message, "#ef4444", "✕");
    }

    private static void afficher(Node ancre, String message, String couleur, String icone) {
        if (ancre == null || ancre.getScene() == null || ancre.getScene().getWindow() == null) {
            return;
        }
        Window fenetre = ancre.getScene().getWindow();

        Label pastille = new Label(icone);
        pastille.setStyle(
                "-fx-min-width: 26; -fx-min-height: 26; -fx-alignment: center;"
                + "-fx-background-color: " + couleur + ";"
                + "-fx-background-radius: 13;"
                + "-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 13px;");

        Label texte = new Label(message);
        texte.setWrapText(true);
        texte.setMaxWidth(280);
        texte.setStyle("-fx-text-fill: #f1f5f9; -fx-font-size: 13.5px;");

        HBox boite = new HBox(12, pastille, texte);
        boite.setAlignment(Pos.CENTER_LEFT);
        boite.setStyle(
                "-fx-background-color: linear-gradient(to bottom right, #1f2937, #111827);"
                + "-fx-background-radius: 14;"
                + "-fx-padding: 14 20 14 14;"
                + "-fx-border-color: " + couleur + ";"
                + "-fx-border-width: 0 0 0 4;"
                + "-fx-border-radius: 14;"
                + "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.45), 22, 0.2, 0, 8);");

        Popup popup = new Popup();
        popup.getContent().add(boite);
        popup.setAutoFix(true);
        popup.show(fenetre);

        // Positionnement en bas à droite de la fenêtre
        double largeur = boite.getWidth() > 0 ? boite.getWidth() : 320;
        double hauteur = boite.getHeight() > 0 ? boite.getHeight() : 60;
        popup.setX(fenetre.getX() + fenetre.getWidth() - largeur - 28);
        popup.setY(fenetre.getY() + fenetre.getHeight() - hauteur - 32);

        // Animation d'entrée : glissement + fondu
        boite.setOpacity(0);
        boite.setTranslateY(20);
        FadeTransition entreeFade = new FadeTransition(Duration.millis(220), boite);
        entreeFade.setFromValue(0);
        entreeFade.setToValue(1);
        TranslateTransition entreeSlide = new TranslateTransition(Duration.millis(220), boite);
        entreeSlide.setFromY(20);
        entreeSlide.setToY(0);
        entreeFade.play();
        entreeSlide.play();

        // Disparition après 2,6 s
        PauseTransition attente = new PauseTransition(Duration.seconds(2.6));
        attente.setOnFinished(e -> {
            FadeTransition sortie = new FadeTransition(Duration.millis(280), boite);
            sortie.setFromValue(1);
            sortie.setToValue(0);
            sortie.setOnFinished(ev -> popup.hide());
            sortie.play();
        });
        attente.play();
    }
}
