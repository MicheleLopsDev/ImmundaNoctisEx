#!/usr/bin/env python3
# Rigenera i pacchetti di Fase 0 da `01-romanzo/`.
#
# Serve perché ogni pacchetto è prompt + testo del capitolo INCOLLATO
# DENTRO: se i capitoli cambiano e i pacchetti no, si finisce per dare
# al modello una versione vecchia del romanzo. È successo il 07/08/2026,
# dopo la conversione alla forma LIBROGAME — da lì questo script.
#
#   python libri/erede-dell-astra/prompt/genera-pacchetti.py
#
# I file in questa cartella sono usa e getta: si possono cancellare
# tutti e rifare (vedi doc/CANTIERE-DI-UN-LIBRO.md).

import io
import os
import sys

PROMPT = """Ti do UN CAPITOLO di un romanzo fantasy. Trasformalo in una CATENA DI
TAPPE: il pezzo di percorso canonico di un librogame, quello piu' ricco,
dove il lettore vede tutto e raccoglie tutto.

Il libro resta come l'autore l'ha deciso. Puoi tagliarlo in tappe, e
puoi aggiungere UNA frase per chiudere una tappa che il taglio
lascerebbe monca: nient'altro. Non riscrivere, non accorciare, non
convertire la persona o il tempo, non correggere refusi. Se ti viene la
tentazione di "far scorrere meglio" una frase dell'autore, non farlo.

1. TAGLIA IN TAPPE
   Una tappa finisce dove il protagonista DECIDE qualcosa, o dove cambia
   luogo o interlocutore — mai a un tot di parole fisso. Ogni tappa e' un
   pezzo continuo del testo originale.
   Numera le tappe da {inizio} a {fine} (sono riservate a questo
   capitolo; se te ne servono meno, lascia i numeri liberi in fondo).

2. MARCA LE MECCANICHE DI GIOCO gia' presenti nel testo:
   - SCONTRO: dove il testo descrive un combattimento (nel materiale e'
     delimitato da *[Inizio Scontro]* e *[Fine Scontro]*). Segna chi
     combatte.
   - ABILITA': dove il protagonista usa una dote (nel testo e' scritto
     come [Disciplina: Nome]). Segna quale.
   - PROVA: dove il testo dice che qualcosa poteva andare storto — una
     serratura, un salto, una menzogna, un inseguimento. Anche senza
     marcatore: si riconosce perche' l'esito e' incerto.
   - ACQUISIZIONE: dove il protagonista ottiene o perde qualcosa che
     contera' dopo — un oggetto, un alleato, un'informazione. Nel testo
     e' scritto come [Ottieni: ...] o [Perdi: ...]. Segnala anche le
     acquisizioni che il testo NON marca ma che si capiscono leggendo.
   - ATTIVAZIONE: dove un oggetto che il protagonista ha GIA' addosso
     diventa attivo, o smette di esserlo. Nel testo e' scritto come
     [Attiva: ...] o [Disattiva: ...]. Non e' un'acquisizione: non lo
     riceve ora, ce l'aveva. Segnala anche le attivazioni che il testo
     non marca ma che si capiscono leggendo (un amuleto che "si sveglia").

3. SEGNALA I BIVI FORZATI: i punti in cui il romanzo fa scegliere al
   protagonista e racconta un solo esito. Per ognuno scrivi DUE righe
   separate: la strada che il romanzo prende, e quella che il testo
   nomina o lascia intendere e non percorre. Separate, perche' e' la
   seconda che diventa un ramo nuovo.
   Le tappe SENZA bivio non scrivono niente: un "no" ripetuto trenta
   volte nasconde le poche righe che dicono qualcosa.

4. CONTROLLA LA FORMA NARRATIVA. Un libro-game si scrive in due modi, e
   l'autore ne sceglie UNO per tutto il libro:
   - LIBROGAME: seconda persona, "Apri la porta", "Scarti di lato";
   - DIARIO: prima persona, "Apro la porta", "Scarto di lato".
   Tutto il resto e' TERZA persona ("Ariel apri' la porta"), che guarda
   il protagonista da fuori.

   Dichiara la forma UNA VOLTA, in testa al capitolo. NON ripeterla su
   ogni tappa: se il capitolo e' tutto LIBROGAME, cinquanta righe che
   dicono "LIBROGAME" nascondono le poche che dicono altro. Annotala
   solo sulle tappe che se ne DISCOSTANO.

   NON correggere niente: e' un AVVISO. Una scena puramente descrittiva
   in terza persona puo' restare tale — succede anche nei libri
   pubblicati — mentre una in cui il protagonista agisce di solito va
   convertita, ma lo decide l'autore. Per le tappe in terza di' anche se
   ti sembra DESCRITTIVA (il protagonista non agisce: un luogo, un
   antefatto, una scena vista da lontano) o AZIONE (il protagonista fa
   qualcosa).

5. DICHIARA OGNI TOCCO AL TESTO. La riga **Testo** compare SOLO sulle
   tappe che hai toccato, con la frase esatta che hai aggiunto:
   l'autore la vede e decide. Sulle tappe intatte NON scrivere niente —
   stessa ragione della forma: cinquanta righe che dicono tutte
   "nessuna aggiunta" nascondono le due che dicono altro.

6. CHIUDI CON UN CONTEGGIO: quante tappe, quanti scontri, quante
   abilita' usate, quante prove, quante acquisizioni, quanti bivi
   forzati, la forma prevalente del capitolo (LIBROGAME o DIARIO) con
   quante tappe se ne discostano, quante tappe in terza persona (di cui
   quante descrittive) e quante tappe hanno il testo modificato.

FORMATO
## Tappa <n> — <titolo> (Cap. {capitolo})
- **Riferimento inizio**: «<prima frase della tappa>»
- **Riferimento fine**: «<ultima frase della tappa>»
- **Meccaniche di gioco**: <SCONTRO: … | ABILITA': … | PROVA: … |
  ACQUISIZIONE: … | nessuna>
- **Bivio forzato**: <SOLO se c'e' un bivio, o se il "no" ha una nota
  che serve (es. "no — un ordine reale, il romanzo non offre il
  rifiuto"). Un "no" nudo NON si scrive: ometti la riga.>
  - **Il romanzo sceglie**: <la strada che il romanzo prende>
  - **Alternativa**: <quella che il testo lascia intendere e non percorre>
  - **Perché conta**: <solo se c'e' qualcosa da dire: il peso di quel
    bivio nel libro>
- **Testo**: <SOLO se hai toccato il testo: aggiunta «<la frase
  esatta>» in coda, per chiudere la tappa. Ometti la riga sulle tappe
  intatte>
- **Nota sulla forma**: <solo se la tappa si discosta dalla forma del
  capitolo: TERZA (descrittiva) | TERZA (azione) | l'altra forma>

CAPITOLO {capitolo}:
"""

