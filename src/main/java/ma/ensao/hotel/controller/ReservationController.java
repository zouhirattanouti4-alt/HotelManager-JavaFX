package ma.ensao.hotel.controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.FileChooser;
import javafx.util.StringConverter;
import ma.ensao.hotel.dao.ChambreDAO;
import ma.ensao.hotel.dao.ReservationDAO;
import ma.ensao.hotel.model.Chambre;
import ma.ensao.hotel.model.Reservation;
import ma.ensao.hotel.model.StatutReservation;
import ma.ensao.hotel.util.AlertUtil;
import ma.ensao.hotel.util.CSVExporter;
import ma.ensao.hotel.util.ToastUtil;
import ma.ensao.hotel.util.ThemeManager;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Contrôleur de la section <b>Gestion des réservations</b> (entité 2).
 * <p>
 * Chaque réservation est liée à une chambre via une ComboBox. Le montant total
 * est calculé automatiquement (nombre de nuits × prix de la chambre).
 *
 * @author ENSAO GI3
 */
public class ReservationController {

    // ------------------------- Tableau -------------------------
    @FXML private TableView<Reservation> tableReservations;
    @FXML private TableColumn<Reservation, Integer> colId;
    @FXML private TableColumn<Reservation, String> colClient;
    @FXML private TableColumn<Reservation, String> colChambre;
    @FXML private TableColumn<Reservation, LocalDate> colArrivee;
    @FXML private TableColumn<Reservation, LocalDate> colDepart;
    @FXML private TableColumn<Reservation, Long> colNuits;
    @FXML private TableColumn<Reservation, StatutReservation> colStatut;
    @FXML private TableColumn<Reservation, Double> colMontant;
    @FXML private TableColumn<Reservation, String> colPaye;

    // ------------------------- Formulaire -------------------------
    @FXML private TextField tfNom;
    @FXML private TextField tfEmail;
    @FXML private TextField tfTel;
    @FXML private ComboBox<Chambre> cbChambre;
    @FXML private DatePicker dpArrivee;
    @FXML private DatePicker dpDepart;
    @FXML private Spinner<Integer> spPersonnes;
    @FXML private ToggleGroup tgStatut;
    @FXML private RadioButton rbAttente;
    @FXML private RadioButton rbConfirmee;
    @FXML private RadioButton rbAnnulee;
    @FXML private RadioButton rbTerminee;
    @FXML private CheckBox chkPaye;
    @FXML private TextArea taRemarques;
    @FXML private Label lblMontant;

    // ------------------------- Recherche / Filtre -------------------------
    @FXML private TextField tfRecherche;
    @FXML private ComboBox<StatutReservation> cbFiltreStatut;
    @FXML private Label lblTotal;

    // ------------------------- Boutons -------------------------
    @FXML private Button btnModifier;
    @FXML private Button btnSupprimer;

