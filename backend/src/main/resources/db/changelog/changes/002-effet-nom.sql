-- Ajout d'un libellé narratif optionnel aux effets.
-- Le type technique reste VOL pour conserver exactement le même traitement backend.

ALTER TABLE public.effet
ADD COLUMN nom character varying(255);

-- Chapitre 144 : véritable vol
UPDATE public.effet
SET nom = 'VOL'
WHERE chapitre_id = 144
  AND type = 'VOL';

-- Chapitres où l'objet est perdu
UPDATE public.effet
SET nom = 'PERTE'
WHERE chapitre_id IN (174, 181, 188, 258, 274, 294)
  AND type = 'VOL';

-- Chapitre où l'objet est cassé
UPDATE public.effet
SET nom = 'CASSE'
WHERE chapitre_id = 277
  AND type = 'VOL';