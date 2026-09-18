--
-- PostgreSQL database dump
--

-- Dumped from database version 16.15
-- Dumped by pg_dump version 16.15


--
-- Name: chapitre; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.chapitre (
    id integer NOT NULL,
    combat boolean NOT NULL,
    text text NOT NULL
);


--
-- Name: chapitre_ennemi; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.chapitre_ennemi (
    chapitre_id integer NOT NULL,
    ennemi_id character varying(255) NOT NULL
);


--
-- Name: combat; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.combat (
    id uuid NOT NULL,
    assauts_livres integer NOT NULL,
    bonus_habilite_en_attente integer NOT NULL,
    chapitre_id integer NOT NULL,
    cree_le timestamp(6) with time zone NOT NULL,
    endurance_perdue boolean NOT NULL,
    ennemi_actif_index integer NOT NULL,
    statut character varying(255) NOT NULL,
    personnage_id uuid NOT NULL,
    CONSTRAINT combat_statut_check CHECK (((statut)::text = ANY ((ARRAY['EN_COURS'::character varying, 'VICTOIRE'::character varying, 'DEFAITE'::character varying, 'FUITE'::character varying, 'INTERROMPU'::character varying])::text[])))
);


--
-- Name: combat_ennemi; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.combat_ennemi (
    id uuid NOT NULL,
    endurance_actuelle integer NOT NULL,
    ordre integer NOT NULL,
    combat_id uuid NOT NULL,
    ennemi_id character varying(255) NOT NULL,
    ordre_liste integer
);


--
-- Name: cond; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.cond (
    id uuid NOT NULL,
    target_id character varying(255),
    type character varying(255),
    valeur character varying(255),
    effet_id uuid,
    lien_id uuid,
    CONSTRAINT cond_type_check CHECK (((type)::text = ANY ((ARRAY['DISCIPLINE'::character varying, 'OBJET'::character varying, 'BOURSE'::character varying, 'HASARD'::character varying, 'FUITE'::character varying, 'ARME'::character varying, 'ENDURANCE'::character varying, 'ENDURANCE_PERDUE'::character varying, 'ENDURANCE_INF'::character varying, 'ASSAUT_MAX'::character varying, 'ASSAUT_ECHEC'::character varying, 'VICTOIRE'::character varying, 'PERMANENT'::character varying])::text[])))
);


--
-- Name: discipline; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.discipline (
    id character varying(255) NOT NULL,
    description character varying(3000) NOT NULL,
    nom character varying(255) NOT NULL,
    CONSTRAINT discipline_id_check CHECK (((id)::text = ANY ((ARRAY['CAMOUFLAGE'::character varying, 'CHASSE'::character varying, 'SIXIEME_SENS'::character varying, 'ORIENTATION'::character varying, 'GUERISON'::character varying, 'MAITRISE_ARMES'::character varying, 'BOUCLIER_PSYCHIQUE'::character varying, 'PUISSANCE_PSYCHIQUE'::character varying, 'COMMUNICATION_ANIMALE'::character varying, 'MAITRISE_MATIERE'::character varying])::text[])))
);


--
-- Name: effet; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.effet (
    id uuid NOT NULL,
    type character varying(255),
    valeur integer,
    chapitre_id integer,
    objet_id character varying(255),
    CONSTRAINT effet_type_check CHECK (((type)::text = ANY ((ARRAY['ENDURANCE'::character varying, 'HABILITE'::character varying, 'REPAS'::character varying, 'VOL'::character varying, 'ECHANGE'::character varying, 'MORT'::character varying])::text[])))
);


--
-- Name: email_verification_token; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.email_verification_token (
    id uuid NOT NULL,
    created_at timestamp(6) with time zone NOT NULL,
    expires_at timestamp(6) with time zone NOT NULL,
    token_hash character varying(64) NOT NULL,
    utilise boolean NOT NULL,
    utilisateur_id uuid NOT NULL
);


--
-- Name: ennemi; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.ennemi (
    id character varying(255) NOT NULL,
    description character varying(2000),
    endurance integer NOT NULL,
    habilite integer NOT NULL,
    nom character varying(255) NOT NULL
);


