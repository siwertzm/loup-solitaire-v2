-- REGLE-12 : le Sac a Dos devient un Objet Special. Les personnages crees
-- avant ce changement le recoivent tous (decision du 16/11/2026), sinon ils
-- ne pourraient plus porter ni objets ni repas.
-- La ligne du catalogue est creee ici si GameDataLoader ne l'a pas encore
-- chargee (il tourne apres Liquibase et la remet a jour depuis objet.json).

INSERT INTO public.objet (id, categorie, description, nom)
VALUES ('sac_a_dos', 'OBJETS_SPECIAUX',
        'Contient vos objets et vos repas. Sans Sac à Dos, vous ne pouvez pas en porter.', 'Sac à Dos')
ON CONFLICT (id) DO NOTHING;

INSERT INTO public.inventaire_item (id, quantite, objet_id, personnage_id)
SELECT gen_random_uuid(), 1, 'sac_a_dos', p.id
FROM public.personnage p
WHERE NOT EXISTS (
    SELECT 1 FROM public.inventaire_item i
    WHERE i.personnage_id = p.id AND i.objet_id = 'sac_a_dos'
);
