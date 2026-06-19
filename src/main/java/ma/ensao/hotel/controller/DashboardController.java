package ma.ensao.hotel.controller;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.PauseTransition;
import javafx.animation.Timeline;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ProgressIndicator;
import javafx.util.Duration;
import ma.ensao.hotel.dao.ChambreDAO;
import ma.ensao.hotel.dao.ReservationDAO;
import ma.ensao.hotel.model.Chambre;
import ma.ensao.hotel.model.Reservation;
import ma.ensao.hotel.model.StatutReservation;
import ma.ensao.hotel.model.TypeChambre;
import ma.ensao.hotel.util.ThemeManager;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controleur du <b>Tableau de bord</b>.
 * <p>
 * Agrege les indicateurs chiffres (avec compteurs anime), alimente les
 * graphiques (PieChart / BarChart), affiche le taux d'occupation via une
 * ProgressBar, genere des <i>analyses intelligentes</i> a partir des donnees
 * et gere la personnalisation de l'interface (ColorPicker).
 *
 * @author ENSAO GI3
 */
public class DashboardController {

    // ------------------------- Indicateurs -------------------------
    @FXML private Label lblTotalChambres;
    @FXML private Label lblChambresDispo;
    @FXML private Label lblTotalReservations;
    @FXML private Label lblRevenu;
    @FXML private Label lblTauxOccupation;
    @FXML private ProgressBar pbOccupation;
    @FXML private ProgressIndicator piLoading;

    // ------------------------- Graphiques -------------------------
    @FXML private PieChart chartTypes;
    @FXML private BarChart<String, Number> chartStatuts;

    // ------------------------- Listes -------------------------
    @FXML private ListView<String> lvActivite;
    @FXML private ListView<String> lvInsights;

    // ------------------------- Personnalisation -------------------------
    @FXML private ColorPicker cpTheme;

    private final ChambreDAO chambreDAO = new ChambreDAO();
    private final ReservationDAO reservationDAO = new ReservationDAO();

    @FXML
    public void initialize() {
        cpTheme.setValue(ThemeManager.ACCENT_DEFAUT);
        piLoading.setVisible(false);
        reload();
    }

    /** Recalcule l'ensemble des indicateurs, graphiques et analyses. */
    public void reload() {
        List<Chambre> chambres = chambreDAO.findAll();
        List<Reservation> reservations = reservationDAO.findAll();
        chargerIndicateurs(chambres, reservations);
        chargerGraphiqueTypes();
        chargerGraphiqueStatuts();
        chargerActivite(reservations);
        chargerInsights(chambres, reservations);
    }

    // =====================================================================
    //  Indicateurs chiffres (avec animation)
    // =====================================================================

    private void chargerIndicateurs(List<Chambre> chambres, List<Reservation> reservations) {
        int totalChambres = chambres.size();
        int dispo = (int) chambres.stream().filter(Chambre::isDisponible).count();
        int totalReservations = reservations.size();
        double revenu = reservations.stream()
                .filter(r -> r.getStatut() == StatutReservation.CONFIRMEE
                          || r.getStatut() == StatutReservation.TERMINEE)
                .mapToDouble(Reservation::getMontantTotal).sum();
        int occupees = totalChambres - dispo;
        double taux = totalChambres == 0 ? 0 : (double) occupees / totalChambres;

        animerValeur(lblTotalChambres, totalChambres, "%.0f");
        animerValeur(lblChambresDispo, dispo, "%.0f");
        animerValeur(lblTotalReservations, totalReservations, "%.0f");
        animerValeur(lblRevenu, revenu, "%,.2f MAD");
        animerValeur(lblTauxOccupation, taux * 100, "%.0f %%");

        // Animation fluide de la barre de progression
        Timeline tlBarre = new Timeline(
                new KeyFrame(Duration.millis(700),
                        new KeyValue(pbOccupation.progressProperty(), taux)));
        tlBarre.play();
    }

