package ma.ensao.hotel.model;

import java.util.Objects;

/**
 * Entité métier <b>Chambre</b> (première entité du projet).
 * <p>
 * Simple POJO encapsulé : chaque champ correspond à une colonne de la table
 * {@code chambres}. Les getters sont exploités directement par les
 * {@code PropertyValueFactory} des TableView.
 *
 * @author ENSAO GI3
 */
public class Chambre {

    private int id;
    private String numero;
    private TypeChambre type;
    private double prixNuit;
    private int capacite;
    private int etage;
    private boolean disponible;
    private String description;

    public Chambre() {
        // Valeurs par défaut cohérentes pour une nouvelle chambre
        this.type = TypeChambre.SIMPLE;
        this.capacite = 1;
        this.disponible = true;
    }

    public Chambre(int id, String numero, TypeChambre type, double prixNuit,
                   int capacite, int etage, boolean disponible, String description) {
        this.id = id;
        this.numero = numero;
        this.type = type;
        this.prixNuit = prixNuit;
        this.capacite = capacite;
        this.etage = etage;
        this.disponible = disponible;
        this.description = description;
    }

    // ----------------------------- Getters / Setters -----------------------------

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getNumero() { return numero; }
    public void setNumero(String numero) { this.numero = numero; }

    public TypeChambre getType() { return type; }
    public void setType(TypeChambre type) { this.type = type; }

    public double getPrixNuit() { return prixNuit; }
    public void setPrixNuit(double prixNuit) { this.prixNuit = prixNuit; }

    public int getCapacite() { return capacite; }
    public void setCapacite(int capacite) { this.capacite = capacite; }

    public int getEtage() { return etage; }
    public void setEtage(int etage) { this.etage = etage; }

    public boolean isDisponible() { return disponible; }
    public void setDisponible(boolean disponible) { this.disponible = disponible; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    /** Libellé d'état utilisé directement dans une colonne de TableView. */
    public String getEtatLibelle() {
        return disponible ? "Disponible" : "Occupée";
    }

    /**
     * Affichage par défaut (fallback pour les ComboBox sans convertisseur).
     */
    @Override
    public String toString() {
        return "Chambre " + numero + " (" + type + ")";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Chambre)) return false;
        return id == ((Chambre) o).id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
