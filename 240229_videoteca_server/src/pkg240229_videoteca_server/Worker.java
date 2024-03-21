package pkg240229_videoteca_server;

import java.net.*;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import javax.xml.parsers.*;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.*;
import org.xml.sax.SAXException;

public class Worker extends Thread {

    private Socket s;
    private int ctrClient;
    private static Semaforo semNoleggi; // semaforo per la scrittura su file di noleggi.xml

    public Worker(Socket s, int ctr, Semaforo semNoleggi) {
        this.s = null;
        this.s = s;
        this.ctrClient = ctr;
        this.semNoleggi = semNoleggi;
    }

    @Override
    public void run() {
        InetAddress addr = null; // indirizzo del client
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance(); // factory per xml
        int port = 0; // porta del client
        try {
            addr = InetAddress.getLocalHost(); // ottengo l'indirizzo del client
            port = s.getLocalPort(); // ottengo la porta del client
        } catch (UnknownHostException ex) {
            System.out.println("[ERROR] Host sconosciuto");
        }
        try {
            DocumentBuilder parser = factory.newDocumentBuilder(); // parser xml
            Document doc = parser.parse(new File("../videoteca.xml")); // carico il file xml
            Element root = doc.getDocumentElement(); // ottengo l'elemento radice
            // oggetti scrittura lettura del socket
            InputStream is = s.getInputStream();
            OutputStream os = s.getOutputStream();
            PrintWriter out = new PrintWriter(os, true); // metto l'auto flush attivo
            BufferedReader in = new BufferedReader(new InputStreamReader(is)); // oggetto per leggere il canale socket
            // cliente in formato stringa composto da indirizzo:porta
            String cliente = addr + ":" + port;
            // mando il messaggio di benvenuto al client
            out.println("Benvenuto " + cliente + " sei il client " + ctrClient);
            String msgServer = "";
            String msgClient = "";
            // variabile per uscire dal ciclo
            boolean exit = false;
            while (((msgClient = in.readLine()) != null) && !exit) { // leggo i messaggi del client
                // azione e parametro che mi invia il client
                String action = "";
                String par = "";
                // ottengo azione e paramtro dal messaggio del client dividendo il messaggio che
                // ha inviato il client
                action = msgClient.split(":")[0];
                par = msgClient.split(":")[1];
                System.out.println("[CLIENT " + ctrClient + "] " + msgClient);
                switch (action) { // analizzo che azione vuole fare il client
                    // se il client invia END
                    case "END":
                        exit = true; // imposto exit a true, perciò esce dal ciclo
                        System.out.println("[INFO] CLIENT " + ctrClient + " chiede di uscire");
                        break;
                    // se il client chiede INIT --> inizializzazione
                    case "INIT":
                        // saldo iniziale per i nuovi client
                        int saldo = 50;
                        System.out.println("[INFO] CLIENT " + ctrClient + " chiede l'inizializzazione");
                        System.out.println("[INFO] Controllo se il cliente è già presente nella lista dei clienti");
                        // apro il file noleggi.xml
                        File noleggiFile = new File("../noleggi.xml");
                        Document noleggi = parser.parse(noleggiFile);
                        Element rootNoleggi = noleggi.getDocumentElement();
                        // ottengo tutit i clienti all'interno del file noleggi.xml
                        NodeList clienti = rootNoleggi.getElementsByTagName("cliente");
                        // verifico se il cliente è già presente
                        boolean thereIsCustomer = false;
                        // scorro ogni cliente
                        for (int i = 0; i < clienti.getLength(); i++) {
                            Element clienteEl = (Element) clienti.item(i);
                            // ottengo l'attributo user per ogni cliente
                            String user = clienteEl.getAttribute("user");
                            // se l'attibuto user e uguale al cliente che ha inviato il messaggio
                            if (user.equalsIgnoreCase(cliente)) {
                                // allora il cliente è già presente nella raccolta dati dei noleggi
                                // quindi imposto la variabile a true
                                thereIsCustomer = true;
                                // ottengo il saldo del cliente
                                NodeList saldoNode = clienteEl.getElementsByTagName("saldo");
                                try {
                                    // converto il saldo del cliente in int
                                    saldo = Integer.parseInt(saldoNode.item(0).getTextContent());
                                } catch (Exception e) {
                                    System.out.println("[ERROR] Errore conversione stringa in intero (saldo)");
                                }
                            }
                        }
                        // se il cliente non è presente in noleggi.xml
                        if (!thereIsCustomer) {
                            System.out.println("[INFO] Il cliente non è presente, lo aggiungo");
                            // creo l'elemento cliente
                            Element clienteEl = noleggi.createElement("cliente");
                            // gli imposto l'attributo user con il valore del cliente
                            clienteEl.setAttribute("user", cliente);
                            // creo l'elemento saldo
                            Element saldoEl = noleggi.createElement("saldo");
                            // imposto il saldo del cliente con default=50
                            saldoEl.appendChild(noleggi.createTextNode(String.valueOf(saldo)));
                            // aggiungo il saldo all'elemento cliente
                            clienteEl.appendChild(saldoEl);
                            // aggiungo il cliente al file noleggi.xml
                            rootNoleggi.appendChild(clienteEl);
                            // controllo tramite semaforo se qualcuno sta già modificando il file
                            // noleggi.xml, nel caso aspetto
                            semNoleggi.aquire();
                            // oggetti per modificare noleggi.xml
                            TransformerFactory tf = TransformerFactory.newInstance();
                            Transformer transformer = tf.newTransformer();
                            DOMSource source = new DOMSource(noleggi);
                            StreamResult result = new StreamResult(noleggiFile);
                            // aggiorno il file noleggi.xml
                            transformer.transform(source, result);
                            // rilascio il semaforo
                            semNoleggi.release();
                            System.out.println("[INFO] Cliente aggiunto con successo.");
                        } else {
                            System.out.println("[INFO] Il cliente è già presente nella lista");
                        }
                        // ottengo tutti i film e le serie
                        NodeList series = root.getElementsByTagName("serie");
                        NodeList movies = root.getElementsByTagName("movie");
                        // inizializzo le liste di film e serie
                        String seriesList = "";
                        String moviesList = "";
                        // prendo l'id per ogni serie e la aggiungo alla mia lista.
                        for (int i = 0; i < series.getLength(); i++) {
                            NamedNodeMap attrs = series.item(i).getAttributes();
                            if (i < series.getLength() - 1) {
                                seriesList += attrs.getNamedItem("id").getNodeValue() + ",";
                            } else {
                                seriesList += attrs.getNamedItem("id").getNodeValue();
                            }
                        }
                        // prendo l'id per ogni film e la aggiungo alla mia lista.
                        for (int i = 0; i < movies.getLength(); i++) {
                            NamedNodeMap attrs = movies.item(i).getAttributes();
                            if (i < movies.getLength() - 1) {
                                moviesList += attrs.getNamedItem("id").getNodeValue() + ",";
                            } else {
                                moviesList += attrs.getNamedItem("id").getNodeValue();
                            }
                        }
                        // invio la lista delle serie, poi la lista dei film e infine il saldo
                        out.println(seriesList);
                        out.println(moviesList);
                        out.println(saldo);
                        break;
                    case "GET_IMAGE": // funzione per ottenere un'immagine richiesta tramite parametro
                        // apro l'immagine richiesta
                        File imageFile = new File("../poster/" + par + ".jpg");
                        // creo il buffer con la grandezza del file
                        byte[] buffer = new byte[(int) imageFile.length()];
                        FileInputStream fis = new FileInputStream(imageFile);
                        BufferedInputStream bis = new BufferedInputStream(fis);
                        bis.mark((int) imageFile.length()); // Segna la posizione corrente nel file
                        bis.read(buffer, 0, buffer.length);
                        // invio la lunghezza del buffer
                        out.println(buffer.length);
                        // Riposiziona il cursore all'inizio del file
                        bis.reset();
                        System.out.println("[INFO] Invio di " + par + ".jpg a CLIENT " + ctrClient);
                        int bytesRead;
                        // leggo il buffer e lo invio al client
                        while ((bytesRead = bis.read(buffer)) != -1) {
                            os.write(buffer, 0, bytesRead);
                        }
                        System.out.println("[INFO] Invio riuscito di " + par + ".jpg a CLIENT " + ctrClient);
                        break;
                    // se il client chiede GET_INFO:parametro
                    case "GET_INFO":
                        // cerco nel xml l'elemento che ha come attributo l'id par
                        NodeList catalogo = null;
                        if (par.contains("ser")) {
                            catalogo = root.getElementsByTagName("serie");
                        } else {
                            catalogo = root.getElementsByTagName("movie");
                        }
                        for (int i = 0; i < catalogo.getLength(); i++) {
                            NamedNodeMap attrs = catalogo.item(i).getAttributes();
                            if (attrs.getNamedItem("id").getNodeValue().equalsIgnoreCase(par)) { // se trovo l'id che mi
                                // ha richiesto il
                                // client
                                System.out.println("[INFO] Invio richiesta XML per richiesta " + msgClient
                                        + " a CLIENT " + ctrClient);
                                String xml = printElement((Element) catalogo.item(i));
                                // lo invio come xml al client
                                out.println(xml);
                                System.out.println("[INFO] Invio riuscito di XML per richiesta " + msgClient
                                        + " a CLIENT " + ctrClient);
                            }
                        }
                        break;
                    // se il client chiede SEARCH:parametro
                    case "SEARCH":
                        // cerco tra gli attori e tutti i titoli par
                        System.out.println("[INFO] CLIENT " + ctrClient + " chiede di cercare " + par);
                        // prendo tutti i titoli che ci sono di film, serie e episodi
                        NodeList titoli = root.getElementsByTagName("titolo");
                        // prendo tutti gli attori presenti nel file
                        NodeList attori = root.getElementsByTagName("attore");
                        // stringa che invio al client con i risultati del search
                        String searchResult = "";
                        for (int i = 0; i < titoli.getLength(); i++) {
                            // se il titolo contiene il parametro della ricerca ignorando il case, aggiungo
                            // alla lista searchResult l'id del padre
                            if (titoli.item(i).getTextContent().toLowerCase().contains(par.toLowerCase())) {
                                Node parentNode = titoli.item(i).getParentNode(); // ottengo il padre dell'elemento
                                if (parentNode.getNodeName().equals("episodio")) { // Se il nodo padre è un episodio,
                                    // prendo il nodo padre di
                                    // quest'ultimo
                                    Node stag = parentNode.getParentNode();
                                    Node serieNode = stag.getParentNode();
                                    if (serieNode instanceof Element) {
                                        Element serieElement = (Element) serieNode;
                                        String id = serieElement.getAttribute("id");
                                        searchResult += id + ",";
                                    }
                                } else {
                                    if (parentNode instanceof Element) {
                                        Element parentElement = (Element) parentNode;
                                        String id = parentElement.getAttribute("id");
                                        searchResult += id + ",";
                                    }
                                }
                            }
                        }
                        // cerco negli attori il paramtro di ricerca
                        for (int i = 0; i < attori.getLength(); i++) {
                            // se trovo il parametro all'interno di uno degli attori lo aggiungo alla lista
                            if (attori.item(i).getTextContent().toLowerCase().contains(par.toLowerCase())) {
                                Node parentNode = attori.item(i).getParentNode();
                                Node parentParent = parentNode.getParentNode();
                                if (parentParent instanceof Element) {
                                    Element parentParentEl = (Element) parentParent;
                                    String id = parentParentEl.getAttribute("id");
                                    searchResult += id + ",";
                                }
                            }
                        }
                        // tolgo l'ultima virgola.
                        if (searchResult.length() > 0) {
                            searchResult = searchResult.substring(0, searchResult.length() - 1);
                        }
                        // invio al client
                        out.println(searchResult);
                        System.out.println("[INFO] Invio risultato ricerca a CLIENT " + ctrClient);
                        break;
                    // se il client chiede IS_RENT:id
                    case "IS_RENT":
                        // vuole sapere se l'elemento con id il parametro è stato noleggiato
                        System.out.println("[INFO] CLIENT " + ctrClient + " chiede se ha noleggiato " + par);
                        // bool per sapere se ho trovato l'elemento
                        boolean find = false;
                        // apro il file noleggi.xml
                        noleggiFile = new File("../noleggi.xml");
                        // creo il documento
                        noleggi = parser.parse(noleggiFile);
                        // prendo la root
                        rootNoleggi = noleggi.getDocumentElement();
                        // controllo se i noleggi di tutti sono scaduti o no, se lo sono elimino il
                        // noleggio:
                        deleteExpiredRent(noleggi, noleggiFile);
                        // prendo tutti i clienti del file noleggi.xml
                        clienti = rootNoleggi.getElementsByTagName("cliente");
                        for (int i = 0; i < clienti.getLength(); i++) {
                            Element clienteEl = (Element) clienti.item(i);
                            String id = clienteEl.getAttribute("user"); // ottengo l'attributo user
                            if (id.equalsIgnoreCase(cliente)) { // se lo user corrisponde al cliente connesso
                                NodeList noleggiCliente = clienteEl.getElementsByTagName("noleggio"); // prendo tutti i
                                // noleggi del
                                // cliente
                                for (int j = 0; j < noleggiCliente.getLength(); j++) { // per ogni noleggio
                                    Element noleggio = (Element) noleggiCliente.item(j);
                                    NodeList titolo = noleggio.getElementsByTagName("id"); // prendo l'id della risorsa
                                    // noleggiata
                                    if (titolo.item(0).getTextContent().equalsIgnoreCase(par)) { // controllo se l'id è
                                        // uguale al parametro
                                        find = true; // nel caso vuol dire che il cliente ha già noleggiato il titolo
                                    }
                                }
                            }
                        }
                        // in base al fatto se ho trovato o no il parametro richiesto invio true o false
                        if (find) {
                            out.println("true");
                        } else {
                            out.println("false");
                        }
                        break;
                    // funzione che permette al client di noleggiare una risorsa RENT:id_risorsa
                    case "RENT":
                        System.out.println("[INFO] CLIENT " + ctrClient + " chiede di noleggiare " + par);
                        // apro noleggi.xml
                        noleggiFile = new File("../noleggi.xml");
                        noleggi = parser.parse(noleggiFile);
                        rootNoleggi = noleggi.getDocumentElement();
                        // ottengo il saldo del cliente
                        clienti = rootNoleggi.getElementsByTagName("cliente");
                        saldo = 0;
                        for (int i = 0; i < clienti.getLength(); i++) {
                            Element clienteEl = (Element) clienti.item(i);
                            String id = clienteEl.getAttribute("user"); // ottengo l'attributo user
                            if (id.equalsIgnoreCase(cliente)) { // se corrisponde al cliente connesso
                                NodeList saldoNode = clienteEl.getElementsByTagName("saldo"); // prendo il saldo
                                try {
                                    // converto il saldo in int
                                    saldo = Integer.parseInt(saldoNode.item(0).getTextContent());

                                } catch (Exception e) {
                                    System.out.println("[ERROR] Errore conversione stringa in intero (saldo)");
                                }
                            }
                        }
                        // ottengo il costo del titolo richiesto
                        NodeList costi = root.getElementsByTagName("costo");
                        int costo = 0;
                        for (int i = 0; i < costi.getLength(); i++) {
                            Element parent = (Element) costi.item(i).getParentNode();
                            String id = parent.getAttribute("id"); // ottengo l'attributo id
                            if (id.equalsIgnoreCase(par)) { //se l'id corrispnde al parametro (il titolo da noleggiare)
                                try {
                                    costo = Integer.parseInt(costi.item(i).getTextContent()); // converto il costo in int
                                } catch (Exception e) {
                                    System.out.println("[ERROR] Errore conversione stringa in intero (costo)");
                                }
                            }
                        }
                        //se l'operazione non si può fare perché va in negativo
                        if (saldo - costo < 0) {
                            out.println("false"); //invio false
                        } else {
                            //altrimenti creo un nuovo elemento noleggio con l'id che è il par
                            Element noleggioElement = noleggi.createElement("noleggio");
                            Element idElement = noleggi.createElement("id");
                            idElement.appendChild(noleggi.createTextNode(par));
                            noleggioElement.appendChild(idElement);
                            //creo la scadenza
                            Element scadenzaElement = noleggi.createElement("scadenza");
                            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
                            Calendar cal = Calendar.getInstance();
                            if (!par.contains("ser")) { // se non è una serie
                                cal.add(Calendar.DATE, 30); // Aggiungi 30 giorni alla data attuale
                            } else {
                                cal.add(Calendar.DATE, 60); // Aggiungi 60 giorni alla data attuale
                            }
                            Date dataScadenza = cal.getTime();
                            String formattedDate = sdf.format(dataScadenza); //formatto la data
                            //aggiungo la scadenza al elemento scadenza
                            scadenzaElement.appendChild(noleggi.createTextNode(formattedDate));
                            noleggioElement.appendChild(scadenzaElement);
                            //ottengo tutti i clienti del file
                            clienti = rootNoleggi.getElementsByTagName("cliente");
                            // aggiorno il saldo
                            for (int i = 0; i < clienti.getLength(); i++) {
                                Element clienteEl = (Element) clienti.item(i);
                                if (clienteEl.getAttribute("user").equalsIgnoreCase(cliente)) {
                                    clienteEl.appendChild(noleggioElement);
                                    NodeList saldoNode = clienteEl.getElementsByTagName("saldo");
                                    saldoNode.item(0).setTextContent(String.valueOf(saldo - costo));
                                }
                            }
                            //controllo se qualcuno sta già modificando il file noleggi.xml
                            semNoleggi.aquire();
                            //aggiorno il file noleggi.xml
                            TransformerFactory tf = TransformerFactory.newInstance();
                            Transformer transformer = tf.newTransformer();
                            DOMSource source = new DOMSource(noleggi);
                            StreamResult result = new StreamResult(noleggiFile);
                            transformer.transform(source, result);
                            //rilascio il semaforo
                            semNoleggi.release();
                            // noleggio avvenuto con successo, mando true
                            out.println("true");
                        }
                        break;
                    //Funzione che richiede tutti gli elementi noleggiati dal cliente che manda la richiesta
                    case "GET_RENTED": // ottiene i titoli noleggiati per un determinato cliente
                        // apro noleggi.xml
                        noleggiFile = new File("../noleggi.xml");
                        noleggi = parser.parse(noleggiFile);
                        rootNoleggi = noleggi.getDocumentElement();
                        //controllo le scadenze dei noleggi, se sono scaduti li elimino
                        deleteExpiredRent(noleggi, noleggiFile);
                        //ottengo tutti i clienti
                        clienti = rootNoleggi.getElementsByTagName("cliente");
                        //xml di risposta che mando al client
                        String xml = "<richiesta>";
                        for (int i = 0; i < clienti.getLength(); i++) { //per ogni cliente cerco il cliente che ha mandato la richiesta
                            Element clienteEl = (Element) clienti.item(i);
                            String id = clienteEl.getAttribute("user"); // prendo l'attributo user
                            if (id.equalsIgnoreCase(cliente)) { // se corrisponde al cliente che ha mandato la richiesta
                                NodeList noleggiCliente = clienteEl.getElementsByTagName("noleggio"); //prendo tutti i noleggi del cliente
                                for (int j = 0; j < noleggiCliente.getLength(); j++) {
                                    Element noleggio = (Element) noleggiCliente.item(j); // per ogni noleggio 
                                    NodeList titolo = noleggio.getElementsByTagName("id"); //ottengo l'id del titolo
                                    NodeList scadenza = noleggio.getElementsByTagName("scadenza"); //ottengo la scadenza
                                    // li metto nella stringa dell'xml
                                    xml += "<noleggio>";
                                    xml += "<id>" + titolo.item(0).getTextContent() + "</id>";
                                    xml += "<scadenza>" + scadenza.item(0).getTextContent() + "</scadenza>";
                                    xml += "</noleggio>";
                                }
                            }
                        }
                        xml += "</richiesta>";
                        //invio l'xml della richiesta al client
                        out.println(xml);
                        break;
                    //funzione che restituisce il saldo di un cliente:
                    case "GET_SALDO":
                        // apro noleggi.xml
                        noleggiFile = new File("../noleggi.xml");
                        noleggi = parser.parse(noleggiFile);
                        rootNoleggi = noleggi.getDocumentElement();
                        clienti = rootNoleggi.getElementsByTagName("cliente");
                        //ottengo il saldo del cliente connesso
                        for (int i = 0; i < clienti.getLength(); i++) {
                            Element clienteEl = (Element) clienti.item(i);
                            String id = clienteEl.getAttribute("user");
                            if (id.equalsIgnoreCase(cliente)) {
                                NodeList saldoNode = clienteEl.getElementsByTagName("saldo");
                                //invio il saldo del cliente al client
                                out.println(saldoNode.item(0).getTextContent());
                            }
                        }
                        break;
                    default: // se il client invia un comando non valido
                        System.out.println("[ERROR] Il client ha inviato un comando non valido.");
                        break;
                }
                if (exit) { // se exit=true esco sal ciclo
                    System.out.println("[INFO] CLIENT " + ctrClient + " sta uscendo");
                    break;
                }
            }
            // chiudo gli oggetti.
            out.close();
            in.close();
            is.close();
            os.close();
            s.close();
            System.out.println("[INFO] CLIENT " + ctrClient + " e' uscito");
        } catch (IOException ex) {
            System.out.println("[ERROR] Errore IO");
        } catch (ParserConfigurationException ex) {
            System.out.println("[ERROR] Errore nell'inizializzazione parser");
        } catch (SAXException ex) {
            System.out.println("[ERROR] Errore sax");
        } catch (TransformerConfigurationException ex) {
            System.out.println("[ERROR] Errore nell'update di noleggi.xml");
        } catch (TransformerException ex) {
            System.out.println("[ERROR] Errore nell'update di noleggi.xml");
        } catch (InterruptedException ex) {
            System.out.println("[ERROR] Interruzione causa semaforo");
        }
    }

