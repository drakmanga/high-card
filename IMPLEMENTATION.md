# Note di implementazione

Riepilogo del lavoro svolto sui task del [README](README.md).
Il processo di collaborazione con l'AI è documentato in [`/ai-assisted/`](ai-assisted/).

---

## Avvio e verifica

```bash
./mvnw test           # 172 test, report di copertura in target/site/jacoco/index.html
./mvnw spring-boot:run
```

Utenze dimostrative: `admin` / `admin123` (lettura e scrittura), `user` / `user123` (sola lettura).
Esempi di chiamata completi nell'[appendice del report](ai-assisted/report.md#appendice--come-verificare-il-risultato).

| Endpoint         | Metodo | Operazione                                        | Policy                 |
|------------------|--------|---------------------------------------------------|------------------------|
| `/auth/v1/login` | `POST` | autenticazione                                    | pubblico               |
| `/user/v1/user`  | `PUT`  | creazione utente, restituisce il `guid` assegnato | ruolo `ADMIN`          |
| `/user/v1/user`  | `POST` | ricerca utenti                                    | ruolo `USER` o `ADMIN` |

> I verbi sono quelli del codice di partenza. Non sono semanticamente corretti:
> l'analisi e la scelta di mantenerli sono nella sezione [Task 6](#task-6--bug-corretti).

---

## Task 1 — Validazione

`ValidationUtil` nel layer di servizio, con regole **whitelist**:

- **email**: forma RFC-compatibile, TLD di almeno due caratteri, massimo 254 caratteri;
- **telefono italiano**: prefisso `+39`/`0039` facoltativo, mobile `3xx` da 9 a 10 cifre,
  fisso `0x` da 6 a 11 cifre; i separatori di formattazione (`+39 330 123 4567`) sono
  normalizzati prima del confronto e prima della persistenza;
- **nome e cognome**: lettere Unicode, spazi, apostrofi e trattini; cifre e simboli respinti.

Validazione applicata su **due livelli**, come previsto nel piano:
Bean Validation su `AddUserRequest` come filtro sul bordo HTTP, e validazione nel servizio come
autorità — perché il servizio deve restare sicuro anche se invocato da un chiamante non HTTP.

## Task 2 — Prevenzione SQL Injection

**Il progetto non contiene SQL**: la persistenza è una `ArrayList` statica in `FakeDatabase`.
Non esiste quindi una query da parametrizzare.

Il difetto reale dell'endpoint `PUT` era che input non fidato raggiungeva lo strato di persistenza
**senza alcun controllo di forma**, che è la precondizione della vulnerabilità. La correzione è
la validazione whitelist del task 1.

### Cosa garantisce, e cosa no

**Nessuna validazione di input garantisce immunità dalla SQL Injection.** L'immunità deriva
dalla **query parametrizzata** (`PreparedStatement`, binding JPA/JDBC), che separa il codice SQL
dai dati e rende irrilevante il contenuto dell'input. La validazione è difesa in profondità:
riduce la superficie di attacco, non la azzera. Sostenere il contrario sarebbe scorretto.

Le blacklist di sequenze pericolose (`'`, `--`, `UNION SELECT`) non sono usate: sono
sistematicamente aggirabili tramite codifiche alternative, e vietano caratteri legittimi.

### Regole applicate

| Campo          | Regola                                                                                                            |
|----------------|-------------------------------------------------------------------------------------------------------------------|
| Nome e cognome | Gruppi di lettere Unicode separati da un **singolo** spazio, apostrofo o trattino                                 |
| Email          | Parte locale alfanumerica con separatori `. _ % + -`, che deve iniziare e terminare con un carattere alfanumerico |
| Telefono       | Numerazione italiana, verificata dopo normalizzazione dei separatori                                              |

La regola sui nomi è **strutturale, non un elenco di caratteri vietati**: il separatore deve
stare *fra* due lettere, quindi non può aprire o chiudere il valore nè comparire due volte di
seguito.

È il compromesso che risolve il caso difficile: `D'Angelo`, `Dell'Orto`, `Anna-Maria` e
`De Luca` sono cognomi italiani legittimi e **devono** passare, quindi l'apostrofo non può essere
vietato; ma `admin'--` non è un nome, e viene respinto pur usando solo caratteri dell'alfabeto
ammesso. Vietare l'apostrofo sarebbe stato al tempo stesso inefficace, perché esistono payload
che non lo usano, e dannoso per gli utenti reali.

