#import "@preview/codly:1.3.0": *

#show link: it => underline(text(fill: blue, it))
#set page(numbering: "1")
#set heading(numbering: "1.1")

#let img(path) = box(
    scale(10%, image("img/" + path + ".png"), reflow: true),
    radius: 10pt,
    stroke: 1pt + black,
    inset: 5pt,
    fill: black,
)

#let fig-img(path, content) = figure(
    img(path),
    caption: content,
)

#let side-img(path, content) = grid(
    columns: (auto, 1fr),
    gutter: 15pt,
    grid.cell(
        img(path),
    ),
    grid.cell(content),
)

#let double-img(i1, i2) = align(center, grid(
    columns: (1fr,) * 2,
    gutter: 20pt,
    img(i1), img(i2),
))

// ----------- //
#show: codly-init.with()

#align(center, {
    text(size: 20pt, [Relazione Progetto Lab. Applicazioni Mobili])
    v(30pt)
    text(size: 16pt, [Ultiwear])
})
Realizzata da Alessandro Nanni\
Email: `alessandro.nanni17@studio.unibo.it`\
Matricola: `0001027757`


#outline(title: [Indice])

#v(100pt)



#counter(page).update(1)

= Overview dell'app
L'applicazione Ultiwear è progettata per giocatori di frisbee con la passione per lo scambio di capi d'abbigliamento.\
Tramite essa è possibile visualizzare i propri capi in un armadio digitale, pubblicarli, mostrare interesse per eventuali scambi, consultare i tornei futuri e parteciparvi, e gestire scambi in tempo reale.

Dato che l'app non vuole sostituire il momento dello scambio in persona, e lasciare un pò di mistero e magia, non ci sono username pubblici e non si ha modo di comunicare con gli altri.

== Note tecniche
Quasi tutti i dati utilizzati sono salvati in un database _firebase_: questo include sia le rappresentazioni in forma di tabelle di oggetti, che le immagini.\
L'app è realizzata con _jetpack compose_: non ci sono file XML contenenti _view_ nelle nella cartella `res`.\
L'autenticazione è effettuata tramite account Google.\
Come consigliato dalla #link("https://developer.android.com/topic/architecture/recommendations#ui-layer")[documentazione ufficiale], l'app utilizza una singola attività, la navigazione tra le diverse sezioni è gestita tramite lo stato di Compose.\
Si cerca di rispettare il modello MVVM, sono presenti _package_ con compiti precisi:
- `data`: comunicazione con il database;
- `model`: contiene le `data class`
- `notifications`: si occupa dell'invio di notifiche;
- `view`: contiene i _composable_;
- `viewModel`: contiene i `ViewModel` intermediari tra `View` e `Model`
L'app utilizza il `MaterialTheme` di Android.\

== Primo avvio dell'app

#side-img(
    "permessi",
)[La prima volta che si avvia l'app verranno richiesti i permessi necessari: invio di notifiche, posizione e accesso alla fotocamera, richiesti dal `manifest`.
    #codly(
        header: [AndroidManifest.xml],
    )
    ```xml
        <uses-permission android:name="android.permission.CAMERA" />
        <uses-permission android:name="android.permission.POST_NOTIFICATIONS"/>
        <uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
    ```
    Nella `MainActivity`, questi vengono richiesti in catena affinché l'ultimo (locationPermission), non sovrascrivi i precedenti.
]

