# Java Multithreading — Lecture 2 Notes

## Thread Creation, Runnable, start() vs run(), Thread Identity & Thread Lifecycle

These notes are based on Lecture 2 and organized for quick revision, conceptual understanding, and fresher interview preparation.

---

# 1. What Is a Thread?

A thread represents an **independent path of execution inside a Java program**.

There are two related concepts:

```text
Thread Object
    ↓
Java object used to configure/control a thread

Thread of Execution
    ↓
Independent sequence of instructions executed by JVM
```

Creating a `Thread` object does **not** immediately start an independently executing thread. The new thread begins when `start()` is called.

---

# 2. Two Fundamental Ways to Create a Thread

### Approach 1 — Extend `Thread`

```java
class MyThread extends Thread {

    @Override
    public void run() {
        System.out.println("Thread is running");
    }
}
```

```java
MyThread t1 = new MyThread();
t1.start();
```

### Approach 2 — Implement `Runnable`

```java
class MyRunnable implements Runnable {

    @Override
    public void run() {
        System.out.println("Thread is running using Runnable");
    }
}
```

```java
MyRunnable task = new MyRunnable();
Thread t1 = new Thread(task);
t1.start();
```

---

# 3. Which Approach Is Preferred?

For normal application code, **implementing `Runnable` is generally preferred**.

The main mental model is:

```text
Runnable → WHAT work should be done

Thread   → HOW/WHICH thread executes that work
```

This is **separation of concerns**.

---

# 4. Why `Runnable` Is Better

## Reason 1 — Separation of Concerns

`Runnable` defines the work while `Thread` controls execution and lifecycle. This makes code easier to understand, test, reuse, and later move to executor/thread-pool designs.

## Reason 2 — Java Has Single Class Inheritance

Java classes extend only one class. Therefore:

```java
class ReportTask extends Thread {
}
```

uses the class inheritance opportunity.

With `Runnable`:

```java
class ReportTask extends BaseReport implements Runnable {

    @Override
    public void run() {
        System.out.println("Generating report");
    }
}
```

The class can still extend another class.

> **Interview answer:** Implementing `Runnable` preserves the ability to extend another class because `Runnable` is an interface.

Do not describe this as multiple class inheritance.

---

# 5. Reusability of `Runnable`

The same task can be given to multiple threads:

```java
Runnable task = new MyRunnable();

Thread t1 = new Thread(task, "Worker-1");
Thread t2 = new Thread(task, "Worker-2");

t1.start();
t2.start();
```

Both threads execute the same task.

### Important warning

If the same `Runnable` object contains mutable fields, those fields are shared by the threads.

```java
class CounterTask implements Runnable {

    private int count;

    @Override
    public void run() {
        count++;
    }
}
```

This shared mutable state can later lead to **race conditions**.

---

# 6. `Runnable` and Lambda Expressions

`Runnable` is a **functional interface** because it has one abstract method:

```java
@FunctionalInterface
public interface Runnable {
    void run();
}
```

Traditional form:

```java
Runnable task = new Runnable() {
    @Override
    public void run() {
        System.out.println("Hello");
    }
};
```

Lambda form:

```java
Runnable task = () -> {
    System.out.println("Hello");
};
```

---

# 7. Shortest Way to Create a Thread

```java
Thread worker = new Thread(
    () -> System.out.println("Hello")
);

worker.start();
```

Or:

```java
new Thread(() -> System.out.println("Hello")).start();
```

---

# 8. Understanding `Thread` Internally

Think:

```text
Runnable
   ↓
Task

Thread
   ↓
Execution mechanism
```

Conceptually:

```text
MyRunnable object
       ↓
   represents task
       ↓
Thread object
       ↓
   represents execution
```

When you write:

```java
Thread t1 = new Thread(task);
```

the `Thread` stores the `Runnable` task. Then:

```java
t1.start();
```

causes a new thread to begin execution and eventually invoke `run()`.

