import {
  Component,
  computed,
  effect,
  HostListener,
  inject,
  input,
  OnDestroy,
  output,
  signal,
} from '@angular/core';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';

import { ObjetChapResponse } from '../../../../core/models/chapitre.model';
import {
  InventaireItem,
  ObjetResume,
  PersonnageResume,
} from '../../../../core/models/personnage.model';
import { ChapitreService } from '../../../../core/services/chapitre.service';
import { PersonnageService } from '../../../../core/services/personnage.service';
import { InventaireSheetService } from '../../../../core/services/inventaire-sheet.service';

/**
 * Contenu de l'onglet "objets" de l'écran chapitre : la liste des objets
 * proposés par le chapitre courant (avec ramassage) + le résumé de
 * l'inventaire du personnage (armes/objets+repas/bourse).
 *
 * Extrait de ChapitrePage pour garder ce fichier gérable : toute la logique
 * de ramassage (quantités restantes, appel API, décompte) est autonome ici.
 * Les données viennent du parent (qui reste seul propriétaire des appels
 * réseau /disciplines, /objets, /personnages/{id}) ; ce composant se
 * contente de les afficher et de notifier le parent après un ramassage
 * réussi via `ramasse`, pour qu'il mette à jour sa propre fiche personnage.
 *
 * Le détail complet de l'inventaire (feuille "SAC À DOS") est un composant
 * global partagé (shared/inventaire-sheet/), ouvrable depuis n'importe où
 * dans l'appli — ce composant se contente de déclencher son ouverture.
 *
 * Objets à valeur positive proposés par un chapitre, OPTIONNELS ou non :
 * - OPTIONNEL (bouton PRENDRE) : le joueur choisit.
 *   Un appui court prend 1 exemplaire.
 *   Un appui prolongé permet d'en prendre plusieurs automatiquement.
 * - OBLIGATOIRE (bouton AUTOMATIQUE) : appliqué tout seul à l'arrivée sur le
 *   chapitre (voir PersonnageService.avancerVersChapitre). Si la catégorie
 *   était déjà pleine à ce moment-là, une partie peut ne pas avoir pu être
 *   appliquée : GET /chapitre le signale alors comme un "restant" au même
 *   titre qu'un objet optionnel (voir ChapitreMapper côté backend), et le
 *   bouton devient cliquable (même popup "catégorie pleine" que pour un
 *   optionnel), pour compléter le manque une fois de la place libérée.
 */
@Component({
  selector: 'app-chapitre-objets',
  standalone: true,
  imports: [TranslatePipe],
  templateUrl: './chapitre-objets.component.html',
  styleUrl: './chapitre-objets.component.scss',
})
export class ChapitreObjetsComponent implements OnDestroy {
  private readonly chapitreService = inject(ChapitreService);
  private readonly personnageService = inject(PersonnageService);
  private readonly inventaireSheet = inject(InventaireSheetService);
  private readonly translate = inject(TranslateService);

  readonly objets = input.required<ObjetChapResponse[]>();
  readonly tousObjets = input.required<ObjetResume[]>();
  readonly personnage = input<PersonnageResume | null>(null);
  readonly personnageId = input<string | null>(null);

  /** Émis avec la fiche personnage à jour après un ramassage/retrait réussi. */
  readonly ramasse = output<PersonnageResume>();

  readonly inventaire = computed(() => this.personnage()?.inventaire ?? []);

  // Alignés sur InventaireService (backend) :
  // MAX_ARMES=2, MAX_OBJETS_ET_REPAS=8 (partagé entre OBJET et REPAS), MAX_BOURSE=50.
  readonly maxArmes = 2;
  readonly maxObjetsEtRepas = 8;
  readonly maxBourse = 50;

  readonly armesCount = computed(() =>
    this.inventaire()
      .filter((i) => i.categorie === 'ARME')
      .reduce((total, i) => total + i.quantite, 0),
  );

  readonly objetsCount = computed(() =>
    this.inventaire()
      .filter((i) => i.categorie === 'OBJET')
      .reduce((total, i) => total + i.quantite, 0),
  );

  readonly repasCount = computed(() =>
    this.inventaire()
      .filter((i) => i.categorie === 'REPAS')
      .reduce((total, i) => total + i.quantite, 0),
  );

  readonly objetsEtRepasCount = computed(
    () => this.objetsCount() + this.repasCount(),
  );

