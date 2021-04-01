import java.util.List;
import java.util.ArrayList;
import java.util.Random;

public class Main {
	public static void main(String[] args) throws InterruptedException {
	    final Fila<String> fila = new Fila();
	    final Generator<String> generator = new SimpleGenerator();
	    
	    final Producer<String> producer = new Producer(fila, generator);
	    final Consumer<String> consumer = new Consumer(fila, new SimpleListener());
	    
	    Thread tp = new Thread(producer);
	    tp.start();
	    
	    // aguarda alguns segundos
	    Thread.currentThread().sleep(5_000);
	    
	    // start as threads consumidoras
	    Thread tc1 = new Thread(consumer);
	    tc1.start();
	    
	    Thread tc2 = new Thread(consumer);
	    tc2.start();
	    
	    tp.join();
	    tc1.join();
	    tc2.join();
	    
	    System.out.println("Tamanho da fila: " + fila.tamanho());
	}
}

class Fila<T> {
    
    private final List<T> values;
    private final Object lock = new Object();
    
    Fila() {
        super();
        this.values = new ArrayList<>();
    }
    
    public boolean adicionar(T value) {
        synchronized (lock) {
            return values.add(value);
        }
    }
    
    public T remover() {
        synchronized (lock) {
            return values.remove(0);
        }
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
            
            if (fila.tamanho() == 1_000) {
                break;
            }
        }
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
        while (fila.tamanho() > 0) {
            T value = fila.remover();
            
            if (listener != null) {
                listener.apply(value);
            }
            
            // dá uma pausa na remoção de valores da fila, simulando um processamento mais lendo dos valores
            try {
                Thread.currentThread().sleep(400);
            } catch(InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
