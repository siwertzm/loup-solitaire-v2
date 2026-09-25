package com.loupsolitaire.backend.service;

import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.loupsolitaire.backend.model.TirageCreation;
import com.loupsolitaire.backend.repository.TirageCreationRepository;

import lombok.RequiredArgsConstructor;

// Tirage des caracteristiques de creation, fait cote serveur (SEC-01).
//
// Appele par PartieService, qui verrouille d'abord la ligne de l'utilisateur
// (UtilisateurRepository.findByIdPourModification) : les deux methodes
// ci-dessous ne sont donc jamais executees en parallele pour un meme
// utilisateur.
@Service
@RequiredArgsConstructor
public class TirageCreationService {

    public static final int BASE_HABILITE = 10;
    public static final int BASE_ENDURANCE = 20;

    private final TirageCreationRepository tirageCreationRepository;
    private final TableDeHasardService tableDeHasardService;

    // Renvoie le tirage en attente s'il existe (pas de relance possible),
    // sinon tire les deux chiffres et les enregistre.
    @Transactional
    public TirageCreation tirerOuRelire(UUID utilisateurId) {
        return tirageCreationRepository.findById(utilisateurId)
                .orElseGet(() -> tirageCreationRepository.save(nouveauTirage(utilisateurId)));
    }

    // Utilise le tirage en attente pour creer un personnage, et le supprime
    // (le personnage suivant aura droit a un nouveau tirage). Si la creation
    // echoue ensuite (disciplines invalides...), la transaction est annulee
    // et le tirage reste en attente, inchange.
    @Transactional
    public TirageCreation consommer(UUID utilisateurId) {
        TirageCreation tirage = tirageCreationRepository.findById(utilisateurId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Aucun tirage en attente : lancez les des (POST /personnages/tirage) avant de creer le personnage"));
        tirageCreationRepository.delete(tirage);
        return tirage;
    }

    private TirageCreation nouveauTirage(UUID utilisateurId) {
        TirageCreation tirage = new TirageCreation();
        tirage.setUtilisateurId(utilisateurId);
        tirage.setHasardHabilite(tableDeHasardService.tirerChiffre());
        tirage.setHasardEndurance(tableDeHasardService.tirerChiffre());
        tirage.setCreeLe(Instant.now());
        return tirage;
    }
}