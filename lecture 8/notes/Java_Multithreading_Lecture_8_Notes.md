# Java Multithreading — Lecture 8 Notes

## Lock-Free Concurrency, Atomic Variables & CAS

This lecture introduces **lock-free concurrency** through Java's atomic classes and the **Compare-And-Set (CAS)** operation.

The main idea is:

```text
Traditional approach
→ synchronized / locks
→ protects critical section
→ thread may block

Lock-free approach
→ atomic variables + CAS
→ no explicit mutual-exclusion lock for the operation
→ thread can retry when contention occurs
```

The goal is not to say that lock-free code is always faster. It is a useful alternative for **specific small state transitions and high-contention use cases**.

---

# 1. The Problem: Race Conditions

From Lecture 4, we know:

```java
count++;
```

looks like one statement, but conceptually contains:

```text
1. Read count
2. Increment
3. Write count
```

So it is a **read-modify-write** operation.

Two threads can interleave:

```text
Initial count = 0

Thread-1 → reads 0
Thread-2 → reads 0

Thread-1 → calculates 1
Thread-2 → calculates 1

Thread-1 → writes 1
Thread-2 → writes 1
```

Final result:

```text
1
```

instead of:

```text
2
```

This is a race condition / lost update.

---

# 2. Traditional Solution: `synchronized`

A common solution is:

```java
synchronized void increment() {
    count++;
}
```

This protects the critical section so only one cooperating thread can execute it at a time.

The downside is that locking introduces coordination overhead:

```text
Thread
  ↓
try to acquire lock
  ↓
may block
  ↓
wait
  ↓
acquire
  ↓
execute
  ↓
release
```

For many workloads this is perfectly appropriate.

But some small operations can be implemented efficiently using atomic variables and CAS without explicit locking.

---

# 3. What Is Lock-Free Concurrency?

**Lock-free concurrency** is a style of concurrent programming where operations can make progress without relying on a traditional mutual-exclusion lock around the shared operation.

A common mechanism is:

```text
Atomic variable
       +
CAS
       ↓
safe state transition
```

The Java `java.util.concurrent.atomic` package provides atomic classes for this purpose.

---

# 4. Atomic Variables

An atomic operation behaves as one indivisible unit from the perspective of concurrent threads.

Java provides classes such as:

```text
AtomicInteger
AtomicLong
AtomicBoolean
AtomicReference
```

These classes provide thread-safe atomic operations on their values/references.

---

# 5. `AtomicInteger`

For counters, Java provides:

```java
AtomicInteger
```

Example:

```java
import java.util.concurrent.atomic.AtomicInteger;

class Counter {

    AtomicInteger count = new AtomicInteger(0);

    void increment() {
        count.incrementAndGet();
    }
}
```

The uploaded `Demo.java` uses two threads, each performing 10,000 increments, with:

```java
AtomicInteger count = new AtomicInteger(0);

count.incrementAndGet();
```

instead of:

```java
count++;
```

The example demonstrates the atomic-counter approach while the two threads execute concurrently. fileciteturn7file0L1-L28

---

# 6. Why `AtomicInteger` Helps

With:

```java
AtomicInteger count = new AtomicInteger(0);
```

you should not do:

```java
count++;
```

because that is not the normal atomic API.

Instead:

```java
count.incrementAndGet();
```

The atomic class provides a dedicated atomic state transition.

So think:

```text
int
→ ordinary variable

AtomicInteger
→ atomic operations provided by the class
```

---

# 7. Common `AtomicInteger` Methods

Important methods include:

```java
get()
set(int newValue)

incrementAndGet()
getAndIncrement()

decrementAndGet()
getAndDecrement()

addAndGet(int delta)
getAndAdd(int delta)

getAndSet(int newValue)
```

---

# 8. `incrementAndGet()` vs `getAndIncrement()`

These two are easy to confuse.

Suppose:

```java
AtomicInteger count = new AtomicInteger(5);
```

### `incrementAndGet()`

```java
int value = count.incrementAndGet();
```

Conceptually:

```text
increment first
↓
return updated value

5 → 6
returns 6
```

### `getAndIncrement()`

```java
int value = count.getAndIncrement();
```

Conceptually:

```text
return old value
↓
then increment

5 → 6
returns 5
```

### Easy memory trick

```text
incrementAndGet()
→ increment THEN get

getAndIncrement()
→ get THEN increment
```

---

# 9. `addAndGet()` vs `getAndAdd()`

Similarly:

```java
addAndGet(5)
```

means:

```text
add first
→ return new value
```

while:

```java
getAndAdd(5)
```

means:

```text
return old value
→ then add
```

This follows the same naming pattern as the increment methods.

---

# 10. AtomicInteger vs `int`

| `int` | `AtomicInteger` |
|---|---|
| Plain variable | Atomic concurrency utility |
| `count++` is non-atomic | `incrementAndGet()` is atomic |
| Needs external coordination for safe shared increment | Provides atomic increment operation |
| Simple and lightweight for non-shared state | Designed for concurrent shared state |

---

# 11. `AtomicLong`

For a `long`-style atomic counter/state variable:

```java
AtomicLong value = new AtomicLong(0);
```

It provides atomic operations similar to `AtomicInteger`.

---

# 12. `AtomicBoolean`

`AtomicBoolean` is useful when a shared state is essentially:

```text
true / false
```

Example use cases:

```java
AtomicBoolean running =
    new AtomicBoolean(true);
```

It is useful for things such as:

- shutdown flags
- one-time state transitions
- coordination flags
- simple producer-consumer state

The lecture connects `AtomicBoolean` with shared boolean state in concurrency.

---

# 13. `AtomicBoolean` vs `volatile boolean`

From the earlier visibility discussion:

```java
volatile boolean flag;
```

is useful when you need visibility of a shared flag.

`AtomicBoolean` is useful when you need **atomic state operations** as well.

For example, an atomic compare-and-set transition can be performed:

```java
flag.compareAndSet(false, true);
```

This means:

```text
If current value == false
        ↓
change it to true
        ↓
as one atomic operation
```

So:

```text
volatile
→ visibility + ordering

AtomicBoolean
→ atomic operations on boolean state
```

A `volatile` variable by itself does not make compound operations atomic.

---

# 14. Why Atomic Classes Are Useful

Atomic classes are especially useful for small shared-state operations such as:

```text
counters
flags
state transitions
object reference updates
```

They can avoid writing explicit lock-based critical sections for those operations.

---

# 15. Important Warning: Atomic Operation ≠ Atomic Block

This is a very important concept.

Suppose:

```java
AtomicInteger count =
    new AtomicInteger(0);

if (count.get() > 4) {
    count.incrementAndGet();
}
```

Each individual atomic operation is safe.

But:

```text
get()
+
decision
+
incrementAndGet()
```

is a **compound logical operation**.

Another thread can change the state between the check and the update.

Therefore:

> **Atomic individual operations do not automatically make a sequence of operations atomic as a whole.**

This is one of the most important interview points of this lecture.

---

# 16. Example of the Compound-Operation Problem

Suppose:

```java
if (count.get() < 10) {
    count.incrementAndGet();
}
```

Two threads can both do:

```text
Thread-1 → get() → 9
Thread-2 → get() → 9

Thread-1 → condition true
Thread-2 → condition true

Thread-1 → increment
Thread-2 → increment
```

The desired invariant may be violated depending on the logic.

The entire check-and-update may need:

```text
CAS loop
or
synchronized / Lock
or
another atomic API designed for that exact state transition
```

---

# 17. `AtomicReference`

Sometimes the shared state is an object reference rather than a number or boolean.

Java provides:

```java
AtomicReference<T>
```

Example:

```java
AtomicReference<String> seat =
    new AtomicReference<>("EMPTY");
```

The uploaded seat-booking example uses:

```java
AtomicReference<String> seat =
    new AtomicReference<>("EMPTY");
```

and two threads attempt to book the same seat. fileciteturn7file1L1-L25

---

# 18. Why `AtomicReference`?

A normal reference:

```java
String seat = "EMPTY";
```

can be read and assigned, but a compound state transition such as:

```text
if EMPTY
→ then assign person
```

needs coordination.

`AtomicReference` provides atomic reference-level operations such as:

```java
compareAndSet(expected, newValue)
```

---

# 19. CAS — Compare And Set

CAS stands for:

> **Compare-And-Set**

A simplified form is:

```java
compareAndSet(expectedValue, newValue)
```

The logic is:

```text
Read current value
        ↓
Is current value == expected value?
        ↓
      YES
        ↓
Replace with new value atomically
```

If the current value is not the expected value:

```text
do not change it
→ return failure
```

---

# 20. CAS Mental Model

Think about a seat:

```text
Current seat = EMPTY
```

Two people want it:

```text
Thread-1 → Aditya
Thread-2 → Rohit
```

Both see:

```text
EMPTY
```

But CAS decides which thread successfully changes the state.

For Thread-1:

```java
seat.compareAndSet("EMPTY", "Aditya");
```

For Thread-2:

```java
seat.compareAndSet("EMPTY", "Rohit");
```

Only one can successfully perform:

```text
EMPTY → person's name
```

because after the first successful update, the current state is no longer `"EMPTY"`.

---

# 21. Complete Seat Booking Example

```java
import java.util.concurrent.atomic.AtomicReference;

class SeatBooking {

    AtomicReference<String> seat =
        new AtomicReference<>("EMPTY");

    boolean bookSeat(String name) {

        String currentValue = seat.get();

        if (!currentValue.equals("EMPTY")) {
            return false;
        }

        return seat.compareAndSet(
            "EMPTY",
            name
        );
    }
}
```

This is the core example from the uploaded `Demo2.java`. fileciteturn7file1L30-L41

Two threads call:

```java
boolean value = sb.bookSeat("Aditya");
```

and:

```java
boolean value = sb.bookSeat("Rohit");
```

with the final seat containing whichever booking won the atomic state transition. fileciteturn7file1L3-L25

---

# 22. Why the Initial `get()` Is Not Enough

Notice:

```java
String currentValue = seat.get();

if (currentValue.equals("EMPTY")) {
    return seat.compareAndSet("EMPTY", name);
}
```

You might wonder:

> "If I already checked that the seat is EMPTY, why do I need CAS?"

Because another thread can change the value **after your `get()` and before your update**.

Timeline:

```text
T1 → get() → EMPTY
T2 → get() → EMPTY

T2 → CAS(EMPTY, Rohit) → SUCCESS

T1 → CAS(EMPTY, Aditya) → FAIL
```

This is exactly why CAS is needed.

The check and conditional update are combined into the CAS operation.

---

# 23. CAS Is a Conditional Atomic Update

The key idea is:

```text
Expected value
       ↓
matches current state?
   ┌────┴────┐
  YES        NO
   ↓          ↓
update       fail
```

This prevents the "check happened earlier, but the state changed afterward" problem.

---

# 24. CAS and Hardware

CAS is a low-level atomic mechanism supported by modern hardware/runtime implementations.

At the conceptual level:

```text
Thread
   ↓
CAS request
   ↓
hardware/runtime atomic operation
   ↓
compare + conditional update
```

The lecture's diagram connects CAS with low-level CPU/memory-controller behavior.

The important programming-level lesson is:

> **The compare and conditional update behave as one atomic state transition.**

You should rely on the Java concurrency guarantees rather than trying to reason about individual electrical signals or a particular CPU implementation.

---

# 25. Why CAS Is Called Lock-Free

A CAS-based operation doesn't require a traditional monitor or explicit mutex around the operation.

Instead:

```text
try update
   ↓
if conflict → retry/fail
```

For example:

```text
Thread-1 → CAS succeeds
Thread-2 → CAS fails
Thread-2 → observes new value
Thread-2 → retries/changes strategy
```

No thread needs to own a conventional lock for the atomic state transition itself.

---

# 26. CAS Retry Model

A typical lock-free algorithm looks conceptually like:

```text
READ current value
      ↓
CALCULATE desired value
      ↓
CAS(current, desired)
      ↓
 ┌─────────────┐
 │             │
SUCCESS       FAILURE
 │             │
 ↓             ↓
done       read again
             ↓
         retry calculation
             ↓
             CAS
```

This is often called a **CAS loop** or retry loop.

---

# 27. Atomic Classes and CAS

A simplified relationship is:

```text
AtomicInteger
AtomicLong
AtomicBoolean
AtomicReference
        ↓
atomic state operations
        ↓
CAS / related atomic mechanisms
```