#figure(
    ```kt
    private fun requestPermissionsOnStartup() {
            notificationPermissionLauncher.launch(POST_NOTIFICATIONS)
        }
    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { _ ->
            requestCameraPermission() // next
        }
    ```,
    caption: [Dopo aver richiesto il permesso per le notifiche, si chiederà quello per la fotocamera],
)
Successivamente, tramite stati _composable_ dell' `authViewModel`, si controlla che l'utente sia registrato e connesso a internet.
#side-img(
    "google_auth",
)[
    #figure(
        ```kt
        GetCredentialRequest.Builder().addCredentialOption(
          GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(BuildConfig.GOOGLE_CLIENT_ID)
            .setAutoSelectEnabled(false)
            .build()).build()
        ```,
        caption: [Codice per creare il prompt "_sign in with Google_" e ottenere un token di autenticazione],
    )
]
In base a questi stati si mostreranno 3 _view_ diverse. Dopo aver eseguito l'accesso con Google, l'utente potrà utilizzare l'app.\
Il token di login sarà poi convertito in un token Firebase, memorizzato in una collezione come UID assieme alla mail dell'utente. Ora l'utente ha accesso all'app, e potrà navigare le sue 5 _view_.\
`var selectedIndex by rememberSaveable { mutableIntStateOf(0) }` tiene traccia della _sub-activity_ corrente. Ognuna di esse è definita dal _composable_ `TabItem`.
```kt
val tabs = listOf(
  TabItem(
      "Wardrobe", R.drawable.wardrobe
  ) {
      WardrobeScreen(wardrobeViewModel)
  },
  ...
)
```
= Guardaroba/Wardrobe
Qui l'utente può inserire dati e foto relativi ai suoi capi d'abbigliamento.
#side-img(
    "add_wardrobe_item",
)[
    Per ogni capo si può fare una foto del fronte, retro, specificare taglia e condizioni, e infine scegliere se pubblicare#footnote[Visibile nella sezione _Browse_, mostrata in seguito.] e indicare se questo capo lo si vuole scambiare. I capi sono mostrati tramite una `LazyVerticalGrid` di 3 colonne. Cliccando un elemento della griglia si potranno visualizzare i dettagli del capo.\
    Le operazioni sul database sono effettuate tramite l'oggetto `ItemHandler`, di cui il `WardrobeViewModel` possiede un riferimento.
    Un `listener` controlla continuamente cambiamenti nella collezione `wardrobe` per gli oggetti dell'owner. In caso di cambiamenti si aggiornano 3 liste di item: una che li contiene tutti, una che contiene solo quelli scambiabili e un ultima che contiene solo quelli posseduti (`items.filter { it.owned }`). Il `listener` permette di aggiornare automaticamente gli _item_ dell'utente in ogni punto dell'app (_Wardrobe_, _Browse_ e _Trade_).\
    Ogni oggetto capo contiene la variabile booleana `owned`. Questa è usata per indicare se il capo deve essere mostrato nell'armadio, nel caso l'utente corrente non ne sia più il proprietario in seguito ad uno scambio#footnote[Eseguibile nella sezione _Quick Trade_, mostrata in seguito.].
]

Inizialmente per gestire la cancellazione di un capo si eliminava il documento corrispondente. Dopo aver aggiunto i post e timeline degli scambi, è stato utilizzato un metodo che non elimina i propri capi, ma li nasconde solamente. Il post associato invece viene eliminato, ma dato che l'item originale esiste ancora sul database, vi si può fare riferimento per mostrare gli item dati via in uno scambio.

Le foto degli item (e come si vedrà in seguito, anche quelle degli scambi) vengono modificate prima di essere caricate sul database. Una singola foto scattata occupa dai 3.5MB ai 4MB. Dato che Firebase fornisce 1GB di memoria prima di far pagare per ogni megabyte occupato, per sicurezza ho implementato due metodi che riducono la dimensione dell'immagine.
```kt
val baos = ByteArrayOutputStream()
bitmap.compress(Bitmap.CompressFormat.WEBP, 80, baos)
val bytes = baos.toByteArray()
```
Questo metodo, dopo aver ricavato la `bitmap` dell'immagine scattata con la fotocamera, converte l'immagine in formato WEBP#footnote[Le immagini WEBP hanno dimensione minore di quelle in formato PNG o JPEG.] con una qualità pari all'80% di quella originale. Infine i byte dell'immagine vengono caricati su Firebase.
= Esplora/Browse