    //funzione per stampare un elemento xml
    public static String printElement(Element el) {
        StringBuilder xml = new StringBuilder();
        xml.append("<").append(el.getTagName());

        // Aggiungi gli attributi
        if (el.hasAttributes()) {
            for (int i = 0; i < el.getAttributes().getLength(); i++) {
                Node attribute = el.getAttributes().item(i);
                xml.append(" ").append(attribute.getNodeName()).append("=\"").append(attribute.getNodeValue())
                        .append("\"");
            }
        }

        xml.append(">");
        // Ottieni i nodi figli
        NodeList children = el.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node node = children.item(i);
            if (node instanceof Element) {
                xml.append(printElement((Element) node));
            } else if (node instanceof Text) {
                // Se il nodo è testo, aggiungi il contenuto
                String text = ((Text) node).getTextContent().trim();
                if (!text.isEmpty()) {
                    xml.append(text);
                }
            }
        }
        // Aggiungi la chiusura dell'elemento
        xml.append("</").append(el.getTagName()).append(">");
        return xml.toString();
    }

    //funzione per eliminare i noleggi scaduti
    public static void deleteExpiredRent(Document doc, File f) {
        Element root = doc.getDocumentElement(); // ottengo l'elemento radice
        NodeList noleggi = root.getElementsByTagName("noleggio"); //ottengo tutti i noleggi
        for (int i = noleggi.getLength() - 1; i >= 0; i--) { //faccio il ciclo al contrario perché devo eliminare degli elementi, e la lunghezza si aggiorna dinamicamente
            Element noleggio = (Element) noleggi.item(i);
            //ottengo la scadenza
            NodeList scadenza = noleggio.getElementsByTagName("scadenza");
            String scadenzaString = scadenza.item(0).getTextContent();
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
            try {
                Date scadenzaDate = sdf.parse(scadenzaString);
                Date now = new Date();
                if (now.after(scadenzaDate)) { // se la data di adesso e dopo la scadenza
                    //rimuovo il noleggio
                    noleggio.getParentNode().removeChild(noleggio);
                }
            } catch (Exception e) {
                System.out.println("[ERROR] Errore nella conversione della data");
            }
        }
        try {
            //controllo se qualcuno sta già modificando il file noleggi.xml
            semNoleggi.aquire();
            //aggiorno il file noleggi.xml
            TransformerFactory tf = TransformerFactory.newInstance();
            Transformer transformer = tf.newTransformer();
            DOMSource source = new DOMSource(doc);
            StreamResult result = new StreamResult(f);
            transformer.transform(source, result);
        } catch (InterruptedException ex) {
            System.out.println("[ERROR] Il thread si è interrotto in deleteExpiredRent");
        } catch (TransformerException ex) {
            System.out.println("[ERROR] Errore nel salvare il nuovo file xml");
        }
        //rilascio il semaforo
        semNoleggi.release();
    }
}