# Numerazione a blocchi: cap.1 -> 1-9, cap.2 -> 10-19, e cosi' via, cosi'
# le tappe non si scontrano fra capitoli e non serve ricucire niente.
def intervallo(capitolo: int) -> tuple[int, int]:
    inizio = 1 if capitolo == 1 else (capitolo - 1) * 10
    return inizio, inizio + (8 if capitolo == 1 else 9)


def main() -> int:
    qui = os.path.dirname(os.path.abspath(__file__))
    cantiere = os.path.dirname(qui)
    romanzo = os.path.join(cantiere, "01-romanzo")

    capitoli = sorted(f for f in os.listdir(romanzo) if f.startswith("cap-") and f.endswith(".md"))
    if not capitoli:
        print(f"nessun capitolo in {romanzo}", file=sys.stderr)
        return 1

    for nome in capitoli:
        numero = int(nome[4:6])
        testo = io.open(os.path.join(romanzo, nome), encoding="utf-8").read().strip()
        inizio, fine = intervallo(numero)
        contenuto = PROMPT.format(inizio=inizio, fine=fine, capitolo=numero) + "\n" + testo + "\n"
        fuori = os.path.join(qui, f"fase0-cap-{numero:02d}.txt")
        # Su un temporaneo e poi sostituzione: una scrittura interrotta a
        # meta' lascerebbe il file troncato (successo davvero, 06/08).
        with io.open(fuori + ".tmp", "w", encoding="utf-8", newline="\n") as fh:
            fh.write(contenuto)
        os.replace(fuori + ".tmp", fuori)
        print(f"cap {numero:02d}: tappe {inizio}-{fine}, {len(contenuto):,} byte")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
