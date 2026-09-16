# Report finale — metodo, errori, valutazione

**Strumento:** Claude Code (Opus) — settembre 2026
**Risultato:** 8 task completati · 172 test verdi · 91,2% di copertura di linea
**Documenti collegati:** [pre-analysis.md](pre-analysis.md) · [plan.md](plan.md)

---

## 1. Direttive principali impartite

Le istruzioni che hanno determinato il risultato sono state poche e brevi, e quasi nessuna
conteneva indicazioni tecniche: contenevano **vincoli di processo**. Le riporto in sintesi,
con l'effetto prodotto.

### D1 — Analisi obbligatoria prima di qualsiasi scrittura
> Nessuna riga di codice prima di aver letto l'intero repository ed esposto architettura,
> flusso fra i layer e difetti individuati, task per task.

Nessun contenuto tecnico. Il valore sta in tre vincoli impliciti: la posta in gioco dichiarata
(prova tecnica di selezione), la fonte di verità indicata (il README, non un mio riassunto), e il
divieto esplicito di iniziare a produrre.

**È la direttiva che ha evitato l'errore più probabile dell'intera sessione** (§2.1).

### D2 — Chiusura delle ambiguità prima dell'autorizzazione a procedere
> Validazione su entrambi i livelli, web e servizio. Endpoint di emissione JWT incluso, ma
> documentato come componente transitorio, con indicazione esplicita di cosa lo sostituisce.

Due decisioni architetturali, con un'istruzione precisa sulla **forma della giustificazione**: non
"aggiungi un login", ma "aggiungilo e documenta perché andrà rimosso e con cosa". La differenza
è misurabile nel risultato — il Javadoc di `AuthServiceImpl` descrive la migrazione a Identity
Provider esterno, il passaggio da HS256 a verifica asimmetrica su JWKS e le classi da eliminare.
Senza quella precisazione sarebbe rimasto un endpoint di login privo di contesto: un errore di
architettura silenzioso in un progetto sottoposto a valutazione.

### D3 — La compilazione non è un criterio di completamento
> Ogni blocco funzionale si chiude con applicazione avviata e chiamate reali agli endpoint,
> con asserzione sul corpo della risposta.

È la direttiva a più alto rendimento dell'intera sessione. "Compila" è una soglia troppo bassa:
il codice compilato e sbagliato è precisamente ciò che uno strumento generativo produce meglio.
La verifica sul campo ha confermato con evidenza la policy di autorizzazione — ruolo `USER` in
lettura 200, in scrittura 403, token manomesso 401, tutti con HTTP 200 — invece di lasciarla
affidata a una dichiarazione dello strumento.

### D4 — I test si ancorano al difetto, non al comportamento
> Ogni bug corretto deve avere un test che fallirebbe sulla versione precedente, con il difetto
> richiamato in commento.

Senza questo vincolo si ottengono test che verificano il codice corretto ma perdono memoria di
cosa lo abbia reso necessario. Da qui i riferimenti espliciti in `AssemblerTest`
(`// Regressione: l'assembler originale assegnava getFirstName() al cognome`).

### D5 — Riverifica dei requisiti già chiusi dopo ogni blocco nuovo
È l'unica pratica che ha intercettato la rottura del task 4 causata dal task 5 (§2.7).

### D6 — Audit avversariale prima di dichiarare chiuso un requisito di sicurezza
> Non basta che i test di iniezione passino: si spara una batteria di payload reali contro
> l'endpoint in esecuzione, e si guarda cosa entra.

Applicata al task 2, ha rivelato due payload che superavano la validazione (§2.9). La differenza
rispetto ai test già presenti è che quei casi non erano stati scelti da chi aveva scritto la
regola — ed è esattamente il punto cieco di qualsiasi suite autogenerata.

---

## 2. Errori e difficoltà

Questa è la sezione che considero più utile alla valutazione. La riporto per intero, inclusi i
casi in cui l'errore è stato mio.

### 2.1 Il rischio maggiore è stato neutralizzato dal metodo, non dallo strumento

