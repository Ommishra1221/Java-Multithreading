# Java Multithreading — Lecture 4 Notes

## Problems in Multithreading: Race Conditions, Atomicity, Visibility, Ordering & Thread Interference

This lecture is the foundation for understanding **why multithreaded programs become unsafe** and why mechanisms such as `synchronized`, `volatile`, atomic classes, locks, and thread-safe collections are needed.

The central idea is:

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
Wrong or inconsistent results
```

The three most important guarantees introduced here are:

```text
Atomicity  → Can another thread interfere halfway?

Visibility → Can another thread see the latest value?

Ordering   → In what order are writes/operations observed?
```

---

# 1. Why Multithreading Creates Problems

Multiple threads can make progress inside the same program, but problems appear when they access the same **mutable shared data** without proper coordination.

Major problems:

1. Race conditions
2. Atomicity problems
3. Visibility problems
4. Ordering problems
5. Thread interference
6. Data inconsistency

These concepts are closely related, but they are **not identical**.

---

# 2. Shared Resource

A **shared resource** is data or an object that can be accessed by more than one thread.

Examples:

```java
int count = 0;

List<Integer> numbers;

BankAccount account;
```

Other examples include:

- Heap objects
- Static variables
- Shared collections
- Files
- Database connections
- Network resources

### Important

A resource becomes especially dangerous when it is:

```text
SHARED
  +
MUTABLE
```

Immutable data is much easier to share safely because its state cannot change after creation.

---

# 3. Critical Section

A **critical section** is the part of the program that reads or modifies shared mutable data and therefore requires controlled access.

Example:

```java
class Counter {

    int count = 0;

    void increment() {
        count++;
    }
}
```

Here:

```text
Shared resource → count

Critical section → count++
```

A critical section can be:

- one statement, or
- several related statements

### Another example

```java
if (balance >= amount) {
    balance = balance - amount;
}
```

The **check + update together** form one logical critical section.

The danger is that another thread may execute between those steps.

---

# 4. Race Condition

## Definition

A **race condition** occurs when the correctness of a program depends on the unpredictable timing or execution order of multiple threads.

In simple terms:

> Multiple threads access the same shared state, and the final result depends on which thread executes at which time.

---

# 5. Why `count++` Causes a Race Condition

Consider:

```java
count++;
```

It looks like one statement, but conceptually it involves multiple operations:

```text
1. Read count
2. Add 1
3. Write updated count
```

So:

```text
count++
```

is **not atomic**.

Another thread can execute between these steps.

---

# 6. Classic Race Condition Example

```java
class Counter {

    int count = 0;

    void increment() {
        count++;
    }
}
```

Suppose:

```text
count = 0
```

Two threads call `increment()`.

## Expected execution

```text
Thread-1: 0 → 1
Thread-2: 1 → 2

Final count = 2
```

## Possible unsafe execution

| Step | Thread-1 | Thread-2 |
|---|---|---|
| 1 | Reads `count = 0` | |
| 2 | | Reads `count = 0` |
| 3 | Calculates `1` | |
| 4 | | Calculates `1` |
| 5 | Writes `1` | |
| 6 | | Writes `1` |

Final:

```text
count = 1
```

Two increments were attempted, but one update was lost.

That is a **race condition** and a **lost update**.

---

# 7. Complete Race Condition Program

```java
class Counter {

    int count = 0;

    void increment() {
        count++;
    }
}

public class RaceConditionDemo {

    public static void main(String[] args)
            throws InterruptedException {

        Counter counter = new Counter();

        Thread t1 = new Thread(() -> {
            for (int i = 1; i <= 100_000; i++) {
                counter.increment();
            }
        });

        Thread t2 = new Thread(() -> {
            for (int i = 1; i <= 100_000; i++) {
                counter.increment();
            }
        });

        t1.start();
        t2.start();

        t1.join();
        t2.join();

        System.out.println(counter.count);
    }
}
```

Expected result:

```text
200000
```

But a possible actual result could be:

```text
173482
```

The exact incorrect value is not fixed. It depends on scheduling and the number of overlapping updates.

A race condition may even produce the correct answer sometimes and an incorrect answer at other times. That makes it difficult to reproduce and debug.

---

# 8. Atomicity

## Definition

An operation is **atomic** when it appears to other threads as one indivisible action.

Conceptually:

```text
START
  ↓
