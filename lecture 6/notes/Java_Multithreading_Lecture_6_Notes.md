# Java Multithreading — Lecture 6 Notes

## Inter-Thread Communication: `wait()`, `notify()`, `notifyAll()` & Producer-Consumer

This lecture explains how threads can **coordinate with each other** instead of continuously checking a condition. The main example is the **Producer-Consumer problem**, where a producer creates data and a consumer uses it.

The central mental model is:

```text
Shared Resource
      ↓
    Condition
      ↓
Thread cannot continue yet
      ↓
    wait()
      ↓
Releases the monitor + enters WAITING
      ↓
Another thread changes the condition
      ↓
notify() / notifyAll()
      ↓
Waiting thread becomes eligible again
      ↓
Reacquires monitor
      ↓
Re-checks condition
      ↓
Continues work
```

The uploaded lecture slides introduce `wait()`, `notify()`, and `notifyAll()` as methods of `Object`, then build the Producer-Consumer example around a shared `Box`. fileciteturn5file0L1-L3

---

# 1. Why Do We Need Inter-Thread Communication?

Synchronization can control **who enters a critical section**, but sometimes a thread also needs to know **when a condition becomes true**.

Example:

```text
Consumer → wants an item
Producer → has not produced one yet
```

The consumer should not keep doing useless work forever. It should wait until the producer changes the shared state.

Similarly:

```text
Producer → wants an empty slot
Consumer → has not consumed the current item yet
```

The producer may need to wait until the consumer makes the resource available again.

This is the purpose of **inter-thread communication**.

---

# 2. Producer-Consumer Problem

The classic model is:

```text
Producer
   ↓
 Shared Box / Buffer
   ↓
Consumer
```

The producer creates an item and places it in the shared resource.

The consumer takes the item from the shared resource.

The lecture uses a single-item `Box` as the simple example.

### Main condition

```text
Box has item
        OR
Box is empty
```

The threads must coordinate around this condition.

---

# 3. What Goes Wrong Without Coordination?

Because thread scheduling is non-deterministic, several bad situations are possible.

### Problem 1 — Consumer runs first

```text
Consumer runs
   ↓
No item available
   ↓
Consumer tries to consume null/empty data
```

### Problem 2 — Producer runs multiple times

If the shared resource can hold only one item:

```text
Producer produces item 1
Producer produces item 2
```

before the consumer takes item 1.

The newer item can overwrite the previous one.

The lecture diagram highlights these coordination problems in the Producer-Consumer setup. fileciteturn5file0L2-L3

---

# 4. Busy Waiting

A beginner solution is to repeatedly check the condition:

```java
while (flag == false) {
    // do nothing
}
```

or:

```java
while (item == null) {
    // keep checking
}
```

This is called **busy waiting** (or spinning when implemented as a tight loop).

### Why is busy waiting bad here?

The thread consumes CPU while doing no useful work.

```text
CPU
 ↓
check condition
 ↓
condition false
 ↓
check again
 ↓
check again
 ↓
check again...
```

The lecture contrasts this with `wait()`, which lets the thread stop consuming CPU while waiting.

---

# 5. `wait()`

The `wait()` method is used when a thread must wait for a condition/event before continuing.

Example:

```java
synchronized (lock) {
    lock.wait();
}
```

Conceptually, `wait()` does three important things:

```text
1. Releases the monitor of the object
2. Moves the thread to WAITING
3. Waits until it is awakened/interrupted/spuriously awakened
```

The uploaded slide explicitly describes the sequence as releasing the monitor lock, entering `WAITING`, and staying there until another thread wakes it. fileciteturn5file0L3-L4

---

# 6. VERY IMPORTANT — `wait()` Releases the Monitor

Suppose:

```java
synchronized (lock) {
    lock.wait();
}
```

Before `wait()`:

```text
Thread owns lock
```

After `wait()`:

```text
Thread releases lock's monitor
        ↓
Thread enters WAITING
```

This is essential.

Otherwise another thread would not be able to acquire the same monitor and change the condition that allows the waiting thread to continue.

---

# 7. `wait()` Must Be Used With the Correct Monitor

The normal pattern is:

```java
synchronized (lock) {
    lock.wait();
}
```

The thread must own the monitor of the object on which `wait()` is being called.