Il task 2 chiede di correggere una **SQL Injection**. Il progetto **non contiene SQL**: la
persistenza è una `ArrayList` statica in `FakeDatabase`.

Il pattern "trova la SQL injection e parametrizza la query" è talmente ricorrente che la risposta
statisticamente attesa è produrre l'artefatto atteso: inventare una query concatenata da
correggere, o introdurre un layer JDBC mai richiesto — violando per giunta il divieto di
modificare l'architettura.

L'analisi ha retto e ha riformulato correttamente il requisito. **Ma ha retto perché la prima
direttiva era analizzare, non risolvere.** Con un'istruzione diretta "risolvi il task 2" il
risultato atteso era codice plausibile e sbagliato, del tipo che supera una revisione distratta
proprio perché assomiglia alla soluzione che ci si aspetta di vedere.

### 2.2 Import inesistente — errore di compilazione

```java
import org.springframework.security.access.AccessDeniedHandler;     // non esiste
import org.springframework.security.web.access.AccessDeniedHandler; // corretto
```

```
[ERROR] SecurityStatusWriter.java:[9,43] cannot find symbol
[ERROR]   symbol:   class AccessDeniedHandler
[ERROR]   location: package org.springframework.security.access
```

Allucinazione classica: il package *suona* corretto, perché `AccessDeniedException` sta davvero
in `org.springframework.security.access` — l'handler no. Costo: un ciclo di compilazione.
Poco grave proprio perché il compilatore lo intercetta. **Sono gli errori che compilano a essere
pericolosi**, come il successivo.

### 2.3 Quattordici test che non venivano eseguiti

Le due classi di test di integrazione erano state nominate `UserControllerIT` e `AuthControllerIT`.
Maven Surefire raccoglie `*Test`, `Test*`, `*Tests`; il suffisso `*IT` appartiene a **Failsafe**,
che non era configurato nel progetto.

La suite riportava:

```
[INFO] Tests run: 84, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

**Nessuna segnalazione. `Skipped: 0`.** I 14 test di integrazione — quelli che verificano
l'invariante centrale del progetto, cioè HTTP 200 su ogni risposta, compresi gli esiti di
sicurezza — non erano mai stati eseguiti. Il report era verde, e un report verde induce a non
guardare oltre il totale.

Individuato controllando la **lista nominativa delle classi eseguite** invece del numero
complessivo. Dopo la rinomina in `*Test`: da 84 a 98 test, tutti verdi.

È il caso che riassume il rischio reale di questo modo di lavorare: **lo strumento ottimizza per
il segnale che gli si chiede di produrre.** "Fai passare i test" e "scrivi test che vengano
eseguiti" non sono la stessa istruzione, e solo la prima era stata data. Da qui la pratica di
verificare l'elenco delle classi e non il totale, adottata per il resto della sessione.

### 2.4 Complessità introdotta senza richiesta

In `LoginAssembler` era comparsa una classe interna:

```java
private static final class GenericResponseFactory {
    private GenericResponseFactory() { }
    private static StatusDTO success() {
        return LoginResponse.buildStatus(LoginResponse.SUCCESS_CODE, "Authentication successful.");
    }
}
```

Un livello di indirezione per incapsulare una singola chiamata a un metodo statico già esistente.
Rimossa, sostituita dalla chiamata diretta. Nessuno l'aveva richiesta: è rumore prodotto per
inerzia stilistica, del tipo che supera una revisione perché "sembra ordinato".

### 2.5 Incoerenza indotta da una correzione precedente

Introdotta la validazione, i dati di seed di `FakeDatabase` sono diventati **non conformi alle
regole appena scritte**:

- `"+39" + i` → `+390`, `+391`… non sono numerazioni italiane valide;
- `"First name " + i` → contiene una cifra, respinta dalla whitelist sui nomi.

**Nessun test falliva**, perché il seed scrive direttamente nella lista senza attraversare la
validazione. Il dataset di partenza contraddiceva però le regole del dominio, e ogni test di
ricerca sarebbe stato costruito su dati che l'applicazione stessa avrebbe rifiutato in ingresso.

Segnalato e corretto con nomi e numerazioni conformi. **Lo strumento non rivede spontaneamente le
conseguenze indirette di una modifica appena introdotta.**

### 2.6 Documentazione divergente dall'implementazione

Il Javadoc generato per `StringUtil.isNullOrEmpty` dichiarava che il metodo rifiuta le stringhe di
soli spazi. L'implementazione era `str == null || str.isEmpty()`, che **non** li rifiuta.

Divergenza sottile e insidiosa: un commento che mente è peggio di un commento assente, perché il
lettore successivo vi si affida senza verificare. Risolta allineando l'implementazione a
`isBlank()`, che è il comportamento corretto per un controllo di obbligatorietà — un campo
valorizzato con `"   "` non è realmente valorizzato.

### 2.7 Dipendenza fra requisiti non rilevata

Descritta per esteso in [plan.md §5](plan.md#5-la-dipendenza-fra-requisiti-che-il-piano-non-aveva-previsto).
In sintesi: completato il task 5, l'invariante del task 4 era rotta, perché Spring Security
respinge con 401/403 prima di `@RestControllerAdvice`. Nessun test falliva, poiché i test del
task 4 erano stati scritti quando la sicurezza non esisteva.

Emersa applicando D5. Risolta con `SecurityStatusWriter`. **Lo strumento è solido su un requisito
alla volta e cieco sull'interazione fra requisiti già completati.**

### 2.8 Una decisione presa due volte, e due difetti emersi per caso

Sull'uso di `PUT` per la creazione l'assistente aveva raccomandato di **documentare senza
correggere**, per non allontanarsi dalla lettera del README, che al task 2 fa riferimento a
*"the PUT endpoint"*.

**Ho respinto la raccomandazione e fatto correggere.** La violazione di RFC 9110 è oggettiva —
`PUT` richiede idempotenza e un URI già noto al client, mentre qui il `guid` è generato dal
server — e il task 6 chiede di correggere i difetti del codice di partenza. La modifica è stata
implementata, testata e verificata sul campo.

**Poi l'ho ritirata.** Rileggendo la specifica ho riconsiderato il peso di quel riferimento: non
è incidentale, è il modo in cui la traccia individua l'endpoint del task 2. Cambiando il verbo,
chi valuta si trova una specifica e un'implementazione che non si parlano più. L'aderenza alla
specifica prevale, e l'anomalia va segnalata invece che corretta — con l'analisi tecnica completa
nel Javadoc di `UserController`.

Registro l'oscillazione perché è il punto in cui il giudizio ha pesato di più, e perché la
raccomandazione iniziale dell'assistente si è rivelata **giusta per la ragione sbagliata**:
suggeriva di non intervenire per prudenza generica, non perché avesse valutato il ruolo di quel
riferimento nella traccia.

**La parte tecnicamente interessante è ciò che il prototipo ha rivelato.** Cambiati i verbi, la
suite riportava 98 test verdi e `BUILD SUCCESS`. La verifica sul campo ha invece trovato due
difetti **preesistenti e latenti**, che nessun test copriva:

1. **Un verbo non mappato produceva `500 Generic error`.** L'eccezione
   `HttpRequestMethodNotSupportedException` ricadeva nella rete di sicurezza del gestore, e al
   client veniva segnalato un errore interno del server invece di una richiesta formulata male.
   Corretto con un handler dedicato che restituisce un `405` applicativo.

2. **Gli errori di conversione dei parametri esponevano la struttura interna dei package:**

   ```
   Failed to convert property value of type 'java.lang.String' to required type
   'it.sara.demo.service.user.criteria.CriteriaGetUsers$OrderTypè for property 'order'
   ```

   Il messaggio predefinito di Spring, restituito tale e quale, rivelava nomi di classi e
   gerarchia dei package. Corretto sanitizzando gli errori di binding, con un test che asserisce
   l'assenza della stringa `it.sara.demo` nel corpo della risposta.

**Entrambe le correzioni sono state mantenute** anche dopo il ritiro della modifica sui verbi:
erano difetti reali, non conseguenze del prototipo. È la stessa lezione del §2.3 in forma diversa,
con in più l'osservazione che **una modifica poi scartata può ripagarsi comunque**, se durante il
percorso si guarda il sistema davvero in funzione.

### 2.9 La validazione contro l'injection aveva due buchi, e i test non li vedevano

Completato il task 2, la posizione era: validazione whitelist applicata, test di iniezione
presenti, requisito chiuso. Il test esisteva davvero e passava:

```java
@ValueSource(strings = {"Robert'); DROP TABLE users;--", "' OR '1'='1", ...})
void shouldRejectInjectionAttemptsInNames(String name) { ... }
```

Prima di dichiarare chiuso il requisito ho preteso un **audit esplicito**: una batteria di payload
reali sparata contro l'endpoint con l'applicazione in esecuzione, invece di fidarmi dei casi che
erano stati scelti per il test. Due payload sono passati:

| Payload                | Campo       | perché passava                                                            |
|------------------------|-------------|---------------------------------------------------------------------------|
| `admin'--`             | `firstName` | apostrofo e trattino ammessi singolarmente, per `D'Angelo` e `Anna-Maria` |
| `admin'--@example.com` | `email`     | l'apostrofo è legale nella parte locale per RFC 5322                      |