  readonly bourseCount = computed(() =>
    this.inventaire()
      .filter((i) => i.categorie === 'BOURSE')
      .reduce((total, i) => total + i.quantite, 0),
  );

  // Aligné sur InventaireSheetComponent : même logique "MAÎTRISÉE" (+2 HAB),
  // comparaison par NOM (PersonnageMapper envoie armeMaitrisee comme nom, pas objetId).
  readonly bonusArmeMaitrisee = 2;

  // Quantité restante à ramasser/compléter par objet du chapitre (objetId ->
  // valeur). GET /chapitre renvoie déjà ce "restant" calculé côté serveur
  // (déclaré - déjà pris/appliqué), optionnel ou non : on la décrémente
  // localement à chaque prise réussie pour un retour visuel immédiat.
  readonly restants = signal<Record<string, number>>({});
  readonly ramassageEnCours = signal<string | null>(null);

  /*
   * Gestion de l'appui prolongé sur PRENDRE.
   *
   * - Appui court : 1 objet.
   * - Après 400 ms : passage en appui long.
   * - Tant que le bouton reste maintenu, on tente un nouveau ramassage
   *   régulièrement.
   *
   * prendreObjet() protège déjà contre plusieurs requêtes simultanées grâce
   * à ramassageEnCours(), donc l'intervalle ne peut pas lancer plusieurs
   * appels API en parallèle.
   */
  private appuiTimeout: ReturnType<typeof setTimeout> | null = null;
  private appuiInterval: ReturnType<typeof setInterval> | null = null;

  private objetMaintenu:
    | {
        objetId: string;
        optionnel: boolean;
      }
    | null = null;

  private appuiLongEnCours = false;

  /*
   * Un pointerdown de souris/doigt est normalement suivi d'un événement
   * click au relâchement. Comme le clic court est déjà traité dans
   * arreterAppui(), ce drapeau permet d'ignorer ce click automatique.
   *
   * Un click provenant du clavier n'a pas de pointerdown : il continue donc
   * de fonctionner normalement pour l'accessibilité.
   */
  private ignorerProchainClic = false;

  constructor() {
    // Réinitialise les quantités restantes à chaque nouveau chapitre (l'input
    // `objets` change), remplaçant le reset manuel que faisait auparavant le
    // parent dans son abonnement à GET /chapitre.
    effect(() => {
      const init: Record<string, number> = {};
      this.objets().forEach((o) => (init[o.objetId] = o.valeur));
      this.restants.set(init);

      // Sécurité : si le chapitre change pendant un maintien.
      this.annulerAppui();
    });
  }

  /**
   * Catégorie d'un objet (ARME/OBJET/OBJETS_SPECIAUX/REPAS/BOURSE), résolue
   * via le catalogue GET /objets : ObjetChapResponse (les objets d'un
   * chapitre) ne porte pas la catégorie, seulement objetId/nom/valeur/optionnel.
   */
  categorieObjet(objetId: string): string | null {
    return (
      this.tousObjets().find(
        (o) => o.id.toLowerCase() === objetId.toLowerCase(),
      )?.categorie ?? null
    );
  }

  /** Quantité restante à ramasser/compléter pour un objet du chapitre. */
  restant(objetId: string): number {
    return this.restants()[objetId] ?? 0;
  }

  /**
   * Un objet ne peut pas toujours être ramassé même si le chapitre en
   * propose : si la catégorie correspondante de l'inventaire est déjà au
   * plafond (armes, objets+repas, bourse — alignés sur InventaireService
   * backend), il faut d'abord en libérer un. OBJETS_SPECIAUX n'a pas de
   * plafond (voir InventaireService.limitePour), toujours faux ici.
   */
  estPlein(categorie: string | null): boolean {
    switch (categorie) {
      case 'ARME':
        return this.armesCount() >= this.maxArmes;

      case 'OBJET':
      case 'REPAS':
        return this.objetsEtRepasCount() >= this.maxObjetsEtRepas;

      case 'BOURSE':
        return this.bourseCount() >= this.maxBourse;

      default:
        return false;
    }
  }

  /** Objets actuellement possédés dans une catégorie (pour le popup "libérer de la place"). */
  itemsDeCategorie(
    categorie: string | null,
  ): (InventaireItem & { maitrisee?: boolean })[] {
    if (categorie === 'ARME') {
      const maitriseeNom = this.personnage()?.armeMaitrisee ?? null;

      return this.inventaire()
        .filter((i) => i.categorie === 'ARME')
        .map((i) => ({
          ...i,
          maitrisee: maitriseeNom !== null && i.nom === maitriseeNom,
        }));
    }

    if (categorie === 'OBJET' || categorie === 'REPAS') {
      return this.inventaire().filter(
        (i) => i.categorie === 'OBJET' || i.categorie === 'REPAS',
      );
    }

    return this.inventaire().filter((i) => i.categorie === categorie);
  }