Conceptual flow:

```text
start()
   ↓
new thread begins
   ↓
Thread.run()
   ↓
task.run()
```

---

# 9. `Thread` Itself Implements `Runnable`

An important Java detail:

```java
public class Thread
    extends Object
    implements Runnable
```

Therefore `Thread` itself has a `run()` method.

---

# 10. `start()` vs `run()` — MOST IMPORTANT

## `run()`

```java
Thread worker = new Thread(() -> {
    System.out.println(
        Thread.currentThread().getName()
    );
}, "Worker-1");

worker.run();
```

No new thread is created. `run()` is a normal method call and executes on the calling thread.

```text
main thread
    ↓
worker.run()
    ↓
main thread executes run()
```

## `start()`

```java
worker.start();
```

Conceptually:

```text
main thread
    ↓
worker.start()
    ↓
JVM starts a new thread
    ↓
new thread invokes run()
```

### Golden Rule

> **`run()` = normal method call**  
> **`start()` = starts independent thread execution**

---

# 11. `run()` vs `start()` Comparison

| `run()` | `start()` |
|---|---|
| Normal method call | Starts independent thread execution |
| Runs on calling thread | `run()` executes on new thread |
| Does not create a new execution stack | New thread gets its own execution stack |
| Can be called like a normal method | A thread object can be started only once |
| Does not provide concurrent execution by itself | Enables concurrent execution |

---

# 12. Visualizing the Difference

### Calling `run()`

```text
Main Thread
     │
     ▼
worker.run()
     │
     ▼
Task executes here
```

### Calling `start()`

```text
Main Thread
     │
     └── start()
             │
             ▼
        Worker Thread
             │
             ▼
          run()
```

---

# 13. Thread Identity

Threads have useful properties such as:

- name
- thread ID
- state
- priority
- daemon status
- interrupted status

---

# 14. `Thread.currentThread()`

To get the thread whose code is currently executing:

```java
Thread.currentThread();
```

Example:

```java
Thread current = Thread.currentThread();
System.out.println(current.getName());
```

Inside `main()`, the current thread is normally named `main`.

---

# 15. Getting Thread Name

```java
String name = Thread.currentThread().getName();
System.out.println(name);
```

---

# 16. Setting a Thread Name

```java
worker.setName("Worker-Thread");
```

Or through the constructor:

```java
Thread worker = new Thread(() -> {
    System.out.println(
        Thread.currentThread().getName()
    );
}, "Worker-Thread");
```

Meaningful names help with debugging, logging, thread dumps, monitoring, and diagnosing concurrent problems.

---

# 17. Thread ID

Modern Java provides:

```java
long id = Thread.currentThread().threadId();
System.out.println("ID: " + id);
```

Older code commonly uses:

```java
getId()
```

but `getId()` is deprecated since Java 19. A numeric thread ID is useful for identification/debugging, but program logic should not normally depend on a specific ID.

---

# 18. Thread Execution Order Is NOT Guaranteed

```java
Thread t1 = new Thread(
    () -> System.out.println("Thread-1")
);

Thread t2 = new Thread(
    () -> System.out.println("Thread-2")
);

t1.start();
t2.start();
```

Possible output:

```text
Thread-1
Thread-2
```

or:

```text
Thread-2
Thread-1
```

Starting `t1` first only makes it eligible for scheduling first; it does not guarantee that it will execute or finish first.

---

# 19. Why Is Execution Order Unpredictable?

It can be influenced by:

- OS scheduling
- JVM scheduling decisions
- number of CPU cores
- current system load
- other running processes
- thread priority
- blocking operations
- lock contention
- I/O operations
- timing of thread creation

Therefore:

> **Never write application logic that depends on accidental scheduling order.**

---

# 20. Non-Determinism

Multithreaded execution is often **non-deterministic**:

```text
Same program
+
Same input
+
Different scheduling
        ↓
Potentially different output order
```

