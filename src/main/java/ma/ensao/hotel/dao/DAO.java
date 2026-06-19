package ma.ensao.hotel.dao;

import java.util.List;
import java.util.Optional;

/**
 * Contrat générique du pattern <b>DAO</b> (Data Access Object).
 * <p>
 * Chaque entité possède son propre DAO implémentant ces opérations CRUD,
 * ce qui isole totalement la logique SQL du reste de l'application.
 *
 * @param <T> type de l'entité gérée
 * @author ENSAO GI3
 */
public interface DAO<T> {

    /** @return toutes les entités persistées. */
    List<T> findAll();

    /** @return l'entité correspondant à l'identifiant, si elle existe. */
    Optional<T> findById(int id);

    /** Insère une nouvelle entité (met à jour son id généré). */
    boolean insert(T entity);

    /** Met à jour une entité existante. */
    boolean update(T entity);

    /** Supprime l'entité identifiée par {@code id}. */
    boolean delete(int id);
}
