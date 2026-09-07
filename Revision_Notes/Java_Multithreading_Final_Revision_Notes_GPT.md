# Java Multithreading — Final Revision Notes

> **Purpose:** One consolidated revision document for Lectures 1–11.
>
> This file combines the lecture notes in learning order, removes repeated explanations, and keeps the concepts, code patterns, interview points, common mistakes, and key comparisons that matter for a fresher building a strong Java multithreading foundation.

---

# 0. Complete Learning Roadmap

```text
Lecture 1
Program → Process → Thread → CPU → Concurrency / Parallelism

Lecture 2
Thread creation → Runnable → start() vs run() → Lifecycle

Lecture 3
sleep() → join() → yield() → interrupt() → priority → daemon

Lecture 4
Race condition → Atomicity → Visibility → Ordering → Thread interference

Lecture 5
synchronized → Monitor → Critical section → Instance/static locks

Lecture 6
wait() → notify() → notifyAll() → Producer-Consumer

Lecture 7
Lock → ReentrantLock → ReadWriteLock → StampedLock
Semaphore → Condition

Lecture 8
Atomic variables → AtomicInteger → AtomicReference → CAS

Lecture 9
CAS retry loop → AtomicReferenceArray → ABA → Versioning

Lecture 10
Executor Framework → Thread pools → Future → ThreadPoolExecutor

Lecture 11
CompletableFuture → ForkJoinPool → ThreadLocal → Virtual Threads
```

---

# PART 1 — FOUNDATIONS

# 1. Program, Process and Thread

## Program

A **program** is a passive set of instructions stored in a file.

Example:

```text
Main.java
Main.class
```

A program itself is not executing.

### Mental model

```text
Program
→ stored instructions
→ passive
```

---

## Process

A **process** is a running instance of a program.

When you execute:

```bash
java Main
```

the operating system starts a process for the application.

### Mental model

```text
Program
   ↓ run
Process
```

A process has its own execution environment, including memory and operating-system resources.

---

## Thread

A **thread** is an independent path of execution inside a process.

```text
One Process
│
├── Main Thread
├── Worker Thread
├── Worker Thread
└── Background Thread
```

### Important

The process provides the execution environment.

The threads perform the actual work.

### Easy mental model

```text
Program = stored instructions
Process = running environment
Thread  = execution path
```

---

# 2. Process vs Thread

| Process | Thread |
|---|---|
| Running instance of a program | Execution path inside a process |
| Own virtual address space | Shares process address space |
| Stronger isolation | Lower isolation |
| More expensive to create/switch | Generally lighter |
| Communication often uses IPC | Shared-memory communication is easier |
| Contains one or more threads | Belongs to one process |

### Interview answer

> A process is an independent execution environment, while a thread is an execution path inside that process.

---

# 3. Main Thread in Java

A normal Java application starts with a main thread that invokes:

```java
public static void main(String[] args)
```

Example:

```java
public class Main {

    public static void main(String[] args) {
        System.out.println("Hello");
    }
}
```

`main()` executes **inside the main thread**.

---

# 4. JVM Memory and Threads

A simplified Java memory model contains:

```text
                 JVM
                  │
        ┌─────────┼─────────┐
        │         │         │
       Heap   Class Data   Threads
                           │
                    ┌──────┼──────┐
                    │      │      │
                 Stack T1 Stack T2 Stack T3
                    │      │      │
                   PC     PC     PC
```

### Shared

Threads in the same JVM share process-level memory such as:

```text
Heap
Class-related runtime data
```

### Private

Each thread has its own execution-related state, including:

```text
Java Stack
PC register
```

### Most important mental model

> **Shared heap + private stacks**

This is a foundational reason why synchronization becomes necessary.

---

# 5. Concurrency vs Parallelism

## Concurrency

Multiple tasks make progress during overlapping periods.

On a single core:

```text
Thread A
  ███      ███

Thread B
     ███      ███
```

The CPU switches between tasks.

### Key point

> Concurrency can happen on a single core.

---

## Parallelism

Multiple tasks execute at the same instant on different processing resources.

```text
Core 1 → Thread A
Core 2 → Thread B
Core 3 → Thread C
```

### Key point

> Parallel execution requires multiple execution resources.

---

## Comparison

| Concurrency | Parallelism |
|---|---|
| Overlapping progress | Simultaneous execution |
| Can happen on one core | Requires multiple execution resources |
| Scheduling/interleaving is central | Hardware parallel execution is central |
| Focus is handling multiple tasks | Focus is doing multiple tasks at once |

### Easy memory trick

```text
Concurrency = dealing with many things
Parallelism = doing many things at once
```

---

# 6. Multitasking vs Multithreading

## Multitasking

Operating-system level concept:

```text
Chrome
VS Code
Music Player
```

Multiple applications/processes are managed by the OS.

## Multithreading

Programming-level concept:

```text
One Java process
├── Main thread
├── Worker thread
├── Worker thread
└── Background thread
```

### Easy memory trick

```text
Multitasking
→ multiple applications/processes

Multithreading
→ multiple threads inside a process
```

---

# PART 2 — CREATING AND MANAGING THREADS

# 7. Two Basic Ways to Create a Thread

## 7.1 Extend `Thread`

```java
class MyThread extends Thread {

    @Override
    public void run() {
        System.out.println("Task");
    }
}

public class Main {

    public static void main(String[] args) {
        MyThread thread = new MyThread();
        thread.start();
    }
}
```

---

## 7.2 Implement `Runnable`

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

        Thread thread =
            new Thread(task);

        thread.start();
    }
}
```

---

# 8. Why `Runnable` Is Usually Preferred

`Runnable` separates:

```text
WHAT work should be done
        ↓
Runnable

HOW the work is executed
        ↓
Thread / Executor
```

Benefits:

```text
Separation of concerns
Inheritance flexibility
Task reusability
Natural fit with executor-based APIs
```

Because Java has single class inheritance:

```java
class MyTask extends SomeClass
                    implements Runnable
```

is possible.

But:

```java
class MyTask extends SomeClass, Thread
```

is not.

---

# 9. `Runnable` Is a Functional Interface

Because `Runnable` has one abstract method:

```java
void run();
```

it can be used with a lambda:

```java
Runnable task =
    () -> System.out.println("Hello");
```

Then:

```java
new Thread(task).start();
```

Or:

```java
new Thread(
    () -> System.out.println("Hello")
).start();
```

### Mental model

```text
Runnable = task
Thread   = execution mechanism
```

---

# 10. `start()` vs `run()` — MOST IMPORTANT

## `run()`

Calling:

```java
thread.run();
```

is just a normal method call.

No new thread is created.

```text
Main Thread
    ↓
run()
    ↓
task executes on main thread
```

---

## `start()`

Calling:

```java
thread.start();
```

starts independent thread execution.

Conceptually:

```text
Main Thread
    ↓
start()
    ↓
New Thread
    ↓
run()
```

### Golden rule

```text
run()
→ normal method call

start()
→ starts thread execution
```

---

# 11. Thread State Lifecycle

Java defines six official thread states:

```text
NEW
RUNNABLE
BLOCKED
WAITING
TIMED_WAITING
TERMINATED
```

---

## NEW

Thread object created:

```java
Thread t =
    new Thread(task);
```

but not started.

```text
NEW
```

---

## RUNNABLE

After:

```java
t.start();
```

the thread becomes `RUNNABLE`.

Java does not expose a separate public `RUNNING` state.

`RUNNABLE` includes the situation where a thread is:

```text
ready to run
or
currently executing
```

---

## BLOCKED

A thread is `BLOCKED` when it is trying to acquire an intrinsic monitor needed to enter synchronized code.

```text
Thread A owns monitor
        ↓
Thread B tries same monitor
        ↓
BLOCKED
```

### Memory trick

```text
BLOCKED
→ waiting for a monitor
```

---

## WAITING

A thread waits indefinitely for some action/event.

Common causes:

```java
object.wait();
thread.join();
LockSupport.park();
```

### Memory trick

```text
WAITING
→ indefinite coordination wait
```

---

## TIMED_WAITING

A thread waits for a bounded period.

Examples:

```java
Thread.sleep(2000);
object.wait(2000);
thread.join(2000);
```

### Memory trick

```text
TIMED_WAITING
→ waiting with a time limit
```

---

## TERMINATED

Execution has finished.

The thread can terminate because:

```text
run() completed normally
or
run() ended due to an uncaught exception
```

A terminated thread cannot be restarted.

---

# 12. BLOCKED vs WAITING vs TIMED_WAITING

| State | Meaning | Examples |
|---|---|---|
| `BLOCKED` | Waiting for monitor | Occupied `synchronized` monitor |
| `WAITING` | Waiting indefinitely | `wait()`, `join()`, `park()` |
| `TIMED_WAITING` | Waiting for limited time | `sleep()`, timed `wait()`, timed `join()` |

---

# 13. Important Thread Utility Methods

## `Thread.currentThread()`

Returns the currently executing thread.

```java
Thread current =
    Thread.currentThread();
```

---

## `getName()` / `setName()`

```java
thread.setName("worker-1");
```

Meaningful names improve:

```text
logging
debugging
thread dumps
monitoring
incident investigation
```

---

## `isAlive()`

Returns `true` after a thread has started and while it has not yet terminated.

```text
NEW
→ false

started + not terminated
→ true

TERMINATED
→ false
```

It does not mean "currently executing on CPU."

---

# 14. `sleep()`

```java
Thread.sleep(2000);
```

Pauses the **current thread** for a specified duration.

It causes:

```text
RUNNABLE
   ↓
TIMED_WAITING
   ↓
RUNNABLE
```

### Critical fact

> `sleep()` does **not** release monitor locks.

---

# 15. `join()`

```java
worker.join();
```

means:

> The **calling thread** waits until `worker` terminates.

Example:

```text
Main
 ↓
worker.start()
 ↓
worker.join()
 ↓
Main waits

Worker
 ↓
work
 ↓
TERMINATED

Main
 ↓
