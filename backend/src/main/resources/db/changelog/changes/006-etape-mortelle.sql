-- Journal : positions (colonne "ordre" de personnage_chapitre_parcouru) des
-- arrivees ou le personnage est mort, pour griser ces etapes. Voir
-- Personnage.etapesMortelles.
CREATE TABLE public.personnage_etape_mortelle (
    personnage_id uuid NOT NULL,
    ordre integer NOT NULL,
    PRIMARY KEY (personnage_id, ordre),
    CONSTRAINT fk_etape_mortelle_personnage FOREIGN KEY (personnage_id) REFERENCES public.personnage (id)
);

-- Personnages actuellement morts : leur derniere arrivee est celle ou ils sont
-- morts. Les morts passees (deja ressuscites) sont inconnues et ne sont pas
-- reconstituees.
INSERT INTO public.personnage_etape_mortelle (personnage_id, ordre)
SELECT p.id, MAX(c.ordre)
FROM public.personnage p
JOIN public.personnage_chapitre_parcouru c ON c.personnage_id = p.id
WHERE p.mort
GROUP BY p.id;