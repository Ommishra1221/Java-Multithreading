# Java Multithreading — Lecture 9 Notes

## Advanced Lock-Free Concurrency: CAS Retry Loop, AtomicReferenceArray & ABA Problem

This lecture continues the lock-free concurrency topic from Lecture 8. The main ideas are `volatile` vs atomic variables, CAS, CAS retry loops, `AtomicReferenceArray`, lock-based vs lock-free concurrency, the ABA problem, and versioning/stamping.

---

# 1. Connection With Previous Lectures

### Lecture 4
```text
Race Condition
Atomicity
Visibility
Ordering
```

### Lecture 5
```text
synchronized
Monitor
Mutual Exclusion
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
AtomicInteger
AtomicBoolean
AtomicReference
CAS
Lock-Free Concurrency
```

### Lecture 9
```text
CAS Retry Loop
AtomicReferenceArray
ABA Problem
Versioning / Stamping
```

Progression:

```text
Problem
  ↓
Locks
  ↓
Advanced Locks
  ↓
Atomic Variables
  ↓
CAS
  ↓
CAS Retry Logic
  ↓
ABA Problem
  ↓
Versioning
```

---

# 2. `volatile` vs Atomic Variables

## `volatile`

`volatile` is mainly used for:

```text
Visibility
+
Ordering guarantees around volatile access
```

Example:

```java
volatile boolean running;
```

## Atomic Variables

Java provides:

```text
AtomicInteger
AtomicLong
AtomicBoolean
AtomicReference
```

They provide atomic operations on shared state and the required visibility for those operations.

### Remember

```text
volatile
→ visibility / ordering

atomic variable
→ atomic state operations
```

Do not treat `volatile` as a replacement for atomic compound operations.

> The simple "volatile means direct RAM access" explanation is only a teaching model. The actual guarantees come from the Java Memory Model and happens-before rules.

---

# 3. Why Atomic Variables Help

Consider:

```java
count++;
```

Conceptually:

```text
READ
 ↓
MODIFY
 ↓
WRITE
```

Two threads can interfere between these operations.

With:

```java
AtomicInteger count = new AtomicInteger(0);
```

we can use:

```java
count.incrementAndGet();
```

The uploaded example uses `AtomicInteger` and `incrementAndGet()` for a shared counter updated by multiple threads. fileciteturn7file0L33-L45

---

# 4. CAS — Compare And Set

CAS stands for:

> **Compare-And-Set**

Basic form:

```java
compareAndSet(expectedValue, newValue)
```

Logic:

```text
Current value == expected value?
           │
      ┌────┴────┐
     YES        NO
      │          │
    update      fail
      │          │
   success      retry/handle
```

### One-line mental model

> **Change the value only if it is still what I expected.**

---

# 5. Why CAS Is Powerful

Suppose:

```text
seat = EMPTY
```

Two threads want:

```text
EMPTY → Aditya
EMPTY → Rohit
```

Only one CAS can successfully perform the conditional update.

If Thread-1 changes:

```text
EMPTY → Aditya
```

then Thread-2's:

```java
compareAndSet("EMPTY", "Rohit")
```

fails because the current value is no longer `EMPTY`.

---

# 6. Why the Initial `get()` Is Not Enough

This is unsafe by itself:

```java
String current = seat.get();

if (current.equals("EMPTY")) {
    // update
}
```

Another thread may change the value after the read.

Safe idea:

```java
return seat.compareAndSet("EMPTY", name);
```

Timeline:

```text
T1 → reads EMPTY

T2 → reads EMPTY
T2 → CAS(EMPTY, Rohit) → SUCCESS

T1 → CAS(EMPTY, Aditya) → FAILURE
```

CAS combines the comparison and conditional update into one atomic operation.

---

# 7. CAS Retry Loop

A common lock-free pattern is:

```java
while (true) {

    // 1. Read current value
    // 2. Calculate new value
    // 3. Try CAS
    // 4. Retry if CAS fails
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
 ┌───────────────┐
 │               │
SUCCESS        FAILURE
 │               │
 ↓               ↓
DONE           READ again
                 ↓
             calculate again
                 ↓
                 CAS
```