continues
```

Timed form:

```java
worker.join(1000);
```

waits for at most the specified duration.

---

# 16. `yield()`

```java
Thread.yield();
```

is a scheduler hint.

It means roughly:

> "I am willing to give other runnable threads a chance."

But the scheduler/OS may ignore it.

Therefore:

```text
yield()
≠ guaranteed context switch
```

---

# 17. `interrupt()`

`interrupt()` is a **cooperative interruption request**.

```java
thread.interrupt();
```

It does not forcibly kill a thread.

A thread should decide how to respond:

```text
stop loop
finish current operation
cleanup
return
```

Example:

```java
while (!Thread.currentThread().isInterrupted()) {
    // work
}
```

---

# 18. `InterruptedException`

Methods such as:

```java
Thread.sleep()
Thread.join()
Object.wait()
```

can throw:

```java
InterruptedException
```

when the thread is interrupted while blocked in those interruptible methods.

Good pattern:

```java
try {
    Thread.sleep(1000);
} catch (InterruptedException e) {
    Thread.currentThread().interrupt();
    return;
}
```

Restoring the interrupt status preserves the cancellation signal.

---

# 19. `isInterrupted()` vs `Thread.interrupted()`

```java
thread.isInterrupted();
```

→ checks a particular thread  
→ does not clear the status

```java
Thread.interrupted();
```

→ checks current thread  
→ clears the status

### Easy memory trick

```text
isInterrupted()
→ inspect THAT thread

interrupted()
→ inspect ME + clear
```

---

# 20. Thread Priority

Java defines:

```java
Thread.MIN_PRIORITY   // 1
Thread.NORM_PRIORITY  // 5
Thread.MAX_PRIORITY   // 10
```

Example:

```java
thread.setPriority(
    Thread.MAX_PRIORITY
);
```

Priority is a scheduling hint, not a guarantee of execution order.

Never design correctness logic around:

```text
priority 10 must run before priority 5
```

---

# 21. Daemon Threads

A daemon thread is a background/support thread.

Example:

```java
Thread worker =
    new Thread(task);

worker.setDaemon(true);

worker.start();
```

The JVM can terminate when no non-daemon/user threads remain, even if daemon threads are still running.

### Important

Daemon status should be set **before** starting the thread.

Typical conceptual use:

```text
background monitoring
support work
runtime services
```

---

# 22. Important Thread Rules

```text
1. A Thread object can be started only once.
2. A terminated Thread cannot be restarted.
3. Reuse the Runnable/task by creating a new Thread.
4. Thread scheduling order is not guaranteed.
5. getState() is only a snapshot.
```

---

# PART 3 — WHY MULTITHREADING BREAKS

# 23. Shared Mutable State

The dangerous combination is:

```text
SHARED
+
MUTABLE
+
MULTIPLE THREADS
```

Examples:

```java
int count;

BankAccount account;

List<Integer> list;
```

---

# 24. Critical Section

A **critical section** is code that accesses shared mutable state and therefore requires coordinated access.

Example:

```java
void increment() {
    count++;
}
```

Here:

```text
Shared resource → count
Critical section → count++
```

---

# 25. Race Condition

A **race condition** occurs when correctness depends on unpredictable thread timing/order.

Example:

```java
count++;
```

looks like one line but is conceptually:

```text
READ
 ↓
MODIFY
 ↓
WRITE
```

Two threads can overlap these steps.

---

# 26. Lost Update Example

Initial:

```text
count = 0
```

Two threads:

```text
T1 → reads 0
T2 → reads 0

T1 → calculates 1
T2 → calculates 1

T1 → writes 1
T2 → writes 1
```

Final:

```text
1
```

instead of:

```text
2
```

This is a classic race/lost-update problem.

---

# 27. Atomicity

Atomicity means:

> A logical operation behaves as one indivisible unit with respect to cooperating threads.

For:

```java
count++;
```

the logical operation needs protection because:

```text
read + increment + write
```

can otherwise be interleaved.

---

# 28. Check-Then-Act Problem

Example:

```java
if (balance >= amount) {
    balance -= amount;
}
```

The problem is:

```text
CHECK
 ↓
another thread changes state
 ↓
ACT
```

The whole logical operation needs one consistent atomicity mechanism.

---

# 29. Visibility Problem

One thread writes shared state:

```java
flag = true;
```

but another thread is not guaranteed to observe the write without an appropriate memory-ordering relationship.

Beginner mental model:

```text
Thread
→ registers/caches
→ shared memory
```

But do not define `volatile` formally as "always reads directly from RAM." The Java Memory Model defines visibility through memory-ordering/happens-before guarantees.

---

# 30. `volatile`

Example:

```java
volatile boolean running;
```

`volatile` provides:

```text
Visibility
+
Ordering guarantees around volatile accesses
+
Atomic read/write of the volatile variable itself
```

### What it does NOT do

```java
volatile int count = 0;

count++;
```

is still not atomic.

So:

```text
volatile
≠
mutual exclusion
≠
compound-operation atomicity
```

---

# 31. Ordering Problem

Consider:

```java
int data = 0;
boolean ready = false;
```

Thread 1:

```java
data = 10;
ready = true;
```

Thread 2:

```java
if (ready) {
    System.out.println(data);
}
```

Without synchronization/happens-before, seeing `ready == true` alone does not establish the required visibility/order for `data`.

A publication pattern can use:

```java
int data = 0;
volatile boolean ready = false;
```

Then the volatile write/read establishes the needed relationship for preceding writes.

---

# 32. Thread Interference

Thread interference means operations from multiple threads overlap in a way that disrupts each other's work.

Typical pattern:

```text
shared mutable state
+
unsafe interleaving
=
thread interference
```

---

# 33. Data Inconsistency

The result is that shared state can violate application rules.

Possible forms:

```text
Lost update
Stale read
Partial update
Check-then-act failure
Unsafe publication
Inconsistent state
```

---

# 34. Three Big Concurrency Questions

Always ask:

```text
ATOMICITY
Can another thread interfere halfway?

VISIBILITY
Can another thread see my latest write?

ORDERING
Will operations be observed in the required order?
```

These are different problems.

---

# PART 4 — SYNCHRONIZATION

# 35. `synchronized`

Basic form:

```java
synchronized (lock) {
    // critical section
}
```

Or a synchronized method:

```java
synchronized void increment() {
    count++;
}
```

It uses a Java monitor to control access.

---

# 36. Monitor / Object Lock

Every Java object can serve as a monitor.

For:

```java
synchronized (lock) {
    // ...
}
```

the thread must acquire `lock`'s monitor.

Conceptually:

```text
Monitor
   ↓
one owner
   ↓
other contenders block
```

---

# 37. Synchronized Instance Method

```java
synchronized void method() {
}
```

is conceptually similar to:

```java
void method() {

    synchronized (this) {
        // method body
    }
}
```

Therefore:

```text
instance synchronized method
→ monitor = this
```

---

# 38. Synchronized Static Method

```java
static synchronized void method() {
}
```

uses the class object:

```java
ClassName.class
```

Conceptually:

```java
static void method() {

    synchronized (ClassName.class) {
        // ...
    }
}
```

Therefore:

```text
instance synchronized
→ this

static synchronized
→ ClassName.class
```

---

# 39. Same Object vs Different Object

Two threads using:

```java
sameObject.method1();
sameObject.method2();
```

can block one another if both instance methods are synchronized because they use the same monitor.

But:

```java
object1.method();
object2.method();
```

can proceed concurrently because the monitors differ.

### Golden rule

> **Synchronization depends on the monitor object, not just the method name.**

---

# 40. Synchronized Method vs Block

## Method

```java
synchronized void update() {
    // whole method protected
}
```

## Block

```java
void update() {

    // unrelated work

    synchronized (lock) {
        // only critical section
    }

    // unrelated work
}
```

Synchronized blocks allow finer-grained control.

---

# 41. Custom Lock Object

Common pattern:

```java
private final Object lock =
    new Object();
```

Then:

```java
synchronized (lock) {
    count++;
}
```

Why:

```text
private
→ external code cannot easily synchronize on it

final
→ lock reference cannot be replaced
```

---

# 42. Wrong Lock

Avoid:

```java
synchronized (new Object()) {
    count++;
}
```

Every execution gets a new monitor.

Therefore:

```text
Thread 1 → Object A
Thread 2 → Object B
A != B
```

No mutual exclusion exists between them.

---

# 43. Same Consistency Rule → Same Lock

Danger:

```java
synchronized (depositLock) {
    balance += amount;
}

synchronized (withdrawLock) {
    balance -= amount;
}
```

If both modify the same `balance`, different locks can allow unsafe concurrent access.

Better:

```java
private final Object balanceLock =
    new Object();
```

and:

```java
synchronized (balanceLock) {
    // deposit
}

synchronized (balanceLock) {
    // withdraw
}
```

### Golden rule

> **State that must satisfy the same consistency rule should be protected by the same synchronization policy.**

---

# 44. Benefits of `synchronized`

Synchronization provides:

```text
Mutual exclusion
Visibility
Ordering
```

It can therefore make a complete logical operation atomic **when the complete operation is protected using the same monitor and competing threads follow the same policy**.

---

# 45. Reentrant Synchronization

Java intrinsic monitors are reentrant.

A thread that already owns a monitor can acquire the same monitor again.

Example:

```java
synchronized void methodA() {
    methodB();
}

synchronized void methodB() {
}
```

A thread entering `methodA()` already owns `this` and can enter `methodB()` on the same object.

---

# PART 5 — INTER-THREAD COMMUNICATION

# 46. Why Do We Need `wait()` / `notify()`?

Synchronization answers:

> "Who can enter the critical section?"

But sometimes a thread needs to wait because a condition is not yet true.

Example:

```text
Consumer
→ wants data

Producer
→ has not produced data yet
```

Instead of wasting CPU checking repeatedly, the consumer should wait.

---

# 47. Busy Waiting

Bad pattern:

```java
while (!dataAvailable) {
    // keep checking
}
```

This consumes CPU while no useful work is happening.

Better:

```text
wait for condition
→ release monitor
→ sleep without busy looping
→ wake
→ re-check condition
```

---

# 48. Producer-Consumer Problem

Classic scenario:

```text
Producer
   ↓
