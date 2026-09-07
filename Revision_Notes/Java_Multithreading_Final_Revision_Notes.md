# Java Multithreading — Final Revision Notes (Lectures 1 → 11)

> Combined, deduplicated, and re-ordered **beginner → advanced** from Lectures 1–11.
> ⭐ marks the points most frequently asked in **fresher interviews**.
> Use Part J (Cheat Sheet), Part K (Comparisons), Part L (Concept Map) and Part M (Rapid-Fire Q&A) for last-minute revision.

---

## Table of Contents

- **Part A — Foundations:** Program, Process, Thread, JVM Memory, Concurrency vs Parallelism
- **Part B — Working With Threads:** Creation, `start()` vs `run()`, Lifecycle, Essential Methods
- **Part C — Why Threads Go Wrong:** Race Conditions, Atomicity, Visibility, Ordering
- **Part D — Synchronization & Monitors:** the `synchronized` keyword
- **Part E — Inter-Thread Communication:** `wait()`, `notify()`, `notifyAll()`, Producer–Consumer
- **Part F — Explicit Locks & Advanced Locking:** `Lock`, `ReentrantLock`, `ReadWriteLock`, `StampedLock`, `Semaphore`, `Condition`
- **Part G — Lock-Free Concurrency:** Atomic Variables, CAS, CAS Retry Loop, ABA Problem
- **Part H — Executor Framework:** Thread Pools, `ExecutorService`, `Future`, `ThreadPoolExecutor`
- **Part I — Advanced Tools:** `CompletableFuture`, `ForkJoinPool`, `ThreadLocal`, Virtual Threads
- **Part J — Final Quick-Revision Cheat Sheet**
- **Part K — Important Comparisons (All Tables)**
- **Part L — Complete Concept Map**
- **Part M — Fresher Interview Rapid-Fire Q&A**

**The whole course in one chain:**

```text
Program → Process → Thread → Shared Memory → Race Conditions
        → synchronized → wait/notify → explicit Locks
        → atomics & CAS → Executor Framework / thread pools
        → CompletableFuture / ForkJoinPool / Virtual Threads
```

---

# PART A — FOUNDATIONS (Lecture 1)

## A1. Why Multithreading?

> ⭐ **Multithreading is not simply a technique for making a program faster.**

It allows multiple execution paths/tasks to make progress within one application (e.g., an IDE accepts typing while doing error checking and background work). Actual speed improvement depends on the workload and whether work can truly execute in parallel.

## A2. The Big Picture — How Java Code Executes

```text
Java Source Code
       ↓
     javac
       ↓
Java Bytecode (.class)
       ↓
   java Main
       ↓
Operating-System Process
       ↓
      JVM
       ↓
    Main Thread
       ↓
 Additional Threads
       ↓
    CPU Cores → Execution
```

> **A program becomes active as a process, and the actual work inside that process is performed by threads.**

Technical detail: the CPU does **not** execute Java bytecode directly. Bytecode is interpreted or JIT-compiled into **native machine instructions**, which the CPU executes. Also, OS memory concepts and JVM memory concepts are related but **not identical abstractions** (the JVM Method Area ≠ OS "code segment").

## A3. Program vs Process vs Thread

| Concept | Meaning |
|---|---|
| **Program** | Passive set of instructions stored in a file (`Demo.java` / `Demo.class`) — not executing |
| **Process** | Running instance of a program — has its own virtual address space |
| **Thread** | Independent path of execution **inside** a process |

Mental model:

```text
Program = what to execute
Process = running environment
Thread  = who/what executes the work
```

⭐ **Interview definition:** *"A program is a passive set of instructions stored in a file, whereas a process is a running instance of that program."*

**Interview trap:** never say "a process is a bigger thread." Correct: *"A process is an independent execution environment, while a thread is an execution path within that process."*

## A4. What a Process Gets From the OS

- **Memory:** virtual address space (code, heap, thread stacks, loaded libraries, memory-mapped files)
- **CPU time:** the OS schedules the process's threads
- **Other resources:** files, sockets, I/O, DB connections, shared libraries, OS handles

Each process normally has its **own address space** → strong isolation; if one process crashes, others continue. Processes communicate via **IPC** (pipes, sockets, shared memory, files, message queues, remote calls).

## A5. What a Thread Has

A thread owns its **execution state**:

- Program counter (PC)
- Call stack + stack frames
- Local variables
- Current instruction position

### ⭐ Why "lightweight process"?

Threads are cheaper to create/switch than processes, share process memory, and communicate through shared memory easily. **But a thread is NOT a process** — it has no independent address space and belongs to exactly one process.

### ⭐ Process vs Thread (interview favorite)

| Process | Thread |
|---|---|
| Running instance of a program | Execution path inside a process |
| Has its own address space | Shares process address space |
| Contains one or more threads | Belongs to one process |
| More expensive to create/switch | Cheaper to create/switch |
| Communication via IPC | Communication via shared memory |
| Stronger isolation | Lower isolation |

## A6. The Main Thread & JVM Startup Flow

When `java Main` runs: the OS starts a process → the JVM initializes **inside that process** (loads/verifies classes, prepares runtime memory) → the JVM creates the **main thread** → `main()` executes on it → additional threads may be created.

> `main()` does not execute independently — it executes **inside the main thread**.

Full sequence (memorize the order):

```text
1. Write Main.java → 2. javac → 3. Main.class (bytecode)
4. java Main → 5. OS starts a process → 6. JVM initializes
7. Classes loaded/verified → 8. Runtime memory prepared
9. main() invoked on the main thread → 10. Additional threads created
11. Scheduler assigns runnable threads to cores
12. JVM interprets/JIT-compiles → 13. CPU executes native instructions
```

## A7. ⭐ JVM Memory: Shared vs Thread-Private (MOST IMPORTANT)

```text
                 ONE JAVA PROCESS / JVM
┌─────────────────────────────────────────────┐
│   SHARED                    PRIVATE         │
│                                             │
│   ┌───────────┐              ┌─────────┐    │
│   │   Heap    │              │Stack T1 │    │
│   └───────────┘              └─────────┘    │
│   ┌───────────┐              ┌─────────┐    │
│   │ Method    │              │Stack T2 │    │
│   │ Area      │              └─────────┘    │
│   └───────────┘              PC T1, PC T2    │
└─────────────────────────────────────────────┘
```

| Memory | Shared or Private | Stores |
|---|---|---|
| **Heap** | Shared by all threads | Objects, arrays (`new Student()`) |
| **Method Area / Metaspace** | Shared | Class metadata, method bytecode, runtime constant pools, static-field class data |
| **Java Stack** | Private per thread | Stack frames: local variables, operand stack, return info |
| **PC Register** | Private per thread | Current/next JVM instruction for that thread |
| **Native Method Stack** | Private per thread | Native method execution state |

⭐ **The one-liner to remember:** *"Threads share process memory (heap objects, class data), but each thread has its own Java stack and PC register."* This shared heap is exactly why **race conditions, synchronization, locks, and thread safety** exist.

**Fine point:** *Method Area* is a JVM **specification concept**; *Metaspace* is HotSpot's **implementation** of class metadata storage. Don't blindly equate them.

## A8. Single Core, Context Switching, Concurrency & Parallelism

**Context switch** = CPU stops executing one thread, saves its state (registers, instruction pointer, stack pointer), selects another thread, restores its state. **Context switching is not free** — too many threads can hurt performance.

⭐ **Concurrency:** multiple tasks make progress during overlapping time periods — **possible on a single core** via interleaving.

⭐ **Parallelism:** multiple tasks execute **simultaneously on different cores/hardware units**.

| Concurrency | Parallelism |
|---|---|
| Overlapping progress, interleaved | Simultaneous execution |
| Can happen on one core | Requires multiple cores |
| About *managing* many things | About *doing* many things at once |
| Doesn't guarantee speed | Can improve throughput for parallelizable work |

A program can be **concurrent but not parallel** (one core, switching) or **concurrent and parallel** (multi-core).

## A9. Multitasking vs Multithreading

| Multitasking (OS concept) | Multithreading |
|---|---|
| Multiple processes/applications (Chrome + VS Code + Spotify) | Multiple threads inside one process |
| Separate address spaces | Shared address space |
| Heavier communication (IPC) | Easy shared-memory communication |
| Stronger isolation | Lower isolation |
| Expensive process creation | Cheaper thread creation |

**Real-world picture** (a Spring Boot app): one JVM process containing main thread + web-server threads + DB-pool threads + GC threads + worker threads — several request threads may run the *same controller code* while sharing heap objects. That is why **thread safety matters in servers**.

### ⭐ Lecture 1 — 10 things you must remember

```text
1. Program = stored/passive instructions.
2. Process = running instance of a program.
3. Thread = execution path inside a process.
4. A process contains one or more threads.
5. Java apps have a main thread that executes main().
6. Threads share process memory, especially heap objects.
7. Each Java thread has its own stack and PC register.
8. One core → threads interleave (concurrency via context switching).
9. Multiple cores → threads can run in parallel.
10. Multitasking = many processes; Multithreading = many threads in one process.
```

---

# PART B — WORKING WITH THREADS (Lectures 2–3)

## B1. Thread Object vs Thread of Execution

```text
Thread Object        → Java object used to configure/control a thread
Thread of Execution  → independent sequence of instructions run by the JVM
```

Creating a `Thread` object does **not** start execution — the new thread begins only when `start()` is called. A new `Thread` is in state **NEW**.

## B2. Two Ways to Create a Thread ⭐

```java
// Approach 1 — extend Thread
class MyThread extends Thread {
    @Override
    public void run() {
        System.out.println("Thread is running");
    }
}
new MyThread().start();

// Approach 2 — implement Runnable  (generally preferred)
class MyRunnable implements Runnable {
    @Override
    public void run() {
        System.out.println("Running via Runnable");
    }
}
Runnable task = new MyRunnable();
new Thread(task).start();
```

### Why `Runnable` is preferred ⭐

```text
Runnable → WHAT work should be done   (the task)
Thread   → HOW/WHICH thread executes it (the executor)
```

1. **Separation of concerns** — task separate from execution; easier to test, reuse, and move to thread pools.
2. **Single class inheritance** — `class ReportTask extends BaseReport implements Runnable` still works; extending `Thread` burns the one inheritance slot. *(Say "preserves inheritance via interface", NOT "multiple inheritance".)*
3. **Reusability** — the same task can go to many threads.
4. **Fits executors** — executor APIs are built around `Runnable`/`Callable`.

### Reuse warning

```java
class CounterTask implements Runnable {
    private int count;              // shared mutable state!
    public void run() { count++; }
}
```

One `Runnable` given to two threads → its fields are **shared** → future race condition.

### Runnable is a functional interface → lambda

```java
Runnable task = () -> System.out.println("Hello");
new Thread(task, "Worker-1").start();
// shortest form:
new Thread(() -> System.out.println("Hello")).start();
```

Internal flow: `start()` → new thread begins → `Thread.run()` → `task.run()` (because `Thread` itself `implements Runnable` and stores the task).

| Extending `Thread` | Implementing `Runnable` |
|---|---|
| Combines task + thread | Separates task from execution |
| Uses class inheritance | Uses interface |
| Cannot extend another class | Can still extend another class |
| Task tied to thread | Task reusable across threads |
| Less natural with executors | Works naturally with executors |

## B3. ⭐ `start()` vs `run()` — THE most important L2 point

```java
worker.run();    // ❌ normal method call — executes on the CALLING thread, no new thread
worker.start();  // ✅ JVM starts a NEW thread, which then invokes run()
```

| `run()` | `start()` |
|---|---|
| Normal method call | Starts independent thread execution |
| Runs on calling thread (e.g., main) | `run()` executes on the new thread |
| No new execution stack | New thread gets its own stack |
| Callable like any method | A thread object can be started **only once** |
| No concurrency by itself | Enables concurrency |

```text
Calling run():  Main → worker.run()          (main executes the body)
Calling start(): Main → start() → new Worker Thread → run()
```

## B4. Thread Identity

```java
Thread current = Thread.currentThread();     // currently executing thread
current.getName(); current.setName("worker-1");
long id = current.threadId();                // getId() deprecated since Java 19
```

Meaningful names (`payment-worker-1`, not `Thread-1`) help debugging, logging, thread dumps, and monitoring. Threads also have state, priority, daemon status, and an interrupted status.

## B5. ⭐ Execution Order Is NOT Guaranteed

```java
t1.start(); t2.start();   // t1 first ≠ t1 executes/finishes first
```

Output order depends on OS/JVM scheduling, core count, load, priorities, blocking, lock contention, I/O, and creation timing. Multithreaded output is **non-deterministic** — same program + same input can produce different interleavings; that alone doesn't mean the program is wrong (it's wrong when the *result* depends on uncontrolled ordering).

To force order use coordination: `join()`, locks, `wait()/notifyAll()`, latches, barriers, semaphores, blocking queues, executors/futures.

## B6. One Start Per Thread

```java
worker.start();
worker.start();   // ❌ IllegalThreadStateException
```

A `Thread` object represents **one lifecycle** (NEW → … → TERMINATED). To run the same task again, create a **new Thread** around the same `Runnable` (task is reusable; the terminated thread is not).

## B7. ⭐ Thread Lifecycle — Six States (`Thread.State`)

```text
             new Thread()
                  ↓
                 NEW
                  │ start()
                  ↓
              RUNNABLE
             ↙    ↓     ↘
        BLOCKED  WAITING  TIMED_WAITING
             ↘     ↓      ↙
               RUNNABLE
                  ↓ run() ends (normally or via exception)
             TERMINATED
```

