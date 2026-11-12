-- DATA-00 : empreinte SHA-256 des quatre JSON du livre (discipline, ennemi,
-- objet, chapitre). GameDataLoader recharge le catalogue quand elle change :
-- corriger le livre = modifier le JSON, sans migration de donnees.
-- Une seule ligne (id = 1).

CREATE TABLE public.catalogue_empreinte (
    id integer NOT NULL,
    empreinte character varying(64) NOT NULL,
    charge_le timestamp(6) with time zone NOT NULL,
    CONSTRAINT catalogue_empreinte_pkey PRIMARY KEY (id)
);