    /** Anime un Label de 0 jusqu'a la valeur cible (effet « compteur »). */
    private void animerValeur(Label label, double cible, String format) {
        DoubleProperty valeur = new SimpleDoubleProperty(0);
        valeur.addListener((o, a, n) -> label.setText(String.format(format, n.doubleValue())));
        Timeline tl = new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(valeur, 0)),
                new KeyFrame(Duration.millis(700), new KeyValue(valeur, cible)));
        tl.play();
    }

    // =====================================================================
    //  Graphiques
    // =====================================================================

    private void chargerGraphiqueTypes() {
        chartTypes.getData().clear();
        Map<TypeChambre, Integer> repartition = chambreDAO.compterParType();
        repartition.forEach((type, nombre) -> {
            if (nombre > 0) {
                chartTypes.getData().add(
                        new PieChart.Data(type.getLibelle() + " (" + nombre + ")", nombre));
            }
        });
    }

    private void chargerGraphiqueStatuts() {
        chartStatuts.getData().clear();
        XYChart.Series<String, Number> serie = new XYChart.Series<>();
        serie.setName("Reservations");
        Map<StatutReservation, Integer> repartition = reservationDAO.compterParStatut();
        repartition.forEach((statut, nombre) ->
                serie.getData().add(new XYChart.Data<>(statut.getLibelle(), nombre)));
        chartStatuts.getData().add(serie);
    }

    private void chargerActivite(List<Reservation> reservations) {
        ObservableList<String> lignes = FXCollections.observableArrayList();
        reservations.stream().limit(8).forEach(r -> lignes.add(
                "• " + r.getClientNom()
                + "  —  Ch. " + r.getChambreNumero()
                + "  —  " + r.getStatut().getLibelle()
                + "  (" + String.format("%.0f MAD", r.getMontantTotal()) + ")"));
        if (lignes.isEmpty()) {
            lignes.add("Aucune reservation enregistree pour le moment.");
        }
        lvActivite.setItems(lignes);
    }

    // =====================================================================
    //  Analyses intelligentes (genere a partir des donnees)
    // =====================================================================

    private void chargerInsights(List<Chambre> chambres, List<Reservation> reservations) {
        ObservableList<String> insights = FXCollections.observableArrayList();

        int total = chambres.size();
        long occupees = chambres.stream().filter(c -> !c.isDisponible()).count();
        double taux = total == 0 ? 0 : (double) occupees / total;
        String pct = String.format("%.0f %%", taux * 100);

        if (total == 0) {
            insights.add("Ajoutez des chambres pour activer les analyses.");
            lvInsights.setItems(insights);
            return;
        }

        // 1. Niveau d'occupation
        if (taux >= 0.8) {
            insights.add("🔴 Occupation elevee (" + pct + ") — envisagez d'ajuster vos tarifs a la hausse.");
        } else if (taux <= 0.3) {
            insights.add("🟢 Forte disponibilite (" + pct + " occupe) — moment ideal pour des promotions.");
        } else {
            insights.add("🟡 Occupation moderee (" + pct + ").");
        }

        // 2. Reservations en attente
        long attente = reservations.stream()
                .filter(r -> r.getStatut() == StatutReservation.EN_ATTENTE).count();
        if (attente > 0) {
            insights.add("⏳ " + attente + " reservation(s) en attente de confirmation.");
        } else {
            insights.add("✅ Aucune reservation en attente.");
        }

        // 3. Type de chambre le plus rentable
        Map<Integer, Chambre> parId = new HashMap<>();
        for (Chambre c : chambres) {
            parId.put(c.getId(), c);
        }
        Map<TypeChambre, Double> revParType = new HashMap<>();
        for (Reservation r : reservations) {
            if (r.getStatut() == StatutReservation.CONFIRMEE
                    || r.getStatut() == StatutReservation.TERMINEE) {
                Chambre c = parId.get(r.getChambreId());
                if (c != null) {
                    revParType.merge(c.getType(), r.getMontantTotal(), Double::sum);
                }
            }
        }
        revParType.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .ifPresent(e -> insights.add("💰 Type le plus rentable : " + e.getKey().getLibelle()
                        + " (" + String.format("%,.0f MAD", e.getValue()) + ")."));

        // 4. Duree moyenne de sejour
        double moyenneNuits = reservations.stream()
                .mapToLong(Reservation::getNombreNuits).average().orElse(0);
        if (moyenneNuits > 0) {
            insights.add("🛏 Duree moyenne de sejour : "
                    + String.format("%.1f", moyenneNuits) + " nuit(s).");
        }

        // 5. Tarif moyen des chambres
        double tarifMoyen = chambres.stream()
                .mapToDouble(Chambre::getPrixNuit).average().orElse(0);
        insights.add("🏷 Tarif moyen : " + String.format("%.0f MAD/nuit", tarifMoyen) + ".");

        lvInsights.setItems(insights);
    }

    // =====================================================================
    //  Actions
    // =====================================================================

    @FXML
    private void handleRefresh() {
        // Affiche brievement l'indicateur de chargement (ProgressIndicator)
        piLoading.setVisible(true);
        PauseTransition pause = new PauseTransition(Duration.millis(450));
        pause.setOnFinished(e -> {
            reload();
            piLoading.setVisible(false);
        });
        pause.play();
    }

    @FXML
    private void handleAppliquerTheme() {
        if (cpTheme.getScene() != null) {
            ThemeManager.appliquerAccent(cpTheme.getScene().getRoot(), cpTheme.getValue());
        }
    }

    @FXML
    private void handleResetTheme() {
        cpTheme.setValue(ThemeManager.ACCENT_DEFAUT);
        if (cpTheme.getScene() != null) {
            ThemeManager.appliquerAccent(cpTheme.getScene().getRoot(), ThemeManager.ACCENT_DEFAUT);
        }
    }
}
