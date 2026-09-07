# Java Multithreading — Lecture 7 Notes

## Advanced Locking Mechanisms in Java

This lecture moves beyond the `synchronized` keyword and introduces the `Lock` family and related concurrency utilities.

The main motivation is that `synchronized` is simple and safe, but advanced code sometimes needs more control over lock acquisition, timeout-based attempts, fairness, read-heavy workloads, multiple permits, and condition-based communication.

---

# 1. Why Do We Need Locks Beyond `synchronized`?

With `synchronized`, Java handles monitor acquisition and release automatically:

```java
synchronized (lock) {
    // critical section
}
```

The lecture highlights limitations for advanced use cases:

```text
1. Less explicit control over lock acquisition/release
2. No direct tryLock()-style acquisition
3. Limited fairness control
4. Locking can create contention
5. Some workloads need specialized strategies
```

Important idea:

> `synchronized` is not bad. It is the simple monitor-based mechanism; explicit locks provide additional control when needed.

---

# 2. The `Lock` Interface

Java provides:

```java
java.util.concurrent.locks.Lock
```

Basic pattern:

```java
lock.lock();

try {
    // critical section
}
finally {
    lock.unlock();
}
```

## Golden rule

> **Whenever you manually acquire a lock, release it in `finally`.**

Otherwise an exception can leave the lock held and other threads may remain blocked.

---

# 3. `lock()` and `unlock()`

Think:

```text
lock()
→ acquire the lock before the protected work

unlock()
→ release the lock after the protected work
```

Conceptually:

```text
Thread
  ↓
lock()
  ↓
lock available?
 ┌───────┴───────┐
 YES             NO
  ↓               ↓
critical       wait/block
section
  ↓
unlock()
```

---

# 4. `ReentrantLock`

One of the most common `Lock` implementations is:

```java
ReentrantLock
```

Example:

```java
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

class Resource {

    Lock lock = new ReentrantLock();

    void f1() {
        lock.lock();

        try {
            System.out.println(
                Thread.currentThread().getName() + " entered"
            );
        }
        finally {
            lock.unlock();
        }
    }
}
```

The uploaded demo uses the same `lock()` / `try` / `finally` / `unlock()` pattern with multiple threads sharing one `ReentrantLock`.

---

# 5. What Does Reentrant Mean?

A lock is **reentrant** when the same thread can acquire the same lock multiple times without blocking itself.

Conceptually:

```text
T1 lock()
→ hold count = 1

T1 lock() again
→ hold count = 2

T1 unlock()
→ hold count = 1

T1 unlock()
→ hold count = 0
→ lock becomes available
```

This is similar to Java's reentrant intrinsic monitors.

---

# 6. `tryLock()`

A major advantage of `ReentrantLock` is:

```java
tryLock()
```

Instead of potentially waiting indefinitely:

```java
lock.lock();
```

a thread can attempt acquisition and handle failure.

Example:

```java
if (lock.tryLock()) {

    try {
        // lock acquired
    }
    finally {
        lock.unlock();
    }

} else {
    // could not acquire lock
}
```

Mental model:

```text
lock()
→ "Wait until I get it."

tryLock()
→ "Can I get it?"
```

---

# 7. `tryLock()` With Timeout

You can limit how long the current thread waits:

```java
if (lock.tryLock(2, TimeUnit.SECONDS)) {

    try {
        // lock acquired
    }
    finally {
        lock.unlock();
    }

} else {
    // timed attempt failed
}
```

Conceptually:

```text
Try to acquire
      ↓
wait up to timeout
      ↓
 ┌────┴─────┐
lock       timeout
acquired
```

---

# 8. Useful `ReentrantLock` Features

Depending on configuration and use, useful APIs include:

```java
lock()
unlock()
tryLock()
tryLock(timeout, unit)
isLocked()
isHeldByCurrentThread()
getHoldCount()
isFair()
```

These can be useful for advanced control, diagnostics, and fairness decisions.

