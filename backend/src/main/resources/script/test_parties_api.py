#!/usr/bin/env python3
"""
Joue plusieurs parties completes contre le VRAI backend (via HTTP), en
empruntant a chaque etape un lien disponible au hasard, et en resolvant
chaque combat rencontre (ATTAQUE/DEFENSE/OBJET/FUITE melanges).

Usage :
    pip install requests
    python3 test_parties_api.py --base-url http://localhost:8080 \
        --username testcombat --password motdepasse123 --parties 20

Necessite un compte deja verifie (POST /auth/login refuse sinon).
"""

import argparse
import random
import sys
import requests


def log(msg):
    print(msg, flush=True)


class ApiError(Exception):
    def __init__(self, resp):
        self.resp = resp
        super().__init__(f"{resp.request.method} {resp.request.url} -> {resp.status_code} : {resp.text[:300]}")


def call(method, url, token=None, json_body=None, expected=(200, 201)):
    headers = {"Authorization": f"Bearer {token}"} if token else {}
    resp = requests.request(method, url, headers=headers, json=json_body, timeout=10)
    if resp.status_code not in expected:
        raise ApiError(resp)
    return resp.json() if resp.text else None


def login(base_url, username, password):
    data = call("POST", f"{base_url}/auth/login", json_body={
        "identifiant": username, "password": password
    })
    return data["accessToken"]


DISCIPLINES_10 = [
    "CAMOUFLAGE", "CHASSE", "SIXIEME_SENS", "ORIENTATION", "GUERISON",
    "MAITRISE_ARMES", "BOUCLIER_PSYCHIQUE", "PUISSANCE_PSYCHIQUE",
    "COMMUNICATION_ANIMALE", "MAITRISE_MATIERE",
]


def creer_personnage(base_url, token, nom, disciplines):
    return call("POST", f"{base_url}/personnages", token, json_body={
        "nom": nom, "disciplines": disciplines,
    }, expected=(201,))


def ramasser_objets_optionnels(base_url, token, perso_id, chapitre):
    """Ramasse tous les objets optionnels proposes par le chapitre (un
    appel par unite de quantite, comme l'exige POST /objets/{objetId})."""
    for o in chapitre.get("objets", []):
        if not o.get("optionnel"):
            continue
        for _ in range(max(1, o.get("valeur", 1))):
            try:
                call("POST", f"{base_url}/personnages/{perso_id}/objets/{o['objetId']}",
                     token, expected=(200, 201))
            except ApiError:
                # Inventaire plein (409) ou autre refus legitime : on
                # continue sans bloquer la partie.
                pass


def jouer_combat(base_url, token, perso_id):
    """Enchaine des tours jusqu'a resolution. Retourne le statut final."""
    combat = call("POST", f"{base_url}/personnages/{perso_id}/combat", token, expected=(200, 201))
    tours = 0
    while combat["statut"] == "EN_COURS":
        tours += 1
        if tours > 200:
            raise RuntimeError(f"Combat {combat['id']} ne se termine jamais (200+ tours)")

        if combat.get("fuitePossible") and random.random() < 0.15:
            action = "FUITE"
        else:
            action = random.choices(
                ["ATTAQUE", "DEFENSE", "OBJET"], weights=[0.75, 0.20, 0.05]
            )[0]

        body = {"action": action}
        if action == "OBJET":
            # Pas d'inventaire suivi ici : on retente une ATTAQUE si ca echoue.
            try:
                combat = call("POST", f"{base_url}/personnages/{perso_id}/combat/tour",
                               token, json_body=body, expected=(200,))
                continue
            except ApiError:
                body = {"action": "ATTAQUE"}

        combat = call("POST", f"{base_url}/personnages/{perso_id}/combat/tour",
                       token, json_body=body, expected=(200,))
    return combat["statut"]