La regola originale vincolava **l'alfabeto** (`^\p{L}[\p{L} '\-]{0,49}$`) ma non la
**struttura**: ammetteva i separatori in qualunque posizione e quantità'. I casi di test coprivano
i payload appariscenti — `DROP TABLE`, `UNION SELECT` — e non quelli che usano soltanto caratteri
consentiti.

**È il limite strutturale dello strumento visto una terza volta:** i test generati coprono i casi
a cui si è pensato, e chi li genera pensa agli stessi casi di chi ha scritto la regola. La
copertura era alta e la regola era sbagliata. Nessun numero lo segnalava.

**Correzione: vincolare la struttura, non ampliare la blacklist.**

```java
// prima: alfabeto ammesso, nessun vincolo di posizione
"^\p{L}[\p{L} '\-]{0,49}$"

// dopo: il separatore deve stare FRA due lettere
"^\p{L}+(?:[ '\-]\p{L}+)*$"
```

Il compromesso è il punto interessante: `D'Angelo`, `Dell'Orto`, `Anna-Maria` sono cognomi
italiani legittimi e **devono** passare, quindi vietare l'apostrofo era escluso — sarebbe stato
al tempo stesso inefficace, perché esistono payload che non lo usano, e dannoso per gli utenti
reali. La regola strutturale respinge `admin'--` senza vietare alcun carattere.

A verifica è stata aggiunta `SqlInjectionTest`: 44 payload su quattro campi, ognuno verificato
sia sul codice applicativo 400 sia sul fatto che la persistenza non sia cresciuta, più sei casi
in positivo sui nomi legittimi.

**Nota di onestà tecnica.** Nessuna di queste misure rende l'endpoint immune alla SQL Injection,
e nella documentazione è scritto esplicitamente. **L'immunità' deriva dalla query parametrizzata;**
la validazione riduce la superficie di attacco. Dichiarare "protezione totale tramite validazione"
sarebbe falso, e cade alla prima domanda su come si gestisce `D'Angelo`.

### 2.10 Efficacia contro efficienza: una scelta consapevole e un difetto vero

Una verifica esplicita sul criterio di scrittura — codice **efficace** (corretto e leggibile) o
**efficiente** (parsimonioso di risorse)? — ha distinto due situazioni che sembravano la stessa.

