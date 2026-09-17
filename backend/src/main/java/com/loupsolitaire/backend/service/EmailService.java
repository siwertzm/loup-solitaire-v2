package com.loupsolitaire.backend.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.mail.from}")
    private String from;

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

            helper.setFrom(from);
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
                    "tu peux simplement ignorer cet email.";

            String html = """
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
                        width="100%%"
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
                                    width="100%%"
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
                                        <td style="padding:34px 34px 18px 34px;">

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
                                                Ton aventure va commencer
                                            </div>

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
                                                width="100%%"
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
                                                Si tu n'es pas à l'origine de cette
                                                inscription, ignore simplement cet email.
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
                    """
                    .replace("{{LIEN}}", lienSecurise);

            helper.setText(texteBrut, html);

            mailSender.send(message);

        } catch (Exception e) {

            log.error(
                    "Echec de l'envoi de l'email de verification a {} : {}",
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

            helper.setFrom(from);
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
                    "ignore simplement cet email.";

            String html = """
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
                        width="100%%"
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
                                    width="100%%"
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
                                                font-size:11px;
                                                letter-spacing:4px;
                                                text-transform:uppercase;
                                                margin-bottom:10px;
                                            ">
                                                Une aventure légendaire
                                            </div>

                                            <div style="
                                                color:#ece2c8;
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
                                        <td style="padding:34px;">

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
                                                Code de vérification
                                            </div>

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
                                                width="100%%"
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
                                                Si tu n'es pas à l'origine de cette
                                                demande, ignore simplement cet email.
                                                Ton mot de passe ne sera pas modifié.
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
                    """
                    .replace("{{CODE}}", codeSecurise)
                    .replace(
                            "{{EXPIRATION}}",
                            Long.toString(expirationMinutes)
                    );

            helper.setText(texteBrut, html);

            mailSender.send(message);

        } catch (Exception e) {

            log.error(
                    "Echec de l'envoi de l'email de reinitialisation a {} : {}",
                    destinataire,
                    e.getMessage()
            );
        }
    }

    /**
     * Evite d'injecter directement une valeur dynamique dans le HTML.
     */
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