---

# 9. Fairness

A lock can be configured as fair:

```java
ReentrantLock lock = new ReentrantLock(true);
```

Conceptually:

```text
true  → fair
false → non-fair
```

Fairness aims to give waiting threads a more predictable opportunity based on acquisition order.

### Trade-off

```text
Fairness
  ↓
more predictable access
  ↓
may reduce throughput
```

So fairness is a design trade-off rather than a free performance improvement.

---

# 10. Starvation

**Starvation** occurs when a thread repeatedly fails to get enough access to a resource to make progress while other threads continue to acquire it.

Example:

```text
Thread A → repeatedly gets lock
Thread B → repeatedly waits
Thread C → repeatedly gets lock
Thread B → still waiting
```

Unfair scheduling/locking can increase starvation risk. Fairness can help, but does not solve every concurrency problem.

---

# 11. `ReadWriteLock`

Some resources are read much more often than they are modified:

```text
File
Database
Cache
Configuration
Shared data
```

Using one exclusive lock for every access can unnecessarily make readers wait for other readers.

`ReadWriteLock` separates access into:

```text
Read Lock  → shared
Write Lock → exclusive
```

---

# 12. Read Lock vs Write Lock

A read lock is a **shared lock**.

Multiple readers can hold it together.

A write lock is **exclusive**.

Only one writer can hold it, and writing excludes readers.

Think:

```text
R1 ─┐
R2 ─┼──> READ LOCK → allowed together
R3 ─┘

W1 ───> WRITE LOCK → exclusive
```

Rules:

```text
Reader + Reader → ✅
Reader + Writer → ❌
Writer + Writer → ❌
```

---

# 13. `ReentrantReadWriteLock`

A common implementation is:

```java
ReadWriteLock rwLock = new ReentrantReadWriteLock();

Lock readLock = rwLock.readLock();
Lock writeLock = rwLock.writeLock();
```

Reader example:

```java
public int read() {
    readLock.lock();

    try {
        return value;
    }
    finally {
        readLock.unlock();
    }
}
```

Writer example:

```java
public void write(int newValue) {
    writeLock.lock();

    try {
        value = newValue;
    }
    finally {
        writeLock.unlock();
    }
}
```

The uploaded reader/writer demo creates multiple reader and writer threads using this model.

---

# 14. Reader-Writer Problem

The Reader-Writer Problem asks:

> How can many threads safely read shared data while writes remain exclusive?

An ordinary exclusive lock gives:

```text
R1 → R2 → R3 → W1 → ...
```

A read-write lock can allow:

```text
R1 ─┐
R2 ─┼──> concurrent reads
R3 ─┘

W1 ───> exclusive write
```

This can improve throughput when reads greatly outnumber writes.

---

# 15. When Should You Use `ReadWriteLock`?

Good candidates:

```text
Read-heavy workload
+
Infrequent writes
+
Useful read concurrency
```

Examples:

- configuration caches
- metadata
- shared lookup structures
- read-heavy in-memory data

If writes are frequent, the extra read/write complexity may not provide a benefit.

---

# 16. Lock Downgrading

A thread can safely move from a write lock to a read lock by acquiring the read lock **before** releasing the write lock.

Conceptually:

```text
WRITE LOCK
    ↓
update state
    ↓
acquire READ LOCK
    ↓
release WRITE LOCK
    ↓
continue under READ LOCK
```

Typical pattern:

```java
writeLock.lock();

try {
    // update state

    readLock.lock();

    try {
        // inspect/read consistent state
    }
    finally {
        readLock.unlock();
    }

}
finally {
    writeLock.unlock();
}
```

The key idea is to avoid a gap between exclusive protection and shared read protection.

---

# 17. `StampedLock`

`StampedLock` is an advanced locking mechanism that supports:

```text
1. Write lock
2. Read lock
3. Optimistic read
```

Example:

```java
StampedLock lock = new StampedLock();
```

