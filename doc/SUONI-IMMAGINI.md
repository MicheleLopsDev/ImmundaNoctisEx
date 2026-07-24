# Suoni una tantum per le immagini del catalogo

Checklist di lavoro per Michele: quali `.mp3` mancano ancora per le
immagini già presenti nell'app. Non è una specifica, è un elenco da
spuntare mentre si producono i file.

**Convenzione**: stesso nome esatto dell'ID immagine, solo `.mp3`
(es. `loc_market.mp3`). Vanno in `app/src/main/assets/sfx/images/`
(cartella nuova, non ancora creata). Suono **una tantum** (non in
loop), riprodotto insieme all'immagine, senza toccare la musica di
sottofondo. File mancante = silenzio, nessun errore — stessa regola
già in uso per le immagini stesse.

**Manutenzione**: questo elenco va aggiornato ogni volta che cambia
uno dei tre cataloghi sorgente:
- `app/.../inference/SceneImageCatalog.kt` (luoghi, `loc_*`)
- `app/.../inference/EnemyImageCatalog.kt` (nemici/bestie ostili)
- `app/.../inference/NpcImageCatalog.kt` (NPC e incontri pacifici)

Se aggiungi un ID a uno di quei file, aggiungilo anche qui.

---

## Luoghi (`loc_*`) — 35

- [ ] `loc_black_gate` — portale di pietra con statue di teschi cornuti, in un bosco
- [x] `loc_caves` — ingresso di grotta tra le montagne
- [x] `loc_crypt` — ingresso di cripta, rune e teschi sull'arco
- [x] `loc_cursed_castle` — cancello di castello gotico, gargoyle, uno scheletro alla soglia
- [x] `loc_forest` — sentiero nel bosco verso le montagne
- [ ] `loc_forest_prey` — bosco fitto con cervi selvatici
- [ ] `loc_graveyard` — cimitero di notte, luna calante
- [x] `loc_harbor` — porto costiero, castello sull'acqua
- [ ] `loc_helgedad` — skyline di città nera di notte
- [ ] `loc_helgedad_gate` — arco d'ingresso con guardiani cornuti
- [ ] `loc_infernal_city` — città infernale sotto un cielo di tempesta
- [ ] `loc_kai_monastery` — monastero in cima alla montagna
- [x] `loc_market` — mercato cittadino affollato
- [ ] `loc_monastery_dawn` — alba sulle montagne, castello all'orizzonte
- [ ] `loc_mountain` — vetta rocciosa tra le nuvole
- [x] `loc_mountain_pass` — passo di montagna, cavaliere e soldati
- [ ] `loc_smithy_exterior` — esterno di una fucina, di notte
- [x] `loc_smithy_interior` — interno di fucina, fabbro alla forgia
- [ ] `loc_standing_stones` — cerchio di pietre erette, notte di luna
- [x] `loc_storm_tower` — torre di pietra con rune, tempesta di fulmini
- [x] `loc_tavern` — interno di taverna affollata
- [ ] `loc_tomb_exterior` — ingresso di tomba, catene e statue demoniache
- [ ] `loc_tomb_interior` — interno di tomba, sarcofago
- [ ] `loc_warehouse` — interno di magazzino, casse e pergamene
- [x] `loc_abandoned_keep` — rocca abbandonata in rovina, mura invase dall'edera
- [x] `loc_ancient_ruins` — rovine di colonne antiche, invase dalla vegetazione
- [x] `loc_battlefield` — campo di battaglia devastato, armi spezzate, corvi in volo
- [x] `loc_dungeon` — corridoio di prigione sotterranea, sbarre e torce
- [x] `loc_haunted_house` — casa abbandonata in una palude morta
- [x] `loc_swamp` — palude nebbiosa, casolare diroccato, sentiero di assi
- [x] `loc_volcano` — vulcano in eruzione, colate di lava
- [x] `loc_waterfall` — cascata tra le rocce nella foresta
- [x] `loc_wizard_cove` — studio/laboratorio nascosto di un mago (non un'insenatura sul mare, nome fuorviante)
- [x] `loc_wizard_tower` — torre solitaria di un mago, rune arcane
- [x] `loc_temple` — Tempio del Sole di Kai, soli incisi, statue guardiane di leone e ariete (rifatta da Michele: la prima versione aveva iconografia cristiana esplicita, fuori tono)

## Nemici e bestie ostili (`enemy_*`/`beast_*`) — 14

- [ ] `enemy_bandits_city` — banditi, ambientazione urbana
- [ ] `enemy_bandits_forest` — banditi, ambientazione boschiva
- [ ] `enemy_bears` — orsi
- [ ] `enemy_doomwolf` — lupo mannaro/lupo maledetto
- [ ] `enemy_flying_beasts` — creature volanti ostili
- [ ] `enemy_giak` — Giak (canone Lupo Solitario)
- [ ] `enemy_helgast` — Helghast (canone Lupo Solitario)
- [ ] `enemy_toads` — rospi/creature d'acquitrino
- [ ] `beast_wolves` — branco di lupi (ululato)
- [ ] `beast_stallion` — cavallo/stallone
- [ ] `beast_cat` — felino
- [ ] `beast_anaconda` — serpente
- [ ] `beast_familiar` — famiglio/creatura magica
- [ ] `beast_rats` — ratti

## NPC e incontri pacifici (`npc_*`) — 11

- [ ] `npc_countess` — contessa
- [ ] `npc_fortune_teller` — indovina
- [ ] `npc_king` — re
- [ ] `npc_peasant_female` — contadina
- [ ] `npc_peasant_male` — contadino
- [ ] `npc_princess` — principessa
- [ ] `npc_royal_mage` — mago di corte
- [ ] `npc_traveler` — viandante
- [ ] `npc_valkyrie` — valchiria
- [ ] `npc_mage` — mago
- [ ] `npc_battlemage` — mago da battaglia

Le `beast_*` compaiono anche qui (stessa immagine, incontro pacifico
invece che ostile): **stesso file audio** già elencato sopra, non va
duplicato.

## Finali dell'avventura (`ending_*`) — 6

Non sono immagini del catalogo: agganciati a come finisce la storia
(`EndingOutcome`) e al genere dell'eroe (stessa distinzione già usata
per la voce TTS). Vanno in `app/src/main/assets/sfx/endings/` (non
`sfx/images/`), stessa regola del resto: file mancante = silenzio.

