# Java Multithreading — Lecture 3 Notes

## Essential Thread Methods in Java

This lecture focuses on important methods used to control, observe, schedule, and coordinate Java threads:

- `sleep()`
- `join()`
- `yield()`
- `interrupt()`
- `isInterrupted()`
- `interrupted()`
- `isAlive()`
- `currentThread()`
- `setName()`
- Thread priority
- Daemon threads

---

# 1. Thread Methods — Big Picture

```text
sleep()          → pause current thread temporarily
join()           → wait for another thread to finish
yield()          → hint that current thread is willing to give up CPU
interrupt()      → request/interruption signal
isInterrupted()  → inspect interrupt status
interrupted()   → inspect current thread's interrupt status and clear it
isAlive()        → check whether a thread has started and not terminated
currentThread() → get the currently executing Thread object
setName()        → give a thread a meaningful name
setPriority()    → set scheduling priority hint
setDaemon()      → mark a thread as a daemon/background thread
```

---

# 2. `Thread.sleep()`

## What does `sleep()` do?

```java
Thread.sleep(milliseconds);
```

It pauses the **currently executing thread** for the specified duration.

Example:

```java
public class Demo {
    public static void main(String[] args) {
        System.out.println("Main thread starts");

        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
        }

        System.out.println("Main thread ends");
    }
}
```

During the sleep:

```text
RUNNABLE
    ↓
TIMED_WAITING
    ↓
RUNNABLE
```

Your uploaded `Demo.java` demonstrates this transition with `Thread.sleep(2000)`. fileciteturn2file7L1-L18

## Important: `sleep()` does not release locks

If a thread owns a monitor/lock and calls:

```java
Thread.sleep(2000);
```

it goes into `TIMED_WAITING`, but it does **not** release the monitor.

Remember:

```text
sleep()
→ pauses the current thread
→ TIMED_WAITING
→ does NOT release monitors
```

### Interview Question

**Does `sleep()` release a lock?**

**No.** `Thread.sleep()` does not release intrinsic monitor locks held by the thread.

---

# 3. `join()`

## Why do we need `join()`?

Normally, after starting a thread, the current thread can continue executing.

Sometimes we need:

> "Wait until that other thread completes before I continue."

That's where `join()` is used.

Example:

```java
Thread t1 = new Thread(() -> {
    try {
        Thread.sleep(2000);
    } catch (InterruptedException e) {
    }

    System.out.println("Thread-0 starts");
});

t1.start();
t1.join();

System.out.println("Main thread ends");
```

Conceptually:

```text
Main Thread
    ↓
t1.start()
    ↓
Main calls t1.join()
    ↓
Main → WAITING
    ↓
t1 executes
    ↓
t1 → TERMINATED
    ↓
Main → RUNNABLE
    ↓
Main continues
```

Your uploaded `Demo2.java` shows the timed version `t1.join(1000)` and the main thread waiting while `t1` continues toward termination. fileciteturn2file0L15-L25

---

# 4. `join()` vs `join(timeout)`

```java
t1.join();
```

The current thread waits until the target thread terminates.

```java
t1.join(1000);
```

The current thread waits for **at most** 1000 milliseconds for `t1`.

If `t1` finishes earlier, the waiting thread can continue earlier. If the timeout expires first, the current thread can continue even though `t1` is still alive.

---

# 5. `join()` and Thread State

When a thread calls:

```java
t1.join();
```

the **calling/current thread** can enter:

```text
WAITING
```

It is the caller that waits, not the target thread.

Example:

```text
Main Thread
    ↓ join(t1)
WAITING

t1
    ↓
RUNNABLE
    ↓
TERMINATED

Main
WAITING → RUNNABLE
```

---

# 6. `yield()`

```java
Thread.yield();
```

`yield()` is a **hint** to the scheduler:

> "I am willing to give my CPU time to another runnable thread."