COMPLETE AS ONE UNIT
```

Another thread cannot safely interfere halfway through the logical operation.

### Core idea

> **Atomicity prevents unsafe interleaving of a logical operation.**

---

# 9. `count++` and Atomicity

Conceptually:

```java
int temp = count;
temp = temp + 1;
count = temp;
```

Therefore:

```text
READ
 ↓
MODIFY
 ↓
WRITE
```

Another thread can run between these steps.

So:

```java
count++;
```

is not atomic.

---

# 10. Check-Then-Act Problem

A very important atomicity problem is:

```text
CHECK
 ↓
ACT
```

Example:

```java
void withdraw(int amount) {

    if (balance >= amount) {
        balance = balance - amount;
    }
}
```

Suppose:

```text
balance = 1000
```

Two threads both try to withdraw:

```text
800
```

Possible execution:

```text
Thread-1 checks → balance sufficient
Thread-2 checks → balance sufficient

Thread-1 deducts 800
Thread-2 deducts 800
```

Both threads made their decision using the old balance.

The **check and update must be treated as one logical atomic operation**.

---

# 11. Real-World Atomicity Example — Money Transfer

Consider:

```text
Account A
   ↓
Deduct money
   ↓
Account B
   ↓
Add money
```

If only the first step completes:

```text
A → money deducted
B → money not credited
```

the system becomes inconsistent.

So the logical transfer should not expose a partially completed state.

---

# 12. How to Solve Atomicity Problems

Common mechanisms include:

## `synchronized`

```java
synchronized void increment() {
    count++;
}
```

## Atomic Classes

```java
AtomicInteger count = new AtomicInteger();

count.incrementAndGet();
```

## Explicit Locks

```java
Lock lock = new ReentrantLock();
```

The correct mechanism depends on the operation and how much state must remain consistent.

---

# 13. Atomic vs Non-Atomic Operations

## Simple reads and writes

Simple reads/writes of most Java primitive values and object references are atomic.

For example:

```java
int x = 10;

x = 20;

int y = x;
```

Another thread does not observe half of an `int` value or half of an object reference.

### But remember

> Atomicity alone does **not** guarantee visibility.

A write may be atomic but another thread still may not reliably observe it without the required memory-ordering/visibility guarantee.

---

# 14. Operations That Are Not Atomic

### Increment/decrement

```java
count++;

count--;
```

Conceptually:

```text
read
 ↓
modify
 ↓
write
```

### Compound assignment

```java
x += 5;
```

Conceptually:

```java
x = x + 5;
```

It contains a read and write, so it is not one indivisible operation.

### Check-then-act

```java
if (balance > 0) {
    withdraw();
}
```

The check and action can be separated by another thread.

### Compound collection logic

Even if individual collection methods are thread-safe, a sequence of calls may not be atomic.

Example:

```java
if (!map.containsKey(key)) {
    map.put(key, value);
}
```

The overall check-then-put operation is not automatically atomic.

An API such as:

```java
map.putIfAbsent(key, value);
```

is designed for this type of atomic action.

---

# 15. Long and Double — Important Technical Note

The lecture notes highlight a Java Language Specification caveat:

A non-volatile `long` or `double` read/write is permitted to be treated as two separate 32-bit operations.

Example:

```java
long value = 100L;

double amount = 50.5;
```

Modern JVMs/processors usually perform these atomically, but portable Java code should not depend on that implementation detail.

Declaring them `volatile` guarantees atomic reads/writes of the variable itself.

However:

```java
volatile long value;

value++;
```

is still **not** an atomic compound operation.

Why?

```text
read
 +
increment
 +
write
```

---

# 16. Visibility Problem

A **visibility problem** occurs when:

> One thread updates shared data, but another thread does not reliably observe the updated value.

The write may have happened correctly.

The issue is that the Java Memory Model does not guarantee that another thread will see that write without an appropriate **happens-before relationship**.

---

# 17. Simplified Mental Model of Visibility

A beginner-friendly model is:

```text
Thread
   ↓
