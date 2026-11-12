#!/usr/bin/env python3
"""OPS-01 : test de charge, N joueurs simultanes qui jouent vraiment.

Chaque joueur cree son compte et son personnage, puis enchaine les chapitres :
lecture du chapitre, combat (ATTAQUE jusqu'a la fin) quand il y en a un,
choix d'un lien disponible, resurrection ou retour apres une defaite.
Bibliotheque standard uniquement.

A lancer contre une base de TEST, jamais la production (comptes crees,
limitation de debit et e-mails a desactiver cote serveur) :

    python3 scripts/charge.py --url http://localhost:8081 --joueurs 50 --duree 180 \\
        --verifier "docker exec loup-db psql -U postgres -d loup_charge -c \\
            \\"update utilisateur set email_verifie = true where username like 'charge-%'\\""

--verifier : commande shell lancee apres les inscriptions pour valider les
e-mails (le lien de verification n'est pas envoye pendant le test).
"""

import argparse
import json
import random
import subprocess
import threading
import time
import urllib.error
import urllib.request
from collections import Counter

# Identifiants attendus par l'API (ceux de GET /disciplines).
DISCIPLINES = ["CAMOUFLAGE", "CHASSE", "SIXIEME_SENS", "ORIENTATION", "GUERISON",
               "MAITRISE_ARMES", "BOUCLIER_PSYCHIQUE", "PUISSANCE_PSYCHIQUE",
               "COMMUNICATION_ANIMALE", "MAITRISE_MATIERE"]

statuts = Counter()
durees = []
verrou = threading.Lock()


def appel(url, methode, chemin, jeton=None, corps=None):
    donnees = json.dumps(corps).encode() if corps is not None else None
    requete = urllib.request.Request(url + chemin, data=donnees, method=methode)
    requete.add_header("Content-Type", "application/json")
    if jeton:
        requete.add_header("Authorization", "Bearer " + jeton)
    debut = time.perf_counter()
    try:
        with urllib.request.urlopen(requete, timeout=30) as reponse:
            code, texte = reponse.status, reponse.read()
    except urllib.error.HTTPError as e:
        code, texte = e.code, e.read()
    except OSError:
        code, texte = 0, b""
    with verrou:
        statuts[code] += 1
        durees.append(time.perf_counter() - debut)
    try:
        return code, json.loads(texte) if texte else None
    except ValueError:
        return code, None


def nouveau_personnage(url, jeton):
    appel(url, "POST", "/personnages/tirage", jeton)
    code, perso = appel(url, "POST", "/personnages", jeton,
                        {"nom": "Loup", "disciplines": random.sample(DISCIPLINES, 5)})
    return perso["id"] if code == 201 and perso else None


def jouer(url, nom, fin, pause):
    _, auth = appel(url, "POST", "/auth/login", corps={"identifiant": nom, "password": "motdepasse-charge"})
    if not auth:
        return
    jeton = auth["accessToken"]
    perso = nouveau_personnage(url, jeton)
    while perso and time.time() < fin:
        code, chapitre = appel(url, "GET", f"/personnages/{perso}/chapitre", jeton)
        appel(url, "GET", f"/personnages/{perso}", jeton)
        if code != 200 or not chapitre:
            perso = nouveau_personnage(url, jeton)
            continue
        if chapitre["combat"] and chapitre["ennemis"]:
            _, combat = appel(url, "POST", f"/personnages/{perso}/combat", jeton)
            for _ in range(40):
                if not combat or combat.get("statut") != "EN_COURS":
                    break
                time.sleep(pause / 4)
                _, combat = appel(url, "POST", f"/personnages/{perso}/combat/tour", jeton, {"action": "ATTAQUE"})
            if combat and combat.get("statut") == "DEFAITE":
                appel(url, "POST", f"/personnages/{perso}/chapitre/revenir-apres-defaite", jeton)
                continue
        liens = [l["chapitreCibleId"] for l in chapitre["liens"] if l["disponible"]]
        random.shuffle(liens)
        avance = False
        for cible in liens:
            code, reponse = appel(url, "POST", f"/personnages/{perso}/chapitre/{cible}", jeton)
            if code == 200:
                avance = True
                if reponse and reponse.get("mort"):
                    appel(url, "POST", f"/personnages/{perso}/ressusciter", jeton)
                break
        if not avance:
            # Bloque (vol a resoudre, fin du tome...) : nouvelle partie.
            appel(url, "DELETE", f"/personnages/{perso}", jeton)
            perso = nouveau_personnage(url, jeton)
        time.sleep(random.uniform(pause / 2, pause * 1.5))


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--url", default="http://localhost:8081")
    parser.add_argument("--joueurs", type=int, default=50)
    parser.add_argument("--duree", type=int, default=180, help="secondes")
    parser.add_argument("--pause", type=float, default=1.0, help="temps de reflexion moyen, secondes")
    parser.add_argument("--verifier", required=True)
    args = parser.parse_args()

    noms = [f"charge-{int(time.time())}-{i}" for i in range(args.joueurs)]
    for nom in noms:
        appel(args.url, "POST", "/auth/register", corps={
            "username": nom, "email": nom + "@test.local", "password": "motdepasse-charge"})
    subprocess.run(args.verifier, shell=True, check=True, capture_output=True)
    statuts.clear()
    durees.clear()

    fin = time.time() + args.duree
    joueurs = [threading.Thread(target=jouer, args=(args.url, nom, fin, args.pause)) for nom in noms]
    for j in joueurs:
        j.start()
    for j in joueurs:
        j.join()

    durees.sort()
    total = len(durees)
    print(f"requetes : {total} en {args.duree} s ({total / args.duree:.0f}/s)")
    print("statuts  : " + ", ".join(f"{c}={n}" for c, n in sorted(statuts.items())))
    if total:
        print(f"duree    : mediane {durees[total // 2] * 1000:.0f} ms, "
              f"p95 {durees[int(total * 0.95)] * 1000:.0f} ms, max {durees[-1] * 1000:.0f} ms")


if __name__ == "__main__":
    main()
