# Piano di intervento — costruzione, decisioni, iterazioni

**Strumento:** Claude Code (Opus)
**Riferimento:** [pre-analysis.md](pre-analysis.md) per l'analisi preliminare

---

## 1. Il piano non segue l'ordine del README

Il README elenca otto task numerati. Eseguirli in quell'ordine sarebbe stato un errore: fra loro
esistono dipendenze tecniche che l'elenco non dichiara. Ho richiesto che venissero ricostruite
**prima** di scrivere codice, e l'esito ha riorganizzato l'intero piano di lavoro.

| Dipendenza                                                                                              | Conseguenza sull'ordine                                                                                                      |
|---------------------------------------------------------------------------------------------------------|------------------------------------------------------------------------------------------------------------------------------|
| `GenericPagedResult.total` è `private` senza accessori                                                  | La paginazione (task 3) è **tecnicamente impossibile** finchè il difetto non è corretto → bug fix in testa                   |
| `catch (Exception e)` in `UserServiceImpl` degrada ogni `GenericException` 400 a un 500 generico        | La validazione (task 1) sarebbe stata **invisibile dall'esterno**: i messaggi non raggiungevano il client → bug fix in testa |
| I test (task 7) asseriscono sul formato delle risposte di errore                                        | L'exception handling (task 4) deve precederli, altrimenti vanno riscritti                                                    |
| Il JWT (task 5) introduce 401/403 emessi da Spring Security, **fuori** dalla catena `@ExceptionHandler` | Il vincolo "tutte le risposte HTTP 200" (task 4) va riaperto **dopo** il task 5                                              |

L'ultima riga è la più significativa: è una dipendenza **all'indietro**, che nessuna delle due
parti aveva previsto all'inizio. La tratto al §5.

### Sequenza eseguita

1. Bug fix bloccanti (task 6)
2. Validazione (task 1) e prevenzione injection (task 2)
3. Exception handling centralizzato (task 4)
4. Ricerca paginata, ordinata, filtrata (task 3)
5. Sicurezza JWT (task 5) — con riapertura del task 4
6. Test (task 7) e Javadoc (task 8), trasversali

---

## 2. Decisioni di progetto

Cinque decisioni hanno determinato la forma del risultato. Le prime due chiudono le ambiguità
emerse dall'analisi preliminare — non risolvibili dal codice, entrambe con impatto diretto sul
risultato; le altre tre riguardano punti in cui la traccia e il codice di partenza non
coincidevano. Le ho discusse valutando le alternative proposte e ho chiuso su queste posizioni.

### Decisione 1 — Validazione su due livelli

**Questione.** Il README colloca la validazione *"in the user creation service"*. Bastava il
servizio, o serviva anche Bean Validation sulle `Request`?

**Decisione: entrambi i livelli.** Bean Validation su `AddUserRequest` come primo filtro sul bordo
HTTP; validazione in `UserServiceImpl` come **autorità**.

**Motivazione.** Se i vincoli vivono solo nelle annotazioni delle `Request`, il servizio è sicuro
soltanto quando lo invoca il controller: qualsiasi altro chiamante — uno scheduler, un consumer di
code, un altro servizio — lo raggiunge senza alcun controllo. La validazione sul bordo è
un'ottimizzazione (scarta subito il traffico malformato), non una garanzia.

**Conseguenza operativa.** Le regex sono definite una sola volta come costanti pubbliche in
`ValidationUtil` e riusate dalle annotazioni del layer web, per impedire che le due definizioni
divergano nel tempo.

### Decisione 2 — Endpoint di emissione JWT, esplicitamente transitorio

**Questione.** Il requisito parla di *validazione* del token. Senza un punto di emissione, però,
non è dimostrabile né testabile end-to-end. Includere un login significa però introdurre un
problema architetturale reale.

**Decisione: includerlo, ma documentarlo come componente transitorio**, con indicazione esplicita
di cosa lo sostituisce in esercizio. Ho richiesto che la giustificazione non finisse in un file di
relazione ma **nel codice**, dove la legge chi dovrà manutenerlo.

**Motivazione.** Un servizio di dominio non deve essere anche authorization server: le due
responsabilità hanno ciclo di vita, superficie di attacco e requisiti di audit diversi. Un
endpoint di login non commentato, in un progetto valutato, si legge come un errore di architettura;
lo stesso endpoint accompagnato dal percorso di dismissione si legge come una scelta consapevole.