CPU registers / caches
   ↓
Shared memory
```

One thread may work with a locally cached/register value while another thread has changed the shared value.

### Important correction

Do **not** memorize:

> `volatile` forces every read/write directly to RAM.

That is an oversimplification.

The Java Memory Model defines visibility and ordering guarantees through happens-before relationships. It does not define visibility simply as "RAM vs CPU cache."

---

# 18. Visibility Example

```java
class VisibilityDemo {

    static boolean flag = false;

    public static void main(String[] args) {

        Thread updater = new Thread(() -> {

            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            flag = true;

            System.out.println("Flag updated to true");
        });

        Thread observer = new Thread(() -> {

            while (!flag) {
                // waiting
            }

            System.out.println("Detected flag change");
        });

        updater.start();
        observer.start();
    }
}
```

Expected behavior:

```text
Updater changes flag to true
        ↓
Observer exits loop
```

But without proper synchronization, the observer is **not guaranteed** to detect the update.

The write happened, but the other thread does not have the required visibility guarantee.

---

# 19. `volatile`

To provide visibility for suitable variables:

```java
static volatile boolean flag = false;
```

A volatile write happens-before a subsequent volatile read of the same variable.

Conceptually:

```text
Thread-1
flag = true
   ↓
volatile write

        happens-before

   ↓
volatile read
Thread-2 reads flag
```

Therefore, if Thread-2 observes the volatile write to `true`, it must observe that updated value.

---

# 20. What `volatile` Guarantees

`volatile` provides:

1. Visibility
2. Ordering guarantees around the volatile access
3. Atomic reads/writes of the volatile variable itself

Typical examples:

```java
volatile boolean running;

volatile boolean shutdownRequested;

volatile int currentState;
```

It is particularly useful for **simple state communication** where one thread publishes a value and other threads observe it.

---

# 21. What `volatile` Does NOT Guarantee

This is one of the most important points of Lecture 4.

```java
volatile int count = 0;

count++;
```

`volatile` does **not** make `count++` atomic.

It is still:

```text
READ count
   ↓
ADD 1
   ↓
WRITE count
```

Two threads can still read the same old value and overwrite one another.

Therefore:

```text
volatile
→ visibility + ordering

volatile
≠
compound-operation atomicity
```

For atomic increments, use:

```java
AtomicInteger
```

or synchronization.

---

# 22. Atomicity vs Visibility

These are different problems.

## Atomicity

Question:

> Can another thread interfere halfway through this logical operation?

Example:

```java
count++;
```

## Visibility

Question:

> Can another thread see my latest write?

Example:

```java
volatile boolean running;
```

A variable can have an atomic write but still have a visibility problem.

---

# 23. Ordering Problem

An **ordering problem** concerns the sequence in which memory operations become observable between threads.

Consider:

```java
int data = 0;
boolean ready = false;
```

Thread-1:

```java
data = 10;
ready = true;
```

Thread-2:

```java
if (ready) {
    System.out.println(data);
}
```

Programmer intends:

```text
write data
   ↓
mark ready
   ↓
other thread sees ready
   ↓
other thread reads data
```

Without synchronization, that cross-thread ordering is not guaranteed.

---

# 24. Why Can Reordering Happen?

For performance, different components may optimize execution:

- compiler
- JVM
- CPU

Examples include:

- instruction scheduling
- register allocation
- pipelining
- store buffering
- other optimizations

The rule is that optimizations must preserve behavior allowed by the Java Memory Model.

Single-threaded reasoning can therefore look correct while unsynchronized multithreaded code can still expose surprising observations.

---

# 25. Classic Ordering Example

Initial:

```java
int data = 0;
boolean ready = false;
```

Thread-1:

```java
data = 10;
ready = true;
```

Thread-2:

```java
if (ready) {
    System.out.println(data);
}
```

You expect:

```text
10
```

But without a happens-before relationship, observing:

```text
ready == true
```

does not by itself guarantee observing the write:

```text
data = 10
```

A possible stale/unexpected observation is:

```text
ready = true
data = 0
```

This can be caused by visibility effects, reordering, or both.

---

# 26. Solving Ordering with `volatile`

Declare the publication flag as volatile:

```java
int data = 0;
volatile boolean ready = false;
```

Thread-1:

```java
data = 10;
ready = true;
```

Thread-2:

```java
if (ready) {
    System.out.println(data);
}
```

The important ordering is:

```text
data = 10
   ↓