---

# 8. Why Does CAS Fail?

Suppose:

```text
count = 10
```

Both threads read 10 and calculate 11.

Then:

```text
T1 → CAS(10, 11) → SUCCESS

T2 → CAS(10, 11) → FAILURE
```

T2 failed because:

```text
expected = 10
current  = 11
```

T2 must read again:

```text
read 11
→ calculate 12
→ CAS(11, 12)
```

---

# 9. CAS Is Optimistic

CAS follows an optimistic approach:

```text
Assume state has not changed
        ↓
calculate
        ↓
try update
        ↓
conflict?
   ┌────┴────┐
  NO        YES
  ↓           ↓
done        retry
```

Useful mental model:

```text
Locking
→ wait/block

CAS
→ fail/retry
```

---

# 10. Lock-Based vs CAS-Based Concurrency

| Lock-Based | CAS / Lock-Free |
|---|---|
| `synchronized`, `Lock` | Atomic classes / CAS |
| Threads may block | Operation can fail and retry |
| Waiting possible | Retrying possible |
| Mutual exclusion | Atomic conditional update |
| Good for larger critical sections | Good for small state transitions |
| Lock overhead | Retry/contending CAS overhead |

### Important

> Lock-free does **not** mean always faster.

Performance depends on:

```text
workload
contention
operation size
retry frequency
hardware
algorithm design
```

---

# 11. AtomicInteger and CAS

For operations such as:

```java
incrementAndGet();
```

the JDK may use low-level atomic mechanisms such as CAS or equivalent hardware-supported operations.

Conceptually:

```text
current = get()
newValue = current + 1
CAS(current, newValue)
```

If another thread changed the value:

```text
CAS fails
→ recalculate
→ retry
```

The exact implementation is JVM/platform dependent, but this is the correct mental model.

---

# 12. Custom Atomic Increment Using CAS

A simplified CAS implementation is:

```java
AtomicInteger count = new AtomicInteger(0);

while (true) {

    int current = count.get();

    int next = current + 1;

    if (count.compareAndSet(current, next)) {
        break;
    }
}
```

Logic:

```text
1. Read latest value
2. Calculate desired value
3. Compare with value read
4. Update only if unchanged
5. If changed → retry
```

---

# 13. Practical Like Counter

The uploaded example creates a `LikeCounter` and lets ten threads call `like()` repeatedly. It uses:

```java
AtomicInteger totalCount = new AtomicInteger(0);
```

and:

```java
totalCount.incrementAndGet();
```

The same file contains commented code showing the manual CAS retry loop with `get()`, `compareAndSet()`, and retry after conflict. fileciteturn8file1L4-L20 fileciteturn8file1L80-L108

---

# 14. Manual CAS Like Counter

Conceptually:

```java
while (true) {

    Integer currentCount = totalCount.get();
    Integer finalCount = currentCount + 1;

    if (totalCount.compareAndSet(currentCount, finalCount)) {
        return;
    }

    System.out.println("Conflict detected. Re-trying...");
}
```

Think:

```text
capture latest state
      ↓
calculate next state
      ↓
CAS
      ↓
success? ── yes → finish
      │
      no
      ↓
state changed by another thread
      ↓
retry
```

---

# 15. `AtomicReferenceArray`

For multiple atomic reference slots, Java provides:

```java
AtomicReferenceArray<E>
```

Example:

```java
AtomicReferenceArray<String> seats =
    new AtomicReferenceArray<>(5);
```

Then individual elements can support atomic operations:

```java
seats.get(0);
seats.set(0, "EMPTY");

seats.compareAndSet(
    0,
    "EMPTY",
    "Aditya"
);
```

---

# 16. Why Use `AtomicReferenceArray`?

Normal array:

```java
String[] seats = new String[5];
```

does not itself provide atomic compare-and-set operations.

`AtomicReferenceArray` is designed so each element can participate in atomic operations.

Mental model:

```text
AtomicReferenceArray
│
├── index 0 → atomic reference operations
├── index 1 → atomic reference operations
├── index 2 → atomic reference operations
└── ...
```

---

# 17. `AtomicReference` vs `AtomicReferenceArray`

| `AtomicReference` | `AtomicReferenceArray` |
|---|---|
| One atomic reference | Multiple atomic reference slots |
| `get/set/compareAndSet` | Same style per index |
| One shared object/reference | Many independently updated positions |
| Example: active object | Example: seats/slots |

---

# 18. Lock vs CAS — Mental Model

### Lock

```text
T1
 ↓
acquires lock
 ↓
T2
 ↓
BLOCKED
```

### CAS

```text
T1 → tries CAS
T2 → tries CAS

one succeeds
other fails

failed thread
    ↓
re-read
    ↓
re-calculate
    ↓
retry
```

Easy memory trick:

```text
Lock
→ WAIT

CAS
→ RETRY
```

---

# 19. ABA Problem

Now consider:

```text
A
↓
B
↓
A
```

Thread-1 reads:

```text
A
```

Thread-2 changes:

```text
A → B
```

and later:

```text
B → A
```

Thread-1 checks again and sees:

```text
A
```

A simple value-based CAS may conclude:

> "The value is still A."

But the value actually changed in between.

This is the:

> **ABA Problem**

---

# 20. Why Is ABA Dangerous?

CAS may only compare the current value with the expected value.

If the sequence was:

```text
A → B → A
```

then:

```text
initial = A
final   = A
```

A simple comparison cannot detect the intermediate `B` unless additional state is tracked.

---

# 21. ABA Example

Conceptually:

```text
T1 reads A

       T2:
       A → B
       B → A

T1 sees A again

T1 → CAS(A, X)
```

T1 may incorrectly assume that nobody changed the state.

In some lock-free algorithms, that can produce an incorrect result.

---

# 22. Versioning / Stamping Solution

A common solution is to track both:

```text
value
+
version/stamp
```

Instead of:

```text
A
```

track:

```text
(A, 1)
```

After changes:

```text
(A, 1)
↓
(B, 2)
↓
(A, 3)
```

The value returned to `A`, but the version changed.

Therefore:

```text
(A, 1) ≠ (A, 3)
```

and the intermediate modification can be detected.

---

# 23. ABA With Version Number

Without versioning:

```text
T1 reads A

T2: A → B → A

T1 CAS(A, X)
→ may succeed
```

With versioning:

```text
T1 reads (A, 1)

T2:
(A, 1)
→ (B, 2)
→ (A, 3)

T1 expects (A, 1)
Current  = (A, 3)

→ mismatch
→ operation fails/retries
```

---

# 24. Stamping

The lecture uses the idea of **stamping/versioning** to detect intermediate changes.

General model:

```text
State = (value, version)
```

Every relevant modification changes the version.

Therefore:

> **Same value does not necessarily mean same state.**

---

# 25. ABA Versioning vs `StampedLock`

Do not confuse these two ideas.

### `StampedLock`

Uses stamps for locking/version state and supports optimistic reads.

### ABA versioning

Uses a changing version together with a value to detect an intermediate:

```text
A → B → A
```

Both involve the idea of stamps/versions, but they solve different problems.

---

# 26. Why CAS Alone Does Not Solve ABA

CAS compares what it was told to compare.

If only the value is tracked:

```text
A → B → A
```

the final `A` looks identical to the initial `A`.

Versioning adds extra state:

```text
(value, version)
```

so the complete state can be compared.

---

# 27. Lock-Free vs Wait-Free

These are different concepts.

### Lock-Free

The system as a whole continues making progress even if individual threads may repeatedly fail and retry.

### Wait-Free

Every individual thread is guaranteed to complete its operation within a bounded number of steps.

For this lecture, remember:

```text
CAS retry loop
→ lock-free style
→ one thread may need multiple retries
```

---

# 28. CAS Under Heavy Contention

Suppose many threads repeatedly update one atomic variable.

You may see:

```text
read
→ calculate
→ CAS fails
→ read
→ calculate
→ CAS fails
→ retry...
```

