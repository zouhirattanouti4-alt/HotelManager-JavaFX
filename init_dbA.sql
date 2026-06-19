-- =====================================================================
--  HotelManager — Script d'initialisation de la base de données
--  SGBD : MySQL 8.x
--  Module Java IHM • ENSAO • GI3 • 2025-2026
--
--  Exécution :
--    mysql -u root -p < init_db.sql
--  ou via MySQL Workbench : ouvrir ce fichier puis « Run ».
--
--  Les valeurs des colonnes « type » et « statut » correspondent aux
--  constantes des énumérations Java (name()) : SIMPLE, DOUBLE, SUITE,
--  FAMILIALE, DELUXE / EN_ATTENTE, CONFIRMEE, ANNULEE, TERMINEE.
-- =====================================================================

DROP DATABASE IF EXISTS hotel_db;
CREATE DATABASE hotel_db
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;
USE hotel_db;

-- ---------------------------------------------------------------------
--  Table : chambres (entité 1)
-- ---------------------------------------------------------------------
CREATE TABLE chambres (
    id            INT AUTO_INCREMENT PRIMARY KEY,
    numero        VARCHAR(10)  NOT NULL UNIQUE,
    type          VARCHAR(20)  NOT NULL DEFAULT 'SIMPLE',
    prix_nuit     DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    capacite      INT          NOT NULL DEFAULT 1,
    etage         INT          NOT NULL DEFAULT 0,
    disponible    BOOLEAN      NOT NULL DEFAULT TRUE,
    description   TEXT,
    date_creation TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ---------------------------------------------------------------------
--  Table : reservations (entité 2) — liée à chambres (clé étrangère)
-- ---------------------------------------------------------------------
CREATE TABLE reservations (
    id                INT AUTO_INCREMENT PRIMARY KEY,
    chambre_id        INT          NOT NULL,
    client_nom        VARCHAR(120) NOT NULL,
    client_email      VARCHAR(120),
    client_telephone  VARCHAR(30),
    date_arrivee      DATE         NOT NULL,
    date_depart       DATE         NOT NULL,
    nombre_personnes  INT          NOT NULL DEFAULT 1,
    statut            VARCHAR(20)  NOT NULL DEFAULT 'EN_ATTENTE',
    montant_total     DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    paye              BOOLEAN      NOT NULL DEFAULT FALSE,
    remarques         TEXT,
    date_creation     TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_reservation_chambre
        FOREIGN KEY (chambre_id) REFERENCES chambres(id)
        ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ---------------------------------------------------------------------
--  Index pour accélérer les filtres et jointures fréquents
-- ---------------------------------------------------------------------
CREATE INDEX idx_chambres_type        ON chambres(type);
CREATE INDEX idx_chambres_disponible  ON chambres(disponible);
CREATE INDEX idx_reservations_statut  ON reservations(statut);
CREATE INDEX idx_reservations_chambre ON reservations(chambre_id);
CREATE INDEX idx_reservations_arrivee ON reservations(date_arrivee);

-- =====================================================================
--  Données d'exemple — Chambres
-- =====================================================================
INSERT INTO chambres (numero, type, prix_nuit, capacite, etage, disponible, description) VALUES
('101', 'SIMPLE',    350.00, 1, 1, TRUE,  'Chambre simple confortable, vue sur jardin, bureau de travail.'),
('102', 'SIMPLE',    350.00, 1, 1, FALSE, 'Chambre simple côté rue, lit simple, salle de bain privative.'),
('201', 'DOUBLE',    550.00, 2, 2, TRUE,  'Chambre double avec grand lit, climatisation et minibar.'),
('202', 'DOUBLE',    550.00, 2, 2, TRUE,  'Chambre double lumineuse, balcon, vue panoramique sur la ville.'),
('301', 'FAMILIALE', 850.00, 4, 3, TRUE,  'Chambre familiale spacieuse, deux lits doubles, idéale familles.'),
('302', 'FAMILIALE', 850.00, 4, 3, FALSE, 'Chambre familiale avec coin salon et kitchenette équipée.'),
('401', 'SUITE',    1200.00, 3, 4, TRUE,  'Suite élégante avec salon séparé, baignoire et vue mer.'),
('402', 'SUITE',    1200.00, 3, 4, TRUE,  'Suite junior, espace bureau, service en chambre 24h/24.'),
('501', 'DELUXE',   1800.00, 2, 5, TRUE,  'Suite Deluxe au dernier étage, terrasse privée et jacuzzi.'),
('502', 'DELUXE',   1800.00, 2, 5, FALSE, 'Suite Deluxe prestige, décoration raffinée, majordome dédié.');

-- =====================================================================
--  Données d'exemple — Réservations
--  (les chambre_id correspondent à l'ordre d'insertion ci-dessus)
-- =====================================================================
INSERT INTO reservations
    (chambre_id, client_nom, client_email, client_telephone,
     date_arrivee, date_depart, nombre_personnes, statut, montant_total, paye, remarques) VALUES
(3, 'Karim Benali',     'karim.benali@email.com',   '0612345678',
 '2026-06-20', '2026-06-23', 2, 'CONFIRMEE', 1650.00, TRUE,  'Arrivée tardive prévue vers 22h.'),
(7, 'Sofia Marlowe',    'sofia.marlowe@email.com',  '0623456789',
 '2026-06-21', '2026-06-25', 2, 'CONFIRMEE', 4800.00, TRUE,  'Voyage de noces — décoration florale demandée.'),
(1, 'Yassine El Amrani','yassine.elamrani@email.com','0634567890',
 '2026-06-19', '2026-06-20', 1, 'TERMINEE',  350.00,  TRUE,  'Client fidèle.'),
(4, 'Laura Schmidt',    'laura.schmidt@email.com',  '0645678901',
 '2026-07-01', '2026-07-05', 2, 'EN_ATTENTE',2200.00, FALSE, 'En attente de confirmation du paiement.'),
(9, 'Omar Haddad',      'omar.haddad@email.com',    '0656789012',
 '2026-07-10', '2026-07-12', 2, 'EN_ATTENTE',3600.00, FALSE, 'Demande un surclassement si possible.'),
(5, 'Fatima Zahra',     'fatima.zahra@email.com',   '0667890123',
 '2026-06-15', '2026-06-18', 4, 'ANNULEE',   2550.00, FALSE, 'Annulation pour raison personnelle.'),
(8, 'James Carter',     'james.carter@email.com',   '0678901234',
 '2026-06-22', '2026-06-24', 3, 'CONFIRMEE', 2400.00, FALSE, 'Facturation au nom de la société.');

-- =====================================================================
--  Vérification rapide
-- =====================================================================
SELECT CONCAT(COUNT(*), ' chambres créées')     AS info FROM chambres;
SELECT CONCAT(COUNT(*), ' réservations créées') AS info FROM reservations;
