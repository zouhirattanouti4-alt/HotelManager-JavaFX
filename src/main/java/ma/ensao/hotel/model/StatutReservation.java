package ma.ensao.hotel.model;

/**
 * Cycle de vie d'une réservation.
 * <p>
 * La valeur persistée est {@link #name()} (ex : {@code "CONFIRMEE"}) ;
 * {@link #toString()} renvoie le libellé pour l'affichage.
 *
 * @author ENSAO GI3
 */
public enum StatutReservation {

    EN_ATTENTE("En attente"),
    CONFIRMEE("Confirmée"),
    ANNULEE("Annulée"),
    TERMINEE("Terminée");

    private final String libelle;

    StatutReservation(String libelle) {
        this.libelle = libelle;
    }

    public String getLibelle() {
        return libelle;
    }

    @Override
    public String toString() {
        return libelle;
    }
}
