package ma.ensao.hotel.controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.FileChooser;
import ma.ensao.hotel.dao.ChambreDAO;
import ma.ensao.hotel.model.Chambre;
import ma.ensao.hotel.model.TypeChambre;
import ma.ensao.hotel.util.AlertUtil;
import ma.ensao.hotel.util.CSVExporter;
import ma.ensao.hotel.util.ToastUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Contrôleur de la section <b>Gestion des chambres</b> (entité 1).
 * <p>
 * Gère le CRUD complet, la recherche/filtrage en temps réel (via
 * {@link FilteredList}) et l'export CSV.
 *
 * @author ENSAO GI3
 */
public class ChambreController {

    // ------------------------- Tableau -------------------------
    @FXML private TableView<Chambre> tableChambres;
    @FXML private TableColumn<Chambre, Integer> colId;
    @FXML private TableColumn<Chambre, String> colNumero;
    @FXML private TableColumn<Chambre, TypeChambre> colType;
    @FXML private TableColumn<Chambre, Double> colPrix;
    @FXML private TableColumn<Chambre, Integer> colCapacite;
    @FXML private TableColumn<Chambre, Integer> colEtage;
    @FXML private TableColumn<Chambre, String> colEtat;

    // ------------------------- Formulaire -------------------------
    @FXML private TextField tfNumero;
    @FXML private ComboBox<TypeChambre> cbType;
    @FXML private Spinner<Integer> spCapacite;
    @FXML private Spinner<Integer> spEtage;
    @FXML private Slider sliderPrix;
    @FXML private Label lblPrixValue;
    @FXML private CheckBox chkDisponible;
    @FXML private TextArea taDescription;

    // ------------------------- Recherche / Filtre -------------------------
    @FXML private TextField tfRecherche;
    @FXML private ComboBox<TypeChambre> cbFiltreType;
    @FXML private Label lblTotal;

    // ------------------------- Boutons -------------------------
    @FXML private Button btnModifier;
    @FXML private Button btnSupprimer;

    private final ChambreDAO chambreDAO = new ChambreDAO();
    private final ObservableList<Chambre> donnees = FXCollections.observableArrayList();

    /** Méthode appelée automatiquement par le FXMLLoader après injection. */
    @FXML
    public void initialize() {
        configurerColonnes();
        configurerFormulaire();
        configurerFiltres();
        configurerSelection();
        reload();
    }

    // =====================================================================
    //  Configuration de l'interface
    // =====================================================================

    private void configurerColonnes() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colNumero.setCellValueFactory(new PropertyValueFactory<>("numero"));
        colType.setCellValueFactory(new PropertyValueFactory<>("type"));
        colPrix.setCellValueFactory(new PropertyValueFactory<>("prixNuit"));
        colCapacite.setCellValueFactory(new PropertyValueFactory<>("capacite"));
        colEtage.setCellValueFactory(new PropertyValueFactory<>("etage"));
        colEtat.setCellValueFactory(new PropertyValueFactory<>("etatLibelle"));