Unlike `ReentrantReadWriteLock`, it introduces optimistic reading.

---

# 18. StampedLock and Stamps

The operations return a `long` stamp:

```java
long stamp = lock.writeLock();

long stamp = lock.readLock();

long stamp = lock.tryOptimisticRead();
```

For read/write locks, the stamp is used when unlocking:

```java
lock.unlockRead(stamp);
lock.unlockWrite(stamp);
```

---

# 19. Optimistic Reading

Instead of immediately acquiring a full read lock:

```text
Reader
 ↓
read lock
 ↓
read
 ↓
unlock
```

optimistic reading does:

```text
Get stamp
   ↓
Read without full read lock
   ↓
Validate stamp
   ↓
Was a conflicting write observed?
 ┌──────┴──────┐
 NO            YES
 ↓              ↓
use result    fallback
               ↓
          acquire read lock
```

---

# 20. `tryOptimisticRead()`

Basic pattern:

```java
long stamp = lock.tryOptimisticRead();

int currentValue = value;

if (!lock.validate(stamp)) {

    stamp = lock.readLock();

    try {
        currentValue = value;
    }
    finally {
        lock.unlockRead(stamp);
    }
}
```

The uploaded `StampedLock` demo follows the same pattern: get an optimistic stamp, read the value, validate it, and fall back to a pessimistic read lock when validation fails.

---

# 21. Why Is It Called Optimistic?

The reader assumes:

> "Probably nobody changed the data while I was reading."

Therefore:

```text
Pessimistic read
→ lock first, then read

Optimistic read
→ read first, then validate
```

If validation succeeds, the read can be used. If it fails, fall back to a properly locked read.

---

# 22. Pessimistic vs Optimistic Reading

| Pessimistic Read | Optimistic Read |
|---|---|
| Acquires read lock first | Starts without full read lock |
| More direct protection | Requires validation |
| Can create more contention | Can reduce contention |
| Simpler | More complex |
| Useful when conflicts are likely | Useful when writes are rare |

---

# 23. StampedLock Write Lock

Example:

```java
long stamp = lock.writeLock();

try {
    value = newValue;
}
finally {
    lock.unlockWrite(stamp);
}
```

The uploaded demo uses this exact stamp-based write-lock/unlock pattern.

---

# 24. `ReentrantReadWriteLock` vs `StampedLock`

| `ReentrantReadWriteLock` | `StampedLock` |
|---|---|
| Read lock + write lock | Read + write + optimistic read |
| Reentrant | Not generally reentrant |
| Familiar lock-based API | Stamp-based API |
| Easier to reason about | More advanced |
| Good for classic reader-writer use cases | Useful when optimistic reads can succeed often |

### Important

Do not assume `StampedLock` behaves like `ReentrantLock` when the same thread tries to reacquire the same lock.

---

# 25. Semaphore

A `Semaphore` is different from a normal lock.

A lock usually represents:

```text
ONE ownership slot
```

A semaphore manages:

```text
MULTIPLE permits
```

Example:

```java
Semaphore semaphore = new Semaphore(3);
```

This starts with three permits.

---

# 26. Semaphore — Basic Usage

Acquire:

```java
semaphore.acquire();
```

Release:

```java
semaphore.release();
```

Conceptually:

```text
permits = 3

T1 acquire → 2
T2 acquire → 1
T3 acquire → 0
T4 acquire → waits
```

When one thread releases:

```text
release()
→ permit becomes available
→ another waiting thread can acquire
```

---

# 27. Binary vs Counting Semaphore

## Binary semaphore

```text
permits = 1
```

Only one permit holder at a time.

## Counting semaphore

```text
permits > 1
```

Example:

```java
new Semaphore(3);
```

allows up to three permit holders concurrently.

---

# 28. Lock vs Semaphore

| Lock | Semaphore |
|---|---|
| Usually one owner | Set of permits |
| Ownership-based | Permit-based |
| `lock()` / `unlock()` | `acquire()` / `release()` |
| Mutual exclusion | Limits concurrent access |