| State | Meaning | Typical cause | How it continues |
|---|---|---|---|
| `NEW` | Object created, `start()` not called | `new Thread(...)` | `start()` |
| `RUNNABLE` | Ready **or** currently executing | after `start()` | scheduled by OS |
| `BLOCKED` | Waiting to acquire an **intrinsic monitor** | entering occupied `synchronized` code | acquires monitor |
| `WAITING` | Indefinite coordination wait | `wait()`, `join()`, `LockSupport.park()` | notify / target terminates / unpark / interrupt |
| `TIMED_WAITING` | Wait with time limit | `sleep(ms)`, `wait(ms)`, `join(ms)`, `parkNanos` | timeout / event / interrupt |
| `TERMINATED` | Execution finished | `run()` completed or threw | cannot restart |

⭐ Key interview facts:

- **Java has no separate `RUNNING` state** — `RUNNABLE` covers both ready-to-run and currently-executing, so a `RUNNABLE` thread is not guaranteed to be on the CPU at that instant.
- **BLOCKED = lock problem; WAITING = indefinite coordination; TIMED_WAITING = limited wait.**
- A `BLOCKED`/`WAITING` thread does **not** automatically release *other* monitors it holds. `wait()` releases only the monitor of the object waited on.
- `WAITING` is not only about `notify()` — `join()` and `park()` also produce it.
- `getState()` returns a **snapshot**; right after `start()` the state is timing-sensitive (a very short task may already be `TERMINATED`).

```java
// TIMED_WAITING example
Thread worker = new Thread(() -> {
    try { Thread.sleep(5000); }
    catch (InterruptedException e) { Thread.currentThread().interrupt(); }
});
worker.start();
Thread.sleep(500);
System.out.println(worker.getState());   // TIMED_WAITING
worker.join();
```

## B8. Essential Thread Methods (Lecture 3)

```text
CONTROL/PAUSE      sleep(), yield()
WAIT FOR OTHER     join()
INTERRUPTION       interrupt(), isInterrupted(), interrupted()
IDENTITY/DEBUG     currentThread(), getName(), setName()
LIFE CHECK         isAlive()
SCHEDULING HINT    setPriority()
BACKGROUND         setDaemon()
```

### `Thread.sleep(ms)` ⭐

Pauses the **current** thread: `RUNNABLE → TIMED_WAITING → RUNNABLE`.

> ⭐ **`sleep()` does NOT release monitor locks.** A sleeping thread keeps holding any locks it owns.

```java
try {
    Thread.sleep(2000);
} catch (InterruptedException e) {
    Thread.currentThread().interrupt();
}
```

### `join()` / `join(timeout)` ⭐

Makes the **calling** thread wait until the target thread terminates (the *caller* waits, not the target — caller enters `WAITING`; `join(1000)` waits at most 1s).

```java
t1.start();
t1.join();          // main waits for t1 to finish
System.out.println("Main continues");
```

### `yield()`

A **hint** to the scheduler: "I'm willing to give up CPU to another runnable thread." The OS **may ignore it**; the thread **remains RUNNABLE** (no WAITING/BLOCKED transition). Not a guaranteed context switch.

### ⭐ Interruption — cooperative, not a kill

> ⭐ **`interrupt()` is a cooperative signal/request — it does NOT forcibly kill a thread.** The target thread decides how to respond.

```java
// Graceful cancellation pattern
class Worker implements Runnable {
    public void run() {
        while (!Thread.currentThread().isInterrupted()) {
            // work...
        }
        System.out.println("Stopping gracefully");
    }
}
Thread worker = new Thread(new Worker(), "worker-1");
worker.start();
// later:
worker.interrupt();
```

**Interrupt status — two different checkers (classic trap):**

| `thread.isInterrupted()` | `Thread.interrupted()` |
|---|---|
| Instance method | Static method |
| Checks THAT thread's status | Checks CURRENT thread's status |
| Does **not** clear the flag | **Clears** the flag after checking |

`InterruptedException` is thrown by blocking methods — `sleep()`, `join()`, `wait()` — when interrupted. The standard catch-block pattern **re-interrupts** to preserve the cancellation signal for higher-level code:

```java
catch (InterruptedException e) {
    Thread.currentThread().interrupt();
}
```

### `isAlive()`

`true` only between `start()` and termination. It does **not** tell you whether the thread is on the CPU, RUNNABLE, BLOCKED, or WAITING.

```java
System.out.println(t1.isAlive()); // false (NEW)
t1.start();
System.out.println(t1.isAlive()); // true
t1.join();
System.out.println(t1.isAlive()); // false
```

### Thread Priority

```java
Thread.MIN_PRIORITY   // 1
Thread.NORM_PRIORITY  // 5 (default)
Thread.MAX_PRIORITY   // 10
```

> ⭐ Priority is a **scheduling hint**, not a guarantee. The OS may respect, partially respect, or ignore it. Never build correctness on "priority 10 runs before priority 5."

### Daemon Threads

```java
Thread t = new Thread(() -> { while (true) { /* background work */ } });
t.setDaemon(true);   // MUST be set BEFORE start()
t.start();
```

> ⭐ **The JVM can terminate when no user (non-daemon) threads remain, even if daemon threads are still running.** Daemon threads do background/support work (e.g., Garbage Collection) and **cannot keep the JVM alive** by themselves. Don't use them for work that must always finish.

### Method quick table

| Method | Purpose | State/effect |
|---|---|---|
| `sleep(ms)` | Pause current thread | `TIMED_WAITING`; keeps locks |
| `join()` / `join(ms)` | Caller waits for target | caller `WAITING` / timed |
| `yield()` | Scheduling hint | stays `RUNNABLE`; ignorable |
| `interrupt()` | Cooperative interruption request | sets interrupt status / wakes blocked methods |
| `isInterrupted()` | Check a thread's status | boolean; doesn't clear |
| `interrupted()` | Check current thread's status | boolean; clears |
| `isAlive()` | Started && not terminated | boolean |
| `currentThread()` | Currently executing thread | Thread reference |
| `setName()` | Meaningful name | debugging aid |
| `setPriority()` | 1–10 hint | no guarantee |
| `setDaemon()` | Background thread | set before `start()` |

### ⭐ Lecture 3 traps (all "No!")

```text
sleep() releases the lock?            NO
yield() guarantees another thread?    NO (hint only)
interrupt() forcibly kills?           NO (cooperative)
join() makes the TARGET wait?         NO (caller waits)
isAlive() means running on CPU?       NO (started && not terminated)
Priority guarantees order?            NO
Daemon keeps JVM alive?               NO
interrupted() == isInterrupted()?     NO (clears vs doesn't)
```

---

# PART C — WHY THREADS GO WRONG (Lecture 4)

## C1. The Central Cause Chain

```text
Shared mutable state
        +
Multiple threads
        +
Missing coordination
        ↓
Unsafe interleaving
        ↓
Race conditions / Thread interference
        ↓
Lost updates · Stale reads · Ordering failures · Partial updates
        ↓
Data inconsistency
```

The three guarantees to fix this:

```text
Atomicity  → Can another thread interfere halfway?
Visibility → Can another thread see the latest value?
Ordering   → In what order are writes/operations observed?
```

## C2. Shared Resource & Critical Section

**Shared resource** = data/object accessible by more than one thread (heap objects, static fields, shared collections, files, DB connections). It becomes dangerous when it is **SHARED + MUTABLE** — immutable data is much easier to share safely.

**Critical section** = the code that reads/writes shared mutable state and needs controlled access:

```java
class Counter {
    int count = 0;
    void increment() {
        count++;          // ← critical section (shared resource = count)
    }
}

// check + update TOGETHER form one logical critical section:
if (balance >= amount) {
    balance = balance - amount;
}
```

## C3. ⭐ Race Condition & the `count++` Problem

> ⭐ **Race condition: the correctness of the program depends on unpredictable thread timing/execution order.**

`count++` looks like one statement but is conceptually **read → modify → write** — so it is **NOT atomic**:

```text
count = 0; two threads increment:

Step | Thread-1              | Thread-2
  1  | reads count = 0       |
  2  |                       | reads count = 0
  3  | calculates 1          |
  4  |                       | calculates 1
  5  | writes 1              |
  6  |                       | writes 1

Final count = 1  ← LOST UPDATE (expected 2)
```

Classic demo (result varies run to run — hard to reproduce/debug):

```java
class Counter {
    int count = 0;
    void increment() { count++; }
}

Thread t1 = new Thread(() -> { for (int i = 0; i < 100_000; i++) counter.increment(); });
Thread t2 = new Thread(() -> { for (int i = 0; i < 100_000; i++) counter.increment(); });
t1.start(); t2.start(); t1.join(); t2.join();
System.out.println(counter.count);   // expected 200000, maybe 173482
```

A race can even produce the correct answer sometimes and a wrong answer other times.

## C4. Atomicity ⭐

> **An operation is atomic when it appears to other threads as one indivisible action.**

**Not atomic:**

```java
count++; count--;              // read-modify-write
x += 5;                        // compound assignment
if (balance > 0) withdraw();   // check-then-act
if (!map.containsKey(k)) map.put(k, v);  // check-then-put (use putIfAbsent)
```

**Check-Then-Act failure** (both threads decide from the old balance):

```text
balance = 1000; two threads withdraw 800:
T1 checks → sufficient;  T2 checks → sufficient
T1 deducts 800;          T2 deducts 800   → both decided on stale data
```

**Money-transfer example:** deduct from A, credit B — if only step 1 completes, the system is inconsistent. A logical transfer must not expose a partially completed state.

**Atomic vs non-atomic fine points:**

- Simple reads/writes of most primitives and references **are** atomic (no half-`int`, no half-reference observed).
- ⭐ Per the JLS, a non-volatile `long`/`double` read/write **may** be treated as two 32-bit operations; declare them `volatile` to guarantee atomic reads/writes of the variable itself.
- But `volatile long value; value++;` is **still not atomic** (read + add + write).
- ⭐ **Atomicity alone does not guarantee visibility** — an atomic write may still not be observed by another thread without a happens-before relationship.

## C5. Visibility ⭐

> **Visibility problem: one thread updates shared data, but another thread does not reliably observe the update.**

```java
class VisibilityDemo {
    static boolean flag = false;          // change to: static volatile boolean flag

    public static void main(String[] args) {
        Thread updater = new Thread(() -> {
            sleep(1000); flag = true;               // write happens
        });
        Thread observer = new Thread(() -> {
            while (!flag) { /* may spin forever */ }
            System.out.println("Detected flag change");  // NOT guaranteed without volatile
        });
        updater.start(); observer.start();
    }
}
```

### `volatile` ⭐ (one of the most-tested keywords)

```java
static volatile boolean flag = false;
```

**Provides:**

1. **Visibility** — a volatile write *happens-before* a subsequent volatile read of the same variable.
2. **Ordering** guarantees around the volatile access.
3. **Atomic read/write of the variable itself**.

**Does NOT provide:**

- Mutual exclusion.
- Atomicity of compound operations — ⭐ **`volatile` does NOT make `count++` atomic.**

Best for simple state communication: `volatile boolean running;`, `volatile boolean shutdownRequested;`.

⚠ **Interview warning:** don't explain volatile as "forces reads/writes directly to RAM." That's a beginner mental model. The JMM defines visibility via **happens-before** relationships.

### Atomicity vs Visibility (different problems!)

```text
Atomicity  → "Can another thread interfere halfway through this logical operation?"
Visibility → "Can another thread see my latest write?"

int x = 10;  // atomic write — but other threads aren't guaranteed to SEE it
```

## C6. Ordering ⭐

```java
int data = 0;
boolean ready = false;            // make ready volatile for the guarantee

// Thread-1
data = 10; ready = true;

// Thread-2
if (ready) System.out.println(data);   // may print 0!
```

Without a happens-before relationship, observing `ready == true` does not guarantee observing `data = 10` — a stale `ready=true, data=0` observation is possible (visibility effects, reordering, or both).

**Why reordering happens:** compiler, JVM, and CPU optimize (instruction scheduling, register allocation, pipelining, store buffering) — allowed as long as single-threaded semantics are preserved.

**Fix with `volatile` (safe publication pattern):**

```java
int data = 0;
volatile boolean ready = false;   // volatile write of ready orders the write to data
```

**Fix with `synchronized` (same monitor on both sides):**

```java
synchronized (lock) { data = 10; ready = true; }   // writer
synchronized (lock) { if (ready) use(data); }      // reader
```

⭐ Both threads must coordinate on the **same monitor** — one synchronized thread does not protect others that bypass synchronization.

## C7. Thread Interference & Data Inconsistency

**Thread interference** = operations from multiple threads interleave so one thread disturbs another's work (ingredients: shared mutable state + non-atomic operation + missing synchronization + unsafe interleaving).

> **Race condition** = the broader correctness problem (result depends on timing).
> **Thread interference** = the harmful overlap itself. A lost update can be both.

**Data inconsistency** = state violates expected rules. Common forms:

```text
Lost update             → two updates, one overwrites the other (count++)
Stale read              → thread keeps using an older value
Inconsistent read       → related fields seen from different logical moments
Partial update          → money deducted from A, not yet credited to B
Check-then-act failure  → acted on a condition another thread already changed
Unsafe publication      → reference received before object state is safely visible
```

## C8. Tools to Prevent These Problems

| Requirement | Suitable mechanism |
|---|---|
| Simple visible status flag | `volatile` |
| Atomic counter update | `AtomicInteger` |
| Several statements atomic together | `synchronized` or `Lock` |
| Concurrent map access | `ConcurrentHashMap` |
| Producer–consumer | `BlockingQueue` / wait-notify |
| State should never change | Immutable object |
| Avoid sharing entirely | Thread confinement / message passing |

```java
// synchronized
count++;  →  synchronized (lock) { count++; }

// atomic class
AtomicInteger count = new AtomicInteger();
count.incrementAndGet();

// explicit lock
lock.lock();
try { count++; } finally { lock.unlock(); }

// immutability — removes synchronization needs by design
final class User {
    private final String name;
    private final int age;
    User(String name, int age) { this.name = name; this.age = age; }
}
```