The exact implementation details can vary by JVM/platform.

For your learning, understand the programming abstraction first.

---

# 28. Lock-Based vs Lock-Free

| Lock-Based | Lock-Free |
|---|---|
| `synchronized`, `Lock` | Atomic classes / CAS |
| Thread may block waiting for lock | No explicit lock ownership for the atomic operation |
| Easier to reason about for large critical sections | Very useful for small state transitions |
| Can suffer from lock contention | Can avoid traditional lock contention |
| May have blocking overhead | May have retries under contention |
| Good for compound critical sections | Good for small atomic updates |

### Important

Do not conclude:

> "Lock-free is always faster."

It depends on:

- contention
- workload
- operation size
- retry frequency
- hardware
- algorithm design

---

# 29. Atomicity vs Lock-Free

These are not the same concept.

### Atomicity

A property:

> The operation behaves as one indivisible unit.

### Lock-free

A concurrency/design approach:

> Progress is achieved without relying on a traditional mutual-exclusion lock for that operation.

A lock-free operation can still be atomic.

---

# 30. `AtomicInteger` Example — Counter

```java
import java.util.concurrent.atomic.AtomicInteger;

public class CounterDemo {

    public static void main(String[] args)
            throws InterruptedException {

        AtomicInteger count =
            new AtomicInteger(0);

        Thread t1 = new Thread(() -> {
            for (int i = 0; i < 10_000; i++) {
                count.incrementAndGet();
            }
        });

        Thread t2 = new Thread(() -> {
            for (int i = 0; i < 10_000; i++) {
                count.incrementAndGet();
            }
        });

        t1.start();
        t2.start();

        t1.join();
        t2.join();

        System.out.println(count.get());
    }
}
```

Expected result:

```text
20000
```

The uploaded example uses the same essential idea with 10,000 increments per thread. fileciteturn7file0L8-L28

---

# 31. Important Methods — Quick Cheat Sheet

## `AtomicInteger`

```java
get()
set(value)

incrementAndGet()
getAndIncrement()

decrementAndGet()
getAndDecrement()

addAndGet(value)
getAndAdd(value)

getAndSet(value)
```

## `AtomicBoolean`

```java
get()
set(value)
compareAndSet(expected, update)
```

## `AtomicReference`

```java
get()
set(value)
compareAndSet(expected, update)
getAndSet(value)
```

---

# 32. Common Interview Question

### Is `AtomicInteger` just a thread-safe `int`?

Not exactly.

A better answer:

> `AtomicInteger` is a concurrency utility that provides atomic operations on an integer value, such as increment, decrement, compare-and-set, and atomic updates.

---

# 33. Common Interview Question

### Does using `AtomicInteger` make every operation on my program atomic?

**No.**

Only operations provided as atomic operations by the class are atomic.

A multi-step sequence involving multiple operations may still require a larger synchronization strategy.

---

# 34. Common Interview Question

### Does `AtomicReference` make the object itself thread-safe?

**No.**

It makes updates to the **reference** atomic.

For example:

```java
AtomicReference<User> user;
```

protects the reference update:

```text
old reference → new reference
```

It does not automatically make all mutable fields inside the `User` object thread-safe.

---

# 35. Common Beginner Mistakes

### Mistake 1

```java
AtomicInteger count = new AtomicInteger();

count++;
```

❌ Don't use it like a plain integer.

Use:

```java
count.incrementAndGet();
```

---

### Mistake 2

> "Atomic means the whole method is atomic."

❌ No.

Atomicity applies to the specific supported atomic operation.

---

### Mistake 3

> "If I use atomic variables, I never need synchronization."

❌ Wrong.

Complex multi-step invariants may still require synchronization, locks, or carefully designed CAS loops.

---

### Mistake 4

> "CAS means compare once and always succeed."

❌ No.

CAS can fail because another thread changed the value between your read and CAS attempt.

---

### Mistake 5

> "Lock-free means no waiting ever."

❌ Not necessarily.

A lock-free algorithm may repeatedly retry under contention.

---

### Mistake 6

> "Lock-free is always faster."

❌ No.

It can be beneficial for suitable workloads but may suffer from repeated retries and contention.

