public class _01_SynchronizedMethodDemo {
    public static void main(String[] args) {

        SyncMethodResource test = new SyncMethodResource();

        Thread t1 = new Thread(() -> test.show());

        Thread t2 = new Thread(() -> test.show());

        t1.start();
        t2.start();
    }
}

class SyncMethodResource {

    synchronized void show() {
        System.out.println(Thread.currentThread().getName() + "Inside show");

        try {
            Thread.sleep(2000);
        }
        catch(Exception e) {}

        System.out.println(Thread.currentThread().getName() + "Show finish");
    }
}
