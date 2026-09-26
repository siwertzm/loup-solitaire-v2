package com.loupsolitaire.backend.service;

import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.loupsolitaire.backend.config.AppProperties;
import com.loupsolitaire.backend.config.EnvoiEmailsConfig;

import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;

/**
 * Construction et envoi des emails.
 *
 * OPS-02 : le message est construit tout de suite, mais l'envoi SMTP part
 * APRES la validation de la transaction en cours, et hors du fil de la
 * requete (file EnvoiEmailsConfig) :
 * - si la transaction est annulee, aucun email ne part avec un lien ou un
 *   code qui n'existe pas en base ;
 * - un SMTP lent ne bloque plus la reponse HTTP ;
 * - un echec SMTP est journalise, jamais propage (comportement inchange).
 * Sans transaction active, l'envoi est simplement mis en file.
 */
@Service
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;
    private final AppProperties appProperties;
    private final Executor envoiEmails;

    public EmailService(
            JavaMailSender mailSender,
            AppProperties appProperties,
            @Qualifier(EnvoiEmailsConfig.EXECUTOR) Executor envoiEmails
    ) {
        this.mailSender = mailSender;
        this.appProperties = appProperties;
        this.envoiEmails = envoiEmails;
    }

    /**
     * Email envoyé après la création du compte.
     */
    public void envoyerEmailVerification(
            String destinataire,
            String lienConfirmation
    ) {
        try {
            MimeMessage message = mailSender.createMimeMessage();

            MimeMessageHelper helper =
                    new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(appProperties.mail().from());
            helper.setTo(destinataire);
            helper.setSubject("Confirme ton entrée dans l'aventure — Loup Solitaire");

            String lienSecurise = echapperHtml(lienConfirmation);

            String texteBrut =
                    "LOUP SOLITAIRE\n\n" +
                    "Bienvenue dans l'aventure.\n\n" +
                    "Il ne reste qu'une étape avant de pouvoir commencer ton périple : " +
                    "confirmer ton adresse email.\n\n" +
                    "Lien de confirmation :\n" +
                    lienConfirmation + "\n\n" +
                    "Ce lien est valable pendant 24 heures.\n\n" +
                    "Si tu n'es pas à l'origine de cette inscription, " +
                    "tu peux simplement ignorer cet email.\n\n" +
                    "Politique de confidentialité : " + lienConfidentialite();

            String html = miseEnPage(
                    "34px 34px 18px 34px",
                    "Ton aventure va commencer",
                    """
                                            <p style="
                                                margin:0 0 17px 0;
                                                color:#ece2c8;
                                                font-size:17px;
                                                line-height:1.65;
                                            ">
                                                Bienvenue, jeune aventurier.
                                            </p>

                                            <p style="
                                                margin:0 0 17px 0;
                                                color:#c9c0a8;
                                                font-size:16px;
                                                line-height:1.65;
                                            ">
                                                Avant de pouvoir parcourir les terres
                                                du Magnamund, nous devons vérifier
                                                ton adresse email.
                                            </p>

                                            <p style="
                                                margin:0 0 28px 0;
                                                color:#c9c0a8;
                                                font-size:16px;
                                                line-height:1.65;
                                            ">
                                                Utilise le sceau ci-dessous pour
                                                confirmer ton compte.
                                            </p>

                                            <!-- BOUTON -->
                                            <table
                                                role="presentation"
                                                width="100%"
                                                cellspacing="0"
                                                cellpadding="0"
                                                border="0"
                                            >
                                                <tr>
                                                    <td align="center">

                                                        <a
                                                            href="{{LIEN}}"
                                                            style="
                                                                display:inline-block;
                                                                padding:15px 28px;
                                                                background-color:#c2703a;
                                                                color:#171c13;
                                                                border-radius:10px;
                                                                font-size:13px;
                                                                font-weight:bold;
                                                                letter-spacing:1.5px;
                                                                text-transform:uppercase;
                                                                text-decoration:none;
                                                            "
                                                        >
                                                            Confirmer mon adresse
                                                        </a>

                                                    </td>
                                                </tr>
                                            </table>

                                            <p style="
                                                margin:27px 0 0 0;
                                                color:#8f8977;
                                                font-size:13px;
                                                line-height:1.6;
                                                text-align:center;
                                            ">
                                                Ce lien est valable pendant
                                                <strong style="color:#d9a94a;">
                                                    24 heures
                                                </strong>.
                                            </p>

                                        </td>
                                    </tr>

                                    <!-- LIEN FALLBACK -->
                                    <tr>
                                        <td style="padding:15px 34px 30px 34px;">

                                            <div style="
                                                padding:16px;
                                                background-color:#202719;
                                                border:1px solid #3c432f;
                                                border-radius:9px;
                                            ">

                                                <p style="
                                                    margin:0 0 8px 0;
                                                    color:#8f8977;
                                                    font-size:12px;
                                                    line-height:1.5;
                                                ">
                                                    Si le bouton ne fonctionne pas,
                                                    copie ce lien dans ton navigateur :
                                                </p>

                                                <p style="
                                                    margin:0;
                                                    color:#c2703a;
                                                    font-size:11px;
                                                    line-height:1.5;
                                                    word-break:break-all;
                                                ">
                                                    {{LIEN}}
                                                </p>

                                            </div>
                    """,
                    "Si tu n'es pas à l'origine de cette inscription, ignore simplement cet email."
            )
                    .replace("{{LIEN}}", lienSecurise);

            helper.setText(texteBrut, html);

            envoyerApresCommit(message, "verification", destinataire);

        } catch (Exception e) {

            log.error(
                    "Echec de la preparation de l'email de verification a {} : {}",
                    destinataire,
                    e.getMessage()
            );
        }
    }

    /**
     * Email envoyé lors d'une demande de mot de passe oublié.
     */
    public void envoyerCodeReinitialisationMotDePasse(
            String destinataire,
            String code,
            long expirationMinutes
    ) {
        try {

            MimeMessage message = mailSender.createMimeMessage();

            MimeMessageHelper helper =
                    new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(appProperties.mail().from());
            helper.setTo(destinataire);
            helper.setSubject("Ton code de vérification — Loup Solitaire");

            String codeSecurise = echapperHtml(code);

            String texteBrut =
                    "LOUP SOLITAIRE\n\n" +
                    "Une demande de réinitialisation de ton mot de passe " +
                    "a été effectuée.\n\n" +
                    "Ton code de vérification :\n\n" +
                    code + "\n\n" +
                    "Ce code est valable pendant " +
                    expirationMinutes +
                    " minutes.\n\n" +
                    "Si tu n'es pas à l'origine de cette demande, " +
                    "ignore simplement cet email.\n\n" +
                    "Politique de confidentialité : " + lienConfidentialite();

            String html = miseEnPage(
                    "34px",
                    "Code de vérification",
                    """
                                            <p style="
                                                margin:0 0 17px 0;
                                                color:#ece2c8;
                                                font-size:17px;
                                                line-height:1.65;
                                            ">
                                                Une demande de nouveau mot de passe
                                                a été effectuée pour ton compte.
                                            </p>

                                            <p style="
                                                margin:0 0 26px 0;
                                                color:#c9c0a8;
                                                font-size:16px;
                                                line-height:1.65;
                                            ">
                                                Saisis le code suivant dans
                                                l'application afin de poursuivre.
                                            </p>

                                            <!-- CODE -->
                                            <table
                                                role="presentation"
                                                width="100%"
                                                cellspacing="0"
                                                cellpadding="0"
                                                border="0"
                                                style="
                                                    margin:0 0 26px 0;
                                                    background-color:#202719;
                                                    border:1px solid #66552f;
                                                    border-radius:12px;
                                                "
                                            >
                                                <tr>
                                                    <td
                                                        align="center"
                                                        style="
                                                            padding:25px 12px;
                                                        "
                                                    >

                                                        <div style="
                                                            color:#d9a94a;
                                                            font-family:'Courier New', monospace;
                                                            font-size:38px;
                                                            font-weight:bold;
                                                            letter-spacing:10px;
                                                        ">
                                                            {{CODE}}
                                                        </div>

                                                    </td>
                                                </tr>
                                            </table>

                                            <p style="
                                                margin:0;
                                                color:#8f8977;
                                                font-size:13px;
                                                line-height:1.6;
                                                text-align:center;
                                            ">
                                                Ce code est valable pendant
                                                <strong style="color:#d9a94a;">
                                                    {{EXPIRATION}} minutes
                                                </strong>.
                                            </p>

                                            <p style="
                                                margin:25px 0 0 0;
                                                color:#8f8977;
                                                font-size:13px;
                                                line-height:1.6;
                                                text-align:center;
                                            ">
                                                Ne communique jamais ce code à
                                                une autre personne.
                                            </p>
                    """,
                    "Si tu n'es pas à l'origine de cette demande, ignore simplement cet email. Ton mot de passe ne sera pas modifié."
            )
                    .replace("{{CODE}}", codeSecurise)
                    .replace(
                            "{{EXPIRATION}}",
                            Long.toString(expirationMinutes)
                    );

            helper.setText(texteBrut, html);

            envoyerApresCommit(message, "reinitialisation", destinataire);

        } catch (Exception e) {

            log.error(
                    "Echec de la preparation de l'email de reinitialisation a {} : {}",
                    destinataire,
                    e.getMessage()
            );
        }
    }

    /**
     * OPS-02 : envoi apres validation de la transaction, dans la file
     * d'envoi. Une transaction annulee n'envoie rien.
     */
    private void envoyerApresCommit(MimeMessage message, String type, String destinataire) {

        Runnable envoi = () -> {
            try {
                mailSender.send(message);
            } catch (Exception e) {
                log.error(
                        "Echec de l'envoi de l'email de {} a {} : {}",
                        type,
                        destinataire,
                        e.getMessage()
                );
            }
        };

        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    mettreEnFile(envoi, type, destinataire);
                }
            });
        } else {
            mettreEnFile(envoi, type, destinataire);
        }
    }

    private void mettreEnFile(Runnable envoi, String type, String destinataire) {
        try {
            envoiEmails.execute(envoi);
        } catch (RejectedExecutionException e) {
            // File pleine ou application en cours d'arret : l'email est
            // perdu, mais la requete du joueur ne doit pas echouer (il peut
            // redemander un lien ou un code).
            log.error("File d'envoi pleine : email de {} a {} abandonne", type, destinataire);
        }
    }

    /**
     * Evite d'injecter directement une valeur dynamique dans le HTML.
     */
    /**
     * Mise en page commune a tous les emails (en-tete, cadre, pied avec le
     * lien vers la politique de confidentialite). Chaque email ne fournit
     * que sa marge, son titre, son contenu et la phrase du pied.
     */
    private static final String GABARIT = """
            <!doctype html>
            <html lang="fr">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>Loup Solitaire</title>
            </head>

            <body style="
                margin:0;
                padding:0;
                background-color:#2c3323;
                font-family:Georgia, 'Times New Roman', serif;
                color:#ece2c8;
            ">

            <table
                role="presentation"
                width="100%"
                cellspacing="0"
                cellpadding="0"
                border="0"
                style="background-color:#2c3323;"
            >
                <tr>
                    <td
                        align="center"
                        style="padding:40px 18px;"
                    >

                        <table
                            role="presentation"
                            width="100%"
                            cellspacing="0"
                            cellpadding="0"
                            border="0"
                            style="
                                max-width:560px;
                                background-color:#171c13;
                                border:1px solid #66552f;
                                border-radius:18px;
                                overflow:hidden;
                            "
                        >

                            <!-- HEADER -->
                            <tr>
                                <td
                                    align="center"
                                    style="
                                        padding:38px 30px 28px 30px;
                                        border-bottom:1px solid #463d27;
                                    "
                                >

                                    <div style="
                                        color:#c2703a;
                                        font-family:Georgia, 'Times New Roman', serif;
                                        font-size:11px;
                                        letter-spacing:4px;
                                        text-transform:uppercase;
                                        margin-bottom:10px;
                                    ">
                                        Une aventure légendaire
                                    </div>

                                    <div style="
                                        color:#ece2c8;
                                        font-family:Georgia, 'Times New Roman', serif;
                                        font-size:30px;
                                        line-height:1.1;
                                        font-weight:bold;
                                        letter-spacing:2px;
                                        text-transform:uppercase;
                                    ">
                                        Loup Solitaire
                                    </div>

                                    <div style="
                                        color:#a9a18c;
                                        font-size:15px;
                                        font-style:italic;
                                        margin-top:9px;
                                    ">
                                        Les Livres dont vous êtes le héros
                                    </div>

                                </td>
                            </tr>

                            <!-- CONTENU -->
                            <tr>
                                <td style="padding:{{PADDING}};">

                                    <div
                                        align="center"
                                        style="
                                            color:#d9a94a;
                                            font-size:18px;
                                            font-weight:bold;
                                            letter-spacing:2px;
                                            text-transform:uppercase;
                                            margin-bottom:20px;
                                        "
                                    >
                                        {{TITRE}}
                                    </div>

                            {{CONTENU}}

                                </td>
                            </tr>

                            <!-- FOOTER -->
                            <tr>
                                <td
                                    align="center"
                                    style="
                                        padding:22px 30px 28px 30px;
                                        border-top:1px solid #463d27;
                                    "
                                >

                                    <div style="
                                        color:#d9a94a;
                                        font-size:22px;
                                        margin-bottom:9px;
                                    ">
                                        ◆
                                    </div>

                                    <p style="
                                        margin:0 0 8px 0;
                                        color:#8f8977;
                                        font-size:12px;
                                        line-height:1.5;
                                    ">
                                        {{PIED}}
                                    </p>

                                    <p style="
                                        margin:0 0 8px 0;
                                        font-size:11px;
                                    ">
                                        <a
                                            href="{{CONFIDENTIALITE}}"
                                            style="color:#8f8977;"
                                        >Politique de confidentialité</a>
                                    </p>

                                    <p style="
                                        margin:0;
                                        color:#605d51;
                                        font-size:11px;
                                    ">
                                        Loup Solitaire
                                    </p>

                                </td>
                            </tr>

                        </table>

                    </td>
                </tr>
            </table>

            </body>
            </html>
            """;

    private String miseEnPage(String marge, String titre, String contenu, String messagePied) {
        return GABARIT
                .replace("{{PADDING}}", marge)
                .replace("{{TITRE}}", titre)
                .replace("{{CONTENU}}", contenu)
                .replace("{{PIED}}", messagePied)
                .replace("{{CONFIDENTIALITE}}", echapperHtml(lienConfidentialite()));
    }

    /**
     * RGPD-04 : lien vers la politique de confidentialite, en pied de chaque
     * email (page statique servie par le backend, voir static/legal).
     */
    private String lienConfidentialite() {
        return appProperties.baseUrl() + "/legal/confidentialite.html";
    }

    private String echapperHtml(String valeur) {

        if (valeur == null) {
            return "";
        }

        return valeur
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}