-- Index manquants sur les colonnes de cle etrangere reellement utilisees
-- dans les recherches. PostgreSQL n'indexe PAS automatiquement les cles
-- etrangeres : sans index, chaque recherche (et chaque suppression en cascade
-- manuelle) parcourt toute la table.
--
-- IF NOT EXISTS / IF EXISTS : la migration peut etre rejouee sans erreur.

-- ------------------------------------------------------------------
-- Donnees des joueurs : grossissent avec le nombre de joueurs.
-- ------------------------------------------------------------------

-- PersonnageRepository.findByUtilisateur (liste des personnages, /auth/me,
-- suppression de compte).
CREATE INDEX IF NOT EXISTS idx_personnage_utilisateur
    ON public.personnage (utilisateur_id);

-- CombatRepository.findFirstByPersonnageAndChapitreIdOrderByCreeLeDesc :
-- appele a chaque tour de combat et a chaque GET /chapitre d'un chapitre de
-- combat. L'index suit exactement le filtre ET le tri de la requete.
-- Couvre aussi findByPersonnage (personnage_id en premiere position).
CREATE INDEX IF NOT EXISTS idx_combat_personnage_chapitre_cree
    ON public.combat (personnage_id, chapitre_id, cree_le DESC);

-- Ennemis d'un combat, charges avec lui a chaque tour.
CREATE INDEX IF NOT EXISTS idx_combat_ennemi_combat
    ON public.combat_ennemi (combat_id);

-- Disciplines d'un personnage, chargees avec lui a chaque requete de jeu.
CREATE INDEX IF NOT EXISTS idx_personnage_discipline_personnage
    ON public.personnage_discipline (personnage_id);

-- RefreshTokenService.revoquerToutesLesSessions (changement / reinitialisation
-- de mot de passe) et suppression de compte.
CREATE INDEX IF NOT EXISTS idx_refresh_token_utilisateur
    ON public.refresh_token (utilisateur_id);

-- Suppression de compte.
CREATE INDEX IF NOT EXISTS idx_email_verification_token_utilisateur
    ON public.email_verification_token (utilisateur_id);

-- ------------------------------------------------------------------
-- Catalogue du livre : petites tables, mais lues par chapitre / lien / effet
-- a chaque premier chargement (avant la mise en cache).
-- ------------------------------------------------------------------

CREATE INDEX IF NOT EXISTS idx_lien_chapitre ON public.lien (chapitre_id);
CREATE INDEX IF NOT EXISTS idx_effet_chapitre ON public.effet (chapitre_id);
CREATE INDEX IF NOT EXISTS idx_effet_objet ON public.effet (objet_id);
CREATE INDEX IF NOT EXISTS idx_cond_lien ON public.cond (lien_id);
CREATE INDEX IF NOT EXISTS idx_cond_effet ON public.cond (effet_id);
CREATE INDEX IF NOT EXISTS idx_objet_chap_chapitre ON public.objet_chap (chapitre_id);
CREATE INDEX IF NOT EXISTS idx_chapitre_ennemi_chapitre ON public.chapitre_ennemi (chapitre_id);
CREATE INDEX IF NOT EXISTS idx_ennemi_resistance_ennemi ON public.ennemi_resistance (ennemi_id);

-- ------------------------------------------------------------------
-- Doublons : ces deux colonnes ont deja une contrainte UNIQUE, qui cree son
-- propre index. Les index ci-dessous ne servaient a rien, mais etaient mis a
-- jour a chaque insertion de token.
-- ------------------------------------------------------------------

DROP INDEX IF EXISTS public.idx_evt_token_hash;
DROP INDEX IF EXISTS public.idx_refresh_token_hash;