    private final ReservationDAO reservationDAO = new ReservationDAO();
    private final ChambreDAO chambreDAO = new ChambreDAO();
    private final ObservableList<Reservation> donnees = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        configurerColonnes();
        configurerFormulaire();
        configurerFiltres();
        configurerSelection();
        reload();
    }

    // =====================================================================
    //  Configuration
    // =====================================================================

    private void configurerColonnes() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colClient.setCellValueFactory(new PropertyValueFactory<>("clientNom"));
        colChambre.setCellValueFactory(new PropertyValueFactory<>("chambreNumero"));
        colArrivee.setCellValueFactory(new PropertyValueFactory<>("dateArrivee"));
        colDepart.setCellValueFactory(new PropertyValueFactory<>("dateDepart"));
        colNuits.setCellValueFactory(new PropertyValueFactory<>("nombreNuits"));
        colStatut.setCellValueFactory(new PropertyValueFactory<>("statut"));
        colMontant.setCellValueFactory(new PropertyValueFactory<>("montantTotal"));
        colPaye.setCellValueFactory(new PropertyValueFactory<>("paiementLibelle"));

        colMontant.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Double montant, boolean empty) {
                super.updateItem(montant, empty);
                setText(empty || montant == null ? null : String.format("%.2f MAD", montant));
            }
        });

        // Pastille de couleur selon le statut
        colStatut.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(StatutReservation statut, boolean empty) {
                super.updateItem(statut, empty);
                if (empty || statut == null) {
                    setText(null);
                    setStyle("");
                    return;
                }
                setText(statut.getLibelle());
                String couleur = switch (statut) {
                    case CONFIRMEE -> "#1a7f37";
                    case EN_ATTENTE -> "#9a6700";
                    case ANNULEE -> "#cf222e";
                    case TERMINEE -> "#57606a";
                };
                setStyle("-fx-text-fill: " + couleur + "; -fx-font-weight: bold;");
            }
        });

        colPaye.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String paye, boolean empty) {
                super.updateItem(paye, empty);
                if (empty || paye == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(paye);
                    setStyle("Payé".equals(paye)
                            ? "-fx-text-fill: #1a7f37;"
                            : "-fx-text-fill: #cf222e;");
                }
            }
        });
    }

    private void configurerFormulaire() {
        // Affichage personnalisé des chambres dans la ComboBox
        StringConverter<Chambre> convertisseur = new StringConverter<>() {
            @Override
            public String toString(Chambre c) {
                return c == null ? "" :
                        "Ch. " + c.getNumero() + " — " + c.getType()
                        + " (" + String.format("%.0f", c.getPrixNuit()) + " MAD)";
            }
            @Override
            public Chambre fromString(String s) { return null; }
        };
        cbChambre.setConverter(convertisseur);

        spPersonnes.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 10, 1));
        spPersonnes.setEditable(true);

        rbAttente.setSelected(true);

        // Recalcul automatique du montant total
        cbChambre.valueProperty().addListener((o, a, n) -> recalculerMontant());
        dpArrivee.valueProperty().addListener((o, a, n) -> recalculerMontant());
        dpDepart.valueProperty().addListener((o, a, n) -> recalculerMontant());

        dpArrivee.setValue(LocalDate.now());
        dpDepart.setValue(LocalDate.now().plusDays(1));
    }

    private void configurerFiltres() {
        cbFiltreStatut.setItems(FXCollections.observableArrayList(StatutReservation.values()));

        FilteredList<Reservation> filtre = new FilteredList<>(donnees, r -> true);
        tfRecherche.textProperty().addListener((o, a, n) -> appliquerFiltres(filtre));
        cbFiltreStatut.valueProperty().addListener((o, a, n) -> appliquerFiltres(filtre));

        SortedList<Reservation> trie = new SortedList<>(filtre);
        trie.comparatorProperty().bind(tableReservations.comparatorProperty());
        tableReservations.setItems(trie);
    }

    private void appliquerFiltres(FilteredList<Reservation> filtre) {
        String requete = tfRecherche.getText() == null ? "" : tfRecherche.getText().toLowerCase().trim();
        StatutReservation statut = cbFiltreStatut.getValue();
        filtre.setPredicate(r -> {
            boolean correspondTexte = requete.isEmpty()
                    || (r.getClientNom() != null && r.getClientNom().toLowerCase().contains(requete))
                    || (r.getChambreNumero() != null && r.getChambreNumero().toLowerCase().contains(requete))
                    || (r.getClientEmail() != null && r.getClientEmail().toLowerCase().contains(requete));
            boolean correspondStatut = (statut == null) || r.getStatut() == statut;
            return correspondTexte && correspondStatut;
        });
        lblTotal.setText(filtre.size() + " réservation(s) affichée(s)");
    }

    private void configurerSelection() {
        var selection = tableReservations.getSelectionModel().selectedItemProperty();
        btnModifier.disableProperty().bind(selection.isNull());
        btnSupprimer.disableProperty().bind(selection.isNull());
        selection.addListener((obs, ancienne, reservation) -> {
            if (reservation != null) {
                remplirFormulaire(reservation);
            }
        });
    }

    // =====================================================================
    //  Actions CRUD
    // =====================================================================

    @FXML
    private void handleAjouter() {
        if (!validerSaisie()) return;
        Reservation r = lireFormulaire();
        if (reservationDAO.insert(r)) {
            ToastUtil.succes(tableReservations, "Réservation enregistrée pour " + r.getClientNom() + ".");
            reload();
            handleVider();
        } else {
            AlertUtil.erreur("Échec", "Impossible d'enregistrer la réservation.");
        }
    }

    @FXML
    private void handleModifier() {
        Reservation selectionnee = tableReservations.getSelectionModel().getSelectedItem();
        if (selectionnee == null) {
            AlertUtil.avertissement("Aucune sélection", "Sélectionnez une réservation à modifier.");
            return;
        }
        if (!validerSaisie()) return;
        Reservation maj = lireFormulaire();
        maj.setId(selectionnee.getId());
        if (reservationDAO.update(maj)) {
            ToastUtil.succes(tableReservations, "Réservation mise à jour.");
            reload();
            handleVider();
        } else {
            AlertUtil.erreur("Échec", "La mise à jour a échoué.");
        }
    }

    @FXML
    private void handleSupprimer() {
        Reservation selectionnee = tableReservations.getSelectionModel().getSelectedItem();
        if (selectionnee == null) return;
        boolean confirme = AlertUtil.confirmer("Confirmation",
                "Supprimer la réservation de « " + selectionnee.getClientNom() + " » ?");
        if (!confirme) return;
        if (reservationDAO.delete(selectionnee.getId())) {
            ToastUtil.succes(tableReservations, "Réservation supprimée.");
            reload();
            handleVider();
        } else {
            AlertUtil.erreur("Échec", "La suppression a échoué.");
        }
    }

    @FXML
    private void handleVider() {
        tableReservations.getSelectionModel().clearSelection();
        tfNom.clear();
        tfEmail.clear();
        tfTel.clear();
        cbChambre.getSelectionModel().clearSelection();
        cbChambre.setValue(null);
        dpArrivee.setValue(LocalDate.now());
        dpDepart.setValue(LocalDate.now().plusDays(1));
        spPersonnes.getValueFactory().setValue(1);
        rbAttente.setSelected(true);
        chkPaye.setSelected(false);
        taRemarques.clear();
        recalculerMontant();
        tfNom.requestFocus();
    }

    @FXML
    private void handleResetFiltre() {
        tfRecherche.clear();
        cbFiltreStatut.getSelectionModel().clearSelection();
        cbFiltreStatut.setValue(null);
    }

    @FXML
    private void handleExport() {
        if (donnees.isEmpty()) {
            AlertUtil.avertissement("Export impossible", "Aucune réservation à exporter.");
            return;
        }
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Exporter les réservations en CSV");
        chooser.setInitialFileName("reservations.csv");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Fichiers CSV", "*.csv"));
        File fichier = chooser.showSaveDialog(tableReservations.getScene().getWindow());
        if (fichier == null) return;

        String[] entetes = {"ID", "Client", "Email", "Téléphone", "Chambre",
                "Arrivée", "Départ", "Nuits", "Personnes", "Statut", "Montant (MAD)", "Paiement"};
        List<String[]> lignes = new ArrayList<>();
        for (Reservation r : donnees) {
            lignes.add(new String[]{
                    String.valueOf(r.getId()),
                    nz(r.getClientNom()),
                    nz(r.getClientEmail()),
                    nz(r.getClientTelephone()),
                    nz(r.getChambreNumero()),
                    String.valueOf(r.getDateArrivee()),
                    String.valueOf(r.getDateDepart()),
                    String.valueOf(r.getNombreNuits()),
                    String.valueOf(r.getNombrePersonnes()),
                    r.getStatut().getLibelle(),
                    String.format("%.2f", r.getMontantTotal()),
                    r.getPaiementLibelle()
            });
        }
        try {
            CSVExporter.exporter(fichier, entetes, lignes);
            AlertUtil.info("Export réussi",
                    lignes.size() + " réservation(s) exportée(s) vers :\n" + fichier.getAbsolutePath());
        } catch (Exception e) {
            AlertUtil.erreur("Erreur d'export", e.getMessage());
        }
    }

    /**
     * Génère et prévisualise une <b>facture</b> pour la réservation sélectionnée.
     * <p>
     * La facture s'affiche dans une boîte de dialogue (aperçu monospace) et peut
     * être enregistrée dans un fichier texte via un {@code FileChooser}.
     */
    @FXML
    private void handleFacture() {
        Reservation r = tableReservations.getSelectionModel().getSelectedItem();
        if (r == null) {
            AlertUtil.avertissement("Aucune sélection",
                    "Sélectionnez une réservation dans le tableau pour générer sa facture.");
            return;
        }
        String facture = construireFacture(r);

        // Aperçu dans un Dialog avec zone de texte monospace
        Dialog<Void> dialogue = new Dialog<>();
        dialogue.setTitle("Facture — " + r.getClientNom());
        dialogue.setHeaderText("Aperçu de la facture n° " + numeroFacture(r));

        TextArea zone = new TextArea(facture);
        zone.setEditable(false);
        zone.setWrapText(false);
        zone.setPrefRowCount(22);
        zone.setPrefColumnCount(54);
        zone.getStyleClass().add("invoice-area");

        DialogPane pane = dialogue.getDialogPane();
        pane.setContent(zone);
        ThemeManager.appliquerStyles(pane);

        ButtonType btnEnregistrer = new ButtonType("Enregistrer (.txt)", ButtonBar.ButtonData.OK_DONE);
        ButtonType btnFermer = new ButtonType("Fermer", ButtonBar.ButtonData.CANCEL_CLOSE);
        pane.getButtonTypes().addAll(btnEnregistrer, btnFermer);

        // Le bouton « Enregistrer » ne ferme pas le dialogue (consume l'événement)
        final Button bouton = (Button) pane.lookupButton(btnEnregistrer);
        bouton.addEventFilter(ActionEvent.ACTION, e -> {
            enregistrerFacture(facture, r);
            e.consume();
        });

        dialogue.showAndWait();
    }

    /** Sauvegarde la facture dans un fichier .txt choisi par l'utilisateur. */
    private void enregistrerFacture(String contenu, Reservation r) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Enregistrer la facture");
        chooser.setInitialFileName("facture_" + numeroFacture(r) + ".txt");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Fichier texte", "*.txt"));
        File fichier = chooser.showSaveDialog(tableReservations.getScene().getWindow());
        if (fichier == null) return;
        try (FileWriter w = new FileWriter(fichier, StandardCharsets.UTF_8)) {
            w.write(contenu);
            AlertUtil.info("Facture enregistrée",
                    "La facture a été enregistrée vers :\n" + fichier.getAbsolutePath());
        } catch (IOException e) {
            AlertUtil.erreur("Erreur", "Impossible d'enregistrer la facture : " + e.getMessage());
        }
    }

    /** Identifiant lisible de la facture (ex : FAC-2026-0007). */
    private String numeroFacture(Reservation r) {
        return String.format("FAC-%d-%04d", LocalDate.now().getYear(), r.getId());
    }

    /** Construit le contenu textuel mis en forme de la facture. */
    private String construireFacture(Reservation r) {
        DateTimeFormatter dateFr = DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.FRENCH);
        String ligne = "==================================================\n";
        StringBuilder sb = new StringBuilder();
        sb.append(ligne);
        sb.append("              HOTELMANAGER — ENSAO\n");
        sb.append("            Facture de réservation\n");
        sb.append(ligne);
        sb.append(String.format("Facture n°   : %s%n", numeroFacture(r)));
        sb.append(String.format("Émise le     : %s%n",
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy à HH:mm"))));
        sb.append("\n");
        sb.append("CLIENT\n");
        sb.append("--------------------------------------------------\n");
        sb.append(String.format("Nom          : %s%n", nz(r.getClientNom())));
        sb.append(String.format("Email        : %s%n", nz(r.getClientEmail())));
        sb.append(String.format("Téléphone    : %s%n", nz(r.getClientTelephone())));
        sb.append("\n");
        sb.append("SÉJOUR\n");
        sb.append("--------------------------------------------------\n");
        sb.append(String.format("Chambre      : %s%n", nz(r.getChambreNumero())));
        sb.append(String.format("Arrivée      : %s%n",
                r.getDateArrivee() == null ? "-" : r.getDateArrivee().format(dateFr)));
        sb.append(String.format("Départ       : %s%n",
                r.getDateDepart() == null ? "-" : r.getDateDepart().format(dateFr)));
        sb.append(String.format("Nuitées      : %d%n", r.getNombreNuits()));
        sb.append(String.format("Personnes    : %d%n", r.getNombrePersonnes()));
        sb.append(String.format("Statut       : %s%n", r.getStatut().getLibelle()));
        sb.append("\n");
        sb.append("DÉTAIL\n");
        sb.append("--------------------------------------------------\n");
        double prixNuit = r.getNombreNuits() == 0 ? 0 : r.getMontantTotal() / r.getNombreNuits();
        sb.append(String.format("%-30s %,10.2f MAD%n",
                r.getNombreNuits() + " nuit(s) x " + String.format("%.2f", prixNuit), r.getMontantTotal()));
        sb.append(ligne);
        sb.append(String.format("%-22s %,15.2f MAD%n", "TOTAL À PAYER", r.getMontantTotal()));
        sb.append(String.format("%-22s %15s%n", "Paiement",
                r.isPaye() ? "RÉGLÉ" : "EN ATTENTE"));
        sb.append(ligne);
        if (r.getRemarques() != null && !r.getRemarques().isBlank()) {
            sb.append("Remarques : ").append(r.getRemarques()).append("\n");
            sb.append(ligne);
        }
        sb.append("        Merci de votre confiance et à bientôt !\n");
        sb.append(ligne);
        return sb.toString();
    }

    // =====================================================================
    //  Helpers
    // =====================================================================

    /** Recharge la liste des chambres (ComboBox) et des réservations (table). */
    public void reload() {
        cbChambre.setItems(FXCollections.observableArrayList(chambreDAO.findAll()));
        donnees.setAll(reservationDAO.findAll());
        lblTotal.setText(donnees.size() + " réservation(s) affichée(s)");
    }

    private void recalculerMontant() {
        Chambre chambre = cbChambre.getValue();
        LocalDate arrivee = dpArrivee.getValue();
        LocalDate depart = dpDepart.getValue();
        if (chambre == null || arrivee == null || depart == null || !depart.isAfter(arrivee)) {
            lblMontant.setText("0.00 MAD");
            return;
        }
        long nuits = java.time.temporal.ChronoUnit.DAYS.between(arrivee, depart);
        double montant = nuits * chambre.getPrixNuit();
        lblMontant.setText(String.format("%.2f MAD  (%d nuit%s)", montant, nuits, nuits > 1 ? "s" : ""));
    }

    private boolean validerSaisie() {
        StringBuilder erreurs = new StringBuilder();
        if (tfNom.getText() == null || tfNom.getText().trim().isEmpty()) {
            erreurs.append("• Le nom du client est obligatoire.\n");
        }
        if (cbChambre.getValue() == null) {
            erreurs.append("• Vous devez sélectionner une chambre.\n");
        }
        if (dpArrivee.getValue() == null || dpDepart.getValue() == null) {
            erreurs.append("• Les dates d'arrivée et de départ sont obligatoires.\n");
        } else if (!dpDepart.getValue().isAfter(dpArrivee.getValue())) {
            erreurs.append("• La date de départ doit être postérieure à l'arrivée.\n");
        }
        if (cbChambre.getValue() != null && spPersonnes.getValue() > cbChambre.getValue().getCapacite()) {
            erreurs.append("• Le nombre de personnes (").append(spPersonnes.getValue())
                   .append(") dépasse la capacité de la chambre (")
                   .append(cbChambre.getValue().getCapacite()).append(").\n");
        }
        String email = tfEmail.getText();
        if (email != null && !email.trim().isEmpty() && !email.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
            erreurs.append("• L'adresse email n'est pas valide.\n");
        }
        if (erreurs.length() > 0) {
            AlertUtil.avertissement("Saisie incomplète", erreurs.toString());
            return false;
        }
        return true;
    }

    private Reservation lireFormulaire() {
        Reservation r = new Reservation();
        Chambre chambre = cbChambre.getValue();
        r.setChambreId(chambre.getId());
        r.setChambreNumero(chambre.getNumero());
        r.setClientNom(tfNom.getText().trim());
        r.setClientEmail(tfEmail.getText() == null ? null : tfEmail.getText().trim());
        r.setClientTelephone(tfTel.getText() == null ? null : tfTel.getText().trim());
        r.setDateArrivee(dpArrivee.getValue());
        r.setDateDepart(dpDepart.getValue());
        r.setNombrePersonnes(spPersonnes.getValue());
        r.setStatut(lireStatut());
        r.setPaye(chkPaye.isSelected());
        r.setRemarques(taRemarques.getText());
        long nuits = java.time.temporal.ChronoUnit.DAYS.between(dpArrivee.getValue(), dpDepart.getValue());
        r.setMontantTotal(nuits * chambre.getPrixNuit());
        return r;
    }

    private StatutReservation lireStatut() {
        Toggle selectionne = tgStatut.getSelectedToggle();
        if (selectionne == null || selectionne.getUserData() == null) {
            return StatutReservation.EN_ATTENTE;
        }
        try {
            return StatutReservation.valueOf(selectionne.getUserData().toString());
        } catch (IllegalArgumentException e) {
            return StatutReservation.EN_ATTENTE;
        }
    }

    private void remplirFormulaire(Reservation r) {
        tfNom.setText(r.getClientNom());
        tfEmail.setText(r.getClientEmail());
        tfTel.setText(r.getClientTelephone());
        // Sélectionne la chambre correspondante dans la ComboBox
        for (Chambre c : cbChambre.getItems()) {
            if (c.getId() == r.getChambreId()) {
                cbChambre.setValue(c);
                break;
            }
        }
        dpArrivee.setValue(r.getDateArrivee());
        dpDepart.setValue(r.getDateDepart());
        spPersonnes.getValueFactory().setValue(r.getNombrePersonnes());
        chkPaye.setSelected(r.isPaye());
        taRemarques.setText(r.getRemarques());
        switch (r.getStatut()) {
            case CONFIRMEE -> rbConfirmee.setSelected(true);
            case ANNULEE -> rbAnnulee.setSelected(true);
            case TERMINEE -> rbTerminee.setSelected(true);
            default -> rbAttente.setSelected(true);
        }
        recalculerMontant();
    }

    private String nz(String s) {
        return s == null ? "" : s;
    }
}