Many retries can consume CPU.

Therefore:

> Lock-free avoids traditional lock blocking, but it does not mean zero overhead.

---

# 29. When Atomic/CAS Is a Good Fit

Good candidates:

```text
simple counters
simple flags/state transitions
single-reference updates
small atomic state machines
some lock-free data structures
highly concurrent small updates
```

---

# 30. When `synchronized` / `Lock` Is Better

Prefer traditional synchronization when:

```text
many statements must be protected together
multiple variables form one invariant
business logic is complex
operation is difficult to express as one atomic transition
CAS retry logic becomes complicated
```

Example:

```java
if (balance >= amount) {
    balance -= amount;
    transactionCount++;
    updateAuditLog();
}
```

This is a larger logical operation and is usually easier to protect with a suitable lock-based design.

---

# 31. Atomic Variable vs Atomic Operation

Very important:

```text
Atomic variable
→ class designed to provide atomic operations

Atomic operation
→ one operation that completes indivisibly
```

Having an atomic variable does not make every sequence of operations atomic.

Example:

```java
if (count.get() > 0) {
    count.decrementAndGet();
}
```

The sequence:

```text
get
→ compare
→ decrement
```

is not automatically one atomic transaction.

---

# 32. Practical Decision Tree

```text
Need shared state?
       ↓
Can it be represented by one simple atomic operation?
       │
   ┌───┴────┐
  YES      NO
   │         │
Atomic/CAS   synchronized / Lock
   │
Is there a possibility of ABA?
   │
Use versioning/stamping when required
```

---

# 33. Interview Questions

### Q1. What is CAS?

CAS stands for Compare-And-Set. It atomically updates a value only when its current value matches an expected value.

### Q2. What happens if CAS fails?

The update is not performed. Another thread changed the state first, so a lock-free algorithm may re-read, recalculate, and retry.

### Q3. What is a CAS retry loop?

A loop that repeatedly performs:

```text
read → calculate → compareAndSet → retry if failed
```

### Q4. Why is CAS optimistic?

It assumes the state has not changed, attempts the update, and handles conflicts by retrying instead of taking a traditional exclusive lock first.

### Q5. What is `AtomicReferenceArray`?

A concurrency utility that provides atomic operations on individual elements of an array of references.

### Q6. What is the ABA problem?

A value changes:

```text
A → B → A
```

and a thread using value-only CAS may not detect that an intermediate change happened.

### Q7. How can ABA be solved?

Track a version/stamp together with the value, for example:

```text
(value, version)
```

### Q8. Is lock-free always faster?

No. Heavy contention can cause many failed CAS attempts and retries.

### Q9. Does an `AtomicReference` make the referenced object thread-safe?

No. It makes operations on the reference atomic; the mutable object itself may still need synchronization.

### Q10. Can a sequence of atomic operations be non-atomic?

Yes. Multiple atomic operations can still form a non-atomic compound operation.

### Q11. What is the difference between lock-free and wait-free?

Lock-free guarantees system-wide progress, while wait-free guarantees bounded completion for every individual operation.

---

# 34. Common Beginner Mistakes

### Mistake 1

> "Atomic variable means everything I do with it is atomic."

❌ No.

Only the supported atomic operations are atomic.

### Mistake 2

> "CAS never fails."

❌ No.

CAS fails when the current value is different from the expected value.

### Mistake 3

> "CAS failure means something is broken."

❌ Not necessarily.

In a CAS loop it commonly means another thread won the race.

### Mistake 4

> "Lock-free means no waiting or CPU cost."

❌ No.

Threads may retry repeatedly and consume CPU.

### Mistake 5

> "ABA means the value stayed the same."

❌ The important point is that it changed and returned:

```text
A → B → A
```

### Mistake 6

> "AtomicReference makes the referenced object thread-safe."

❌ No.

It protects reference operations, not arbitrary mutable fields inside the object.

### Mistake 7

> "Versioning means timestamp."

Not necessarily. A version is usually a monotonically changing counter/stamp associated with the state.

---

# 35. Lecture 9 Master Mental Model