If `wait()` is called without owning that object's monitor, Java throws:

```text
IllegalMonitorStateException
```

---

# 8. `wait()`, `notify()`, and `notifyAll()` Belong to `Object`

These methods are defined by the `Object` class, not by `Thread`.

Conceptually:

```text
Object
 ├── wait()
 ├── notify()
 └── notifyAll()
```

Why?

Because these operations work with the **monitor associated with a particular object**.

Example:

```java
Object lock = new Object();

synchronized (lock) {
    lock.wait();
}
```

The object `lock` provides the monitor used for coordination.

The lecture notes emphasize that these methods operate on the monitor of a specific object. fileciteturn5file0L1-L3

---

# 9. `notify()`

`notify()` is used to signal one thread that is waiting on the same object's monitor.

Example:

```java
synchronized (lock) {
    lock.notify();
}
```

Conceptually:

```text
Waiting threads
[T1] [T2] [T3]
       ↓
     notify()
       ↓
One waiting thread is selected
       ↓
That thread becomes eligible to compete for the monitor
```

The exact choice is not something application code should depend on.

### Important

`notify()` does **not** directly hand the lock to the awakened thread.

The awakened thread must still acquire the monitor before it can continue past `wait()`.

The slide illustrates `notify()` as waking one waiting thread, after which that thread competes for the lock. fileciteturn5file0L4-L5

---

# 10. `notifyAll()`

`notifyAll()` signals **all threads waiting on that object's monitor**.

Example:

```java
synchronized (lock) {
    lock.notifyAll();
}
```

Conceptually:

```text
Waiting queue
[T1] [T2] [T3]
       ↓
   notifyAll()
       ↓
All become eligible to compete
       ↓
Only one acquires the monitor at a time
```

The others may become `BLOCKED` while competing for the monitor.

The lecture presents `notifyAll()` as a safer option in situations with multiple waiting threads because all relevant waiters get a chance to re-check their conditions. fileciteturn5file0L5-L6

### Important

`notifyAll()` does **not** mean all waiting threads run at the same time.

Only one thread can own the same monitor at a time.

---

# 11. `notify()` vs `notifyAll()`

| `notify()` | `notifyAll()` |
|---|---|
| Signals one waiting thread | Signals all waiting threads |
| Which waiter is selected is not guaranteed | All waiting threads become eligible to compete |
| Potentially less wake-up overhead | Potentially more wake-up overhead |
| Can be harder to use safely with multiple conditions/waiters | Often safer when several waiters may need to re-check conditions |

### Easy memory trick

```text
notify()
→ wake one

notifyAll()
→ wake all
```

But remember:

> **Waking/notification is not the same as immediately running or owning the lock.**

---

# 12. Producer-Consumer Using `wait()` and `notify()`

A simple single-item design can look like this:

```java
class Box {

    Integer item;
    boolean flag = false;

    synchronized void producer(int value)
            throws InterruptedException {

        while (flag == true) {
            wait();
        }

        item = value;
        flag = true;

        System.out.println("Producer produces " + item);

        notify();
    }

    synchronized void consumer()
            throws InterruptedException {

        while (flag == false) {
            wait();
        }

        System.out.println("Consumer consumes " + item);

        item = null;
        flag = false;

        notify();
    }
}
```

This is the core pattern shown in the lecture slides and demonstrated in the uploaded `Demo3(2).java`. fileciteturn5file1L30-L53

---

# 13. Understand the Producer Logic

```java
synchronized void producer(int value)
        throws InterruptedException {

    while (flag == true) {
        wait();
    }

    item = value;
    flag = true;

    notify();
}
```

Meaning:

```text
Producer enters monitor
       ↓
Is box already full?
       ↓
YES → wait()
       ↓
release monitor + WAITING
       ↓
Consumer consumes item
       ↓
Consumer notifies
       ↓
Producer wakes/re-enters monitor
       ↓
re-check flag
       ↓
produce item
```

If the box is empty (`flag == false`), producer can put the new item immediately.

---

# 14. Understand the Consumer Logic

```java
synchronized void consumer()
        throws InterruptedException {

    while (flag == false) {
        wait();
    }

    System.out.println("Consumer consumes " + item);

    item = null;
    flag = false;

    notify();
}
```