---

# 36. Lecture 8 — Master Mental Model

Think of shared state as:

```text
Shared State
     ↓
Need atomic transition
     ↓
Can a small atomic operation solve it?
      ┌──────────────┐
     YES             NO
      ↓               ↓
Atomic Class       synchronized /
or CAS             Lock / other design
      ↓
No traditional
lock for that
small operation
```

---

# 37. CAS — One-Line Mental Model

Memorize:

> **"Change the value only if it is still what I expected."**

Example:

```java
seat.compareAndSet("EMPTY", "Aditya");
```

means:

> "If the seat is still `EMPTY`, change it to `Aditya`; otherwise, don't change it and report failure."

---

# 38. Connection With Previous Lectures

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
Mutual exclusion
```

### Lecture 7

```text
ReentrantLock
ReadWriteLock
StampedLock
Semaphore
Condition
```

### Lecture 8

```text
Atomic variables
CAS
Lock-free concurrency
```

So the progression is:

```text
Problem
  ↓
synchronized
  ↓
advanced locks
  ↓
atomic / lock-free techniques
```

---

# 39. ⭐ Lecture 8 — Must Remember

```text
1. count++ is a non-atomic read-modify-write operation.

2. Atomic classes provide thread-safe atomic state operations.

3. Important classes:
   AtomicInteger
   AtomicLong
   AtomicBoolean
   AtomicReference

4. AtomicInteger.incrementAndGet() performs an atomic increment.

5. incrementAndGet() returns the updated value.

6. getAndIncrement() returns the old value, then increments.

7. Atomic operations do not automatically make a whole block atomic.

8. Compound check-then-act logic may still require CAS,
   synchronization, or another coordination mechanism.

9. AtomicReference is useful for atomic reference updates.

10. CAS = Compare-And-Set.

11. CAS updates the value only if the current value still matches
    the expected value.

12. CAS can fail when another thread changes the state first.

13. A CAS loop can retry after failure.

14. Lock-free does not mean "always faster."

15. Lock-free does not mean "no retries."

16. Use atomic classes for small shared-state transitions where they fit.

17. For larger multi-step consistency rules, locks/synchronization
    or another suitable concurrency design may still be necessary.

18. The Java concurrency abstraction is more important than reasoning
    about a specific CPU's electrical or cache implementation.
```

---

# 🔥 One-Minute Revision

> **Lock-free concurrency provides an alternative to traditional locking for suitable small shared-state operations. Java's atomic classes such as `AtomicInteger`, `AtomicLong`, `AtomicBoolean`, and `AtomicReference` provide atomic operations without requiring a traditional synchronized critical section. `AtomicInteger.incrementAndGet()` safely increments a shared counter, while methods such as `getAndIncrement()` return the old value before performing the increment. However, atomic individual operations do not make a multi-step block atomic. CAS, or Compare-And-Set, solves many check-then-update problems by changing a value only if its current value still matches an expected value. If another thread changed it first, CAS fails and the algorithm can retry. This makes CAS a fundamental building block of lock-free concurrency. Lock-free does not automatically mean faster; its benefits depend on the workload, contention, and algorithm design.**

---

# 📌 Final Concept Map

```text
                 SHARED MUTABLE STATE
                         │
                         ↓
                    RACE CONDITION
                         │
                         ↓
              Need atomic state change
                         │
            ┌────────────┴────────────┐
            │                         │
         Lock-based               Lock-free
            │                         │
     synchronized / Lock        Atomic Classes
                                      │
                    ┌─────────────────┼─────────────────┐
                    │        │        │                 │
              AtomicInteger AtomicLong AtomicBoolean AtomicReference
                    │                                      │
                    └────────────── CAS ────────────────────┘
                                      │
                           Compare-And-Set
                                      │
                              expected == current?
                                  /         \
                                YES         NO
                                 │           │
                              update       fail
                                 │           │
                                done      retry / handle
```

## Lecture 8 Core Principle

> **Use atomic variables and CAS when the shared-state transition is small enough to be represented as an atomic operation. When the consistency rule spans multiple operations or multiple pieces of state, a larger coordination mechanism such as `synchronized`, `Lock`, or a carefully designed CAS algorithm may still be required.**
