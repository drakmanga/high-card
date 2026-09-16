# Pre-analisi — Impostazione della collaborazione con l'AI

**Strumento:** Claude Code (CLI agent, modello Opus)
**Repository:** `high-card` — Spring Boot 3.5.0, Java 17, Lombok
**Data:** settembre 2026

---

## 1. Criterio di impostazione

Ho impostato la sessione partendo da un principio: **l'assistente non scrive codice finché non
ha dimostrato di aver capito il progetto**. È il vincolo di processo più importante che ho posto,
e ha condizionato tutto il resto del lavoro.

La ragione è specifica di questo esercizio. Su un repository sconosciuto, la modalità di errore
tipica di uno strumento generativo non è il codice che non compila — quello lo intercetta il
compilatore — ma il codice **plausibile e sbagliato**, prodotto per soddisfare la richiesta prima
di aver verificato che la richiesta sia formulata correttamente rispetto al codice reale. Un giro
di analisi speso prima vale diverse ore di revisione dopo.

### Contesto fornito

| Cosa                  | Scelta                                                 | Motivazione                                                                                                           |
|-----------------------|--------------------------------------------------------|-----------------------------------------------------------------------------------------------------------------------|
| Accesso al repository | Diretto, sul filesystem                                | L'assistente legge i sorgenti da sé: nessun rischio che un mio estratto parziale orienti l'analisi                    |
| Specifica             | Il `README.md` integrale                               | Fonte di verità unica                                                                                                 |
| Posta in gioco        | Dichiarata esplicitamente (prova tecnica di selezione) | Cambia il profilo di rischio accettabile: nessuna scorciatoia, nessuna libreria esotica, nessuna riscrittura creativa |
| Architettura          | **Non** spiegata da me                                 | Volutamente: usare la prima analisi come test di leggibilità del codice esistente                                     |
| Posizione dei bug     | **Non** indicata                                       | Il task 6 non dice quanti sono né dove: volevo misurare la capacità di individuazione autonoma                        |

### Cosa ho deliberatamente omesso

Non ho riassunto io i requisiti. Un riassunto introduce il *mio* eventuale fraintendimento in una
forma che l'assistente non può verificare, perché diventa a sua volta la specifica. Puntare alla
fonte originale mantiene verificabile ogni interpretazione.

Non ho proposto un'architettura obiettivo. Il README vieta di modificare i layer esistenti: volevo
accertarmi che il vincolo emergesse dalla lettura del codice, non da una mia istruzione — perché
un vincolo compreso viene rispettato, un vincolo imposto viene aggirato alla prima difficoltà.

---

## 2. Esito della prima analisi

L'assistente ha letto 26 sorgenti, il `pom.xml` e le properties, ed ha eseguito `./mvnw compile`
di propria iniziativa per stabilire una baseline (esito: compila, soli warning della JVM).

Risultati utilizzabili al primo passaggio:

- ricostruzione corretta del flusso a strati
  `Request → Assembler → Criteria → Service → Model → Assembler → DTO → Response`;
- individuazione dei due endpoint e del fatto che `getUsers` è uno stub (`return null`);
- **12 anomalie** elencate con file e riga.

Su questa base ho impostato la discussione sulle scelte di progetto, documentata in [plan.md](plan.md).

---

## 3. Limiti riscontrati nella comprensione iniziale

Questa sezione riporta i punti in cui lo strumento si è rivelato inaffidabile o insufficiente.
Li documento perché sono il vero oggetto di valutazione: sapere **dove** uno strumento sbaglia
è la condizione per poterlo usare.

### 3.1 Task 2: un requisito che non corrisponde al codice

Il README chiede di correggere una SQL Injection sull'endpoint `PUT`. **Nel progetto non esiste
SQL**: la persistenza è una `ArrayList` statica in `FakeDatabase`.