produces data
   ↓
Shared buffer/resource
   ↓
Consumer
   ↓
consumes data
```

Without coordination:

```text
Consumer may run before data exists
Producer may overwrite existing data
Threads may operate in the wrong order
```

---

# 49. `wait()`

Example:

```java
synchronized (lock) {

    while (!condition) {
        lock.wait();
    }

    // proceed
}
```

Important behavior:

```text
wait()
→ thread enters WAITING
→ releases that object's monitor
→ waits for notification/interruption/etc.
→ before returning, reacquires that monitor
```

### Very important

> `wait()` releases the monitor of the object on which it is invoked.

It does not release every lock the thread may hold.

---

# 50. `wait()` Must Be Used With the Correct Monitor

If:

```java
synchronized (lock) {
    lock.wait();
}
```

the object must be the same synchronization object.

A call like:

```java
other.wait();
```

without owning `other`'s monitor can cause:

```text
IllegalMonitorStateException
```

---

# 51. Why `wait()`, `notify()`, `notifyAll()` Belong to `Object`

They operate on an object's monitor.

Therefore they are defined in:

```java
java.lang.Object
```

not in `Thread`.

---

# 52. `notify()`

```java
lock.notify();
```

wakes one waiting thread associated with that object's monitor.

Which waiting thread gets the opportunity is not a simple guaranteed FIFO contract.

Therefore:

```text
notify()
→ wake one waiter
```

---

# 53. `notifyAll()`

```java
lock.notifyAll();
```

wakes all threads waiting on that monitor so they can compete to reacquire the monitor and re-check their conditions.

### Mental model

```text
notify()
→ one waiter

notifyAll()
→ all waiters
```

---

# 54. Why `notifyAll()` Is Often Safer

With multiple categories of waiting threads, `notify()` can wake a thread whose condition is still false.

That thread goes back to waiting.

Another eligible thread may never be signaled depending on the design.

`notifyAll()` wakes everyone, allowing each thread to re-check its condition.

### Important

`notifyAll()` is not automatically the most efficient choice, but it is often the safer communication strategy for guarded-condition designs.

---

# 55. Guarded Block — `while`, Not `if`

Use:

```java
while (!condition) {
    lock.wait();
}
```

not:

```java
if (!condition) {
    lock.wait();
}
```

Why?

```text
Wake up
  ↓
condition may still be false
  ↓
check again
  ↓
wait again if necessary
```

This handles:

```text
spurious wakeups
other threads consuming the condition
notification without the condition being satisfied
```

---

# 56. Deadlock

Deadlock occurs when threads wait indefinitely for each other's resources.

Classic:

```text
Thread 1
holds Lock A
waits for Lock B

Thread 2
holds Lock B
waits for Lock A
```

Neither can progress.

### Common prevention ideas

```text
consistent lock ordering
avoid unnecessary nested locks
keep lock scope small
use timed lock acquisition when appropriate
```

---

# 57. `sleep()` vs `wait()`

| `sleep()` | `wait()` |
|---|---|
| `Thread.sleep()` | `Object.wait()` |
| Causes `TIMED_WAITING` | `WAITING` / `TIMED_WAITING` |
| Does not release monitor | Releases object's monitor |
| Used for delay | Used for coordination |
| No `synchronized` requirement by itself | Must own object's monitor |

---

# PART 6 — ADVANCED LOCKING

# 58. Why `Lock`?

`synchronized` is excellent for simple mutual exclusion.

The `Lock` API is useful when more explicit control is needed:

```text
Manual acquisition/release
tryLock()
Timed acquisition
Fairness options
Explicit Conditions
```

---

# 59. Basic `Lock` Pattern

```java
lock.lock();

try {
    // critical section

} finally {
    lock.unlock();
}
```

### Golden rule

> **Manual lock → unlock in `finally`.**

---

# 60. `ReentrantLock`

Common implementation:

```java
ReentrantLock lock =
    new ReentrantLock();
```

It is reentrant:

```text
same thread
→ acquire lock again
→ allowed
```

---

# 61. `tryLock()`

Instead of waiting indefinitely:

```java
if (lock.tryLock()) {

    try {
        // acquired
    } finally {
        lock.unlock();
    }

} else {
    // failed
}
```

Timed form:

```java
lock.tryLock(
    2,
    TimeUnit.SECONDS
);
```

### Mental model

```text
lock()
→ wait until acquired

tryLock()
→ try and handle failure/timeout
```

---

# 62. Fairness

A `ReentrantLock` can be configured as fair:

```java
new ReentrantLock(true);
```

Fairness tries to make acquisition more orderly for waiting threads.

Trade-off:

```text
more predictable access
→ potentially lower throughput
```

---

# 63. Starvation

Starvation means a thread repeatedly fails to obtain the resources it needs and therefore makes no useful progress.

Unfair scheduling/locking can increase starvation risk.

Fairness can help, but it does not solve every concurrency problem.

---

# 64. `ReadWriteLock`

Useful for read-heavy workloads.

It provides:

```text
Read Lock
→ shared

Write Lock
→ exclusive
```

Example:

```java
ReentrantReadWriteLock rw =
    new ReentrantReadWriteLock();

Lock readLock =
    rw.readLock();

Lock writeLock =
    rw.writeLock();
```

Rules:

```text
Reader + Reader → allowed

Reader + Writer → not allowed

Writer + Writer → not allowed
```

---

# 65. Reader-Writer Problem

Imagine:

```text
100 readers
1 writer
```

An exclusive lock forces readers to serialize unnecessarily.

A read-write lock lets multiple readers operate together when no writer is active.

Good for:

```text
Read-heavy
Infrequent writes
```

---

# 66. Lock Downgrading

A thread may:

```text
acquire write lock
→ update state
→ acquire read lock
→ release write lock
→ continue as reader
```

The read lock is obtained before releasing the write lock so the state is not exposed through an unwanted gap.

---

# 67. `StampedLock`

Provides:

```text
Write lock
Read lock
Optimistic read
```

It uses stamps:

```java
long stamp =
    lock.tryOptimisticRead();
```

---

# 68. Optimistic Reading

Flow:

```text
get stamp
   ↓
read without full read lock
   ↓
validate stamp
   ↓
valid?
 /   \
yes   no
 |     |
use   fallback
      read lock
```

Example:

```java
long stamp =
    lock.tryOptimisticRead();

int current = value;

if (!lock.validate(stamp)) {

    stamp = lock.readLock();

    try {
        current = value;
    } finally {
        lock.unlockRead(stamp);
    }
}
```

### Mental model

```text
Pessimistic
→ lock first, read later

Optimistic
→ read first, validate later
```

---

# 69. Important `StampedLock` Limitation

`StampedLock` is **not generally reentrant**.

Do not treat it like:

```text
ReentrantLock
or
synchronized
```

in that respect.

Also:

```text
optimistic read
→ must validate
```

---

# 70. Semaphore

A semaphore manages **permits**, not one lock owner.

Example:

```java
Semaphore semaphore =
    new Semaphore(3);
```

At most three permit holders can enter the controlled resource concurrently.

Usage:

```java
semaphore.acquire();

try {
    // use resource

} finally {
    semaphore.release();
}
```

---

# 71. Semaphore Example

```text
permits = 3

T1 acquire → 2
T2 acquire → 1
T3 acquire → 0
T4 acquire → waits

T1 release → 1
T4 can acquire
```

Useful for:

```text
API concurrency limits
database access limits
connection pools
scarce resources
bounded concurrent work
```

---

# 72. Lock vs Semaphore

| Lock | Semaphore |
|---|---|
| Usually one owner | Multiple permits |
| Mutual exclusion | Bounded concurrency |
| `lock()` / `unlock()` | `acquire()` / `release()` |
| Ownership-based | Permit-based |

---

# 73. `Condition`

`Condition` gives more flexible waiting/signaling when using explicit locks.

Example:

```java
ReentrantLock lock =
    new ReentrantLock();

Condition notEmpty =
    lock.newCondition();

Condition notFull =
    lock.newCondition();
```

Operations:

```java
condition.await();
condition.signal();
condition.signalAll();
```

Multiple conditions allow different waiting queues for the same lock.

---

# 74. Condition + Guarded Waiting

Correct:

```java
lock.lock();

try {

    while (!conditionIsTrue) {
        condition.await();
    }

    // work

} finally {
    lock.unlock();
}
```

Use `while` because the condition must always be re-checked after wakeup.

---

# PART 7 — ATOMIC VARIABLES AND CAS

# 75. Lock-Free Concurrency

For some small state transitions, explicit locks are not required.

Java provides:

```text
AtomicInteger
AtomicLong
AtomicBoolean
AtomicReference
AtomicReferenceArray
```

These provide atomic operations on shared state.

---

# 76. `AtomicInteger`

Example:

```java
AtomicInteger count =
    new AtomicInteger(0);

count.incrementAndGet();
```

Useful methods:

```java
get()
set()
incrementAndGet()
getAndIncrement()
decrementAndGet()
getAndDecrement()
addAndGet()
getAndAdd()
getAndSet()
compareAndSet()
```

---

# 77. `incrementAndGet()` vs `getAndIncrement()`

Suppose:

```java
AtomicInteger count =
    new AtomicInteger(5);
```

### `incrementAndGet()`

```java
int value =
    count.incrementAndGet();
```

```text
5 → 6
returns 6
```

### `getAndIncrement()`

```java
int value =
    count.getAndIncrement();
```

```text
5 → 6
returns 5
```

### Memory trick

```text
incrementAndGet
→ increment THEN get

getAndIncrement
→ get THEN increment
```

Same pattern applies to:

```text
addAndGet
getAndAdd
```

---

# 78. `AtomicBoolean`

Useful for shared state such as:

```java
AtomicBoolean running =
    new AtomicBoolean(true);
