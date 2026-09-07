package com.loupsolitaire.backend.exception;

// Reservee aux controles d'appartenance (un joueur/combat qui n'appartient pas
// a l'utilisateur authentifie). Pas encore utilisee a ce stade du projet -
// prete pour les controleurs Joueur/Combat a venir (voir modele-donnees.md, section 5).
public class AccesRefuseException extends RuntimeException {
    public AccesRefuseException(String message) {
        super(message);
    }
}
