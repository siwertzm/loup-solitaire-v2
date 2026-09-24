-- Disciplines possedees par un ennemi (ex. Puissance Psychique des Vordaks),
-- distinctes de ennemi_resistance. Voir Ennemi.disciplines.
CREATE TABLE public.ennemi_discipline (
    ennemi_id character varying(255) NOT NULL,
    discipline_id character varying(255) NOT NULL,
    PRIMARY KEY (ennemi_id, discipline_id),
    CONSTRAINT fk_ennemi_discipline_ennemi FOREIGN KEY (ennemi_id) REFERENCES public.ennemi (id),
    CONSTRAINT fk_ennemi_discipline_discipline FOREIGN KEY (discipline_id) REFERENCES public.discipline (id)
);

-- Bases existantes : GameDataLoader ne recharge pas les ennemis deja presents,
-- on ajoute donc les lignes ici. Sur une base neuve, la table ennemi est
-- encore vide a ce stade : le WHERE EXISTS evite l'erreur de FK et c'est
-- ennemi.json qui fournit les donnees.
INSERT INTO public.ennemi_discipline (ennemi_id, discipline_id)
SELECT e.id, 'PUISSANCE_PSYCHIQUE'
FROM public.ennemi e
WHERE e.id IN ('vordak', 'vordak_puissant')
  AND EXISTS (SELECT 1 FROM public.discipline d WHERE d.id = 'PUISSANCE_PSYCHIQUE');