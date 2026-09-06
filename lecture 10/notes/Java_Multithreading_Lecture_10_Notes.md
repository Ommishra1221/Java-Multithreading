# Java Multithreading — Lecture 10 Notes

## Executor Framework in Java

This lecture introduces the **Executor Framework**, a higher-level approach to concurrent task execution. Instead of creating and managing one `Thread` for every task, we submit tasks to an executor, which manages reusable worker threads, queues, results, scheduling, shutdown, and overload behavior.

---

# 1. Why Manual Thread Creation Becomes a Problem

For a small program this is fine:

```java
Thread t1 = new Thread(task1);
Thread t2 = new Thread(task2);

t1.start();
t2.start();
```

But imagine hundreds or thousands of tasks:

```text
Task 1 → Thread 1
Task 2 → Thread 2
Task 3 → Thread 3
...
Task N → Thread N
```

Problems can include:

- thread memory overhead
- OS scheduling overhead
- thread creation/destruction cost
- excessive context switching
- poor scalability

The lecture slides illustrate substantial per-thread memory usage and show that very large numbers of threads can lead to **thrashing**. These figures are illustrative, not a universal fixed memory cost for every Java thread. fileciteturn9file0L1-L4

### Core idea

> **More threads do not automatically mean more performance.**

---

# 2. Thread-per-Task vs Executor Model

### Manual model

```text
Task
 ↓
Create Thread
 ↓
Start Thread
 ↓
Execute
 ↓
Thread finishes
```

### Executor model

```text
Task
 ↓
ExecutorService
 ↓
Thread Pool
 ↓
Worker Thread
 ↓
Execute Task
```

The executor separates:

```text
WHAT work should be done?
        ↓
Runnable / Callable

HOW the work is executed?
        ↓
Executor Framework
```

This is **separation of task submission from thread management**.

---

# 3. What Is the Executor Framework?

The Executor Framework is a Java concurrency framework that manages asynchronous task execution through executors and worker threads.

The central idea is:

```text
You submit tasks.
The framework manages threads.
```

This lets application code focus more on business logic instead of manually handling every thread.

---

# 4. Thread Pool

A **thread pool** is a collection of reusable worker threads.

Instead of:

```text
Task 1 → new Thread
Task 2 → new Thread
Task 3 → new Thread
```

we use:

```text
             ┌───────────────┐
Task 1 ─────→│               │
Task 2 ─────→│   Work Queue  │
Task 3 ─────→│               │
Task 4 ─────→│               │
             └───────┬───────┘
                     ↓
              ┌─────────────┐
              │Worker Threads│
              │ W1 W2 W3 W4│
              └─────────────┘
```

The workers are reused for many tasks.

---

# 5. Why Reuse Worker Threads?

Suppose there are 4 worker threads and 10 tasks:

```text
Worker-1 → Task1 → Task5 → Task9
Worker-2 → Task2 → Task6 → Task10
Worker-3 → Task3 → Task7
Worker-4 → Task4 → Task8
```

The same workers handle multiple tasks.

This avoids repeatedly paying the cost of:

```text
create thread
→ execute
→ destroy thread
→ create another
```

---

# 6. Work Queue

When all appropriate workers are busy, additional tasks can wait in a queue.

```text
Submitted Tasks
      ↓
   Work Queue
      ↓
Worker becomes free
      ↓
Worker takes next task
```

The lecture diagram explicitly shows tasks entering a queue while a set of worker threads consumes those tasks. fileciteturn9file0L2-L4

The queue therefore acts as a **buffer between incoming work and available workers**.

---

# 7. Executor Hierarchy

The important hierarchy is:

```text
Executor
   ↓
ExecutorService
   ↓
Implementations
```

Important implementations include:

```text
ThreadPoolExecutor
ScheduledThreadPoolExecutor
ForkJoinPool
```

The lecture diagram shows `Executor` → `ExecutorService` and the major implementation families. fileciteturn9file0L3-L4

---

# 8. `Executor`

The basic `Executor` abstraction provides:

```java
void execute(Runnable command);
```

Example:

```java
Executor executor = ...;

executor.execute(() -> {
    System.out.println("Task running");
});
```

Think:

> **Executor = basic task execution abstraction.**

---

# 9. `ExecutorService`

`ExecutorService` extends the basic executor idea and adds richer task submission and lifecycle management.

Important methods include:

```java
execute()
submit()
shutdown()
shutdownNow()
invokeAll()
invokeAny()
```

For this lecture, remember the major distinction:

```text
execute() → simple Runnable execution
submit()  → Future/result tracking
```

---

# 10. `execute()`

Example:

```java
executor.execute(() -> {
    System.out.println("Hello");
});
```

The task is a `Runnable` and no `Future` is returned.

The uploaded `Demo.java` demonstrates a fixed pool with `execute()` and five submitted tasks. fileciteturn9file4L4-L21

---

# 11. `submit()`

Example:

```java
Future<Integer> future = executor.submit(() -> {
    return 10;
});
```

`submit()` can accept `Runnable` or `Callable` tasks and returns a `Future` representing the submitted computation.

---

# 12. `Runnable` vs `Callable`

## `Runnable`

```java
Runnable task = () -> {
    System.out.println("Running");
};
```

It represents a task that does not return a result:

```text
run()
 ↓
void
```

## `Callable<T>`

```java
Callable<Integer> task = () -> {
    return 10;
};
```

It represents a task that can return a value:

```text
call()
 ↓
T
```

`Callable` can also throw checked exceptions.

The lecture slides explicitly contrast `Runnable`'s lack of a return type with `Callable`'s result-returning behavior. fileciteturn9file0L4-L6

---

# 13. Runnable vs Callable — Interview Table

| Runnable | Callable |
|---|---|
| `run()` | `call()` |
| returns `void` | returns a value |
| commonly used with `execute()` | commonly used with `submit()` |
| cannot directly declare checked exceptions in `run()` | can throw checked exceptions |
| good for tasks where no result is required | good when a result is needed |

---

# 14. `Future`

A `Future` is a handle representing the result of an asynchronous computation.

Example:

```java
Future<Integer> future = executor.submit(() -> {
    return 10;
});
```

Think:

```text
submit task
    ↓
Future
    ↓
result available later
```

The task may still be running when `submit()` returns.

---

# 15. `future.get()`

To obtain the result:

```java
Integer result = future.get();
```

If the result is not ready, `get()` can block the calling thread until it becomes available.

The uploaded `Demo2.java` demonstrates submitting a `Callable` and later calling `f1.get()` to retrieve its result. fileciteturn9file3L7-L23

### Important

```text
submit() → asynchronous submission
get()    → may block
```

---

# 16. Exceptions: `execute()` vs `submit()`

The lecture code contrasts exception handling for the two APIs.

With:

```java
executor.execute(() -> {
    int x = 10 / 0;
});
```

the task exception occurs on worker-thread execution.

With:

```java
Future<Integer> future = executor.submit(() -> {
    return 10 / 0;
});
```

the failure is associated with the `Future`, and calling:

```java
future.get();
```

reports it to the caller through an `ExecutionException` whose cause is the task exception.

The uploaded `Demo3.java` demonstrates this `execute()` vs `submit()` distinction. fileciteturn9file2L9-L26

---

# 17. `ThreadPoolExecutor`

`ThreadPoolExecutor` is a configurable implementation of `ExecutorService`.

It allows control over things such as:

```text
corePoolSize
maximumPoolSize
keepAliveTime
TimeUnit
workQueue
ThreadFactory
RejectedExecutionHandler
```

The lecture slide explicitly shows these configuration components. fileciteturn9file0L7-L9

---

# 18. Basic `ThreadPoolExecutor` Example

The uploaded `Demo4.java` uses:

```java
ThreadPoolExecutor executor =
    new ThreadPoolExecutor(
        2,
        5,
        10,
        TimeUnit.SECONDS,
        new ArrayBlockingQueue<>(2)
    );
```

