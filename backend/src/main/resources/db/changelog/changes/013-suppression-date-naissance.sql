-- RGPD-01 (decision D-08) : la date de naissance n'est plus collectee.
-- La colonne et toutes les valeurs deja enregistrees sont supprimees.
-- (Les sauvegardes anterieures en contiennent encore : elles disparaissent
-- avec leur duree de conservation, voir le registre RGPD.)
ALTER TABLE public.utilisateur DROP COLUMN IF EXISTS date_naissance;