--
-- Name: ennemi_resistance; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.ennemi_resistance (
    ennemi_id character varying(255) NOT NULL,
    discipline_id character varying(255) NOT NULL
);


--
-- Name: inventaire_item; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.inventaire_item (
    id uuid NOT NULL,
    quantite integer NOT NULL,
    objet_id character varying(255) NOT NULL,
    personnage_id uuid NOT NULL
);


--
-- Name: lien; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.lien (
    id uuid NOT NULL,
    chapitre_id integer NOT NULL,
    chapitre_cible_id integer NOT NULL
);


--
-- Name: objet; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.objet (
    id character varying(255) NOT NULL,
    categorie character varying(255) NOT NULL,
    description character varying(1000),
    nom character varying(255) NOT NULL,
    CONSTRAINT objet_categorie_check CHECK (((categorie)::text = ANY ((ARRAY['ARME'::character varying, 'OBJET'::character varying, 'OBJETS_SPECIAUX'::character varying, 'REPAS'::character varying, 'BOURSE'::character varying])::text[])))
);


--
-- Name: objet_chap; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.objet_chap (
    id uuid NOT NULL,
    optionnel boolean NOT NULL,
    valeur integer NOT NULL,
    chapitre_id integer NOT NULL,
    objet_id character varying(255) NOT NULL
);


--
-- Name: objet_chapitre_ramasse; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.objet_chapitre_ramasse (
    id uuid NOT NULL,
    quantite integer NOT NULL,
    chapitre_id integer NOT NULL,
    objet_id character varying(255) NOT NULL,
    personnage_id uuid NOT NULL
);


--
-- Name: password_reset_token; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.password_reset_token (
    id uuid NOT NULL,
    code_hash character varying(255) NOT NULL,
    created_at timestamp(6) with time zone NOT NULL,
    expires_at timestamp(6) with time zone NOT NULL,
    tentatives integer NOT NULL,
    utilise boolean NOT NULL,
    utilisateur_id uuid NOT NULL,
    reset_token_expires_at timestamp(6) with time zone,
    reset_token_hash character varying(64)
);


--
-- Name: personnage; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.personnage (
    id uuid NOT NULL,
    date_creation timestamp(6) with time zone NOT NULL,
    dernier_statut_repas character varying(255),
    dernier_tirage_hasard integer,
    derniere_activite timestamp(6) with time zone,
    endurance_actuelle integer NOT NULL,
    endurance_max integer NOT NULL,
    habilite integer NOT NULL,
    habilite_base integer NOT NULL,
    habilite_temp integer NOT NULL,
    mort boolean NOT NULL,
    nom character varying(255) NOT NULL,
    vol_en_attente character varying(255),
    arme_maitrisee_id character varying(255),
    chapitre_actuel_id integer NOT NULL,
    chapitre_precedent_id integer,
    utilisateur_id uuid NOT NULL,
    CONSTRAINT personnage_dernier_statut_repas_check CHECK (((dernier_statut_repas)::text = ANY ((ARRAY['CHASSE'::character varying, 'REPAS_CONSOMME'::character varying, 'MALUS_ENDURANCE'::character varying])::text[]))),
    CONSTRAINT personnage_vol_en_attente_check CHECK (((vol_en_attente)::text = ANY ((ARRAY['ARME'::character varying, 'TOUT'::character varying])::text[])))
);


--
-- Name: personnage_discipline; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.personnage_discipline (
    personnage_id uuid NOT NULL,
    discipline_id character varying(255) NOT NULL
);


--
-- Name: refresh_token; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.refresh_token (
    id uuid NOT NULL,
    created_at timestamp(6) with time zone NOT NULL,
    expires_at timestamp(6) with time zone NOT NULL,
    revoked boolean NOT NULL,
    token_hash character varying(64) NOT NULL,
    utilisateur_id uuid NOT NULL
);


--
-- Name: utilisateur; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.utilisateur (
    id uuid NOT NULL,
    date_creation timestamp(6) with time zone,
    date_naissance date,
    email character varying(255) NOT NULL,
    email_verifie boolean NOT NULL,
    password character varying(255) NOT NULL,
    username character varying(255) NOT NULL
);


