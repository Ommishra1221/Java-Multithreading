# Java Multithreading — Lecture 5 Notes

## `synchronized` Keyword in Java

This lecture explains how Java synchronization protects shared mutable state, prevents unsafe concurrent access, and provides mutual exclusion, visibility, and ordering guarantees.

---

# 1. Why Do We Need Synchronization?

When multiple threads access the same mutable data, their operations can overlap.

Example:

```java
class Counter {
    int count = 0;

    void increment() {
        count++;
    }
}
```

Because `count++` is a read-modify-write operation, two threads can interfere with each other and cause race conditions or lost updates.

> **Synchronization is used to coordinate concurrent access to shared mutable data.**

---

# 2. Critical Section

A **critical section** is the part of code that accesses shared mutable state and must be protected from unsafe concurrent execution.

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

A critical section can be one statement or several related statements.

For example:

```java
if (balance >= amount) {
    balance = balance - amount;
}
```

The check and update together form one logical critical section.

---

# 3. `synchronized`

Java provides the `synchronized` keyword to protect a critical section.

Example:

```java
class Counter {
    private int count = 0;

    synchronized void increment() {
        count++;
    }
}
```

If one thread is executing `increment()` on a particular `Counter` object, another thread cannot enter another synchronized instance method guarded by the same object's monitor until the first thread leaves the protected region.

---

# 4. Main Benefits of Synchronization

Synchronization mainly provides:

```text
1. Protection of shared data
2. Mutual exclusion / safe atomicity of the protected logical operation
3. Memory visibility
4. Ordering / happens-before guarantees
```

---

# 5. Mutual Exclusion

Mutual exclusion means that competing threads cannot simultaneously enter the same monitor-protected region.

Conceptually:

```text
Thread-1 → acquires monitor → enters critical section
Thread-2 → requests same monitor → BLOCKED
```

After Thread-1 leaves and releases the monitor, another thread can acquire it.

---

# 6. Synchronization and Atomicity

Consider:

```java
synchronized void increment() {
    count++;
}
```

`count++` is still conceptually:

```text
read
→ add 1
→ write
```

But because the complete operation is protected by the same monitor, another cooperating thread cannot enter the protected section at the same time.

So the **logical operation** can behave atomically with respect to threads using the same monitor.

> This does not mean every statement becomes a hardware-level atomic instruction.

---

# 7. Synchronization and Visibility

Synchronization also provides memory visibility.

Conceptually:

```text
Thread-1
   ↓
writes shared data
   ↓
releases monitor
   ↓
same monitor acquired by Thread-2
   ↓
Thread-2 can observe writes made before the release
```

This is expressed through a **happens-before relationship**.

Therefore:

```text
synchronized
    ↓
Mutual exclusion + Memory visibility
```

---

# 8. Synchronization Does Not Stop All Threads

A common misunderstanding is:

> "If one thread is synchronized, all other threads stop."

Incorrect.

Synchronization only controls access to code protected by a particular monitor.

If Thread-1 holds one monitor, Thread-2 can still:

- execute unrelated code
- use a different monitor
- continue other work

> **Synchronization restricts access to a protected region; it does not stop multithreading.**

---

# 9. Monitor / Object Lock

Every Java object can be used as a monitor.

Example:

```java
Object lock = new Object();

synchronized (lock) {
    // protected code
}
```

The monitor is managed by the JVM. It is not a normal field inside the Java class.

---

# 10. How Monitor Acquisition Works

When a thread reaches:

```java
synchronized (lock) {
    // critical section
}
```

conceptually:

```text
1. Try to acquire lock's monitor
2. If available → acquire it
3. Enter synchronized region
4. If another thread owns it → cannot enter
5. Competing thread becomes BLOCKED
6. Owner exits synchronized region
7. Monitor becomes available
8. Another thread can acquire it
```

The monitor acquisition is performed atomically by the JVM.

---

# 11. Synchronized Instance Method

Example:

```java
class Test {
    synchronized void show() {
        System.out.println("entered");
    }
}
```

For an instance method, the monitor is:

```java
this
```