**Scelta consapevole, mantenuta.** `getUsers` filtra e ordina l'intera collezione prima di tagliare
la pagina: su un milione di righe sarebbe inaccettabile, ma la sorgente è una lista in memoria e,
soprattutto, quel codice ricalca la clausola `WHERE ... ORDER BY ... LIMIT ... OFFSET` in cui va
tradotto. L'efficienza qui appartiene alla query, non al servizio, e forzarla nel posto sbagliato
renderebbe il codice peggiore senza renderlo più veloce. La scelta è ora dichiarata nel Javadoc
del metodo.

**Difetto vero, corretto.** Il login eseguiva **due operazioni Bcrypt** per tentativo:

```java
String encodedPassword = passwordEncoder.encode(account.password());              // ~100 ms
boolean passwordMatches = passwordEncoder.matches(criteria.getPassword(), encodedPassword);
```

Il doppio costo era il sintomo; la causa era peggiore: `ACCOUNTS` conteneva le password **in
chiaro**, cifrate al volo a ogni accesso. **È l'inverso di come funziona un archivio credenziali**,
dove la password in chiaro non esiste in nessun punto del sistema.

L'intenzione — tempo di risposta costante per non rivelare quali username siano censiti — era
corretta, l'esecuzione no. Corretto memorizzando gli hash Bcrypt precalcolati e confrontando con
una sola invocazione, con un hash fittizio costante per il ramo dello username inesistente.

Non è un difetto che un profiler avrebbe segnalato, né un test: passavano tutti. **È emerso**
ponendo al codice una domanda che nessuna metrica pone — *questo è scritto bene o solo scritto?*

**Estensione dell'audit: un bug di concorrenza.** La stessa domanda posta al resto del codice ha
trovato un difetto più serio. `FakeDatabase.TABLE_USER` era un `ArrayList` **condiviso fra i
thread di richiesta**: un'applicazione web serve ogni chiamata su un thread diverso, e una ricerca
che attraversa la collezione mentre una creazione vi scrive solleva
`ConcurrentModificationException` — cioè un 500 al chiamante.

Verificato prima di dichiararlo, con un test che esegue in parallelo 500 scritture e 500
attraversamenti completi:

```
ConcurrencyTest.shouldSurviveConcurrentReadAndWrite
  accesso concorrente fallito: java.util.ConcurrentModificationException
```

Corretto con `CopyOnWriteArrayList`, che itera su uno snapshot e non fallisce mai. È la struttura
adatta a questo profilo — letture frequenti, scritture rare — perché il costo della copia si paga
solo in scrittura. Il test resta come regressione.

Nessuno dei 158 test esistenti lo copriva: **sono tutti a thread singolo**, come lo è per
default qualunque suite generata. La concorrenza non si manifesta finché non la si provoca.

**Una duplicazione silenziosa.** Il limite massimo di pagina era scritto due volte, come letterale
`100` nell'annotazione `@Max` del layer web e come costante `MAX_LIMIT` nel servizio: due
definizioni della stessa regola, destinate a divergere alla prima modifica. Unificate in
`CriteriaGetUsers.MAX_LIMIT`, riusata da entrambi.

**Un'ottimizzazione valutata e respinta.** La chiave di firma JWT viene ricostruita a ogni
richiesta (decodifica Base64 e costruzione della `SecretKey`). Renderla cache avrebbe richiesto
double-checked locking con campo `volatile` per risparmiare microsecondi su un percorso che paga
già 100 ms di Bcrypt a monte: complessità concorrente in cambio di nulla di misurabile.
**Lasciata invariata.** Registrarla qui serve a mostrare che la decisione è stata presa, non
mancata: il criterio "efficacia prima di efficienza" vale anche quando indica di non intervenire.

### 2.11 Attriti minori
- `mvn -o` (modalità offline) fallisce su `spring-boot:run`: il plugin non era in cache locale.
  Trenta secondi, nessuna conseguenza.
- Un `pkill -f "spring-boot:run"` ha terminato anche la shell che lo stava eseguendo, interrompendo
  a metà il comando successivo. Rilevato perché il file atteso non era stato creato. Banale, ma
  è il genere di effetto collaterale che in uno script non presidiato passa inosservato.

---