```

And:

```java
running.compareAndSet(
    false,
    true
);
```

This supports atomic boolean state transitions.

---

# 79. `AtomicReference`

Useful when shared state is an object reference.

Example:

```java
AtomicReference<String> seat =
    new AtomicReference<>("EMPTY");
```

Then:

```java
seat.compareAndSet(
    "EMPTY",
    "Aditya"
);
```

---

# 80. `AtomicReferenceArray`

Useful when each array element needs atomic reference operations.

Example:

```java
AtomicReferenceArray<String> seats =
    new AtomicReferenceArray<>(5);
```

Then:

```java
seats.compareAndSet(
    0,
    "EMPTY",
    "Aditya"
);
```

The operation is applied to the specified index.

---

# 81. CAS — Compare-And-Set

Basic form:

```java
compareAndSet(
    expectedValue,
    newValue
);
```

Meaning:

> Update the value only if the current value is still equal to the expected value.

Flow:

```text
Current == Expected?
        │
   ┌────┴────┐
  YES        NO
   │          │
update       fail
   │          │
success      retry/handle
```

---

# 82. CAS Retry Loop

A common lock-free pattern:

```java
while (true) {

    int current =
        value.get();

    int next =
        current + 1;

    if (value.compareAndSet(
            current,
            next)) {

        break;
    }
}
```

Flow:

```text
READ
 ↓
CALCULATE
 ↓
CAS
 ↓
 ┌─────────┐
success   failure
   ↓         ↓
 done     read again
             ↓
          recalculate
             ↓
            CAS
```

---

# 83. Why CAS Fails

Suppose:

```text
current = 10
```

Both threads read:

```text
10
```

Both calculate:

```text
11
```

T1:

```text
CAS(10,11)
→ success
```

T2:

```text
CAS(10,11)
→ failure
```

because:

```text
current = 11
expected = 10
```

T2 then retries.

---

# 84. CAS Is Optimistic

Lock:

```text
Acquire
→ block if unavailable
→ execute
→ release
```

CAS:

```text
Read
→ assume state is unchanged
→ attempt update
→ retry if conflict
```

### Easy memory trick

```text
Lock
→ wait

CAS
→ retry
```

This is a mental model, not a claim that every lock-free algorithm literally spins.

---

# 85. Atomic Operation ≠ Atomic Block

This is extremely important.

Example:

```java
if (count.get() > 0) {
    count.decrementAndGet();
}
```

Each atomic operation is individually safe.

But:

```text
get()
→ condition
→ decrement
```

is a compound logical operation.

Another thread can change the state between them.

Therefore:

> Atomic classes do not automatically make arbitrary multi-step business logic atomic.

---

# 86. `volatile` vs Atomic

```text
volatile
→ visibility + ordering

Atomic class
→ atomic operations
```

Do not use:

```text
volatile = atomic
```

And do not claim that:

```text
volatile = direct RAM access
```

as the formal explanation.

---

# 87. ABA Problem

Suppose the value starts as:

```text
A
```

Thread 1 reads:

```text
A
```

Thread 2 performs:

```text
A → B
```

then:

```text
B → A
```

Thread 1 now sees:

```text
A
```

and may believe:

```text
"Nothing changed."
```

But actually:

```text
A → B → A
```

occurred.

This is the:

> **ABA problem**

---

# 88. Solving ABA With Versioning

Store:

```text
(value, version)
```

Example:

```text
(A,1)
(B,2)
(A,3)
```

Now a thread that originally saw:

```text
(A,1)
```

can detect:

```text
(A,3)
```

as a different state.

### Core idea

> **Same value does not necessarily mean same state.**

---

# 89. Lock-Free vs Lock-Based

| Lock-Based | Lock-Free / CAS |
|---|---|
| `synchronized`, `Lock` | Atomic classes, CAS |
| Threads may block | CAS can fail/retry |
| Good for larger critical sections | Good for small state transitions |
| Simpler for complex invariants | Useful for carefully designed atomic updates |
| Can suffer lock contention | Can suffer retry contention |
| Not automatically slower | Not automatically faster |

---

# PART 8 — EXECUTOR FRAMEWORK

# 90. Why Executors?

Manual:

```java
new Thread(task).start();
```

works for small examples.

At scale, one thread per task can cause:

```text
memory overhead
thread-creation overhead
OS scheduling overhead
context-switching overhead
uncontrolled thread growth
```

---

# 91. Executor Framework

The Executor Framework separates:

```text
Task submission
from
Thread management
```

Instead of:

```text
Create thread
→ execute task
```

think:

```text
Submit task
→ executor manages workers
```

---

# 92. Thread Pool

A thread pool is a reusable set of worker threads.

```text
Tasks
  ↓
Queue
  ↓
Worker 1
Worker 2
Worker 3
```

Workers are reused across tasks.

Benefits:

```text
Controlled concurrency
Thread reuse
Predictable resource usage
Queue-based workload management
```

---

# 93. Executor Hierarchy

Simplified:

```text
Executor
   ↓
ExecutorService
   ↓
ThreadPoolExecutor
```

Scheduled execution:

```text
ScheduledExecutorService
   ↓
ScheduledThreadPoolExecutor
```

---

# 94. `Executor`

Basic API:

```java
void execute(Runnable command);
```

Important:

> An `Executor` is an abstraction and does not necessarily create a new thread.

---

# 95. `ExecutorService`

Adds:

```text
submit()
shutdown()
shutdownNow()
invokeAll()
invokeAny()
awaitTermination()
```

It combines execution with lifecycle and result-management functionality.

---

# 96. `execute()` vs `submit()`

## `execute()`

```java
executor.execute(task);
```

```text
Runnable
No Future
Fire-and-forget style
```

## `submit()`

```java
Future<Integer> future =
    executor.submit(() -> 10);
```

```text
Runnable or Callable
Returns Future
Supports result/completion/cancellation tracking
```

---

# 97. Runnable vs Callable

| Runnable | Callable |
|---|---|
| `run()` | `call()` |
| No result | Returns `T` |
| Cannot declare checked exceptions in `run()` | Can throw checked exceptions |
| `execute()` or `submit()` | Usually `submit()` |

---

# 98. Future

A `Future<T>` represents the pending result of an asynchronous computation.

Example:

```java
Future<Integer> future =
    executor.submit(() -> 42);
```

Important methods:

```java
get()
get(timeout, unit)
isDone()
isCancelled()
cancel(...)
```

---

# 99. `Future.get()`

```java
Integer result =
    future.get();
```

If result isn't ready:

```text
calling thread waits
```

Therefore:

```text
submit()
→ asynchronous submission

get()
→ may block
```

---

# 100. Future Exceptions

`get()` can report:

```text
InterruptedException
ExecutionException
CancellationException
```

For task failure:

```java
try {
    future.get();
} catch (ExecutionException e) {
    System.out.println(
        e.getCause()
    );
}
```

---

# 101. `Future.cancel()`

```java
future.cancel(true);
```

Cancellation is a request.

If the task is running, interruption may be attempted.

A running task must cooperate with interruption.

```text
cancel
≠
forced kill
```

---

# 102. `isDone()` vs `get()`

```text
isDone()
→ non-blocking status check

get()
→ wait for result if necessary
```

`isDone() == true` can mean:

```text
success
exception
cancellation
```

It does not guarantee success.

---

# 103. `ThreadPoolExecutor`

Provides detailed configuration over:

```text
corePoolSize
maximumPoolSize
keepAliveTime
TimeUnit
workQueue
ThreadFactory
RejectedExecutionHandler
```

Example:

```java
ThreadPoolExecutor executor =
    new ThreadPoolExecutor(
        2,
        4,
        30,
        TimeUnit.SECONDS,
        new ArrayBlockingQueue<>(10)
    );
```

---

# 104. Core vs Maximum Pool Size

The central rule is:

```text
Core workers first
       ↓
Queue
       ↓
Extra workers up to maximum
       ↓
Reject
```

Example:

```text
core = 2
max = 4
queue = 2
```

Under sustained work:

```text
Task 1 → worker 1
Task 2 → worker 2
Task 3 → queue
Task 4 → queue
Task 5 → worker 3
Task 6 → worker 4
Task 7 → rejected
```

---

# 105. Why Queue Choice Matters

Queue affects:

```text
memory usage
latency
worker growth
overload behavior
whether maximum pool size is reached
```

Common queues:

```text
ArrayBlockingQueue
LinkedBlockingQueue
SynchronousQueue
```

---

# 106. `ArrayBlockingQueue`

```java
new ArrayBlockingQueue<>(100);
```

Bounded queue.

Advantages:

```text
explicit capacity
predictable backlog
explicit overload/rejection behavior
```

---

# 107. `LinkedBlockingQueue`

Can be bounded:

```java
new LinkedBlockingQueue<>(1000);
```

or effectively unbounded:

```java
new LinkedBlockingQueue<>();
```

An unbounded queue can grow significantly if producers outpace workers.

---

# 108. `SynchronousQueue`

Has no normal storage capacity.

A task must be handed directly to a worker.

Conceptually:

```text
Task
 ↓
Direct handoff
 ↓
Worker
```

This is part of the design of cached thread pools.

---

# 109. Rejection Policies

When:

```text
maximum workers reached
+
queue full
```

a rejection policy decides what happens.

## `AbortPolicy`

```text
throw RejectedExecutionException
```

## `CallerRunsPolicy`

```text
caller thread executes task
```

This can provide backpressure.

## `DiscardPolicy`

```text
drop new task
```

## `DiscardOldestPolicy`

```text
remove oldest queued task
→ retry new task
```

---

# 110. Fixed Thread Pool

```java
Executors.newFixedThreadPool(4);
```

Conceptually:

```text
fixed number of workers
+
queue
```

Useful for controlled concurrency.

A conventional fixed thread pool uses an effectively unbounded `LinkedBlockingQueue`, so backlog can grow under sustained overload.

---

# 111. Cached Thread Pool

```java
Executors.newCachedThreadPool();
```

Conceptually:

```text
core = 0
large maximum
SynchronousQueue
idle workers reused
```

Useful for many short-lived asynchronous tasks.

### Risk

It can create a very large number of platform threads under heavy load.

---

# 112. Single-Thread Executor

```java
Executors.newSingleThreadExecutor();
```

Uses one worker.

Tasks execute sequentially:

```text
Task 1
 ↓
