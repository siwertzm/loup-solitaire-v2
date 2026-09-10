package com.loupsolitaire.backend.service;

import org.springframework.stereotype.Service;

// Tables de resolution de combat, reprises telles quelles du prototype
// CombatSimulator apres equilibrage (voir conception : ATTACK/DEFEND
// equilibres a ~88% de victoire chacun sur un cas de reference).
//
// TABLE_DEGATS_INFLIGES ("TABLE_LS" dans le prototype) : degats infliges a
// l'ennemi par une action ATTAQUE du joueur.
// TABLE_DEGATS_SUBIS ("TABLE_E" dans le prototype) : degats subis par le
// joueur lors de la riposte de l'ennemi (ATTAQUE/DEFENSE/OBJET).
//
// Indexation : [colonne de rapport de combat][chiffre 0-9 tire]. -999
// signifie un coup fatal net (mort immediate du camp concerne), traite
// comme n'importe quel degat par l'appelant (la verification "<= 0" suffit
// a declencher la mort correctement).
@Service
public class TableCombatService {

    private static final int[][] TABLE_DEGATS_INFLIGES = {
        { -6, 0, 0, 0, 0, -1, -2, -3, -4, -5 },                  // col 0  : -11 ou +
        { -7, 0, 0, 0, -1, -2, -3, -4, -5, -6 },                 // col 1  : -10/-9
        { -8, 0, 0, -1, -2, -3, -4, -5, -6, -7 },                // col 2  : -8/-7
        { -9, 0, -1, -2, -3, -4, -5, -6, -7, -8 },                // col 3  : -6/-5
        { -10, -1, -2, -3, -4, -5, -6, -7, -8, -9 },              // col 4  : -4/-3
        { -11, -2, -3, -4, -5, -6, -7, -8, -9, -10 },             // col 5  : -2/-1
        { -12, -3, -4, -5, -6, -7, -8, -9, -10, -11 },            // col 6  : 0
        { -14, -4, -5, -6, -7, -8, -9, -10, -11, -12 },           // col 7  : 1/2
        { -16, -5, -6, -7, -8, -9, -10, -11, -12, -14 },          // col 8  : 3/4
        { -18, -6, -7, -8, -9, -10, -11, -12, -14, -16 },         // col 9  : 5/6
        { -999, -7, -8, -9, -10, -11, -12, -14, -16, -18 },       // col 10 : 7/8
        { -999, -8, -9, -10, -11, -12, -14, -16, -18, -999 },     // col 11 : 9/10
        { -999, -9, -10, -11, -12, -14, -16, -18, -999, -999 }    // col 12 : 11 ou +
    };

    private static final int[][] TABLE_DEGATS_SUBIS = {
        { -999, 0, -3, -4, -5, -6, -7, -8, -9, -999 },   // col 0  : -11 ou +
        { -999, 0, -3, -4, -5, -6, -6, -7, -7, -10 },    // col 1  : -10/-9
        { -10, 0, -2, -3, -4, -5, -5, -6, -6, -8 },      // col 2  : -8/-7
        { -8, 0, -1, -2, -3, -4, -4, -5, -5, -6 },       // col 3  : -6/-5
        { -7, 0, -1, -1, -2, -3, -4, -4, -5, -5 },       // col 4  : -4/-3
        { -6, 0, -1, -1, -2, -2, -3, -4, -4, -5 },       // col 5  : -2/-1
        { -5, 0, 0, -1, -1, -2, -2, -3, -4, -4 },        // col 6  : 0
        { -5, 0, 0, -1, -1, -2, -2, -3, -3, -4 },        // col 7  : 1/2
        { -4, 0, 0, -1, -1, -2, -2, -2, -3, -3 },        // col 8  : 3/4
        { -4, 0, 0, 0, -1, -1, -2, -2, -3, -3 },         // col 9  : 5/6
        { -4, 0, 0, 0, 0, -1, -2, -2, -2, -3 },          // col 10 : 7/8
        { -3, 0, 0, 0, 0, -1, -2, -2, -2, -3 },          // col 11 : 9/10
        { -3, 0, 0, 0, 0, -1, -1, -2, -2, -2 }           // col 12 : 11 ou +
    };

    // Tirage 0-9 -> % de reduction de degats et bonus d'HABILITE, pendant
    // une action DEFENSE. Le tirage=0 donne un blocage total (100%) mais
    // aucun bonus offensif ; le tirage=9 est le meilleur compromis des deux.
    private static final int[] REDUCTION_DEFENSE = { 100, 25, 30, 40, 50, 60, 70, 80, 85, 90 };
    private static final int[] BONUS_HABILITE = { 5, 1, 1, 2, 2, 3, 3, 3, 4, 4 };

    // Traduit un rapport de combat (HABILETE attaquant - HABILETE
    // defenseur) en index de colonne, clampe sur [-11, 11].
    public int diffVersColonne(int diff) {
        if (diff <= -11) return 0;
        if (diff <= -9) return 1;
        if (diff <= -7) return 2;
        if (diff <= -5) return 3;
        if (diff <= -3) return 4;
        if (diff <= -1) return 5;
        if (diff == 0) return 6;
        if (diff <= 2) return 7;
        if (diff <= 4) return 8;
        if (diff <= 6) return 9;
        if (diff <= 8) return 10;
        if (diff <= 10) return 11;
        return 12;
    }

    public int degatsInfliges(int diff, int tirage) {
        return TABLE_DEGATS_INFLIGES[diffVersColonne(diff)][tirage];
    }

    public int degatsSubis(int diff, int tirage) {
        return TABLE_DEGATS_SUBIS[diffVersColonne(diff)][tirage];
    }

    public int reductionDefense(int tirage) {
        return REDUCTION_DEFENSE[tirage];
    }

    public int bonusHabilite(int tirage) {
        return BONUS_HABILITE[tirage];
    }
}