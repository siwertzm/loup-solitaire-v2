-- SEC-01 : tirage des caracteristiques de creation fait par le serveur.
-- Une ligne au plus par utilisateur (tirage en attente), supprimee a la
-- creation du personnage, et avec le compte (ON DELETE CASCADE).
CREATE TABLE public.tirage_creation (
    utilisateur_id uuid NOT NULL,
    hasard_habilite integer NOT NULL,
    hasard_endurance integer NOT NULL,
    cree_le timestamp(6) with time zone NOT NULL,
    CONSTRAINT pk_tirage_creation PRIMARY KEY (utilisateur_id),
    CONSTRAINT fk_tirage_creation_utilisateur FOREIGN KEY (utilisateur_id)
        REFERENCES public.utilisateur (id) ON DELETE CASCADE,
    CONSTRAINT ck_tirage_creation_habilite CHECK (hasard_habilite BETWEEN 0 AND 9),
    CONSTRAINT ck_tirage_creation_endurance CHECK (hasard_endurance BETWEEN 0 AND 9)
);