Conceptually equivalent to:

```java
void show() {
    synchronized (this) {
        System.out.println("entered");
    }
}
```

### Key point

> **A synchronized instance method locks the current object (`this`).**

---

# 12. Multiple Synchronized Methods on the Same Object

Example:

```java
class Test {
    synchronized void m1() {
        System.out.println("m1");
    }

    synchronized void m2() {
        System.out.println("m2");
    }
}
```

If:

```text
Thread-1 → test.m1()
Thread-2 → test.m2()
```

and both use the same `test` object:

```text
same object
    ↓
same monitor
    ↓
only one enters at a time
```

The methods are different, but the monitor is the same.

---

# 13. Different Objects Have Different Monitors

Suppose:

```java
Test first = new Test();
Test second = new Test();
```

Then:

```text
Thread-1 → first.m1()
Thread-2 → second.m1()
```

can execute concurrently because:

```text
first  → first object's monitor
second → second object's monitor
```

Therefore:

> **Synchronization depends on the monitor object, not simply on the method name or class name.**

---

# 14. Synchronized Method vs Synchronized Block

## Synchronized Method

```java
synchronized void increment() {
    // whole method protected
}
```

The entire method is protected.

## Synchronized Block

```java
void increment() {
    System.out.println("Before");

    synchronized (this) {
        count++;
    }

    System.out.println("After");
}
```

Only the critical section is protected.

---

# 15. Why Use a Synchronized Block?

If only a small part of a method accesses shared mutable state, protecting the whole method may keep the monitor occupied longer than necessary.

A synchronized block provides **fine-grained control**.

Example:

```java
void increment() {
    System.out.println("Before increment");

    synchronized (this) {
        count++;
    }

    System.out.println("After increment");
}
```

This can reduce unnecessary contention.

### But don't make the block too small blindly

The **complete logical operation that must remain consistent** should be protected.

Example:

```java
synchronized (this) {
    if (balance >= amount) {
        balance = balance - amount;
    }
}
```

The check and update must remain inside the same synchronized region.

---

# 16. Custom Lock Objects

A class does not have to synchronize on `this`.

It can use a dedicated lock:

```java
class Counter {
    private final Object lock = new Object();
    private int count = 0;

    void increment() {
        synchronized (lock) {
            count++;
        }
    }
}
```

Here:

```text
Monitor = lock
```

not `this`.

### Recommended form

```java
private final Object lock = new Object();
```

`private` prevents external code from using the lock, and `final` prevents the lock reference from being replaced.

---

# 17. Multiple Locks Inside One Object

Multiple lock objects can be useful when they protect **independent state**.

Example:

```java
class AccountManager {
    private final Object accountLock = new Object();
    private final Object auditLock = new Object();

    private int balance;
    private int auditEntries;

    void updateBalance(int amount) {
        synchronized (accountLock) {
            balance += amount;
        }
    }

    void recordAuditEntry() {
        synchronized (auditLock) {
            auditEntries++;
        }
    }
}
```

These operations can run concurrently because they protect independent state with different monitors.

---

# 18. VERY IMPORTANT — Different Locks for the Same Data

Using different locks is dangerous when they protect the same shared state.

Bad design:

```java
class Bank {
    private final Object depositLock = new Object();
    private final Object withdrawLock = new Object();

    private int balance;

    void deposit(int amount) {
        synchronized (depositLock) {
            balance += amount;
        }
    }

    void withdraw(int amount) {
        synchronized (withdrawLock) {
            balance -= amount;
        }
    }
}
```

Both modify:

```text
balance
```

but use different monitors.

Therefore they may execute concurrently and still corrupt the shared state.

### Correct

```java
class Bank {
    private final Object balanceLock = new Object();
    private int balance;

    void deposit(int amount) {
        synchronized (balanceLock) {
            balance += amount;
        }
    }

    void withdraw(int amount) {
        synchronized (balanceLock) {
            balance -= amount;
        }
    }
}
```

### Golden Rule

> **Data that participates in the same consistency rule must be protected by the same lock.**

---

# 19. Common Mistake — `synchronized(new Object())`

