package com.loupsolitaire.backend.exception;

// Limite de debit atteinte (SEC-02) : reponse 429 avec l'en-tete
// Retry-After, en secondes (voir GlobalExceptionHandler et
// LimitationDebitFilter).
public class TropDeRequetesException extends RuntimeException {

    private final long secondesAvantNouvelEssai;

    public TropDeRequetesException(long secondesAvantNouvelEssai) {
        super("Trop de tentatives, reessayez dans " + secondesAvantNouvelEssai + " secondes");
        this.secondesAvantNouvelEssai = secondesAvantNouvelEssai;
    }

    public long getSecondesAvantNouvelEssai() {
        return secondesAvantNouvelEssai;
    }
}