#side-img(
    "browse",
)[
    In questa sezione si possono vedere le foto e dettagli degli _item_ postati. Per ogni post non proprio si può anche mettere like e se il proprietario lo ha permesso, esprimere interesse di scambio. I bottoni di scambio e like sono oscurati e non cliccabili per i propri capi d'abbigliamento.\
    Ogni post è contenuto in una `LazyColumn`, ed è una `Card` composta da un `Pager` che si può scorrere per vedere le foto di fronte e retro e una `InfoBar`.\
    Il `BrowseViewModel` contiene un riferimento allo stesso `handler`, e dunque `listener` del di _Wardrobe_. Dunque, quando un utente preme il bottone like, il `ViewModel` viene notificato di questo cambiamento e ricarica la colonna.
    Per evitare che un utente possa mettere like allo stesso post più volte, ogni post dispone di una collezione che contiene gli id degli utenti che hanno messo like.\
    Similmente, la collezione `trade_interests` tiene traccia degli utenti che hanno espresso interesse a scambiare un certo item.
]
```kt
try {
  val newLikes = handler.toggleLike(wardrobeUid, currentUser.uid)
  // update state
  _items.value = _items.value.map { item ->
    if (item.wardrobeUid == wardrobeUid) {
        item.copy(post = item.post?.copy(likes = newLikes))
    } else item
  }
} catch (e: Exception) {
  Log.e(tag, "Error toggling like", e)
}
```
I like possono essere messi e tolti: il handler si occupa di aggiornare il database con una transazione per rendere atomica l'operazione di lettura e scrittura. Il `ViewModel` crea una copia degli _items_, e se l'id di uno di essi corrisponde con quello a cui è stato cambiato il like, si aggiorna il valore mostrato nella _card_.

= Eventi/Upcoming Events
Qui si possono vedere i prossimi tornei da tutto il mondo, ottenuti tramite l'api di #link("https://ultical.com/")[Ultical]. Sono _card_ ordinate dalla data di inizio più vicina alla più lontana. Ogni card ha associata una _checkbox_ per indicare se si sarà presenti ad un certo torneo.
#side-img(
    "events",
)[
    Il singleton `APIClient` inizializza una sola volta (tramite `by lazy`) l'interfaccia `UlticalAPI` fornendogli l'url a cui fare la chiamata HTTP, e una factory da usare per processare i dati. In questo caso si usa la libreria GSON di Google in quanto l'API di ultical restituisce un oggetto JSON. Retrofit è la libreria usata per fare chiamate HTTP.
    ```kt
    val api: UlticalApi by lazy {
      Retrofit.Builder()
          .baseUrl(URL)
          .addConverterFactory(
            GsonConverterFactory.create()
            )
          .build()
          .create(UlticalApi::class.java)
    }
    ```
    L'interfaccia fornisce un metodo `getEvents()` per ottenere una lista di eventi.
]

Quando viene aperta questa sezione per la prima volta, prima si esegue un "flattening" dell oggetto JSON: dato che un torneo ha una lista di edizioni, ciascuna con un ID, si esegue questa operazione per lavorare su dati più omogenei. Nella lista appiattita vengono inclusi solamente gli eventi con data di inizio maggiore o uguale a quella attuale.\
Quando un utente si dichiara presente ad un torneo, non solo si aggiorna la collezione su Firebase, ma un _listener_ sulla collezione `user_attendances` modifica la `MutableMap` `attendances`, che per ogni ID, associa un booleano per indicare se l'utente sarà presente ad un certo torneo.
Questo esempio evidenza perfettamente l'utilità del `ViewModel`: qualora ci fosse bisogno di ricomporre lo schermo, non è necessario fare una nuova chiamata all'API, dal momento che i valori sono già memorizzati `EventViewModel`.\ Quando si apre nuovamente la schermata eventi dopo aver chiuso l'app, le presenze sono ripristinate sulla _view_ perché ogni _card_ dispone di un riferimento al `ViewModel`, che può usare per controllare se l'utente è presente al torneo con id pari a quello della _card_ che si sta componendo.

= Scambi/Trades
In questa _view_ si possono vedere e fare tutte le operazioni relative agli scambi, interagendo direttamente o indirettamente con altri utenti.

La sezione degli scambi è costituita da 3 _view_. Quella attualmente in vista è controllata da una `state variable` di _compose_
```kt
var selectedTab by remember { mutableIntStateOf(0) }
val tabs = listOf("Matches", "Manual Trade", "Quick Trade")
```
Per ogni `tab`, se la sua posizione nella lista coincide con l'indice, il suo sfondo viene colorato per evidenziare che è quella selezionata. Un altro stato, `showTradesDialog` (booleano) indica se bisogna mostrare il dialogo che contiene lo storico degli scambi.

== Matches
In questa sezione è possibile visualizzare i tornei a cui parteciperanno gli utenti che hanno espresso interesse per un determinato capo, o per i quali l'utente stesso ha mostrato interesse.
#double-img("posted_items", "interested_items")

