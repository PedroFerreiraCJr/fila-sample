import java.util.Random;
import java.util.Queue;
import java.util.LinkedList;
import java.util.Scanner;

public class Main {
	public static void main(String[] args) throws InterruptedException {
	    try (Scanner scan = new Scanner(System.in)) {
	        final Fila<String> fila = new Fila<>();
	        
	        Thread tscan = new Thread(() -> {
	            String line = null;
	            while (true) {
	                System.out.println("Digite 'exit' para terminar o processamento das threads.");
	                line = scan.nextLine();
	                if (line != null) {
	                    if (line.equals("exit")) {
	                        fila.terminate();
	                        System.out.println("******** Fila concluída ********");
	                        break;
	                    }
	                }
	            }
	        });
	        tscan.start();
            
            Thread.currentThread().sleep(5_000);
            
            final Generator<String> generator = new SimpleGenerator();
            final Producer<String> producer = new Producer(fila, generator);
            final Consumer<String> consumer = new Consumer(fila, new SimpleListener());
            
            
            // start as threads consumidoras
            Thread tc1 = new Thread(consumer);
            tc1.start();
            
            Thread tc2 = new Thread(consumer);
            tc2.start();
            
            // aguarda alguns segundos antes de produzir algo na fila
            Thread.currentThread().sleep(5_000);
            
            Thread tp = new Thread(producer);
            tp.start();
            
            tc1.join();
            tc2.join();
            tp.join();
            
            System.out.println("Itens na fila: " + fila.tamanho());
	    }
	}
}

class Fila<T> {
    
    private final Queue<T> values;
    private boolean terminated;
    private final Object lock = new Object();
    
    Fila() {
        super();
        this.terminated = false;
        this.values = new LinkedList<>();
    }
    
    public boolean adicionar(T value) {
        synchronized (lock) {
            System.out.println("Thread produtora está adicionando valores a fila...");
            boolean result = values.add(value);
            if (result) {
                System.out.println("Thread produtora está notificando as demais threads consumidoras...");
                lock.notifyAll();
            }
            return result;
        }
    }
    
    public T remover() {
        synchronized (lock) {
            while (tamanho() == 0) {
                try {
                    System.out.println("Thread consumidora está aguardando no bloco de guarda...");
                    lock.wait();
                } catch(InterruptedException e) {
                    e.printStackTrace();
                }
            }
            
            return values.remove();
        }
    }
    
    public void terminate() {
        this.terminated = true;
    }
    
    public boolean terminated() {
        return this.terminated;
    }
    
    public int tamanho() {
        return values.size();
    }
}

interface Generator<T> {
    T generate();
}

class SimpleGenerator implements Generator<String> {
    
    private final Random rand = new Random();
    
    public String generate() {
        return String.format("Pessoa %d", rand.nextInt(Short.MAX_VALUE));
    }
}

class Producer<T> implements Runnable {
    private Fila<T> fila;
    private Generator<T> generator;
    
    Producer(Fila<T> fila, Generator<T> generator) {
        this.fila = fila;
        this.generator = generator;
    }
    
    @Override
    public void run() {
        while (true) {
            if (fila.terminated()) {
                break;
            }

            T value = generator.generate();
            boolean adicionado = false;
            do {
                adicionado = fila.adicionar(value);
                
                if (!adicionado) {
                    try {
                        Thread.currentThread().sleep(50);
                    } catch(InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            } while (!adicionado);
            
            //System.out.println("Valor adicionado: " + value.toString());
            
            // caso tenha sido acidionado a fila, dá uma pausa
            try {
                Thread.currentThread().sleep(100);
            } catch(InterruptedException e) {
                e.printStackTrace();
            }
        }
        
        System.out.println("Thread produtora terminada!");
    }
}

interface Listener<T> {
    void apply(T value);
}

class SimpleListener implements Listener<String> {
    public void apply(String value) {
        System.out.println(String.format("Pessoa removida: %s", value));
    }
}

class Consumer<T> implements Runnable {
    private Fila<T> fila;
    private Listener<T> listener;
    
    Consumer(Fila<T> fila, Listener<T> listener) {
        this.fila = fila;
        this.listener = listener;
    }
    
    @Override
    public void run() {
        while (fila.tamanho() != 0 || !fila.terminated()) {
            T value = fila.remover();
            
            if (listener != null) {
                listener.apply(value);
            }

            try {
                Thread.currentThread().sleep(400);
            } catch(InterruptedException e) {
                e.printStackTrace();
            }
        }
        
        System.out.println("Thread consumidora terminada!");
    }
}