Non-deterministic output order does **not automatically mean the program is wrong**. It becomes a correctness problem when the final result depends on uncontrolled execution order.

---

# 21. If We Need a Specific Order

Common coordination mechanisms include:

```text
join()
locks
wait() / notifyAll()
latches
barriers
semaphores
blocking queues
executors / futures
```

---

# 22. One Thread Object Can Be Started Only Once

```java
Thread worker = new Thread(() -> {
    System.out.println("Running");
});

worker.start();
worker.start();   // ❌
```

The second call throws:

```text
java.lang.IllegalThreadStateException
```

---

# 23. Why Can't We Restart a Thread?

A `Thread` object represents one lifecycle:

```text
NEW
 ↓
started
 ↓
active states
 ↓
TERMINATED
```

After termination, that lifecycle is complete.

To execute the same task again, create a **new Thread object**:

```java
Runnable task = () -> {
    System.out.println("Running");
};

Thread first = new Thread(task);
first.start();

Thread second = new Thread(task);
second.start();
```

The task can be reused, but the terminated `Thread` object cannot be restarted.

---

# 24. Thread Lifecycle

Java defines six official thread states using:

```java
Thread.State
```

The six states are:

```text
NEW
RUNNABLE
BLOCKED
WAITING
TIMED_WAITING
TERMINATED
```

---

# 25. Lifecycle Overview

```text
                start()
                   ↓
                 NEW
                   ↓
               RUNNABLE
              ↙    ↓     ↘
         BLOCKED WAITING TIMED_WAITING
              ↘    ↓     ↙
                RUNNABLE
                   ↓
               TERMINATED
```

A thread may move among the active and waiting states multiple times before termination.

---

# 26. State 1 — NEW

A thread is in `NEW` when the `Thread` object has been created but `start()` has not been called.

```java
Thread worker = new Thread(() -> {
    System.out.println("Running");
});
```

At this point:

```java
worker.getState()
```

returns:

```text
NEW
```

### Mental model

> **Object exists, but independent thread execution has not started.**

---

# 27. State 2 — RUNNABLE

After:

```java
worker.start();
```

the thread normally enters `RUNNABLE`.

### Important Java-specific detail

Java does **not** expose a separate public `RUNNING` state in `Thread.State`.

`RUNNABLE` includes both:

```text
Ready to receive processor time
        +
Currently executing
```

Therefore:

> A thread reported as `RUNNABLE` is not necessarily executing on the CPU at that exact instant.

---

# 28. State 3 — BLOCKED

A thread becomes `BLOCKED` when waiting to acquire an **intrinsic monitor** needed to enter or re-enter synchronized code.

Example:

```java
class SharedResource {

    synchronized void use() {
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
```

If one thread owns the monitor and another tries to enter the same synchronized method, the second thread may become:

```text
BLOCKED
```

### Mental model

> **BLOCKED = "I need a monitor to enter synchronized code, but another thread currently owns it."**

---

# 29. BLOCKED Does Not Mean "Releases All Locks"

A blocked thread is waiting to acquire a particular monitor. It has not acquired that requested monitor yet.

It does **not** automatically release every other monitor it may already hold.

---

# 30. State 4 — WAITING

A thread enters `WAITING` when it waits **indefinitely for another thread/event/action**.

Common causes:

```java
object.wait();
thread.join();
LockSupport.park();
```

---

# 31. `wait()` and WAITING

Example:

```java
synchronized (lock) {
    lock.wait();
}
```

Conceptually:

```text
holds lock
   ↓
wait()
   ↓
releases that lock's monitor
   ↓
WAITING
```

It can later be awakened through:

```java
lock.notify();
lock.notifyAll();
```

After notification, it must reacquire the same monitor before returning from `wait()`.

---

# 32. `join()` and WAITING

```java
worker.join();
```

The current thread waits until `worker` terminates.

It is not waiting for `notify()`.

---