--
-- Name: chapitre chapitre_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.chapitre
    ADD CONSTRAINT chapitre_pkey PRIMARY KEY (id);


--
-- Name: combat_ennemi combat_ennemi_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.combat_ennemi
    ADD CONSTRAINT combat_ennemi_pkey PRIMARY KEY (id);


--
-- Name: combat combat_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.combat
    ADD CONSTRAINT combat_pkey PRIMARY KEY (id);


--
-- Name: cond cond_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.cond
    ADD CONSTRAINT cond_pkey PRIMARY KEY (id);


--
-- Name: discipline discipline_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.discipline
    ADD CONSTRAINT discipline_pkey PRIMARY KEY (id);


--
-- Name: effet effet_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.effet
    ADD CONSTRAINT effet_pkey PRIMARY KEY (id);


--
-- Name: email_verification_token email_verification_token_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.email_verification_token
    ADD CONSTRAINT email_verification_token_pkey PRIMARY KEY (id);


--
-- Name: ennemi ennemi_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.ennemi
    ADD CONSTRAINT ennemi_pkey PRIMARY KEY (id);


--
-- Name: inventaire_item inventaire_item_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.inventaire_item
    ADD CONSTRAINT inventaire_item_pkey PRIMARY KEY (id);


--
-- Name: lien lien_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.lien
    ADD CONSTRAINT lien_pkey PRIMARY KEY (id);


--
-- Name: objet_chap objet_chap_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.objet_chap
    ADD CONSTRAINT objet_chap_pkey PRIMARY KEY (id);


--
-- Name: objet_chapitre_ramasse objet_chapitre_ramasse_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.objet_chapitre_ramasse
    ADD CONSTRAINT objet_chapitre_ramasse_pkey PRIMARY KEY (id);


--
-- Name: objet objet_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.objet
    ADD CONSTRAINT objet_pkey PRIMARY KEY (id);


--
-- Name: password_reset_token password_reset_token_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.password_reset_token
    ADD CONSTRAINT password_reset_token_pkey PRIMARY KEY (id);


--
-- Name: personnage personnage_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.personnage
    ADD CONSTRAINT personnage_pkey PRIMARY KEY (id);


--
-- Name: refresh_token refresh_token_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.refresh_token
    ADD CONSTRAINT refresh_token_pkey PRIMARY KEY (id);


--
-- Name: objet_chapitre_ramasse uk6f3glp6x7q87y8x4g7jluj8i6; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.objet_chapitre_ramasse
    ADD CONSTRAINT uk6f3glp6x7q87y8x4g7jluj8i6 UNIQUE (personnage_id, chapitre_id, objet_id);


--
-- Name: email_verification_token uk7ay5cmnpq0hps453t4wg1weqb; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.email_verification_token
    ADD CONSTRAINT uk7ay5cmnpq0hps453t4wg1weqb UNIQUE (token_hash);


--
-- Name: password_reset_token uk8arrp7ixidtqmxnv22t862g3e; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.password_reset_token
    ADD CONSTRAINT uk8arrp7ixidtqmxnv22t862g3e UNIQUE (reset_token_hash);


--
-- Name: utilisateur uk_utilisateur_email; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.utilisateur
    ADD CONSTRAINT uk_utilisateur_email UNIQUE (email);


--
-- Name: utilisateur uk_utilisateur_username; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.utilisateur
    ADD CONSTRAINT uk_utilisateur_username UNIQUE (username);


--
-- Name: inventaire_item ukcm7kpnssw5s0wg0ap9heonby3; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.inventaire_item
    ADD CONSTRAINT ukcm7kpnssw5s0wg0ap9heonby3 UNIQUE (personnage_id, objet_id);


--
-- Name: refresh_token ukkdj16cltjxdksuyiosdhliveg; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.refresh_token
    ADD CONSTRAINT ukkdj16cltjxdksuyiosdhliveg UNIQUE (token_hash);


--
-- Name: utilisateur utilisateur_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.utilisateur
    ADD CONSTRAINT utilisateur_pkey PRIMARY KEY (id);


--
-- Name: idx_evt_token_hash; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_evt_token_hash ON public.email_verification_token USING btree (token_hash);


--
-- Name: idx_password_reset_utilisateur; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_password_reset_utilisateur ON public.password_reset_token USING btree (utilisateur_id);


