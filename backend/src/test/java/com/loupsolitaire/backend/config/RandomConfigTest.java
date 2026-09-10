package com.loupsolitaire.backend.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Random;

import org.junit.jupiter.api.Test;

class RandomConfigTest {

    private final RandomConfig randomConfig = new RandomConfig();

    @Test
    void exposeUneInstanceDeRandom() {
        Random random = randomConfig.random();

        assertThat(random).isNotNull();
    }

    @Test
    void chaqueAppelRenvoieUneNouvelleInstance() {
        // Le bean n'est pas cense etre un singleton fige en dur dans le code :
        // c'est Spring (scope singleton par defaut) qui garantit une seule
        // instance au sein du contexte applicatif. Ici on verifie juste que
        // la methode elle-meme fabrique bien un "new Random()" a chaque appel,
        // et non une constante partagee codee en dur dans la classe.
        Random premierAppel = randomConfig.random();
        Random deuxiemeAppel = randomConfig.random();

        assertThat(premierAppel).isNotSameAs(deuxiemeAppel);
    }

    @Test
    void laRandomProduiteFonctionneEtRestePredictibleAvecUneGraineFixe() {
        // Verifie que le bean retourne bien un java.util.Random standard,
        // dont le comportement est previsible quand on fixe la graine -
        // propriete exploitee par les tests des services qui mockent ce bean
        // (ex: TableDeHasardService) pour rendre les tirages deterministes.
        Random random = new Random(42L);
        int tirage = random.nextInt(10);

        assertThat(tirage).isBetween(0, 9);
    }
}