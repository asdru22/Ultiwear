#import "@preview/codly:1.3.0": *

#show link: it => underline(text(fill: blue, it))

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

#pagebreak()
#set page(numbering: "1")
#set heading(numbering: "1.1")

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
)[In base a questi stati si mostreranno 3 _view_ diverse. Dopo aver eseguito l'accesso con Google, l'utente potrà utilizzare l'app.
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
Il token di login sarà poi convertito in un token Firebase, memorizzato in una collezione come UID assieme alla mail dell'utente. Ora l'utente ha accesso all'app, e potrà navigare le sue 5 _view_.\
`var selectedIndex by rememberSaveable { mutableIntStateOf(0) }` tiene traccia della _sub-activity_ corrente. Ognuna di esse è definita dal _composable_ `TabItem`.
```kt
val tabs = listOf(
  TabItem(
      "Wardrobe",
      R.drawable.wardrobe
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
= Eventi/Upcoming Events
Qui si possono vedere i prossimi tornei da tutto il mondo, ottenuti tramite l'api di #link("https://ultical.com/")[Ultical]. Sono _card_ ordinate dalla data di inizio più vicina alla più lontana. Ogni card ha associata una _checkbox_ per indicare se si sarà presenti ad un certo torneo.
#side-img("events")[Il singleton `APIClient` inizializza una sola volta (tramite `by lazy`) l'interfaccia `UlticalAPI` fornendogli l'url a cui fare la chiamata HTTP, e una factory da usare per processare i dati. In questo caso si usa la libreria GSON di Google in quanto l'API di ultical restituisce un oggetto JSON.
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