  libelleCategorie(categorie: string | null): string {
    switch (categorie) {
      case 'ARME':
        return this.translate.instant('CHAPITRE_OBJETS.LIBELLE_ARMES');

      case 'OBJET':
      case 'REPAS':
        return this.translate.instant(
          'CHAPITRE_OBJETS.LIBELLE_OBJETS_REPAS',
        );

      case 'BOURSE':
        return this.translate.instant('CHAPITRE_OBJETS.LIBELLE_BOURSE');

      default:
        return '';
    }
  }

  /** Catégorie affichée dans le popup de libération de place ; null = fermé. */
  readonly popupCategorie = signal<string | null>(null);

  /** Objet du chapitre qu'on essayait de ramasser/compléter quand le popup s'est ouvert. */
  private readonly objetEnAttente = signal<{
    objetId: string;
    optionnel: boolean;
  } | null>(null);

  readonly retraitPopupEnCours = signal<string | null>(null);

  /**
   * Début d'un appui souris/tactile sur le bouton PRENDRE.
   *
   * On attend 400 ms avant de considérer qu'il s'agit d'un appui long.
   * Avant ce délai, le relâchement sera traité comme un clic classique.
   */
  demarrerAppui(
    objet: { objetId: string; optionnel: boolean },
    event: PointerEvent,
  ): void {
    if (this.restant(objet.objetId) <= 0) {
      return;
    }

    if (this.estPlein(this.categorieObjet(objet.objetId))) {
      return;
    }

    this.annulerAppui();

    this.objetMaintenu = objet;
    this.appuiLongEnCours = false;
    this.ignorerProchainClic = true;

    // Empêche certains comportements natifs parasites lors du maintien.
    event.preventDefault();

    this.appuiTimeout = setTimeout(() => {
      if (
        !this.objetMaintenu ||
        this.objetMaintenu.objetId !== objet.objetId
      ) {
        return;
      }

      this.appuiLongEnCours = true;

      // Premier objet de l'appui prolongé.
      this.prendrePendantAppui(objet);

      // Puis répétition tant que le doigt / clic reste maintenu.
      this.appuiInterval = setInterval(() => {
        this.prendrePendantAppui(objet);
      }, 150);
    }, 400);
  }

  /**
   * Le navigateur génère également un click après pointerup.
   * Pour souris/tactile on l'ignore car le comportement est déjà géré par
   * pointerdown/pointerup.
   *
   * Pour une activation clavier, aucun pointerdown n'a eu lieu :
   * on conserve donc le comportement normal +1.
   */
  gererClicObjet(objet: {
    objetId: string;
    optionnel: boolean;
  }): void {
    if (this.ignorerProchainClic) {
      this.ignorerProchainClic = false;
      return;
    }

    this.prendreObjet(objet);
  }

  /**
   * Pendant un maintien on ne ramasse que si :
   * - il reste encore quelque chose ;
   * - l'inventaire n'est pas plein ;
   * - aucune requête précédente n'est encore en cours.
   */
  private prendrePendantAppui(objet: {
    objetId: string;
    optionnel: boolean;
  }): void {
    if (
      !this.objetMaintenu ||
      this.objetMaintenu.objetId !== objet.objetId
    ) {
      return;
    }

    if (this.restant(objet.objetId) <= 0) {
      this.annulerAppui();
      return;
    }

    const categorie = this.categorieObjet(objet.objetId);

    if (this.estPlein(categorie)) {
      this.annulerAppui();
      return;
    }

    this.prendreObjet(objet);
  }

  /**
   * Pointerup peut arriver en dehors du bouton si l'utilisateur déplace
   * légèrement son doigt. L'écoute globale garantit donc toujours l'arrêt.
   */
  @HostListener('window:pointerup')
  arreterAppui(): void {
    if (!this.objetMaintenu) {
      return;
    }

    const objet = this.objetMaintenu;
    const etaitUnAppuiLong = this.appuiLongEnCours;

    this.nettoyerTimersAppui();

    this.objetMaintenu = null;
    this.appuiLongEnCours = false;

    // Si le maintien n'a pas atteint 400 ms, c'est un clic normal : +1.
    if (!etaitUnAppuiLong) {
      this.prendreObjet(objet);
    }

    /*
     * Normalement un click arrive juste après pointerup et consomme
     * ignorerProchainClic. Ce timeout sert de sécurité si le navigateur ne
     * génère finalement aucun click.
     */
    setTimeout(() => {
      this.ignorerProchainClic = false;
    }, 0);
  }

