-- DATA-11 / D-11 : nouveau type de condition EVITEMENT (partir sans livrer
-- le combat, 112 et 180). FUITE a 0 assaut ne veut plus dire que "fuite des
-- la premiere action" (191, 220, 339). La contrainte CHECK de cond.type est
-- recreee avec EVITEMENT, sinon le chargement du catalogue est refuse.

ALTER TABLE public.cond DROP CONSTRAINT cond_type_check;

ALTER TABLE public.cond ADD CONSTRAINT cond_type_check CHECK ((type)::text = ANY (ARRAY[
    'DISCIPLINE', 'OBJET', 'BOURSE', 'HASARD', 'FUITE', 'ARME', 'ENDURANCE',
    'ENDURANCE_PERDUE', 'ENDURANCE_INF', 'ASSAUT_MAX', 'ASSAUT_MIN', 'ASSAUT_ECHEC',
    'VICTOIRE', 'PERMANENT', 'SANS_ARME', 'EVITEMENT'
]::text[]));