So this example has:

```text
corePoolSize    = 2
maximumPoolSize = 5
keepAliveTime   = 10 seconds
queue           = ArrayBlockingQueue(capacity 2)
```

fileciteturn9file1L7-L16

---

# 19. `corePoolSize`

Example:

```text
corePoolSize = 2
```

This is the executor's baseline/core worker capacity.

For beginner understanding:

> The executor normally maintains core workers to handle tasks.

---

# 20. `maximumPoolSize`

Example:

```text
maximumPoolSize = 5
```

This is the maximum worker count the executor can reach under its configured queue/thread-creation behavior.

Important:

> `maximumPoolSize` does not mean the executor immediately creates that many threads.

---

# 21. `keepAliveTime`

Example:

```text
10 TimeUnit.SECONDS
```

This controls how long eligible idle excess workers can remain before being terminated.

For beginner understanding:

```text
extra idle worker
      ↓
wait for keepAlive period
      ↓
terminate if eligible
```

Whether core workers are affected also depends on the executor configuration.

---

# 22. Work Queue — `ArrayBlockingQueue`

The example uses:

```java
new ArrayBlockingQueue<>(2)
```

This is a **bounded queue** with capacity 2.

```text
[Task][Task]
```

Once it becomes full, the executor may create additional workers up to the maximum according to `ThreadPoolExecutor`'s execution rules; after that, the rejection policy applies.

---

# 23. `ThreadPoolExecutor` Task-Submission Flow

A simplified mental model is:

```text
New task
   ↓
Core worker capacity available?
   ↓
Use/create core worker
   ↓
If core capacity reached
   ↓
Offer task to queue
   ↓
Queue accepts?
   ├── YES → wait in queue
   └── NO
        ↓
Can create worker below maximum?
   ├── YES → create extra worker
   └── NO  → reject task
```

This is the most important conceptual flow for understanding `ThreadPoolExecutor`.

---

# 24. Why Queue Choice Matters

The queue strongly influences executor behavior.

Common choices include:

```text
ArrayBlockingQueue
LinkedBlockingQueue
SynchronousQueue
```

A bounded queue:

```text
limits queued work
→ gives explicit overload behavior
```

An unbounded queue:

```text
can absorb more tasks
→ may hide overload
→ can increase memory use
```

Therefore queue selection is part of executor design.

---

# 25. Rejection Policy

A task can be rejected when:

```text
pool is at maximum capacity
        +
queue cannot accept more tasks
```

Then a `RejectedExecutionHandler` decides what happens.

---

# 26. `AbortPolicy`

Behavior:

```text
new task
   ↓
reject
   ↓
RejectedExecutionException
```

This makes overload visible to the caller.

---

# 27. `DiscardPolicy`

Behavior:

```text
new task
   ↓
reject
   ↓
silently discard
```

No exception is thrown for that rejected submission.

Only use it when losing that work is acceptable.

---

# 28. `DiscardOldestPolicy`

Behavior:

```text
Queue full
   ↓
remove oldest queued task
   ↓
try to submit new task again
```

Conceptually:

```text
Before:
[A][B][C]

New task D arrives

Remove oldest A

After:
[B][C][D]
```

The lecture slides specifically highlight this policy along with `AbortPolicy` and `DiscardPolicy`. fileciteturn9file0L10-L11

---

# 29. Other Important Rejection Policy — `CallerRunsPolicy`

A useful additional policy is:

```java
ThreadPoolExecutor.CallerRunsPolicy
```

When the pool cannot accept the task, the submitting/caller thread executes the task itself.

This can act as a form of **backpressure** because the producer slows down while doing the work.

---

# 30. Rejection Policies — Quick Table

| Policy | Behavior |
|---|---|
| `AbortPolicy` | Throws `RejectedExecutionException` |
| `DiscardPolicy` | Silently discards the new task |
| `DiscardOldestPolicy` | Removes oldest queued task and retries submission |
| `CallerRunsPolicy` | Caller thread executes the task |

---

# 31. Fixed Thread Pool