def jouer_une_partie(base_url, token, index):
    nom = f"TestAuto-{index}-{random.randint(0, 999999)}"
    disciplines = random.sample(DISCIPLINES_10, 5)
    perso = creer_personnage(base_url, token, nom, disciplines)
    perso_id = perso["id"]

    combats_livres = 0
    for step in range(500):
        chapitre = call("GET", f"{base_url}/personnages/{perso_id}/chapitre", token)

        # Seul le chapitre 350 mene a la victoire finale (351) ; ce lien est
        # cependant absent de la base (voir GameDataLoader.PAGES_FIN_DE_JEU),
        # donc on le detecte directement sur l'id plutot que sur ses liens.
        if chapitre["id"] == 350:
            return {"issue": "VICTOIRE_FINALE", "chapitre": 350, "combats": combats_livres}

        ramasser_objets_optionnels(base_url, token, perso_id, chapitre)

        if chapitre["combat"]:
            statut = jouer_combat(base_url, token, perso_id)
            combats_livres += 1
            if statut == "DEFAITE":
                return {"issue": "MORT_COMBAT", "chapitre": chapitre["id"], "combats": combats_livres}
            # Le combat vient de se resoudre : la disponibilite des liens
            # (victoire/fuite/assaut_max/assaut_echec) depend de CET etat,
            # donc on doit relire le chapitre plutot que d'utiliser la
            # copie perimee recuperee AVANT le combat.
            chapitre = call("GET", f"{base_url}/personnages/{perso_id}/chapitre", token)

        liens_dispo = [l for l in chapitre["liens"] if l["disponible"]]
        if not liens_dispo:
            # GameDataLoader ignore volontairement les liens vers 351/352
            # (voir PAGES_FIN_DE_JEU) : un chapitre dont la SEULE sortie y
            # menait se retrouve avec liens=[] en base. Le cas 351 (victoire,
            # uniquement depuis le chapitre 350) est deja intercepte plus
            # haut : ici, liens=[] ne peut donc plus signifier que la mort.
            if not chapitre["liens"]:
                return {"issue": "MORT_NARRATIVE", "chapitre": chapitre["id"], "combats": combats_livres}
            return {"issue": "BLOQUE_LIENS_EXISTANTS_MAIS_INDISPONIBLES",
                    "chapitre": chapitre["id"], "combats": combats_livres}

        cible = random.choice(liens_dispo)["chapitreCibleId"]
        call("POST", f"{base_url}/personnages/{perso_id}/chapitre/{cible}", token, expected=(200, 204))

    return {"issue": "BLOQUE_BOUCLE (500+ pas)", "chapitre": None, "combats": combats_livres}


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--base-url", default="http://localhost:8080")
    parser.add_argument("--username", required=True)
    parser.add_argument("--password", required=True)
    parser.add_argument("--parties", type=int, default=10)
    args = parser.parse_args()

    token = login(args.base_url, args.username, args.password)
    log(f"Connecte. Lancement de {args.parties} parties...\n")

    resultats = {}
    erreurs = []
    for i in range(args.parties):
        try:
            r = jouer_une_partie(args.base_url, token, i)
            resultats[r["issue"]] = resultats.get(r["issue"], 0) + 1
            log(f"[{i+1}/{args.parties}] {r['issue']:<20} (chap {r['chapitre']}, {r['combats']} combat(s))")
        except (ApiError, RuntimeError) as e:
            erreurs.append(str(e))
            log(f"[{i+1}/{args.parties}] ERREUR : {e}")

    log("\n=== Resume ===")
    for issue, count in sorted(resultats.items(), key=lambda kv: -kv[1]):
        log(f"  {issue:<20} {count}")
    if erreurs:
        log(f"\n{len(erreurs)} erreur(s) API rencontree(s) :")
        for e in erreurs:
            log(f"  - {e}")
    else:
        log("\nAucune erreur API rencontree.")


if __name__ == "__main__":
    sys.exit(main())