  @HostListener('window:pointercancel')
  annulerAppui(): void {
    this.nettoyerTimersAppui();
    this.objetMaintenu = null;
    this.appuiLongEnCours = false;

    setTimeout(() => {
      this.ignorerProchainClic = false;
    }, 0);
  }

  private nettoyerTimersAppui(): void {
    if (this.appuiTimeout !== null) {
      clearTimeout(this.appuiTimeout);
      this.appuiTimeout = null;
    }

    if (this.appuiInterval !== null) {
      clearInterval(this.appuiInterval);
      this.appuiInterval = null;
    }
  }

  /**
   * Ramasse (optionnel) ou complète (obligatoire) un objet du chapitre —
   * sauf si la catégorie est déjà pleine, auquel cas on ouvre le popup de
   * libération de place au lieu d'appeler l'API (qui refuserait de toute
   * façon, InventaireService plafonne aussi côté serveur).
   */
  prendreObjet(objet: {
    objetId: string;
    optionnel: boolean;
  }): void {
    if (this.restant(objet.objetId) <= 0) {
      return;
    }

    if (this.ramassageEnCours()) {
      return;
    }

    const categorie = this.categorieObjet(objet.objetId);

    if (this.estPlein(categorie)) {
      this.annulerAppui();
      this.objetEnAttente.set(objet);
      this.popupCategorie.set(categorie);
      return;
    }

    this.executerRamassage(objet);
  }

  private executerRamassage(objet: {
    objetId: string;
    optionnel: boolean;
  }): void {
    const id = this.personnageId();

    if (!id) {
      return;
    }

    this.ramassageEnCours.set(objet.objetId);

    this.chapitreService.ramasserObjet(id, objet.objetId).subscribe({
      next: (p) => {
        this.ramasse.emit(p);

        // Optionnel : le backend n'ajoute jamais qu'1 exemplaire par appel.
        // Obligatoire : le backend complète tout le manque en un appel, on
        // repasse donc directement à 0 plutôt que de décrémenter de 1.
        this.restants.update((r) => ({
          ...r,
          [objet.objetId]: objet.optionnel
            ? Math.max(0, this.restant(objet.objetId) - 1)
            : 0,
        }));

        this.ramassageEnCours.set(null);

        // Fin automatique du maintien quand il ne reste plus rien.
        if (this.restant(objet.objetId) <= 0) {
          this.annulerAppui();
        }
      },

      error: (err) => {
        console.error(
          this.translate.instant('CHAPITRE_OBJETS.ERREUR_RAMASSAGE'),
          err,
        );

        this.ramassageEnCours.set(null);
        this.annulerAppui();
      },
    });
  }

  /** Retire 1 exemplaire depuis le popup, puis termine le ramassage initial une fois la place libérée. */
  retirerPourLiberer(objetId: string): void {
    const id = this.personnageId();

    if (!id || this.retraitPopupEnCours()) {
      return;
    }

    this.retraitPopupEnCours.set(objetId);

    this.personnageService.retirerObjet(id, objetId, 1).subscribe({
      next: (p) => {
        this.ramasse.emit(p);
        this.inventaireSheet.notifierMiseAJour(p);
        this.retraitPopupEnCours.set(null);

        const enAttente = this.objetEnAttente();

        this.fermerPopup();

        if (enAttente) {
          this.executerRamassage(enAttente);
        }
      },

      error: (err) => {
        console.error(
          this.translate.instant('CHAPITRE_OBJETS.ERREUR_RETRAIT'),
          err,
        );

        this.retraitPopupEnCours.set(null);
      },
    });
  }

  fermerPopup(): void {
    this.popupCategorie.set(null);
    this.objetEnAttente.set(null);
  }

  /** Ouvre la feuille "SAC À DOS" globale (voir shared/inventaire-sheet/). */
  ouvrirSac(): void {
    const id = this.personnageId();

    if (id) {
      this.inventaireSheet.ouvrir(id);
    }
  }

  ngOnDestroy(): void {
    this.nettoyerTimersAppui();
  }
}