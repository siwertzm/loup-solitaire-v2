-- SEC-06 : nouvel email demande depuis le profil, en attente de confirmation.
-- L'email du compte (et emailVerifie) ne change qu'au clic sur le lien recu
-- a la nouvelle adresse : une faute de frappe ne bloque plus la connexion.

ALTER TABLE public.utilisateur ADD COLUMN email_en_attente character varying(255);