Your uploaded `Demo3.java` uses `Thread.yield()` inside a loop and describes it as a willingness to give CPU time to another thread of the same priority. fileciteturn2file6L1-L6 fileciteturn2file6L24-L31

## Is `yield()` guaranteed?

**No.** The operating system may ignore the hint.

So:

```java
Thread.yield();
```

does **not** mean:

> "Stop this thread and definitely run another thread."

It means approximately:

> "Scheduler, if you want, you can consider running someone else."

### Important properties

```text
yield()
→ scheduling hint
→ OS can ignore it
→ not a guaranteed context switch
→ thread remains RUNNABLE
```

The thread does not enter `WAITING`, `TIMED_WAITING`, or `BLOCKED` merely because of `yield()`. fileciteturn2file6L24-L31

---

# 7. `interrupt()`

A common beginner explanation is:

> "interrupt() stops a thread."

That is too simplistic.

A better mental model is:

> **`interrupt()` is a cooperative signal/request telling a thread that it should stop, cancel, or respond to interruption.**

It does not forcibly kill the thread.

Example:

```java
t1.interrupt();
```

Your uploaded `Demo4.java` demonstrates interruption of a long-running loop. fileciteturn2file5L18-L27

---

# 8. Interrupt Status / Interrupt Flag

A thread has an interrupt status.

Conceptually:

```text
Thread
   ↓
interrupt status
   ↓
true / false
```

Calling:

```java
t1.interrupt();
```

requests interruption by setting/triggering the thread's interruption mechanism. The target thread can then respond appropriately.

---

# 9. Gracefully Handling Interrupts

A thread can cooperate with interruption.

Example:

```java
Thread t1 = new Thread(() -> {
    while (!Thread.currentThread().interrupted()) {
        System.out.println("Running");
    }
});
```

Conceptually:

```text
while not interrupted
       ↓
continue work

interruption requested
       ↓
condition becomes false
       ↓
loop ends
```

This is a useful pattern for gracefully stopping a long-running task. fileciteturn2file5L1-L27

### Important correction

The safest wording is:

> `interrupt()` requests interruption; the target thread decides how to respond.

A thread is not forcibly killed by `interrupt()`.

---

# 10. `isInterrupted()`

```java
thread.isInterrupted();
```

checks the interrupt status of that particular thread.

It returns:

```text
true
```

or:

```text
false
```

It does **not clear** the interrupt status. fileciteturn2file5L27-L27

---

# 11. `Thread.interrupted()`

```java
Thread.interrupted();
```

checks the interrupt status of the **current thread** and clears the status.

The lecture notes contrast the two:

```text
isInterrupted()
→ checks interrupt status
→ does not clear it

interrupted()
→ checks current thread's interrupt status
→ clears it
```

fileciteturn2file5L27-L30

---

# 12. `isInterrupted()` vs `interrupted()`

| `isInterrupted()` | `interrupted()` |
|---|---|
| Instance method | Static method |
| Checks a particular `Thread` object's interrupt status | Checks current thread |
| Does not clear status | Clears status after checking |

### Easy memory trick

```text
thread.isInterrupted()
→ "Check THAT thread"

Thread.interrupted()
→ "Check ME (current thread)"
→ then clear
```

---

# 13. `InterruptedException`

When certain blocking methods are interrupted, Java can throw:

```java
InterruptedException
```

Important examples include:

```java
Thread.sleep(...)
Thread.join(...)
Object.wait(...)
```

Your uploaded lecture code connects `sleep()`, `join()`, and `wait()` with interruption behavior. fileciteturn2file5L28-L30

Example:

```java
try {
    Thread.sleep(5000);
} catch (InterruptedException e) {
    Thread.currentThread().interrupt();
}
```

---

# 14. Why Does `InterruptedException` Happen?

Suppose a thread is sleeping:

```text
Thread
   ↓
TIMED_WAITING
   ↓
sleep()
```

Another thread calls:

```java
thread.interrupt();
```