# 33. WAITING Does Not Always Mean `notify()`

Different APIs resume a waiting thread in different ways:

```text
Object.wait()
    → notification, interruption, or permitted spurious wakeup

Thread.join()
    → target thread termination or interruption

LockSupport.park()
    → unpark, interruption, or permitted spurious return
```

### Mental model

> **WAITING = indefinite coordination wait**

---

# 34. WAITING and Lock Release

Calling:

```java
wait()
```

does not automatically release all locks.

```java
synchronized (lock) {
    lock.wait();
}
```

It releases **the monitor of `lock`**. It does not automatically release unrelated monitors held by that thread.

---

# 35. State 5 — TIMED_WAITING

`TIMED_WAITING` means a thread waits for another event/action **for a limited amount of time**.

Examples:

```java
Thread.sleep(2000);
object.wait(2000);
thread.join(2000);
```

Also:

```java
LockSupport.parkNanos(...);
LockSupport.parkUntil(...);
```

### Mental model

> **TIMED_WAITING = limited-duration wait/delay**

---

# 36. `sleep()` vs `wait()` — Very Important

Both can produce `TIMED_WAITING`, but their lock behavior differs.

### `Thread.sleep()`

```java
Thread.sleep(2000);
```

→ does **not** release monitors.

### `Object.wait(timeout)`

```java
object.wait(2000);
```

→ releases the monitor of that object.

The thread later has to reacquire it before continuing.

---

# 37. State 6 — TERMINATED

A thread enters `TERMINATED` after its execution finishes.

This happens when:

```text
run() completes normally
```

or:

```text
run() ends because of an uncaught exception
```

Example:

```java
Thread worker = new Thread(() -> {
    System.out.println("Done");
});

worker.start();
worker.join();

System.out.println(worker.getState());
```

Output:

```text
TERMINATED
```

---

# 38. TERMINATED → Cannot Restart

Once a thread is `TERMINATED`, calling:

```java
worker.start();
```

again throws:

```text
IllegalThreadStateException
```

Create another `Thread` object instead.

---

# 39. BLOCKED vs WAITING vs TIMED_WAITING

| State | Main reason | Typical example | How it continues |
|---|---|---|---|
| `BLOCKED` | Waiting to acquire intrinsic monitor | Entering occupied `synchronized` code | Acquires monitor |
| `WAITING` | Waiting indefinitely | `wait()`, `join()`, `park()` | Event / notification / termination / etc. |
| `TIMED_WAITING` | Waiting with time limit | `sleep()`, timed `wait()`, timed `join()` | Event / interruption / timeout |

### Easy memory trick

```text
BLOCKED
→ LOCK problem

WAITING
→ INDEFINITE coordination

TIMED_WAITING
→ LIMITED waiting
```

---

# 40. Complete Lifecycle

```text
             new Thread()
                  ↓
                 NEW
                  │
               start()
                  ↓
              RUNNABLE
             ↙    ↓     ↘
        BLOCKED  WAITING  TIMED_WAITING
             ↘     ↓      ↙
               RUNNABLE
                  ↓
             run() ends
                  ↓
             TERMINATED
```

---

# 41. Important Point About `getState()`

```java
worker.start();
System.out.println(worker.getState());
```

You may expect `RUNNABLE`, but it is not guaranteed.

The state immediately after `start()` is timing-sensitive. If the task is very short, it may already be `TERMINATED` when inspected.

> **`getState()` returns a snapshot of the thread's state at that moment.**

---

# 42. Example: `TIMED_WAITING`

```java
Thread worker = new Thread(() -> {
    try {
        Thread.sleep(5000);
    } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
    }
});

worker.start();
Thread.sleep(500);
System.out.println(worker.getState());
worker.join();
```

The worker will most likely be `TIMED_WAITING` while sleeping.

---

# 43. Example: `BLOCKED`

Conceptually:

```text
Thread-1
   ↓
acquires lock
   ↓
sleep(5000)

Thread-2
   ↓
tries same synchronized section
   ↓
BLOCKED
```

`Thread-2` is waiting for the monitor currently owned by `Thread-1`.

---

# 44. Example: `WAITING`

A thread can do:

```java
synchronized (lock) {
    lock.wait();
}
```

Another thread can later do:

```java
synchronized (lock) {
    lock.notifyAll();
}
```

The first thread is in `WAITING` before it is notified.

---

# 45. Full Thread Creation Mental Model

```text
TASK
 ↓
Runnable
 ↓
Thread
 ↓
start()
 ↓
NEW → RUNNABLE
 ↓
execution
 ↓
possibly:
BLOCKED / WAITING / TIMED_WAITING
 ↓
RUNNABLE
 ↓
TERMINATED
```

---

# 46. `Thread` vs `Runnable`

| Extending `Thread` | Implementing `Runnable` |
|---|---|
| Combines task + thread | Separates task from execution |
| Uses class inheritance | Uses interface |
| Cannot extend another class | Can still extend another class |
| Task is tied more closely to thread | Same task can be reused |
| Less natural with executors | Works naturally with executor-based design |
| Less commonly preferred | Usually preferred |

---

# 47. Code Patterns You Should Know

## A. Extend Thread

```java
class MyThread extends Thread {

    @Override
    public void run() {
        System.out.println("Task");
    }
}

public class Main {

    public static void main(String[] args) {
        new MyThread().start();
    }
}
```

## B. Implement Runnable

```java
class MyTask implements Runnable {

    @Override
    public void run() {
        System.out.println("Task");
    }
}

public class Main {

    public static void main(String[] args) {
        Runnable task = new MyTask();
        new Thread(task).start();
    }
}
```

## C. Lambda

```java
new Thread(
    () -> System.out.println("Task")
).start();
```

---

# 48. One Task → Multiple Threads

```java
Runnable task = () -> {
    System.out.println(
        "Running in: "
        + Thread.currentThread().getName()
    );
};

Thread t1 = new Thread(task, "Worker-1");
Thread t2 = new Thread(task, "Worker-2");

t1.start();
t2.start();
```

Possible outputs can appear in either order because scheduling is not guaranteed.

---

# 🧠 The Most Important Mental Models of Lecture 2

## 1. Task vs Thread

```text
Runnable = TASK
Thread   = EXECUTOR
```

## 2. `run()` vs `start()`

```text
run()
→ normal method
→ current thread

start()
→ new thread execution
→ then run()
```

## 3. Lifecycle

```text
NEW
 ↓ start()
RUNNABLE
 ↓
BLOCKED / WAITING / TIMED_WAITING
 ↓
RUNNABLE
 ↓
TERMINATED
```

## 4. Shared task

```text
            Runnable task
             /        \
            /          \
        Thread 1      Thread 2
            \          /
             \        /
          same object/state
```

Shared mutable state can become dangerous.

---

# 🎤 Fresher Interview Questions

### 1. How can you create a thread in Java?

Two foundational ways:

- extend `Thread`
- implement `Runnable`

Higher-level APIs also exist, but `Thread` and `Runnable` are the foundation.

### 2. Why is `Runnable` generally preferred over extending `Thread`?

It separates the task from thread execution, preserves class inheritance, improves reuse, and fits naturally with executor-based APIs.

### 3. What is the difference between `start()` and `run()`?

`run()` is a normal method call and executes on the calling thread.

`start()` begins independent thread execution, after which the new thread executes `run()`.

### 4. Can we call `start()` twice?

No. The second call throws `IllegalThreadStateException`.

### 5. Can a terminated thread be restarted?

No. Create a new `Thread` object.

### 6. What is a functional interface?

An interface with one abstract method. `Runnable` qualifies because its abstract method is `run()`.

### 7. Why can thread output order change?

Because thread scheduling is not deterministic.

