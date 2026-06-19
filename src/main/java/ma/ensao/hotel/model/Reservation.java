package ma.ensao.hotel.model;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * Entité métier <b>Réservation</b> (seconde entité du projet).
 * <p>
 * Une réservation est rattachée à une {@link Chambre} via {@code chambreId}
 * (clé étrangère). Le champ {@code chambreNumero} est <i>transient</i> : il
 * n'existe pas en base, il est rempli par une jointure SQL pour faciliter
 * l'affichage du numéro de chambre dans la TableView.
 *
 * @author ENSAO GI3
 */
public class Reservation {

    private int id;
    private int chambreId;
    private String chambreNumero;      // rempli par jointure (affichage uniquement)
    private String clientNom;
    private String clientEmail;
    private String clientTelephone;
    private LocalDate dateArrivee;
    private LocalDate dateDepart;
    private int nombrePersonnes;
    private StatutReservation statut;
    private double montantTotal;
    private boolean paye;
    private String remarques;

    public Reservation() {
        this.statut = StatutReservation.EN_ATTENTE;
        this.nombrePersonnes = 1;
    }

    // ----------------------------- Getters / Setters -----------------------------

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getChambreId() { return chambreId; }
    public void setChambreId(int chambreId) { this.chambreId = chambreId; }

    public String getChambreNumero() { return chambreNumero; }
    public void setChambreNumero(String chambreNumero) { this.chambreNumero = chambreNumero; }

    public String getClientNom() { return clientNom; }
    public void setClientNom(String clientNom) { this.clientNom = clientNom; }

    public String getClientEmail() { return clientEmail; }
    public void setClientEmail(String clientEmail) { this.clientEmail = clientEmail; }

    public String getClientTelephone() { return clientTelephone; }
    public void setClientTelephone(String clientTelephone) { this.clientTelephone = clientTelephone; }

    public LocalDate getDateArrivee() { return dateArrivee; }
    public void setDateArrivee(LocalDate dateArrivee) { this.dateArrivee = dateArrivee; }

    public LocalDate getDateDepart() { return dateDepart; }
    public void setDateDepart(LocalDate dateDepart) { this.dateDepart = dateDepart; }

    public int getNombrePersonnes() { return nombrePersonnes; }
    public void setNombrePersonnes(int nombrePersonnes) { this.nombrePersonnes = nombrePersonnes; }

    public StatutReservation getStatut() { return statut; }
    public void setStatut(StatutReservation statut) { this.statut = statut; }

    public double getMontantTotal() { return montantTotal; }
    public void setMontantTotal(double montantTotal) { this.montantTotal = montantTotal; }

    public boolean isPaye() { return paye; }
    public void setPaye(boolean paye) { this.paye = paye; }

    public String getRemarques() { return remarques; }
    public void setRemarques(String remarques) { this.remarques = remarques; }

    // ----------------------------- Logique métier -----------------------------

    /** Nombre de nuitées entre l'arrivée et le départ (0 si dates invalides). */
    public long getNombreNuits() {
        if (dateArrivee == null || dateDepart == null) return 0;
        long nuits = ChronoUnit.DAYS.between(dateArrivee, dateDepart);
        return Math.max(nuits, 0);
    }

    /** Libellé de paiement pour affichage en colonne. */
    public String getPaiementLibelle() {
        return paye ? "Payé" : "Non payé";
    }

    @Override
    public String toString() {
        return clientNom + " — Chambre " + chambreNumero;
    }
}
