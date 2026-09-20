package com.loupsolitaire.backend.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

class PersonnageTest {

    @Test
    void marquerMortPositionneMortEtMarqueLaDerniereArriveeDuJournal() {
        Personnage personnage = new Personnage();
        personnage.getChapitresParcourus().addAll(List.of(0, 1, 53));

        personnage.marquerMort();

        assertThat(personnage.isMort()).isTrue();
        assertThat(personnage.getEtapesMortelles()).containsExactly(2);
    }

    @Test
    void marquerMortNeMarqueQueLArriveeCouranteMemeSurUnChapitreRevisite() {
        // 85 est traverse deux fois (retour paye) : on ne meurt que la 2e fois.
        Personnage personnage = new Personnage();
        personnage.getChapitresParcourus().addAll(List.of(0, 85, 322, 85));

        personnage.marquerMort();

        assertThat(personnage.getEtapesMortelles()).containsExactly(3);
    }

    @Test
    void marquerMortSansJournalPositionneSeulementMort() {
        // Personnage ancien, cree avant le journal : rien a marquer, mais la mort
        // est bien enregistree.
        Personnage personnage = new Personnage();

        personnage.marquerMort();

        assertThat(personnage.isMort()).isTrue();
        assertThat(personnage.getEtapesMortelles()).isEmpty();
    }

    @Test
    void marquerMortDeuxFoisSurLaMemeArriveeNeMarqueQuUneEtape() {
        Personnage personnage = new Personnage();
        personnage.getChapitresParcourus().addAll(List.of(0, 17));

        personnage.marquerMort();
        personnage.marquerMort();

        assertThat(personnage.getEtapesMortelles()).containsExactly(1);
    }

    @Test
    void uneMortAnterieureResteMarqueeApresUneNouvelleArriveeEtUneNouvelleMort() {
        // Mort en 53, resurrection (retour en 47), puis mort en 17.
        Personnage personnage = new Personnage();
        personnage.getChapitresParcourus().addAll(List.of(0, 47, 53));
        personnage.marquerMort();
        personnage.setMort(false);
        personnage.getChapitresParcourus().addAll(List.of(47, 322, 17));

        personnage.marquerMort();

        assertThat(personnage.getEtapesMortelles()).containsExactlyInAnyOrder(2, 5);
    }
}