### 8. What is `Thread.currentThread()`?

It returns the thread whose code is currently executing.

### 9. Why should we give meaningful thread names?

They improve logging, debugging, thread dumps, monitoring, and diagnosis.

### 10. What are the six Java thread states?

```text
NEW
RUNNABLE
BLOCKED
WAITING
TIMED_WAITING
TERMINATED
```

### 11. Does Java have a separate `RUNNING` state?

No. Java's `RUNNABLE` includes both ready-to-run and currently-executing situations.

### 12. Difference between `BLOCKED` and `WAITING`?

```text
BLOCKED
→ waiting to acquire a monitor

WAITING
→ waiting indefinitely for another action/event
```

### 13. Difference between `WAITING` and `TIMED_WAITING`?

```text
WAITING
→ no time limit

TIMED_WAITING
→ time limit exists
```

### 14. Does `sleep()` release a lock?

No.

### 15. Does `wait()` release the lock?

It releases the monitor of the object on which `wait()` is called, then the thread must reacquire it before returning.

---

# ⚠️ Common Beginner Mistakes

### Mistake 1

```java
worker.run();
```

thinking a new thread was created.

**Incorrect.** `run()` does not create a new thread.

### Mistake 2

```java
t1.start();
t2.start();
```

thinking `t1` will always execute first.

**Incorrect.** Start order ≠ execution order.

### Mistake 3

Thinking:

```text
RUNNABLE = definitely running on CPU
```

**Incorrect.** `RUNNABLE` includes both ready and executing situations.

### Mistake 4

Thinking:

```text
WAITING = waiting for notify()
```

**Incorrect.** `join()` and `LockSupport.park()` can also result in `WAITING`.

### Mistake 5

Thinking:

```text
sleep() releases lock
```

**Incorrect.** `sleep()` does not release monitors.

### Mistake 6

Thinking a blocked/waiting thread automatically releases every lock it owns.

**Incorrect.** Lock behavior depends on the operation that caused the state.

---

# ⭐ Lecture 2 — 12 Things You Must Remember

```text
1. Thread object ≠ thread of execution.

2. Creating Thread object puts it in NEW.

3. start() begins independent thread execution.

4. run() called directly is just a normal method call.

5. Runnable represents the task.

6. Thread represents the execution mechanism.

7. Runnable is generally preferred because of separation of concerns,
   inheritance flexibility and reuse.

8. Runnable is a functional interface, so lambda expressions work.

9. Thread execution order is not guaranteed.

10. A Thread object can be started only once.

11. Java has six states:
    NEW, RUNNABLE, BLOCKED, WAITING,
    TIMED_WAITING, TERMINATED.

12. BLOCKED = monitor acquisition,
    WAITING = indefinite coordination,
    TIMED_WAITING = limited wait.
```

---

# 🔥 One-Minute Revision

> **I can create a Java thread by extending `Thread` or implementing `Runnable`. `Runnable` is generally preferred because it separates the task from the execution mechanism and preserves inheritance flexibility. Since `Runnable` is a functional interface, I can use a lambda. Creating a `Thread` object puts it in `NEW`; calling `run()` directly does not create a new thread, while calling `start()` starts independent thread execution. Thread execution order is not guaranteed. A thread moves through Java's six states — `NEW`, `RUNNABLE`, `BLOCKED`, `WAITING`, `TIMED_WAITING`, and `TERMINATED`. `BLOCKED` means waiting for a monitor, `WAITING` means indefinite coordination wait, and `TIMED_WAITING` means a wait with a time limit. A thread object cannot be restarted after it has been started/terminated.**

---

# Lecture 2 Core Chain

```text
Task
 ↓
Runnable
 ↓
Thread
 ↓
start()
 ↓
NEW
 ↓
RUNNABLE
 ↓
BLOCKED / WAITING / TIMED_WAITING
 ↓
RUNNABLE
 ↓
TERMINATED
```