Thread-safe collections (`ConcurrentHashMap`, `CopyOnWriteArrayList`, `BlockingQueue`, `ConcurrentLinkedQueue`) protect **their own operations** — ⭐ a *sequence* of calls may still not be atomic (`containsKey` then `put` → use `putIfAbsent`).

> **The safest shared mutable state is often the state that is not shared** — thread-local variables, method-local variables, message passing, immutable snapshots, task confinement.

## C9. ⭐ `volatile` vs `synchronized` vs `AtomicInteger`

| Feature | `volatile` | `synchronized` | `AtomicInteger` |
|---|---|---|---|
| Visibility | ✅ | ✅ | ✅ |
| Ordering guarantees | ✅ around volatile access | ✅ | ✅ via atomic operations |
| `count++` atomic | ❌ | ✅ when protected | ✅ `incrementAndGet()` |
| Mutual exclusion | ❌ | ✅ | ❌ (not a lock) |
| Best for | Simple shared flags/state | Larger critical sections | Simple atomic numeric ops |

## C10. Lecture 4 must-remembers

```text
1.  Shared mutable state is the major source of multithreading problems.
2.  Critical section = code accessing shared mutable data.
3.  count++ is NOT atomic (read-modify-write).
4.  Race condition = result depends on unpredictable timing/order.
5.  Atomicity = operation cannot be interfered with halfway.
6.  Visibility = latest write is reliably observable by other threads.
7.  Ordering = operations observed in the required sequence.
8.  volatile → visibility + ordering; NOT mutual exclusion; NOT count++ atomicity.
9.  synchronized → mutual exclusion + visibility + ordering.
10. AtomicInteger → atomic counter/state operations.
11. Thread-safe collections protect single operations, not multi-step sequences.
12. Immutable objects reduce synchronization needs.
13. Reduce shared mutable state before adding locks.
14. Rely on JMM happens-before guarantees, not cache/timing assumptions.
15. All threads must follow the SAME synchronization policy.
```

---

# PART D — SYNCHRONIZATION & MONITORS (Lecture 5)

## D1. Why Synchronize?

```java
class Counter {
    private int count = 0;
    synchronized void increment() { count++; }
}
```

> **Synchronization coordinates concurrent access to shared mutable data.** It provides:

```text
1. Protection of shared data
2. Mutual exclusion / atomic logical operation within the protected region
3. Memory visibility
4. Ordering / happens-before guarantees
```

**Mutual exclusion:** `Thread-1` acquires monitor → enters; `Thread-2` requesting the same monitor → `BLOCKED` until release.

**Atomicity via synchronization:** `count++` is still read-modify-write internally, but because the whole logical operation is protected by one monitor, cooperating threads cannot interleave into it — so the *logical* operation behaves atomically (this is not hardware-level atomicity of each instruction).

**Visibility via synchronization:** writes made before monitor **release** become visible to a thread that later **acquires** the same monitor (happens-before via monitor operations).

⚠ **Not "stop the world":** synchronization only restricts code guarded by *that monitor*. Other threads still run unrelated code, use different monitors, and do other work.

## D2. Monitor / Object Lock

**Every Java object can act as a monitor** (managed by the JVM, not a normal field):

```java
Object lock = new Object();
synchronized (lock) {
    // protected code
}
```

Acquisition flow: try acquire → available? acquire & enter : BLOCKED → owner exits → monitor released → another thread acquires. Acquisition/release is handled atomically by the JVM, and the monitor is **released automatically** when the region exits — **including via an exception**.

**Conceptual monitor internals** (teaching model, not exact JVM layout):

```text
Monitor { owner, holdCount, entrySet (waiting-to-acquire), waitSet (wait() waiters) }
```

## D3. ⭐ Which Lock Is Used? (top interview question)

```text
synchronized instance method → monitor = this
synchronized static method   → monitor = ClassName.class
synchronized block           → monitor = the object inside parentheses
```

```java
// synchronized instance method ≡
void show() { synchronized (this) { ... } }

// synchronized static method ≡  (no 'this' in static context)
static void increment() { synchronized (Counter.class) { ... } }
```

- Two synchronized instance methods on the **same object** → same monitor → only one at a time (even different methods `m1`/`m2`).
- Two different objects (`first.m1()`, `second.m1()`) → different monitors → **can run concurrently**.
- Instance synchronized method and static synchronized method use **different monitors** (`this` vs `Class.class`) → they do **not** block each other.
- Static synchronized coordinates across **all instances** through the single class monitor.

> ⭐ **Synchronization depends on the monitor object, not the method name or class.**

## D4. Method vs Block, Custom Locks

```java
// Whole method protected (coarse)
synchronized void increment() { count++; }

// Only critical section (fine-grained → less contention)
void increment() {
    System.out.println("Before");
    synchronized (this) { count++; }
    System.out.println("After");
}
```

But don't shrink blindly — the **complete logical operation** (e.g., check + update) must stay inside one synchronized region:

```java
synchronized (lock) {
    if (balance >= amount) { balance -= amount; }   // check+update together
}
```

**Custom lock object (recommended form):**

```java
class Counter {
    private final Object lock = new Object();
    private int count = 0;
    void increment() {
        synchronized (lock) { count++; }   // monitor = lock, not this
    }
}
```

`private` stops external code locking on it; `final` prevents the reference being replaced. Multiple locks per object are fine **only for independent state**:

```java
class AccountManager {
    private final Object accountLock = new Object();
    private final Object auditLock = new Object();
    void updateBalance(int amount) { synchronized (accountLock) { balance += amount; } }
    void recordAuditEntry()        { synchronized (auditLock)   { auditEntries++; } }
}
```

### ⭐ DANGER — different locks protecting the SAME data

```java
// WRONG: deposit/withdraw both touch 'balance' but use different monitors
// → they can run concurrently and corrupt balance
class Bank {
    private final Object depositLock = new Object();
    private final Object withdrawLock = new Object();
    ...
}

// CORRECT: one consistency rule → one lock
private final Object balanceLock = new Object();
```

> ⭐ **Golden rule: data participating in the same consistency rule must be protected by the same lock.**

## D5. Classic `synchronized` Mistakes

```java
synchronized (new Object()) { count++; }   // ❌ every call → new monitor → no mutual exclusion

private Object lock = new Object();
void changeLock() { lock = new Object(); } // ❌ threads end up on different monitors → use final

public final Object lock = new Object();   // ❌ external code can lock on it → keep private

if (balance >= amount) {
    synchronized (lock) { balance -= amount; }  // ❌ check outside monitor → include the check
}
```

## D6. Reentrancy

Java monitors are **reentrant** — a thread already owning a monitor can acquire it again (hold count increments) without self-blocking:

```java
class Service {
    synchronized void methodA() { methodB(); }   // works: same thread re-enters
    synchronized void methodB() { System.out.println("Inside methodB"); }
}
```

## D7. `synchronized` vs `volatile` (recap from Part C)

```text
volatile     → visibility + ordering + atomic reads/writes of the variable itself; NO mutual exclusion; count++ still unsafe
synchronized → mutual exclusion + visibility + ordering; protects larger logical critical sections
```

They are **not interchangeable**.

## D8. Performance & Contention

A lock held too long makes T2, T3, T4 wait. Rule: **protect the complete logical critical section, but keep unrelated work outside the lock.**

### Lecture 5 must-remembers

```text
1.  synchronized protects shared mutable state via object monitors.
2.  Every Java object can be a monitor.
3.  instance method → this; static method → ClassName.class; block → the given object.
4.  Different objects → different monitors → may run concurrently.
5.  Same monitor → mutual exclusion.
6.  synchronized gives mutual exclusion + visibility + ordering.
7.  synchronized(new Object()) is wrong for shared-state protection.
8.  Custom locks: private final.
9.  Multiple locks only for independent state; one consistency rule → one lock.
10. Monitors are reentrant; released automatically on block/method exit (even on exception).
11. Synchronized blocks give finer-grained control than whole-method locking.
```

---

# PART E — INTER-THREAD COMMUNICATION (Lecture 6)

## E1. Why Communication Beyond Locks?

Synchronization controls **who enters** a critical section; a thread also needs to know **when a condition becomes true** (consumer waits for an item; producer waits for a free slot).

```text
Shared Resource → Condition → thread can't continue yet → wait()
→ releases monitor + WAITING → another thread changes condition
→ notify()/notifyAll() → waiter eligible → reacquire monitor → re-check condition → continue
```

## E2. Producer–Consumer & Busy Waiting

```text
Producer → Shared Box/Buffer → Consumer
Condition: box has item  OR  box is empty
```

Without coordination: consumer may consume nothing (runs first); producer may overwrite an unconsumed item.

**Busy waiting** = repeatedly checking a condition in a loop:

```java
while (flag == false) { /* keep checking — burns CPU, does no useful work */ }
```

`wait()` instead parks the thread (no CPU burn) until notification.

## E3. ⭐ `wait()` / `notify()` / `notifyAll()`

They are **methods of `Object`** (not `Thread`) because they operate on the **monitor associated with an object**.

| Method | Belongs to | Purpose | Monitor requirement | Effect |
|---|---|---|---|---|
| `wait()` | `Object` | Wait for a condition | must own object's monitor | releases that monitor → `WAITING` |
| `notify()` | `Object` | Signal ONE waiter | must own monitor | one waiter becomes eligible to compete |
| `notifyAll()` | `Object` | Signal ALL waiters | must own monitor | all waiters become eligible (still one monitor owner at a time) |

```java
synchronized (lock) {
    while (!condition) {
        lock.wait();          // releases lock's monitor, WAITING
    }
    // condition true → work
    lock.notifyAll();         // signal others (still holding monitor until block exits)
}
```

Key semantics:

- ⭐ **`wait()` releases the monitor of the object waited on** (essential — otherwise no other thread could ever enter and change the condition). It does not release unrelated monitors the thread holds.
- Calling `wait()/notify()/notifyAll()` **without owning that monitor** → `IllegalMonitorStateException`.
- **Notification ≠ lock transfer.** The notifier keeps the monitor until its synchronized region exits; the awakened thread must **reacquire** the monitor before continuing past `wait()` (it may briefly be `BLOCKED` while competing).
- `notify()` selects one waiter — **which one is unspecified**; `notifyAll()` wakes all — **not simultaneous execution**, only one owns the monitor at a time.
- `notifyAll()` is often safer with multiple waiters/conditions (each re-checks its own guard) — but ⭐ it is **not** a deadlock-prevention mechanism.
- `wait()` does not terminate the thread: RUNNABLE → wait() → WAITING → notify/interrupt/spurious → reacquire → RUNNABLE.

## E4. ⭐ Why `while` and not `if` around `wait()`

> ⭐ A waiting thread can wake **without the condition being true** — a **spurious wakeup** — and with multiple threads, another thread may consume/alter the shared state before the awakened thread reacquires the monitor.

```java
// Guarded block pattern — ALWAYS:
synchronized (lock) {
    while (!condition) {   // while, never if
        lock.wait();
    }
    // safe: condition holds now
}
```

Wake up → **do not assume the condition is true → re-check.**

## E5. ⭐ `wait()` vs `sleep()` (top-3 interview question)

| `wait()` | `sleep()` |
|---|---|
| Method of `Object` | Static method of `Thread` |
| Purpose: **coordination** on a condition | Purpose: **timed delay** |
| **Releases** the object's monitor | **Does not** release monitors |
| Must be called inside `synchronized` (owning the monitor) | No monitor requirement |
| `WAITING` (or timed variant) | `TIMED_WAITING` |
| Resumes on notify / interrupt / spurious wakeup | Resumes on timeout / interrupt |

Memory trick:

```text
sleep() → "I want a timed break."      (keeps locks)
wait()  → "I need to wait for a condition/event." (releases the lock)
```

## E6. Producer–Consumer with `wait`/`notify` (single-item Box)

```java
class Box {
    Integer item;
    boolean flag = false;                       // false = empty, true = full

    synchronized void producer(int value) throws InterruptedException {
        while (flag == true) {                  // box full → wait
            wait();
        }
        item = value;
        flag = true;
        System.out.println("Producer produces " + item);
        notify();                               // wake the consumer
    }

    synchronized void consumer() throws InterruptedException {
        while (flag == false) {                 // box empty → wait
            wait();
        }
        System.out.println("Consumer consumes " + item);
        item = null;
        flag = false;
        notify();                               // wake the producer
    }
}
```

Flow per side:

```text
Producer: enter monitor → full? wait (release+WAITING) → consumer consumes & notifies
          → reacquire → re-check flag → produce → notify
Consumer: enter monitor → empty? wait → producer produces & notifies
          → reacquire → re-check → consume → notify
```

Real-world analogues: web-request producer → worker consumer; message producer → processor; log producer → writer; document creator → printer.

## E7. Deadlock Preview

Deadlock = threads wait indefinitely on locks held by each other:

```text
T1 holds Lock-A, waits for Lock-B
T2 holds Lock-B, waits for Lock-A   → cycle → nobody progresses
```

(Detailed discussion with the advanced locks in Part F.)

### Lecture 6 must-remembers

```text
1.  wait/notify/notifyAll belong to Object (monitor-based coordination).
2.  They must be called while owning that object's monitor, else IllegalMonitorStateException.
3.  wait() releases the monitor of that object and enters WAITING.
4.  Notification does not transfer the monitor; the awakened thread must reacquire it.
5.  notify() → one waiter (unspecified which); notifyAll() → all waiters (one owner at a time).
6.  Always re-check the condition after waking → use while, not if (spurious wakeups).
7.  wait() ≠ sleep() (releases monitor vs keeps monitors; coordination vs delay).
8.  Busy waiting wastes CPU; blocking coordination is preferable here.
9.  notifyAll() does not automatically prevent deadlocks.
10. Synchronization protects the resource; communication tells threads WHEN to wait and retry.
```