### 2.12 La review finale sull'applicazione in esecuzione ha trovato sei difetti che la suite non vedeva

A lavoro concluso — 159 test verdi, 89,0% di copertura di linea — ho fatto eseguire una **review
avversariale sull'applicazione avviata**, con richieste reali invece che sulla sola lettura del
codice. Ha prodotto sette rilievi, sei dei quali difetti veri.

Tre riguardavano l'invariante centrale del progetto, quella su cui il task 4 è costruito:

1. **Rotta inesistente e `Content-Type` sbagliato rispondevano `500 Generic error`.** Il gestore
   catch-all intercettava anche eccezioni che portano già il proprio stato e ne scartava il
   codice. Un errore del chiamante veniva dichiarato guasto del server: chi consuma l'API non
   distingue più la richiesta sbagliata dal servizio rotto.
2. **La regola di fallback `anyRequest().authenticated()` era troppo permissiva.** I matcher sono
   per path esatto, quindi `PUT /user/v1/user/` e `PUT /USER/v1/user` non incontravano la regola
   `hasRole(ADMIN)` e scivolavano sulla regola finale, che un token di sola lettura soddisfa.
   Non sfruttabile — quelle varianti non hanno un handler mappato — ma il controllo di ruolo era
   già stato superato: a fermare la richiesta era l'assenza di una rotta, non la policy.
3. **Il `guid` dell'utente creato non tornava al client.** Il difetto 10 era stato corretto nel
   servizio, ma il controller ignorava il valore di ritorno: `AddUserResult.guid` e l'intera
   classe `AddUserResponse` erano codice morto, e la creazione rispondeva senza identificativo.

Gli altri tre erano minori ma concreti: il filtro JWT, essendo un bean `Filter`, veniva registrato
anche dal servlet container e girava fuori dalla catena di sicurezza; il log degli errori di
validazione riportava i valori rifiutati, quindi email, telefono e nome, a livello `WARN`; e un
valore di enum inesistente veniva segnalato come corpo malformato, pur essendo il JSON valido.

**Il punto non è la gravità dei singoli rilievi, è come sono stati trovati.** Nessuno dei sei
era visibile leggendo il codice o guardando la suite: servivano richieste vere, comprese quelle
che nessuno pensa di scrivere in un test — un path con lo slash finale, un `Content-Type` assente,
una rotta che non esiste. **È la quarta volta nel progetto che la stessa lezione si ripresenta**
(§2.3, §2.8, §2.9): **la suite verde certifica ciò che qualcuno ha pensato di verificare.** La
differenza rispetto alle volte precedenti è che qui la verifica è stata cercata apposta, a
lavoro già dichiarato concluso, invece di emergere per caso da un'altra modifica.

Correzione di rotta durante l'intervento: la prima proposta per il rilievo 1 aggiungeva un handler
`404` per le rotte inesistenti. Con la policy chiusa in `denyAll()` — la correzione del rilievo 2 —
quell'handler diventa irraggiungibile, perché la sicurezza nega il percorso prima del
`DispatcherServlet`. **È stato rimosso invece che lasciato**: due rilievi corretti insieme possono
rendere superflua una delle due correzioni, e il codice morto è esattamente ciò che il rilievo 3
contestava. Un percorso inesistente risponde quindi con una negazione e non con un `404`, il che
impedisce anche di enumerare le rotte.

**Seconda passata, con l'applicazione in esecuzione e credenziali reali.** Ripetuta la verifica
sull'applicazione corretta — login `admin` e `user`, token contraffatti, payload di iniezione,
80 richieste concorrenti — sono emersi altri otto difetti. Due contano più degli altri:

- **Log injection sullo username.** Lo username arrivava al log senza filtro e senza vincolo di
  alfabeto: un ritorno a capo nel campo inseriva righe arbitrarie nel log applicativo, verificate
  dal vivo. Chi legge il log, o l'aggregatore che lo indicizza, riceve eventi che nessun
  componente ha prodotto — e il log è la fonte usata per ricostruire un incidente. Corretto su
  due livelli: whitelist di caratteri sulla richiesta e neutralizzazione dei caratteri di
  controllo prima della scrittura.