### Easy memory rule

> **Lock answers "Who owns the critical section?"**

> **Semaphore answers "How many may enter?"**

---

# 29. Real-World Semaphore Example — API Concurrency Limit

Suppose at most three API calls should run concurrently:

```java
Semaphore semaphore = new Semaphore(3);

semaphore.acquire();

try {
    callApi();
}
finally {
    semaphore.release();
}
```

Other applications include:

- connection pools
- limited hardware resources
- worker capacity
- bounded concurrent requests

---

# 30. `Condition`

`Condition` provides more flexible waiting/signaling with explicit locks.

It is conceptually related to:

```text
wait()
notify()
notifyAll()
```

but works with a `Lock` such as `ReentrantLock`.

Example:

```java
ReentrantLock lock = new ReentrantLock();
Condition condition = lock.newCondition();
```

Then:

```java
condition.await();
condition.signal();
condition.signalAll();
```

These operations are used while holding the associated lock.

---

# 31. `Condition` vs `wait/notify`

Intrinsic monitor style:

```java
synchronized (lock) {
    lock.wait();
    lock.notify();
    lock.notifyAll();
}
```

Explicit-lock style:

```java
lock.lock();

try {
    condition.await();
    condition.signal();
    condition.signalAll();
}
finally {
    lock.unlock();
}
```

A major advantage is that a single lock can have **multiple condition queues**.

---

# 32. Why Multiple Conditions Matter

For a bounded buffer, two logical conditions might be:

```text
notFull
notEmpty
```

Conceptually:

```text
Producer
   ↓
notFull condition

Consumer
   ↓
notEmpty condition
```

This lets code signal the relevant waiting group instead of unnecessarily waking unrelated waiters.

---

# 33. Guarded Blocks and Spurious Wakeups

A waiting thread should re-check its condition after waking.

Prefer:

```java
while (!conditionIsSatisfied) {
    condition.await();
}
```

rather than:

```java
if (!conditionIsSatisfied) {
    condition.await();
}
```

Mental model:

```text
WAIT
 ↓
WAKE UP
 ↓
CHECK CONDITION AGAIN
 ↓
 ┌───────┴────────┐
TRUE             FALSE
 ↓                 ↓
continue          wait again
```

This guarded-block pattern protects against spurious wakeups and also handles the case where another thread changes the shared state before the awakened thread gets the lock again.

---

# 34. Deadlock

A **deadlock** occurs when threads are permanently blocked waiting for resources held by each other.

Classic example:

```text
Thread-1
holds Lock-A
    ↓
waits for Lock-B

Thread-2
holds Lock-B
    ↓
waits for Lock-A
```

Result:

```text
T1 waits for T2
T2 waits for T1
→ nobody progresses
```

Advanced locking APIs do not automatically prevent deadlocks.

---

# 35. Starvation vs Deadlock

### Starvation

A thread keeps being denied access and cannot make enough progress.

### Deadlock

Threads are stuck in a circular dependency and cannot proceed.

```text
Starvation → one thread may keep losing
Deadlock   → threads wait on each other permanently
```

---

# 36. Fairness vs Performance

```text
Fair lock
→ more predictable access
→ can reduce throughput

Unfair lock
→ often higher throughput
→ can increase starvation risk
```

Fairness is therefore a trade-off.

---

# 37. `Lock` vs `synchronized`

| `synchronized` | `Lock` |
|---|---|
| Simpler | More explicit control |
| Automatic monitor release | Manual `unlock()` |
| No direct `tryLock()` | `tryLock()` available |
| Less explicit fairness control | Fairness configurable in implementations such as `ReentrantLock` |
| Excellent for straightforward critical sections | Useful for advanced locking requirements |

---

# 38. Which Mechanism Should You Choose?

