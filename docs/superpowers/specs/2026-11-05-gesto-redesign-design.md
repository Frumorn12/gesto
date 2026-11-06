# Gesto (ex Waypad): redesign e rinomina — design

Proposta visiva approvata da Andrea il 05/11/2026, in una pagina privata non inclusa nel repository
(prototipo cliccabile, tabelle prima/dopo, gesti, idee da Moonlight/Sunshine, icona, piano).
Questo documento ne riporta le decisioni in forma da costruire.

## Perché

Andrea trova Waypad "difficile da usare e molto brutta". Problemi verificati nel codice:
riparte sempre dall'onboarding, il codice di pairing va letto nel terminale del PC, dieci schermate
con la diagnostica nella barra principale, menu ad arco e striscia del fullscreen invisibili, ogni
trascinamento lento sullo schermo remoto diventa un drag (320 ms), il tasto indietro chiude l'app da
Pairing/Host fidati/QR, gli errori non si vedono in sessione e un comando fallito porta lo stato a
`Error` (barra in basso sparita, input scartato), testi tecnici in inglese hardcoded, icona azzurra e UI
verde lime scollegate.

## Obiettivi

- L'app si chiama **Gesto**: app Android, demone Linux e comando sul PC (`gesto`).
- All'apertura si ricollega da sola all'ultimo PC e riapre l'ultima modalità.
- Home "I tuoi PC" + quattro modalità in una barra in basso: Touchpad, Schermo, Tastiera (foglio), Comandi.
- Pairing con PIN di 4 cifre mostrato sul telefono e notifica Consenti/Rifiuta sul PC; codice e QR restano.
- Grafica Ecomic, struttura del tema e icona della stessa famiglia di Tratto.
- Interfaccia in italiano e inglese (segue la lingua del telefono).
- Idee da Moonlight/Sunshine elencate sotto.

## Non obiettivi (questo giro)

- Risoluzione di Hyprland adattata al telefono, collegamento fuori casa (signaling) nell'app,
  gamepad virtuale a schermo, F-Droid ufficiale.
- Nessuna modifica alla cifratura del canale.

## Anti-obiettivi (sarebbe un fallimento anche se funziona)

- Un gesto o un comando raggiungibile solo in un modo invisibile.
- Testi con gergo (portal, fingerprint, backend, JPEG Q, 1600p) nelle schermate principali.
- Una sessione che si blocca o sparisce dopo un singolo comando fallito.
- Perdere i telefoni già accoppiati quando il demone cambia nome.

## Decisioni