**Esito.** Il blocco Javadoc su `AuthServiceImpl` descrive la migrazione a Identity Provider esterno
(Keycloak, Auth0, Entra ID) con l'applicazione riconfigurata come puro OAuth2 Resource Server,
il passaggio da firma simmetrica HS256 a verifica asimmetrica RS256/ES256 su JWKS, e l'elenco delle
classi da rimuovere. Con la precisazione che conta: **la logica di validazione già scritta —
issuer, audience, scadenza, policy sui ruoli — resta invariata**, perché cambia solo chi emette
il token.

### Decisione 3 — Riformulazione del task 2

**Questione.** Il task chiede di correggere una SQL Injection. Non esiste SQL nel progetto.

**Decisione: riformulare, non simulare.** Nessuna query inventata, nessun layer JDBC introdotto.
La vulnerabilità reale dell'endpoint `PUT` è che input non fidato raggiungeva la persistenza
senza controllo di forma: la correzione è la validazione whitelist del task 1.

**Motivazione.** Inventare una query vulnerabile per poi correggerla sarebbe stato un artefatto
costruito per assomigliare alla soluzione attesa. Ho richiesto in aggiunta che il principio
generale — con un database reale la difesa primaria è la query parametrizzata, la validazione è
difesa aggiuntiva e mai sostitutiva — fosse scritto **in `ValidationUtil`** e non solo in un
documento, insieme alla ragione per cui non si usano blacklist di caratteri pericolosi
(aggirabili tramite codifiche alternative).

### Decisione 4 — Verbi HTTP: anomalia documentata, non corretta

**Questione.** Il codice di partenza espone la creazione su `PUT` e la ricerca su `POST`.
Entrambi sono in contrasto con RFC 9110. Correggerli o segnalarli?

**Decisione finale: mantenere i verbi, documentare l'anomalia** — nel Javadoc di
`UserController`, dove la legge chi manutiene il codice, e non solo in un documento allegato.
È l'unico punto del lavoro in cui una decisione è stata presa, ribaltata e infine ripristinata.

**Motivazione.** In una prova di selezione l'aderenza alla specifica prevale sulla convenzione:
il riferimento a *"the PUT endpoint"* non è incidentale, è il modo in cui la traccia individua
l'endpoint del task 2, e cambiando il verbo quella frase non corrisponde più a nulla nel codice
consegnato. È anche il comportamento corretto su un'API già pubblicata: cambiare un verbo rompe
tutti i client esistenti e richiede una nuova versione dell'endpoint, non una modifica in loco.
L'analisi tecnica resta, ed è quella che dimostra di conoscere la differenza.

**Contenuto dell'analisi documentata.** `PUT` andrebbe sostituito con `POST`, perché richiede
idempotenza e un URI noto al client, mentre qui il `guid` è generato dal server. Per la ricerca
`GET` sarebbe la scelta ortodossa e la renderebbe cacheabile; `POST` resta però difendibile,
perché i criteri nel corpo non finiscono in access log, cronologia, header `Referer` e cache
intermedie — rilevante dato che il filtro può contenere nome, cognome ed email di utenti censiti.