Task 2
 ↓
Task 3
```

Useful for:

```text
ordered processing
serialized writes
single-consumer workflows
```

---

# 113. Scheduled Executor

```java
ScheduledExecutorService scheduler =
    Executors.newScheduledThreadPool(2);
```

Supports:

```text
delayed execution
periodic execution
```

---

# 114. `schedule()`

```java
scheduler.schedule(
    task,
    2,
    TimeUnit.SECONDS
);
```

Runs once after the delay.

---

# 115. `scheduleAtFixedRate()` vs `scheduleWithFixedDelay()`

### Fixed rate

```java
scheduleAtFixedRate(...)
```

tries to maintain a regular schedule.

### Fixed delay

```java
scheduleWithFixedDelay(...)
```

waits for the previous execution to finish, then waits the delay, then runs again.

### Memory trick

```text
Fixed Rate
→ regular schedule

Fixed Delay
→ wait after completion
```

---

# 116. Periodic Task Exceptions

An uncaught exception from a periodic task can suppress subsequent executions of that periodic task.

If future executions should continue, handle expected exceptions inside the task.

---

# 117. Executor Shutdown

## `shutdown()`

```java
executor.shutdown();
```

Means:

```text
No new tasks
+
allow accepted tasks to finish
```

It does not itself wait for termination.

---

## `shutdownNow()`

```java
List<Runnable> pending =
    executor.shutdownNow();
```

Attempts to:

```text
stop accepting work
interrupt running workers
return queued tasks that never started
```

It is best-effort.

---

## `awaitTermination()`

```java
executor.awaitTermination(
    10,
    TimeUnit.SECONDS
);
```

waits for termination for up to the given timeout.

---

# 118. `invokeAll()`

Submit multiple `Callable`s and wait for all:

```java
List<Future<Integer>> futures =
    executor.invokeAll(tasks);
```

Useful when every task result matters.

---

# 119. `invokeAny()`

Submit multiple `Callable`s and obtain one successful result.

Useful when:

```text
any one successful result
is sufficient
```

---

# 120. Production Thread-Pool Checklist

Ask:

```text
CPU-bound or I/O-bound?
How long do tasks run?
How many tasks arrive?
How many concurrent operations are safe?
What queue capacity is appropriate?
What happens during overload?
How are tasks cancelled?
How is shutdown handled?
How are workers monitored?
```

---

# PART 9 — COMPLETABLEFUTURE

# 121. Why `CompletableFuture`?

Basic `Future` is good for:

```text
wait
check
cancel
get result
```

But it does not provide fluent asynchronous composition.

`CompletableFuture` adds:

```text
transform
consume
combine
recover
observe
chain
```

---

# 122. What Is `CompletableFuture`?

It implements:

```text
Future
+
CompletionStage
```

Therefore:

```text
Future
→ pending result

CompletionStage
→ one stage in a larger async pipeline
```

---

# 123. `runAsync()`

For no result:

```java
CompletableFuture<Void> future =
    CompletableFuture.runAsync(() -> {
        System.out.println("Running");
    });
```

---

# 124. `supplyAsync()`

For a result:

```java
CompletableFuture<Integer> future =
    CompletableFuture.supplyAsync(
        () -> 10
    );
```

### Comparison

```text
runAsync()
→ action
→ no result

supplyAsync()
→ Supplier<T>
→ returns result
```

---

# 125. Default Executor

When no executor is supplied, asynchronous `CompletableFuture` methods normally use:

```java
ForkJoinPool.commonPool()
```

A custom executor can be provided when:

```text
blocking I/O
specific concurrency limit
thread naming
monitoring
workload isolation
```

matters.

---

# 126. `thenApply()`

Transforms a result:

```java
CompletableFuture<Integer> future =
    CompletableFuture
        .supplyAsync(() -> 10)
        .thenApply(value -> value * 2);
```

Flow:

```text
10
 ↓
20
```

---

# 127. `thenAccept()`

Consumes the result but produces no new result.

```java
CompletableFuture<Void> future =
    CompletableFuture
        .supplyAsync(() -> 10)
        .thenAccept(value -> {
            System.out.println(value);
        });
```

---

# 128. `thenRun()`

Runs an action after completion but does not receive the previous result.

```java
future.thenRun(() -> {
    System.out.println("Completed");
});
```

---

# 129. `thenApply()` vs `thenAccept()` vs `thenRun()`

| Method | Receives result? | Produces result? | Purpose |
|---|---:|---:|---|
| `thenApply()` | Yes | Yes | Transform |
| `thenAccept()` | Yes | No | Consume |
| `thenRun()` | No | No | Run after completion |

---

# 130. Non-Async vs Async Methods

```java
future.thenApply(fn);
```

does not promise a new thread.

It may run in a thread involved in completing the previous stage, or in the thread registering the continuation if the stage is already complete.

Async form:

```java
future.thenApplyAsync(fn);
```

schedules the continuation using the default async executor.

Custom executor:

```java
future.thenApplyAsync(
    fn,
    executor
);
```

Same pattern exists for:

```text
thenAcceptAsync()
thenRunAsync()
```

---

# 131. `thenCombine()`

Combines successful results of two independent futures.

```java
CompletableFuture<Integer> first =
    CompletableFuture.supplyAsync(
        () -> 10
    );

CompletableFuture<Integer> second =
    CompletableFuture.supplyAsync(
        () -> 20
    );

CompletableFuture<Integer> total =
    first.thenCombine(
        second,
        Integer::sum
    );
```

Result:

```text
30
```

Flow:

```text
first  → 10 ──┐
              ├─ combine → 30
second → 20 ──┘
```

---

# 132. `exceptionally()`

Provides a fallback after failure.

```java
future.exceptionally(
    exception -> 0
);
```

Mental model:

```text
failure
 ↓
fallback
```

---

# 133. `whenComplete()`

Observes success/failure.

```java
future.whenComplete(
    (result, exception) -> {
        // logging / metrics / cleanup
    }
);
```

Normally it observes rather than replacing the result.

---

# 134. `handle()`

Receives both:

```text
result
exception
```

and can produce a new result in either case.

```java
future.handle(
    (result, exception) -> {
        if (exception != null) {
            return "Fallback";
        }
        return "Result: " + result;
    }
);
```

---

# 135. Error Handling Comparison

| Method | Runs on success | Runs on failure | Can produce new result |
|---|---:|---:|---:|
| `exceptionally()` | Recovery-oriented | Yes | Yes |
| `whenComplete()` | Yes | Yes | Normally observes only |
| `handle()` | Yes | Yes | Yes |

---

# 136. `get()` vs `join()` on `CompletableFuture`

Both can block:

```java
future.get();
future.join();
```

Difference:

```text
get()
→ ExecutionException
→ checked interruption/timeout handling as applicable

join()
→ CompletionException
→ unchecked
```

---

# 137. Avoid Blocking Between Every Stage

Bad composition:

```java
int first = firstFuture.get();
int second = secondFuture.get();

int total = first + second;
```

Better:

```java
CompletableFuture<Integer> total =
    firstFuture.thenCombine(
        secondFuture,
        Integer::sum
    );
```

This declares the relationship instead of manually blocking between stages.

---

# 138. Asynchronous ≠ Non-Blocking

This is very important.

```java
CompletableFuture.supplyAsync(
    () -> blockingDatabaseCall()
);
```

is asynchronous to the caller.

But the worker thread can still be blocked on the database call.

Therefore:

```text
Asynchronous
→ caller does not execute task directly

Non-blocking
→ executing thread does not wait/park for that operation
```

They are different concepts.

---

# PART 10 — FORKJOINPOOL

# 139. What Problem Does ForkJoinPool Solve?

Best for:

```text
CPU-bound
divide-and-conquer
recursive
parallelizable
independent subtasks
```

Pattern:

```text
Large problem
   ↓
Split
   ↓
Split again
   ↓
Solve subtasks
   ↓
Combine results
```

Examples:

```text
array sum
merge sort
recursive search
tree traversal
image processing
```

---

# 140. Threshold / Base Case

A recursive task needs a stopping condition.

```text
size <= threshold
→ calculate directly

size > threshold
→ split
```

Too small a threshold:

```text
too many tiny tasks
→ overhead
```

Too large a threshold:

```text
less parallelism
```

---

# 141. `fork()` and `join()`

```java
task.fork();
```

schedules a subtask.

```java
task.join();
```

waits for and retrieves its result.

---

# 142. Work Stealing

Workers maintain local queues.

```text
Worker A → Queue A
Worker B → Queue B
Worker C → Queue C
```

If Worker B becomes idle:

```text
Worker B
   ↓
steals work
from another worker
   ↓
continues processing
```

This is:

> **Work stealing**

It helps balance irregular recursive workloads.

---

# 143. `RecursiveTask<V>`

For a result:

```java
class SumTask
        extends RecursiveTask<Integer> {

    @Override
    protected Integer compute() {
        return 10;
    }
}
```

---

# 144. `RecursiveAction`

For no result:

```java
class PrintTask
        extends RecursiveAction {

    @Override
    protected void compute() {
        System.out.println(
            "Processing"
        );
    }
}
```

---

# 145. RecursiveTask vs RecursiveAction

```text
RecursiveTask<V>
→ returns V

RecursiveAction
→ no result
```

---

# 146. Standard Fork-Join Pattern

A good pattern is:

```java
leftTask.fork();

int rightResult =
    rightTask.compute();

int leftResult =
    leftTask.join();