Convenience factory:

```java
ExecutorService executor =
    Executors.newFixedThreadPool(2);
```

The uploaded `Demo.java` uses this exact pattern. fileciteturn9file4L4-L21

Conceptually:

```text
FixedThreadPool(2)

Worker-1
Worker-2
```

Tasks beyond immediately available workers wait according to the executor's queueing behavior.

---

# 32. Why Fixed Thread Pool?

It provides a controlled worker count.

Useful when you want predictable concurrency and do not want a new thread created for every task.

Typical idea:

```text
number of workers stays fixed
```

---

# 33. Cached Thread Pool

Convenience factory:

```java
Executors.newCachedThreadPool();
```

It is designed for workloads with many short-lived asynchronous tasks and can create/reuse workers dynamically.

Conceptually:

```text
short-lived tasks
     ↓
reuse idle workers
     ↓
create more workers when demand requires
```

It does not have a traditional bounded waiting queue like the fixed-pool model.

### Caution

Under heavy demand it can create a large number of threads, so it should not be used blindly.

---

# 34. Fixed vs Cached Thread Pool

| Fixed Thread Pool | Cached Thread Pool |
|---|---|
| Fixed worker count | Dynamic worker creation/reuse |
| More predictable resources | Can grow significantly under bursts |
| Tasks wait when workers are busy | Designed for short-lived asynchronous tasks |
| Useful for controlled concurrency | Useful for highly bursty short tasks |

---

# 35. Single Thread Executor

Factory:

```java
Executors.newSingleThreadExecutor();
```

Conceptually:

```text
1 worker
+
queue
```

Tasks submitted to that executor are executed sequentially.

Example:

```text
Task1
 ↓
Task2
 ↓
Task3
 ↓
Task4
```

### Use cases

- ordered background processing
- logging pipelines
- serialized updates
- work that must not overlap

This does not make your entire application single-threaded. It only serializes tasks submitted to that executor.

---

# 36. Scheduled Thread Pool

The executor framework also provides:

```java
ScheduledThreadPoolExecutor
```

and:

```java
Executors.newScheduledThreadPool(n)
```

Useful for delayed or periodic execution.

---

# 37. `schedule()`

Example:

```java
scheduler.schedule(
    () -> System.out.println("Hello"),
    2,
    TimeUnit.SECONDS
);
```

Meaning:

```text
wait 2 seconds
     ↓
run task
```

The lecture slide shows this delayed-execution model. fileciteturn9file0L10-L11

---

# 38. `scheduleAtFixedRate()`

Example:

```java
scheduler.scheduleAtFixedRate(
    task,
    0,
    2,
    TimeUnit.SECONDS
);
```

Conceptually:

```text
initial delay
   ↓
run
   ↓
run again according to fixed-rate scheduling
   ↓
repeat
```

---

# 39. `scheduleWithFixedDelay()`

Another important scheduled method:

```java
scheduleWithFixedDelay()
```

Conceptually:

```text
run task
   ↓
wait for task to finish
   ↓
wait for delay
   ↓
run again
```

So:

```text
scheduleAtFixedRate
→ fixed-rate style scheduling

scheduleWithFixedDelay
→ fixed delay AFTER previous completion
```

---

# 40. Executor Lifecycle

An executor should eventually be shut down when it is no longer needed.

Basic lifecycle:

```text
RUNNING
   ↓
shutdown()
   ↓
No new tasks
   ↓
Existing submitted tasks finish
   ↓
TERMINATED
```

The uploaded examples call `shutdown()` after submitting tasks. fileciteturn9file4L15-L24 fileciteturn9file1L19-L34

---

# 41. `shutdown()`

```java
executor.shutdown();
```

Meaning:

- stop accepting new tasks
- previously submitted tasks may continue
- executor eventually terminates after existing work finishes

This is usually the preferred normal shutdown approach.

---

# 42. `shutdownNow()`

```java
executor.shutdownNow();
```

This is more aggressive/best-effort.

It:

- stops accepting new tasks
- attempts to interrupt running tasks
- returns tasks that were queued but never started

### Important

It does **not** forcibly kill arbitrary Java code.

Tasks need to respond appropriately to interruption.

---

# 43. `shutdown()` vs `shutdownNow()`

| `shutdown()` | `shutdownNow()` |
|---|---|
| Graceful | Best-effort aggressive stop |
| Existing tasks may finish | Attempts to interrupt running tasks |
| Does not interrupt running work by itself | Interrupts worker threads |
| Preferred normal shutdown | Useful when urgent stop is required |

---

# 44. `invokeAll()`

`ExecutorService` provides:

```java
invokeAll()
```

for submitting multiple `Callable` tasks and obtaining their `Future` objects.

Conceptually:

```text
List<Callable<T>>
       ↓
invokeAll()
       ↓
List<Future<T>>
```

---

# 45. `invokeAny()`

`invokeAny()` submits multiple `Callable` tasks and returns a result from one task according to its completion/success semantics.

Mental model:

```text
Task A ─┐
Task B ─┼──> compete to complete
Task C ─┘
          ↓
     one result returned
```

Useful when any one successful answer is sufficient.

---

# 46. `Future` as a Ticket

A good mental model:

> **Future = a ticket/handle for a result that may be available later.**

```text
submit()
   ↓
Future<T>
   ↓
background computation
   ↓
future.get()
   ↓
result
```

---

# 47. Thread Pool Executor — Complete Mental Model

```text
                New Task
                   ↓
            ExecutorService
                   ↓
          ThreadPoolExecutor
                   ↓
          ┌────────┴────────┐
          ↓                 ↓
    Worker Threads      Work Queue
          ↓                 ↓
       Execute         Waiting Tasks
          │                 │
          └────── consume ──┘
```

---

# 48. `ThreadPoolExecutor` Decision Flow

```text
Task arrives
    ↓
Core worker capacity available?
   /                  \
 YES                  NO
  ↓                     ↓
use/create core      queue task
                        ↓
                    queue full?
                    /        \
                  NO          YES
                  ↓             ↓
              wait in       below max?
               queue         /      \
                           YES       NO
                            ↓         ↓
                       extra worker reject
                                      ↓
                              rejection policy
```

This is a simplified teaching model; exact behavior depends on the executor's queue and configuration.

---

# 49. Production Perspective

A good production thread-pool design asks:

```text
1. How expensive are the tasks?
2. CPU-bound or I/O-bound?
3. How much concurrency is safe?
4. How large should the queue be?
5. What happens when the system is overloaded?
6. What rejection policy is appropriate?
7. How will shutdown be handled?
```

Thread-pool sizing and queue choice are system-design decisions.

---

# 50. CPU-Bound vs I/O-Bound Tasks

## CPU-bound

Examples:

```text
calculations
compression
image processing
complex algorithms
```

Too many workers can increase CPU contention and reduce efficiency.

## I/O-bound

Examples:

```text
database calls
network requests
file operations
remote APIs
```

Workers may spend time waiting, so a higher degree of concurrency can sometimes make sense.

The correct pool size depends on the workload and environment.

---

# 51. Why Bounded Queues Can Be Useful

Suppose:

```text
workers full
+
queue unlimited
```

The system can keep accepting tasks while memory usage grows.

With a bounded queue:

```text
workers full
+
queue full
     ↓
backpressure / rejection
```

This makes overload behavior explicit.

---

# 52. Common Beginner Mistakes

### Mistake 1

> Executor Framework means threads are no longer used.

❌ Wrong.

The framework still uses threads; it manages them for you.

### Mistake 2

> One task always gets a brand-new thread.

❌ Wrong.

Thread pools reuse worker threads.

### Mistake 3

> `submit()` immediately gives the result.

❌ Wrong.

It returns a `Future` that represents a result that may become available later.

### Mistake 4

> `Future.get()` never blocks.

❌ Wrong.

It may block until the result is ready.

### Mistake 5

> More worker threads always improve performance.