```text
Simple mutual exclusion
→ synchronized

Need explicit control / tryLock / fairness
→ ReentrantLock

Many readers + fewer writers
→ ReentrantReadWriteLock

Read-heavy workload + optimistic reads
→ StampedLock

Need N concurrent permits
→ Semaphore

Need multiple condition queues
→ Condition
```

This is a design guideline, not an absolute rule.

---

# 39. Connection With Previous Lectures

### Lecture 4

```text
Race condition
Atomicity
Visibility
Ordering
```

### Lecture 5

```text
synchronized
Monitor
Critical section
Mutual exclusion
```

### Lecture 6

```text
wait()
notify()
notifyAll()
Producer-Consumer
```

### Lecture 7

```text
Lock
 ↓
ReentrantLock
 ↓
tryLock / fairness
 ↓
ReadWriteLock
 ↓
StampedLock
 ↓
Semaphore
 ↓
Condition
```

These are different tools for different coordination requirements.

---

# 40. Interview Questions

## Q1. Why use `ReentrantLock` instead of `synchronized`?

When explicit features such as `tryLock()`, timed acquisition, fairness, and manual lock control are useful.

## Q2. Why should `unlock()` be in `finally`?

To make sure the lock is released even when an exception occurs.

## Q3. What does reentrant mean?

The same thread can acquire the same lock multiple times without blocking itself.

## Q4. What is `tryLock()`?

It attempts to acquire a lock without forcing an indefinite wait.

## Q5. What is `ReadWriteLock`?

A mechanism that separates shared read access from exclusive write access.

## Q6. Can multiple readers hold a read lock together?

Yes, provided no writer owns the write lock.

## Q7. What is optimistic reading?

Reading without first acquiring a full read lock and later validating that the read remained valid.

## Q8. What happens when `StampedLock.validate(stamp)` fails?

The optimistic read is not considered valid; the code should use an appropriate fallback such as a proper read lock and re-read the data.

## Q9. What is a semaphore?

A concurrency utility that manages a number of permits rather than a single ownership lock.

## Q10. What is `Condition`?

A coordination mechanism associated with an explicit lock that supports waiting/signaling and can provide multiple condition queues per lock.

## Q11. Why use `while` instead of `if` around `await()`/`wait()`?

Because a wake-up does not guarantee the condition is true. The condition must be checked again before continuing.

## Q12. What is lock downgrading?

Acquiring a read lock while still holding a write lock, then releasing the write lock and continuing under the read lock.

## Q13. Is `StampedLock` reentrant?

No, not generally. Do not assume it behaves like `ReentrantLock`.

## Q14. What is starvation?

A thread repeatedly fails to get enough access to a resource to make progress.

## Q15. What is deadlock?

Threads wait indefinitely in a circular dependency on locks/resources.

---

# 41. Common Beginner Mistakes

### Mistake 1

```java
lock.lock();
// work
lock.unlock();
```

with no `finally`.

❌ Dangerous.

Use:

```java
lock.lock();
try {
    // work
}
finally {
    lock.unlock();
}
```

### Mistake 2

Thinking `tryLock()` is exactly the same as `lock()`.

❌ No. `lock()` may wait indefinitely; `tryLock()` lets you handle failure or timeout.

### Mistake 3

Thinking read locks allow writers at the same time.

❌ No. A writer is exclusive.

### Mistake 4

Thinking optimistic read means no validation is required.

❌ The pattern is:

```text
read → validate → fallback if invalid
```

### Mistake 5

Using `if` instead of `while` around condition waiting.

❌ Re-check the condition after waking.

### Mistake 6

Thinking fairness automatically improves performance.

❌ Fairness can reduce throughput.

### Mistake 7

Thinking a semaphore is just another lock.

❌ A semaphore controls a set of permits and is useful for limiting concurrency.

---

# 42. Master Comparison Table

