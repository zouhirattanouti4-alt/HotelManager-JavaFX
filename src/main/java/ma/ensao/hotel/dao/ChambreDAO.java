package ma.ensao.hotel.dao;

import ma.ensao.hotel.model.Chambre;
import ma.ensao.hotel.model.TypeChambre;

import java.sql.Connection;
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
 * DAO de l'entité {@link Chambre} : implémente le CRUD et expose quelques
 * agrégats utilisés par le tableau de bord.
 *
 * @author ENSAO GI3
 */
public class ChambreDAO implements DAO<Chambre> {

    private final Database db = Database.getInstance();

    // ------------------------------------------------------------------ READ

    @Override
    public List<Chambre> findAll() {
        List<Chambre> chambres = new ArrayList<>();
        String sql = "SELECT * FROM chambres ORDER BY numero";
        try (Statement st = db.getConnection().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                chambres.add(mapper(rs));
            }
        } catch (SQLException e) {
            System.err.println("[ChambreDAO] findAll : " + e.getMessage());
        }
        return chambres;
    }

    @Override
    public Optional<Chambre> findById(int id) {
        String sql = "SELECT * FROM chambres WHERE id = ?";
        try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapper(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("[ChambreDAO] findById : " + e.getMessage());
        }
        return Optional.empty();
    }

    /** @return uniquement les chambres marquées comme disponibles (pour les réservations). */
    public List<Chambre> findDisponibles() {
        List<Chambre> chambres = new ArrayList<>();
        String sql = "SELECT * FROM chambres WHERE disponible = TRUE ORDER BY numero";
        try (Statement st = db.getConnection().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                chambres.add(mapper(rs));
            }
        } catch (SQLException e) {
            System.err.println("[ChambreDAO] findDisponibles : " + e.getMessage());
        }
        return chambres;
    }

    // ----------------------------------------------------------------- WRITE

    @Override
    public boolean insert(Chambre c) {
        String sql = "INSERT INTO chambres "
                + "(numero, type, prix_nuit, capacite, etage, disponible, description) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = db.getConnection()
                .prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            remplirParametres(ps, c);
            if (ps.executeUpdate() == 0) return false;
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    c.setId(keys.getInt(1));
                }
            }
            return true;
        } catch (SQLException e) {
            System.err.println("[ChambreDAO] insert : " + e.getMessage());
            return false;
        }
    }

    @Override
    public boolean update(Chambre c) {
        String sql = "UPDATE chambres SET "
                + "numero = ?, type = ?, prix_nuit = ?, capacite = ?, "
                + "etage = ?, disponible = ?, description = ? WHERE id = ?";
        try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
            remplirParametres(ps, c);
            ps.setInt(8, c.getId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[ChambreDAO] update : " + e.getMessage());
            return false;
        }
    }

    @Override
    public boolean delete(int id) {
        String sql = "DELETE FROM chambres WHERE id = ?";
        try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[ChambreDAO] delete : " + e.getMessage());
            return false;
        }
    }

    // ------------------------------------------------------------ STATISTIQUES

    /** Nombre total de chambres. */
    public int compterTotal() {
        return compter("SELECT COUNT(*) FROM chambres");
    }

    /** Nombre de chambres disponibles. */
    public int compterDisponibles() {
        return compter("SELECT COUNT(*) FROM chambres WHERE disponible = TRUE");
    }

    /** Répartition du nombre de chambres par type (pour le PieChart). */
    public Map<TypeChambre, Integer> compterParType() {
        Map<TypeChambre, Integer> repartition = new LinkedHashMap<>();
        for (TypeChambre t : TypeChambre.values()) {
            repartition.put(t, 0);
        }
        String sql = "SELECT type, COUNT(*) AS total FROM chambres GROUP BY type";
        try (Statement st = db.getConnection().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                try {
                    repartition.put(TypeChambre.valueOf(rs.getString("type")), rs.getInt("total"));
                } catch (IllegalArgumentException ignored) {
                    // Type inconnu en base : on l'ignore
                }
            }
        } catch (SQLException e) {
            System.err.println("[ChambreDAO] compterParType : " + e.getMessage());
        }
        return repartition;
    }

    // -------------------------------------------------------------- Helpers

    private int compter(String sql) {
        try (Statement st = db.getConnection().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            System.err.println("[ChambreDAO] compter : " + e.getMessage());
        }
        return 0;
    }

    /** Renseigne les 7 premiers paramètres communs à INSERT et UPDATE. */
    private void remplirParametres(PreparedStatement ps, Chambre c) throws SQLException {
        ps.setString(1, c.getNumero());
        ps.setString(2, c.getType().name());
        ps.setDouble(3, c.getPrixNuit());
        ps.setInt(4, c.getCapacite());
        ps.setInt(5, c.getEtage());
        ps.setBoolean(6, c.isDisponible());
        ps.setString(7, c.getDescription());
    }

    /** Transforme une ligne du ResultSet en objet Chambre. */
    private Chambre mapper(ResultSet rs) throws SQLException {
        Chambre c = new Chambre();
        c.setId(rs.getInt("id"));
        c.setNumero(rs.getString("numero"));
        try {
            c.setType(TypeChambre.valueOf(rs.getString("type")));
        } catch (IllegalArgumentException e) {
            c.setType(TypeChambre.SIMPLE);
        }
        c.setPrixNuit(rs.getDouble("prix_nuit"));
        c.setCapacite(rs.getInt("capacite"));
        c.setEtage(rs.getInt("etage"));
        c.setDisponible(rs.getBoolean("disponible"));
        c.setDescription(rs.getString("description"));
        return c;
    }
}