---

# PART F — EXPLICIT LOCKS & ADVANCED LOCKING (Lecture 7)

## F1. Why Go Beyond `synchronized`?

`synchronized` is simple and safe (automatic acquisition/release), but advanced use cases need more:

```text
1. Less explicit control over acquisition/release
2. No tryLock()-style attempt
3. Limited fairness control
4. Contention with coarse locking
5. Specialized strategies (read-heavy, N permits, multiple condition queues)
```

> `synchronized` is not bad — explicit locks add **control when needed**.

## F2. The `Lock` Interface — Golden Rule ⭐

```java
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

class Resource {
    Lock lock = new ReentrantLock();

    void f1() {
        lock.lock();
        try {
            // critical section
        } finally {
            lock.unlock();      // ⭐ ALWAYS unlock in finally
        }
    }
}
```

> ⭐ **Whenever you manually acquire a lock, release it in `finally`** — otherwise an exception can leave it held forever and block every other thread. (Unlike `synchronized`, release is NOT automatic.)

## F3. `ReentrantLock`

**Reentrant** = the same thread can acquire the same lock multiple times without blocking itself (hold count tracks acquisitions; unlock decrements; lock frees at count 0):

```text
T1 lock() → hold 1 → T1 lock() again → hold 2 → T1 unlock() → 1 → T1 unlock() → 0 → available
```

Useful APIs: `lock()`, `unlock()`, `tryLock()`, `tryLock(timeout, unit)`, `isLocked()`, `isHeldByCurrentThread()`, `getHoldCount()`, `isFair()`.

### `tryLock()` ⭐

```java
if (lock.tryLock()) {                 // "Can I get it?" — no indefinite wait
    try { /* work */ } finally { lock.unlock(); }
} else {
    // handle failure — do something else
}

if (lock.tryLock(2, TimeUnit.SECONDS)) { /* timed attempt */ } else { /* timeout path */ }

lock()    → "Wait until I get it."
tryLock() → "Can I get it?"
```

### Fairness

```java
ReentrantLock lock = new ReentrantLock(true);   // fair
```

Fair locks give waiting threads more predictable, order-based access → **reduces starvation risk but may reduce throughput**. Unfair (default) → usually higher throughput, higher starvation risk.

**Starvation** = a thread repeatedly fails to get enough access to a resource to make progress while others keep acquiring it.

| `synchronized` | `Lock` |
|---|---|
| Simpler, automatic monitor release | Explicit control, manual `unlock()` |
| No `tryLock()` | `tryLock()` + timed attempts |
| No explicit fairness | Fairness configurable |
| Great for straightforward critical sections | Advanced requirements |

## F4. `ReadWriteLock` / `ReentrantReadWriteLock`

For **read-heavy** resources (config caches, metadata, shared lookups), one exclusive lock makes readers wait on readers unnecessarily.

```java
ReadWriteLock rwLock = new ReentrantReadWriteLock();
Lock readLock = rwLock.readLock();     // SHARED
Lock writeLock = rwLock.writeLock();   // EXCLUSIVE

public int read(int key) {
    readLock.lock();
    try { return value; } finally { readLock.unlock(); }
}
public void write(int newValue) {
    writeLock.lock();
    try { value = newValue; } finally { writeLock.unlock(); }
}
```

```text
R1 ─┐
R2 ─┼─ READ LOCK → allowed together      R + R → ✅
R3 ─┘                                    R + W → ❌
W1 ── WRITE LOCK → exclusive             W + W → ❌
```

Use when: read-heavy + infrequent writes + real benefit from read concurrency. Frequent writes → the extra complexity buys little.

### Lock Downgrading

Acquire the read lock **while still holding** the write lock, then release the write lock — no gap where another writer could slip in:

```java
writeLock.lock();
try {
    // update state
    readLock.lock();
    try { /* read consistent state */ } finally { readLock.unlock(); }
} finally {
    writeLock.unlock();
}
```

## F5. `StampedLock` & Optimistic Reading

`StampedLock` supports **write lock, read lock, and optimistic read**. Operations return a `long` **stamp** used for unlocking/validation:

```java
StampedLock lock = new StampedLock();

// Write
long stamp = lock.writeLock();
try { value = newValue; } finally { lock.unlockWrite(stamp); }

// Optimistic read — read first, validate after
long stamp = lock.tryOptimisticRead();
int currentValue = value;                       // read without full lock
if (!lock.validate(stamp)) {                    // a write happened → fall back
    stamp = lock.readLock();
    try { currentValue = value; } finally { lock.unlockRead(stamp); }
}
```

```text
Pessimistic read → lock first, then read        (simpler, more contention)
Optimistic read  → read first, then validate    (fallback to read lock if invalid)
                    "Probably nobody changed the data while I was reading."
```

⚠ **`StampedLock` is NOT generally reentrant** — don't assume `ReentrantLock`-like behavior.

| `ReentrantReadWriteLock` | `StampedLock` |
|---|---|
| Read + write locks | Read + write + optimistic read |
| Reentrant | Not generally reentrant |
| Familiar lock API | Stamp-based API |
| Easier to reason about | More advanced; great when optimistic reads usually succeed |

## F6. `Semaphore`

A lock = one ownership slot. A **semaphore** = a set of **permits**:

```java
Semaphore semaphore = new Semaphore(3);   // counting semaphore, 3 permits

semaphore.acquire();
try {
    callApi();                            // at most 3 threads inside concurrently
} finally {
    semaphore.release();
}
```

```text
permits=3: T1 acquire→2 · T2 acquire→1 · T3 acquire→0 · T4 acquire→ WAITS
release() → permit freed → a waiting thread acquires
Binary semaphore: permits=1. Counting: permits>1.
```

> ⭐ **Lock answers "Who owns the critical section?" Semaphore answers "How many may enter?"**

Real uses: API concurrency limits, connection pools, limited hardware, bounded concurrent requests.

## F7. `Condition`

`Condition` = `wait/notify`-style coordination for explicit `Lock`s, with a major advantage: **one lock can have multiple condition queues**:

```java
ReentrantLock lock = new ReentrantLock();
Condition notFull  = lock.newCondition();   // bounded buffer
Condition notEmpty = lock.newCondition();

lock.lock();
try {
    while (!conditionIsSatisfied) {         // still while, never if
        condition.await();
    }
    // work
    condition.signal();      // or signalAll()
} finally {
    lock.unlock();
}
```

Mapping: `wait()`→`await()`, `notify()`→`signal()`, `notifyAll()`→`signalAll()` — used **while holding the associated lock**. Multiple conditions let you signal the *relevant* waiting group (producers vs consumers) instead of waking everyone.

## F8. Deadlock & Starvation (formal)

- **Deadlock:** threads permanently blocked in a circular wait (T1 holds A wants B; T2 holds B wants A). Advanced lock APIs do **not** auto-prevent it.
- **Starvation:** one thread keeps losing the race for a resource; others keep winning. `Starvation → one thread may keep losing; Deadlock → threads wait on each other permanently.`
- **Fairness vs performance** is a trade-off (fair → predictable, lower throughput).

## F9. Choosing the Mechanism ⭐

| Need | Use |
|---|---|
| Simple mutual exclusion | `synchronized` |
| tryLock / timeout / fairness | `ReentrantLock` |
| Many readers, few writers | `ReentrantReadWriteLock` |
| Read-heavy + optimistic reads | `StampedLock` |
| Exactly N concurrent accesses | `Semaphore` |
| Multiple condition queues | `Condition` |

### Lecture 7 must-remembers

```text
1.  Lock = explicit acquire/release; unlock() in finally.
2.  ReentrantLock is reentrant; adds tryLock, timed acquisition, optional fairness.
3.  Fairness ↓starvation but can ↓throughput.
4.  ReadWriteLock = shared reads + exclusive writes (R+R ok; R+W, W+W not).
5.  Lock downgrading: take read lock before releasing write lock.
6.  StampedLock adds optimistic read: stamp → read → validate → fallback if invalid.
7.  StampedLock is not reentrant.
8.  Semaphore = permits, not ownership (Semaphore(3) → up to 3 concurrent).
9.  Condition = await/signal/signalAll per lock; multiple condition queues possible.
10. Always guard waits with while; re-check after wake.
11. More power → more responsibility (deadlocks, starvation, forgotten unlocks).
```

---

# PART G — LOCK-FREE CONCURRENCY: ATOMICS & CAS (Lectures 8–9)

## G1. Two Approaches to a Safe Update

```text
Traditional (lock-based) → synchronized / Lock → protect critical section → thread may BLOCK
Lock-free                → atomic variables + CAS → no explicit lock for the operation → RETRY on conflict
```

Lock-free is an alternative for **small state transitions / high-contention counters & flags** — not a claim that it's always faster.

## G2. Atomic Variables

`java.util.concurrent.atomic` provides thread-safe atomic state operations for:

```text
AtomicInteger · AtomicLong · AtomicBoolean · AtomicReference<T>
```

### `AtomicInteger` counter (replaces the racy `count++`)

```java
class Counter {
    AtomicInteger count = new AtomicInteger(0);
    void increment() { count.incrementAndGet(); }   // NOT count++
}

// two threads × 10,000 increments → prints exactly 20000
```

### Key methods

```text
get()  set(v)  getAndSet(v)
incrementAndGet()  getAndIncrement()      decrementAndGet()  getAndDecrement()
addAndGet(d)       getAndAdd(d)
```

⭐ **`incrementAndGet()` vs `getAndIncrement()`** (classic trap):

```text
count = 5:
incrementAndGet() → increment FIRST, return 6   ("increment THEN get")
getAndIncrement() → return 5 FIRST, then increment ("get THEN increment")
addAndGet(5)/getAndAdd(5) follow the same naming pattern.
```

### `AtomicBoolean` vs `volatile boolean` ⭐

```text
volatile boolean → visibility + ordering only (simple flag publication)
AtomicBoolean    → atomic state OPERATIONS, e.g. the one-shot transition:

flag.compareAndSet(false, true);   // if currently false → set true, atomically
```

Good for shutdown flags, one-time transitions, coordination flags.

### `AtomicReference<T>` — atomic reference updates

A plain reference supports read/assign, but "if EMPTY then assign person" needs an atomic **check-and-update**:

```java
AtomicReference<String> seat = new AtomicReference<>("EMPTY");
```

⚠ `AtomicReference<User>` makes **reference updates** atomic — it does **NOT** make the mutable fields inside `User` thread-safe.

### `AtomicReferenceArray<E>` — per-element atomics (L9)

```java
AtomicReferenceArray<String> seats = new AtomicReferenceArray<>(5);
seats.get(0);  seats.set(0, "EMPTY");
seats.compareAndSet(0, "EMPTY", "Aditya");   // atomic per index
```

| `AtomicReference` | `AtomicReferenceArray` |
|---|---|
| One atomic reference | Many independent atomic slots |
| `get/set/compareAndSet` | Same operations per index |
| e.g. active object | e.g. seats/slots |

## G3. ⭐ CAS — Compare-And-Set

> ⭐ **CAS one-liner: "Change the value only if it is still what I expected."**

```java
compareAndSet(expectedValue, newValue)
```

```text
Current == expected?
   ├─ YES → replace with new value ATOMICALLY → success
   └─ NO  → change nothing → failure (another thread won the race)
```

CAS is a **conditional atomic update** supported by low-level hardware/runtime mechanisms — rely on the Java abstraction, not CPU internals.

### Seat booking — why the initial `get()` is not enough

```java
class SeatBooking {
    AtomicReference<String> seat = new AtomicReference<>("EMPTY");

    boolean bookSeat(String name) {
        String currentValue = seat.get();
        if (!currentValue.equals("EMPTY")) return false;
        return seat.compareAndSet("EMPTY", name);   // check+update combined
    }
}
```

```text
T1 → get() → EMPTY
T2 → get() → EMPTY
T2 → CAS(EMPTY, "Rohit")   → SUCCESS
T1 → CAS(EMPTY, "Aditya")  → FAIL   (state changed between get() and CAS)
```

The gap between `get()` and the update is exactly why CAS must combine compare + set in one atomic step.

## G4. ⭐ CAS Retry Loop (the lock-free pattern)

```java
while (true) {
    int current = count.get();          // 1. read latest
    int next = current + 1;             // 2. calculate desired
    if (count.compareAndSet(current, next)) {
        break;                          // 3. success → done
    }
    // 4. failure → someone changed it → loop re-reads and retries
}
```

```text
READ → CALCULATE → CAS → SUCCESS: done
                     └→ FAILURE: read again → recalculate → CAS …
```

Why CAS fails: T1 and T2 both read 10 and compute 11; T1's `CAS(10,11)` succeeds; T2's fails (current=11) → T2 re-reads 11, computes 12, retries.

> **CAS is optimistic** — assume no change, try, retry on conflict.
> **Locking → WAIT; CAS → RETRY.** (Lock: loser blocks. CAS: loser re-reads and retries.)

`incrementAndGet()` itself is conceptually `get() → +1 → CAS(current, next)` with internal retry (implementation is JVM/platform dependent).

## G5. Lock-Based vs Lock-Free

| Lock-Based | CAS / Lock-Free |
|---|---|
| `synchronized`, `Lock` | Atomic classes / CAS |
| Thread may block waiting | Operation fails and retries |
| Mutual exclusion | Atomic conditional update |
| Good for larger critical sections | Good for small state transitions |
| Lock overhead | Retry overhead under contention |

⚠ Under heavy contention a CAS loop can spin: read→calculate→fail→retry, consuming CPU. **Lock-free ≠ zero overhead, ≠ no waiting, ≠ always faster** — it depends on contention, workload, operation size, retry frequency, hardware, and algorithm design.

Related terms: **lock-free** = system-wide progress guaranteed even if individual threads retry; **wait-free** = every operation completes in bounded steps.