--
-- Name: idx_refresh_token_hash; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_refresh_token_hash ON public.refresh_token USING btree (token_hash);


--
-- Name: personnage fk1q9n22aydhls5ihv24momptf4; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.personnage
    ADD CONSTRAINT fk1q9n22aydhls5ihv24momptf4 FOREIGN KEY (chapitre_actuel_id) REFERENCES public.chapitre(id);


--
-- Name: ennemi_resistance fk3fuspte7j8h7ye1ritwagglhr; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.ennemi_resistance
    ADD CONSTRAINT fk3fuspte7j8h7ye1ritwagglhr FOREIGN KEY (ennemi_id) REFERENCES public.ennemi(id);


--
-- Name: effet fk484v5edfs909otccllowc8eli; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.effet
    ADD CONSTRAINT fk484v5edfs909otccllowc8eli FOREIGN KEY (objet_id) REFERENCES public.objet(id);


--
-- Name: objet_chap fk532eyfcwbjcn30jfrdtg8pd51; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.objet_chap
    ADD CONSTRAINT fk532eyfcwbjcn30jfrdtg8pd51 FOREIGN KEY (objet_id) REFERENCES public.objet(id);


--
-- Name: ennemi_resistance fk5u6rp8k4ohbj5d31oo6rx4w9o; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.ennemi_resistance
    ADD CONSTRAINT fk5u6rp8k4ohbj5d31oo6rx4w9o FOREIGN KEY (discipline_id) REFERENCES public.discipline(id);


--
-- Name: combat_ennemi fk6tdlarsbsthky1pvod86ykbcb; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.combat_ennemi
    ADD CONSTRAINT fk6tdlarsbsthky1pvod86ykbcb FOREIGN KEY (ennemi_id) REFERENCES public.ennemi(id);


--
-- Name: lien fk6vhmjr7kuhyb57g5k56c5rj2q; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.lien
    ADD CONSTRAINT fk6vhmjr7kuhyb57g5k56c5rj2q FOREIGN KEY (chapitre_id) REFERENCES public.chapitre(id);


--
-- Name: password_reset_token fk6xhhidrwocldvi9ifxkmynsdc; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.password_reset_token
    ADD CONSTRAINT fk6xhhidrwocldvi9ifxkmynsdc FOREIGN KEY (utilisateur_id) REFERENCES public.utilisateur(id);


--
-- Name: refresh_token fk73aakvlbiorvdu09w9jbhk4bj; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.refresh_token
    ADD CONSTRAINT fk73aakvlbiorvdu09w9jbhk4bj FOREIGN KEY (utilisateur_id) REFERENCES public.utilisateur(id);


--
-- Name: combat fk9f9ik9c04hvcn8w8dfge3hioi; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.combat
    ADD CONSTRAINT fk9f9ik9c04hvcn8w8dfge3hioi FOREIGN KEY (personnage_id) REFERENCES public.personnage(id);


--
-- Name: chapitre_ennemi fk9g8d7hcvnya0uwc72rgle8ltt; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.chapitre_ennemi
    ADD CONSTRAINT fk9g8d7hcvnya0uwc72rgle8ltt FOREIGN KEY (chapitre_id) REFERENCES public.chapitre(id);


--
-- Name: cond fkaplr9unbw3jlktsr06e2gcfco; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.cond
    ADD CONSTRAINT fkaplr9unbw3jlktsr06e2gcfco FOREIGN KEY (effet_id) REFERENCES public.effet(id);


--
-- Name: effet fkc07y7wo63km2lqa5dyupmq1kc; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.effet
    ADD CONSTRAINT fkc07y7wo63km2lqa5dyupmq1kc FOREIGN KEY (chapitre_id) REFERENCES public.chapitre(id);


--
-- Name: objet_chapitre_ramasse fkcpt8vm3x9kyn2jyvpvkyq1f5s; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.objet_chapitre_ramasse
    ADD CONSTRAINT fkcpt8vm3x9kyn2jyvpvkyq1f5s FOREIGN KEY (personnage_id) REFERENCES public.personnage(id);