- **Token firmato ma privo di `sub`.** Superava la validazione e autenticava una richiesta priva
  di identità: l'azione risultava eseguita, ma non attribuibile a nessuno. Ora il soggetto è
  obbligatorio.

Gli altri sei sono codice e documentazione che avevano smesso di corrispondere al sistema:
l'utenza di default generata da Spring Boot a ogni avvio e stampata nei log, un `trim()`
irraggiungibile perché la validazione rifiuta già gli spazi di bordo, `getByGuid` e
`GenericResponse.success(...)` senza chiamanti, un messaggio di errore che cablava `100` invece
del vincolo `MAX_LIMIT`, e un Javadoc che descriveva `total` al contrario di quello che vale.

**Terza verifica, provando l'applicazione a mano.** Il filtro di ricerca confrontava l'intera
stringa con un campo per volta: cercare `Mario Rossi` non trovava nessuno, perché nessun campo
contiene nome e cognome insieme — mentre `Mario` e `Rossi`, separatamente, trovavano entrambi
l'utente. Nessun test lo mostrava, perché tutti interrogavano con una parola sola: la suite
verificava la regola implementata, non l'uso che ne fa una persona. Il filtro è ora valutato per
parole, ognuna delle quali deve trovare riscontro in almeno uno dei tre campi.