È il punto di massimo rischio dell'intero esercizio. Il pattern *"trova la SQL injection e
parametrizza la query"* è così ricorrente che la risposta statisticamente attesa da un modello
generativo è produrre l'artefatto atteso: inventare una query concatenata da correggere, oppure
introdurre un layer JDBC che nessuno ha chiesto — violando per giunta il divieto di modificare
l'architettura.

L'analisi ha retto: ha dichiarato l'assenza di SQL e ha riformulato il requisito.
**Ma ha retto perché il primo comando era analizzare, non risolvere.** Con un'istruzione diretta
del tipo "risolvi il task 2" il risultato atteso sarebbe stato codice convincente e sbagliato.
Considero questo il rischio meglio gestito della sessione, e insieme quello che è andato più
vicino a concretizzarsi.

### 3.2 Nessuna capacità di distinguere difetto intenzionale e rumore di scaffolding

Le 12 anomalie sono state individuate correttamente, ma lo strumento non ha modo di sapere quali
siano **seminate dall'esaminatore** e quali siano semplice sciatteria del codice di partenza.

Caso concreto: `PUT` è usato per la creazione, in contrasto con RFC 9110. Il README però fa
riferimento esplicito a *"the PUT endpoint"* per individuare l'endpoint del task 2.

La decisione — **documentare l'anomalia senza correggerla** — richiede di valutare se prevalga la
convenzione tecnica o la lettera della specifica, e quella valutazione dipende dal contesto: chi
legge, con quale aspettativa, e quale danno produce una specifica che non corrisponde più al
codice. Non è deducibile dal codice e non è delegabile. Il percorso completo, che ha incluso un
prototipo poi ritirato, è nella [Decisione 4](plan.md#decisione-4--verbi-http-anomalia-documentata-non-corretta).

### 3.3 Disallineamento di toolchain non segnalato

Il `pom.xml` dichiara `<java.version>17</java.version>`; la JDK locale è Corretto 26. Compila,
perché Lombok regge, ma il tema non è stato sollevato spontaneamente: è emerso solo quando ho
richiesto una build esplicita. Su un progetto reale è esattamente il disallineamento che non si
manifesta in locale e rompe la pipeline di integrazione.

### 3.4 Il vincolo architetturale è quello più fragile

*"Do not modify the existing layered architecture"* è un vincolo **negativo**: non produce alcun
artefatto verificabile, quindi nessun test può dimostrare che sia stato rispettato. La scorciatoia
naturale per qualsiasi assistente è passare `AddUserRequest` direttamente al servizio, risparmiando
un assembler — che è precisamente il comportamento sanzionato dalla domanda di approfondimento del
README. L'ho trattato come invariante da riverificare a ogni diff, non come acquisito.

### 3.5 Il metodo adottato non scala a una codebase reale

Le 26 classi entrano per intero nella finestra di contesto. Su un progetto di produzione, con
centinaia di classi, la lettura esaustiva è impraticabile: si passa a ricerca mirata, e compare
il rischio opposto — risposte espresse con sicurezza su porzioni di codice mai lette.
**Il risultato ottenuto qui non è estrapolabile** senza cambiare metodo.

---

## 4. Ambiguità aperte al termine dell'analisi

L'analisi si è chiusa con due questioni che non erano risolvibili leggendo il codice, perché
dipendono da come si interpreta la traccia. Le ho affrontate prima di autorizzare la scrittura di
codice; le decisioni e le rispettive motivazioni sono nel [piano di intervento](plan.md#2-decisioni-di-progetto).

1. **Livello di applicazione della validazione** — solo nel servizio, come dice la lettera del
   README, oppure anche Bean Validation sul layer web?
2. **Perimetro del requisito JWT** — sola validazione di token emessi altrove, oppure anche un
   punto di emissione, con il problema architetturale che ne consegue?

---

## 5. Regola di lavoro fissata per il seguito

I documenti di questa cartella sono stati redatti **durante** il lavoro, non ricostruiti alla fine.
Un resoconto scritto a posteriori descrive il risultato e non il processo, e il processo è ciò
che questa sezione dell'esercizio chiede di valutare.