Per l'email si è scelto un sottoinsieme deliberato di quanto consentirebbe RFC 5322, che ammette
anche apice singolo e backtick nella parte locale: sono legali ma praticamente inutilizzati, e
la loro esclusione riduce la superficie senza impatto sugli indirizzi reali.

### Verifica

`SqlInjectionTest` esegue **44 payload** contro l'endpoint `PUT` — `DROP TABLE`, `OR '1'='1`,
`UNION SELECT`, commenti SQL, `xp_cmdshell`, iniezioni temporali, codifiche esadecimali e URL —
su tutti e quattro i campi. Per ciascuno verifica il codice applicativo 400 **e** che la
persistenza non sia cresciuta. Sei casi verificano in positivo che i nomi legittimi con apostrofo,
trattino e spazio restino accettati.

## Task 3 — Paginazione, ordinamento, ricerca

`UserServiceImpl.getUsers`, prima uno stub `return null`.
Filtro case-insensitive su nome, cognome ed email, valutato **per parole**: ognuna deve trovare
riscontro in almeno uno dei tre campi, così che `Mario Rossi` trovi l'utente che `Mario` e `Rossi`
trovano separatamente. Ordinamento dai quattro valori di `OrderType`;
paginazione `offset`/`limit` con massimo 100 elementi per pagina e default 20.

Il `total` restituito conta gli elementi che soddisfano il filtro **prima** del taglio di pagina,
così che il client possa calcolare il numero di pagine.

## Task 4 — Exception handling centralizzato

`GlobalExceptionHandler` (`@RestControllerAdvice`) uniforma ogni esito allo standard `StatusDTO`:
**HTTP 200 sempre**, codice reale in `status.code`, `traceId` per la correlazione con i log.
Copre eccezioni applicative, errori di Bean Validation, corpi JSON malformati, parametri non
convertibili, verbi non ammessi sulla risorsa (405), `Content-Type` assente o non gestito (415),
e una rete di sicurezza per le eccezioni impreviste (il cui dettaglio resta nei log e non viene
esposto al client).

**Log e dati personali.** L'errore viene registrato con il messaggio già ripulito, non con quello
originale dell'eccezione: quest'ultimo riporta i valori rifiutati — email, telefono, nome — e su
dati personali di clienti il log applicativo non è una destinazione accettabile. Il `traceId`
correla comunque risposta e richiesta.

La rete di sicurezza cattura solo ciò che non ha un esito proprio: un'eccezione che porta già
il proprio stato riceve un handler dedicato, altrimenti un errore del chiamante verrebbe
dichiarato guasto del server e l'invariante su `status.code` perderebbe significato.

L'invariante riguarda le risposte con corpo. Restano fuori tre esiti di puro protocollo, privi di
corpo per definizione: `OPTIONS` (gestito dal framework), `HEAD` e `TRACE` (respinto dal container).

`SecurityStatusWriter` estende lo stesso standard agli esiti di Spring Security, che vengono
prodotti **prima** della catena `@ExceptionHandler`: anche 401 e 403 tornano come HTTP 200 con il
codice applicativo nel corpo.

## Task 5 — Sicurezza JWT

Il token è validato su tutti i criteri richiesti:

| Criterio       | Implementazione                                                                  |
|----------------|----------------------------------------------------------------------------------|
| **Policy**     | Ruoli nel claim `roles` → authority Spring; regole per rotta in `SecurityConfig` |
| **Issuer**     | `requireIssuer` sul claim `iss`                                                  |
| **Expiration** | `exp` verificato con tolleranza di sfasamento configurabile                      |
| *Firma*        | HMAC-SHA256; i token non firmati sono respinti                                   |
| *Audience*     | `requireAudience` sul claim `aud`                                                |

**Autorizzazione in negativo:** le regole per rotta sono per path esatto, quindi una variante del
percorso non le incontra. La catena si chiude con `denyAll()`, non con `authenticated()`: senza,
una variante del percorso di scrittura scivolerebbe sulla regola finale e un token di sola lettura
la supererebbe. Un percorso inesistente riceve perciò una negazione e non un `404`, il che impedisce
anche di enumerare le rotte.

Sessione `STATELESS`, CSRF disattivato perché non si usano cookie di sessione.
Il segreto di firma è letto da `JWT_SECRET`, con un valore di sviluppo come fallback.

> **L'endpoint `/auth/v1/login` è un componente dimostrativo.** Esiste perché senza un punto
> di emissione il requisito JWT non sarebbe verificabile end-to-end. In esercizio va rimosso:
> un servizio di dominio non deve essere anche authorization server. L'evoluzione è delegare
> l'emissione a un Identity Provider esterno (Keycloak, Auth0, Entra ID) e riconfigurare
> l'applicazione come puro **OAuth2 Resource Server**, sostituendo la firma simmetrica HS256 con
> la verifica asimmetrica RS256/ES256 su JWKS. La logica di validazione già presente — issuer,
> audience, scadenza, policy — resta invariata. Dettaglio completo nel Javadoc di `AuthServiceImpl`.

## Task 6 — Bug corretti

| #  | Dove                                  | Difetto                                                                                                                                                                                           |
|----|---------------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| 1  | `AddUserAssembler`                    | `setLastName(request.getFirstName())`: il cognome andava perso                                                                                                                                    |
| 2  | `UserAssembler`                       | `substring(lastIndexOf("@")+1)`: il DTO riceveva **solo il dominio** dell'email                                                                                                                   |
| 3  | `UserAssembler`                       | `phoneNumber` mai mappato sul DTO                                                                                                                                                                 |
| 4  | `UserServiceImpl`                     | `catch (Exception)` inghiottiva le `GenericException` 400 e le degradava tutte a 500: i messaggi di validazione non raggiungevano mai il client                                                   |
| 5  | `GenericException`                    | `GENERIC_ERROR` era uno `StatusDTO` statico **mutabile** condiviso, privo di `traceId`                                                                                                            |
| 6  | `GenericPagedResult`                  | `total` privo di accessori: la paginazione non poteva funzionare                                                                                                                                  |
| 7  | `CriteriaGetUsers`                    | `BY_LASTNAME_DESC` con etichetta `"by lastName"`, priva di `desc`                                                                                                                                 |
| 8  | `UserRepository`                      | `getAll()` esponeva il riferimento vivo alla lista statica                                                                                                                                        |
| 9  | `UserRepository`                      | `NullPointerException` su `guid` nullo                                                                                                                                                            |
| 10 | `UserServiceImpl`                     | `AddUserResult` non restituiva il `guid` dell'utente creato                                                                                                                                       |
| 11 | `FakeDatabase`                        | dati di seed non conformi alle regole di validazione del dominio                                                                                                                                  |
| 12 | `UserAssembler`                       | `NullPointerException` su entità o email nulle                                                                                                                                                    |
| 13 | `FakeDatabase`                        | `ArrayList` condiviso fra i thread di richiesta: una ricerca concorrente a una creazione sollevava `ConcurrentModificationException`                                                              |
| 14 | `AuthServiceImpl`                     | password memorizzate in chiaro e cifrate a ogni login, con doppia operazione BCrypt                                                                                                               |
| 15 | `GetUsersRequest` / `UserServiceImpl` | limite massimo di pagina definito due volte, destinato a divergere                                                                                                                                |
| 16 | `GlobalExceptionHandler`              | verbo non mappato segnalato come errore interno 500 anzichè 405                                                                                                                                   |
| 17 | `GlobalExceptionHandler`              | gli errori di conversione esponevano al client il nome completo delle classi interne                                                                                                              |
| 18 | `UserController`                      | il `guid` prodotto da `AddUserResult` veniva scartato: la creazione rispondeva senza identificativo, e il client non poteva referenziare l'utente appena creato                                   |
| 19 | `GlobalExceptionHandler`              | `Content-Type` assente o non gestito segnalato come errore interno 500 anzichè 415                                                                                                                |
| 20 | `SecurityConfig`                      | la regola finale `authenticated()` lasciava passare a qualunque utente autenticato le varianti del percorso di scrittura (slash finale, maiuscole), che non incontrano il matcher per path esatto |
| 21 | `SecurityConfig`                      | `JwtAuthenticationFilter`, essendo un bean `Filter`, veniva registrato anche dal servlet container su tutti i percorsi, quindi fuori dalla catena di sicurezza                                    |
| 22 | `GlobalExceptionHandler`              | il log degli errori di validazione riportava i valori rifiutati, cioè email, telefono e nome dell'utente                                                                                          |
| 23 | `GlobalExceptionHandler`              | un valore di enum inesistente veniva segnalato come corpo malformato, pur essendo il JSON sintatticamente valido                                                                                  |
| 24 | `AuthServiceImpl` / `LoginRequest`    | lo username finiva nel log senza filtro: un ritorno a capo permetteva di inserire righe arbitrarie nel log applicativo                                                                            |
| 25 | `JwtTokenProvider`                    | un token firmato ma privo di `sub` autenticava una richiesta priva di identità, non attribuibile a nessuno                                                                                        |
| 26 | `HighCardApplication`                 | l'autoconfigurazione di Spring Boot creava un'utenza in memoria con password generata a ogni avvio, stampata nei log                                                                              |
| 27 | `UserServiceImpl`                     | `trim()` sui campi testuali: irraggiungibile, perché la validazione rifiuta già gli spazi di bordo                                                                                                |
| 28 | `UserRepository`                      | `getByGuid` non era invocato da nessuno nè coperto da test                                                                                                                                        |
| 29 | `GenericResponse`                     | `success(...)` rimasto orfano dopo il difetto 18: la creazione ora costruisce la risposta nell'assembler                                                                                          |
| 30 | `GetUsersRequest`                     | il messaggio del limite massimo cablava `100` invece del vincolo, pronto a divergere da `MAX_LIMIT`                                                                                               |
| 31 | `GenericPagedResponse`                | il Javadoc dichiarava `total` «al netto della paginazione», mentre il valore è il totale **prima** del taglio di pagina                                                                           |
| 32 | `UserServiceImpl`                     | il filtro confrontava l'intera stringa con un campo per volta: cercare per nome completo non trovava mai nessuno, perché nessun campo contiene nome e cognome insieme                             |

### Anomalia rilevata e deliberatamente non corretta: i verbi HTTP

`PUT` è usato per la creazione e `POST` per la ricerca. **Entrambi sono in contrasto con
RFC 9110**, e l'analisi è la seguente:

- **`PUT` per la creazione è il difetto più netto.** `PUT` richiede semantica **idempotente**
  e indirizza una risorsa a un URI già noto al client. Qui il `guid` è generato dal server e
  ogni invocazione ripetuta produce un utente distinto: è la definizione di `POST`. `PUT`
  sarebbe corretto solo su un URI del tipo `/user/{guid}`, con identificativo scelto dal client.
- **`POST` per la ricerca è accettabile ma non ottimale.** La scelta ortodossa sarebbe `GET`
  con i criteri in query string, che renderebbe l'operazione cacheabile e ne dichiarerebbe
  l'idempotenza. `POST` resta però un compromesso difendibile: i criteri viaggiano nel corpo e
  non finiscono in access log, cronologia del client, header `Referer` e cache intermedie —
  vantaggio concreto quando il filtro contiene nome, cognome ed email di utenti censiti.

**Decisione: mantenere i verbi originali.** La specifica dell'esercizio identifica esplicitamente
l'endpoint di creazione come *"the PUT endpoint"*: modificarlo renderebbe quel riferimento
incoerente con il codice consegnato. In una prova di selezione l'aderenza alla specifica prevale
sulla convenzione, e l'anomalia viene segnalata invece che corretta — che è anche il
comportamento corretto su un'API già pubblicata, dove cambiare un verbo rompe tutti i client
esistenti e richiede una nuova versione, non una modifica in loco.

L'analisi è riportata anche nel Javadoc di `UserController`, dove la legge chi manutiene il codice.

**Effetto collaterale utile.** La modifica è stata prototipata e poi ritirata, ma ha portato alla
luce i difetti 16 e 17, che erano latenti e non coperti da alcun test: un verbo non mappato
produceva un errore interno `500` invece di un `405`, e gli errori di conversione dei parametri
restituivano al client il nome completo delle classi interne. **Entrambe le correzioni sono state
mantenute.**

### Limite noto e deliberatamente non colmato: Unicità dell'email

Due creazioni con la stessa email producono due utenti distinti: nè `UserServiceImpl.addUser`
nè `UserRepository.save` verificano i duplicati. La specifica non richiede l'Unicità, e per un
archivio utenti resta comunque una lacuna di integrità del dato, quindi la si dichiara.

**Decisione: non introdurre il vincolo.** La ragione non è l'economia dell'intervento, ma il
punto in cui andrebbe messo. Un controllo nel servizio — leggere e poi scrivere — è un
*check-then-act*: due richieste concorrenti con la stessa email lo superano entrambe, perché fra
la lettura e la scrittura non c'è atomicità. Il vincolo appartiene al layer di persistenza, dove
può essere atomico: un indice univoco su un database reale, o un'entrata `putIfAbsent` su una
mappa indicizzata per email nella persistenza simulata. Aggiungerlo qui significherebbe quindi
scegliere anche il codice applicativo del conflitto (`409`), non previsto dalla specifica.

## Task 7 — Test

172 test, tutti verdi. Copertura JaCoCo: **91,2% linee, 79,7% branch, 100% classi.**

Criterio adottato: ogni bug corretto ha un test che **fallirebbe sulla versione precedente**,
con il difetto richiamato in commento (si veda `AssemblerTest`).

## Task 8 — Javadoc

Presente su tutte le classi, sui metodi pubblici e sugli attributi, con particolare attenzione
alle motivazioni di progetto: strategia difensiva in `ValidationUtil`, criteri di validazione in
`JwtTokenProvider`, policy in `SecurityConfig`, natura dimostrativa di `AuthServiceImpl`.

---

## Risposta alla domanda di approfondimento

> **Perché, nei progetti grandi e complessi, è sconsigliato usare oggetti esposti sul web
> (`? extends GenericRequest`, `? extends GenericResponse`) all'interno del Service layer?**

Perché lega la logica di business al contratto di trasporto, e i due hanno ragioni di
cambiare del tutto indipendenti. In concreto:

**1. Accoppiamento fra contratto pubblico e logica interna.** Se il service accetta una
`Request`, ogni modifica dell'API — rinominare un campo, cambiare versione, adeguarsi a un
client — si propaga dentro la logica di dominio. Il contratto esposto è la parte più instabile
del sistema, ed è quella su cui si ha meno controllo: renderla una dipendenza del dominio
significa subire quell'instabilità ovunque.

**2. Il servizio diventa invocabile solo via HTTP.** Un service che parla in `Request`/`Response`
non è riutilizzabile da uno scheduler, da un consumer di code, da un batch o da un altro
servizio senza costruire artificialmente oggetti web che nessuno ha ricevuto da una richiesta.
La logica resta prigioniera del canale attraverso cui è nata.

**3. La validazione si sposta nel posto sbagliato.** Se i vincoli vivono solo nelle annotazioni
delle `Request`, il servizio è sicuro **solo** quando lo chiama il controller. Ogni altro
chiamante lo raggiunge senza controlli. È esattamente il motivo per cui, in questo progetto, la
validazione è duplicata: Bean Validation come primo filtro sul bordo HTTP, ma l'autorità è nel
servizio.

**4. Rischio concreto di esposizione involontaria.** Gli oggetti web sono serializzati verso
l'esterno. Se sono gli stessi che circolano nel dominio, ogni campo aggiunto per necessita'
interna diventa un campo pubblicato per default — e quasi sempre nessuno se ne accorge, perché
non c'è un punto in cui la decisione "questo si espone" viene presa esplicitamente.
Gli assembler sono quel punto.

**5. Perdita di significato semantico.** Una `Request` descrive *cosa è arrivato dalla rete*, un
`Criteria` descrive *cosa deve fare il dominio*. Spesso non coincidono: paginazione con valori di
default, campi derivati, identità presa dal token e non dal corpo della richiesta. Collassare i
due concetti nasconde proprio le trasformazioni che meritano di essere esplicite.

**6. Le generiche non aiutano, peggiorano.** Una firma `<T extends GenericRequest>` sembra
disaccoppiare, ma fa il contrario: dichiara che il dominio conosce e accetta la gerarchia web.
È accoppiamento con un livello di indirezione in più — più difficile da vedere e da rimuovere,
perché la dipendenza è sparsa fra i parametri di tipo invece di essere concentrata in un
assembler che si può riscrivere.

**Il costo del disaccoppiamento** è una classe di traduzione per operazione e qualche riga di
mappatura. È un costo lineare e localizzato. Il costo dell'accoppiamento cresce invece con la
dimensione del sistema e si manifesta quando è più caro pagarlo: al primo cambio di versione
dell'API, o al primo riuso della logica fuori dal canale HTTP.

È la struttura che questo progetto già adotta — `Request → Assembler → Criteria → Service →
Result → Assembler → Response` — ed è il motivo per cui non è stata modificata.