volatile write: ready = true
   ↓
volatile read: ready == true
   ↓
read data
```

If Thread-2 observes the volatile write to `ready`, it must also observe the preceding write to `data`.

This is a common **safe-publication pattern**.

---

# 27. Solving Ordering with `synchronized`

The same monitor can establish visibility and ordering:

Writer:

```java
synchronized (lock) {
    data = 10;
    ready = true;
}
```

Reader:

```java
synchronized (lock) {
    if (ready) {
        System.out.println(data);
    }
}
```

The guarantee comes from:

```text
Thread-1
monitor release
     ↓
same monitor
     ↓
Thread-2 monitor acquisition
```

Synchronization creates the necessary visibility and ordering relationship.

### Important

Both sides must coordinate using the **same synchronization policy/monitor**.

It is not enough for only one thread to use synchronization while another accesses the same state without coordinating.

---

# 28. Thread Interference

Thread interference occurs when operations from multiple threads interleave in a way that causes one thread to disturb another thread's work.

Example:

```text
Thread-1 reads count
Thread-2 reads count
Thread-1 writes new value
Thread-2 overwrites it
```

Typical ingredients:

```text
Shared mutable state
+
Non-atomic operation
+
Missing synchronization
+
Unsafe interleaving
```

---

# 29. Race Condition vs Thread Interference

These concepts are related but emphasize different things.

### Race Condition

Describes the broader correctness problem:

> The result depends on thread timing or ordering.

### Thread Interference

Describes the harmful overlap:

> One thread's operations interfere with another thread's operations.

A lost update can be both:

```text
Thread interference
+
Race condition
```

---

# 30. Data Inconsistency

**Data inconsistency** means the program's stored state no longer follows its expected rules or real-world meaning.

Many concurrency problems can produce inconsistency:

```text
Race conditions
Visibility problems
Ordering problems
Thread interference
```

---

# 31. Common Forms of Data Inconsistency

## Lost Update

```java
count++;
```

Two updates are attempted, but one overwrites the other.

## Stale Read

One thread reads an older value.

```java
while (!flag) {
    // may continue using stale value
}
```

## Inconsistent Read

A thread sees related fields from different logical moments.

Example:

```text
accountBalance → new state
transactionStatus → old state
```

## Partial Update

Only part of a multi-step operation has completed.

Example:

```text
Money deducted from Account A
but not yet credited to Account B
```

## Check-Then-Act Failure

A thread checks a condition, another thread changes the state, and the first thread acts based on the old condition.

## Unsafe Publication

A thread receives a reference before the object's state has been safely made visible.

---

# 32. Preventing Multithreading Problems

Several tools can be used.

## 1. `synchronized`

```java
synchronized (lock) {
    count++;
}
```

Useful when:

- several operations must execute as one unit
- multiple fields form one consistency rule
- mutual exclusion + visibility are needed
- threads need monitor-based coordination

---

# 33. Atomic Classes

Example:

```java
AtomicInteger count = new AtomicInteger();

count.incrementAndGet();
```

Common atomic classes:

```text
AtomicInteger
AtomicLong
AtomicBoolean
AtomicReference
```

Useful for simple atomic state transitions without protecting a larger compound operation.

---

# 34. Explicit Locks

Example:

```java
Lock lock = new ReentrantLock();

lock.lock();