```text
              CURRENT STATE
                    │
                    ↓
              READ THE VALUE
                    │
                    ↓
             CALCULATE NEW VALUE
                    │
                    ↓
                CAS(old,new)
                    │
             ┌──────┴──────┐
             │             │
          SUCCESS        FAILURE
             │             │
             ↓             ↓
            DONE       SOMEONE CHANGED IT
                           │
                           ↓
                       READ AGAIN
                           │
                           ↓
                       RECALCULATE
                           │
                           ↓
                          CAS
```

Then the advanced problem:

```text
A
↓
B
↓
A
```

= **ABA problem**

Solution when intermediate changes matter:

```text
(value, version)
```

---

# 36. ⭐ Lecture 9 — Must Remember

```text
1. volatile mainly provides visibility and ordering guarantees.

2. Atomic classes provide atomic operations on shared state.

3. CAS = Compare-And-Set.

4. CAS changes the value only if it still matches the expected value.

5. CAS failure usually means another thread changed the state first.

6. Lock-free algorithms often use CAS retry loops.

7. CAS is optimistic: try → fail if changed → retry.

8. AtomicInteger.incrementAndGet() is an atomic increment operation and
   may rely internally on CAS or equivalent atomic mechanisms.

9. AtomicReference is useful for atomic reference updates.

10. AtomicReferenceArray provides atomic operations on individual
    reference elements in an array.

11. Individual atomic operations do not automatically make a sequence
    of operations atomic.

12. Lock-free does NOT mean always faster.

13. Heavy contention can cause many CAS retries.

14. ABA problem = A → B → A.

15. Simple value-only CAS may not detect the intermediate change.

16. Versioning/stamping can solve ABA by comparing (value, version).

17. Lock-free techniques are useful for suitable small state transitions,
    not every complex business operation.

18. Complex multi-step invariants may still be better protected using
    synchronized or Lock.
```

---

# 🔥 One-Minute Revision

> **CAS is the foundation of many lock-free algorithms. A thread reads a value, calculates a new value, and uses `compareAndSet(expected, newValue)` to update it only if the value has not changed since the read. If CAS fails, another thread changed the state, so the algorithm can re-read, recalculate, and retry. This creates the CAS retry-loop pattern. Atomic classes such as `AtomicInteger` provide atomic state operations, while `AtomicReferenceArray` provides similar operations for individual array elements. However, atomic individual operations do not automatically make a larger sequence atomic. A major advanced issue is the ABA problem, where a value changes `A → B → A`; value-only CAS may miss the intermediate change. Versioning or stamping can solve this by tracking `(value, version)`. Lock-free code can reduce traditional blocking but is not automatically faster, especially under heavy contention. Complex multi-step invariants may still be better handled with locks or synchronization.**

---

# 📌 Final Concept Map

```text
                 LOCK-FREE CONCURRENCY
                         │
                         ↓
                    ATOMIC VARIABLES
                         │
         ┌───────────────┼────────────────┐
         │               │                │
   AtomicInteger    AtomicReference   AtomicReferenceArray
         │               │                │
         └───────────────┼────────────────┘
                         ↓
                         CAS
                Compare-And-Set
                         │
                ┌────────┴────────┐
                │                 │
             SUCCESS            FAILURE
                │                 │
                ↓                 ↓
              DONE             RETRY
                                  │
                                  ↓
                               READ AGAIN
                                  │
                                  ↓
                              RECALCULATE
                                  │
                                  ↓
                                  CAS

                         │
                         ↓
                    ADVANCED ISSUE
                         │
                         ↓
                      A → B → A
                         │
                         ↓
                    ABA PROBLEM
                         │
                         ↓
                  VERSION / STAMPING
                         │
                         ↓
                   (value, version)
```

## Lecture 9 Core Principle

> **Use atomic variables and CAS when the shared-state transition is small enough to be represented as an atomic operation. When the consistency rule spans multiple operations or multiple pieces of state, a larger coordination mechanism such as `synchronized`, `Lock`, or a carefully designed CAS algorithm may still be required.**
