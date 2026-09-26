package com.loupsolitaire.backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * OPS-02 : file d'envoi des emails, hors du fil de la requete HTTP.
 *
 * Un serveur SMTP lent (Brevo en panne, reseau sature) ne bloque plus
 * l'inscription ni le mot de passe oublie : la requete repond des que la
 * transaction est validee, l'email part ensuite depuis cette file.
 *
 * - 2 envois en parallele au plus : suffisant pour le volume attendu et
 *   evite de saturer le fournisseur SMTP ;
 * - 500 emails en attente au plus : au-dela, l'email est abandonne et
 *   journalise (voir EmailService), la requete n'echoue pas ;
 * - a l'arret (redeploiement Render), les emails deja en file ont 30 s pour
 *   partir.
 */
@Configuration
public class EnvoiEmailsConfig {

    public static final String EXECUTOR = "envoiEmailsExecutor";

    @Bean(name = EXECUTOR)
    public ThreadPoolTaskExecutor envoiEmailsExecutor(
            @Value("${app.mail.envois-paralleles:2}") int envoisParalleles,
            @Value("${app.mail.file-attente-max:500}") int fileAttenteMax) {

        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(envoisParalleles);
        executor.setMaxPoolSize(envoisParalleles);
        executor.setQueueCapacity(fileAttenteMax);
        executor.setThreadNamePrefix("envoi-email-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        return executor;
    }
}