try {
    count++;
} finally {
    lock.unlock();
}
```

Explicit locks can provide features such as:

- interruptible lock acquisition
- timed lock attempts
- fairness policies
- multiple condition objects

### Important

Because you manually acquire the lock, make sure it is released, normally using:

```java
finally
```

---

# 35. Thread-Safe Collections

Examples:

```text
ConcurrentHashMap
CopyOnWriteArrayList
BlockingQueue
ConcurrentLinkedQueue
```

These are designed for concurrent access.

However:

> A thread-safe collection does not automatically make every sequence of multiple operations atomic.

For example:

```java
if (!map.containsKey(key)) {
    map.put(key, value);
}
```

may still require a better atomic API such as:

```java
map.putIfAbsent(key, value);
```

---

# 36. Immutability

An immutable object does not change after creation.

Example:

```java
final class User {

    private final String name;
    private final int age;

    User(String name, int age) {
        this.name = name;
        this.age = age;
    }
}
```

Because its state cannot change after construction, a properly constructed and safely published immutable object is much easier to share between threads.

### Big idea

> Immutability removes many synchronization requirements by design.

---

# 37. Avoiding Shared Mutable State

A powerful concurrency principle is:

> **The safest shared mutable state is often the state that is not shared.**

Useful techniques:

- Thread-local variables
- Method-local variables
- Message passing
- Immutable snapshots
- Task confinement
- Giving each thread ownership of separate data

Good concurrent design often reduces sharing **before** adding locks.

---

# 38. Choosing the Correct Tool

| Requirement | Suitable Mechanism |
|---|---|
| Simple visible status flag | `volatile` |
| Atomic counter update | `AtomicInteger` |
| Several statements must be atomic together | `synchronized` or `Lock` |
| Concurrent map access | `ConcurrentHashMap` |
| Producer-consumer communication | `BlockingQueue` or coordination mechanisms |
| State should never change | Immutable object |
| Avoid sharing entirely | Thread confinement / message passing |

---

# 39. `volatile` vs `synchronized` vs `AtomicInteger`

| Feature | `volatile` | `synchronized` | `AtomicInteger` |
|---|---|---|---|
| Visibility | ✅ | ✅ | ✅ |
| Ordering guarantees | ✅ around volatile access | ✅ | ✅ through atomic operations |
| Simple read/write | ✅ | ✅ | ✅ |
| `count++` atomic | ❌ | ✅ when protected | ✅ with `incrementAndGet()` |
| Mutual exclusion | ❌ | ✅ | ❌ in the lock sense |
| Best for | Simple shared state/flags | Larger critical sections | Simple atomic numeric operations |

---

# 40. Lecture Example — Synchronized Critical Section

The uploaded lecture example protects the critical section with:

```java
synchronized(this) {
    count++;
}
```

The same `Counter` object is shared by both threads, and both threads wait with `join()` before printing the final count.

Conceptually:

```text
Thread-1 ──┐
           ├──> same Counter object
Thread-2 ──┘
                ↓
          synchronized(this)
                ↓
       only one thread at a time
```

This protects the critical section from unsafe simultaneous access.

---

# 41. Lecture Example — `volatile`

The uploaded example declares:

```java
static volatile boolean flag = false;
```

One thread changes:

```java
flag = true;
```

while another continuously checks:

```java
while(!flag) {
    // ...
}
```

This demonstrates the use of `volatile` for visibility of a shared flag.

The critical idea is:

```text
Thread 1
flag = true
      ↓
volatile write
      ↓
visibility guarantee
      ↓
Thread 2
reads flag
```

---

# 42. The Three Guarantees — Most Important Section

You should be very clear about these three.

## Atomicity

```text
Can another thread interfere halfway?
```

Example problem:

```java
count++;
```

Typical tools:

```text
synchronized
AtomicInteger
Lock
```

---

## Visibility

```text
Can another thread see the latest value?
```

Example:

```java
volatile boolean running;
```

Typical tools:

```text
volatile
synchronized
atomic/other coordination mechanisms
```

---

## Ordering

```text
In what order are writes/operations observed?
```

Typical tools:

```text
volatile
synchronized
locks
other happens-before mechanisms
```

---

# 43. Very Important: `volatile` Is Not a Replacement for Synchronization

Do not conclude:

```text
volatile = synchronization
```

It is not.

`volatile` mainly provides:

```text
visibility
+
ordering around volatile access
+
atomic read/write of the volatile variable itself
```

It does not provide mutual exclusion and does not make compound operations like:

```java
count++;
```

atomic.

---

# 44. Very Important: Atomicity ≠ Visibility

This distinction frequently appears in interviews.

Example:

```java
int x = 10;
```

A simple write may be atomic, but that does not automatically mean another thread is guaranteed to observe that write.

So:

```text
Atomicity
→ operation cannot be observed/interfered with halfway