| Tema | Decisione | Di chi |
|---|---|---|
| Nome | Gesto. Repo `Frumorn12/gesto` (app), `Frumorn12/gesto-linux` (demone), `gesto-signal`; `waypad-suite` archiviato | Andrea |
| Icona | Cornice di Tratto (quadrato rx 26, gradiente #7A5CF6→#4E31D6) con una **g** corsiva bianca a tratto unico, stroke 6.5, estremi tondi. Path in viewBox 108: `M59,37 C53,30 40,31 37,42 C34,52 42,57 49,53 C55,49 58,41 59,33 C59,46 58,62 55,72 C52,82 41,84 39,78 C37,72 51,66 74,56` | Andrea |
| Lingua | Inglese (`values/`) + italiano (`values-it/`) | Andrea |
| Demone | Rinomina, pairing con notifica, app da avviare, MAC per Wake-on-LAN | Andrea |
| Identificativo | `applicationId` e package `it.frumorn.gesto` (come `it.frumorn.tratto`); si riaccoppia una volta | Claude, accettato |
| Angoli | 2 dp (preset "spigolo", come l'HUD di SmartExploration), opzione "arrotondati" | Claude |
| Tema | Segue il sistema; scelta Sistema/Chiaro/Scuro; schermo remoto sempre scuro | Claude |

## Sistema visivo

- Palette Ecomic (uguale a Tratto): Deep Viola `#6546F3` (azioni), Ossidiana `#271E52`, Ossidiana deep `#1F1747`,
  Viola medio `#493990`, Lavanda `#A491FA` (accento sul fondo scuro, 6,9:1), Off Black `#1E1E1E` (testo), bianco (fondo),
  grigi `#4D4D4D`, `#999999`, `#E0E0E0`, `#F7F7F7`, tint attivo `#EFEAFE`. Stato: successo `#167D5A`, errore `#B3261E`.
  Scuro: fondo `#1F1747`, superfici `#271E52`, testo `#F2F2F2`, accento `#A491FA`, testo su accento `#1F1747`.
- Font unico Manrope (regular/medium/semibold/bold, da `~/code/tratto/app/src/main/res/font/`): Regular per titoli e testi,
  Medium pulsanti, SemiBold risalti; tracking negativo sui titoli (da -2,5% a -1%); maiuscolo solo nelle etichette piccole.
- Forme: 2 dp, bordi 1 dp, niente ombre tranne sui pannelli flottanti (pulsante del menu, menu, fogli).
- Movimento: entrata 320 ms `CubicBezier(0.2, 0.7, 0.2, 1)` con traslazione 10 dp + dissolvenza; uscita 190 ms;
  menu da scaleY 0.85 in 320 ms; foglio tastiera da scaleY 0.7 in 350 ms; pulsazione di richiamo 1,8 s (prime 5 aperture);
  la g dell'avvio si scrive da sola (1,3 s). Rispetta "rimuovi animazioni" di sistema.

## Architettura dell'informazione

Schermate: Avvio → (Touchpad | Schermo | Comandi) con barra in basso; I tuoi PC; Aggiungi un PC (Cerca → PIN | Codice | Indirizzo | QR);
Impostazioni → Avanzate, Diagnostica. La tastiera è un foglio sopra la modalità corrente.

Tasto indietro: foglio aperto → lo chiude; menu rapido aperto → lo chiude; in sessione (Touchpad/Schermo/Comandi) → apre il menu rapido;
Aggiungi un PC → passo precedente, poi I tuoi PC; Impostazioni/Avanzate/Diagnostica → schermata precedente;
I tuoi PC e Avvio → esce dall'app. Mai uscire dall'app da uno stato intermedio.

Avvio: se c'è un PC fidato, connette quello con `lastConnectedAt` più recente (timeout 6 s) e apre l'ultima modalità salvata;
se fallisce → I tuoi PC con avviso "<PC> non risponde". Senza PC fidati → I tuoi PC vuota con "Aggiungi un PC".

## Interazioni

- Touchpad: 1 dito muove (velocità 0,5–3,0, predefinita 1,0; accelerazione attiva), tocco ≤250 ms entro 8 dp = clic sinistro,
  tocco a 2 dita = clic destro, 2 dita = scroll, tenere fermo ≥500 ms poi muovere = trascinamento, tocco a 3 dita = tastiera.
  Suggerimenti in basso per le prime 5 sessioni.
- Schermo: modalità "Touchpad" (predefinita, come sopra) o "Diretto" (tocco = clic dove tocchi; muovere senza tenere = sposta il cursore;
  tenere ≥400 ms poi muovere = trascinamento; secondo tocco entro 250 ms e 60 px non sposta il cursore → doppio clic stabile).
  Pizzico = zoom 1×–3× con pan a due dita; due dita parallele = scroll. Pulsante viola 46 dp trascinabile (posizione salvata) + menu rapido:
  Tastiera, Clic destro, Audio del PC, Tocco, Qualità, Modalità gioco, Torna al touchpad. In orizzontale va a tutto schermo e resta solo il pulsante.
- Modalità gioco: un solo interruttore (menu rapido); attiva tutto schermo + preset Fluida + inoltro controller; spegnendola torna la qualità di prima.
- Qualità: Automatica (scala 1920/60/q70 → 1600/60/q62 → 1280/60/q58 → 1280/30/q52, scende di un gradino se gli fps consegnati restano sotto
  il 75% del target per 3 finestre da 2 s; non risale nella stessa sessione), Fluida 1280/60/q58, Nitida 2400/30/q86, Risparmio 1280/30/q52;
  Avanzate: fps 30/45/60, lato lungo 1280/1600/1920/2560/3840, qualità 35–92 (→ preset "Personalizzata").
- Comandi: musica (precedente, play/pausa, successivo), volume (−, muto, +), luminosità (−, +), 8 scorciatoie (Copia, Incolla, Annulla, Salva,
  Chiudi, Terminale, Launcher, Esc), App sul PC (dal demone), Blocca, Sospendi tenendo premuto 1,2 s. Comandi non supportati: spenti con il motivo.
- Avvisi: snackbar in basso sopra la barra (testo + azione opzionale). Un comando fallito non cambia lo stato di connessione.
- Errori tradotti: porta chiusa/irraggiungibile, timeout, impronta cambiata, codice errato/scaduto, troppi tentativi, pairing pubblico bloccato,
  rifiutato sul PC, nessuna risposta dal PC, portal in attesa.
- Scorciatoie Android dinamiche per ogni PC fidato (`gesto://connect?host=<id>`).

## Protocollo nuovo (app ↔ demone)

- Discovery: l'app manda `GESTO_DISCOVER_V1`; il demone accetta anche `WAYPAD_DISCOVER_V1`. Risposta con `service: "gesto"`
  (l'app accetta anche `dev.waypad.daemon`) e campo nuovo `approval_pairing: true`.
- Messaggio sicuro nuovo `{"type":"approval_pair_request","request_id","device_name","pin","app_version"}` (pin = 4 cifre).
  Il demone mostra una notifica (Notify via D-Bus con azioni `allow`/`deny`, persistente) e risponde entro 120 s con gli stessi `data`
  di `pair_request`, oppure errore `pairing_rejected`, `pairing_timeout`, `approval_unavailable` (niente server notifiche con azioni),
  `pairing_busy` (un'altra richiesta in attesa), `rate_limited`, `public_pairing_denied`.
- Risposte di pair/approval/auth: `data.mac_addresses: [String]` (MAC "aa:bb:cc:dd:ee:ff" delle interfacce fisiche attive).
- Capacità: `capabilities.apps: [{id, name}]`; comando nuovo `launch_app {id}`.
- Inviti `gesto://invite?...`; l'app accetta ancora `waypad://invite?...`.

## Demone

Binario e pacchetto `gesto`; `gesto` senza argomenti stampa stato e telefoni collegati; `gesto pair [--qr …]` (ex `invite`/`pair-code`),
`gesto devices [list|revoke <id>]`, `gesto doctor`, `gesto serve` (systemd); alias nascosti per i vecchi sottocomandi.
Config `~/.config/gesto/config.json`, stato `~/.local/state/gesto/` con migrazione automatica da `waypad-daemon` (telefoni accoppiati validi).
Unit `gesto.service`. App da avviare in config: `apps: [{"id","name","command":[argv…]}]`, lanciate con `setsid -f uwsm app -- <argv>`.

## GitHub e rilascio

Come Tratto: README in inglese (banner, badge flat-square in palette, sezioni Why "Gesto", Features con vetrine, Install, Set up your PC,
Pairing and security, Building, Thanks to, License), fastlane `en-US`/`it-IT`, tag annotato `v0.2.0` "Gesto 0.2.0", APK firmato con chiave in
`~/.config/gesto/` (fuori dal repo), descrizione e topics. Repo rinominati con `gh repo rename`.

## Prove

Ogni fase: APK debug installato con adb sul Nothing Phone (3a) di Andrea (1080×2392, 420 dpi, Android 16), demone acceso su questo PC,
screenshot `adb exec-out screencap -p` in chiaro e scuro, test JVM verdi. Percorsi: prima apertura, pairing, chiusura/riapertura,
touchpad, schermo verticale e orizzontale, tastiera, comandi, indietro ovunque, demone spento a metà, PIN rifiutato.