Incorrect:

```java
synchronized (new Object()) {
    count++;
}
```

Every execution creates a new object:

```text
Call 1 → Object A
Call 2 → Object B
Call 3 → Object C
```

Different objects mean different monitors, so threads do not block one another.

Correct:

```java
private final Object lock = new Object();

synchronized (lock) {
    count++;
}
```

Now all competing threads use the same monitor.

---

# 20. Static Synchronization

Instance state belongs to individual objects:

```java
class Counter {
    int count;
}
```

Static state belongs to the class:

```java
class Counter {
    static int count;
}
```

For a static synchronized method:

```java
static synchronized void increment() {
    count++;
}
```

the monitor is:

```java
Counter.class
```

not `this`.

---

# 21. Static Synchronized Method — Conceptual Equivalent

```java
class Counter {
    private static int count = 0;

    static synchronized void increment() {
        count++;
    }
}
```

is conceptually equivalent to:

```java
class Counter {
    private static int count = 0;

    static void increment() {
        synchronized (Counter.class) {
            count++;
        }
    }
}
```

A static method has no `this` reference.

---

# 22. Multiple Objects Still Share the Class Monitor

Even if:

```java
Counter first = new Counter();
Counter second = new Counter();
```

static synchronized methods still coordinate through:

```text
Counter.class
```

So two calls to the same static synchronized method cannot execute simultaneously just because they came through different instances.

---

# 23. Instance Lock vs Class Lock

```java
class Test {

    synchronized void instanceMethod() {
        // monitor = this
    }

    static synchronized void staticMethod() {
        // monitor = Test.class
    }
}
```

Therefore:

```text
instanceMethod()
→ object monitor

staticMethod()
→ class monitor
```

These methods do **not automatically block one another** because they use different monitors.

---

# 24. Conceptual Monitor Internals

For learning, imagine a monitor as:

```text
Monitor {
    owner
    holdCount
    entrySet
    waitSet
}
```

Conceptually:

```text
owner
→ current owning thread

holdCount
→ number of times the owner has acquired it

entrySet
→ threads waiting to acquire it

waitSet
→ threads waiting after wait()
```

This is a **teaching model**, not the exact JVM memory layout. JVM implementations can use optimized locking mechanisms internally.

---

# 25. Entering a Synchronized Region

For:

```java
synchronized (lock) {
    // critical section
}
```

conceptually:

```text
Thread reaches synchronized
        ↓
Try to acquire monitor
        ↓
Monitor available?
   ┌────┴────┐
  YES       NO
   ↓         ↓
 acquire    BLOCKED
   ↓         ↓
 enter      wait
 critical   for monitor
 section      ↓
   ↓       monitor released
 exit          ↓
   ↓        another thread
 release      can acquire
```

---

# 26. Exiting a Synchronized Region

The monitor is automatically released when execution leaves the synchronized method or block.

This also happens when an exception causes the thread to leave the synchronized region.

Example:

```java
synchronized (lock) {
    throw new RuntimeException();
}
```

The JVM releases the monitor during unwinding.

### Advantage

`synchronized` automatically handles monitor release when the protected region ends.

---

# 27. Reentrant Nature of `synchronized`

Java monitors are **reentrant**.

A thread that already owns a monitor can acquire the same monitor again.

Example:

```java
class Service {

    synchronized void methodA() {
        methodB();
    }

    synchronized void methodB() {
        System.out.println("Inside methodB");
    }
}
```

If a thread owns the monitor while executing `methodA()`, it can enter `methodB()` on the same object without blocking itself.

Conceptually:

```text
Thread owns monitor
      ↓
acquires same monitor again
      ↓
hold count increases
      ↓
allowed to continue
```

---

# 28. Common Mistake — Replacing the Lock Reference

Avoid:

```java
class Counter {
    private Object lock = new Object();

    void changeLock() {
        lock = new Object();
    }
}
```

Different threads may end up using different monitor objects.

Prefer:

```java
private final Object lock = new Object();
```

---

# 29. Common Mistake — Exposing the Lock Publicly

Avoid:

```java
public final Object lock = new Object();
```

External code can synchronize on it and unexpectedly interfere with the class.

Prefer:

```java
private final Object lock = new Object();
```

---

# 30. Common Mistake — Protecting Only Part of a Compound Operation

Incorrect:

```java
if (balance >= amount) {
    synchronized (lock) {
        balance -= amount;
    }
}
```

The condition is outside the monitor. Another thread may change the balance after the check but before the update.

Correct:

```java
synchronized (lock) {
    if (balance >= amount) {
        balance -= amount;
    }
}
```

The check + update are now one protected logical operation.

---

# 31. Important `synchronized` Rules

```text
synchronized instance method
→ monitor = this

synchronized static method
→ monitor = ClassName.class

synchronized block
→ monitor = object inside parentheses
```

And:

```text
same monitor
→ threads coordinate / mutually exclude

different monitors
→ threads may execute concurrently
```

---

# 32. `synchronized` vs `volatile`

From the previous lecture:

### `volatile`

```text
Visibility
Ordering around volatile access
Atomic read/write of the variable itself
No mutual exclusion
Does NOT make count++ atomic
```

### `synchronized`

```text
Mutual exclusion
Visibility
Ordering
Can protect a larger logical critical section
```

Therefore:

> **`volatile` and `synchronized` are not interchangeable.**

---

# 33. `synchronized` and Performance

Synchronization has overhead.

If a lock is held unnecessarily long:

```text
Thread-1
   ↓
holds lock
   ↓
Thread-2 waits
Thread-3 waits
Thread-4 waits
```

Too much contention can reduce concurrency and performance.

### Good rule

Protect the complete logical critical section, but avoid putting unrelated work inside the lock.

---

# 34. Interview Questions

## Q1. What is synchronization in Java?

Synchronization is a mechanism for coordinating concurrent access to shared mutable state.

## Q2. What is a monitor?

A monitor is the synchronization mechanism associated with an object that controls access to code synchronized on that object.

## Q3. What lock does a synchronized instance method use?

The monitor of:

```java
this
```

## Q4. What lock does a synchronized static method use?

The monitor of:

```java
ClassName.class
```

## Q5. What lock does a synchronized block use?

The monitor of the object supplied in:

```java
synchronized (lock)
```

## Q6. Why is `synchronized(new Object())` wrong for shared-state protection?

Because each execution creates a different monitor object.

## Q7. What happens if another thread tries to acquire an occupied monitor?

It cannot enter the synchronized region and may become `BLOCKED`.

## Q8. Does synchronization stop the whole application?

No. It only restricts access to code protected by a particular monitor.

## Q9. Why use synchronized blocks?

For finer-grained control so only the necessary critical section is protected.

## Q10. Can two synchronized methods execute at the same time?

It depends on their monitor. If they use the same monitor, they cannot both enter simultaneously. If they use different monitors, they may run concurrently.

## Q11. Can a synchronized instance method and synchronized static method execute concurrently?

Yes, normally, because they use different monitors: `this` and `ClassName.class`.

## Q12. What is reentrant synchronization?

A thread that already owns a monitor can acquire the same monitor again without blocking itself.

## Q13. Does synchronized provide visibility?

Yes. Monitor release followed by a later acquisition of the same monitor creates the required happens-before relationship.

## Q14. What is the most important rule when using multiple lock objects?

The same consistency rule for shared state must use the same lock.

## Q15. Why should a custom lock normally be private final?

`private` prevents external code from using it accidentally, and `final` prevents replacing the lock reference.

---

# 35. Common Beginner Mistakes

### Mistake 1

> "The lock belongs to the synchronized method."

❌ Wrong.

For an instance synchronized method, the monitor is the object (`this`).

### Mistake 2

> "Any two synchronized blocks protect each other."

❌ Wrong.

They must synchronize on the same monitor.

### Mistake 3

> "synchronized stops all threads."

❌ Wrong.

Only threads competing for the same monitor-protected region are blocked.

### Mistake 4

> "Different locks are always better."

❌ Wrong.

Different locks are useful for independent state, but unsafe when different locks protect the same consistency rule.

