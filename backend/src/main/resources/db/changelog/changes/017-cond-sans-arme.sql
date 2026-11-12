-- REGLE-11 : nouveau type de condition SANS_ARME (combat a mains nues, 260).
-- La contrainte CHECK de cond.type liste les types autorises : elle est
-- recreee avec SANS_ARME, sinon le chargement du catalogue est refuse.

ALTER TABLE public.cond DROP CONSTRAINT cond_type_check;

ALTER TABLE public.cond ADD CONSTRAINT cond_type_check CHECK ((type)::text = ANY (ARRAY[
    'DISCIPLINE', 'OBJET', 'BOURSE', 'HASARD', 'FUITE', 'ARME', 'ENDURANCE',
    'ENDURANCE_PERDUE', 'ENDURANCE_INF', 'ASSAUT_MAX', 'ASSAUT_ECHEC',
    'VICTOIRE', 'PERMANENT', 'SANS_ARME'
]::text[]));