Visibility
→ latest write is observable by another thread
```

They solve different problems.

---

# 45. Very Important: Thread Safety Requires a Shared Policy

The lecture emphasizes:

> All threads accessing shared state must follow the same synchronization policy.

Example:

```java
Thread-1:
synchronized(lock) {
    // access data
}
```

while:

```java
Thread-2:
// directly access same data
```

does not automatically provide the required protection.

Synchronization works when cooperating threads coordinate through the same mechanism.

---

# 46. What the Different Problems Look Like

```text
RACE CONDITION
→ Result depends on timing

ATOMICITY PROBLEM
→ Operation can be interrupted/interleaved halfway

VISIBILITY PROBLEM
→ One thread does not reliably see another's write

ORDERING PROBLEM
→ Operations are observed in an unexpected order

THREAD INTERFERENCE
→ Threads disturb each other's work

DATA INCONSISTENCY
→ Final program state violates expected rules
```

---

# 47. The Cause-and-Effect Chain

Remember this entire chain:

```text
Shared mutable state
        +
Multiple threads
        +
Missing coordination
        ↓
Unsafe interleaving
        ↓
Race conditions / thread interference
        ↓
Lost updates
Stale reads
Ordering failures
Partial updates
        ↓
Data inconsistency
```

This is the central story of Lecture 4.

---

# 48. Interview Questions

## Q1. What is a race condition?

A race condition occurs when the correctness of a program depends on unpredictable thread timing or execution order.

## Q2. Why is `count++` not atomic?

Because it is conceptually a read-modify-write sequence:

```text
read
→ increment
→ write
```

## Q3. What is a critical section?

The part of code that accesses shared mutable state and must be controlled to prevent unsafe concurrent access.

## Q4. What is atomicity?

The property that a logical operation behaves as one indivisible unit with respect to other threads.

## Q5. What is a visibility problem?

When one thread writes shared data but another thread is not guaranteed to observe the updated value.

## Q6. Does `volatile` make `count++` thread-safe?

**No.**

`volatile` does not make compound operations atomic.

## Q7. What does `volatile` provide?

Primarily:

```text
Visibility
+
Ordering guarantees around volatile access
+
Atomic read/write of the variable itself
```

## Q8. Does `volatile` mean the variable is always read directly from RAM?

**Do not use this as the formal explanation.**

The Java Memory Model defines visibility through happens-before guarantees; "RAM vs cache" is only a simplified mental model.

## Q9. What is thread interference?

When operations from different threads interleave in a way that causes one thread to disturb another's work.

## Q10. Difference between race condition and thread interference?

```text
Race condition
→ correctness depends on timing/order

Thread interference
→ overlapping operations disrupt each other
```

## Q11. What is data inconsistency?

When the program's state no longer satisfies its expected logical or real-world rules.

## Q12. How can race conditions be prevented?

Depending on the problem:

```text
synchronized
Atomic classes
Locks
Thread-safe collections
Immutability
Reduced sharing
Coordination mechanisms
```

## Q13. When should you use `volatile`?

For simple shared state where visibility and ordering are needed, such as:

```java
volatile boolean running;
```

## Q14. When is `AtomicInteger` useful?

When you need atomic operations such as:

```java
count.incrementAndGet();
```

without protecting a larger critical section with a lock.

## Q15. When is `synchronized` useful?

When multiple statements or multiple pieces of state must be protected as one consistent operation and mutual exclusion is required.

---

# 49. Common Beginner Mistakes

### Mistake 1

```text
count++ is one statement
→ therefore atomic
```

❌ Wrong.

One Java statement can represent multiple operations.

### Mistake 2

```text
volatile solves all thread-safety problems
```

❌ Wrong.

`volatile` does not provide mutual exclusion and does not make compound operations atomic.

### Mistake 3

```text
volatile = always directly accesses RAM
```

❌ Too simplistic.

Use the Java Memory Model / happens-before explanation.

### Mistake 4

```text
synchronized = only prevents simultaneous access
```

❌ Incomplete.

It also provides visibility and ordering guarantees through monitor operations.

### Mistake 5

```text
Thread-safe collection
→ every multi-step operation is thread-safe
```

❌ Wrong.

A sequence of separate operations may still need atomic coordination.

### Mistake 6

```text
One thread uses synchronized
→ other threads are automatically protected
```

❌ Wrong.

Threads must coordinate using the same synchronization policy.

---

# 50. Lecture 4 Master Mental Model

Think about concurrency problems in three questions:

```text
QUESTION 1
Can another thread interfere halfway?
        ↓