- [x] `ending_victory_male` — grido/voce di gioia, eroe maschio
- [x] `ending_victory_female` — grido/voce di gioia, eroe femmina
- [x] `ending_defeat_male` — grido di morte, eroe maschio
- [x] `ending_defeat_female` — grido di morte, eroe femmina
- [x] `ending_neutral_male` — finale né vittoria né sconfitta netta, eroe maschio
- [x] `ending_neutral_female` — finale né vittoria né sconfitta netta, eroe femmina

Michele: "puoi prevederli tutti se non ci sono non si suonano, così
posso magari mettere delle canzoni se non trovo nulla, l'importante è
che non vada in errore" — qualunque file, anche una canzone intera
buttata dentro come segnaposto, va bene: non c'è controllo di durata o
contenuto, solo il nome del file conta.

## Musica di sottofondo — 10

A differenza di tutto il resto in questo documento, NON sono legate a
un'immagine del catalogo: sono le tracce del catalogo musicale
(`app/.../util/BundledMusicCatalog.kt`), scelte a mano nelle Opzioni
(o in modalità **Casuale**, 24/07/2026: le alterna senza mai ripetere
quella appena suonata). Vanno in `app/src/main/assets/music/`, in
LOOP quando è selezionata una singola traccia. Categoria assegnata a
orecchio dal titolo dove non era ovvia (segnato sotto): da confermare
con Michele, correggibile cambiando solo il `displayName` nel catalogo.

- [x] `menu_Destino_segnato` — Main Theme (default), "Destino Segnato"
- [x] `esplorazione_Where_The_Statues_Kneel` — Esplorazione, "Where the Statues Kneel"
- [x] `combattimento_The_Iron_Vow` — Combattimento, "The Iron Vow"
- [x] `mercato_What_Is_the_Fee_` — Mercato, "What Is the Fee?"
- [x] `romantico_Breath_and_Bone` — Romantico, "Breath and Bone"
- [x] `Il_Voto_di_Ferro` — Combattimento (categoria non confermata), "Il Voto di Ferro"
- [x] `Il_cuore_e_la_spada` — Romantico (categoria non confermata), "Il Cuore e la Spada"
- [x] `Monete_per_un_fiore` — Mercato (categoria non confermata), "Monete per un Fiore"
- [x] `Tra_Market_e_Tower` — Mercato (categoria non confermata), "Tra Market e Tower"
- [x] `L_eterno_ritorno` — Esplorazione (categoria non confermata), "L'Eterno Ritorno"

## Casi particolari, forse da escludere

- [ ] `misc_battle_clash` — placeholder generico di scontro: un suono
  ci starebbe (es. riusare `combat_start`), ma non è un'immagine con
  un soggetto preciso.
- `hero_female` / `hero_male` — ritratto dell'EROE stesso (banner
  della scena): **sostituiti dai finali sopra**, che sono l'aggancio
  giusto (come finisce la storia, non che aspetto ha l'eroe). Restano
  fuori dalla checklist immagini.
