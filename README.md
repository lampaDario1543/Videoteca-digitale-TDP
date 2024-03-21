# Videoteca digitale (TDP):

## Introduzione:

Programma per la gestione digitale di una videoteca in cui sono presenti diverse tipoligie di risorse (film e serie TV), in cui un utente viene "registrato", e può ottenere le informazioni delle varie risorse, cercarle o noleggiarle.

**La directory comprende:**

* Il database che contiene le risorse e le loro informazioni (videoteca.xml)
* Il database degli utenti con i loro noleggi (noleggi.xml)
* L'XML Shema per la validazione di videoteca.xml (videoteca.xsd)
* L'XML Schema per la validazione di noleggi.xml (noleggi.xsd)
* Il codice del server che è di tipo TCP e multi-client (./240229_videoteca_server/)
* Il codice del client (./240229_videoteca_client/)
* La directory "./poster/", che contiene i poster delle risorse

## Come si usa:

Per far partire il programma bisogna per prima cosa avviare il server, e poi il client. Il server rimane in ascolto di richieste da parte dei client, e quando un client si connette, il server crea un thread per gestire la richiesta del client. Il client può fare diverse richieste, come:

| Richiesta        | Risposta                                                                                                                        | Cosa fa                                                                                                                                                                                                                                                                                              |
| ---------------- | ------------------------------------------------------------------------------------------------------------------------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `INIT`         | Risponde inviando la lista degli id delle serie separate da ',', la lista dei film separati da ',' ed il saldo dell'utente | Inizializza la connessione con il client, controllando se è già presente nel database dei clienti (noleggi.xml), altrimenti lo aggiunge, invia al client la lista                                                                                                                       |
| `END`          | -                                                                                                                               | Termina la connessione del socket con il client, perciò chiude tutti gli oggetti del server.                                                                                                                                                                                                   |
| `GET_IMAGE:id` | Restituisce un array di byte (l'immagine)                                                                                       | Invia al cliente un'immagine sottoforma di array di byte, l'immagine è presente nella cartella dei poster e come nome ha l'id della risorsa (id_risorsa.jpg). Quando viene inviato questo comando il client deve specificare l'id dell'immagine che vuole ottenere dopo i ':'. |
| `GET_INFO:id`  | Restituisce un file xml                                                                                                         | Ritorna le informazioni di una determinata risorsa, che sia un film o una serie, sottoforma di xml. Id come parametro.                                                                                                                                                                          |
| `SEARCH:query` | Restituisce una stringa (una lista di id di risorse che presentano la query)                                                    | Ritorna la lista dei film o delle serie TV che hanno tra gli attori, o come titolo, o come titolo di un episodio la query richiesta come parametro.                                                                                                                                        |
| `IS_RENT:id`   | Restituisce boolean (`true `o `false`)                                                                                      | Verifica se l'utente che ha mandato il comando ha già noleggiato oppure no la risorsa che ha l'id inviato come parametro, se è già noleggiato invia `true `altrimenti invia `false`.                                                                                                |
| `RENT:id`      | Restituisce boolean (`true `o `false`)                                                                                      | Permette all'utente di noleggiare la risorsa che come id ha il parametro inviato col comando, se il noleggio va a buon fine  ritorna `true` altrimenti `false`.                                                                                                                       |
| `GET_RENTED`   | Restituisce una stringa (la lista degli id delle risorse noleggiate dall'utente)                                                | Ritorna la lista degli id delle risorse noleggiate dall'utente, separate da ','.                                                                                                                                                                                                                |
| `GET_SALDO`    | Ritorna un interno                                                                                                              | Ritorna il saldo dell'utente che ha inviato il comando.                                                                                                                                                                                                                                              |

## Licenza:

MIT License
Copyright © 2024 Tommaso Malinverno
Per maggiori informazioni [qui](./licenza.txt)

## Copyright:

Copyright © 2024 Tommaso Malinverno
