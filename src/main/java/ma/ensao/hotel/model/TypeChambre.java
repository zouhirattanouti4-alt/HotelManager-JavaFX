package ma.ensao.hotel.model;

/**
 * Énumération des types de chambres proposés par l'hôtel.
 * <p>
 * Chaque type embarque un libellé lisible (affiché dans l'IHM) et une capacité
 * par défaut servant à pré-remplir le formulaire de saisie. La méthode
 * {@link #name()} (ex : {@code "SUITE"}) reste la valeur persistée en base,
 * tandis que {@link #toString()} renvoie le libellé pour les ComboBox/TableView.
 *
 * @author ENSAO GI3
 */
public enum TypeChambre {

    SIMPLE("Simple", 1),
    DOUBLE("Double", 2),
    SUITE("Suite", 3),
    FAMILIALE("Familiale", 5),
    DELUXE("Deluxe", 2);

    private final String libelle;
    private final int capaciteDefaut;

    TypeChambre(String libelle, int capaciteDefaut) {
        this.libelle = libelle;
        this.capaciteDefaut = capaciteDefaut;
    }

    public String getLibelle() {
        return libelle;
    }

    public int getCapaciteDefaut() {
        return capaciteDefaut;
    }

    /** Affichage convivial dans les contrôles JavaFX. */
    @Override
    public String toString() {
        return libelle;
    }
}