The sleeping thread can be awakened and receive:

```text
InterruptedException
```

This allows the thread to handle cancellation or interruption in a controlled way.

---

# 15. Why Re-Interrupt in a `catch` Block?

A common pattern is:

```java
catch (InterruptedException e) {
    Thread.currentThread().interrupt();
}
```

This preserves/restores the interrupted status after catching the exception, allowing higher-level code to notice that interruption was requested.

---

# 16. `isAlive()`

`isAlive()` checks whether a thread has been started and has not yet terminated.

Your uploaded `Demo5.java` demonstrates the pattern:

```java
System.out.println(t1.isAlive()); // false

t1.start();

System.out.println(t1.isAlive()); // true
```

After the thread finishes:

```java
System.out.println(t1.isAlive()); // false
```

fileciteturn2file4L10-L21

### Mental model

```text
NEW
   ↓
start()
   ↓
ALIVE
   ↓
TERMINATED
```

So:

```text
Before start       → false
After start        → true (while alive)
After termination  → false
```

---

# 17. What `isAlive()` Does NOT Tell You

`isAlive()` does not tell you:

- whether the thread is currently executing on the CPU,
- whether it is `RUNNABLE`,
- whether it is `BLOCKED`,
- whether it is `WAITING`.

It only tells you whether the thread has started and has not yet terminated.

---

# 18. `currentThread()`

```java
Thread.currentThread()
```

returns a reference to the **currently executing thread**.

Example:

```java
System.out.println(
    Thread.currentThread().getName()
);
```

Your uploaded `Demo6.java` demonstrates this exact pattern. fileciteturn2file3L1-L14

Mental model:

```text
Who is executing this line?
             ↓
Thread.currentThread()
```

---

# 19. `setName()`

A thread can be given a custom name:

```java
t1.setName("worker-1");
```

Then:

```java
System.out.println(
    Thread.currentThread().getName()
);
```

can print:

```text
worker-1
```

The uploaded `Demo6.java` demonstrates this. fileciteturn2file3L3-L14

Meaningful names help with debugging, logging, monitoring, and thread dumps.

Prefer:

```text
payment-worker-1
```

over:

```text
Thread-1
```

---

# 20. Thread Priority

Java defines three common priority constants:

```java
Thread.MIN_PRIORITY   // 1
Thread.NORM_PRIORITY  // 5
Thread.MAX_PRIORITY   // 10
```

Your uploaded `Demo7.java` demonstrates `setPriority(10)` and records the values 1, 5, and 10. fileciteturn2file2L11-L28

---

# 21. Is Higher Priority Guaranteed to Run First?

**No.**

Priority should be treated as a **scheduling hint**, not a strict guarantee of execution order.

The lecture notes emphasize that the OS may:

```text
→ respect priority
→ partially respect priority
→ not meaningfully respect it
```

fileciteturn2file2L20-L28

Therefore, do not make application correctness depend on:

```text
priority 10 must run before priority 5
```

---

# 22. Priority Example

```java
Thread t1 = new Thread(() -> {
    System.out.println("Custom thread running");
});

Thread t2 = new Thread(() -> {
    System.out.println("Custom-2 thread running");
});

t1.start();
t2.start();

t1.setPriority(10);

System.out.println(t1.getPriority());
```

This demonstrates priority assignment, but it does not guarantee that `t1` executes before `t2`. fileciteturn2file2L1-L28

---

# 23. Daemon Threads

A **daemon thread** is a background/support thread.

Example:

```java
Thread t1 = new Thread(() -> {
    while (true) {
        System.out.println("Running...");
    }
});

t1.setDaemon(true);
t1.start();
```

This pattern is shown in your uploaded `Demo8.java`. fileciteturn2file1L1-L17

---

# 24. User Threads vs Daemon Threads

Conceptually:

```text
Java Application
│
├── User Threads
│
└── Daemon Threads
```

Important rule:

> **The JVM can terminate when no user (non-daemon) threads remain, even if daemon threads are still running.**

Daemon threads should therefore generally be used for background/support work rather than work that must always finish.

The lecture code describes daemon threads as background-running threads that stop once the main/user work has completed. fileciteturn2file1L23-L28

---

# 25. Garbage Collection and Daemon Threads

The lecture uses:

```text
Garbage Collection → daemon thread
```

as an example of background JVM activity. fileciteturn2file1L23-L28

For interview preparation, remember the conceptual association:

> **Daemon threads perform background/support work and do not keep the JVM alive by themselves.**

---

# 26. Important Rule for `setDaemon()`

Daemon status should be configured **before starting the thread**:

```java
Thread t = new Thread(task);

t.setDaemon(true);

t.start();
```

---

# 27. Thread Methods — Quick Comparison

| Method | Main Purpose | Typical State/Effect |
|---|---|---|
| `sleep()` | Pause current thread | `TIMED_WAITING` |
| `join()` | Wait for another thread | Calling thread can be `WAITING` |
| `join(timeout)` | Wait for another thread for limited time | Timed waiting behavior |
| `yield()` | Scheduling hint | Remains eligible to run |
| `interrupt()` | Request interruption | Signals target thread |
| `isInterrupted()` | Check a thread's interrupt status | Boolean; does not clear |
| `interrupted()` | Check current thread's status | Boolean; clears status |
| `isAlive()` | Check whether started and not terminated | Boolean |
| `currentThread()` | Get currently executing thread | Returns Thread reference |
| `setName()` | Set thread name | Changes identity label |
| `setPriority()` | Set scheduling priority | 1–10 |
| `setDaemon()` | Mark as daemon | Set before `start()` |

---

# 28. `sleep()` vs `join()`

```text
sleep()
→ "I need a break."

join()
→ "I need to wait for THAT thread."
```

### Example

```java
Thread.sleep(2000);
```

The current thread pauses for a period.

```java
t1.join();
```

The current thread waits for `t1`.

---

# 29. `sleep()` vs `yield()`

```text
sleep()
→ definite timed pause
→ TIMED_WAITING

yield()
→ scheduling hint
→ remains eligible to run
→ OS may ignore it
```

---

# 30. `interrupt()` vs Forced Termination

A useful interview concept:

> **`interrupt()` is cooperative; it is not a forced kill.**

The target thread should respond to the interruption in a controlled way.

This is why patterns such as:

```java
while (!Thread.currentThread().interrupted()) {
    // work
}
```

can be used for graceful cancellation. fileciteturn2file5L18-L30

---

# 31. Practical Example — Graceful Worker

```java
class Worker implements Runnable {

    @Override
    public void run() {
        while (!Thread.currentThread().isInterrupted()) {
            System.out.println("Working...");
        }

        System.out.println("Stopping gracefully");
    }
}
```

Then:

```java
Thread worker = new Thread(new Worker(), "worker-1");

worker.start();

// Later...
worker.interrupt();
```

Conceptually:

```text
worker starts
    ↓
does work
    ↓
interrupt() requested
    ↓
worker notices it
    ↓
loop exits
    ↓
worker terminates
```

---

# 32. Practical Example — Waiting for Completion

```java
Thread worker = new Thread(() -> {
    System.out.println("Worker started");

    try {
        Thread.sleep(2000);
    } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
    }

    System.out.println("Worker finished");
});

worker.start();

worker.join();

System.out.println("Main continues");
```

Conceptually:

```text
Main
 ↓
start worker
 ↓
join()
 ↓
WAITING

Worker
 ↓
work
 ↓
TERMINATED

Main
 ↓
RUNNABLE
 ↓
continues
```

---

# 33. Important Interview Traps

### Trap 1 — Does `sleep()` release the lock?

**No.**

### Trap 2 — Does `yield()` guarantee another thread will run?

**No.** It is a scheduling hint.

