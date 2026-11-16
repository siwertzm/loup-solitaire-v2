-- REGLE-10 : nouveau type de condition ASSAUT_MIN (effet a partir du N-ieme
-- assaut, 283). La contrainte CHECK de cond.type est recreee avec ASSAUT_MIN,
-- sinon le chargement du catalogue est refuse.

ALTER TABLE public.cond DROP CONSTRAINT cond_type_check;

ALTER TABLE public.cond ADD CONSTRAINT cond_type_check CHECK ((type)::text = ANY (ARRAY[
    'DISCIPLINE', 'OBJET', 'BOURSE', 'HASARD', 'FUITE', 'ARME', 'ENDURANCE',
    'ENDURANCE_PERDUE', 'ENDURANCE_INF', 'ASSAUT_MAX', 'ASSAUT_MIN', 'ASSAUT_ECHEC',
    'VICTOIRE', 'PERMANENT', 'SANS_ARME'
]::text[]));
