package ma.ensao.hotel.dao;

import ma.ensao.hotel.model.Reservation;
import ma.ensao.hotel.model.StatutReservation;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * DAO de l'entité {@link Reservation}.
 * <p>
 * Les lectures utilisent une jointure {@code LEFT JOIN} avec la table
 * {@code chambres} afin de récupérer le numéro de chambre associé pour
 * l'affichage, sans requête supplémentaire (évite le problème N+1).
 *
 * @author ENSAO GI3
 */
public class ReservationDAO implements DAO<Reservation> {

    private final Database db = Database.getInstance();

    private static final String SELECT_BASE =
            "SELECT r.*, c.numero AS chambre_numero "
            + "FROM reservations r "
            + "LEFT JOIN chambres c ON r.chambre_id = c.id ";

    // ------------------------------------------------------------------ READ

    @Override
    public List<Reservation> findAll() {
        List<Reservation> reservations = new ArrayList<>();
        String sql = SELECT_BASE + "ORDER BY r.date_arrivee DESC";
        try (Statement st = db.getConnection().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                reservations.add(mapper(rs));
            }
        } catch (SQLException e) {
            System.err.println("[ReservationDAO] findAll : " + e.getMessage());
        }
        return reservations;
    }

    @Override
    public Optional<Reservation> findById(int id) {
        String sql = SELECT_BASE + "WHERE r.id = ?";
        try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapper(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("[ReservationDAO] findById : " + e.getMessage());
        }
        return Optional.empty();
    }

    // ----------------------------------------------------------------- WRITE

    @Override
    public boolean insert(Reservation r) {
        String sql = "INSERT INTO reservations "
                + "(chambre_id, client_nom, client_email, client_telephone, "
                + "date_arrivee, date_depart, nombre_personnes, statut, "
                + "montant_total, paye, remarques) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = db.getConnection()
                .prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            remplirParametres(ps, r);
            if (ps.executeUpdate() == 0) return false;
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    r.setId(keys.getInt(1));
                }
            }
            return true;
        } catch (SQLException e) {
            System.err.println("[ReservationDAO] insert : " + e.getMessage());
            return false;
        }
    }

    @Override
    public boolean update(Reservation r) {
        String sql = "UPDATE reservations SET "
                + "chambre_id = ?, client_nom = ?, client_email = ?, client_telephone = ?, "
                + "date_arrivee = ?, date_depart = ?, nombre_personnes = ?, statut = ?, "
                + "montant_total = ?, paye = ?, remarques = ? WHERE id = ?";
        try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
            remplirParametres(ps, r);
            ps.setInt(12, r.getId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[ReservationDAO] update : " + e.getMessage());
            return false;
        }
    }

    @Override
    public boolean delete(int id) {
        String sql = "DELETE FROM reservations WHERE id = ?";
        try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[ReservationDAO] delete : " + e.getMessage());
            return false;
        }
    }

    // ------------------------------------------------------------ STATISTIQUES

    public int compterTotal() {
        return compter("SELECT COUNT(*) FROM reservations");
    }

    /** Chiffre d'affaires des réservations confirmées ou terminées. */
    public double chiffreAffaires() {
        String sql = "SELECT COALESCE(SUM(montant_total), 0) "
                + "FROM reservations WHERE statut IN ('CONFIRMEE', 'TERMINEE')";
        try (Statement st = db.getConnection().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            if (rs.next()) return rs.getDouble(1);
        } catch (SQLException e) {
            System.err.println("[ReservationDAO] chiffreAffaires : " + e.getMessage());
        }
        return 0;
    }

    /** Répartition des réservations par statut (pour le BarChart). */
    public Map<StatutReservation, Integer> compterParStatut() {
        Map<StatutReservation, Integer> repartition = new LinkedHashMap<>();
        for (StatutReservation s : StatutReservation.values()) {
            repartition.put(s, 0);
        }
        String sql = "SELECT statut, COUNT(*) AS total FROM reservations GROUP BY statut";
        try (Statement st = db.getConnection().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                try {
                    repartition.put(StatutReservation.valueOf(rs.getString("statut")), rs.getInt("total"));
                } catch (IllegalArgumentException ignored) {
                }
            }
        } catch (SQLException e) {
            System.err.println("[ReservationDAO] compterParStatut : " + e.getMessage());
        }
        return repartition;
    }

    // -------------------------------------------------------------- Helpers

    private int compter(String sql) {
        try (Statement st = db.getConnection().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            System.err.println("[ReservationDAO] compter : " + e.getMessage());
        }
        return 0;
    }

    private void remplirParametres(PreparedStatement ps, Reservation r) throws SQLException {
        ps.setInt(1, r.getChambreId());
        ps.setString(2, r.getClientNom());
        ps.setString(3, r.getClientEmail());
        ps.setString(4, r.getClientTelephone());
        ps.setDate(5, Date.valueOf(r.getDateArrivee()));
        ps.setDate(6, Date.valueOf(r.getDateDepart()));
        ps.setInt(7, r.getNombrePersonnes());
        ps.setString(8, r.getStatut().name());
        ps.setDouble(9, r.getMontantTotal());
        ps.setBoolean(10, r.isPaye());
        ps.setString(11, r.getRemarques());
    }

    private Reservation mapper(ResultSet rs) throws SQLException {
        Reservation r = new Reservation();
        r.setId(rs.getInt("id"));
        r.setChambreId(rs.getInt("chambre_id"));
        r.setChambreNumero(rs.getString("chambre_numero"));
        r.setClientNom(rs.getString("client_nom"));
        r.setClientEmail(rs.getString("client_email"));
        r.setClientTelephone(rs.getString("client_telephone"));
        Date arrivee = rs.getDate("date_arrivee");
        Date depart = rs.getDate("date_depart");
        if (arrivee != null) r.setDateArrivee(arrivee.toLocalDate());
        if (depart != null) r.setDateDepart(depart.toLocalDate());
        r.setNombrePersonnes(rs.getInt("nombre_personnes"));
        try {
            r.setStatut(StatutReservation.valueOf(rs.getString("statut")));
        } catch (IllegalArgumentException e) {
            r.setStatut(StatutReservation.EN_ATTENTE);
        }
        r.setMontantTotal(rs.getDouble("montant_total"));
        r.setPaye(rs.getBoolean("paye"));
        r.setRemarques(rs.getString("remarques"));
        return r;
    }
}