Il settimo rilievo della prima passata — l'assenza di un vincolo di Unicità sull'email — è stato
**dichiarato e non colmato**: si veda [plan.md, Decisione 5](plan.md#decisione-5--unicità-dellemail-limite-dichiarato-non-colmato).

## 3. Approcci adottati

**Gate di comprensione prima dell'esecuzione (D1).** Un giro di conversazione speso a far esporre
l'analisi invece del codice. Ha evitato la riscrittura architetturale inventata sul task 2.

**Decisioni di progetto trattenute, esecuzione delegata.** Le cinque decisioni documentate in
[plan.md §2](plan.md#2-decisioni-di-progetto) dipendono dal contesto — la traccia, l'intenzione di
chi valuta, il profilo di rischio — ed è esattamente ciò di cui lo strumento non dispone.

**Verifica eseguibile come criterio di completamento (D3).** Mai la compilazione come soglia.

**Test ancorati al difetto (D4).** Ogni correzione lascia traccia del motivo per cui è stata
necessaria.

**Rilettura dei requisiti chiusi dopo ogni blocco nuovo (D5).** Unica pratica che intercetta le
regressioni fra task.

**Verifica nominativa dei test eseguiti.** Adottata dopo il caso §2.3: si legge l'elenco delle
classi, non il numero verde.

**Audit avversariale sui requisiti di sicurezza (D6).** Payload reali contro l'endpoint in
esecuzione prima di considerare chiuso il task 2. Ha trovato due buchi che la suite non vedeva.

---

## 4. Valutazione

### Dove ha accelerato in modo sostanziale
- **Stesura meccanica**: Javadoc su oltre 40 classi, assembler, boilerplate. Valore intellettuale
  nullo, costo in ore reale.
- **Enumerazione sistematica dei casi di test**: 57 asserzioni parametrizzate su email,
  numerazioni italiane e nomi, con i casi limite corretti (`+3933012345678` troppo lungo,
  `mario@example.c` con TLD di un solo carattere). Qui la macchina è semplicemente superiore:
  non si annoia e non salta il caso noioso.
- **Ricognizione iniziale**: 12 anomalie con file e riga su 26 classi mai viste, in un solo
  passaggio. A mano sarebbe stata mezza giornata di lettura.
- **Configurazione a partire da requisiti già decisi**: `SecurityFilterChain`, filtro JWT,
  `@RestControllerAdvice`. Codice noto, che va però ricordato con esattezza.

Stima onesta: **le ore di stesura si riducono in misura rilevante; le ore di decisione e di
verifica non cambiano, e quelle di revisione aumentano.** Il guadagno netto resta ampio, ma si
sposta il tipo di lavoro richiesto — meno scrittura, più controllo.

### Dove è risultato insufficiente
- **Non distingue un difetto intenzionale dal rumore di scaffolding, e in caso di dubbio sceglie
  di non intervenire per prudenza generica.** Sull'uso di `PUT` per la creazione la
  raccomandazione di non intervenire si è rivelata corretta, ma per la ragione sbagliata: non
  aveva valutato il ruolo che il riferimento *"the PUT endpoint"* svolge nella traccia (§2.8).
- **Non vede le interazioni fra requisiti già chiusi** (§2.7). Ogni task è trattato come isolato,
  e le regressioni fra task non producono alcun segnale.
- **Ottimizza per il segnale richiesto, non per l'obiettivo** (§2.3). "98 test verdi" era un numero
  vero e una misura falsa finché 14 test non venivano eseguiti. Lo stesso meccanismo in §2.9: i
  test di iniezione passavano tutti mentre la regola lasciava passare `admin'--`, perché i casi
  di test erano stati scelti da chi aveva scritto la regola.
- **Non rivede le conseguenze indirette delle proprie modifiche** (§2.5).
- **Introduce indirezione superflua** quando non è vincolato (§2.4).
- **Documenta ciò che intendeva scrivere, non ciò che ha scritto** (§2.6).

### Il limite di metodo più rilevante
Questo repository ha **26 classi** ed entra per intero nella finestra di contesto. Il risultato
**non è estrapolabile** a una codebase di produzione: su un sistema di centinaia di classi la
lettura esaustiva è impraticabile, si passa a ricerca mirata, e compare il rischio opposto —
risposte espresse con sicurezza su porzioni di codice mai lette. Le pratiche che qui si sono
rivelate utili (gate di comprensione, verifica eseguibile, rilettura dei requisiti chiusi)
diventerebbero in quel contesto **necessarie**, non facoltative.

### Sintesi
Acceleratore netto sulla stesura e sull'enumerazione sistematica; **contributo nullo** sulle
decisioni che dipendono dal contesto; e un rischio specifico, che risiede interamente nella
qualità del segnale che lo strumento produce — un report verde è un'affermazione dello
strumento, non una prova. Il valore di chi lo governa non sta nel saper formulare la richiesta,
ma nel sapere **cosa non dare per acquisito**.

---

## Appendice — Come verificare il risultato

```bash
# suite completa e report di copertura in target/site/jacoco/index.html
./mvnw test

# avvio
./mvnw spring-boot:run

# 1. autenticazione (utenze dimostrative: admin/admin123, user/user123)
TOKEN=$(curl -s -X POST http://localhost:8080/auth/v1/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"admin","password":"admin123"}' | jq -r .accessToken)

# 2. ricerca paginata, ordinata, filtrata
curl -s -X POST http://localhost:8080/user/v1/user \
  -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"query":"ross","limit":5,"offset":0,"order":"BY_LASTNAME"}'

# 3. creazione con validazione della numerazione italiana
curl -s -X PUT http://localhost:8080/user/v1/user \
  -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"firstName":"Anna","lastName":"De Luca","email":"anna.deluca@example.com","phoneNumber":"+39 330 123 4599"}'

# 4. la policy nega la scrittura al ruolo di sola lettura -> HTTP 200, status.code 403
USER_TOKEN=$(curl -s -X POST http://localhost:8080/auth/v1/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"user","password":"user123"}' | jq -r .accessToken)
curl -s -X PUT http://localhost:8080/user/v1/user \
  -H "Authorization: Bearer $USER_TOKEN" -H 'Content-Type: application/json' \
  -d '{"firstName":"Test","lastName":"Test","email":"t@t.it","phoneNumber":"+393301234500"}'

# 5. verbo non mappato sulla risorsa -> HTTP 200, status.code 405
curl -s -X GET http://localhost:8080/user/v1/user -H "Authorization: Bearer $TOKEN"
```
