package com.loupsolitaire.backend.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.loupsolitaire.backend.model.Chapitre;
import com.loupsolitaire.backend.model.Personnage;
import com.loupsolitaire.backend.repository.ChapitreRepository;
import com.loupsolitaire.backend.response.ChapitreParcouruResponse;

import lombok.RequiredArgsConstructor;

// Journal du parcours d'un personnage : les chapitres traverses (Personnage.
// chapitresParcourus) accompagnes du debut du texte de chacun, pour que le
// joueur reconnaisse ses etapes autrement que par un numero.
@Service
@RequiredArgsConstructor
public class JournalService {

    // L'ecran n'affiche qu'une ligne (quelques dizaines de caracteres) ; on
    // garde de la marge pour les grands ecrans sans envoyer des paragraphes
    // entiers pour chaque etape.
    static final int LONGUEUR_MAX_EXTRAIT = 120;

    // Jetons remplaces par le frontend a l'affichage d'un chapitre (ex.
    // "[[ENNEMI]]" = carte de l'ennemi, voir ChapitrePage) : sans sens dans
    // un extrait.
    private static final Pattern JETON = Pattern.compile("\\[\\[[^\\]]*\\]\\]");
    // Le texte des chapitres contient du HTML (<strong>...</strong>) : on
    // garde le contenu, pas le balisage.
    private static final Pattern BALISE = Pattern.compile("<[^>]*>");
    private static final Pattern ESPACES = Pattern.compile("\\s+");

    private final ChapitreRepository chapitreRepository;

    // Ce que le journal sait d'un chapitre, calcule une seule fois par chapitre
    // distinct (un chapitre revisite reutilise le meme apercu).
    private record Apercu(String extrait, boolean combat, boolean effets, boolean objets) {
    }

    private static final Apercu APERCU_VIDE = new Apercu("", false, false, false);

    // Du plus recent au plus ancien : le premier est le chapitre courant. Un
    // chapitre revisite apparait autant de fois qu'il a ete traverse, mais son
    // texte n'est charge qu'une fois.
    @Transactional(readOnly = true)
    public List<ChapitreParcouruResponse> lister(Personnage personnage) {
        List<Integer> ids = new ArrayList<>(personnage.getChapitresParcourus());
        if (ids.isEmpty()) {
            return List.of();
        }

        Map<Integer, Apercu> apercus = chapitreRepository.findAllById(new HashSet<>(ids)).stream()
                .collect(Collectors.toMap(Chapitre::getId, JournalService::apercu));

        Collections.reverse(ids);
        return ids.stream()
                .map(id -> reponse(id, apercus.getOrDefault(id, APERCU_VIDE)))
                .toList();
    }

    // Les effets et objets sont charges par paquets (voir @BatchSize sur
    // Chapitre) : ces appels ne declenchent pas une requete par chapitre.
    private static Apercu apercu(Chapitre chapitre) {
        return new Apercu(
                extraire(chapitre.getText()),
                chapitre.isCombat(),
                !chapitre.getEffets().isEmpty(),
                !chapitre.getObjets().isEmpty());
    }

    private static ChapitreParcouruResponse reponse(Integer id, Apercu apercu) {
        return new ChapitreParcouruResponse(
                id, apercu.extrait(), apercu.combat(), apercu.effets(), apercu.objets());
    }

    // Debut du texte, sur une ligne : sans jetons ni balises, retours a la
    // ligne ramenes a un espace, coupe sur un mot et suivi de "..." (un seul
    // caractere) si le texte est plus long que LONGUEUR_MAX_EXTRAIT.
    static String extraire(String texte) {
        if (texte == null) {
            return "";
        }

        String propre = JETON.matcher(texte).replaceAll(" ");
        propre = BALISE.matcher(propre).replaceAll("");
        propre = ESPACES.matcher(propre).replaceAll(" ").trim();

        if (propre.length() <= LONGUEUR_MAX_EXTRAIT) {
            return propre;
        }

        // Dernier espace avant la limite ; si le debut n'en contient pas (mot
        // tres long), coupe franche plutot qu'un extrait quasi vide.
        int coupe = propre.lastIndexOf(' ', LONGUEUR_MAX_EXTRAIT);
        if (coupe < LONGUEUR_MAX_EXTRAIT / 2) {
            coupe = LONGUEUR_MAX_EXTRAIT;
        }
        return propre.substring(0, coupe).stripTrailing() + "\u2026";
    }
}