Meaning:

```text
Consumer enters monitor
       ↓
Is box empty?
       ↓
YES → wait()
       ↓
release monitor + WAITING
       ↓
Producer puts item
       ↓
Producer notifies
       ↓
Consumer wakes/re-enters monitor
       ↓
re-check flag
       ↓
consume item
```

The uploaded code shows the consumer using `while(flag == false) wait();` and then clearing the item/flag before notifying another thread. fileciteturn5file1L46-L55

---

# 15. Why `while`, Not `if`?

This is one of the **most important rules** in inter-thread communication.

Do this:

```java
while (conditionIsNotSatisfied) {
    wait();
}
```

Not:

```java
if (conditionIsNotSatisfied) {
    wait();
}
```

Why?

Because a waiting thread can wake up even though the condition is still false. This is called a **spurious wakeup**.

Also, when multiple threads are waiting, another thread may change the shared state before the awakened thread actually reacquires the monitor.

Therefore the thread must always re-check the condition after waking.

---

# 16. Spurious Wakeup

A **spurious wakeup** means a thread waiting with `wait()` may return even though no matching `notify()`/`notifyAll()` caused the desired condition to become true.

The important rule is:

```text
WAKE UP
   ↓
DO NOT ASSUME CONDITION IS TRUE
   ↓
RE-CHECK CONDITION
```

This is why the guarded-block pattern is:

```java
while (!condition) {
    wait();
}
```

The uploaded slides connect spurious wakeups with the need for a guarded block using a `while` loop. fileciteturn5file0L7-L7

---

# 17. Guarded Block

A **guarded block** is code that waits until a condition becomes true.

Pattern:

```java
synchronized (lock) {

    while (!condition) {
        lock.wait();
    }

    // condition is satisfied
    // perform work
}
```

The important part is:

```java
while (!condition)
```

not:

```java
if (!condition)
```

---

# 18. Why Must `wait()` / `notify()` Be Inside `synchronized`?

These methods operate on the monitor of an object.

Therefore the calling thread must own that monitor.

Correct:

```java
synchronized (lock) {
    lock.wait();
}
```

Correct:

```java
synchronized (lock) {
    lock.notify();
}
```

Incorrect:

```java
lock.wait();        // ❌
lock.notify();      // ❌
```

without owning `lock`'s monitor.

Otherwise:

```text
IllegalMonitorStateException
```

can occur.

---

# 19. Thread States During Communication

A useful state picture is:

```text
Producer / Consumer
        ↓
     RUNNABLE
        ↓
condition false
        ↓
      wait()
        ↓
     WAITING
        ↓
 monitor released
        ↓
notify / notifyAll
        ↓
eligible to continue
        ↓
tries to acquire monitor
        ↓
RUNNABLE
```

If another thread still owns the monitor after notification, the awakened thread may temporarily be `BLOCKED` while trying to reacquire it.

The lecture diagram shows waiting threads being awakened and then competing for the monitor. fileciteturn5file0L4-L5

---

# 20. `wait()` vs `sleep()`

This difference is extremely important.

| `wait()` | `sleep()` |
|---|---|
| Method of `Object` | Static method of `Thread` |
| Used for coordination | Used for timed delay |
| Releases the monitor of the object being waited on | Does not release monitors |
| Usually used inside `synchronized` | Does not require owning a monitor |
| Can enter `WAITING` | Enters `TIMED_WAITING` |
| Resumes through notification/interruption/spurious wakeup conditions | Resumes after timeout or interruption |

### Easy memory trick

```text
sleep()
→ I want a timed break.

wait()
→ I need to wait for a condition/event.
```

The lecture slides explicitly contrast `sleep()` as `TIMED_WAITING` with `wait()` as `WAITING`. fileciteturn5file0L7-L7

---

# 21. `wait()` Does Not Mean "Give Up the Thread Forever"

When a thread calls:

```java
wait();
```

it is not terminated.

The thread remains alive and waiting for an appropriate wake-up/recovery path.

Conceptually:

```text
RUNNABLE
   ↓
wait()
   ↓
WAITING
   ↓
notification/interruption/spurious return
   ↓
reacquire monitor
   ↓
RUNNABLE
```

---

# 22. Important Detail About `notify()`

Suppose:

```text
Waiting Queue
[T1] [T2] [T3]
```

and the owner calls:

```java
notify();
```

One waiting thread becomes eligible to compete for the monitor.

It is **not guaranteed that the awakened thread immediately executes**.

The thread still has to reacquire the monitor.

Therefore:

```text
notify()
≠
"run this thread now"
```

---

# 23. Important Detail About `notifyAll()`

Suppose:

```text
Waiting Queue
[T1] [T2] [T3]
```

Then:

```java
notifyAll();
```

makes all waiting threads eligible.

But:

```text
T1 → acquires monitor
T2 → BLOCKED
T3 → BLOCKED
```

Only one thread can own the same monitor at a time.

Therefore `notifyAll()` means:

> **Wake all waiters so they can compete/re-check conditions.**

It does not mean:

> "Execute all waiting threads simultaneously."

---

# 24. Why `notifyAll()` Is Often Safer

With multiple waiting threads or multiple conditions, using only `notify()` can accidentally wake a thread whose condition is still false.

That thread wakes, re-checks, and may go back to waiting, potentially leaving another eligible thread asleep.

`notifyAll()` wakes all relevant waiters so each can re-check its own guard condition.

### Important

`notifyAll()` is **not a magic deadlock-prevention mechanism**. Poor lock design can still produce deadlocks.

---

# 25. Deadlock

A **deadlock** occurs when threads wait indefinitely for resources/locks held by one another.

Simple conceptual example:

```text
Thread-1 holds Lock-A
Thread-1 waits for Lock-B

Thread-2 holds Lock-B
Thread-2 waits for Lock-A
```

Neither can continue.

```text
T1 → waiting for T2
T2 → waiting for T1
```

This forms a cycle.

Inter-thread communication and synchronization must therefore be designed carefully.

The lecture introduces deadlock as one of the risks of incorrect synchronization/coordination.

---

# 26. Producer-Consumer Real-World Example

The concept is not limited to numbers.

Example:

```text
Document Creator
       ↓
     Buffer
       ↓
     Printer
```

The producer creates print jobs.

The consumer/printer processes those jobs.

Other examples:

```text
Web request producer → Worker consumer
Message producer     → Message processor
Log producer         → Log writer
Task producer        → Background worker
```

The same idea appears whenever one part of a system generates work and another part consumes it.

---

# 27. Uploaded `Demo3(2).java` — Complete Flow

The uploaded example creates one shared `Box` and two threads:

```java
Box box = new Box();
```

Producer thread:

```java
box.producer(i);
```

Consumer thread:

```java
box.consumer();
```

Both methods are synchronized, and the `Box` contains the shared `item` and `flag`. fileciteturn5file1L1-L30

The coordination logic is:

```text
                BOX
          ┌──────────────┐
          │ item         │
          │ flag         │
          └──────────────┘
             ↑        ↓
         Producer  Consumer
```

Producer waits while the box is full:

```java
while (flag == true) {
    wait();
}
```

Consumer waits while the box is empty:

```java
while (flag == false) {
    wait();
}
```

Then each side changes the condition and notifies the other. fileciteturn5file1L34-L53

---

# 28. Busy Waiting Version vs `wait()` Version

## Busy waiting

```java
while (flag == false) {
    // keep checking
}
```

Problem:

```text
CPU usage ↑
Useful work ↓
```

## `wait()` version

```java
while (flag == false) {
    wait();
}
```

Now:

```text
Condition false
      ↓
WAITING
      ↓
CPU can do other work
      ↓
notification
      ↓
re-check condition
```

This is the major efficiency benefit of inter-thread communication.

---

# 29. `notify()` / `notifyAll()` and the Waiting Queue

A useful conceptual model is:

```text
             MONITOR
                 │
        ┌────────┴────────┐
        │                 │
     Owner            Waiting threads
        │                 │
       T1              T2 T3 T4
        │
   synchronized
        │
    notifyAll()
        │
   T2 T3 T4 become
   eligible to compete
        │
   only one gets monitor
```

The slide presents this as a simplified model of a monitor, an owning thread, and waiting threads. fileciteturn5file0L4-L6

---

# 30. A Very Important Sequence to Memorize

For a waiting thread:

```text
Thread owns monitor
        ↓
wait()
        ↓
monitor released
        ↓
WAITING
        ↓
notify()/notifyAll()/other wake-up
        ↓
eligible to continue
        ↓
reacquire monitor
        ↓
check condition again
        ↓
continue or wait again
```

The **re-check** is essential.

---

# 31. Common Beginner Mistakes

### Mistake 1

```java
if (!condition) {
    wait();
}
```

❌ Use `while` for a guarded wait.

```java
while (!condition) {
    wait();
}
```

---

### Mistake 2

Calling:

```java
wait();
```

outside the appropriate synchronized context.

❌ Can cause `IllegalMonitorStateException`.

---

### Mistake 3

Thinking:

```text
notify()
→ thread starts immediately
```

❌ No. It becomes eligible to compete for the monitor.

---

### Mistake 4

Thinking:

```text
notifyAll()
→ all threads execute simultaneously
```

❌ No. They compete for the monitor and only one can own it at a time.

---

### Mistake 5

Thinking:

```text
wait() and sleep() are basically the same
```

❌ No.

The biggest difference is lock/monitor behavior and purpose.

---

### Mistake 6

Thinking:

```text
notifyAll() automatically prevents deadlock
```

❌ No. Good synchronization/lock design is still required.

---

### Mistake 7

Using busy waiting when blocking coordination is appropriate.

❌ It can waste CPU.

---

# 32. Interview Questions

## Q1. What is inter-thread communication?

It is a mechanism that allows threads to coordinate their execution based on shared conditions/state.

---

## Q2. What are `wait()`, `notify()`, and `notifyAll()`?

They are methods of `Object` used for monitor-based thread coordination.

---

## Q3. Why are these methods in `Object` rather than `Thread`?

Because they operate on the monitor associated with a particular object.

---

## Q4. What happens when a thread calls `wait()`?

It releases the monitor of that object, enters `WAITING`, and waits for an appropriate wake-up condition.

---

## Q5. Does `wait()` release the lock?

Yes, it releases the monitor of the object on which `wait()` is called.

It does not automatically release unrelated monitors.

---

## Q6. Why must `wait()` be called inside synchronized code?

Because the thread must own the object's monitor when calling `wait()`.

---

## Q7. What happens if `wait()` is called without owning the monitor?

`IllegalMonitorStateException` can be thrown.

---

## Q8. Difference between `notify()` and `notifyAll()`?

```text
notify()
→ signals one waiting thread

notifyAll()
→ signals all waiting threads
```

---

## Q9. Does `notify()` release the lock immediately?

No. The notifying thread keeps the monitor until it exits the synchronized region. The awakened thread later competes to reacquire the monitor.

---

## Q10. Why use `while` around `wait()`?

Because the condition must be re-checked after wake-up due to spurious wakeups and because another thread may have changed the shared state before the awakened thread reacquires the monitor.

---

## Q11. What is busy waiting?

Repeatedly checking a condition in a loop without blocking, which can waste CPU.

---

## Q12. What is the Producer-Consumer problem?

A classic coordination problem where producers generate data/tasks and consumers process them through shared state/buffers.

---

## Q13. Difference between `wait()` and `sleep()`?

```text
wait()
→ coordination
→ releases object's monitor
→ WAITING

sleep()
→ timed delay
→ does not release monitors
→ TIMED_WAITING
```

---

## Q14. Can `notifyAll()` run all waiting threads simultaneously?

No. They become eligible to compete for the monitor, but only one can own that monitor at a time.

---

## Q15. Does `notifyAll()` guarantee no deadlock?

No. It can make coordination safer in some designs but does not eliminate deadlocks caused by incorrect locking.

---

# 33. `wait()` / `notify()` / `notifyAll()` Cheat Sheet

| Method | Belongs to | Purpose | Monitor requirement | Typical state/effect |
|---|---|---|---|---|
| `wait()` | `Object` | Wait for coordination | Must own object's monitor | `WAITING` |
| `notify()` | `Object` | Signal one waiter | Must own object's monitor | One waiter becomes eligible |
| `notifyAll()` | `Object` | Signal all waiters | Must own object's monitor | All waiters become eligible |

---

# 34. Connection With Previous Lectures

## Lecture 4 — Visibility / Ordering

We learned:

```text
Shared state
↓
Visibility / Ordering problems
```

## Lecture 5 — Synchronization

We learned:

```text
synchronized
↓
Mutual exclusion + visibility + ordering
```

## Lecture 6 — Communication

Now we add:

```text
wait()
notify()
notifyAll()
↓
Condition-based coordination
```

So the progression is:

```text
Lecture 4
Problems
   ↓
Lecture 5
Protect shared state
   ↓
Lecture 6
Coordinate when conditions change
```

---

# 35. Most Important Mental Model of Producer-Consumer

Think about the box as having two states:

```text
EMPTY
  ↓
Producer can produce
  ↓
FULL
  ↓
Consumer can consume
  ↓
EMPTY
  ↓
Producer can produce again
```

And the threads communicate when they cannot proceed:

```text
Producer sees FULL
      ↓
wait()

Consumer sees EMPTY
      ↓
wait()
```

After changing the state:

```text
Producer produces
      ↓
notify()
      ↓
Consumer gets chance to continue
```

```text
Consumer consumes
      ↓
notify()
      ↓
Producer gets chance to continue
```

The uploaded slide's Producer-Consumer diagram shows exactly this full/empty coordination idea. fileciteturn5file0L2-L3

---

# 36. ⭐ Lecture 6 — Must Remember

```text
1. Inter-thread communication lets threads coordinate based on shared conditions.

2. Producer-Consumer is the classic example.

3. Busy waiting repeatedly checks a condition and wastes CPU.

4. wait() releases the monitor of the object and puts the thread in WAITING.

5. wait(), notify(), and notifyAll() belong to Object.

6. These methods require the caller to own the corresponding monitor.

7. Calling them incorrectly can cause IllegalMonitorStateException.

8. notify() signals one waiting thread.

9. notifyAll() signals all waiting threads.

10. Notification does not immediately transfer the monitor to the awakened thread.

11. Use while, not if, around wait().

12. Spurious wakeups are one reason the condition must always be re-checked.

13. wait() and sleep() are NOT the same.

14. wait() releases the relevant monitor; sleep() does not release monitors.

15. notifyAll() does not automatically prevent deadlocks.

16. Only one thread can own a given monitor at a time.

17. The awakened thread must reacquire the monitor before continuing.

18. Good coordination = protect the shared state + wait for the right condition.
```

---

# 🔥 One-Minute Revision

> **Inter-thread communication allows multiple threads to coordinate around shared conditions. The classic Producer-Consumer problem uses a shared buffer or resource. Busy waiting wastes CPU because a thread keeps checking a condition without blocking. `wait()` lets a thread release the monitor of the object, enter `WAITING`, and wait until it can continue. `notify()` signals one waiting thread, while `notifyAll()` signals all waiting threads. These methods belong to `Object` because they operate on an object's monitor, and they must be called while owning that monitor. After waking, a thread must reacquire the monitor and re-check the condition, which is why `wait()` should be used inside a `while` guard rather than an `if`. Spurious wakeups and changes made by other threads make that re-check essential. `wait()` is a coordination mechanism and releases the relevant monitor, whereas `sleep()` is a timed delay and does not release monitors.**

---

# 📌 Final Concept Map

```text
              INTER-THREAD COMMUNICATION
                         │
                         ↓
                   Shared Resource
                         │
                         ↓
                    Condition
                    /       \
                   /         \
            Condition true  Condition false
                  │                │
                  ↓                ↓
               Work          wait() / WAITING
                                   │
                                   ↓
                         Monitor released
                                   │
                                   ↓
                        Another thread updates
                                   │
                         ┌─────────┴─────────┐
                         ↓                   ↓
                     notify()          notifyAll()
                         ↓                   ↓
                    one waiter         all waiters
                         │                   │
                         └────────┬──────────┘
                                  ↓
                        Compete for monitor
                                  ↓
                         Re-check condition
                                  ↓
                         Continue / wait again
```

## Lecture 6 Core Principle

> **Synchronization protects the shared resource; inter-thread communication tells threads when they should wait and when they should try again.**

---

# Next Topic

After learning `wait()`, `notify()`, and `notifyAll()`, the natural next step is deeper coordination patterns and the problems they can create, including **deadlocks, locks/conditions, and higher-level concurrency utilities**.
