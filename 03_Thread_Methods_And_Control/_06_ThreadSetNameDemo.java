public class _06_ThreadSetNameDemo {
    public static void main(String[] args) {
        Thread t1 = new Thread(() -> {
            System.out.println(Thread.currentThread().getName());
        });

        t1.setName("worker-1");

        t1.start();
    }
}

/*
    currentThread() --> reference of current running thread
*/