--
-- Name: personnage fkdievxojt1eeu0f9w3py56a6es; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.personnage
    ADD CONSTRAINT fkdievxojt1eeu0f9w3py56a6es FOREIGN KEY (chapitre_precedent_id) REFERENCES public.chapitre(id);


--
-- Name: personnage fkh40dj9aw487afntugmn51yscn; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.personnage
    ADD CONSTRAINT fkh40dj9aw487afntugmn51yscn FOREIGN KEY (arme_maitrisee_id) REFERENCES public.objet(id);


--
-- Name: combat_ennemi fkhklistb4jiuxwjwfysww4ra5i; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.combat_ennemi
    ADD CONSTRAINT fkhklistb4jiuxwjwfysww4ra5i FOREIGN KEY (combat_id) REFERENCES public.combat(id);


--
-- Name: personnage fkkbjqp2xm3r8fiyv9vtoejba6p; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.personnage
    ADD CONSTRAINT fkkbjqp2xm3r8fiyv9vtoejba6p FOREIGN KEY (utilisateur_id) REFERENCES public.utilisateur(id);


--
-- Name: objet_chapitre_ramasse fkkdyuld6rloomabw9wtaamgjbe; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.objet_chapitre_ramasse
    ADD CONSTRAINT fkkdyuld6rloomabw9wtaamgjbe FOREIGN KEY (objet_id) REFERENCES public.objet(id);


--
-- Name: personnage_discipline fkm25r8y90ch1v0avncnga7wh1k; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.personnage_discipline
    ADD CONSTRAINT fkm25r8y90ch1v0avncnga7wh1k FOREIGN KEY (discipline_id) REFERENCES public.discipline(id);


--
-- Name: chapitre_ennemi fkmoav2v4lcd331dkl3pqlyvkv3; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.chapitre_ennemi
    ADD CONSTRAINT fkmoav2v4lcd331dkl3pqlyvkv3 FOREIGN KEY (ennemi_id) REFERENCES public.ennemi(id);


--
-- Name: inventaire_item fkn0r3au504ro7iudamthun6m2w; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.inventaire_item
    ADD CONSTRAINT fkn0r3au504ro7iudamthun6m2w FOREIGN KEY (personnage_id) REFERENCES public.personnage(id);


--
-- Name: objet_chapitre_ramasse fkn8sff6uu72jj6yilcswqwpavs; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.objet_chapitre_ramasse
    ADD CONSTRAINT fkn8sff6uu72jj6yilcswqwpavs FOREIGN KEY (chapitre_id) REFERENCES public.chapitre(id);


--
-- Name: inventaire_item fknuvn1isehcd9bgpd5peeol9vs; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.inventaire_item
    ADD CONSTRAINT fknuvn1isehcd9bgpd5peeol9vs FOREIGN KEY (objet_id) REFERENCES public.objet(id);


--
-- Name: cond fkpdkfpx67y7w8lb6e4q1nbosd5; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.cond
    ADD CONSTRAINT fkpdkfpx67y7w8lb6e4q1nbosd5 FOREIGN KEY (lien_id) REFERENCES public.lien(id);


--
-- Name: lien fkra2smmp693rcs6bwlaepumxx2; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.lien
    ADD CONSTRAINT fkra2smmp693rcs6bwlaepumxx2 FOREIGN KEY (chapitre_cible_id) REFERENCES public.chapitre(id);


--
-- Name: email_verification_token fkrr0vgst9bc36yqfd3drv79puk; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.email_verification_token
    ADD CONSTRAINT fkrr0vgst9bc36yqfd3drv79puk FOREIGN KEY (utilisateur_id) REFERENCES public.utilisateur(id);


--
-- Name: personnage_discipline fktewx2jtg8bl98en5q4afapr5o; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.personnage_discipline
    ADD CONSTRAINT fktewx2jtg8bl98en5q4afapr5o FOREIGN KEY (personnage_id) REFERENCES public.personnage(id);


--
-- Name: objet_chap fktig94inc05r5rg22atsh0nenh; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.objet_chap
    ADD CONSTRAINT fktig94inc05r5rg22atsh0nenh FOREIGN KEY (chapitre_id) REFERENCES public.chapitre(id);


--
-- PostgreSQL database dump complete
--


