-- Journal du parcours : liste ordonnee des chapitres traverses par personnage
-- (une ligne par arrivee, revisites comprises). "ordre" = position dans la
-- liste (@OrderColumn). Voir Personnage.chapitresParcourus.
CREATE TABLE public.personnage_chapitre_parcouru (
    personnage_id uuid NOT NULL,
    ordre integer NOT NULL,
    chapitre_id integer NOT NULL,
    PRIMARY KEY (personnage_id, ordre),
    CONSTRAINT fk_parcouru_personnage FOREIGN KEY (personnage_id) REFERENCES public.personnage (id),
    CONSTRAINT fk_parcouru_chapitre FOREIGN KEY (chapitre_id) REFERENCES public.chapitre (id)
);

-- Personnages deja existants : leur parcours passe est inconnu, on amorce le
-- journal avec leur chapitre courant pour qu'il ne soit pas vide.
INSERT INTO public.personnage_chapitre_parcouru (personnage_id, ordre, chapitre_id)
SELECT id, 0, chapitre_actuel_id
FROM public.personnage;