return leftResult + rightResult;
```

The current worker:

```text
forks one side
+
computes the other side
+
joins the forked side
```

This keeps the worker productive and reduces unnecessary scheduling overhead.

---

# 147. `invoke()` vs `fork()` vs `join()` vs `compute()`

| Method | Purpose |
|---|---|
| `pool.invoke(task)` | Submit top-level task and wait for result |
| `task.fork()` | Schedule subtask |
| `task.join()` | Wait for subtask result |
| `task.compute()` | Execute task logic directly |

---

# 148. Common ForkJoinPool

Java provides:

```java
ForkJoinPool.commonPool()
```

It is shared by features such as:

```text
many CompletableFuture async methods
parallel streams
ForkJoin usage outside an explicit pool in relevant cases
```

Do not occupy it with long blocking operations carelessly.

---

# 149. `newWorkStealingPool()`

```java
Executors.newWorkStealingPool();
```

or:

```java
Executors.newWorkStealingPool(4);
```

provides a work-stealing executor.

Useful for:

```text
small independent tasks
recursive work
```

It does not guarantee execution order.

---

# 150. ForkJoinPool vs Normal Thread Pool

Normal pool:

```text
submit independent tasks
→ shared queue
→ worker executes
```

ForkJoinPool:

```text
task
→ splits recursively
→ worker-local queues
→ work stealing
→ join results
```

---

# PART 11 — THREADLOCAL

# 151. What Is `ThreadLocal`?

`ThreadLocal<T>` stores a separate value for each thread.

Example:

```java
private static final
ThreadLocal<String> USER =
    new ThreadLocal<>();
```

Thread-1:

```java
USER.set("Harshita");
```

Thread-2:

```java
USER.set("Aditya");
```

Each thread sees its own value.

---

# 152. `ThreadLocal` Methods

```java
set(value)
get()
remove()
```

Use cleanup:

```java
CONTEXT.set(value);

try {
    performWork();

} finally {
    CONTEXT.remove();
}
```

---

# 153. When Is ThreadLocal Useful?

When contextual information needs to flow through several method calls without being explicitly passed through every layer.

Examples:

```text
request ID
tracing information
security context
tenant information
transaction context
```

But explicit parameters are often clearer when practical.

---

# 154. ThreadLocal + Thread Pools

This is very important.

Workers are reused:

```text
Task A → Worker 1
Task A finishes
Task B → Worker 1
```

If Task A leaves a ThreadLocal value:

```text
Task B may see stale data
```

Therefore:

```java
try {
    // use ThreadLocal
}
finally {
    threadLocal.remove();
}
```

---

# PART 12 — VIRTUAL THREADS

# 155. Platform Threads

Traditional Java threads are called:

> **Platform threads**

Conceptually:

```text
Java thread
   ↓
OS thread
   ↓
CPU
```

They are relatively expensive compared with virtual threads.

---

# 156. Why Blocking Work Is Hard With Platform Threads

Typical request:

```text
receive request
 ↓
database call
 ↓
wait
 ↓
remote API
 ↓
wait
 ↓
respond
```

The thread may spend most of its lifetime waiting.

High concurrency therefore requires many platform threads.

---

# 157. Virtual Thread

A virtual thread is a lightweight Java thread scheduled by the JVM/runtime.

Conceptually:

```text
Many virtual threads
        ↓
JVM scheduler
        ↓
Fewer carrier platform threads
        ↓
OS / CPU
```

The platform thread temporarily executing a virtual thread is its:

> **Carrier thread**

---

# 158. Mounting / Unmounting

```text
Virtual thread ready
        ↓
mounted on carrier
        ↓
executes
        ↓
supported blocking I/O
        ↓
can unmount
        ↓
carrier becomes available
        ↓
another virtual thread can run
```

When the blocking operation becomes ready, the virtual thread can resume, potentially on another carrier.

---

# 159. Virtual Threads Do Not Create More CPU

Virtual threads improve:

```text
concurrency
scalability
for waiting/blocking tasks
```

They do not increase:

```text
CPU core count
```

Therefore:

```text
CPU-bound
→ still limited by CPU capacity

I/O/blocking-heavy
→ virtual threads can improve concurrency significantly
```

---

# 160. Java Version

Virtual threads became a permanent Java feature in:

```text
Java 21
```

Important APIs:

```java
Thread.startVirtualThread(...)

Thread.ofVirtual()

Executors.newVirtualThreadPerTaskExecutor()
```

---

# 161. Creating a Virtual Thread

```java
Thread thread =
    Thread.startVirtualThread(() -> {

        System.out.println(
            Thread.currentThread()
        );

    });
```

Builder form:

```java
Thread thread =
    Thread.ofVirtual()
        .name("order-task")
        .start(task);
```

Unstarted form:

```java
Thread thread =
    Thread.ofVirtual()
        .name("order-task")
        .unstarted(task);

thread.start();
```

---

# 162. `isVirtual()`

```java
Thread.currentThread().isVirtual();
```

returns whether the current thread is virtual.

---

# 163. Virtual-Thread-Per-Task Executor

```java
ExecutorService executor =
    Executors
        .newVirtualThreadPerTaskExecutor();
```

Conceptually:

```text
Task 1 → Virtual Thread 1
Task 2 → Virtual Thread 2
Task 3 → Virtual Thread 3
```

This is **not a traditional thread pool**.

The intended model is:

```text
one task
→ one virtual thread
→ task finishes
→ virtual thread ends
```

---

# 164. Do Not Pool Virtual Threads

Platform threads benefit from pooling because creation is relatively expensive.

Virtual threads are cheap enough that:

```text
one task
→ one virtual thread
```

is usually the preferred model.

Limit concurrency around the scarce resource instead of artificially pooling virtual threads.

---

# 165. Virtual Threads and Resource Limits

Virtual threads do not make every dependency unlimited.

Still-limited resources include:

```text
Database connections
API rate limits
Memory
File descriptors
Sockets
Downstream capacity
```

Example:

```java
Semaphore dbLimit =
    new Semaphore(50);
```

This limits concurrent access to the database even if many virtual threads exist.

### Core principle

> **Threads can be cheap while dependencies remain expensive.**

---

# 166. Virtual Threads + `synchronized`

Modern JDKs have reduced important sources of monitor-related pinning.

Still, long blocking operations in native/foreign code can occupy carriers.

Practical rule:

```text
Keep critical sections small
Do not hold locks during long external I/O
Measure real behavior
```

---

# 167. Virtual Threads + ThreadLocal

Virtual threads support ThreadLocal.

But large numbers of virtual threads mean large ThreadLocal payloads can create memory pressure.

Prefer:

```text
small context values
remove values when done
explicit parameters where practical
```

---

# PART 13 — MASTER COMPARISONS

# 168. `run()` vs `start()`

| `run()` | `start()` |
|---|---|
| Normal method call | Starts new thread execution |
| Same/current thread | New execution path |
| No new stack for a new thread | New thread execution state |
| Does not provide concurrency by itself | Enables concurrent execution |

---

# 169. `sleep()` vs `wait()` vs `join()`

| Method | Purpose | Releases monitor? |
|---|---|---|
| `sleep()` | Timed pause of current thread | No |
| `wait()` | Condition-based coordination | Yes, the invoked object's monitor |
| `join()` | Wait for another thread to terminate | Not generally a monitor-release operation of the target |

---

# 170. `volatile` vs `synchronized` vs Atomic

| Feature | `volatile` | `synchronized` | Atomic class |
|---|---|---|---|
| Visibility | ✅ | ✅ | ✅ for its operations |
| Ordering | ✅ around volatile access | ✅ | ✅ according to atomic operation semantics |
| Mutual exclusion | ❌ | ✅ | ❌ in the traditional lock sense |
| `count++` atomic | ❌ | ✅ when protected | Use atomic increment |
| Best for | Simple shared state/flags | Larger critical sections | Small atomic state transitions |

---

# 171. `synchronized` vs `Lock`

| `synchronized` | `Lock` |
|---|---|
| Simple syntax | Explicit API |
| Automatic release | Manual release |
| No direct `tryLock()` | `tryLock()` |
| Limited fairness options | Fairness options in implementations |
| `wait/notify` with monitor | `Condition` with Lock |
| Excellent for straightforward protection | Useful for advanced control |

---

# 172. `Lock` vs Semaphore

```text
Lock
→ who owns the lock?

Semaphore
→ how many permits are available?
```

---

# 173. `Future` vs `CompletableFuture`

| `Future` | `CompletableFuture` |
|---|---|
| Represents pending result | Represents pending result + completion stage |
| `get()` central | Composition is central |
| Blocking-oriented retrieval | Fluent asynchronous pipeline |
| Basic cancellation/status APIs | Transform/combine/recover/observe |
| Good for simple single result | Good for async workflows |

---

# 174. `CompletableFuture` vs Virtual Threads

```text
CompletableFuture
→ represent work as async stages
→ transform/combine stages
→ callback-style composition

Virtual Threads
→ write sequential blocking code
→ one lightweight thread per task
→ scale many waiting tasks
```

They are not direct replacements.

---

# 175. ForkJoinPool vs Virtual Threads

| ForkJoinPool | Virtual Threads |
|---|---|
| CPU-bound parallel work | High-concurrency blocking work |
| Divide-and-conquer | Thread-per-task/request |
| Recursive subtasks | Ordinary sequential blocking code |
| Work stealing | JVM scheduling over carriers |
| Useful CPU parallelism | Concurrency can greatly exceed CPU count |
| Avoid long blocking tasks | Blocking is a primary use case |

---

# 176. `ThreadPoolExecutor` Task Flow

Remember this exact sequence:

```text
New task
   ↓
core threads available?
   ↓
YES → create/use core worker
NO
   ↓
queue accepts?
   ↓
YES → queue task
NO
   ↓
worker count below max?
   ↓
YES → create extra worker
NO
   ↓
