package ma.ensao.hotel.controller;

import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.BorderPane;
import javafx.util.Duration;
import ma.ensao.hotel.util.AlertUtil;
import ma.ensao.hotel.util.ToastUtil;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Controleur de la fenetre principale.
 */
public class MainController {

    @FXML private BorderPane rootPane;

    @FXML private Node chambres;
    @FXML private Node reservations;
    @FXML private Node dashboard;
    @FXML private ChambreController chambresController;
    @FXML private ReservationController reservationsController;
    @FXML private DashboardController dashboardController;

    @FXML private Button navChambres;
    @FXML private Button navReservations;
    @FXML private Button navDashboard;

    @FXML private Label lblClock;
    @FXML private Label lblDate;
    @FXML private Label lblGreeting;
    @FXML private ToggleButton btnTheme;

    private static final DateTimeFormatter FMT_HEURE = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final DateTimeFormatter FMT_DATE =
            DateTimeFormatter.ofPattern("EEEE d MMMM yyyy", Locale.FRENCH);

    @FXML
    public void initialize() {
        demarrerHorloge();
        majSalutationEtDate();
        afficherVue(0);
    }

    private void demarrerHorloge() {
        // ... (Le code de l'horloge reste identique)
        javafx.animation.Timeline horloge = new javafx.animation.Timeline(new javafx.animation.KeyFrame(Duration.seconds(1),
                e -> lblClock.setText(LocalTime.now().format(FMT_HEURE))));
        horloge.setCycleCount(javafx.animation.Timeline.INDEFINITE);
        horloge.play();
        lblClock.setText(LocalTime.now().format(FMT_HEURE));
    }

    private void majSalutationEtDate() {
        int h = LocalTime.now().getHour();
        String salutation = (h < 12) ? "Bonjour" : (h < 18 ? "Bon apres-midi" : "Bonsoir");
        lblGreeting.setText(salutation + ", bienvenue 👋");
        lblDate.setText(LocalDate.now().format(FMT_DATE));
    }

    // =====================================================================
    //  Navigation avec Animations Premium
    // =====================================================================

    private void afficherVue(int index) {
        masquerVue(chambres);
        masquerVue(reservations);
        masquerVue(dashboard);

        navChambres.getStyleClass().remove("nav-active");
        navReservations.getStyleClass().remove("nav-active");
        navDashboard.getStyleClass().remove("nav-active");

        switch (index) {
            case 1 -> {
                navReservations.getStyleClass().add("nav-active");
                animerApparition(reservations);
                reservationsController.reload();
            }
            case 2 -> {
                navDashboard.getStyleClass().add("nav-active");
                animerApparition(dashboard);
                dashboardController.reload();
            }
            default -> {
                navChambres.getStyleClass().add("nav-active");
                animerApparition(chambres);
            }
        }
    }

    private void masquerVue(Node noeud) {
        noeud.setVisible(false);
        noeud.setManaged(false);
    }

    private void animerApparition(Node noeud) {
        noeud.setVisible(true);
        noeud.setManaged(true);
        noeud.setOpacity(0);
        noeud.setTranslateY(30); // Décalage pour l'effet de glissement

        FadeTransition ft = new FadeTransition(Duration.millis(350), noeud);
        ft.setToValue(1.0);

        TranslateTransition tt = new TranslateTransition(Duration.millis(350), noeud);
        tt.setToY(0);

        ParallelTransition pt = new ParallelTransition(ft, tt);
        pt.play();
    }

    @FXML private void handleNavChambres()     { afficherVue(0); }
    @FXML private void handleNavReservations() { afficherVue(1); }
    @FXML private void handleNavDashboard()    { afficherVue(2); }

    // =====================================================================
    //  Bascule de theme clair / sombre
    // =====================================================================

    @FXML
    private void handleToggleTheme() {
        appliquerTheme(btnTheme.isSelected());
    }

    @FXML
    private void handleToggleThemeMenu() {
        btnTheme.setSelected(!btnTheme.isSelected());
        appliquerTheme(btnTheme.isSelected());
    }

    private void appliquerTheme(boolean sombre) {
        if (sombre) {
            if (!rootPane.getStyleClass().contains("dark")) {
                rootPane.getStyleClass().add("dark");
            }
            btnTheme.setText("☀");
        } else {
            rootPane.getStyleClass().remove("dark");
            btnTheme.setText("🌙");
        }
    }

    // =====================================================================
    //  Menus
    // =====================================================================

    @FXML
    private void handleActualiser() {
        chambresController.reload();
        reservationsController.reload();
        dashboardController.reload();
        ToastUtil.succes(rootPane, "Données rechargées depuis la base.");
    }

    @FXML
    private void handleQuitter() {
        if (AlertUtil.confirmer("Quitter", "Voulez-vous vraiment fermer l'application ?")) {
            Platform.exit();
        }
    }

    @FXML private void handleAfficherChambres()     { afficherVue(0); }
    @FXML private void handleAfficherReservations()  { afficherVue(1); }
    @FXML private void handleAfficherDashboard()     { afficherVue(2); }

    @FXML
    private void handleGuide() {
        AlertUtil.info("Guide d'utilisation",
                "1. « Chambres » : ajoutez / modifiez / supprimez vos chambres.\n"
                        + "2. « Réservations » : créez une réservation liée à une chambre.\n"
                        + "3. « Tableau de bord » : consultez les statistiques et analyses.\n\n"
                        + "Astuces :\n"
                        + "• Utilisez la recherche et les filtres pour retrouver un élément.\n"
                        + "• Le bouton 🌙 / ☀ bascule le thème clair / sombre.\n"
                        + "• La couleur d'accent (tableau de bord) recolore toute l'application.\n"
                        + "• « Exporter CSV » sauvegarde vos données.");
    }

    @FXML
    private void handleApropos() {
        AlertUtil.info("A propos",
                "HotelManager — Suite de gestion hôtelière\n"
                        + "Mini-projet JavaFX • ENSAO • Filière GI3\n"
                        + "Module : Développement Java IHM (2025/2026)\n"
                        + "Encadrante : Mme Douae EL HILA\n\n"
                        + "Technologies : JavaFX 21 • MySQL • JDBC • Architecture MVC / DAO");
    }
}