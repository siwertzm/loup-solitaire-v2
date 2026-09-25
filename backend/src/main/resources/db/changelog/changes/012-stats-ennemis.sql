UPDATE public.ennemi
SET habilite = 16, endurance = 18
WHERE id = 'serpent_aile';

INSERT INTO public.ennemi (id, nom, description, habilite, endurance)
VALUES ('kraan_229', 'Kraan', 'Une créature volante', 16, 25);

UPDATE public.chapitre_ennemi
SET ennemi_id = 'kraan_229'
WHERE chapitre_id = 229;

INSERT INTO public.ennemi (id, nom, description, habilite, endurance)
VALUES ('garde_corps_220', 'Garde du corps', 'Un garde du corps.', 11, 20);

UPDATE public.chapitre_ennemi
SET ennemi_id = 'garde_corps_220'
WHERE chapitre_id = 220;

INSERT INTO public.ennemi (id, nom, description, habilite, endurance)
VALUES ('glok_loup_maudit_340', 'Glok Loup Maudit', 'Un guerrier gobelin monté sur un loup.', 14, 24);

UPDATE public.chapitre_ennemi
SET ennemi_id = 'glok_loup_maudit_340'
WHERE chapitre_id = 340;

UPDATE public.objet_chap
SET valeur = 1
WHERE objet_id = 'lance' AND chapitre_id = 291;

UPDATE public.chapitre
SET text = replace(
    text,
    'l''est, rendez-vous au <strong>215</strong>.',
    'l''est, rendez-vous au <strong>15</strong>.'
)
WHERE id = 201;

UPDATE public.lien
SET chapitre_cible_id = '15'
WHERE chapitre_id = '201' AND chapitre_cible_id = '215';

UPDATE public.objet_chap
SET optionnel = true
WHERE objet_id = 'repas' AND chapitre_id = 62;