❌ Wrong.

Too many workers can increase context switching and contention.

### Mistake 6

> `shutdownNow()` forcibly kills every task.

❌ Wrong.

It is best-effort and uses interruption for running tasks.

### Mistake 7

> Fixed thread pool means tasks cannot be queued.

❌ Wrong.

Tasks can wait for available workers.

### Mistake 8

> An unbounded queue is always better because it avoids rejection.

❌ Wrong.

It can hide overload and increase memory consumption.

---

# 53. Fresher Interview Questions

## Q1. Why do we use the Executor Framework?

To separate task submission from thread management and efficiently reuse worker threads rather than manually creating a thread for every task.

## Q2. What is a thread pool?

A collection of reusable worker threads that execute submitted tasks.

## Q3. Difference between `Executor` and `ExecutorService`?

`Executor` provides basic task execution through `execute()`; `ExecutorService` adds richer submission and lifecycle-management APIs.

## Q4. Difference between `execute()` and `submit()`?

```text
execute()
→ Runnable
→ no Future

submit()
→ Runnable / Callable
→ Future
```

## Q5. Difference between `Runnable` and `Callable`?

```text
Runnable
→ run()
→ no result

Callable<T>
→ call()
→ returns T
→ can throw checked exceptions
```

## Q6. What is a Future?

A handle representing the result/status of an asynchronous computation.

## Q7. Is `Future.get()` blocking?

It can be, when the result is not ready.

## Q8. What is `ThreadPoolExecutor`?

A configurable `ExecutorService` implementation for managing workers, queues, and task rejection.

## Q9. What is `corePoolSize`?

The baseline/core worker capacity.

## Q10. What is `maximumPoolSize`?

The maximum worker count the executor can reach under its configured execution rules.

## Q11. What is `keepAliveTime`?

The idle period after which eligible excess workers can terminate.

## Q12. What happens when the pool and queue are both saturated?

The configured rejection policy is invoked.

## Q13. What is `AbortPolicy`?

Reject the task and throw `RejectedExecutionException`.

## Q14. What is `DiscardPolicy`?

Silently discard the rejected task.

## Q15. What is `DiscardOldestPolicy`?

Remove the oldest queued task and retry submission of the new task.

## Q16. Difference between `shutdown()` and `shutdownNow()`?

```text
shutdown()
→ graceful

shutdownNow()
→ best-effort interruption + returns queued tasks
```

## Q17. What is a fixed thread pool?

An executor designed around a fixed worker count.

## Q18. What is a single-thread executor?

An executor with one worker that processes submitted tasks sequentially.

## Q19. What is a scheduled thread pool?

An executor that supports delayed and periodic task execution.

## Q20. Why not create a new thread for every task?

Because thread creation, memory usage, scheduling, and context switching can become expensive at scale.

---

# 54. Master Comparison Table

| Concept | Main Purpose |
|---|---|
| `Thread` | Manual thread management |
| `Executor` | Basic task execution |
| `ExecutorService` | Task execution + lifecycle/control |
| `ThreadPoolExecutor` | Fine-grained configurable thread pool |
| Fixed Thread Pool | Controlled worker count |
| Cached Thread Pool | Dynamic worker reuse/growth for suitable short tasks |
| Single Thread Executor | Sequential task execution |
| Scheduled Thread Pool | Delayed/periodic execution |
| `Runnable` | Task without a return result |
| `Callable<T>` | Task with a return result |
| `Future<T>` | Handle for an asynchronous result |

---

# 55. Connection With Previous Lectures

### Lecture 1

```text
Process
Thread
Concurrency
Parallelism
```

### Lecture 2

```text
Thread creation
Runnable
start() vs run()
Thread lifecycle
```

### Lecture 3

```text
sleep()
join()
interrupt()
thread identity
priority
daemon threads
```

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
ReentrantLock
ReadWriteLock
StampedLock
Semaphore
Condition
```

### Lecture 8–9

```text
Atomic variables
CAS
Lock-free concurrency
ABA
Versioning
```

### Lecture 10

```text
Task management at scale
Thread pools
Future
ExecutorService
ThreadPoolExecutor
Scheduling
Shutdown
Rejection
```

So the progression is:

```text
Threads
  ↓