ATOMICITY

QUESTION 2
Can another thread see my update?
        ↓
VISIBILITY

QUESTION 3
Will another thread observe operations in
 the required order?
        ↓
ORDERING
```

Then:

```text
Atomicity + Visibility + Ordering
          ↓
      Thread Safety
```

Not every concurrency problem requires the same tool.

---

# ⭐ Lecture 4 — Must Remember

```text
1. Shared mutable state is the major source of multithreading problems.

2. A critical section is code that accesses shared mutable data.

3. count++ is NOT atomic.

4. Race condition = result depends on unpredictable thread timing/order.

5. Atomicity = a logical operation cannot be interfered with halfway.

6. Visibility = one thread can reliably observe another thread's write.

7. Ordering = sequence of operations becomes observable in the required order.

8. volatile provides visibility and ordering guarantees around volatile access.

9. volatile DOES NOT make count++ atomic.

10. synchronized provides mutual exclusion, visibility, and ordering.

11. AtomicInteger is useful for atomic counter/state operations.

12. Thread-safe collections protect their own operations, but not
    necessarily multi-operation sequences.

13. Immutable objects reduce synchronization requirements.

14. Reducing shared mutable state often makes concurrency easier.

15. Correct concurrent programs must rely on Java Memory Model guarantees,
    not assumptions about CPU caches or execution timing.
```

---

# 🔥 One-Minute Revision

> **The major source of concurrency problems is shared mutable state. When multiple threads access it without proper coordination, unsafe interleaving can cause race conditions, visibility problems, ordering problems, thread interference, and data inconsistency. `count++` is not atomic because it is a read-modify-write operation. Atomicity asks whether another thread can interfere halfway through a logical operation. Visibility asks whether another thread can see the latest write. Ordering asks how operations become observable between threads. `volatile` provides visibility and ordering guarantees but does not make compound operations atomic. `synchronized` provides mutual exclusion plus visibility and ordering through monitor boundaries. `AtomicInteger`, explicit locks, thread-safe collections, immutability, and reduced sharing are additional tools for building safer concurrent programs.**

---

# 📌 Final Lecture 4 Concept Map

```text
                 MULTITHREADING PROBLEMS
                         │
            ┌────────────┼─────────────┐
            │            │             │
        ATOMICITY    VISIBILITY     ORDERING
            │            │             │
        count++        flag          data/ready
            │            │             │
            └────────────┼─────────────┘
                         ↓
                THREAD INTERFERENCE
                         ↓
                RACE CONDITIONS
                         ↓
                 DATA INCONSISTENCY
                         ↓
        ┌────────────────┼────────────────┐
        │                │                │
   synchronized       volatile      Atomic classes
        │                                 │
      Locks                    Thread-safe collections
        │                                 │
        └──────── Immutability ───────────┘
                         │
                    Reduced Sharing
                         ↓
                  Safer Concurrency
```

## Lecture 4 Core Principle

> **Atomicity protects a logical operation from unsafe interference, visibility ensures threads can observe the required updates, and ordering ensures those updates become observable in the required sequence. Understanding these three guarantees is the foundation of thread-safe Java programming.**