### Trap 3 — Does `interrupt()` forcibly kill the thread?

**No.** It is cooperative.

### Trap 4 — Does `join()` make the target thread wait?

**No.** The calling thread waits for the target thread.

### Trap 5 — Does `isAlive()` mean the thread is currently executing?

**No.** It means the thread has started and has not yet terminated.

### Trap 6 — Does priority guarantee execution order?

**No.** It is a scheduling hint.

### Trap 7 — Can a daemon thread keep the JVM alive?

**No.** Daemon threads do not prevent JVM termination once all user threads finish.

### Trap 8 — Are `Thread.interrupted()` and `isInterrupted()` the same?

**No.** `interrupted()` checks the current thread and clears the status; `isInterrupted()` checks a thread and does not clear it.

---

# 34. Lecture 3 — Master Mental Model

Think of the important methods as categories:

```text
CONTROL / PAUSE
├── sleep()
└── yield()

WAIT FOR OTHER THREAD
└── join()

CANCELLATION / INTERRUPTION
├── interrupt()
├── isInterrupted()
└── interrupted()

IDENTITY / DEBUGGING
├── currentThread()
├── getName()
└── setName()

LIFE CHECK
└── isAlive()

SCHEDULING HINT
└── setPriority()

BACKGROUND EXECUTION
└── setDaemon()
```

---

# 35. Lecture 3 — One-Minute Revision

> **`sleep()` pauses the current thread for a specified duration and puts it into `TIMED_WAITING`; it does not release monitors. `join()` makes the calling thread wait for another thread to finish, while `join(timeout)` limits the wait. `yield()` is only a scheduling hint and the OS may ignore it. `interrupt()` is a cooperative request for interruption, not a forced thread kill. `isInterrupted()` checks a thread's interrupt status without clearing it, while `Thread.interrupted()` checks the current thread and clears its interrupt status. `isAlive()` checks whether a thread has started and not yet terminated. `currentThread()` returns the current thread, and `setName()` helps identify threads. Thread priority ranges from 1 to 10, with 5 as normal, but priority does not guarantee execution order. Daemon threads are background/support threads and do not keep the JVM alive once all user threads finish.**

---

# ⭐ Lecture 3 — Must Remember

```text
1. sleep() → pause current thread → TIMED_WAITING

2. sleep() → DOES NOT release monitor locks

3. join() → calling thread waits for target thread

4. yield() → scheduling hint, NOT guarantee

5. interrupt() → cooperative interruption request

6. interrupt() ≠ forcibly kill thread

7. isInterrupted() → check status, don't clear

8. interrupted() → check CURRENT thread + clear status

9. isAlive() → started && not terminated

10. currentThread() → current executing thread

11. Priority:
    MIN = 1
    NORM = 5
    MAX = 10

12. Priority is NOT a guaranteed execution order

13. Daemon = background/support thread

14. JVM can terminate when no user threads remain

15. Meaningful thread names help debugging
```

---

# 🔥 Final Lecture 3 Chain

```text
Thread
│
├── sleep()
│      ↓
│   TIMED_WAITING
│
├── join()
│      ↓
│   calling thread waits
│
├── yield()
│      ↓
│   scheduling hint
│
├── interrupt()
│      ↓
│   cooperative cancellation/interruption
│
├── isInterrupted()
│      ↓
│   inspect interrupt status
│
├── interrupted()
│      ↓
│   inspect current status + clear
│
├── isAlive()
│      ↓
│   started && not terminated
│
├── currentThread()
│      ↓
│   current executing thread
│
├── setName()
│      ↓
│   easier debugging/logging
│
├── setPriority()
│      ↓
│   scheduling priority hint
│
└── setDaemon()
       ↓
    background thread
```

## Lecture 3 Core Principle

> **Thread methods don't all "control a thread" in the same way. Some pause it, some make another thread wait, some provide scheduling hints, some request interruption, and some simply inspect or configure the thread.**
