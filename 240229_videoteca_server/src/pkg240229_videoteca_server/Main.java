package pkg240229_videoteca_server;

import java.net.*;
import java.io.*;

public class Main {

    public static final int PORT = 12345;

    public static void main(String[] args) {
        int clientCtr = 1; //counter dei clienti
        Semaforo semNoleggi= new Semaforo(1); //semaforo che garantisce thread safe
        try {
            ServerSocket ss = new ServerSocket(PORT); //creo il server
            while (true) { //ciclo infinito
                System.out.println("[INFO] In attesa di connessione...");
                Worker s= new Worker(ss.accept(),clientCtr,semNoleggi); //creo il thread
                s.start(); //faccio partire il thread
                System.out.println("[INFO] Connesso con CLIENT "+clientCtr);
                clientCtr++; //incremento il counter dei client
            }
        } catch (IOException ex) {
            System.out.println("[ERRORE] Errore IO");
        }
    }

}