-- Verrou optimiste sur le personnage (voir Personnage.version) : protege
-- contre deux actions simultanees (double tap en combat, double clic sur
-- "Prendre"...). Les personnages existants demarrent a la version 0.
ALTER TABLE public.personnage
ADD COLUMN version bigint NOT NULL DEFAULT 0;