-- REGLE-08 : ordre des ennemis multiples garanti.
--
-- 1. chapitre_ennemi.ordre : position de l'ennemi dans le chapitre (0 =
--    premier affronte), maintenue par Hibernate (@OrderColumn sur
--    Chapitre.ennemis). Sans elle, l'ordre dependait de l'ordre physique
--    des lignes en base (ex. 180 : le chef avant ses deux soldats).
--    Les lignes existantes sont numerotees selon chapitre.json ; sur une
--    base neuve la table est vide ici et GameDataLoader la remplit ensuite.
ALTER TABLE public.chapitre_ennemi ADD COLUMN IF NOT EXISTS ordre integer;

UPDATE public.chapitre_ennemi
SET ordre = CASE ennemi_id
    WHEN 'glok_deuxieme_112' THEN 1
    WHEN 'glok_deuxieme_260' THEN 1
    WHEN 'glok_deuxieme_336' THEN 1
    WHEN 'soldat_premier' THEN 1
    WHEN 'soldat_deuxieme' THEN 2
    WHEN 'loup_maudit_2' THEN 1
    WHEN 'loup_maudit_3' THEN 2
    WHEN 'loup_maudit_4' THEN 3
    ELSE 0
END
WHERE ordre IS NULL;

ALTER TABLE public.chapitre_ennemi ALTER COLUMN ordre SET NOT NULL;

-- 2. combat_ennemi.ordre_liste n'est plus utilisee : l'ordre d'un combat
--    vient de la colonne "ordre", deja remplie a la creation du combat
--    (@OrderBy sur Combat.ennemis).
ALTER TABLE public.combat_ennemi DROP COLUMN IF EXISTS ordre_liste;
