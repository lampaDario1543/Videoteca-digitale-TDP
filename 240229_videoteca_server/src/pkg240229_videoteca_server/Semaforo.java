package pkg240229_videoteca_server;
public class Semaforo {
    private int count;
    public Semaforo(){
        count=1;
    }
    public Semaforo(int c){
        count=c;
    }
    public synchronized void aquire() throws InterruptedException{
        if(count==0)
            this.wait();
        count--;
    }
    public synchronized void release(){
        count++;
        if(count>0)
            this.notify();
    }
}