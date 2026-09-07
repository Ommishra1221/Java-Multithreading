public class _04_StaticSynchronizedDemo {
    public static void main(String[] args) {

        Thread t1 = new Thread(() -> StaticSyncCounter.increment());

        Thread t2 = new Thread(() -> StaticSyncCounter.increment());

        t1.start();
        t2.start();
    }
}

// Static Synchronization

class StaticSyncCounter {

    static int count = 0;

    static void increment() {
        synchronized(StaticSyncCounter.class) {
            try {
                Thread.sleep(2000);
            }
            catch(Exception e) {}

            count++;
            System.out.println(count);
        }
    }
}