## G6. ⭐ Atomic Operation ≠ Atomic Block

> ⭐ **Individual atomic operations do NOT make a sequence of them atomic.**

```java
if (count.get() < 10) {          // atomic read...
    count.incrementAndGet();     // ...atomic increment — but the CHECK+UPDATE pair is NOT atomic
}
// Two threads can both see 9 and both increment → invariant violated.
```

The compound check-then-act needs: a CAS loop, a suitable atomic API, or `synchronized`/`Lock`.

Similarly `AtomicVariable` ≠ everything atomic — only the class's supported atomic operations are.

## G7. ⭐ The ABA Problem (Lecture 9)

```text
Value sequence:  A → B → A

T1 reads A
      T2: A → B, then B → A
T1 re-checks: sees A → value-only CAS concludes "unchanged"
T1 → CAS(A, X) succeeds — but the state DID change in between.
```

A value-only CAS cannot detect the intermediate `B`. In some lock-free algorithms this produces incorrect behavior.

### Solution — Versioning / Stamping

Track state as **(value, version)** where version changes on every modification:

```text
(A,1) → (B,2) → (A,3)

T1 expected (A,1); current is (A,3) → mismatch → CAS fails/retries
```

> **Same value does not necessarily mean same state.** A version is usually a monotonically changing counter/stamp — not a timestamp.

⚠ Don't confuse ABA versioning with `StampedLock` stamps — both use "stamps" but solve different problems (lock/optimistic-read state vs detecting intermediate value changes).

## G8. When to Use What

**Atomic/CAS good fit:** simple counters, flags, single-reference updates, small atomic state machines, high-concurrency small updates.

**Prefer `synchronized`/`Lock` when:** many statements must be protected together, multiple variables form one invariant, business logic is complex, or the operation can't be expressed as one atomic transition:

```java
if (balance >= amount) {
    balance -= amount;
    transactionCount++;
    updateAuditLog();      // larger logical op → lock-based design
}
```

Decision tree:

```text
Shared state? → representable as ONE simple atomic operation?
   ├─ YES → Atomic class / CAS   (+ versioning if ABA matters)
   └─ NO  → synchronized / Lock
```

### Lectures 8–9 must-remembers

```text
1.  count++ is a non-atomic read-modify-write.
2.  Atomic classes: AtomicInteger, AtomicLong, AtomicBoolean, AtomicReference (+AtomicReferenceArray).
3.  incrementAndGet() → new value; getAndIncrement() → old value.
4.  Atomic operations ≠ atomic blocks; check-then-act needs CAS loop or locks.
5.  CAS updates only if current == expected; can fail when another thread moved first.
6.  CAS failure in a loop is normal — retry (it means someone else won the race).
7.  CAS is optimistic: try → fail if changed → retry.
8.  AtomicReference protects the reference, not the referenced object's fields.
9.  ABA = A→B→A undetected by value-only CAS; solve with (value, version) stamping.
10. Lock-free ≠ always faster ≠ no retries; heavy contention burns CPU.
11. Lock-free = system-wide progress; wait-free = bounded per-operation completion.
12. Complex multi-step invariants → synchronized / Lock remain appropriate.
```

---

# PART H — EXECUTOR FRAMEWORK & THREAD POOLS (Lecture 10)

> ⭐ **Core mental shift: don't think "I need to create a thread." Think "I need to submit a task to an execution policy."**

```text
Task → Executor/ExecutorService → Work Queue → Worker Threads → Task Execution → Result/Completion
```

## H1. Why Not One Thread Per Task?

```java
Thread thread = new Thread(() -> processOrder());
thread.start();
```

