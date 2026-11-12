import en from '../../public/assets/i18n/en.json';
import fr from '../../public/assets/i18n/fr.json';

type Traductions = { [cle: string]: string | Traductions };

function aplatir(t: Traductions, prefixe = ''): Map<string, string> {
  const resultat = new Map<string, string>();
  for (const [cle, valeur] of Object.entries(t)) {
    if (typeof valeur === 'string') {
      resultat.set(prefixe + cle, valeur);
    } else {
      aplatir(valeur, `${prefixe}${cle}.`).forEach((v, k) => resultat.set(k, v));
    }
  }
  return resultat;
}

function parametres(valeur: string): string[] {
  return [...valeur.matchAll(/\{\{\s*(\w+)\s*\}\}/g)].map((m) => m[1]).sort();
}

describe('traductions', () => {
  const textesFr = aplatir(fr);
  const textesEn = aplatir(en);

  it('fr.json et en.json ont les memes cles', () => {
    expect([...textesEn.keys()].sort()).toEqual([...textesFr.keys()].sort());
  });

  it('fr.json et en.json ont les memes parametres {{...}}', () => {
    const differents = [...textesFr]
      .filter(([cle]) => textesEn.has(cle))
      .filter(
        ([cle, valeur]) => parametres(valeur).join() !== parametres(textesEn.get(cle)!).join(),
      )
      .map(([cle]) => cle);
    expect(differents).toEqual([]);
  });

  it('fr.json vouvoie le joueur', () => {
    const tutoiement = [...textesFr]
      .filter(([, valeur]) => /(^|[^\p{L}])(tu|toi|ton|ta|tes)(?![\p{L}])/iu.test(valeur))
      .map(([cle]) => cle);
    expect(tutoiement).toEqual([]);
  });

  it('fr.json ecrit HABILETÉ comme le livre', () => {
    const fautes = [...textesFr]
      // hors parametres : {{habilite}} est un nom de variable, pas un texte affiche
      .filter(([, valeur]) => /habilite/i.test(valeur.replace(/\{\{[^}]*\}\}/g, '')))
      .map(([cle]) => cle);
    expect(fautes).toEqual([]);
  });
});
