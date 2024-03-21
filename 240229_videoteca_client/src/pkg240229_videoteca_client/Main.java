package pkg240229_videoteca_client;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.net.*;
import java.io.*;
import java.util.HashSet;
import java.util.Set;
import javax.swing.*;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class Main extends JFrame {

    // porta del server
    private static final int PORT = 12345;
    private static JButton[] filmButtons;
    private static JButton[] seriesButtons;
    // Pannelli che uso nella GUI
    private static JPanel filmPanel = null; // pannello con la lista dei film
    private static JPanel mainPanel = new JPanel(); // pannello principale
    private static JPanel seriesPanel = new JPanel(); // pannello con la lista delle serie
    private static JPanel searchPanel = new JPanel(); // pannello per la ricerca
    private static JPanel episodePanel = null; // pannello che contiene info dell'episodio
    private static JPanel searchResultPanel = null; // pannello con i risultati della ricerca
    private static JPanel infoPanel = null; // pannello con le informazioni della risorsa
    private static JPanel collectionPanel; // pannello con i contenuti noleggiati
    private static JLabel saldoLabel = new JLabel("Saldo: 0€"); // etichetta con il saldo dell'utente
    private static JScrollPane searchResultScrollPanel = null; // scroll panel per i risultati della ricerca
    private static JScrollPane filmScrollPanel; // scroll panel per i film
    private static JScrollPane seriesScrollPanel; // scroll panel per le serie
    // dimensioni dei poster croppati
    private static int posterWidth = 125;
    private static int posterHeight = 200;

    public Main() { // costruttore per la finestra perché eredito JFrame
        super();
        setTitle("Videoteca digitale"); // imposto il titolo della finestra
        setSize(1000, 500); // imposto la dimensione iniziale della finestra
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); // imposto la chiusura della finestra quando si preme x
        setResizable(true); // imposto che si può ridimensionare la finestra
    }

    public static void main(String[] args) {
        Main fin = new Main(); // creo la finestra
        mainPanel.setLayout(new GridBagLayout());
        // imposto il layout del pannello principale
        GridBagConstraints gbcMain = new GridBagConstraints();
        gbcMain.insets = new Insets(5, 5, 5, 5);
        // creo i pannelli per i film e le serie aggiungendo il bordo con il titolo
        filmPanel = new JPanel();
        filmPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
        filmPanel.setBorder(BorderFactory.createTitledBorder("Film")); // Titolo del pannello
        seriesPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
        seriesPanel.setBorder(BorderFactory.createTitledBorder("Serie TV")); // Titolo del pannello
        try {
            System.out.println("[INFO] In attessa di connessione...");
            Socket s = new Socket(InetAddress.getLocalHost(), PORT); // mi connetto
            System.out.println("[INFO] Connesso.");
            // oggetti per leggere ed inviare messaggi sul socket
            InputStream is = s.getInputStream();
            OutputStream os = s.getOutputStream();
            PrintWriter out = new PrintWriter(os, true); // metto l'auto flush attivo
            BufferedReader in = new BufferedReader(new InputStreamReader(is)); // oggetto per leggere il canale socket
            String msgServer = "";
            // aggiungo un listener alla finestra per chiudere la connessione quando si
            // preme x
            fin.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosing(WindowEvent e) {
                    try {
                        System.out.println("[INFO] chiedo di uscire");
                        // quando si preme x mando end al server
                        out.println("END: ");
                        // chiudo gli oggetti che uso per il socket
                        out.close();
                        in.close();
                        os.close();
                        is.close();
                        s.close();
                        System.out.println("[INFO] chiudo la connessione.");
                        fin.dispose(); // Chiude la finestra
                        System.out.println("[INFO] Finestra chiusa");
                        System.out.println("[INFO] termino il processo");
                        // termino il processo
                        System.exit(0);
                    } catch (IOException ex) {
                        System.out.println("Errore IO");
                    }
                }
            });
            // ottengo il messaggio di benvenuto dal server
            msgServer = in.readLine();
            System.out.println("[SERVER] " + msgServer);
            // chiedo al server di inizializzare la finestra.
            out.println("INIT: ");
            // ottengo le due liste: serie e film e il saldo che ho a disposizione
            String seriesList = in.readLine();
            String moviesList = in.readLine();
            String saldoStr = in.readLine();
            System.out.println("[SERVER] " + seriesList);
            System.out.println("[SERVER] " + moviesList);
            System.out.println("[SERVER] " + saldoStr);
            // salvo gli id dei film all'interno di un array di stringhe, separando la lista
            // del server in base alla virgola
            String[] movies = moviesList.split(",");
            // Creo un array di bottoni per i film, della lunghezza della lista che ha
            // mandato il server
            filmButtons = new JButton[movies.length];
            // Creazione dei bottoni dei film
            for (int i = 0; i < movies.length; i++) {
                // creo il bottone con la funzione
                JButton button = createButton(movies[i], in, is, out, fin, false);
                // aggiungo all'array
                filmButtons[i] = button;
                // aggiungo al pannello del film
                filmPanel.add(button);
            }
            // salvo gli id dei serie all'interno di un array di stringhe, separando la lista
            // del server in base alla virgola
            String[] series = seriesList.split(",");
            // Creo un array di bottoni per le serie, della lunghezza della lista che ha
            // mandato il server
            seriesButtons = new JButton[series.length];
            for (int i = 0; i < series.length; i++) {
                //creo i bottoni
                JButton button = createButton(series[i], in, is, out, fin, true);
                //aggiungo all'array
                seriesButtons[i] = button;
                seriesPanel.add(button);
            }
            // creo il pannello per la rierca
            JTextField searchField = new JTextField(25); //barra di ricerca
            JButton searchButton = new JButton("Cerca"); //bottone per cercare
            searchButton.addActionListener(new ActionListener() { //aggiungo un listener al bottone di ricerca
                @Override
                public void actionPerformed(ActionEvent e) {
                    String query = searchField.getText(); //prendo il testo contenuto nella barra di ricerca
                    searchField.setText(""); //imposto la barra di ricerca vuota
                    if (searchResultScrollPanel != null) { // se il pannello dei risultati è attivo
                        searchResultScrollPanel.setVisible(false); //lo rendo invisibile
                        mainPanel.remove(searchResultScrollPanel); //lo rimuovo dal pannello principale
                        mainPanel.revalidate(); //aggiorno il pannello principale
                        searchResultScrollPanel = null;
                    }
                    //stessa cosa del pannello di prima per infoPanel e collectionPanel
                    if (infoPanel != null) {
                        infoPanel.setVisible(false);
                        mainPanel.revalidate();
                    }
                    if (collectionPanel != null) {
                        collectionPanel.setVisible(false);
                        mainPanel.remove(collectionPanel);
                        mainPanel.revalidate();
                        collectionPanel = null;
                    }
                    //se la barra di ricerca è vuota
                    if (query.equals("")) {
                        //imposto il menu principale visibile
                        seriesScrollPanel.setVisible(true);
                        filmScrollPanel.setVisible(true);
                        mainPanel.revalidate();
                        //faccio comparire una finestra di errore che comunica che la ricerca è vuota
                        JOptionPane.showMessageDialog(fin, "Il campo di ricerca è vuoto!", "Errore",
                                JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                    System.out.println("[INFO] Richiedo la ricerca di " + query + " al server (SEARCH:" + query + ")");
                    //se non è vuoto invio la funzione di ricerca al server chiedendo SEARCH:query
                    out.println("SEARCH:" + query);
                    try {
                        //ottengo il risultato di ricerca che mi manda il server
                        String searchResult = in.readLine();
                        System.out.println("[INFO] Risultato della ricerca ricevuto con successo.");
                        if (searchResult.equals("")) { //se la ricerca non ha dato risultati
                            filmScrollPanel.setVisible(true); //imposto il pannello dei film visibile
                            seriesScrollPanel.setVisible(true); //imposto il pannello delle serie visibile
                            mainPanel.revalidate(); //aggiorno il pannello principale
                            JOptionPane.showMessageDialog(fin, "Nessun risultato trovato", "Errore", //faccio comparire una che informa l'utente che non ci sono risultati
                                    JOptionPane.INFORMATION_MESSAGE);
                            return;
                        }
                        // altrimenti se ci sono risultati
                        //creo un pannello con i risultati
                        searchResultPanel = new JPanel();
                        searchResultPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
                        // creo il bottone per tornare indietro
                        JButton backButton = new JButton("Indietro");
                        backButton.addActionListener(new ActionListener() {
                            @Override
                            public void actionPerformed(ActionEvent e) { //se il bottone è cliccato
                                if (searchResultScrollPanel != null) { //se il pannello dei risultati è attivo
                                    searchResultScrollPanel.setVisible(false); //lo rendo invisibile
                                    mainPanel.remove(searchResultScrollPanel); //lo rimuovo
                                    mainPanel.revalidate();
                                    searchResultScrollPanel = null;
                                }
                                //faccio tornare visibile la home
                                filmScrollPanel.setVisible(true);
                                seriesScrollPanel.setVisible(true);
                                mainPanel.revalidate();
                            }
                        });
                        //aggiungo il bottone al pannello dei risultati
                        searchResultPanel.add(backButton);
                        //prendo gli id delle risorse trovate separando per ,
                        String[] ids = searchResult.split(",");
                        //il server manda la lista con anche i duplicati, perciò li elimino
                        // elimino i duplicati
                        Set<String> uniqueElements = new HashSet<>();
                        for (String s : ids) {
                            uniqueElements.add(s);
                        }
                        ids = uniqueElements.toArray(new String[0]);
                        //creo i bottoni per ogni id
                        for (int i = 0; i < ids.length; i++) {
                            boolean isSeries = true;
                            if (ids[i].charAt(0) == 'm') { //controllo se è un film o una serie controllando la prima lettera dell'id
                                isSeries = false;
                            }
                            //creo il bottone
                            JButton button = createButton(ids[i], in, is, out, fin, isSeries);
                            //aggiungo il bottone al pannello.
                            searchResultPanel.add(button);
                        }
                        // creo il pannello con scroll per i risultati
                        searchResultScrollPanel = new JScrollPane(searchResultPanel);
                        //rendo invisibile la home
                        filmScrollPanel.setVisible(false);
                        seriesScrollPanel.setVisible(false);
                        //posiziono il pannello dei risultati
                        gbcMain.gridx = 0;
                        gbcMain.gridy = 1;
                        //lo aggiungo al main
                        mainPanel.add(searchResultScrollPanel, gbcMain);
                        //lo imposto visibile
                        searchResultScrollPanel.setVisible(true);
                        mainPanel.revalidate();
                    } catch (IOException ex) {
                        System.out.println("[ERROR] Errore IO");
                    }
                }
            });
            //bottone per vedere i film noleggiati dall'utente
            JButton collectButton = new JButton("Collezione");
            collectButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) { //se premuto
                    try {
                        if (collectionPanel != null) { // se il pannello esiste già ritorno.
                            return;
                        }
                        System.out.println("[INFO] Richiedo i titoli noleggiati");
                        //richiedo i titoli noleggiati al server tramite il comando GET_RENTED
                        out.println("GET_RENTED: ");
                        String xml = "";//variabile per l'xml che mi restituisce il server
                        xml = in.readLine(); //leggo l'xml
                        // salvo nel file xml
                        String xmlPath = "./richieste/rent.xml"; //creo un file rent.xml nella cartella delle richieste
                        PrintWriter writer = new PrintWriter(xmlPath, "UTF-8"); //stampo l'xml mandato dal server sul file
                        writer.println(xml);  //scrivo l'xml
                        writer.close(); //chiudo il wrtier
                        System.out.println("[INFO] Richiesta salvata in " + xmlPath);
                        collectionPanel = new JPanel(new GridBagLayout());
                        //creo un bottone per tornare indietro
                        JButton backButton = new JButton("Indietro");
                        backButton.addActionListener(new ActionListener() {
                            @Override
                            public void actionPerformed(ActionEvent e) {
                                //imposto il pannello della collezione invisibile
                                collectionPanel.setVisible(false);
                                mainPanel.remove(collectionPanel); // e lo rimuovo
                                mainPanel.revalidate();
                                collectionPanel = null;
                                //rendo visibile la home
                                filmScrollPanel.setVisible(true);
                                seriesScrollPanel.setVisible(true);
                            }
                        });
                        //aggiungo il bottone al pannello per tornare indietro
                        gbcMain.gridx = 0;
                        gbcMain.gridy = 0;
                        collectionPanel.add(backButton, gbcMain);
                        //creo il pannello per i film noleggiati dall'utente
                        createCollectionPanel(xmlPath, in, out, is);

                        //disattivo i vari pannelli e li rimuovo se esistono
                        if (searchResultScrollPanel != null) {
                            searchResultScrollPanel.setVisible(false);
                            mainPanel.remove(searchResultScrollPanel);
                            mainPanel.revalidate();
                            searchResultScrollPanel = null;
                        }
                        if (infoPanel != null) {
                            infoPanel.setVisible(false);
                            mainPanel.revalidate();
                        }
                        if (episodePanel != null) {
                            episodePanel.setVisible(false);
                            mainPanel.remove(episodePanel);
                            mainPanel.revalidate();
                        }
                        //rendo invisibile la home
                        filmScrollPanel.setVisible(false);
                        seriesScrollPanel.setVisible(false);
                        //posiziono
                        gbcMain.gridx = 0;
                        gbcMain.gridy = 1;
                        //aggiungo al pannello principale il pannello della libreria dei titoli noleggiati
                        mainPanel.add(collectionPanel, gbcMain);
                        mainPanel.revalidate();

                    } catch (IOException ex) {
                        System.out.println("[ERROR] Errore IO");
                    }
                }
            });
            //aggiungo al pannnello di ricerca la barra, il bottone per cercare e il bottone che mostra la libreria
            searchPanel.add(searchField);
            searchPanel.add(searchButton);
            searchPanel.add(collectButton);
            //imposto il saldo dell'utente
            saldoLabel.setText("Saldo: " + saldoStr + " €");
            //aggiungo il saldo al pannello
            searchPanel.add(saldoLabel);

            //posiziono
            gbcMain.gridx = 0;
            gbcMain.gridy = 0;
            //aggiungo al main
            mainPanel.add(searchPanel, gbcMain);
            //posiziono
            gbcMain.gridx = 0;
            gbcMain.gridy = 1;
            //creo gli scroll panel per i film e le serie
            filmScrollPanel = new JScrollPane(filmPanel);
            seriesScrollPanel = new JScrollPane(seriesPanel);
            mainPanel.add(filmScrollPanel, gbcMain);
            // Aggiungi il pannello delle serie TV al pannello principale
            gbcMain.gridx = 0;
            gbcMain.gridy = 2;
            mainPanel.add(seriesScrollPanel, gbcMain);
            JScrollPane scrollPane = new JScrollPane(mainPanel);
            scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
            //aggiungo il pannello principale alla finestra.
            fin.add(scrollPane);
            fin.setVisible(true);
        } catch (UnknownHostException ex) {
            System.out.println("[ERROR] Host sconosciuto");
        } catch (IOException ex) {
            System.out.println("[ERROR] Errore IO");
        }
    }

    //funzione che restituisce un array di byte che rappresentano un immagine (richiesta al server)
    public static byte[] getImageBuffer(BufferedReader in, InputStream is) throws IOException {
        // Leggi la lunghezza dell'immagine
        String msgServer = in.readLine();
        int length = 0;
        try {
            // converto la lunghezza dell'immagine in int
            length = Integer.valueOf(msgServer).intValue();
        } catch (Exception e) {
            System.out.println("[ERROR] conversione della lunghezza dell'immagine in int");
        }
        // Leggi l'array di byte dell'immagine
        byte[] buffer = new byte[length];
        int bytesRead;
        int totalBytesRead = 0;
        while (totalBytesRead < length
                && (bytesRead = is.read(buffer, totalBytesRead, length - totalBytesRead)) != -1) {
            totalBytesRead += bytesRead;
        }
        //ritorno il buffer dell'immagine
        return buffer;
    }

    //funzione per la creazione del bottone con l'immagine del film o della serie e le informazioni quando cliccato
    public static JButton createButton(String id, BufferedReader in, InputStream is, PrintWriter out, Main fin, boolean isSeries) throws IOException {
        System.out.println("[INFO] Richiedo " + "GET_IMAGE:" + id);
        //richiedo l'immagine al server mandando GET_IMAGE:id e ottengo l'immagine
        out.println("GET_IMAGE:" + id);
        //creo un ImageIcon con l'immagine
        ImageIcon icon = new ImageIcon(getImageBuffer(in, is));
        Image image = icon.getImage();
        //creo un immagine scalata
        Image scaledImage = image.getScaledInstance(posterWidth, posterHeight, Image.SCALE_SMOOTH);
        ImageIcon scaledIcon = new ImageIcon(scaledImage);
        //creo il bottone con l'immagine dentro
        JButton button = new JButton(scaledIcon);
        button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) { //se il bottone viene premuto
                //rendo invisibile la home
                seriesScrollPanel.setVisible(false);
                filmScrollPanel.setVisible(false);
                //disattivo i vari pannelli e li rimuovo se esistono
                if (searchResultScrollPanel != null) {
                    searchResultScrollPanel.setVisible(false);
                    mainPanel.remove(searchResultScrollPanel);
                    mainPanel.revalidate();
                    searchResultScrollPanel = null;
                }
                System.out.println("[INFO] Richiedo le informazioni di " + id + " (GET_INFO:" + id + ")");
                // richiedo le informazioni al server: GET_INFO:id e ottengo l'xml
                out.println("GET_INFO:" + id);
                try {
                    // leggo il file xml che mi viene inviato
                    String xml = in.readLine();
                    System.out.println("[INFO] xml della richiesta " + "GET_INFO:" + id + " ricevuto con successo.");
                    // creo un file richiesta-id.xml e ci scrivo il contenuto di xml
                    // facendo iniziare il file con <richiesta> e finire con </richiesta>
                    String xmlPath = "./richieste/richiesta-" + id + ".xml";
                    PrintWriter writer = new PrintWriter(xmlPath, "UTF-8");
                    writer.println("<richiesta>");
                    writer.println(xml);
                    writer.println("</richiesta>");
                    writer.close();
                    System.out.println("[INFO] xml della richiesta " + "GET_INFO:" + id + " salvato in " + xmlPath);
                    // apro il file per estrapolare i dati xml
                    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                    DocumentBuilder parser = factory.newDocumentBuilder();
                    Document doc = parser.parse(new File(xmlPath));
                    System.out.println("[INFO] lettura di " + xmlPath);
                    Element root = doc.getDocumentElement();
                    //ottengo il titolo, il regista, il genere, l'anno, la trama
                    String title = root.getElementsByTagName("titolo").item(0).getTextContent();
                    String director = root.getElementsByTagName("regista").item(0).getTextContent();
                    String genere = root.getElementsByTagName("genere").item(0).getTextContent();
                    String trama = root.getElementsByTagName("trama")
                            .item(root.getElementsByTagName("trama").getLength() - 1).getTextContent();
                    //essendo che l'anno delle serie è un dato composto da inizio e fine gestisco l'acquisizione dell'anno in modo diverso
                    String anno = " ";
                    //prendo tutto l'anno
                    Element annoTot = (Element) root.getElementsByTagName("anno").item(0);
                    // creo il box per la stagione e l'episodio
                    JComboBox<String> stagioneBox = new JComboBox<>();
                    JComboBox<String> episodeBox = new JComboBox<>();
                    //disabilito il box degli episodi
                    episodeBox.setEnabled(false);
                    //prendo la durata se non è una serie
                    String durata = "";
                    if (!isSeries) {
                        durata = root.getElementsByTagName("durata").item(0).getTextContent();
                    }
                    if (isSeries) {
                        // se è una serie separo inizio e fine e li salvo in anno come inizio-fine
                        String inizio = annoTot.getElementsByTagName("inizio").item(0).getTextContent();
                        String fine = annoTot.getElementsByTagName("fine").item(0).getTextContent();
                        anno = inizio + " - " + fine;
                        // se è una serie creo un JComboBox con le stagioni che ottengo dall'xml,
                        // dall'attributo numero di ogni stagione
                        NodeList stagioni = root.getElementsByTagName("stagione");
                        for (int i = 0; i < stagioni.getLength(); i++) {
                            Element stagione = (Element) stagioni.item(i);
                            String numero = stagione.getAttribute("numero");
                            // aggiungo le stagioni al box
                            stagioneBox.addItem("Stagione " + numero);
                        }
                    } else {
                        //altrimenti prendo l'anno e basta.
                        anno = annoTot.getTextContent();
                    }
                    //creo il pannello per gli attori
                    JPanel actorsPanel = new JPanel(new GridBagLayout());
                    // creo un panel per visualizzare le informazioni, che estrapolo dall'xml
                    infoPanel = new JPanel(new GridBagLayout());
                    GridBagConstraints gbc = new GridBagConstraints();
                    gbc.insets = new Insets(5, 5, 5, 5);
                    infoPanel.setBorder(BorderFactory.createTitledBorder(title));
                    // ottengo gli attori
                    NodeList attori = root.getElementsByTagName("attore");
                    actorsPanel.setBorder(BorderFactory.createTitledBorder("Attori"));
                    for (int i = 0; i < attori.getLength(); i++) { //aggiungo gli attori al pannello degli attori uno sotto l'altro
                        String actorName = attori.item(i).getTextContent();
                        gbc.gridx = 0;
                        gbc.gridy = i;
                        gbc.anchor = GridBagConstraints.NORTHWEST;
                        actorsPanel.add(new JLabel("- " + actorName), gbc);
                    }
                    // creo un bottone per tornare indietro
                    JButton backButton = new JButton("Indietro");
                    backButton.addActionListener(new ActionListener() {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            //disattivo i vari pannelli e li rimuovo se esistono
                            if (episodePanel != null) {
                                mainPanel.remove(episodePanel);
                                mainPanel.revalidate();
                            }
                            if (searchResultScrollPanel != null) {
                                searchResultScrollPanel.setVisible(false);
                                mainPanel.remove(searchResultScrollPanel);
                                mainPanel.revalidate();
                                searchResultScrollPanel = null;
                            }
                            //rendo visibile la home e faccio scomparire il pannello delle informazioni
                            infoPanel.setVisible(false);
                            filmScrollPanel.setVisible(true);
                            seriesScrollPanel.setVisible(true);
                            mainPanel.revalidate();
                        }
                    });
                    // Richiedo l'immagine e l'aggiungo al panel
                    System.out.println("[INFO] Richiedo " + "GET_IMAGE:" + id);
                    out.println("GET_IMAGE:" + id);
                    //come prima ottengo l'immagine
                    ImageIcon icon = new ImageIcon(getImageBuffer(in, is));
                    Image image = icon.getImage();
                    //creo un immagine scalata
                    Image scaledImage = image.getScaledInstance(posterWidth, posterHeight, Image.SCALE_SMOOTH);
                    ImageIcon scaledIcon = new ImageIcon(scaledImage);
                    JLabel imageLabel = new JLabel(scaledIcon);
                    System.out.println("[INFO] Richiedo " + "IS_RENT:" + id);
                    //chiedo al server se la risorsa di cui l'utente vuole ottenere le informazioni è noleggiata (IS_RENT:id), mi risponde con true o false
                    out.println("IS_RENT:" + id);
                    //ottengo la risposta
                    String isRent = in.readLine();
                    System.out.println("[SERVER] IS_RENT:" + isRent);
                    // ottengo il costo della risorsa
                    String costo = doc.getElementsByTagName("costo").item(0).getTextContent();
                    //creo un bottone per noleggiare la risorsa
                    JButton rentButton = new JButton("Noleggia " + costo + "€");
                    rentButton.addActionListener(new ActionListener() {
                        @Override
                        public void actionPerformed(ActionEvent e) { // se premuto
                            try {
                                System.out.println("[INFO] Richiedo " + "RENT:" + id);
                                //mando il comando RENT:id che permette di noleggiare la risorsa
                                out.println("RENT:" + id);
                                // ottengo il risultato del noleggio (true o false)
                                String rentResult = in.readLine();
                                System.out.println("[SERVER] RENT:" + rentResult);
                                updateSaldo(in, out); // aggiorno il saldo graficamente nella GUI
                                if (rentResult.equals("true")) { // se il rent è andato a buon fine
                                    System.out.println("[INFO] Noleggio avvenuto con successo");
                                    rentButton.setEnabled(false); //disabilito il bottone
                                    mainPanel.revalidate();
                                    //comunico all'utente tramtite una finestra di dialogo che il noleggio è avvenuto con successo
                                    JOptionPane.showMessageDialog(fin,
                                            "Noleggio avvennuto con successo, contenuto aggiunto alla tua liberia",
                                            "Successo noleggio",
                                            JOptionPane.INFORMATION_MESSAGE);
                                } else {
                                    //se il noleggio è andato male comunico all'utente che non ha abbastanza saldo
                                    System.out.println("[INFO] Il noleggio non è avvenuto.");
                                    JOptionPane.showMessageDialog(fin,
                                            "Non hai abbastanza saldo per l'acquisto di questo titolo.",
                                            "Credito insufficiente",
                                            JOptionPane.ERROR_MESSAGE);
                                }
                            } catch (IOException ex) {
                                System.out.println("[ERROR] Errore IO");
                            }
                        }
                    });
                    //posiziono
                    gbc.gridx = 1;
                    gbc.gridy = 0;
                    gbc.anchor = GridBagConstraints.NORTHWEST;
                    //aggiungo il bottone del noleggio al panel
                    infoPanel.add(rentButton, gbc);
                    //se la risorsa è già noleggiata disabilito il bottone
                    if (isRent.equals("false")) {
                        rentButton.setEnabled(true);
                    } else {
                        rentButton.setEnabled(false);
                    }
                    //aggiungo il bottone per tornare indietro al panel
                    gbc.gridx = 0;
                    gbc.gridy = 0;
                    gbc.anchor = GridBagConstraints.NORTHWEST;
                    infoPanel.add(backButton, gbc);
                    //aggiungo l'immagine al panel
                    gbc.gridx = 0;
                    gbc.gridy = 1;
                    gbc.anchor = GridBagConstraints.WEST;
                    infoPanel.add(imageLabel, gbc);
                    //aggiungo il regista al panel
                    gbc.gridx = 1;
                    gbc.gridy = 2;
                    infoPanel.add(new JLabel("Regista: " + director), gbc);
                    //aggiungo l'anno al panel
                    gbc.gridx = 1;
                    gbc.gridy = 4;
                    infoPanel.add(new JLabel("Anno: " + anno), gbc);
                    //aggiungo la trama al panel
                    gbc.gridx = 1;
                    gbc.gridy = 5;
                    infoPanel.add(new JLabel("Trama: " + trama), gbc);
                    //aggiungo il genere al panel
                    gbc.gridx = 1;
                    gbc.gridy = 1;
                    infoPanel.add(new JLabel("Genere: " + genere), gbc);
                    //se è una serie
                    if (isSeries) {
                        stagioneBox.addActionListener(new ActionListener() { // aggiungo un listener alla box della stagione
                            @Override
                            public void actionPerformed(ActionEvent e) {
                                //quando viene selezionata un'opzione attivo la box degli episodi
                                episodeBox.setEnabled(true);
                                //e la aggiorno con gli episodi della stagione selezionata
                                updateEpisode(stagioneBox, episodeBox, root);
                            }
                        });
                        episodeBox.addActionListener(new ActionListener() { //aggoingo un listener alla box degli episodi
                            @Override
                            public void actionPerformed(ActionEvent e) { // quando viene selezionato un episodio
                                //rimuovo il pannello degli episodi se esiste
                                if (episodePanel != null) {
                                    episodePanel.setVisible(false);
                                    infoPanel.remove(episodePanel);
                                    infoPanel.revalidate();
                                    infoPanel.repaint();
                                }
                                //aggiorno il pannello degli episodi
                                updateEpisodePanel(episodeBox, infoPanel, root, (String) stagioneBox.getSelectedItem());
                            }
                        });
                        //aggiungo la box della stagione e dell'episodio al panel
                        gbc.gridx = 0;
                        gbc.gridy = 2;
                        gbc.anchor = GridBagConstraints.WEST;
                        infoPanel.add(stagioneBox, gbc);
                        gbc.gridy = 3;
                        infoPanel.add(episodeBox, gbc);
                    } else {
                        //altrimenti aggiungo la durata al panel
                        gbc.gridx = 1;
                        gbc.gridy = 6;
                        infoPanel.add(new JLabel("Durata: " + durata + " minuti"), gbc);
                    }
                    //aggiungo il pannello degli attori al panel
                    gbc.gridx = 4;
                    gbc.gridy = 1;
                    gbc.anchor = GridBagConstraints.NORTHEAST;
                    infoPanel.add(actorsPanel, gbc);
                    // aggiungo il panel delle informazioni al pannello principale
                    gbc.gridx = 0;
                    gbc.gridy = 1;
                    mainPanel.add(infoPanel, gbc);
                    infoPanel.setVisible(true);
                    mainPanel.revalidate();
                } catch (IOException ex) {
                    System.out.println("[ERROR] Errore IO");
                } catch (ParserConfigurationException ex) {
                    System.out.println("[ERROR] Errore parser XML");
                } catch (SAXException ex) {
                    System.out.println("[ERROR] Errore SAX");
                }
            }
        });
        //ritorno il bottone che ho creato.
        return button;
    }
    // funzione per aggiornare i campi della box degli episodi in funzione della stagione
    public static void updateEpisode(JComboBox stagioneBox, JComboBox episodeBox, Element root) {
        String selectedValue = (String) stagioneBox.getSelectedItem(); //prendo il valore della stagione selezionata
        episodeBox.removeAllItems(); //tolgo tutti gli elementi dalla box degli episodi
        // cerco la stagione con l'attributo numero uguale al valore selzionato, e
        // prendo gli episodi
        NodeList stagioni = root.getElementsByTagName("stagione"); //prendo tutte le stagioni
        for (int i = 0; i < stagioni.getLength(); i++) {
            Element stagione = (Element) stagioni.item(i);
            //prendo il numero della stagione
            String n_stagione = stagione.getAttribute("numero");
            n_stagione = "Stagione " + n_stagione;
            if (n_stagione.equals(selectedValue)) { //se la stagione è quella selezionata
                NodeList episodi = stagione.getElementsByTagName("episodio"); // prendo tutti gli episodi della stagione
                for (int j = 0; j < episodi.getLength(); j++) { //per ogni episodio
                    Element episodio = (Element) episodi.item(j);
                    //prendo il numero e il titolo dell'episodio
                    String numero = episodio.getElementsByTagName("numero").item(0).getTextContent();
                    String titolo = episodio.getElementsByTagName("titolo").item(0).getTextContent();
                    //aggiungo l'episodio alla box
                    episodeBox.addItem(numero + " - " + titolo);
                }
            }
        }
    }
    //funzione per aggiornare il pannello degli episodi, in funzione dell'episodio selezionato
    public static void updateEpisodePanel(JComboBox episodeBox, JPanel infoPanel, Element root, String n_stagione) {
        String selectedValue = (String) episodeBox.getSelectedItem(); //prendo l'episodio selezionato
        episodePanel = new JPanel(new GridBagLayout()); // creo il pannello con le info dell'episodio
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        episodePanel.setBorder(BorderFactory.createTitledBorder(selectedValue)); //imposto il titolo del pannello
        // cerco l'episodio con il titolo uguale al valore selzionato, e
        // prendo le informazioni
        //prendo tutte le stagioni
        NodeList stagioni = root.getElementsByTagName("stagione");
        //per ogni stagione
        for (int i = 0; i < stagioni.getLength(); i++) {
            Element stagione = (Element) stagioni.item(i);
            // prendo il numero della stagione dall'xml
            String n_stagioneXML = "Stagione " + stagione.getAttribute("numero");
            //se la stagione è quella selezionata
            if (n_stagioneXML.equalsIgnoreCase(n_stagione)) {
                //prendo tutti gli episodi della stagione
                NodeList episodi = stagione.getElementsByTagName("episodio");
                for (int j = 0; j < episodi.getLength(); j++) {
                    // per ogni episodio
                    Element episodio = (Element) episodi.item(j);
                    String titolo = episodio.getElementsByTagName("titolo").item(0).getTextContent();
                    String numero = episodio.getElementsByTagName("numero").item(0).getTextContent();
                    String val = numero + " - " + titolo;
                    //se l'episodio è quello selezionato
                    if (val.equalsIgnoreCase(selectedValue)) {
                        //prendo trama e durata
                        String trama = episodio.getElementsByTagName("trama").item(0).getTextContent();
                        String durata = episodio.getElementsByTagName("durata").item(0).getTextContent();
                        //li aggiungo al pannello
                        gbc.gridx = 0;
                        gbc.gridy = 0;
                        gbc.anchor = GridBagConstraints.NORTHWEST;
                        episodePanel.add(new JLabel(n_stagione + " episodio " + numero), gbc);

                        gbc.gridx = 0;
                        gbc.gridy = 1;
                        episodePanel.add(new JLabel("Titolo: " + titolo), gbc);

                        gbc.gridx = 0;
                        gbc.gridy = 2;
                        episodePanel.add(new JLabel("Durata: " + durata + " minuti"), gbc);
                        // trama
                        gbc.gridx = 0;
                        gbc.gridy = 3;
                        episodePanel.add(new JLabel("Trama: " + trama), gbc);

                        gbc.gridx = 1;
                        gbc.gridy = 6;
                        gbc.anchor = GridBagConstraints.CENTER;
                        infoPanel.add(episodePanel, gbc);
                        infoPanel.revalidate();
                    }
                }
            }
        }
    }
    //funzione per creare il pannello con gli elementi noleggiati dall'utente
    private static void createCollectionPanel(String path, BufferedReader in, PrintWriter out, InputStream is) {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        try {
            // apro xml e tiro giù i dati
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder parser = factory.newDocumentBuilder();
            Document doc = parser.parse(new File(path));
            System.out.println("[INFO] lettura di " + path);
            Element root = doc.getDocumentElement();
            //ottengo tutti gli elementi noleggiati
            NodeList rents = root.getElementsByTagName("noleggio");
            // se non ci sono elementi noleggiati mostro un messaggio di nessun contenuto noleggiato
            if (rents.getLength() == 0) {
                JLabel noRentLabel = new JLabel("Nessun contenuto noleggiato");
                gbc.gridx = 1;
                gbc.gridy = 1;
                collectionPanel.add(noRentLabel, gbc);
                return;
            }
            // per ogni noleggio creo un panel con le informazioni
            for (int i = 0; i < rents.getLength(); i++) {
                JPanel elPanel = new JPanel(new GridBagLayout());
                Element rent = (Element) rents.item(i);
                //ottengo l'id dell'elemento noleggiato e la scadenza
                String id = rent.getElementsByTagName("id").item(0).getTextContent();
                String scadenza = rent.getElementsByTagName("scadenza").item(0).getTextContent();
                //richiedo le informazioni dell'elemento noleggiato
                out.println("GET_INFO:" + id);
                //ottengo in risposta l'xml
                String xml = in.readLine();
                System.out.println("[INFO] xml della richiesta " + "GET_INFO:" + id + " ricevuto con successo.");
                // creo un file richiesta-id.xml e ci scrivo il contenuto di xml
                String xmlPath = "./richieste/richiesta-" + id + ".xml";
                PrintWriter writer = new PrintWriter(xmlPath, "UTF-8");
                writer.println("<richiesta>");
                writer.println(xml);
                writer.println("</richiesta>");
                writer.close();
                System.out.println("[INFO] xml della richiesta " + "GET_INFO:" + id + " salvato in " + xmlPath);
                // apro il file per estrapolare i dati xml
                Document docEl = parser.parse(new File(xmlPath));
                Element rootEl = docEl.getDocumentElement();
                String title = rootEl.getElementsByTagName("titolo").item(0).getTextContent();
                String trama = rootEl.getElementsByTagName("trama").item(rootEl.getElementsByTagName("trama").getLength() - 1).getTextContent();
                String anno = " ";
                String director = rootEl.getElementsByTagName("regista").item(0).getTextContent();
                String genre = rootEl.getElementsByTagName("genere").item(0).getTextContent();
                Element annoTot = (Element) rootEl.getElementsByTagName("anno").item(0);
                // se l'lemento è una serie prendo inizio e fine e li metto come inizio-fine
                if (id.contains("ser")) {
                    String inizio = annoTot.getElementsByTagName("inizio").item(0).getTextContent();
                    String fine = annoTot.getElementsByTagName("fine").item(0).getTextContent();
                    anno = inizio + "-" + fine;
                } else {
                    anno = annoTot.getTextContent();
                }
                // Richiedo l'immagine e l'aggiungo al panel
                System.out.println("[INFO] Richiedo " + "GET_IMAGE:" + id);
                //ottengo l'immagine dell'elemento noleggiato
                out.println("GET_IMAGE:" + id);
                ImageIcon icon = new ImageIcon(getImageBuffer(in, is));
                Image image = icon.getImage();
                Image scaledImage = image.getScaledInstance(posterWidth, posterHeight, Image.SCALE_SMOOTH);
                ImageIcon scaledIcon = new ImageIcon(scaledImage);
                JLabel imageLabel = new JLabel(scaledIcon);
                // metto il bordo al panel
                elPanel.setBorder(BorderFactory.createTitledBorder(title));
                //inserisco gli elementi nel panel
                gbc.gridx = 0;
                gbc.gridy = 0;
                gbc.anchor = GridBagConstraints.NORTHWEST;
                elPanel.add(imageLabel, gbc);
                gbc.gridx = 1;
                gbc.gridy = 1;
                elPanel.add(new JLabel("Trama: " + trama), gbc);
                gbc.gridx = 1;
                gbc.gridy = 2;
                elPanel.add(new JLabel("Regista: " + director), gbc);
                gbc.gridx = 1;
                gbc.gridy = 3;
                elPanel.add(new JLabel("Anno: " + anno), gbc);
                gbc.gridx = 1;
                gbc.gridy = 4;
                elPanel.add(new JLabel("Genere: " + genre), gbc);
                gbc.gridx = 1;
                gbc.gridy = 5;
                // metto la scadenza da formato yyyy-mm-ddTHH:MM:SS a dd-mm-yyyy
                elPanel.add(new JLabel("Scadenza: " + scadenza), gbc);
                gbc.gridx = 1;
                gbc.gridy = (i + 1);
                collectionPanel.add(elPanel, gbc);
            }
        } catch (IOException e) {
            System.out.println("[ERROR] Errore IO");
        } catch (ParserConfigurationException ex) {
            System.out.println("[ERROR] Errore configurazione parser");
        } catch (SAXException ex) {
            System.out.println("[ERROR] Errore SAX");
        }
    }
    //funzione per l'aggiornamento del saldo
    private static void updateSaldo(BufferedReader in, PrintWriter out) {
        try {
            System.out.println("[INFO] Richiedo il saldo (GET_SALDO)");
            //chiedo il mio saldo al server con il comando GET_SALDO
            out.println("GET_SALDO: ");
            //ottengo il saldo come risposta del server
            String saldoStr = in.readLine();
            System.out.println("[SERVER] GET_SALDO: " + saldoStr);
            //aggiorno la label del saldo
            saldoLabel.setText("Saldo: " + saldoStr + " €");
        } catch (IOException ex) {
            System.out.println("[ERROR] Errore IO");
        }
    }
}