Problems at scale: thread resource usage (native/JVM/OS resources — don't memorize a fixed "1 MB per thread"), scheduling overhead, context switching, creation/destruction cost, no reuse, uncontrolled growth, hard lifecycle management.

**Thread pool solution:**

```text
Tasks → Queue → Reusable Worker Threads → execute → take next task
Worker-1 → Task 1 → Task 5 → Task 9      (workers are NOT recreated per task)
```

## H2. Executor Hierarchy

```text
Executor                      → execute(Runnable)
   ↓
ExecutorService               → + lifecycle, results, cancellation, bulk ops
   ↓                          → submit, invokeAll, invokeAny, shutdown, shutdownNow, awaitTermination
AbstractExecutorService
   ↓
ThreadPoolExecutor            → the configurable engine

ExecutorService
   ↓
ScheduledExecutorService      → delayed/periodic
   ↓
ScheduledThreadPoolExecutor
```

> `Executor` only guarantees *task execution* — **not** that a new thread is created (an executor may just call `command.run()`, create a thread, or use a pool).

**Separation of concerns:** task (Runnable/Callable) describes the work; executor decides how/when it runs. Task vs Executor:

| Concept | Responsibility |
|---|---|
| Task | Describes the work |
| Executor | Decides how/when the work runs |
| Thread | Low-level execution unit |
| Thread Pool | Reusable set of worker threads |

## H3. ⭐ `execute()` vs `submit()`

```java
executor.execute(() -> System.out.println("Sending notification"));   // fire-and-forget

Future<Integer> future = executor.submit(() -> 10 + 20);              // Callable → Future
Future<?> future2 = executor.submit(() -> System.out.println("done")); // Runnable → Future<?>; get() yields null
```

| Feature | `execute()` | `submit()` |
|---|---|---|
| Task | `Runnable` | `Runnable` / `Callable` |
| Return | nothing | `Future` |
| Completion tracking / Future-cancellation | no | yes |
| Typical use | fire-and-forget | result / tracking / cancellation |

```text
execute() → execute and forget
submit()  → submit and track
```

## H4. ⭐ `Runnable` vs `Callable`

| Feature | `Runnable` | `Callable<V>` |
|---|---|---|
| Method | `run()` | `call()` |
| Return value | none | `V` |
| Checked exceptions | cannot declare | can declare (`call() throws Exception`) |
| `execute()` | ✅ | ❌ |
| `submit()` / `invokeAll()` / `invokeAny()` | ✅ | ✅ |

## H5. `Future` ⭐

> **A `Future` is a handle to the result/completion state of an asynchronous task.**

```java
Future<Integer> future = executor.submit(() -> { Thread.sleep(1000); return 10 + 20; });
System.out.println("Main does other work...");   // benefit: overlap work with the task
Integer result = future.get();                    // BLOCKS if not ready
```

| Method | Purpose |
|---|---|
| `get()` | Wait for completion, return result (**blocking**) |
| `get(timeout, unit)` | Bounded wait → `TimeoutException` |
| `isDone()` | Completed (success, exception, **or** cancellation all count) |
| `isCancelled()` | Was it cancelled |
| `cancel(true)` | Request cancellation; may **interrupt** a running task |

⭐ **Cancellation is cooperative** — `cancel(true)` does not forcibly kill code; the task should check interruption:

```java
Callable<Void> task = () -> {
    while (!Thread.currentThread().isInterrupted()) { /* work */ }
    return null;
};
```

### Exception handling: `execute()` vs `submit()` ⭐

```java
// execute(): exception goes to the worker's uncaught-exception path — NOT surfaced to submitter.
//             Handle it inside the task:
executor.execute(() -> {
    try { int r = 10 / 0; }
    catch (ArithmeticException e) { System.out.println("Task failed: " + e.getMessage()); }
});

// submit(): exception is CAPTURED by the Future, rethrown on get() as ExecutionException:
try {
    Integer result = future.get();
} catch (ExecutionException e) {
    Throwable cause = e.getCause();      // the original task exception
}
```

⚠ If you ignore the `Future` and never call `get()`, a `submit()` failure can pass silently.

## H6. `ThreadPoolExecutor` — the Configurable Engine

`Executors` = convenience factory class; `ThreadPoolExecutor` = the configurable engine.

```java
ThreadPoolExecutor executor = new ThreadPoolExecutor(
    corePoolSize,          // core workers
    maximumPoolSize,       // upper worker limit
    keepAliveTime,         // idle timeout for EXCESS workers
    TimeUnit,
    workQueue,             // holds tasks when no worker free
    threadFactory,         // custom thread creation (naming!)
    rejectionHandler       // what to do when saturated
);
```

- **corePoolSize:** workers created **as tasks arrive** (not eagerly at construction; `prestartAllCoreThreads()` available). Core workers are normally retained when idle.
- **maximumPoolSize:** extra threads beyond core are created only when **core is busy AND queue is full AND workers < max**.
- **keepAliveTime:** excess (above-core) idle workers are removed after this timeout; `allowCoreThreadTimeOut(true)` can extend timeout to core threads.

### ⭐ THE decision flow (top interview question)

```text
New task submitted
  → workers < corePoolSize?        YES → create core worker, run task
  → NO → queue accepts task?       YES → enqueue
  → NO → workers < maximumPoolSize? YES → create extra worker, run task
  → NO → REJECT (RejectedExecutionHandler)
```

> ⭐ **Golden rule: Core threads first → queue second → extra threads up to max → rejection.**

Worked example — core 2, max 4, queue capacity 2 (tasks all long-running):

```text
Task 1 → T1 · Task 2 → T2 · Task 3 → queue · Task 4 → queue
Task 5 → queue full → T3 · Task 6 → queue full → T4
Task 7 → max workers + full queue → REJECTED
```

### ⭐ The unbounded-queue trap

With an unbounded queue the queue is never full → extra workers are never created → **`maximumPoolSize` may never be used and the pool stays at core size** while the backlog grows in memory. (Very common interview point.)

### Queue types

| Queue | Traits |
|---|---|
| `ArrayBlockingQueue(100)` | bounded, array-backed, fixed capacity, predictable overload behavior |
| `LinkedBlockingQueue()` | can be bounded; **effectively unbounded** without capacity; used by fixed pools; backlog risk |
| `SynchronousQueue` | zero capacity, direct handoff to a worker; used by cached pools |

### Factory presets (know their internals)

| Factory | Internals | Notes |
|---|---|---|
| `newFixedThreadPool(n)` | core=max=n, unbounded `LinkedBlockingQueue` | predictable concurrency; queue can grow under overload |
| `newCachedThreadPool()` | core=0, huge max, `SynchronousQueue`, keepAlive≈60s | on-demand workers, reuse, idle removal; **risky** for rapid long-running/blocking tasks |
| `newSingleThreadExecutor()` | one worker, sequential, unbounded queue | ordered processing, serial file writes; slow worker → backlog |
| `newScheduledThreadPool(n)` | delayed + periodic tasks | preferred over `Timer` |
| `newWorkStealingPool()` | ForkJoinPool-based | many small independent/recursively generated tasks; no order guarantee |

Worker lifecycle: create → execute task → take next queued task → idle → reuse or terminate per rules.

## H7. Scheduled Execution

```java
ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);

scheduler.schedule(task, 2, TimeUnit.SECONDS);            // run once after delay
scheduler.scheduleAtFixedRate(task, 0, 2, TimeUnit.SECONDS);
scheduler.scheduleWithFixedDelay(task, 0, 2, TimeUnit.SECONDS);
```

| Fixed **Rate** | Fixed **Delay** |
|---|---|
| Planned start times: `0, 2, 4, …` (initialDelay + n·period) | Delay counted **after previous completion** |
| Tries to maintain a frequency | Regular pause after each run |
| Overlapping runs of the same task: never (starts late if a run overruns) | never |
| Metrics, polling, cadence jobs | Cleanup, retry-style jobs |

⭐ **If a periodic task throws an uncaught exception, subsequent executions of THAT task are suppressed** — handle failures inside the task:

```java
scheduler.scheduleAtFixedRate(() -> {
    try { performScheduledWork(); }
    catch (Exception e) { System.out.println("Scheduled task failed: " + e.getMessage()); }
}, 0, 10, TimeUnit.SECONDS);
```

## H8. Rejection Policies ⭐

Rejection happens when: executor shut down **OR** (pool at max size AND bounded queue full).

| Policy | Behavior |
|---|---|
| `AbortPolicy` (default) | Throws `RejectedExecutionException` — fail loudly |
| `CallerRunsPolicy` | **Submitting thread runs the task** → natural **backpressure** (producer slows down) |
| `DiscardPolicy` | Silently drops the new task (data-loss risk) |
| `DiscardOldestPolicy` | Drops the oldest queued task, retries submission |

```text
Abort → fail loudly · CallerRuns → caller works · Discard → drop new · DiscardOldest → drop old queued
```

## H9. Lifecycle: `shutdown()` / `shutdownNow()` / `awaitTermination()`

Executor worker threads are normally **non-daemon** → an active executor can keep the JVM alive after `main()` returns. Always shut executors down.

| `shutdown()` | `shutdownNow()` |
|---|---|
| Graceful, orderly | Best-effort aggressive |
| Finishes accepted (queued) tasks | Returns waiting tasks as `List<Runnable>` |
| Rejects new tasks (`RejectedExecutionException`) | Rejects new tasks |
| Normal choice | Attempts to **interrupt** running tasks (they must cooperate) |

`awaitTermination(timeout, unit)` — wait (bounded) for termination; use after shutdown calls.

⭐ **Graceful shutdown pattern:**

```java
executor.shutdown();
try {
    if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
        executor.shutdownNow();
        if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
            System.out.println("Executor did not terminate");
        }
    }
} catch (InterruptedException e) {
    executor.shutdownNow();
    Thread.currentThread().interrupt();      // preserve interruption
}
```

⭐ **Never swallow `InterruptedException`:**

```java
// Bad:  catch (InterruptedException e) { /* ignored */ }
// Good:
catch (InterruptedException e) {
    Thread.currentThread().interrupt();
    return;
}
```

## H10. Bulk Operations

```java
// invokeAll — wait for ALL; Futures list preserves input order
List<Future<Integer>> futures = executor.invokeAll(List.of(() -> 10, () -> 20, () -> 30));

// invokeAny — returns the FIRST SUCCESSFUL result; unfinished tasks cancelled
String response = executor.invokeAny(List.of(() -> callServerA(), () -> callServerB(), () -> callServerC()));
```

| `invokeAll()` | `invokeAny()` |
|---|---|
| Wait for all | One successful result |
| All Futures, input order | Chosen by successful completion |
| Every result matters | Any success is enough |

## H11. Pool Sizing & Production Checklist

- **CPU-bound** (image processing, compression, encryption, math): extra threads add scheduling overhead, not CPU capacity → size near available cores.
- **I/O-bound** (DB, network, files): more concurrency helps (threads wait) but respect DB connection limits, downstream service limits, memory.
- Prefer **bounded** resources in production: queue capacity, max workers, submission rate, task timeout, rejection behavior, downstream concurrency. An unbounded queue just moves overload from thread creation → **memory consumption**.
- Name threads via `ThreadFactory`:

```java
AtomicInteger counter = new AtomicInteger();
ThreadFactory factory = task -> {
    Thread t = new Thread(task);
    t.setName("order-worker-" + counter.incrementAndGet());
    return t;
};
```

- Monitor: `getPoolSize()`, `getActiveCount()`, `getQueue().size()`, `getCompletedTaskCount()`, `getTaskCount()`, `getLargestPoolSize()` — watch for queue growth / hitting max size.

**Complete production-style example:**

```java
ThreadPoolExecutor executor = new ThreadPoolExecutor(
    2, 4, 30, TimeUnit.SECONDS,
    new ArrayBlockingQueue<>(10),
    new ThreadPoolExecutor.CallerRunsPolicy());

Future<String> future = executor.submit(() -> {
    System.out.println("Processing order on " + Thread.currentThread().getName());
    Thread.sleep(1000);
    return "ORDER_PROCESSED";
});
try {
    System.out.println(future.get());
} catch (InterruptedException e) {
    Thread.currentThread().interrupt();
} catch (ExecutionException e) {
    System.out.println("Order failed: " + e.getCause().getMessage());
} finally {
    executor.shutdown();
}
```

### Lecture 10 must-remembers (abridged — full list concepts above)

```text
1.  Executor separates task submission from execution policy.
2.  execute() → Runnable, no Future.  submit() → Runnable/Callable → Future.
3.  Runnable → no result; Callable<T> → result + checked exceptions.
4.  Future.get() blocks; cancel(true) is cooperative interruption.
5.  submit() captures task exceptions → ExecutionException.getCause() on get().
6.  Pool flow: core → queue → extra up to max → reject.
7.  Unbounded queue can prevent growth beyond corePoolSize.
8.  Fixed = fixed workers + unbounded queue; Cached = on-demand + SynchronousQueue.
    Single = sequential; Scheduled = delayed/periodic.
9.  Fixed rate = planned schedule; fixed delay = pause after completion.
10. Uncaught exception in a periodic task suppresses future runs.
11. Abort/CallerRuns/Discard/DiscardOldest; CallerRuns = natural backpressure.
12. shutdown() graceful; shutdownNow() best-effort interrupt + returns pending; awaitTermination() waits.
13. Preserve interruption; never ignore InterruptedException.
14. More threads ≠ more performance; CPU-bound vs I/O-bound sizing differs.
15. Prefer bounded queues/resources in production; name and monitor workers.
```

---

# PART I — ADVANCED TOOLS (Lecture 11)

```text
Future           → eventual result of ONE asynchronous task
CompletableFuture→ asynchronous completion PIPELINE
ForkJoinPool     → parallelizes divisible CPU-bound computations
ThreadLocal      → separate contextual state per thread
Virtual Threads  → make very large numbers of blocking tasks practical
```

> **Core principle: choose the concurrency tool for the workload — not because an API is newer.**

## I1. Basic `Future` Recap & Its Limits

```java
Future<Integer> future = executor.submit(() -> 50);
// get() / get(timeout) / isDone() / isCancelled() / cancel(true)
```

Don't call `get()` immediately after submit — do independent work first, then `get()`. A plain `Future` can only **wait / poll / cancel / retrieve**; it cannot fluently express:

```text
task completes → transform → run next → combine → handle failure
```

That gap is what `CompletableFuture` fills.

## I2. `CompletableFuture`

Implements **both** `Future<T>` (pending result) and `CompletionStage<T>` (one stage of a pipeline that auto-triggers the next).

### Creating stages

```java
CompletableFuture<Void> f1 = CompletableFuture.runAsync(() -> System.out.println("Task running"));
CompletableFuture<Integer> f2 = CompletableFuture.supplyAsync(() -> 10);
```

| Method | Functional interface | Result |
|---|---|---|
| `runAsync()` | `Runnable` | no value (`Void`) |
| `supplyAsync()` | `Supplier<T>` | returns `T` |

**Default executor = `ForkJoinPool.commonPool()`** unless you pass a custom executor — recommended for blocking I/O, concurrency limits, naming, or workload isolation:

```java
ExecutorService pool = Executors.newFixedThreadPool(4);
CompletableFuture<Integer> f = CompletableFuture.supplyAsync(() -> 10, pool);
```

### The three continuation methods ⭐

| Method | Receives result? | Produces result? | Purpose |
|---|---|---|---|
| `thenApply()` | yes | yes | **transform** (can change type) |
| `thenAccept()` | yes | no | **consume** (print/save/notify) |
| `thenRun()` | no | no | run after completion |

```java
CompletableFuture.supplyAsync(() -> 10)
    .thenApply(v -> v * 2)                     // 10 → 20
    .thenApply(v -> "Result: " + v)            // type change ok
    .thenAccept(System.out::println)
    .thenRun(() -> System.out.println("Pipeline completed"));
```

### `thenApply()` vs `thenApplyAsync()` ⭐

```text
thenApply()       → non-async continuation; NO promise of a new thread — may run on the
                    thread completing the previous stage, or the thread attaching it if already complete
                    (⭐ "always same thread" and "always new thread" are BOTH wrong)
thenApplyAsync()  → scheduled via the default async executor (or a supplied one)
```

Same pattern exists: `thenAccept/thenAcceptAsync`, `thenRun/thenRunAsync`.

### `thenCombine()` — independent results

```java
CompletableFuture<Integer> first  = CompletableFuture.supplyAsync(() -> 10);
CompletableFuture<Integer> second = CompletableFuture.supplyAsync(() -> 20);

CompletableFuture<Integer> total = first.thenCombine(second, (a, b) -> a + b);   // 30

// real-world: user + order fetched concurrently, then combined
userFuture.thenCombine(orderFuture, (user, order) -> user + " | " + order);
```

### Error handling ⭐

| Method | Success | Failure | Can transform result? |
|---|---|---|---|
| `exceptionally()` | — | recovery with fallback value | yes (fallback) |
| `whenComplete()` | observes | observes | normally **observes only** (logging/metrics/cleanup) |
| `handle()` | observes | observes | yes — new result for either case |

```java
// exceptionally — fallback after failure
CompletableFuture.supplyAsync(() -> { throw new RuntimeException("Service unavailable"); })
    .exceptionally(ex -> 0);

// whenComplete — observe outcome without normally replacing it
CompletableFuture.supplyAsync(() -> 10)
    .whenComplete((result, ex) -> {
        if (ex == null) System.out.println("Completed: " + result);
        else            System.out.println("Failed: " + ex.getMessage());
    });

// handle — transform in both cases
CompletableFuture.supplyAsync(() -> 10 / 0)
    .handle((result, ex) -> ex != null ? "Fallback response" : "Result: " + result);
```

### `get()` vs `join()`

```text
get()  → ExecutionException  (checked — must handle)
join() → CompletionException (unchecked)
both block when the result isn't ready
```

### Pipeline style — avoid blocking between stages ⭐

```java
// blocking style (defeats the purpose):
Integer a = firstFuture.get(); Integer b = secondFuture.get();

// compositional style:
CompletableFuture<Integer> total = firstFuture.thenCombine(secondFuture, Integer::sum);

// full pipeline — submitter never blocks between stages:
CompletableFuture<Void> pipeline = CompletableFuture
    .supplyAsync(() -> "User Data")
    .thenApply(data -> data + " processed")
    .thenAccept(System.out::println);
```

⭐ **Asynchronous ≠ non-blocking.** `supplyAsync(() -> blockingDatabaseCall())` is asynchronous for the caller, but the worker thread still blocks during the call. And the **common pool is shared** — long blocking work occupying it slows unrelated tasks. For blocking I/O use a dedicated executor, bounded I/O pool, or virtual threads.

## I3. `ForkJoinPool` — Divide & Conquer

For **recursive, CPU-bound** parallelism (array sums, merge sort, recursive search, tree processing):

```text
1. Problem small enough (size <= threshold)? → solve directly
2. Else split into subtasks → run in parallel → combine results
```

**Threshold is a performance decision:** too small → task-management overhead; too large → less parallelism.

- `task.fork()` → schedule subtask asynchronously
- `task.join()` → wait for subtask result
- `pool.invoke(task)` → submit top-level task and wait
- `task.compute()` → run logic directly in the current worker

### Work stealing ⭐

Each worker has a **local deque**; a worker normally consumes its own queue, and an **idle worker steals work from another worker's queue** — balancing irregular recursive workloads. (Not one global queue.)

```text
Main task → splits → subtasks enter worker-local queues → idle workers steal
→ small tasks solved directly → partial results joined → final result
```

### Task types

| Type | Returns value? |
|---|---|
| `RecursiveTask<V>` | yes |
| `RecursiveAction` | no (overrides `compute()`) |

Both extend `ForkJoinTask`.

### Standard pattern — parallel array sum

```java
class SumTask extends RecursiveTask<Integer> {
    private final int[] arr; private final int lo, hi;
    private static final int THRESHOLD = 2;
    SumTask(int[] arr, int lo, int hi) { this.arr = arr; this.lo = lo; this.hi = hi; }

    @Override
    protected Integer compute() {
        if (hi - lo <= THRESHOLD) {                 // small enough → direct
            int sum = 0;
            for (int i = lo; i < hi; i++) sum += arr[i];
            return sum;
        }
        int mid = (lo + hi) / 2;
        SumTask left  = new SumTask(arr, lo, mid);
        SumTask right = new SumTask(arr, mid, hi);
        left.fork();                                 // schedule left
        int rightResult = right.compute();           // compute right locally
        int leftResult  = left.join();               // wait for left
        return leftResult + rightResult;             // combine
    }
}
// fork ONE side, compute the OTHER locally, then join → less scheduling overhead
// [1..8] with threshold 2 → splits → combine → 36
```

### Common pool & fit

`ForkJoinPool.commonPool()` is **shared** by parallel streams, many `CompletableFuture` async defaults, and ForkJoinTask usage — don't block it carelessly. `Executors.newWorkStealingPool()` / `(4)` creates a dedicated work-stealing executor.

**Good fit:** CPU-bound, independent subtasks, naturally decomposable, many small computations, combinable partial results.
**Poor fit:** long network/DB waits, long lock holds, strict ordering needs, non-divisible work — blocking a worker hurts the pool; use a dedicated executor or virtual threads instead.

## I4. `ThreadLocal`

Associates a **separate value with each thread**:

```text
One ThreadLocal → T1:A   T2:B   T3:C
```

```java
private static final ThreadLocal<String> USER_NAME = new ThreadLocal<>();

// Thread-1: USER_NAME.set("Harshita");  → reads "Harshita"
// Thread-2: USER_NAME.set("Aditya");    → reads "Aditya"
```

Methods: `set(value)`, `get()`, `remove()`.

**When it earns its keep:** contextual data needed across a call chain (Controller → Service → Repository) without threading a parameter through every method — request IDs, tracing info, security context, transaction context, tenant info. (Prefer explicit parameters when practical — ThreadLocal hides data flow.)

### ⭐ ThreadLocal + thread pools = must clean up

Pool workers are **reused** — Task A's leftover value leaks into Task B:

```java
CONTEXT.set(value);
try {
    performWork();
} finally {
    CONTEXT.remove();      // ⭐ always remove in finally
}
```

Also true with virtual threads: huge numbers of threads + large ThreadLocal values → large memory use. Avoid big ThreadLocal objects; don't use ThreadLocal as a resource pool.

## I5. Virtual Threads (Java 21+)

**Platform thread** = traditional thread: `Java thread → OS thread → CPU`. Expensive (memory, creation, context switching, OS limits). Blocking-heavy servers (call DB → wait → call API → wait) need many platform threads mostly sitting in wait.

**Virtual thread** = lightweight thread **scheduled by the JVM**, not the OS:

```text
Many Virtual Threads → JVM Scheduler → Carrier Platform Threads → OS/CPU
```

- **Carrier thread** = platform thread temporarily running a virtual thread.
- **Mount** when ready → executes Java code; **unmount** at a supported blocking operation → carrier freed for another virtual thread; resume later on same or different carrier.
- Inside code, `Thread.currentThread()` still refers to the **virtual** thread, not the carrier.

APIs:

```java
Thread.startVirtualThread(() -> System.out.println(Thread.currentThread()));

Thread t = Thread.ofVirtual().name("order-task").start(task);       // builder + name
Thread u = Thread.ofVirtual().name("order-task").unstarted(task); u.start();

boolean v = Thread.currentThread().isVirtual();    // true inside a virtual thread

try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
    for (int i = 1; i <= 10_000; i++) {
        int id = i;
        executor.submit(() -> {
            Thread.sleep(Duration.ofSeconds(1));                 // simulates I/O wait
            System.out.println("Completed task " + id);
            return null;
        });
    }
}
```

### ⭐ Virtual-thread rules

1. **They do NOT create more CPU.** They improve *concurrency for waiting/blocking work*, not CPU capacity. CPU-bound work stays CPU-bound (use ForkJoinPool for divide-and-conquer).
2. **Not a traditional pool:** `newVirtualThreadPerTaskExecutor()` creates a **new virtual thread per task** (task ends → thread ends). ⭐ **Do not pool virtual threads** — cheap creation is the whole point.
3. **Scarce resources stay scarce:** 100,000 virtual threads ≠ 100,000 DB connections. Limit concurrency **around the scarce resource** (e.g., `Semaphore dbLimit = new Semaphore(50)`), not around thread count.
4. **Good fit:** web requests, REST calls, DB operations, file/network I/O, message processing, request-per-thread designs. **Poor fit:** long CPU-intensive work (video encoding, encryption, big numeric computation).
5. `synchronized`: modern JDKs reduced pinning issues, but keep critical sections small; don't hold locks during long external I/O; measure real behavior.

## I6. Choosing the Tool ⭐

| Requirement | Better choice |
|---|---|
| Track one task's result / wait / cancel | `Future` |
| Chain, transform, combine, recover | `CompletableFuture` |
| Divide-and-conquer CPU-bound parallelism | `ForkJoinPool` |
| Per-thread contextual state | `ThreadLocal` |
| Huge numbers of blocking tasks | Virtual Threads |

**ForkJoinPool vs Virtual Threads:**

| ForkJoinPool | Virtual Threads |
|---|---|
| CPU-bound parallel work | High-concurrency blocking work |
| Splits computation into subtasks | One lightweight thread per task |
| Work stealing | JVM scheduling over carrier threads |
| Best for divide-and-conquer | Best for thread-per-request |
| Parallelism ≈ useful CPU capacity | Concurrency ≫ CPU count |
| Avoid long blocking | Blocking is the primary use case |

**CompletableFuture vs Virtual Threads — not replacements:**

```text
CompletableFuture → represent work as STAGES, attach continuations, compose results
                    (independent calls to combine; framework already stage-based)
Virtual Threads   → write NORMAL sequential blocking code, one virtual thread per task
                    (naturally sequential workflow; readability of imperative code)
```

```java
// CompletableFuture: two independent fetches composed declaratively
CompletableFuture<String> response = CompletableFuture
    .supplyAsync(() -> fetchUser())
    .thenCombine(CompletableFuture.supplyAsync(() -> fetchOrders()),
                 (user, orders) -> buildResponse(user, orders));

// Virtual thread style: plain sequential code (calls run sequentially unless
// you spawn separate virtual threads for them)
String user = fetchUser();
String orders = fetchOrders();
String response = buildResponse(user, orders);
```

### Lecture 11 must-remembers

```text
1.  Future → one async result; CompletableFuture → completion pipeline (Future + CompletionStage).
2.  runAsync → no result; supplyAsync → result.
3.  thenApply → transform; thenAccept → consume; thenRun → run after completion.
4.  thenApplyAsync → asynchronously scheduled continuation.
5.  thenCombine → combine two independent successful results.
6.  exceptionally → fallback; whenComplete → observe; handle → transform either way.
7.  get() → ExecutionException (checked); join() → CompletionException (unchecked).
8.  Avoid get() between every stage; asynchronous ≠ non-blocking.
9.  ForkJoinPool → CPU-bound divide & conquer; work stealing; threshold controls splitting.
10. RecursiveTask<V> returns; RecursiveAction doesn't; fork one side, compute the other.
11. ThreadLocal → per-thread value; remove() in finally (pool reuse leaks values).
12. Virtual threads → JVM-scheduled lightweight threads over carrier platform threads.
13. Virtual threads don't add CPU; ideal for blocking I/O at scale; don't pool them.
14. Limit scarce resources (Semaphore) separately from thread counts.
```

---

# PART J — FINAL QUICK-REVISION CHEAT SHEET

## J1. One-Line Definitions

```text
Program       = passive instructions stored in a file
Process       = running instance of a program (own address space)
Thread        = execution path inside a process (own stack + PC; shares heap)
Main thread   = JVM-created thread that runs main()
Concurrency   = overlapping progress, interleaving — possible on 1 core
Parallelism   = simultaneous execution on multiple cores
Context switch= save one thread's state, run another (has overhead)
Race condition= result depends on unpredictable thread timing
Critical section = code touching shared mutable state
Atomicity     = operation is one indivisible unit
Visibility    = latest write reliably observable by other threads
Ordering      = operations observed in the required sequence
Monitor       = per-object lock mechanism used by synchronized
Semaphore     = N permits instead of one owner
CAS           = update only if value still equals expected
Future        = handle to an async task's pending result
Executor      = separates task submission from execution policy
Virtual thread= lightweight JVM-scheduled thread over carrier platform threads
```

## J2. The 15 Absolute Essentials (if you learn nothing else) ⭐

```text
 1. Threads share the heap; each thread has its own stack + PC register.
 2. Runnable = task; Thread = executor. Prefer Runnable (+ lambdas).
 3. run() = normal method call; start() = new thread. Start a thread only ONCE.
 4. Six states: NEW, RUNNABLE, BLOCKED, WAITING, TIMED_WAITING, TERMINATED.
    (No separate RUNNING state.)
 5. sleep() → TIMED_WAITING, keeps locks. wait() → releases monitor, needs monitor.
 6. interrupt() is cooperative, not a kill. Re-interrupt in catch blocks.
 7. count++ is read-modify-write → NOT atomic → race condition / lost update.
 8. volatile = visibility + ordering; NOT mutual exclusion; does NOT fix count++.
 9. synchronized: instance → this, static → Class.class, block → given object.
    Gives mutual exclusion + visibility + ordering. Reentrant.
10. wait/notify/notifyAll belong to Object; call inside synchronized while owning
    the monitor; always re-check condition in a while loop (spurious wakeups).
11. Lock: always unlock() in finally. tryLock/fairness → ReentrantLock.
    Read-heavy → ReadWriteLock; optimistic reads → StampedLock; N-at-a-time → Semaphore.
12. Atomic classes + CAS = lock-free updates. CAS fails → retry. ABA → versioning.
    Atomic ops ≠ atomic blocks.
13. Executor: execute() → no Future; submit() → Future. Callable returns values.
    Future.get() blocks.
14. Pool flow: core → queue → extra up to max → reject. Unbounded queue traps max size.
    CallerRunsPolicy = backpressure; shutdown() then awaitTermination().
15. Pick tools by workload: CompletableFuture = composition, ForkJoinPool = CPU
    divide-and-conquer, virtual threads = massive blocking I/O concurrency.
```

## J3. State Cheat Card

| You call… | On whom | Effect |
|---|---|---|
| `Thread.sleep(ms)` | current thread | TIMED_WAITING; keeps all monitors |
| `t.join()` | caller of join | caller WAITING until t terminates |
| `t.join(ms)` | caller | caller TIMED_WAITING (bounded) |
| `lock.wait()` | current thread (must own lock) | releases lock's monitor → WAITING |
| `lock.notify()/notifyAll()` | owner of lock's monitor | waiters become eligible; must reacquire monitor |
| `t.interrupt()` | target t | sets status / wakes blocking ops via InterruptedException |
| `Thread.yield()` | current thread | hint only; stays RUNNABLE |
| `condition.await()/signal()` | lock holder | Condition-based wait/signal (like wait/notify) |
| `task.fork()` / `task.join()` | ForkJoin task | schedule subtask / await its result |
| `future.get()` | caller | blocks until done (ExecutionException on task failure) |

## J4. Common Mistakes — Final Warning List ⭐

```text
❌ "A program is running in RAM"                 → a PROCESS is the running instance
❌ "Each thread has its own heap"                → heap is shared; stack/PC are private
❌ "Multithreading always makes programs faster" → overhead exists; depends on workload
❌ worker.run() expecting a new thread            → run() is a plain method call
❌ Assuming t1.start() first ⇒ t1 runs first      → scheduling is non-deterministic
❌ Calling start() twice                          → IllegalThreadStateException
❌ "RUNNABLE = definitely on the CPU"             → RUNNABLE includes ready-to-run
❌ "sleep() releases the lock"                    → it never releases monitors
❌ "yield()/priority guarantee anything"           → hints only
❌ "count++ is one statement, so it's atomic"     → read-modify-write
❌ "volatile makes count++ thread-safe"           → volatile ≠ mutual exclusion/atomicity
❌ "volatile = direct RAM access"                 → explain via happens-before instead
❌ synchronized(new Object())                     → new monitor every call
❌ Different locks guarding the same data         → one consistency rule ⇒ one lock
❌ Check outside the synchronized region          → check+update inside together
❌ if(!cond) wait()                               → use while (spurious wakeups)
❌ wait()/notify() without owning the monitor     → IllegalMonitorStateException
❌ "notifyAll() runs everyone simultaneously / prevents deadlock" → no and no
❌ lock.lock() without try/finally unlock         → lock leak on exception
❌ Optimistic read without validate()/fallback    → read → validate → fallback
❌ "Atomic ops make the whole block atomic"       → sequences need CAS loops/locks
❌ "CAS never fails / lock-free is always faster" → both false; retries cost CPU
❌ "AtomicReference makes the object thread-safe" → only reference updates
❌ One thread per task in production              → use pools/executors
❌ "submit() returns the result"                  → it returns a Future
❌ "maximumPoolSize threads are created upfront"  → core → queue → extra → reject
❌ Unbounded queue expecting max growth           → pool stays at core size
❌ shutdownNow() "instantly kills tasks"          → best-effort interruption
❌ Ignoring InterruptedException                  → restore interrupt status
❌ "thenApply() always same/new thread"           → non-async makes no thread promise
❌ "Asynchronous = non-blocking"                  → worker threads can still block
❌ "ForkJoinPool for everything"                  → it's for CPU-bound divisible work
❌ Forgetting ThreadLocal.remove() in pools       → stale data leaks across tasks
❌ "Virtual threads = more CPU / remove DB limits" → concurrency aid, not capacity
❌ Pooling virtual threads                        → one thread per task is the model
```

---

# PART K — IMPORTANT COMPARISONS (Master Table Collection)

### K1. Program vs Process vs Thread
See **A3/A5** — program = stored instructions; process = running instance with own address space; thread = execution path sharing process memory, owning its stack + PC.

### K2. Concurrency vs Parallelism — see A8
Concurrent on one core (interleaving) vs simultaneous on many cores.

### K3. Multitasking vs Multithreading — see A9
Many processes vs many threads in one process.

### K4. Thread vs Runnable — see B2
Task+executor combined vs separated; interface preserves inheritance; Runnable is reusable and pool-friendly.

### K5. `start()` vs `run()` — see B3
New thread + own stack vs plain method call on the current thread.

### K6. BLOCKED vs WAITING vs TIMED_WAITING — see B7
Monitor acquisition vs indefinite coordination vs bounded wait.

### K7. `sleep()` vs `wait()` — see E5 ⭐
Thread-static delay, keeps monitors vs Object monitor coordination, releases the monitor.

### K8. `isInterrupted()` vs `interrupted()` — see B8
Check that thread, keeps flag vs check current thread, clears flag.

### K9. `volatile` vs `synchronized` vs `AtomicInteger` — see C9 ⭐
Visibility/ordering vs mutual exclusion + visibility + ordering vs atomic small operations.

### K10. Atomicity vs Visibility vs Ordering — see C4–C6
Interference halfway vs seeing the latest value vs observed sequence.

### K11. Race Condition vs Thread Interference vs Data Inconsistency — see C7
Timing-dependent correctness vs harmful interleaving vs rule-violating final state.

### K12. `synchronized` vs `Lock` — see F3
Automatic monitor release & simplicity vs explicit control, tryLock, timeout, fairness.

### K13. Lock vs Semaphore
"Who owns the critical section?" vs "How many may enter?"

### K14. Pessimistic vs Optimistic Reading — see F5
Lock-then-read vs read-then-validate-with-fallback.

### K15. `ReentrantReadWriteLock` vs `StampedLock` — see F5
Reentrant read/write vs non-reentrant + optimistic read.

### K16. Lock-Based vs CAS/Lock-Free — see G5
Block-and-wait vs fail-and-retry; big critical sections vs small atomic transitions.

### K17. Lock-Free vs Wait-Free — see G5
System-wide progress vs bounded per-operation completion.

### K18. `AtomicInteger.incrementAndGet()` vs `getAndIncrement()` — see G2
Returns new value vs returns old value.

### K19. `AtomicReference` vs `AtomicReferenceArray` — see G2
One atomic reference vs many per-index atomic slots.

### K20. ABA Versioning vs `StampedLock` stamps — see G7
Detecting A→B→A value changes vs locking/optimistic-read state.

### K21. `execute()` vs `submit()` — see H3
Runnable/fire-and-forget vs Runnable+Callable/Future tracking.

### K22. `Runnable` vs `Callable` — see H4
No result, no checked exceptions vs result + checked exceptions.

### K23. `shutdown()` vs `shutdownNow()` — see H9
Graceful, finishes queued work vs best-effort interrupt, returns pending tasks.

### K24. Fixed Rate vs Fixed Delay — see H7
Planned start cadence vs pause after each completion.

### K25. Rejection Policies — see H8
Abort (throw) / CallerRuns (backpressure) / Discard (drop new) / DiscardOldest (drop old).

### K26. Executor Presets — see H6
Fixed / Cached / Single / Scheduled / WorkStealing internals.

### K27. Future vs CompletableFuture vs ForkJoinPool vs ThreadLocal vs Virtual Threads — see I6
Result handle / composition pipeline / CPU divide-and-conquer / per-thread context / blocking-task scale.

### K28. Platform Threads vs Virtual Threads — see I5
OS-backed, expensive, pooled vs JVM-scheduled, cheap, one-per-task; same Java APIs.

---

# PART L — COMPLETE CONCEPT MAP

```text
                        JAVA MULTITHREADING
                                │
  ┌─────────────────────────────┼──────────────────────────────┐
  │ FOUNDATIONS                 │ PROBLEMS                     │ SOLUTIONS
  │                             │                              │
  │ Program                     │ Shared mutable state         │ volatile
  │   ↓                         │   + multiple threads         │   visibility+ordering
  │ Process (own memory)        │   + no coordination          │
  │   ↓                         │   ↓                          │ synchronized / monitor
  │ JVM (heap+method area       │ Unsafe interleaving          │   mutual exclusion
  │       shared; stack+PC      │   ↓                          │   + visibility+ordering
  │       private per thread)   │ RACE CONDITIONS              │
  │   ↓                         │   ↓ lost updates, stale      │ wait/notify/notifyAll
  │ Main thread                 │     reads, partial updates   │   guarded blocks (while)
  │   ↓                         │   ↓                          │
  │ More threads                │ Data inconsistency           │ Explicit Locks
  │   ↓                         │                              │   ReentrantLock (tryLock,
  │ Context switch              │ THE 3 GUARANTEES             │    fairness)
  │   ↓                         │   Atomicity  (count++)       │   ReadWriteLock (R+R ok)
  │ Concurrency (1 core)        │   Visibility (flag)          │   StampedLock (optimistic)
  │ Parallelism (N cores)       │   Ordering   (data/ready)    │   Semaphore (N permits)
  │                             │                              │   Condition (await/signal)
  │ THREAD LIFECYCLE            │ TOOLS THAT FIX THEM          │
  │ NEW → RUNNABLE →            │   synchronized (Part D)      │ LOCK-FREE
  │   BLOCKED/WAITING/          │   atomics+CAS (Part G)       │   AtomicInteger/Long/Boolean
  │   TIMED_WAITING →           │   locks (Part F)             │   AtomicReference(+Array)
  │   TERMINATED                │   immutability               │   CAS: expected==current?
  │                             │   reduced sharing            │     yes→update / no→retry
  │ THREAD METHODS              │                              │   CAS loop = optimistic
  │ sleep/join/yield            │ COMMUNICATION (Part E)       │   ABA → (value, version)
  │ interrupt (cooperative)     │   Producer ↔ Box ↔ Consumer  │
  │ isAlive/currentThread       │   busy-wait bad → wait()     │ EXECUTOR FRAMEWORK (Part H)
  │ priority (hint 1–10)        │   notify one / notifyAll     │   Executor → ExecutorService
  │ daemon (JVM exits when      │   reacquire + RE-CHECK       │     → ThreadPoolExecutor
  │   no user threads)          │   wait ≠ sleep               │   core → queue → max → reject
  │                             │                              │   execute vs submit
  │                             │                              │   Runnable vs Callable
  │                             │                              │   Future (get blocks)
  │                             │                              │   shutdown/shutdownNow
  │                             │                              │   CallerRuns = backpressure
  │                             │                              │
  │                             │                              │ ADVANCED (Part I)
  │                             │                              │   Future → CF pipeline
  │                             │                              │   supplyAsync/thenApply/
  │                             │                              │     thenAccept/thenCombine
  │                             │                              │   exceptionally/handle
  │                             │                              │   ForkJoinPool: split→fork/
  │                             │                              │     join→work stealing
  │                             │                              │   ThreadLocal: per-thread
  │                             │                              │     value; remove() in finally
  │                             │                              │   Virtual threads: JVM-
  │                             │                              │     scheduled; 1 per task;
  │                             │                              │     blocking-friendly scale
  └─────────────────────────────┴──────────────────────────────┴──────────────────────────
                                │
                                ↓
                     THREAD-SAFE APPLICATIONS
                (right tool for the right workload)
```

### The Learning Path in One Line

```text
Understand threads (A–B) → understand why they clash (C) → guard state (D)
→ coordinate threads (E) → scale control (F) → go lock-free (G)
→ stop managing threads manually (H) → compose & scale at a higher level (I)
```

---

# PART M — FRESHER INTERVIEW RAPID-FIRE Q&A

### Foundations

**Q. What is the difference between a program, a process, and a thread?**
Program = stored passive instructions; process = running instance with its own address space; thread = execution path inside a process sharing its memory.

**Q. Which JVM memory is shared vs private?**
Shared: heap, method area/Metaspace. Private per thread: Java stack, PC register.

**Q. Concurrency vs parallelism?**
Concurrency = overlapping progress via interleaving (possible on one core); parallelism = simultaneous execution on multiple cores.

**Q. Why isn't multithreading always faster?**
Context switching and coordination overhead; benefits depend on workload and parallelizability.

### Threads & Lifecycle

**Q. Two ways to create a thread? Which is preferred and why?**
Extend `Thread` or implement `Runnable`. `Runnable` preferred: separates task from execution, preserves class inheritance, reusable, works with executors.

**Q. `start()` vs `run()`?**
`start()` creates a new thread which then calls `run()`; calling `run()` directly is a normal method call on the current thread.

**Q. Can we restart a thread?**
No — second `start()` throws `IllegalThreadStateException`. Reuse the Runnable with a new Thread.

**Q. Six thread states? Does Java have RUNNING?**
NEW, RUNNABLE, BLOCKED, WAITING, TIMED_WAITING, TERMINATED. No separate RUNNING — RUNNABLE covers ready + executing.

**Q. BLOCKED vs WAITING?**
BLOCKED = waiting to acquire a monitor; WAITING = indefinite coordination wait (`wait()`, `join()`, `park()`).

**Q. Does sleep() release locks?**
No. `wait()` releases the monitor of the object waited on.

**Q. Does interrupt() kill a thread?**
No — it's a cooperative request; the thread decides how to respond (check `isInterrupted()`, catch `InterruptedException`).

**Q. `isInterrupted()` vs `interrupted()`?**
Instance method, doesn't clear vs static method on current thread, clears the flag.

**Q. Can a daemon thread keep the JVM alive?**
No. JVM exits when no user (non-daemon) threads remain.

**Q. Does priority guarantee execution order?**
No — it's only a scheduling hint (1–10, default 5).

### Problems & Guarantees

**Q. What is a race condition? Give an example.**
Correctness depends on unpredictable thread timing — e.g., `count++` (read-modify-write) executed by two threads producing a lost update.

**Q. What is a critical section?**
Code that accesses shared mutable state and must be protected.

**Q. Atomicity vs visibility?**
Atomicity: operation can't be interfered with halfway. Visibility: other threads can observe the latest write. One atomic write isn't automatically visible.

**Q. What does volatile guarantee — and not guarantee?**
Visibility + ordering around volatile access + atomic reads/writes of the variable itself. No mutual exclusion; does NOT make `count++` atomic. (Explain via happens-before, not "direct RAM access.")

### Synchronization & Communication

**Q. Which monitor does synchronized use?**
Instance method → `this`; static method → `ClassName.class`; block → the given object.

**Q. Can two synchronized methods run concurrently?**
Yes — if they use different monitors (different objects, or instance vs static).

**Q. What is reentrancy?**
A thread owning a monitor can acquire it again without blocking (hold count).

**Q. Why is `synchronized(new Object())` wrong?**
Every call creates a different monitor — no mutual exclusion.

**Q. Why are wait/notify in Object, not Thread?**
They operate on an object's monitor; the caller must own it, else `IllegalMonitorStateException`.

**Q. What does wait() actually do?**
Releases that monitor, enters WAITING; wakes on notify/notifyAll/interrupt/spurious wakeup; must reacquire the monitor before continuing.

**Q. notify() vs notifyAll()?**
One waiter (unspecified which) vs all waiters — either way only one thread owns the monitor at a time; notification ≠ immediate execution.

**Q. Why `while(!condition) wait();` instead of `if`?**
Spurious wakeups + state changes by other threads before reacquiring the monitor → always re-check.

**Q. What is busy waiting?**
Repeatedly checking a condition without blocking — wastes CPU; `wait()`-based coordination avoids it.

### Locks

**Q. ReentrantLock vs synchronized?**
Explicit control: `tryLock()`, timed acquisition, fairness, multiple Conditions. Must `unlock()` in `finally`.

**Q. What does reentrant mean for a lock?**
Same thread can acquire it multiple times; lock frees when hold count reaches 0.

**Q. ReadWriteLock rules?**
Multiple readers together (shared); writers exclusive vs readers and other writers. Great for read-heavy data.

**Q. What is optimistic reading in StampedLock?**
`tryOptimisticRead()` → read → `validate(stamp)`; on failure fall back to a full read lock. StampedLock is not generally reentrant.

**Q. Lock vs Semaphore?**
Lock = single ownership; Semaphore = N permits limiting concurrent access (e.g., API/pool limits).

**Q. Starvation vs deadlock?**
Starvation: a thread keeps losing access. Deadlock: circular wait — T1 holds A wants B; T2 holds B wants A; nobody progresses.

### Atomics & CAS

**Q. What is CAS?**
Compare-And-Set: atomically set a new value only if the current value still equals the expected value; otherwise fail (retry).

**Q. Why is a get()-then-act check unsafe even on an atomic variable?**
Another thread can change the value between `get()` and the update; individual atomic ops don't make the sequence atomic — use CAS, a CAS loop, or a lock.

**Q. What is the ABA problem and its fix?**
Value goes A→B→A so value-only CAS misses the change; fix by comparing `(value, version)` — versioning/stamping.

**Q. Is lock-free always faster?**
No — heavy contention causes repeated CAS retries; depends on workload and algorithm.

### Executors

**Q. Why use the Executor Framework?**
It separates task submission from execution: reusable workers, controlled concurrency, queuing, results (Future), cancellation, lifecycle, scheduling.

**Q. execute() vs submit()?**
execute: Runnable, no Future, fire-and-forget. submit: Runnable/Callable, returns Future for result/tracking/cancellation.

**Q. Runnable vs Callable?**
run() returns void and can't declare checked exceptions; call() returns V and can throw checked exceptions.

**Q. Is Future.get() blocking? What about cancel(true)?**
get() blocks until done (or use get(timeout)). cancel(true) is cooperative — it may interrupt; the task must cooperate.

**Q. Where does a submit() task's exception go?**
Into the Future — rethrown from get() as ExecutionException; get the original via getCause(). (execute() exceptions go to the worker's uncaught path.)

**Q. Describe ThreadPoolExecutor task flow.**
Fill core workers → enqueue → if queue full create workers up to maximumPoolSize → otherwise reject. Unbounded queue means max is never reached.

**Q. Which rejection policies exist?**
Abort (exception, default), CallerRuns (caller executes → backpressure), Discard (drop new), DiscardOldest (drop oldest queued).

**Q. shutdown() vs shutdownNow()?**
Graceful (finish accepted tasks) vs best-effort interrupt + return pending tasks. Follow with awaitTermination().

**Q. Fixed-rate vs fixed-delay scheduling?**
Fixed rate targets a regular start schedule; fixed delay waits a set time after the previous completion. An uncaught exception in a periodic task stops its future runs.

### Advanced Tools

**Q. Future vs CompletableFuture?**
Future = placeholder for one result (wait/poll/cancel); CompletableFuture = Future + CompletionStage for transform/combine/recover pipelines without blocking between stages.

**Q. thenApply vs thenAccept vs thenRun?**
Transform (receives + returns), consume (receives only), run-after (neither).

**Q. thenApply() vs thenApplyAsync()?**
Non-async gives no thread guarantee (may run on the completing thread); Async schedules the continuation (optionally on a given executor).

**Q. get() vs join()?**
get() throws checked ExecutionException; join() throws unchecked CompletionException. Both block.

**Q. Is asynchronous the same as non-blocking?**
No — the caller may not block, but the worker thread can still block inside the task (e.g., DB call); don't run long blocking work on the common pool.

**Q. What problem does ForkJoinPool solve? What is work stealing?**
CPU-bound divide-and-conquer parallelism via RecursiveTask/RecursiveAction, thresholds, fork/join. Work stealing: idle workers take tasks from other workers' local queues.

**Q. RecursiveTask vs RecursiveAction?**
Returns a value vs returns nothing.

**Q. What is ThreadLocal? Why is it risky with pools?**
Per-thread value storage. Workers are reused → stale values leak to the next task; always `remove()` in finally.

**Q. What is a virtual thread? When to use it?**
A lightweight JVM-scheduled thread mounted on carrier platform threads (Java 21). Ideal for massive blocking-I/O concurrency (thread-per-request); not for CPU-bound work and it doesn't add CPU capacity or lift downstream resource limits.

**Q. Should you pool virtual threads?**
No — use `newVirtualThreadPerTaskExecutor()` (one virtual thread per task) and limit scarce resources with, e.g., a Semaphore.

---

## 🎯 Final One-Minute Revision (the whole course)

> **A program becomes a process, and threads inside that process do the work — sharing the heap while each keeps its own stack and PC. I create threads via Runnable tasks (preferred) and start them with start(); execution order is never guaranteed. Shared mutable state without coordination causes race conditions — count++ is not atomic — so I need atomicity, visibility, and ordering: volatile for simple flags, synchronized/monitors for mutual exclusion + visibility, wait/notify (inside while-guarded synchronized blocks) for condition-based coordination, explicit Locks for tryLock/fairness/read-write/optimistic/permit-based needs, and atomic classes + CAS for lock-free small updates (retrying on failure, versioning against ABA). At scale I stop managing threads manually and submit tasks to executors (execute vs submit, Callable + Future, ThreadPoolExecutor's core→queue→max→reject flow, graceful shutdown), then compose asynchronous pipelines with CompletableFuture, parallelize CPU-bound work with ForkJoinPool, keep per-thread context in ThreadLocal (cleaned up in finally), and use virtual threads for massive blocking-I/O concurrency. Threads share memory; safety comes from the Java Memory Model's happens-before guarantees and choosing the right tool for the workload.**

---

*Compiled from Lectures 1–11 notes · Program → Process → Thread → Safety → Coordination → Locking → Lock-Free → Executors → Advanced Tools.*