Thread management
  ↓
Synchronization
  ↓
Lock-free techniques
  ↓
Executor Framework
  ↓
Production-scale task execution
```

---

# ⭐ Lecture 10 — Must Remember

```text
1. Thread-per-task designs do not scale indefinitely.

2. Threads have meaningful memory and scheduling overhead.

3. Too many threads can cause excessive context switching.

4. Executor Framework separates task submission from thread management.

5. Thread pools reuse worker threads.

6. Work queues hold tasks waiting for workers.

7. Executor → basic task-execution abstraction.

8. ExecutorService → task execution + lifecycle/control.

9. ThreadPoolExecutor → configurable thread-pool implementation.

10. execute() → Runnable, no Future.

11. submit() → Runnable/Callable, returns Future.

12. Runnable → no result.

13. Callable<T> → returns T and can throw checked exceptions.

14. Future → handle for an asynchronous result.

15. Future.get() may block.

16. corePoolSize → baseline worker capacity.

17. maximumPoolSize → upper worker limit under configuration.

18. keepAliveTime → idle time for eligible excess workers.

19. Queue choice affects pool behavior.

20. Saturation triggers the rejection policy.

21. AbortPolicy → throws exception.

22. DiscardPolicy → silently drops the new task.

23. DiscardOldestPolicy → removes oldest queued task and retries.

24. shutdown() → graceful shutdown.

25. shutdownNow() → best-effort interruption + queued tasks returned.

26. FixedThreadPool → fixed worker count.

27. CachedThreadPool → dynamic worker reuse/growth.

28. SingleThreadExecutor → sequential tasks.

29. ScheduledThreadPool → delayed/periodic execution.

30. More threads do NOT automatically mean better performance.
```

---

# 🔥 One-Minute Revision

> **The Executor Framework solves the scalability and management problems of creating a separate thread for every task. Instead of manually managing threads, we submit `Runnable` or `Callable` tasks to an `ExecutorService`, which manages reusable worker threads and a work queue. `execute()` is used for `Runnable` tasks without a `Future`, while `submit()` can accept `Runnable` or `Callable` and returns a `Future`. `Callable` can return a result, and `Future.get()` retrieves it but may block the caller. `ThreadPoolExecutor` provides detailed control through core pool size, maximum pool size, keep-alive time, work queue, and rejection policy. Fixed, cached, single-thread, and scheduled executors provide common configurations. Proper shutdown is essential. The main lesson is: think in terms of submitting units of work to a controlled execution system instead of creating one thread per task.**

---

# 📌 Final Concept Map

```text
                      EXECUTOR FRAMEWORK
                              │
                 ┌────────────┴────────────┐
                 │                         │
               TASKS                   THREAD POOL
                 │                         │
          ┌──────┴──────┐            reusable workers
          │             │                    │
      Runnable       Callable                │
          │             │                    │
        void          result                  │
          │             │                    │
          └──────┬──────┘                    │
                 ↓                           │
            ExecutorService ←───────────────┘
                 │
       ┌─────────┼───────────────┐
       │         │               │
    execute    submit        scheduling
       │         │               │
       │       Future        ScheduledPool
       │         │
       │       get()
       │         │
       │      result
       │
       ↓
ThreadPoolExecutor
       │
  ┌────┼───────────────┐
  ↓    ↓               ↓
core  max             queue
pool  pool              │
                        ↓
                 rejection policy
```

## Lecture 10 Core Principle

> **Don't think “one task = one thread.” Think “one task = a unit of work submitted to an execution system.” The Executor Framework decides how worker threads, queues, scheduling, results, shutdown, and overload are managed.**

## Next Direction

The next natural step after this lecture is to understand how executors behave under real workload conditions: queue saturation, task cancellation, `Future` cancellation, graceful shutdown, and more advanced asynchronous composition.