        // Format monétaire de la colonne prix
        colPrix.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Double prix, boolean empty) {
                super.updateItem(prix, empty);
                setText(empty || prix == null ? null : String.format("%.2f MAD", prix));
            }
        });

        // Coloration de l'état (disponible = vert, occupée = rouge)
        colEtat.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String etat, boolean empty) {
                super.updateItem(etat, empty);
                if (empty || etat == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(etat);
                    setStyle("Disponible".equals(etat)
                            ? "-fx-text-fill: #1a7f37; -fx-font-weight: bold;"
                            : "-fx-text-fill: #cf222e; -fx-font-weight: bold;");
                }
            }
        });
    }

    private void configurerFormulaire() {
        // ComboBox des types
        cbType.setItems(FXCollections.observableArrayList(TypeChambre.values()));
        cbType.getSelectionModel().selectFirst();
        // Pré-remplissage intelligent de la capacité selon le type choisi
        cbType.valueProperty().addListener((obs, ancien, nouveau) -> {
            if (nouveau != null) {
                spCapacite.getValueFactory().setValue(nouveau.getCapaciteDefaut());
            }
        });

        // Spinners (éditables)
        spCapacite.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 10, 1));
        spEtage.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 50, 0));
        spCapacite.setEditable(true);
        spEtage.setEditable(true);

        // Slider de prix synchronisé avec son label
        lblPrixValue.setText(String.format("%.0f MAD", sliderPrix.getValue()));
        sliderPrix.valueProperty().addListener((obs, ancien, nouveau) ->
                lblPrixValue.setText(String.format("%.0f MAD", nouveau.doubleValue())));
    }

    private void configurerFiltres() {
        // Le filtre par type accepte une valeur nulle = "tous les types"
        cbFiltreType.setItems(FXCollections.observableArrayList(TypeChambre.values()));

        FilteredList<Chambre> filtre = new FilteredList<>(donnees, c -> true);
        tfRecherche.textProperty().addListener((o, a, n) -> appliquerFiltres(filtre));
        cbFiltreType.valueProperty().addListener((o, a, n) -> appliquerFiltres(filtre));

        SortedList<Chambre> trie = new SortedList<>(filtre);
        trie.comparatorProperty().bind(tableChambres.comparatorProperty());
        tableChambres.setItems(trie);

        // Met à jour le compteur quand la liste filtrée change
        filtre.addListener((javafx.collections.ListChangeListener<Chambre>) c ->
                lblTotal.setText(filtre.size() + " chambre(s) affichée(s)"));
    }

    private void appliquerFiltres(FilteredList<Chambre> filtre) {
        String requete = tfRecherche.getText() == null ? "" : tfRecherche.getText().toLowerCase().trim();
        TypeChambre type = cbFiltreType.getValue();
        filtre.setPredicate(chambre -> {
            boolean correspondTexte = requete.isEmpty()
                    || chambre.getNumero().toLowerCase().contains(requete)
                    || (chambre.getDescription() != null
                        && chambre.getDescription().toLowerCase().contains(requete));
            boolean correspondType = (type == null) || chambre.getType() == type;
            return correspondTexte && correspondType;
        });
        lblTotal.setText(filtre.size() + " chambre(s) affichée(s)");
    }

    private void configurerSelection() {
        // Désactive Modifier/Supprimer tant qu'aucune ligne n'est sélectionnée
        var selection = tableChambres.getSelectionModel().selectedItemProperty();
        btnModifier.disableProperty().bind(selection.isNull());
        btnSupprimer.disableProperty().bind(selection.isNull());

        // Remplit le formulaire au clic sur une ligne
        selection.addListener((obs, ancienne, chambre) -> {
            if (chambre != null) {
                remplirFormulaire(chambre);
            }
        });
    }

    // =====================================================================
    //  Actions CRUD
    // =====================================================================

    @FXML
    private void handleAjouter() {
        if (!validerSaisie()) return;

        Chambre chambre = lireFormulaire();
        if (chambreDAO.insert(chambre)) {
            ToastUtil.succes(tableChambres, "Chambre " + chambre.getNumero() + " ajoutée avec succès.");
            reload();
            handleVider();
        } else {
            AlertUtil.erreur("Échec de l'ajout",
                    "Impossible d'ajouter la chambre.\n"
                    + "Le numéro « " + chambre.getNumero() + " » existe peut-être déjà.");
        }
    }

    @FXML
    private void handleModifier() {
        Chambre selectionnee = tableChambres.getSelectionModel().getSelectedItem();
        if (selectionnee == null) {
            AlertUtil.avertissement("Aucune sélection", "Sélectionnez une chambre à modifier.");
            return;
        }
        if (!validerSaisie()) return;

        Chambre maj = lireFormulaire();
        maj.setId(selectionnee.getId());
        if (chambreDAO.update(maj)) {
            ToastUtil.succes(tableChambres, "Chambre mise à jour.");
            reload();
            handleVider();
        } else {
            AlertUtil.erreur("Échec", "La mise à jour a échoué (numéro en doublon ?).");
        }
    }

    @FXML
    private void handleSupprimer() {
        Chambre selectionnee = tableChambres.getSelectionModel().getSelectedItem();
        if (selectionnee == null) return;

        boolean confirme = AlertUtil.confirmer("Confirmation",
                "Supprimer la chambre " + selectionnee.getNumero() + " ?\n"
                + "⚠ Toutes les réservations liées seront également supprimées.");
        if (!confirme) return;

        if (chambreDAO.delete(selectionnee.getId())) {
            ToastUtil.succes(tableChambres, "Chambre supprimée.");
            reload();
            handleVider();
        } else {
            AlertUtil.erreur("Échec", "La suppression a échoué.");
        }
    }

    @FXML
    private void handleVider() {
        tableChambres.getSelectionModel().clearSelection();
        tfNumero.clear();
        cbType.getSelectionModel().selectFirst();
        spCapacite.getValueFactory().setValue(1);
        spEtage.getValueFactory().setValue(0);
        sliderPrix.setValue(0);
        chkDisponible.setSelected(true);
        taDescription.clear();
        tfNumero.requestFocus();
    }

    @FXML
    private void handleResetFiltre() {
        tfRecherche.clear();
        cbFiltreType.getSelectionModel().clearSelection();
        cbFiltreType.setValue(null);
    }

    @FXML
    private void handleExport() {
        if (donnees.isEmpty()) {
            AlertUtil.avertissement("Export impossible", "Aucune chambre à exporter.");
            return;
        }
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Exporter les chambres en CSV");
        chooser.setInitialFileName("chambres.csv");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Fichiers CSV", "*.csv"));
        File fichier = chooser.showSaveDialog(tableChambres.getScene().getWindow());
        if (fichier == null) return;

        String[] entetes = {"ID", "Numéro", "Type", "Prix/nuit (MAD)", "Capacité", "Étage", "État", "Description"};
        List<String[]> lignes = new ArrayList<>();
        for (Chambre c : donnees) {
            lignes.add(new String[]{
                    String.valueOf(c.getId()),
                    c.getNumero(),
                    c.getType().getLibelle(),
                    String.format("%.2f", c.getPrixNuit()),
                    String.valueOf(c.getCapacite()),
                    String.valueOf(c.getEtage()),
                    c.getEtatLibelle(),
                    c.getDescription() == null ? "" : c.getDescription()
            });
        }
        try {
            CSVExporter.exporter(fichier, entetes, lignes);
            AlertUtil.info("Export réussi",
                    lignes.size() + " chambre(s) exportée(s) vers :\n" + fichier.getAbsolutePath());
        } catch (Exception e) {
            AlertUtil.erreur("Erreur d'export", e.getMessage());
        }
    }

    // =====================================================================
    //  Helpers
    // =====================================================================

    /** Recharge les données depuis la base (exposé pour le contrôleur principal). */
    public void reload() {
        donnees.setAll(chambreDAO.findAll());
        lblTotal.setText(donnees.size() + " chambre(s) affichée(s)");
    }

    private boolean validerSaisie() {
        StringBuilder erreurs = new StringBuilder();
        if (tfNumero.getText() == null || tfNumero.getText().trim().isEmpty()) {
            erreurs.append("• Le numéro de chambre est obligatoire.\n");
        }
        if (cbType.getValue() == null) {
            erreurs.append("• Le type de chambre est obligatoire.\n");
        }
        if (sliderPrix.getValue() <= 0) {
            erreurs.append("• Le prix par nuit doit être supérieur à 0.\n");
        }
        if (erreurs.length() > 0) {
            AlertUtil.avertissement("Saisie incomplète", erreurs.toString());
            return false;
        }
        return true;
    }

    private Chambre lireFormulaire() {
        Chambre c = new Chambre();
        c.setNumero(tfNumero.getText().trim());
        c.setType(cbType.getValue());
        c.setPrixNuit(sliderPrix.getValue());
        c.setCapacite(spCapacite.getValue());
        c.setEtage(spEtage.getValue());
        c.setDisponible(chkDisponible.isSelected());
        c.setDescription(taDescription.getText());
        return c;
    }

    private void remplirFormulaire(Chambre c) {
        tfNumero.setText(c.getNumero());
        cbType.setValue(c.getType());
        spCapacite.getValueFactory().setValue(c.getCapacite());
        spEtage.getValueFactory().setValue(c.getEtage());
        sliderPrix.setValue(c.getPrixNuit());
        chkDisponible.setSelected(c.isDisponible());
        taDescription.setText(c.getDescription());
    }
}