In _Posted Items_ puoi vedere quante persone sono interessate agli oggetti che hai pubblicato e che parteciperanno a un torneo a cui sarai presente anche tu. Ad esempio, due persone che vogliono scambiare la maglia dell'Italia saranno a _Pw'Hat_, a cui ci sarò anch'io.
In _Interested Items_ puoi invece visualizzare gli oggetti per cui hai espresso interesse, i cui proprietari saranno presenti a un torneo in comune con te. Ad esempio, l'utente proprietario della canottiera del giappone per la quale ho espresso interesse a scambiare sarà, come me, ai _Beach Masters Barcelona_.\
Anche in questa _view_, uno stato `var showIncoming by remember { mutableStateOf(false) }` si occupa di indicare quale sezione sarà visibile.\
La struttura è gestita da un unico componente _composable_, il quale, in base al valore della variabile `showIncoming`, determina quale delle due liste utilizzare per popolare il contenuto. L'assegnazione avviene tramite `displayMatches = if (showIncoming) incomingMatches else matches`.\
`TradeMatchesViewModel` si occupa di fare gli incroci tra i tornei e gli oggetti che gli utenti vogliono scambiare. Dato che richiede le gli eventi e gli _item_ di un utente, esso dispone di riferimenti ai `ViewModel` delle sezioni _browse_ ed _events_. Prima di calcolare gli incroci, si attende che i valori dei ViewModel richiesti siano pronti.
```kt
viewModelScope.launch {
  combine(
      snapshotFlow { browseViewModel.items.value.isNotEmpty() },
      snapshotFlow { eventViewModel.events.isNotEmpty() }
  ) { browseReady, eventsReady ->
      browseReady && eventsReady
  }
      .filter { it }
      .first()
  loadPostedMatches()
  loadInterestedMatches()
}
```
Viene avviata una coroutine legata al ciclo di vita di `TradeMatchesViewModel`.
All'interno di essa, gli stati osservabili dei `ViewModel` dipendenti (`browseViewModel` ed `eventViewModel`) vengono convertiti in `Flow`, affinché si possano osservare le variazioni dei loro valori.
Le due `Flow` vengono combinate tramite l'operatore `combine`, in modo da produrre un unico flusso che emette un valore booleano ogni volta che uno dei due stati cambia. Il valore emesso è `true` solo quando entrambi i `ViewModel` hanno completato il caricamento dei propri dati.
Infine si filtrano solo i valori `true`, e il metodo `first()` sospende la coroutine fino alla prima emissione valida.
Quando entrambi i `ViewModel` hanno terminato il caricamento, la coroutine riprende l'esecuzione ed esegue le funzioni `loadPostedMatches()` e `loadInterestedMatches()`, il calcolo delle combinazioni è delegato a `TradeHandler`.\

Per quanto riguarda ricavare gli utenti interessati ai propri capi si eseguono i seguenti passi:
+ da Firebase, seleziona tutti i `trade_interests` associati ad oggetti che l'utente possiede;
+ mappa l'id di un item a una liste di id di utenti interessati, usata per fare controlli più veloci (`tradeMap`);
+ ottieni una lista di utenti interessati a oggetti che l'utente corrente possiede, rimuovendo duplicati;
+ mappa le coppie `<userID, tournamentID>` a un booleano per indicare se un utente parteciperà a un certo torneo;
+ per ogni oggetto pubblicato, recupera gli utenti interessati dalla `tradeMap`;
+ per ogni torneo a cui l'utente partecipa, conta quanti interessati partecipano a quel torneo;
+ se c'è ne è almeno 1, aggiungi quel torneo e il numero di interessati ai valori da restituire;

Per ricavare i tornei in cui saranno presenti i proprietari dei capi per cui si ha espresso interesse di scambio si eseguono passaggi simili:
+ si ottengono i `trade_interests` dell'utente da Firebase;
+ si estraggono gli id degli oggetti interessati;
+ controllo aggiuntivo per ogni _item_ per assicurarsi che esista ancora;
+ selezione tornei a cui partecipa l'utente in una mappa `<tournamentID, boolean>`;
+ preparazione di una mappa `<<ownerId, tournamentId>, Boolean>` (`attendanceMap`) che indica se un proprietario sarà presente o meno ad un torneo specifico;
+ per ogni proprietario:
    + si recuperano i tornei a cui esso partecipa;
    + si aggiorna `attendanceMap` per ogni torneo di quell'utente;