reject
```

### Golden interview sentence

> **Core first → queue second → max threads third → rejection last.**

---

# PART 14 — MASTER INTERVIEW QUESTIONS

# 177. Foundation Questions

### What is a thread?

An independent path of execution inside a process.

### Program vs process?

A program is passive stored instructions; a process is a running instance.

### Concurrency vs parallelism?

Concurrency is overlapping progress; parallelism is simultaneous execution.

### What is the main thread?

The JVM-created thread that invokes `main()`.

### What memory is private to each Java thread?

Its execution-related state, especially its Java stack and PC register.

---

# 178. Thread API Questions

### Why is `Runnable` generally preferred over extending `Thread`?

Separation of concerns, inheritance flexibility, reuse, and compatibility with executor-based designs.

### Difference between `run()` and `start()`?

`run()` is a normal call; `start()` begins independent thread execution.

### Can a thread be restarted?

No. Create a new thread object.

### Does `sleep()` release locks?

No.

### What does `join()` do?

Makes the calling thread wait for another thread to terminate.

### Does `yield()` guarantee a context switch?

No.

### Does `interrupt()` kill a thread?

No. It is a cooperative interruption request.

### Difference between `isInterrupted()` and `Thread.interrupted()`?

Instance check without clearing vs current-thread check that clears the status.

---

# 179. Thread Lifecycle Questions

### Six Java states?

```text
NEW
RUNNABLE
BLOCKED
WAITING
TIMED_WAITING
TERMINATED
```

### Does Java have a separate RUNNING state?

No.

### Difference between BLOCKED and WAITING?

```text
BLOCKED
→ waiting for monitor

WAITING
→ waiting indefinitely for coordination
```

---

# 180. Synchronization Questions

### What is a critical section?

Code accessing shared mutable state that requires coordination.

### What does synchronized provide?

```text
Mutual exclusion
Visibility
Ordering
```

### What lock does an instance synchronized method use?

```java
this
```

### What lock does a static synchronized method use?

```java
ClassName.class
```

### What lock does a synchronized block use?

The object specified inside the parentheses.

### Why should custom locks be `private final`?

To avoid external misuse and prevent lock-reference replacement.

### Does `synchronized` stop the whole application?

No.

### Can two synchronized methods run concurrently?

Yes, if they use different monitors.

---

# 181. Inter-Thread Communication Questions

### Why use `wait()`?

To wait for a condition without busy-waiting and release the monitor.

### Why are `wait()` and `notify()` in `Object`?

Because they operate on an object's monitor.

### What does `notify()` do?

Wakes one waiting thread.

### What does `notifyAll()` do?

Wakes all waiting threads so they can compete to reacquire the monitor and re-check conditions.

### Why use `while` instead of `if`?

Because conditions must be re-checked after waking, including for spurious wakeups and changes made by other threads.

### What is deadlock?

Threads wait indefinitely for resources held by one another.

---

# 182. Advanced Lock Questions

### Why use `ReentrantLock`?

For explicit control, `tryLock()`, timeouts, and configurable fairness.

### What does reentrant mean?

The same thread can reacquire the same lock.

### What is ReadWriteLock?

Shared read access + exclusive write access.

### What is StampedLock optimistic reading?

Read first without a full read lock, then validate; if invalid, fall back to a real read lock.

### Is StampedLock reentrant?

No, not generally.

### What is a Semaphore?

A permit-based concurrency limiter.

### What is Condition?

A flexible waiting/signaling mechanism associated with an explicit lock.

---

# 183. Atomic / CAS Questions

### What is CAS?

Compare-And-Set; update only if the current value still equals the expected value.

### What happens when CAS fails?

The update is not performed; a lock-free algorithm may re-read and retry.

### Why is CAS optimistic?

It tries the update first and handles conflicts through failure/retry rather than traditional lock ownership.

### Does AtomicInteger make every block atomic?

No.

### What is the ABA problem?

A value changes:

```text
A → B → A
```

and a simple CAS can miss the intermediate change.

### How can ABA be detected?

Track a version/stamp together with the value.

---

# 184. Executor Framework Questions

### Why use the Executor Framework?

To separate task submission from thread management and reuse controlled worker threads.

### Thread pool?

Reusable worker threads that process submitted tasks.

### `execute()` vs `submit()`?

```text
execute()
→ Runnable
→ no Future

submit()
→ Runnable / Callable
→ Future
```

### Runnable vs Callable?

```text
Runnable → no result
Callable → result + checked exceptions
```

### What is Future?

A handle representing an asynchronous computation's result/status.

### Can `Future.get()` block?

Yes.

### `shutdown()` vs `shutdownNow()`?

```text
shutdown()
→ graceful

shutdownNow()
→ best-effort interruption + return queued tasks
```

### What is `CallerRunsPolicy`?

The submitting thread runs the task, which can create backpressure.

### Core → queue → max → reject?

Yes.

---

# 185. CompletableFuture Questions

### Why is CompletableFuture more powerful than Future?

It supports composition:

```text
transform
combine
consume
recover
observe
```

### `runAsync()` vs `supplyAsync()`?

```text
runAsync()
→ no result

supplyAsync()
→ result
```

### `thenApply()`?

Transform result.

### `thenAccept()`?

Consume result.

### `thenRun()`?

Run action after completion.

### `thenCombine()`?

Combine two successful results.

### `exceptionally()`?

Recover with a fallback.

### `whenComplete()`?

Observe success/failure.

### `handle()`?

Inspect result/exception and produce a new result.

### `thenApply()` vs `thenApplyAsync()`?

Non-async continuation has no new-thread guarantee; async version is scheduled through an async executor.

---

# 186. ForkJoinPool Questions

### What workload is ForkJoinPool best for?

CPU-bound divide-and-conquer work.

### What is work stealing?

Idle worker steals available work from another worker's local queue.

### `RecursiveTask` vs `RecursiveAction`?

```text
RecursiveTask<V>
→ result

RecursiveAction
→ no result
```

### Why use a threshold?

To stop recursive splitting when tasks become small enough.

---

# 187. ThreadLocal Questions

### What does ThreadLocal do?

Stores separate state for each thread.

### Why is cleanup important in a thread pool?

Worker threads are reused, so stale values can leak into later tasks.

### Cleanup pattern?

```java
try {
    // use ThreadLocal
}
finally {
    threadLocal.remove();
}
```

---

# 188. Virtual Thread Questions

### What is a virtual thread?

A lightweight JVM-scheduled Java thread.

### What is a carrier thread?

A platform thread temporarily executing a virtual thread.

### Do virtual threads increase CPU?

No.

### Best use case?

High-concurrency workloads with lots of blocking/waiting.

### Are virtual threads a good replacement for CPU parallelism?

No. CPU-bound work still needs controlled parallelism.

### Should virtual threads be pooled?

Generally no. Use one virtual thread per task.

### Does a virtual thread remove database limits?

No. Limit the scarce resource separately, often with a semaphore or pool.

---

# PART 15 — COMMON MISTAKES TO NEVER MAKE

```text
1. run() creates a new thread.
   ❌ False.

2. start() guarantees execution order.
   ❌ False.

3. sleep() releases locks.
   ❌ False.

4. interrupt() forcibly kills a thread.
   ❌ False.

5. RUNNABLE means definitely on CPU.
   ❌ False.

6. volatile makes count++ atomic.
   ❌ False.

7. synchronized only provides mutual exclusion.
   ❌ Incomplete.

8. Any synchronized blocks protect each other.
   ❌ False; they need the same monitor.

9. synchronized(new Object()) is a shared lock.
   ❌ False.

10. wait() always means notify() is required.
    ❌ Not always; join()/park() can also cause WAITING.

11. sleep() and wait() behave the same.
    ❌ False.

12. notifyAll() guarantees all waiting threads run immediately.
    ❌ False.

13. AtomicInteger makes any multi-step block atomic.
    ❌ False.

14. CAS cannot fail.
    ❌ False.

15. Lock-free means no retries or no CPU cost.
    ❌ False.

16. More threads always mean more performance.
    ❌ False.

17. Future.get() is non-blocking.
    ❌ False.

18. maximumPoolSize means the pool immediately creates max threads.
    ❌ False.

19. Unbounded queue solves overload.
    ❌ False; backlog can grow.

20. shutdownNow() kills tasks instantly.
    ❌ False; it is best-effort interruption.

21. CompletableFuture is automatically non-blocking.
    ❌ False.

22. thenApply() always creates a new thread.
    ❌ False.

23. ForkJoinPool is the best executor for every workload.
    ❌ False.

24. Virtual threads create more CPU capacity.
    ❌ False.

25. Virtual threads remove downstream resource limits.
    ❌ False.

26. ThreadLocal is automatically safe in a thread pool.
    ❌ False; stale values must be removed.
```

---

# PART 16 — MOST IMPORTANT CODE PATTERNS

# 189. Basic Runnable Thread

```java
Runnable task = () -> {
    System.out.println(
        Thread.currentThread().getName()
    );
};

Thread worker =
    new Thread(task, "worker-1");

worker.start();
```

---

# 190. Safe Synchronized Critical Section

```java
private final Object lock =
    new Object();

void increment() {

    synchronized (lock) {
        count++;
    }
}
```

---

# 191. Guarded Wait

```java
synchronized (lock) {

    while (!condition) {
        lock.wait();
    }

    // proceed
}
```

---

# 192. Notify

```java
synchronized (lock) {

    condition = true;

    lock.notifyAll();
}
```

---

# 193. ReentrantLock

```java
lock.lock();

try {
    // critical section
}
finally {
    lock.unlock();
}
```

---

# 194. AtomicInteger

```java
AtomicInteger count =
    new AtomicInteger(0);

count.incrementAndGet();
```

---

# 195. CAS Loop

```java
while (true) {

    int current =
        value.get();

    int next =
        current + 1;

    if (value.compareAndSet(
            current,
            next)) {

        break;
    }
}
```

---

# 196. Thread Pool

```java
ExecutorService executor =
    Executors.newFixedThreadPool(4);

executor.submit(task);

executor.shutdown();
```

---

# 197. Callable + Future

```java
Future<Integer> future =
    executor.submit(() -> {
        return 100;
    });

try {
    Integer result =
        future.get();

} catch (InterruptedException e) {

    Thread.currentThread().interrupt();

} catch (ExecutionException e) {

    System.out.println(
        e.getCause()
    );
}
```

---

# 198. CompletableFuture Pipeline

```java
CompletableFuture<Void> pipeline =
    CompletableFuture
        .supplyAsync(() -> "User Data")
        .thenApply(data ->
            data + " processed"
        )
        .thenAccept(System.out::println);