### Mistake 5

```java
synchronized (new Object()) {
    count++;
}
```

❌ Wrong for shared-state protection because every execution gets a new monitor.

### Mistake 6

> "Only the update needs to be synchronized."

Not necessarily.

For:

```java
if (balance >= amount) {
    balance -= amount;
}
```

both the check and update need to be protected together.

### Mistake 7

> "synchronized only solves race conditions."

Incomplete.

It also provides visibility and ordering guarantees.

---

# 36. Lecture 5 Master Mental Model

Think:

```text
SHARED DATA
     ↓
CRITICAL SECTION
     ↓
MONITOR / LOCK
     ↓
synchronized
     ↓
ONE OWNER AT A TIME
     ↓
OTHER COMPETING THREADS BLOCK
     ↓
OWNER EXITS
     ↓
MONITOR RELEASED
     ↓
ANOTHER THREAD CAN ENTER
```

And remember:

```text
synchronized
    │
    ├── Mutual Exclusion
    ├── Visibility
    └── Ordering
```

When the complete logical operation is protected by the same monitor, its operations can be made atomic with respect to cooperating threads.

---

# ⭐ Lecture 5 — Must Remember

```text
1. synchronized protects shared mutable state.

2. A critical section is the code that must be protected.

3. Every Java object can serve as a monitor.

4. synchronized instance method → locks this.

5. synchronized static method → locks ClassName.class.

6. synchronized block → locks the object inside parentheses.

7. Different objects have different monitors.

8. Same monitor → competing threads cannot enter simultaneously.

9. Different monitors → threads may execute concurrently.

10. synchronized provides mutual exclusion + visibility + ordering.

11. synchronized can protect a complete logical operation from unsafe interleaving.

12. synchronized(new Object()) is usually wrong for shared-state protection.

13. Custom lock objects should normally be private final.

14. Multiple locks are useful when protecting independent state.

15. The same consistency rule must use the same lock.

16. synchronized is reentrant.

17. The monitor is automatically released when synchronized execution ends,
    including when an exception exits the region.

18. Synchronized blocks provide finer-grained control than whole-method locking.
```

---

# 🔥 One-Minute Revision

> **Synchronization in Java is used to control concurrent access to shared mutable state. A critical section is the code that needs protection. `synchronized` works through object monitors: a synchronized instance method uses `this`, a static synchronized method uses `ClassName.class`, and a synchronized block uses the specified lock object. When one thread owns a monitor, another thread competing for the same monitor cannot enter and may become `BLOCKED`. Synchronization provides mutual exclusion, visibility, and ordering guarantees, and can make a complete logical operation atomic with respect to threads using the same monitor. Different objects have different monitors, so synchronization only works between threads that use the same monitor. Dedicated locks should normally be `private final`, and different locks should only be used when they protect independent state. Java monitors are reentrant and are automatically released when the synchronized region ends.**

---

# 📌 Final Concept Map

```text
                  SYNCHRONIZATION
                         │
                         ↓
                Shared Mutable Data
                         │
                         ↓
                   Critical Section
                         │
                         ↓
                    synchronized
                         │
                         ↓
                      Monitor
                  /      │       \\
                 /       │        \\
          Instance     Static      Block
             │           │           │
           this     ClassName.class  lock
             \\           │           /
              \\          │          /
                       LOCK
                         │
             ┌───────────┼───────────┐
             ↓           ↓           ↓
        Mutual        Visibility   Ordering
        Exclusion
             │
             ↓
       Atomic logical
        operations
             │
             ↓
       Thread-safe access
```

## Lecture 5 Core Principle

> **Synchronization is not simply "making a method single-threaded." It is coordinating threads through a shared monitor so that the complete critical section is protected while also providing the required visibility and ordering guarantees.**

# Next Topic

The next topic is **Inter-Thread Communication**:

```java
wait();
notify();
notifyAll();
```

Synchronization prevents competing threads from entering the same protected region at the same time, but it does not by itself tell a thread what to do when a required condition is not yet satisfied. That leads naturally to inter-thread communication.
