-- Insertion des villes
INSERT IGNORE INTO villes (ville_id, nom) VALUES
                                       (UUID_TO_BIN('11111111-1111-1111-1111-111111111111'), 'Paris'),
                                       (UUID_TO_BIN('22222222-2222-2222-2222-222222222222'), 'Lyon'),
                                       (UUID_TO_BIN('33333333-3333-3333-3333-333333333333'), 'Marseille'),
                                       (UUID_TO_BIN('44444444-4444-4444-4444-444444444444'), 'Bordeaux'),
                                       (UUID_TO_BIN('55555555-5555-5555-5555-555555555555'), 'Lille');

-- Insertion des trains
INSERT IGNORE INTO trains (train_id, nom_train) VALUES
                                             ('TGV100', 'TGV Paris-Lyon'),
                                             ('TGV200', 'TGV Lyon-Marseille'),
                                             ('INTER300', 'Intercités Bordeaux-Paris'),
                                             ('TER400', 'TER Lille-Paris');

-- Insertion des services (pour le 18/03/2026)
INSERT IGNORE INTO services_ferroviaires (service_id, date_trajet, train_id, ville_depart_id, ville_arrivee_id, prix_base) VALUES
                                                                                                                        (UUID_TO_BIN('a1111111-1111-1111-1111-111111111111'), '2026-03-18', 'TGV100', UUID_TO_BIN('11111111-1111-1111-1111-111111111111'), UUID_TO_BIN('22222222-2222-2222-2222-222222222222'), 89.90),
                                                                                                                        (UUID_TO_BIN('a2222222-2222-2222-2222-222222222222'), '2026-03-20', 'TGV200', UUID_TO_BIN('22222222-2222-2222-2222-222222222222'), UUID_TO_BIN('33333333-3333-3333-3333-333333333333'), 75.50),
                                                                                                                        (UUID_TO_BIN('a3333333-3333-3333-3333-333333333333'), '2026-03-25', 'INTER300', UUID_TO_BIN('44444444-4444-4444-4444-444444444444'), UUID_TO_BIN('11111111-1111-1111-1111-111111111111'), 95.00);

-- Insertion des clients de test
INSERT IGNORE INTO clients (client_id, nom, prenom, photo_ref) VALUES
                                                            (UUID_TO_BIN('c1111111-1111-1111-1111-111111111111'), 'Dupont', 'Jean', 'photos/jean_dupont.jpg'),
                                                            (UUID_TO_BIN('c2222222-2222-2222-2222-222222222222'), 'Martin', 'Marie', 'photos/marie_martin.jpg');

-- Insertion des contrôleurs de test
INSERT IGNORE INTO controleurs (controleur_id, login, hash_mot_de_passe, nom, prenom) VALUES
                                                                                   (UUID_TO_BIN('d1111111-1111-1111-1111-111111111111'), 'Nathan', 'Anne', 'Petit', 'Paul'),
                                                                                   (UUID_TO_BIN('e2222222-2222-2222-2222-222222222222'), 'Christian', 'Essome', 'Robert', 'Sophie');