+ per ogni oggetto interessato:
    + controlla ogni torneo a cui l'utente partecipa;
    + verifica se il proprietario dell'_item_ sarà presente;
    + in caso positivo, recupera i dettagli del torneo e aggiungili a quelli da restituire.

== Manual Trade
La sezione _manual trade_ permette di rimuovere rapidamente uno o più _item_ dal proprio guardaroba, e aggiungerne altri in un'ipotetica situazione di scambio. Si potranno selezionare gli item da dare via, e quelli ricevuti. Inoltre è possibile scattare una foto e selezionare l'evento presso cui è stato fatto lo scambio tra una lista dei 3 più vicini per località.\
Questo tipo di scambio è a quello rapido, che verrà mostrato in seguito, se l'utente con cui si sta scambiando non ha l'app.

#double-img("manual_trade1", "manual_trade2")
La maglia con il bordo azzurro è l'item che ho deciso di dare via, i due pantaloni sono gli item che sto ricevendo. Scorrendo in basso è possibile scattare una foto del momento, e selezionare il torneo dove è stato fatto lo scambio.
Per ottenere i 3 eventi più vicini è abbastanza facile, dato che l'api fornisce latitudine e longitudine di quasi tutti i tornei.
+ dalla lista degli eventi si eliminano quelli senza latitudine e longitudine;
+ per ogni torneo:
    + prepara calcola la distanza tra la posizione attuale e il torneo;
    + crea una coppia `<tournament, distance>`.
+ ordina le coppie in base alla distanza;
+ prendi le prime 3.
Dato che la distanza viene calcolata in metri, nella card viene formattata per mostrare i kilometri.

== Quick Trade
Lo scambio rapido può essere svolto tra utenti che possiedono l'app. Si crea una sessione di scambio che per entrambi permette di selezionare gli _item_ che si stanno dando via e quelli che si stanno ricevendo.\
Un utente inizierà la sessione di scambio, generando un _qrcode_. L'altro potrà usare uno scanner per inquadrare il _qrcode_ e dare via alla sessione di scambio.
Prima di creare il _qrcode_, il `ViewModel` inserisce nel database una `trade_session`, caratterizzata da creatore, partecipanti, data di creazioni e un booleano che indica se è pronta, ovvero se il _qrcode_ è stato scannerizzato. Inizializzata a `false`. Verrà restituito anche un id corrispondente alla sessione iniziata, che verrà convertito in un _qrcode_.\
Per fare ciò si usa un metodo che converte la stringa in una matrice di bit, adottando il formato del _qrcode_. In seguito si itera per righe e per colonne su ogni bit di questa matrice, e dove si trova un bit `true` si disegna un pixel nero, altrimenti bianco.\
Per la scansione dei _qrcode_ viene utilizzato un blocco `LaunchedEffect`, che inizializza e configura la fotocamera tramite _CameraX_#footnote[Liberia per Android Jetpack che semplifica l'integrazione della fotocamera nelle app Android.]. Quest'ultima consente di visualizzare in tempo reale il flusso video all'interno di un componente PreviewView.
Attraverso il componente `ImageAnalysis`, ogni fotogramma catturato viene analizzato in tempo reale mediante la libreria _ML Kit_, che si occupa del riconoscimento dei _qrcode_.\
Quando l'analisi ha successo, _ML Kit_ restituisce una lista di potenziali codici. Ognuno di essi viene controllato e, se valido, viene chiamata la funzione `onQrScanned(it)`, dove `it` rappresenta la stringa decodificata dal _qrcode_.
Questa funzione funge da callback e invoca un metodo del `ViewModel` che consente all'utente che ha effettuato la scansione di entrare nella relativa sessione di scambio.

#double-img("quick_trade_qr_code", "quick_trade_scan")