| Mechanism | Main Idea | Best Fit |
|---|---|---|
| `synchronized` | Simple monitor-based mutual exclusion | Straightforward critical sections |
| `ReentrantLock` | Explicit/reentrant lock control | `tryLock`, timeout, fairness |
| `ReentrantReadWriteLock` | Shared reads + exclusive writes | Read-heavy workloads |
| `StampedLock` | Read/write + optimistic reading | Advanced read-heavy workloads |
| `Semaphore` | Multiple permits | Limit concurrent access |
| `Condition` | Flexible waiting/signaling | Multiple condition queues |

---

# 43. Master Mental Model

Think of these as answers to different questions:

```text
"Only one thread at a time?"
        ↓
synchronized / Lock

"Need manual control or tryLock?"
        ↓
ReentrantLock

"Many readers, few writers?"
        ↓
ReentrantReadWriteLock

"Can likely-read-only work avoid a full read lock?"
        ↓
StampedLock optimistic read

"Allow exactly N threads concurrently?"
        ↓
Semaphore

"Need multiple waiting conditions?"
        ↓
Condition
```

---

# 44. ⭐ Lecture 7 — Must Remember

```text
1. Lock gives explicit control over acquisition/release.

2. Explicit unlock() should normally be inside finally.

3. ReentrantLock allows the same thread to acquire the same lock repeatedly.

4. tryLock() can avoid indefinite blocking and supports timed attempts.

5. Fairness can reduce starvation but may reduce throughput.

6. ReadWriteLock provides shared reads + exclusive writes.

7. Multiple readers can read together when no writer owns the write lock.

8. StampedLock adds optimistic reading.

9. Optimistic read:
   get stamp → read → validate → fallback if invalid.

10. StampedLock is not generally reentrant.

11. Semaphore manages permits rather than one lock owner.

12. Semaphore(3) can allow up to three permit holders at once.

13. Condition provides flexible waiting/signaling with explicit locks.

14. Waiting conditions should generally be checked in a while loop.

15. More powerful locking APIs provide more control but require more careful
    design to avoid deadlocks, starvation, contention, and incorrect unlocks.
```

---

# 🔥 One-Minute Revision

> **`synchronized` is simple and safe for ordinary mutual exclusion, but advanced applications sometimes need more control. The `Lock` interface provides explicit lock acquisition/release, and `ReentrantLock` adds reentrancy, `tryLock()`, timeout-based acquisition, and optional fairness. For read-heavy workloads, `ReentrantReadWriteLock` allows multiple readers while keeping writes exclusive. `StampedLock` adds optimistic reading: obtain a stamp, read without a full lock, validate the stamp, and fall back to a proper read lock if validation fails. A `Semaphore` manages multiple permits rather than one ownership lock and is useful when only a fixed number of threads should access a resource concurrently. `Condition` provides more flexible wait/signal coordination and can maintain multiple waiting queues for one lock. With this extra power comes extra responsibility: release locks correctly and design carefully to avoid starvation and deadlocks.**

---

# 📌 Final Concept Map

```text
                       JAVA LOCKING
                            │
          ┌─────────────────┼──────────────────┐
          │                 │                  │
      synchronized         Lock            Semaphore
          │                 │                  │
       monitor       ┌──────┼──────┐       permits
                     │      │      │
              ReentrantLock │  StampedLock
                     │      │      │
                  tryLock   │  optimistic read
                  fairness  │
                            │
                    ReadWriteLock
                       │
                ┌──────┴──────┐
                │             │
             readLock      writeLock
             shared        exclusive

Lock
  │
  └── Condition
        │
   await / signal / signalAll
```

## Lecture 7 Core Principle

> **Choose the concurrency mechanism based on the actual coordination problem: mutual exclusion, explicit lock control, read/write concurrency, optimistic reads, bounded concurrency, or condition-based communication. More flexibility can improve control and performance, but it also increases the responsibility on the developer to use the mechanism correctly.**

## Next Topic

The next natural step is to study how these locking mechanisms interact with **concurrent collections, atomic classes, executors, thread pools, and higher-level concurrency APIs**.
