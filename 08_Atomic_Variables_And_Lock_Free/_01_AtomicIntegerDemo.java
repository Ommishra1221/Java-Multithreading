import java.util.concurrent.atomic.AtomicInteger;

public class _01_AtomicIntegerDemo {
   public static void main(String[] args) {
    
    AtomicCounter counter = new AtomicCounter();
    
    Thread t1 = new Thread(() -> {
        for(int i = 1; i <= 10000; i++) {
            counter.increment();
        }
    });

    Thread t2 = new Thread(() -> {
        for(int i = 1; i <= 10000; i++) {
            counter.increment();
        }
    });

    t1.start();
    t2.start();

    try {
        Thread.sleep(2000);
    }
    catch(InterruptedException e) {}

    System.out.println(counter.count);

   } 
}

// AtomicInteger
class AtomicCounter {
    //int count = 0;
    AtomicInteger count = new AtomicInteger(0);

    void increment() {
        count.incrementAndGet();

        //count++;
    }
}

// t1 & t2 --> concurrently
// t1 & t2 --> parallel

// CAS --> COmpare and set operation