#side-img("quick_trade_items")[
    Il `ViewModel` modifica il documento corrispondente per inserire l'utente nei membri della sessione e dare via alla sessione di scambio, dove si potrà vedere in tempo reale gli _item_ che entrambi le parti vogliono scambiare.

    Si possono selezionare dal proprio guardaroba gli item da dare, e quelli non dichiarati come scambiabili non saranno visibili. Si possono inoltre vedere gli item che si stanno ricevendo.\
    Quando entrambi confermano, gli item verranno trasferiti automaticamente nei corrispettivi nuovi armadi. Per finalizzare lo scambio, si imposta `confirmedBySender` o `confirmedByReceiver` nel database a true, quando entrambi sono veri si creano copie degli item scambiati, dove l'id dei proprietari vengono invertiti. In questo modo si ha un riferimento all oggetto scambiato associato a se, anche l'utente a cui è stato dato lo scambia nuovamente o elimina.\
    I dati relativi allo scambio vengono caricati una sola volta sul database, sarà poi il `TradeHandler` a determinare quali oggetti mostrare in base all'id dell'utente.
]

== Storico Scambi
#side-img("trade_history")[
    Qui si possono vedere gli scambi fatti in passato, ordinati in ordine cronologico a partire dal più recente. Per ogni scambio si può vedere la data, torneo, la foto scattata e gli oggetti ceduti e ricevuti.\
    Il `TradeHandler` ottiene una lista di scambi, dove l'utente corrente è o `userA` o `userB`. Per ognuno di questi, si crea un oggetto `Trade`. Questo insieme di liste viene usato dal `TradeViewModel` per avere accesso nella view _Trades_, indipendentemente dallo stato della pagina, agli scambi passati.
]
= Notifiche
L'app può inviare 3 tipi di notifiche periodicamente. Nella `MainActivity` viene creato il canale usato per la ricezione delle notifiche.
== Quando è stato aggiunto un nuovo torneo
Sempre nella `MainActivity` viene definito il worker che controlla ogni ora se ci sono nuovi tornei.
```kt
val tournamentCheckRequest = PeriodicWorkRequestBuilder<TournamentCheckWorker>(
  1, TimeUnit.HOURS
).build()

WorkManager.getInstance(this).enqueueUniquePeriodicWork(
  "tournament_check",
  ExistingPeriodicWorkPolicy.KEEP,
  tournamentCheckRequest
)
```
#side-img("notif_new_tournaments")[
    Dato che i `ViewModel` sono collegati al lifecycle dei _composable_, non possono essere usati per queste operazioni in background. Nel metodo `doWork()` del `CoroutineWorker` associato a questo lavoro, si eseguono i seguenti passi:
]
+ carica i dati salvati nel work precedente nella _preferences_ (_storage_ XML che contiene coppie chiave-valore);
+ tra questi, seleziona gli id dei tornei salvati;
+ esegui la chiamata all'API;
+ per ogni evento ed edizione:
    + se inizia in un giorno dopo oggi;
    + aggiungi il suo id a quelli da salvare
    + salva in una lista i dati di quel torneo (salvati in un oggetto `TournamentUI`, già usato nella view `Events`)
+ se questa lista non è vuota, invia una notifica con i nomi di questi tornei;
+ salva la _preference_ con gli id dei nuovi tornei,

== Quando un post raggiunge i 5 like
#align(center, img("notif_popular_post"))
Come per la notifica precedente, il controllo periodico è fatto da un `Worker`. Nel suo metodo `doWork()`:
+ carica tutti i post dell'utente e li converte in oggetti;
+ se il post ha un numero di like $>=$ a 5, e la notifica associata a questo post non è stata inviata
    + invia la notifica;
    + imposta il campo `notification` a true per indicare che la notifica è già stata mandata
== Giorni mancanti ai prossimi 3 tornei a cui si partecipa
#side-img(
    "notif_upcoming_tournaments",
)[
    Ogni volta che un utente dichiara la sua presenza ad un torneo dalla sezione Events, viene chiamato il metodo metodo `NotificationScheduler.scheduleDailyReminder(context)`, che dopo un delay per assicurarsi che inizi a mezzogiorno, avvia un `Worker` che ogni giorno informa l'utente di quanti giorni mancano ai suoi 3 tornei più vicini.
]
Nel metodo `doWork()`, si invoca l'API per ottenere la lista dei tornei, i dati vengono convertiti in una lista di tornei, ignorando quelli passati, e si selezionano quelli a cui l'utente ha dichiarato di partecipare. Tra questi, si calcola la differenza tra il giorno attuale e il giorno in cui essi iniziano, e in base a questo valore si ordina la lista. Infine si selezionano i nomi dei primi 3 tornei, con associati i giorni che mancano. Questi sono i valori che verranno comunicati dalla notifica.
