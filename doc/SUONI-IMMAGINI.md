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

## Luoghi (`loc_*`) — 24

- [ ] `loc_black_gate` — portale di pietra con statue di teschi cornuti, in un bosco
- [ ] `loc_caves` — ingresso di grotta tra le montagne
- [ ] `loc_crypt` — ingresso di cripta, rune e teschi sull'arco
- [ ] `loc_cursed_castle` — cancello di castello gotico, gargoyle, uno scheletro alla soglia
- [ ] `loc_forest` — sentiero nel bosco verso le montagne
- [ ] `loc_forest_prey` — bosco fitto con cervi selvatici
- [ ] `loc_graveyard` — cimitero di notte, luna calante
- [ ] `loc_harbor` — porto costiero, castello sull'acqua
- [ ] `loc_helgedad` — skyline di città nera di notte
- [ ] `loc_helgedad_gate` — arco d'ingresso con guardiani cornuti
- [ ] `loc_infernal_city` — città infernale sotto un cielo di tempesta
- [ ] `loc_kai_monastery` — monastero in cima alla montagna
- [ ] `loc_market` — mercato cittadino affollato
- [ ] `loc_monastery_dawn` — alba sulle montagne, castello all'orizzonte
- [ ] `loc_mountain` — vetta rocciosa tra le nuvole
- [ ] `loc_mountain_pass` — passo di montagna, cavaliere e soldati
- [ ] `loc_smithy_exterior` — esterno di una fucina, di notte
- [ ] `loc_smithy_interior` — interno di fucina, fabbro alla forgia
- [ ] `loc_standing_stones` — cerchio di pietre erette, notte di luna
- [ ] `loc_storm_tower` — torre di pietra con rune, tempesta di fulmini
- [ ] `loc_tavern` — interno di taverna affollata
- [ ] `loc_tomb_exterior` — ingresso di tomba, catene e statue demoniache
- [ ] `loc_tomb_interior` — interno di tomba, sarcofago
- [ ] `loc_warehouse` — interno di magazzino, casse e pergamene

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

## Casi particolari, forse da escludere

- [ ] `misc_battle_clash` — placeholder generico di scontro: un suono
  ci starebbe (es. riusare `combat_start`), ma non è un'immagine con
  un soggetto preciso.
- `hero_female` / `hero_male` — ritratto dell'EROE stesso (banner
  della scena): **probabilmente nessun suono ha senso qui**, sono il
  personaggio del giocatore, non un luogo o una creatura. Lasciati
  fuori dalla checklist sopra apposta — dimmi se invece li vuoi.