```

---

# 199. CompletableFuture Combination

```java
CompletableFuture<Integer> first =
    CompletableFuture.supplyAsync(
        () -> 10
    );

CompletableFuture<Integer> second =
    CompletableFuture.supplyAsync(
        () -> 20
    );

CompletableFuture<Integer> total =
    first.thenCombine(
        second,
        Integer::sum
    );
```

---

# 200. ForkJoin Pattern

```java
class SumTask
        extends RecursiveTask<Integer> {

    private static final int THRESHOLD = 10;

    @Override
    protected Integer compute() {

        if (/* small enough */) {
            return /* direct calculation */;
        }

        SumTask left = /* split */;
        SumTask right = /* split */;

        left.fork();

        int rightResult =
            right.compute();

        int leftResult =
            left.join();

        return leftResult + rightResult;
    }
}
```

---

# 201. ThreadLocal Cleanup

```java
CONTEXT.set(value);

try {
    performWork();

} finally {
    CONTEXT.remove();
}
```

---

# 202. Virtual Thread Per Task

```java
try (ExecutorService executor =
        Executors.newVirtualThreadPerTaskExecutor()) {

    executor.submit(() -> {
        // blocking task
    });
}
```

---

# PART 17 — MASTER DECISION GUIDE

# 203. Which Tool Should I Use?

```text
Need simple mutual exclusion?
        ↓
synchronized

Need explicit locking / tryLock / fairness?
        ↓
ReentrantLock

Many readers, fewer writers?
        ↓
ReentrantReadWriteLock

Advanced optimistic reads?
        ↓
StampedLock

Need N concurrent permits?
        ↓
Semaphore

Need multiple wait conditions with explicit lock?
        ↓
Condition

Need a simple atomic counter?
        ↓
AtomicInteger

Need atomic reference update?
        ↓
AtomicReference

Need atomic array-element reference updates?
        ↓
AtomicReferenceArray

Need one asynchronous result?
        ↓
Future

Need async transformation / combination / recovery?
        ↓
CompletableFuture

Need CPU-bound divide-and-conquer?
        ↓
ForkJoinPool

Need per-thread context?
        ↓
ThreadLocal

Need very high concurrency with blocking I/O?
        ↓
Virtual Threads
```

---

# PART 18 — FINAL QUICK REVISION CHEAT SHEET

## Thread Basics

```text
Program
→ stored code

Process
→ running instance

Thread
→ execution path

Concurrency
→ overlapping progress

Parallelism
→ simultaneous execution
```

---

## Thread Creation

```text
extends Thread
implements Runnable
```

Preferred general model:

```text
Runnable = task
Thread / Executor = execution
```

---

## Thread Execution

```text
run()
→ normal call

start()
→ new thread execution
```

---

## Thread States

```text
NEW
RUNNABLE
BLOCKED
WAITING
TIMED_WAITING
TERMINATED
```

---

## Thread Methods

```text
sleep()
→ timed pause

join()
→ wait for another thread

yield()
→ scheduling hint

interrupt()
→ cooperative interruption request

isAlive()
→ started and not terminated

currentThread()
→ current thread

setName()
→ debugging/observability

setPriority()
→ scheduling hint

setDaemon()
→ background thread
```

---

## Concurrency Problems

```text
Race condition
→ result depends on timing

Atomicity
→ no unsafe halfway interference

Visibility
→ latest write is observable

Ordering
→ operations observed in required order

Thread interference
→ operations disrupt each other

Data inconsistency
→ state violates expected rules
```

---

## Synchronization

```text
synchronized
→ mutual exclusion
→ visibility
→ ordering
```

Locks:

```text
instance synchronized → this

static synchronized   → ClassName.class

synchronized(lock)    → lock
```

---

## Inter-Thread Communication

```text
wait()
→ releases monitor
→ WAITING

notify()
→ wake one waiter

notifyAll()
→ wake all waiters

Guarded block
→ while, not if
```

---

## Advanced Locks

```text
ReentrantLock
→ explicit control / tryLock / fairness

ReadWriteLock
→ multiple readers / exclusive writer

StampedLock
→ optimistic read

Semaphore
→ permits

Condition
→ multiple condition queues
```

---

## Atomic / CAS

```text
AtomicInteger
→ atomic numeric operations

AtomicReference
→ atomic reference operations

CAS
→ compare current with expected
→ update if still equal

CAS failure
→ retry

ABA
→ A → B → A

Versioning
→ (value, version)
```

---

## Executor Framework

```text
Executor
→ basic execution

ExecutorService
→ lifecycle + richer APIs

ThreadPoolExecutor
→ configurable pool

execute()
→ Runnable

submit()
→ Runnable / Callable + Future

Future
→ async result handle
```

---

## ThreadPool Flow

```text
Core
 ↓
Queue
 ↓
Maximum
 ↓
Reject
```

---

## CompletableFuture

```text
runAsync()
→ no result

supplyAsync()
→ result

thenApply()
→ transform

thenAccept()
→ consume

thenRun()
→ run after

thenCombine()
→ combine

exceptionally()
→ fallback

whenComplete()
→ observe

handle()
→ observe + transform
```

---

## ForkJoinPool

```text
CPU-bound
↓
split
↓
fork
↓
work stealing
↓
join
↓
combine
```

---

## ThreadLocal

```text
same ThreadLocal object
→ different value per thread

Thread pools
→ clean with remove()
```

---

## Virtual Threads

```text
platform thread
→ OS-backed

virtual thread
→ JVM-scheduled lightweight thread

carrier thread
→ platform thread currently running virtual thread
```

Best for:

```text
blocking I/O
high concurrency
thread-per-request/task
```

Not a CPU multiplier.

---

# PART 19 — FINAL CONCEPT MAP

```text
                       JAVA MULTITHREADING
                               │
        ┌──────────────────────┼─────────────────────────┐
        │                      │                         │
     BASICS                 PROBLEMS                 SOLUTIONS
        │                      │                         │
 Program/Process/Thread    Race Condition          synchronized
        │                 Atomicity                Lock
 Concurrency/Parallelism   Visibility               RW Lock
        │                 Ordering                 StampedLock
        │                 Interference              Semaphore
        │                 Inconsistency              Condition
        │                                              │
        └──────────────────────┬───────────────────────┘
                               │
                         THREAD CONTROL
                               │
               ┌───────────────┼────────────────┐
               │               │                │
            Creation        Lifecycle        Communication
               │               │                │
          Thread/Runnable   NEW→...→END      wait/notify
               │
               └──────────────────┐
                                  │
                           ATOMIC / LOCK-FREE
                                  │
                       ┌──────────┴──────────┐
                       │                     │
                 Atomic Classes             CAS
                       │                     │
            AtomicInteger/Long/Boolean   Retry Loop
            AtomicReference              ABA
            AtomicReferenceArray         Versioning
                       │
                       └──────────┬──────────┘
                                  │
                         TASK EXECUTION
                                  │
                     ┌────────────┴─────────────┐
                     │                          │
                Executor Framework        CompletableFuture
                     │                          │
             Thread Pool / Queue         Async Pipeline
                     │                    Transform/Combine
             Future / Callable            Error Handling
                     │
                     └────────────┬─────────────┐
                                  │
                         ADVANCED EXECUTION
                                  │
                ┌─────────────────┼─────────────────┐
                │                 │                 │
           ForkJoinPool       ThreadLocal      Virtual Threads
                │                 │                 │
        Divide & Conquer     Per-thread data    Blocking I/O
        Work Stealing        Cleanup            High Concurrency
```

---

# PART 20 — THE BIGGEST INTERVIEW MENTAL MODELS

## 1. What actually happens when Java runs?

```text
Java source
   ↓
bytecode
   ↓
OS process
   ↓
JVM
   ↓
main thread
   ↓
additional threads
   ↓
CPU execution
```

---

## 2. Why does shared data become dangerous?

```text
Shared
+
Mutable
+
Multiple threads
+
No coordination
        ↓
Race / Visibility / Ordering problems
```

---

## 3. How does synchronization help?

```text
Critical section
      ↓
Monitor
      ↓
One owner
      ↓
Others block
      ↓
Visibility + Ordering
```

---

## 4. How does CAS differ?

```text
Read
 ↓
Calculate
 ↓
CAS
 ↓
Success → done
Failure → retry
```

---

## 5. How does Executor Framework change programming?

```text
OLD
Task → create Thread → execute

NEW
Task → submit → Executor manages workers
```

---

## 6. How do the advanced tools differ?

```text
Future
→ one async result

CompletableFuture
→ async pipeline

ForkJoinPool
→ CPU divide-and-conquer

Virtual Threads
→ many blocking tasks
```

---

# 🏆 Final 30-Second Revision

> **A Java process contains multiple threads that may execute concurrently and, on multiple cores, in parallel. Threads share process-level memory such as the heap but have private execution state such as their Java stacks. Threads can be created using `Thread` or `Runnable`, and `start()` is what begins new thread execution; `run()` is just a method call. Multithreaded programs become difficult because shared mutable state creates race conditions, atomicity problems, visibility problems, and ordering issues. `synchronized` protects critical sections through monitors and provides mutual exclusion, visibility, and ordering. `wait()`/`notify()` enable condition-based communication. Advanced utilities such as `ReentrantLock`, `ReadWriteLock`, `StampedLock`, `Semaphore`, and `Condition` provide specialized control. Atomic classes and CAS provide lock-free techniques for suitable small state transitions, with ABA/versioning as an important advanced concern. The Executor Framework manages reusable worker threads and task queues; `Future` represents one asynchronous result, while `CompletableFuture` enables asynchronous pipelines. `ForkJoinPool` is designed for CPU-bound divide-and-conquer work, `ThreadLocal` provides per-thread context, and virtual threads make large numbers of blocking tasks practical without requiring the same number of platform threads.**

---

# ⭐ FINAL PRINCIPLE

> **The goal of multithreading is not "use more threads." The goal is to make concurrent work correct, efficient, observable, and scalable by choosing the right coordination and execution mechanism for the workload.**