**Percorso completo** — le tre oscillazioni e i due difetti latenti che il prototipo poi
ritirato ha fatto emergere, entrambi corretti e mantenuti — in
[report.md §2.8](report.md#28-una-decisione-presa-due-volte-e-due-difetti-emersi-per-caso).

### Decisione 5 — Unicità dell'email: limite dichiarato, non colmato

**Questione.** Due creazioni con la stessa email producono due utenti distinti: nè
`UserServiceImpl.addUser` né `UserRepository.save` verificano i duplicati. Il README non chiede
l'Unicità, ma per un archivio utenti resta una lacuna di integrità del dato. Introdurre il
vincolo o dichiararlo?

**Decisione: dichiararlo in `IMPLEMENTATION.md`, senza introdurlo.**

**Motivazione.** Non è l'economia dell'intervento, è il punto in cui il vincolo andrebbe messo.
Un controllo nel servizio — leggere e poi scrivere — è un *check-then-act*: due richieste
concorrenti con la stessa email lo superano entrambe, perché fra la lettura e la scrittura non
c'è atomicità. Sarebbe un controllo che sembra garantire l'Unicità senza garantirla, cioè
peggio che nessun controllo. Il vincolo appartiene alla persistenza, dove può essere atomico:
indice univoco su un database reale, `putIfAbsent` su una mappa indicizzata per email in quella
simulata.

**Conseguenza.** Implementarlo comporterebbe anche scegliere il codice applicativo del conflitto
(`409`), non previsto dalla specifica, e aggiungere un comportamento non richiesto a ridosso della
consegna. Il limite è documentato con l'analisi, che è ciò che dimostra di averlo visto.

## 3. Iterazioni sul piano

### Iterazione 1 — Gate di comprensione
Vincolo posto all'avvio: nessuna scrittura di codice prima di un'analisi completa del repository,
con esposizione dell'architettura e dei difetti individuati. Costo: un giro di conversazione.
Beneficio: la riformulazione corretta del task 2 (Decisione 3) invece di una modifica
architetturale inventata.

### Iterazione 2 — Chiusura delle ambiguità prima del codice
Le due questioni aperte dall'analisi sono state discusse e chiuse (Decisioni 1 e 2) prima di
autorizzare la scrittura. L'ordine è deliberato: una decisione architetturale presa **dopo** che
il codice esiste tende a essere ratificata invece che valutata, perché il costo di cambiarla è
già stato sostenuto.

### Iterazione 3 — Verifica eseguibile come criterio di completamento
Soglia fissata esplicitamente: **la compilazione non chiude un blocco funzionale**. Dopo ogni
blocco, applicazione avviata e chiamate `curl` reali con asserzione sul corpo della risposta.

È così che la policy di autorizzazione è stata confermata con evidenza invece che per fiducia:
ruolo `USER` in lettura → `status.code` 200; stesso ruolo in scrittura → `status.code` 403; token
manomesso → 401; e in tutti e tre i casi HTTP 200, come impone il task 4.

### Iterazione 4 — Correzioni di rotta sulla qualità del codice generato
Tre interventi correttivi durante la generazione: indirezione superflua in `LoginAssembler`;
dati di seed di `FakeDatabase` diventati non conformi alle regole di validazione appena scritte,
senza che alcun test fallisse; Javadoc di `StringUtil.isNullOrEmpty` divergente
dall'implementazione. Il criterio che li accomuna: lo strumento non rivede le conseguenze
indirette delle proprie modifiche, né la corrispondenza fra ciò che documenta e ciò che scrive.
Dettaglio in [report.md §2.4–2.6](report.md#24-complessità-introdotta-senza-richiesta).

### Iterazione 5 — Criterio di sufficienza dei test
Criterio imposto: non "copri tutto", ma **ogni difetto corretto deve avere un test che fallirebbe
sulla versione precedente**, con il difetto richiamato in commento.

È il motivo per cui `AssemblerTest` contiene annotazioni esplicite del tipo
`// Regressione: l'assembler originale troncava l'email al solo dominio`. Senza questo vincolo si
ottengono test che verificano il comportamento corretto ma perdono memoria di cosa lo ha reso
necessario — e che quindi nessuno sa più perché non si possano semplificare.

### Iterazione 6 — Audit avversariale sul requisito di sicurezza
Prima di considerare chiuso il task 2 ho preteso una verifica che non si appoggiasse ai test già
scritti: payload di iniezione reali sparati contro l'endpoint con l'applicazione in esecuzione.
Due sono passati, perché la regola vincolava l'alfabeto ammesso ma non la **struttura**, e i casi
di test coprivano i payload appariscenti e non quelli composti di soli caratteri consentiti.

Correzione: regola strutturale (il separatore deve stare *fra* due lettere) invece di una
blacklist più lunga, così `D'Angelo` e `Dell'Orto` restano validi. A verifica, `SqlInjectionTest`
con 44 payload su quattro campi. Payload, regex e compromesso in
[report.md §2.9](report.md#29-la-validazione-contro-linjection-aveva-due-buchi-e-i-test-non-li-vedevano).

### Iterazione 7 — Review avversariale sull'applicazione in esecuzione

Con il lavoro dichiarato concluso ho commissionato tre verifiche successive condotte
**sull'applicazione avviata**, non sulla sola lettura del codice: una review ha richieste reali
(sei difetti veri, più un limite dichiarato e non colmato — Decisione 5); una seconda passata con
token contraffatti, payload di iniezione e 80 richieste concorrenti (altri otto); e una prova
manuale, che ha mostrato il difetto più visibile di tutti — cercare `Mario Rossi` non trovava
nessuno, perché il filtro confrontava l'intera stringa con un campo per volta. Nessun test lo
copriva, perché tutti cercavano con una parola sola: verificavano la regola implementata, non
l'uso che ne fa una persona.

**Criterio adottato: nessun rilievo si chiude senza un test che fallirebbe sulla versione
precedente.** Le quindici correzioni hanno aggiunto tredici test: le due che non ne hanno uno
sono rimozioni di codice morto e di documentazione errata, dove non esiste un comportamento da
far fallire. Elenco completo dei rilievi in
[report.md §2.12](report.md#212-la-review-finale-sullapplicazione-in-esecuzione-ha-trovato-sei-difetti-che-la-suite-non-vedeva).

## 4. Ripartizione del lavoro

### Delegato all'assistente
- **Stesura meccanica**: Javadoc, assembler, boilerplate dei DTO.
- **Espansione sistematica dei casi di test** a partire dalle regole da me fissate: le 57
  asserzioni parametrizzate di `ValidationUtilTest` su email, numerazioni e nomi sono enumerazione
  esaustiva, l'attività in cui lo strumento rende di più.
- **Traduzione in configurazione di requisiti già decisi**: `SecurityFilterChain`, filtro
  `OncePerRequestFilter`, `@RestControllerAdvice`.
- **Ricognizione delle anomalie** nel codice di partenza.

### Trattenuto e deciso da me
- **Le cinque decisioni di progetto** del §2.
- **La selezione delle anomalie da correggere** (Decisione 4), che richiede di interpretare
  l'intenzione della traccia e non il codice.
- **Il livello di severità delle regole di validazione**: le regex sono state proposte
  dall'assistente, ma l'insieme dei casi che devono passare e fallire l'ho fissato io, e alcune
  proposte iniziali erano troppo permissive.
- **Il vincolo architetturale**, riverificato a ogni diff: nessun oggetto `Request`/`Response` è
  entrato nel layer di servizio, nessun `User` di persistenza è uscito verso il web.
- **La soglia di completamento** (Iterazione 3) e **il criterio di sufficienza dei test**
  (Iterazione 5).

---

## 5. La dipendenza fra requisiti che il piano non aveva previsto

Il task 4 impone che **ogni** risposta, errori inclusi, torni con HTTP 200. Il task 5 introduce
Spring Security, che respinge le richieste non autenticate con 401 e quelle non autorizzate con
403 **prima** che raggiungano il controller — quindi fuori dalla portata di `@RestControllerAdvice`.

Completato il task 5, l'invariante del task 4 era silenziosamente rotta. **Nessun test falliva**,
perché i test del task 4 erano stati scritti quando la sicurezza non esisteva ancora.

Correzione: `SecurityStatusWriter`, che fornisce `AuthenticationEntryPoint` e `AccessDeniedHandler`
personalizzati e riporta anche gli esiti di sicurezza allo standard `StatusDTO` con HTTP 200.
Verificata in `UserControllerTest`, dove richieste anonime e token manomessi sono asseriti come
`status().isOk()` con `status.code` 401.

È l'esempio più netto del limite dello strumento su questo esercizio: **affidabile su un
requisito alla volta, cieco sull'interazione fra requisiti già chiusi.** La dipendenza è emersa
perché ho adottato la pratica di rileggere i requisiti completati dopo ogni blocco nuovo, non
perché sia stata segnalata.

---

## 6. Stato finale

| Task                                 | Esito                                        | Verifica                                     |
|--------------------------------------|----------------------------------------------|----------------------------------------------|
| 1. Validazione email e telefono IT   | Completo                                     | 57 asserzioni parametrizzate + integrazione  |
| 2. Prevenzione SQL Injection         | Completo, requisito riformulato              | 44 payload di iniezione su 4 campi           |
| 3. Paginazione, ordinamento, ricerca | Completo, filtro corretto dopo prova manuale | 10 test unitari + 4 di integrazione          |
| 4. Exception handling centralizzato  | Completo, riaperto dopo il task 5            | Ogni test di integrazione asserisce HTTP 200 |
| 5. JWT (policy, issuer, expiration)  | Completo                                     | 9 test + verifica manuale via `curl`         |
| 6. Bug fixing                        | 32 corretti, 2 documentati                   | Test di regressione dedicati                 |
| 7. Unit testing                      | 172 test                                     | `./mvnw test`, tutti verdi                   |
| 8. Javadoc                           | Completo su tutte le classi                  | —                                            |

Copertura misurata con JaCoCo: **91,2% linee · 79,7% branch · 100% classi.**
