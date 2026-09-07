# Java Multithreading — Lecture 10 Notes

## Executor Framework in Java

> **Core mental shift:** Do not think, “I need to create a thread.” Think, “I need to submit a task to an execution policy.”

The Executor Framework is Java’s higher-level system for submitting tasks without directly managing the threads that execute them.

```text
Task
  ↓
Executor / ExecutorService
  ↓
Work Queue
  ↓
Worker Threads
  ↓
Task Execution
  ↓
Result / Completion
```

---

# 1. Why Manual Thread Creation Is Not Ideal

A manual approach such as:

```java
Thread thread = new Thread(() -> processOrder());
thread.start();
```

makes the application responsible for:

```text
Define task
Create thread
Decide how it runs
Manage lifecycle
Coordinate completion/interruption
```

This works for small examples, but does not scale well for many independent tasks.

### Main problems

```text
1. Thread resource usage
2. Scheduling overhead
3. Context switching
4. Thread creation/destruction cost
5. No natural thread reuse
6. Uncontrolled thread growth
7. Difficult lifecycle management
```

A platform thread requires native/JVM/OS resources. Exact stack size varies by JVM, OS, and configuration, so do not memorize a fixed “1 MB per thread” rule.

Too many runnable threads can also cause excessive context switching and reduce useful CPU work.

---

# 2. Thread Pool — The Solution

Instead of:

```text
Task → create thread → execute → terminate
```

use:

```text
Tasks
  ↓
Queue
  ↓
Reusable Worker Threads
  ↓
Execute tasks
  ↓
Take more tasks
```

A thread pool reuses worker threads.

Example:

```text
Worker-1 → Task 1 → Task 5 → Task 9
Worker-2 → Task 2 → Task 6 → Task 10
```

The workers do not need to be recreated for every task.

---

# 3. What Is the Executor Framework?

The Executor Framework provides an abstraction for **submitting tasks without directly managing the worker threads**.

Without an executor:

```java
Thread thread = new Thread(() -> processOrder(order));
thread.start();
```

With an executor:

```java
executor.execute(() -> processOrder(order));
```

With an executor, the execution system can decide whether a task should:

```text
Run immediately
Wait in a queue
Use an existing worker
Cause another worker to be created
Be rejected under overload
```

The reference notes describe this as separating **task submission** from **task execution**. fileciteturn10file0L69-L92

---

# 4. Task vs Executor

| Concept | Responsibility |
|---|---|
| Task | Describes the work |
| Executor | Decides how/when the work runs |
| Thread | Low-level execution unit |
| Thread Pool | Reusable set of worker threads |

Mental model:

```text
Business Logic
     ↓
Runnable / Callable
     ↓
    TASK

Concurrency Policy
     ↓
Executor / ExecutorService
```

This separation makes application code easier to test and allows the concurrency policy to change independently. fileciteturn10file0L103-L110

---

# 5. Executor Hierarchy

```text
Executor
   ↓
ExecutorService
   ↓
AbstractExecutorService
   ↓
ThreadPoolExecutor
```

For scheduled execution:

```text
ExecutorService
   ↓
ScheduledExecutorService
   ↓
ScheduledThreadPoolExecutor
```

The reference material gives this hierarchy directly. fileciteturn10file0L178-L191

---

# 6. `Executor` Interface

The smallest abstraction is:

```java
public interface Executor {
    void execute(Runnable command);
}
```

It provides only:

```java
execute(Runnable)
```

Important point:

> `Executor` does not guarantee that a new thread will be created.

A valid executor can simply do:

```java
command.run();
```

or create a new thread, or use a thread pool. fileciteturn10file0L192-L228

---

# 7. `ExecutorService`

`ExecutorService` extends `Executor` and adds practical task-management features:

```text
Task execution
Task results
Task cancellation
Bulk operations
Lifecycle management
```

Important methods:

```java
execute(...)
submit(...)
invokeAll(...)
invokeAny(...)
shutdown()
shutdownNow()
awaitTermination(...)
```

fileciteturn10file0L229-L250

---

# 8. `execute()`

```java
executor.execute(task);
```

Properties:

```text
Accepts Runnable
Returns nothing
Does not return Future
Good for fire-and-forget tasks
```

Example:

```java
executor.execute(() -> {
    System.out.println("Sending notification");
});
```

Because there is no `Future`, the caller has no per-task Future handle for result tracking or Future-based cancellation. fileciteturn10file0L251-L267

---

# 9. `submit()`

`submit()` returns a `Future`.

### Callable

```java
Future<Integer> future =
    executor.submit(() -> 10 + 20);
```

### Runnable

```java
Future<?> future =
    executor.submit(() -> {
        System.out.println("Task completed");
    });
```

For a successfully completed `Runnable`, `future.get()` returns `null`. fileciteturn10file0L268-L276

---

# 10. `execute()` vs `submit()`

| Feature | `execute()` | `submit()` |
|---|---|---|
| Task | `Runnable` | `Runnable` / `Callable` |
| Return | Nothing | `Future` |
| Completion tracking | No direct handle | Through `Future` |
| Per-task cancellation through Future | No | Yes |
| Typical use | Fire-and-forget | Result/cancellation/completion tracking |

fileciteturn10file0L287-L301

### Easy memory trick

```text
execute() → execute and forget
submit()  → submit and track
```

---

# 11. Runnable vs Callable

## Runnable

```java
@FunctionalInterface
public interface Runnable {
    void run();
}
```

Characteristics:

```text
No return value
Cannot declare checked exceptions in run()
Works with execute()
Works with submit()
```

## Callable

```java
@FunctionalInterface
public interface Callable<V> {
    V call() throws Exception;
}
```

Characteristics:

```text
Returns a value
Can throw checked exceptions
Used with submit()
Used with invokeAll()
Used with invokeAny()
```

fileciteturn10file0L303-L332

---

# 12. Runnable vs Callable — Interview Table

| Feature | Runnable | Callable<V> |
|---|---|---|
| Method | `run()` | `call()` |
| Return value | No | Yes |
| Checked exceptions | Cannot declare | Can declare |
| `execute()` | Yes | No |
| `submit()` | Yes | Yes |

fileciteturn10file0L334-L344

---

# 13. Future

A `Future` is a handle to the **result or completion state of an asynchronous task**.

```text
Task submitted now
       ↓
Task may finish later
       ↓
Future represents pending result
```

Example:

```java
Future<Integer> future =
    executor.submit(() -> {
        Thread.sleep(1000);
        return 10 + 20;
    });

System.out.println("Main thread can do other work");

Integer result = future.get();
```

fileciteturn10file0L345-L362

---

# 14. Important Future Methods

```java
future.get();
future.get(timeout, unit);
future.isDone();
future.isCancelled();
future.cancel(true);
```

| Method | Purpose |
|---|---|
| `get()` | Wait for completion and return result |
| `get(timeout, unit)` | Wait only for specified duration |
| `isDone()` | Check whether task has completed |
| `isCancelled()` | Check whether task was cancelled |
| `cancel(true)` | Request cancellation and may interrupt a running task |

fileciteturn10file0L367-L379

---

# 15. `Future.get()` Is Blocking

```java
Integer result = future.get();
```

If the result is already available:

```text
returns immediately
```

If the result is not ready:

```text
calling thread waits
```

So:

```text
submit() → asynchronous submission
get()    → may block caller
```

The reference notes explicitly state that calling `get()` immediately can make the calling code wait. fileciteturn10file0L380-L388

---

# 16. Timed `Future.get()`

To avoid waiting forever:

```java
try {
    Integer result =
        future.get(2, TimeUnit.SECONDS);

    System.out.println(result);

} catch (TimeoutException exception) {
    System.out.println("Task did not finish in time");
}
```

This limits how long the caller waits. fileciteturn10file0L393-L400

---

# 17. Cancelling a Future

```java
boolean cancelled = future.cancel(true);
```

`true` means that if the task is already running, the executor may interrupt the worker thread.

### Critical point

> **Cancellation is cooperative.**

`cancel(true)` does not forcibly terminate arbitrary Java code.

A task should respond to interruption appropriately. fileciteturn10file0L402-L413

Example:

```java
Callable<Void> task = () -> {
    while (!Thread.currentThread().isInterrupted()) {
        // Perform work
    }
    return null;
};
```

---

# 18. Exception Handling With `execute()`

Example:

```java
executor.execute(() -> {
    int result = 10 / 0;
});
```

The exception is not returned to the submitting thread through a `Future`.

It is handled through the worker thread's uncaught-exception mechanism. A failed worker may terminate and the pool can create a replacement when required. fileciteturn10file0L418-L435

### Controlled handling

```java
executor.execute(() -> {
    try {
        int result = 10 / 0;
    } catch (ArithmeticException exception) {
        System.out.println(
            "Task failed: " + exception.getMessage()
        );
    }
});
```

---

# 19. Exception Handling With `submit()`

Example:

```java
Future<Integer> future =
    executor.submit(() -> 10 / 0);
```

The exception is captured by the `Future`.

Then:

```java
try {
    Integer result = future.get();
} catch (ExecutionException exception) {
    Throwable actualCause = exception.getCause();
}
```

The original task exception is available through:

```java
exception.getCause();
```

If the Future is ignored and `get()` is never called, the task failure can be easy to miss. fileciteturn10file0L436-L456

---

# 20. Why Thread Pools?

A thread pool provides:

```text
Reusable workers
Controlled concurrency
Task queue
Resource limits
Worker lifecycle management
```

This is the main scalability advantage over manually creating a thread for every task.

---

# 21. Fixed Thread Pool

Create one with:

```java
ExecutorService executor =
    Executors.newFixedThreadPool(2);
```

If five tasks are submitted:

```text
2 workers
5 tasks
```

At most two tasks execute concurrently, while remaining tasks wait in the queue.

The uploaded reference code uses this exact pattern with five tasks and two workers. fileciteturn9file4L4-L21

### Benefits

```text
Fixed worker count
Predictable concurrency
Worker reuse
Simple configuration
```

---

# 22. Worker Reuse

A worker follows a lifecycle like:

```text
Create worker
     ↓
Execute task
     ↓
Take another queued task
     ↓
Become idle
     ↓
Reuse or terminate according to rules
```

A worker is not destroyed after every task. Worker reuse is one of the main benefits of a pool.

---

# 23. Work Queue

A thread pool normally contains a queue for tasks that cannot execute immediately.

```text
Submitted task
      ↓
Available worker?
   /          \
 YES            NO
  ↓              ↓
execute        queue
```

The queue prevents the application from creating a new thread for every incoming task.

But an unbounded queue can still grow continuously if tasks arrive faster than workers complete them. fileciteturn10file0L153-L162

---

# 24. ThreadPoolExecutor

`ThreadPoolExecutor` is the configurable concrete thread-pool engine.

`Executors` is the convenience utility class containing factory methods.

```text
Executors
→ convenience factories

ThreadPoolExecutor
→ configurable execution engine
```

The reference notes emphasize this distinction. fileciteturn10file0L575-L592

---

# 25. `ThreadPoolExecutor` Constructor

A commonly used constructor is:

```java
ThreadPoolExecutor executor =
    new ThreadPoolExecutor(
        corePoolSize,
        maximumPoolSize,
        keepAliveTime,
        timeUnit,
        workQueue,
        threadFactory,
        rejectionHandler
    );
```

Important parameters:

```text
corePoolSize
maximumPoolSize
keepAliveTime
TimeUnit
workQueue
ThreadFactory
RejectedExecutionHandler
```

fileciteturn10file0L593-L617

---

# 26. `corePoolSize`

Example:

```java
corePoolSize = 2;
```

When a new task arrives and fewer than two workers exist, the pool normally creates another worker.

### Important detail

Core threads are not necessarily created when the executor is constructed.

By default they are started as tasks arrive.

They can be prestarted:

```java
executor.prestartCoreThread();
executor.prestartAllCoreThreads();
```

Core workers are normally retained when idle unless core-thread timeout is explicitly enabled. fileciteturn10file0L618-L630

---

# 27. `maximumPoolSize`

Example:

```java
maximumPoolSize = 5;
```

This is the maximum number of workers the pool may contain.

### Very important

The pool does not immediately create five threads.

Extra threads beyond `corePoolSize` are created only when:

```text
All core workers are occupied
AND
Queue cannot accept another task
AND
Current workers < maximumPoolSize
```

fileciteturn10file0L631-L645

---

# 28. `keepAliveTime`

Example:

```java
keepAliveTime = 30;
TimeUnit unit = TimeUnit.SECONDS;
```

When workers exceed `corePoolSize`, an excess worker can be removed after remaining idle for the keep-alive duration.

```text
Core workers
→ normally retained

Extra workers
→ removed after idle timeout
```

Core timeout can also be enabled:

```java
executor.allowCoreThreadTimeOut(true);
```

fileciteturn10file0L656-L667

---

# 29. The Most Important `ThreadPoolExecutor` Decision Flow

For a typical configuration:

```text
New task submitted
        ↓
worker count < corePoolSize?
     /            \
   YES             NO
    ↓               ↓
create worker   queue accepts?
and run task      /      \
                 YES       NO
                  ↓         ↓
                queue    worker < max?
                           /       \
                         YES        NO
                          ↓          ↓
                    extra worker   reject
```

### Golden rule

> **Core threads first → queue second → extra threads up to maximum → rejection.**

fileciteturn10file0L668-L681

---

# 30. Example — Core 2, Max 4, Queue 2

Configuration:

```text
corePoolSize = 2
maximumPoolSize = 4
queueCapacity = 2
```

Assume all tasks stay busy long enough for all submissions to arrive.

| Task | Result |
|---|---|
| Task 1 | Creates worker T1 |
| Task 2 | Creates worker T2 |
| Task 3 | Added to queue |
| Task 4 | Added to queue |
| Task 5 | Queue full → creates T3 |
| Task 6 | Queue full → creates T4 |
| Task 7 | Max workers + full queue → rejected |

State before any task completes:

```text
Workers: T1 T2 T3 T4
Queue:   Task 3 Task 4
```

fileciteturn10file0L682-L701

---

# 31. The Unbounded Queue Trap

Suppose:

```text
corePoolSize = 2
maximumPoolSize = 5
```

and the queue is unbounded.

Because the queue never becomes full:

```text
tasks keep entering queue
→ pool normally remains at core size
→ maximumPoolSize may never be used
```

This is a **very common interview point**. fileciteturn10file0L640-L655

---

# 32. Queue Types

## `ArrayBlockingQueue`

```java
new ArrayBlockingQueue<>(100)
```

Characteristics:

```text
Bounded
Array-backed
Fixed capacity
Predictable overload behavior
```

A bounded queue is useful when memory and overload need explicit limits. fileciteturn10file0L762-L772

## `LinkedBlockingQueue`

```java
new LinkedBlockingQueue<>();
```

Characteristics:

```text
Can be bounded
Effectively unbounded if capacity is not supplied
Commonly used by fixed thread pools
Can accumulate a large backlog
```

fileciteturn10file0L773-L786

## `SynchronousQueue`

```java
new SynchronousQueue<>();
```

Characteristics:

```text
No internal storage capacity
Direct handoff to worker
Associated with cached thread pool behavior
```

fileciteturn10file0L787-L796

---

# 33. Fixed Thread Pool Internals

Conceptually:

```java
Executors.newFixedThreadPool(3)
```

uses:

```text
corePoolSize = 3
maximumPoolSize = 3
unbounded LinkedBlockingQueue
```

Characteristics:

```text
Fixed number of workers
Queued tasks wait
Threads are reused
No guaranteed completion order
Queue can grow during sustained overload
```

fileciteturn10file0L797-L817

---

# 34. Cached Thread Pool

```java
Executors.newCachedThreadPool();
```

Conceptually:

```text
corePoolSize = 0
maximumPoolSize = very large
workQueue = SynchronousQueue
keepAliveTime ≈ 60 seconds
```

Characteristics:

```text
Creates workers on demand
Reuses idle workers
Removes idle workers after keep-alive
Uses direct handoff
Can create a very large number of threads
```

Suitable for many short-lived tasks, but risky for rapid streams of long-running/blocking tasks because the thread count can grow substantially. fileciteturn10file0L818-L840

---

# 35. Single-Thread Executor

```java
ExecutorService executor =
    Executors.newSingleThreadExecutor();
```

Characteristics:

```text
One worker
Sequential task execution
Internal queue
Automatic worker replacement when necessary
Worker reuse
```

Execution:

```text
Task 1 → Task 2 → Task 3
```

Useful for:

```text
Ordered event processing
Serial file writing
Single-consumer workflows
Resource confinement
```

Its queue is unbounded, so a slow worker can create a large backlog. fileciteturn10file0L841-L864

---

# 36. Scheduled Executor

```java
ScheduledExecutorService scheduler =
    Executors.newScheduledThreadPool(2);
```

Supports:

```text
One-time delayed tasks
Fixed-rate periodic tasks
Fixed-delay periodic tasks
```

It is generally preferred over `Timer` for concurrent scheduled execution. fileciteturn10file0L865-L873

---

# 37. `schedule()`

```java
scheduler.schedule(
    () -> System.out.println("Executed after 2 seconds"),
    2,
    TimeUnit.SECONDS
);
```

Meaning:

```text
Submit task
   ↓
wait 2 seconds
   ↓
run once
```

The delay is relative to the submission time. fileciteturn10file0L874-L883

---

# 38. `scheduleAtFixedRate()`

```java
scheduler.scheduleAtFixedRate(
    () -> System.out.println("Running"),
    0,
    2,
    TimeUnit.SECONDS
);
```

The intended start times follow a fixed schedule:

```text
initialDelay
initialDelay + period
initialDelay + 2 × period
...
```

If one run takes longer than the period, the next execution starts late; executions of the same periodic task do not overlap with one another. fileciteturn10file0L884-L904

### Good for

```text
Regular metrics collection
Periodic polling
Regular cadence jobs
```

---

# 39. `scheduleWithFixedDelay()`

```java
scheduler.scheduleWithFixedDelay(
    () -> System.out.println("Running"),
    0,
    2,
    TimeUnit.SECONDS
);
```

The delay is measured after the previous execution finishes.

Example:

```text
Task duration = 5s
Delay = 2s

Start 1 = 0s
End 1   = 5s
Start 2 = 7s
```

fileciteturn10file0L905-L928

### Good for

```text
Cleanup
Retry work
Background jobs where a pause is needed after completion
```

---

# 40. Fixed Rate vs Fixed Delay

| Feature | Fixed Rate | Fixed Delay |
|---|---|---|
| Basis | Planned start times | Previous completion |
| Tries to maintain frequency | Yes | No |
| Waits after task finishes | Not necessarily | Yes |
| Same task overlaps itself | No | No |
| Typical use | Metrics/polling | Cleanup/retry |

fileciteturn10file0L929-L945

### Easy memory trick

```text
Fixed Rate
→ regular schedule

Fixed Delay
→ wait after previous completion
```

---

# 41. Exception in Periodic Tasks

If a periodic task throws an uncaught exception, later executions of that periodic task are suppressed.

Therefore, when the periodic job must continue after expected failures, handle those failures inside the task.

```java
scheduler.scheduleAtFixedRate(() -> {
    try {
        performScheduledWork();
    } catch (Exception exception) {
        System.out.println(
            "Scheduled task failed: " +
            exception.getMessage()
        );
    }
}, 0, 10, TimeUnit.SECONDS);
```

fileciteturn10file0L950-L964

---

# 42. Rejection Policies

A task may be rejected when:

```text
Executor is shut down
OR
Pool is at maximum size
AND
Bounded queue is full
```

The `RejectedExecutionHandler` decides what happens next. fileciteturn10file0L965-L970

---

# 43. `AbortPolicy`

Default policy:

```java
new ThreadPoolExecutor.AbortPolicy();
```

Behavior:

```text
Reject task
   ↓
throw RejectedExecutionException
```

Good when losing work silently is unacceptable. fileciteturn10file0L971-L982

---

# 44. `CallerRunsPolicy`

```java
new ThreadPoolExecutor.CallerRunsPolicy();
```

Behavior:

```text
Executor overloaded
       ↓
Submitting thread executes task
       ↓
Producer becomes slower
```

This creates a form of **natural backpressure** because the producer is temporarily busy executing the task instead of submitting more work. fileciteturn10file0L983-L993

### Easy memory trick

> **Pool is full → caller does the work.**

---

# 45. `DiscardPolicy`

```java
new ThreadPoolExecutor.DiscardPolicy();
```

Behavior:

```text
Reject task
   ↓
silently drop it
```

This can cause unnoticed data loss and should only be used when dropping work is explicitly acceptable and monitored. fileciteturn10file0L994-L998

---

# 46. `DiscardOldestPolicy`

```java
new ThreadPoolExecutor.DiscardOldestPolicy();
```

Behavior:

```text
Queue full
   ↓
Remove oldest queued task
   ↓
Retry submission of new task
```

Useful when fresh information is more valuable than stale queued work, but dangerous when every task must be processed. fileciteturn10file0L999-L1012

---

# 47. Rejection Policy Comparison

| Policy | Behavior |
|---|---|
| `AbortPolicy` | Throws `RejectedExecutionException` |
| `CallerRunsPolicy` | Caller executes the task |
| `DiscardPolicy` | Silently drops the new task |
| `DiscardOldestPolicy` | Removes oldest queued task, then retries |

### Memory trick

```text
Abort
→ Fail loudly

CallerRuns
→ Caller works

Discard
→ Drop new

DiscardOldest
→ Drop old queued task
```

---

# 48. Executor Lifecycle

An executor should be shut down when it is no longer needed.

Important methods:

```java
shutdown();
shutdownNow();
awaitTermination(...);
```

Executor worker threads are normally non-daemon threads. An active executor can therefore keep the JVM running after `main()` completes. fileciteturn10file0L457-L461

---

# 49. `shutdown()`

```java
executor.shutdown();
```

Behavior:

```text
Stops accepting new tasks
Allows previously accepted tasks to complete
Initiates orderly shutdown
Returns immediately
Does not itself wait for termination
```

Submitting a task after shutdown causes `RejectedExecutionException`. fileciteturn10file0L462-L472

---

# 50. `shutdownNow()`

```java
List<Runnable> pendingTasks =
    executor.shutdownNow();
```

Behavior:

```text
Stops accepting new tasks
Removes tasks that have not started
Returns pending tasks
Attempts to interrupt running tasks
```

It does **not** guarantee immediate termination. Running tasks must cooperate with interruption. fileciteturn10file0L473-L482

---

# 51. `shutdown()` vs `shutdownNow()`

| `shutdown()` | `shutdownNow()` |
|---|---|
| Graceful | Best-effort aggressive shutdown |
| Finish accepted tasks | Attempt interruption of running tasks |
| Reject new tasks | Reject new tasks |
| Queued tasks can continue | Waiting tasks are returned |
| Normal choice | Urgent stop use case |

---

# 52. `awaitTermination()`

```java
boolean terminated =
    executor.awaitTermination(
        10,
        TimeUnit.SECONDS
    );
```

It waits for the executor to terminate for at most the specified duration.

Normally use it after:

```java
shutdown();
```

or:

```java
shutdownNow();
```

fileciteturn10file0L483-L490

---

# 53. Graceful Shutdown Pattern

```java
executor.shutdown();

try {
    if (!executor.awaitTermination(
            10,
            TimeUnit.SECONDS)) {

        executor.shutdownNow();

        if (!executor.awaitTermination(
                10,
                TimeUnit.SECONDS)) {
            System.out.println(
                "Executor did not terminate"
            );
        }
    }

} catch (InterruptedException exception) {

    executor.shutdownNow();
    Thread.currentThread().interrupt();
}
```

Mental model:

```text
Graceful shutdown
      ↓
wait
      ↓
Terminated?
  /        \
YES         NO
 |           |
done     shutdownNow()
             ↓
          wait again
```

fileciteturn10file0L491-L514

---

# 54. Preserve Interruption

Do not silently ignore `InterruptedException`.

### Bad

```java
try {
    Thread.sleep(1000);
} catch (InterruptedException exception) {
    // ignored
}
```

### Better

```java
try {
    Thread.sleep(1000);
} catch (InterruptedException exception) {
    Thread.currentThread().interrupt();
    return;
}
```

Restoring the interrupt flag allows higher-level code to observe the cancellation request. fileciteturn10file0L1078-L1097

---

# 55. `invokeAll()`

`invokeAll()` submits a collection of `Callable` tasks and waits until all of them complete.

```java
List<Callable<Integer>> tasks = List.of(
    () -> 10,
    () -> 20,
    () -> 30
);

List<Future<Integer>> futures =
    executor.invokeAll(tasks);
```

Then:

```java
for (Future<Integer> future : futures) {
    System.out.println(future.get());
}
```

Important points:

```text
Blocks until all tasks complete
Returns one Future for each task
Future list follows input order
A task may complete successfully or exceptionally
```

fileciteturn10file0L515-L539

---

# 56. `invokeAny()`

`invokeAny()` submits multiple `Callable` tasks and returns one successful result.

```java
Integer result = executor.invokeAny(tasks);
```

Once a successful result is available, unfinished tasks are cancelled.

Useful when multiple independent sources can produce an acceptable answer.

Example:

```java
List<Callable<String>> servers = List.of(
    () -> callServerA(),
    () -> callServerB(),
    () -> callServerC()
);

String response = executor.invokeAny(servers);
```

It returns the **first successful result**, not necessarily the first task that terminates. fileciteturn10file0L540-L558

---

# 57. `invokeAll()` vs `invokeAny()`

| `invokeAll()` | `invokeAny()` |
|---|---|
| Wait for all | Return one successful result |
| Returns all Futures | Returns one result |
| Useful when every result matters | Useful when any success is enough |
| Preserves input order in Future list | Chooses based on successful completion |

---

# 58. CPU-Bound vs I/O-Bound Tasks

Pool sizing depends on workload.

## CPU-Bound

Examples:

```text
Image processing
Compression
Encryption
Numerical calculations
```

Too many threads usually increase scheduling overhead without creating more CPU capacity. fileciteturn10file0L1039-L1058

## I/O-Bound

Examples:

```text
Database calls
Network calls
File operations
```

Some additional concurrency can help because threads may spend time waiting.

But the pool must still respect:

```text
Database connection limits
Remote service limits
Memory limits
Downstream capacity
```

fileciteturn10file0L1059-L1066

---

# 59. Prefer Bounded Resources in Production

A production executor should normally have explicit limits around:

```text
Queue capacity
Maximum worker count
Submission rate
Task timeout
Rejection behavior
Downstream concurrency
```

An unbounded queue can simply move overload from:

```text
Thread creation
```

to:

```text
Memory consumption
```

fileciteturn10file0L1067-L1077

---

# 60. Thread Naming

Meaningful worker names help with debugging and production logs.

Example:

```java
AtomicInteger counter = new AtomicInteger();

ThreadFactory threadFactory = task -> {
    Thread thread = new Thread(task);
    thread.setName(
        "order-worker-" +
        counter.incrementAndGet()
    );
    return thread;
};
```

Example names:

```text
order-worker-1
order-worker-2
order-worker-3
```

The reference notes demonstrate this exact `ThreadFactory` approach. fileciteturn10file0L1098-L1107

---

# 61. Monitor the Executor

Useful `ThreadPoolExecutor` metrics include:

```java
executor.getPoolSize();
executor.getActiveCount();
executor.getQueue().size();
executor.getCompletedTaskCount();
executor.getTaskCount();
executor.getLargestPoolSize();
```

These help answer:

```text
Is the queue continuously growing?
Is the pool regularly reaching maximum size?
Are tasks completing fast enough?
Is the executor frequently overloaded?
```

fileciteturn10file0L1123-L1135

---

# 62. Complete Production-Style Example

```java
ThreadPoolExecutor executor =
    new ThreadPoolExecutor(
        2,
        4,
        30,
        TimeUnit.SECONDS,
        new ArrayBlockingQueue<>(10),
        new ThreadPoolExecutor.CallerRunsPolicy()
    );

Future<String> future = executor.submit(() -> {

    System.out.println(
        "Processing order on " +
        Thread.currentThread().getName()
    );

    Thread.sleep(1000);

    return "ORDER_PROCESSED";
});

try {

    String result = future.get();
    System.out.println(result);

} catch (InterruptedException exception) {

    Thread.currentThread().interrupt();

} catch (ExecutionException exception) {

    System.out.println(
        "Order failed: " +
        exception.getCause().getMessage()
    );

} finally {

    executor.shutdown();
}
```

This combines:

```text
Explicit pool configuration
Bounded queue
Overload handling
Callable/value-returning task
Future result
Exception handling
Proper shutdown
```

The reference material presents the same overall controlled order-processing pattern. fileciteturn10file0L1136-L1196

---

# 63. Major Interview Question — Why Use Executor Framework?

### Answer

> The Executor Framework separates task submission from thread management and allows tasks to run on reusable worker threads, providing controlled concurrency, task queuing, result tracking, cancellation, lifecycle management, and scheduling.

---

# 64. Major Interview Question — `execute()` vs `submit()`

```text
execute()
→ Runnable
→ no Future
→ fire-and-forget

submit()
→ Runnable / Callable
→ Future
→ result/cancellation/completion tracking
```

---

# 65. Major Interview Question — Runnable vs Callable

```text
Runnable
→ run()
→ void
→ cannot declare checked exceptions

Callable<T>
→ call()
→ returns T
→ checked exceptions allowed
```

---

# 66. Major Interview Question — What Is a Future?

> A `Future` is a handle representing the result or completion state of an asynchronous computation.

Remember:

```text
submit()
→ Future
→ task runs
→ get()
→ result
```

---

# 67. Major Interview Question — Is `Future.get()` Blocking?

**Yes.**

If the result is not ready, the caller waits.

---

# 68. Major Interview Question — Core vs Maximum Pool Size

Most important rule:

```text
1. Fill core threads
2. Fill queue
3. Create extra workers up to maximum
4. Reject if both are full
```

Do not think:

```text
core = 2, max = 5
→ immediately create 5 threads
```

That is incorrect.

---

# 69. Major Interview Question — Why Does Queue Type Matter?

Because it affects:

```text
Memory
Latency
Maximum concurrency
Pool growth
Overload behavior
Rejection behavior
```

A queue is part of the executor's concurrency policy.

---

# 70. Major Interview Question — Why Can an Unbounded Queue Prevent Maximum Pool Growth?

Because the executor normally creates threads beyond the core size only after the queue cannot accept the task.

If the queue is effectively unbounded:

```text
queue never full
→ extra workers not needed
→ pool often remains at core size
```

---

# 71. Major Interview Question — Shutdown Methods

```text
shutdown()
→ stop new submissions
→ finish accepted tasks

shutdownNow()
→ stop new submissions
→ return waiting tasks
→ attempt interruption of running tasks

awaitTermination()
→ wait for termination
```

---

# 72. Major Interview Question — What Is Backpressure?

Backpressure means slowing or limiting producers when the execution system is overloaded.

A bounded queue + rejection policy can provide explicit overload behavior.

`CallerRunsPolicy` can create natural backpressure by making the submitting thread execute the task. fileciteturn10file0L983-L992

---

# 73. Common Beginner Mistakes

### Mistake 1

> Executor Framework means no threads are used.

❌ Wrong.

Threads are still used; the framework manages them at a higher level.

### Mistake 2

> One new thread is created for every task.

❌ Wrong for a thread-pool executor.

Workers are reused.

### Mistake 3

> `submit()` immediately gives the result.

❌ No. It gives a `Future`.

### Mistake 4

> `Future.get()` is always asynchronous.

❌ No. It may block.

### Mistake 5

> `maximumPoolSize` means the pool immediately creates that many workers.

❌ No.

Core → queue → extra workers → rejection.

### Mistake 6

> More threads always improve performance.

❌ No.

Too many threads can increase scheduling/context-switching overhead.

### Mistake 7

> `shutdownNow()` instantly kills all running tasks.

❌ No.

It is best-effort interruption.

### Mistake 8

> Ignoring `InterruptedException` is harmless.

❌ No.

Preserve the interruption signal when appropriate.

### Mistake 9

> An unbounded queue solves overload.

❌ No.

The backlog can consume increasing memory.

### Mistake 10

> Fixed-rate and fixed-delay scheduling are the same.

❌ No.

Fixed rate is schedule-oriented; fixed delay is completion-oriented.

---

# 74. Master Comparison Table

| Mechanism | Main Idea |
|---|---|
| Raw `Thread` | Low-level thread creation/management |
| `Executor` | Abstraction for executing `Runnable` |
| `ExecutorService` | Execution + lifecycle + results + bulk operations |
| `ThreadPoolExecutor` | Configurable thread-pool implementation |
| `execute()` | Submit `Runnable` without Future |
| `submit()` | Submit task and receive Future |
| `Runnable` | No result |
| `Callable<T>` | Returns result and may throw checked exceptions |
| `Future` | Tracks result/completion/cancellation |
| Fixed pool | Fixed workers + queue |
| Cached pool | On-demand workers + direct handoff |
| Single-thread executor | Sequential execution with one worker |
| Scheduled executor | Delayed and periodic execution |
| `invokeAll()` | Wait for all task results |
| `invokeAny()` | Return one successful result |
| `shutdown()` | Graceful shutdown |
| `shutdownNow()` | Best-effort interruption + return pending tasks |

---

# 75. Complete Executor Framework Mental Model

```text
                   APPLICATION
                        │
                        ↓
                      TASK
               Runnable / Callable
                        │
               execute() / submit()
                        │
                        ↓
                 ExecutorService
                        │
                        ↓
                 ThreadPoolExecutor
                        │
           ┌────────────┴────────────┐
           │                         │
      Worker Threads             Work Queue
           │                         │
       ┌───┼───┐                     │
       │   │   │                  waiting
      W1  W2  W3                   tasks
       │   │   │                     │
       └───┼───┘                     │
           │                         │
           └────── consume ──────────┘
                        │
                        ↓
                   TASK EXECUTES
                        │
                  ┌─────┴─────┐
                  │           │
               Runnable     Callable
                  │           │
                 void        result
                              │
                            Future
                              │
                            get()
                              │
                            result
```

---

# 76. Executor Framework Decision Tree

```text
Need to run a task?
       ↓
Runnable or Callable
       ↓
Need result / cancellation / completion tracking?
       │
   ┌───┴────┐
  YES      NO
   │         │
submit()   execute()
   │
 Future
```

For pool design:

```text
Need controlled workers?
        ↓
ThreadPoolExecutor / fixed pool

Need dynamic short-lived task execution?
        ↓
Cached pool (with care)

Need strict sequential task execution?
        ↓
Single-thread executor

Need delayed/periodic execution?
        ↓
Scheduled executor
```

---

# 77. Production Design Checklist

Before creating an executor, ask:

```text
1. CPU-bound or I/O-bound?
2. How many tasks can arrive?
3. How long do tasks run?
4. How much blocking occurs?
5. How many concurrent tasks can downstream services support?
6. What should the queue capacity be?
7. What should maximum worker count be?
8. What should happen under overload?
9. How will tasks be cancelled?
10. How will the executor be shut down?
11. Should worker threads have meaningful names?
12. What metrics will be monitored?
```

---

# 78. ⭐ Lecture 10 — Must Remember

```text
1. Do not create one thread per task blindly.

2. Threads have memory and scheduling overhead.

3. Thread pools reuse worker threads.

4. Executor separates task submission from execution policy.

5. Executor is an abstraction; it does not necessarily create a new thread.

6. ExecutorService adds lifecycle, results, cancellation, and bulk operations.

7. execute() → Runnable, no Future.

8. submit() → Runnable/Callable, returns Future.

9. Runnable → no result.

10. Callable<T> → result + checked exceptions.

11. Future.get() can block.

12. future.cancel(true) is cooperative interruption, not forced termination.

13. ThreadPoolExecutor is the configurable thread-pool engine.

14. corePoolSize = core worker count.

15. maximumPoolSize = upper worker limit under the pool rules.

16. keepAliveTime = idle timeout for eligible excess workers.

17. Queue choice strongly affects behavior.

18. Core threads are created as work arrives by default.

19. Typical flow:
    core workers → queue → extra workers → rejection.

20. An unbounded queue can prevent growth beyond corePoolSize.

21. Fixed pool → fixed workers + unbounded queue.

22. Cached pool → on-demand workers + SynchronousQueue.

23. Single-thread executor → sequential execution.

24. Scheduled executor → delayed/periodic execution.

25. Fixed rate uses planned schedule.

26. Fixed delay waits after completion.

27. Periodic tasks with uncaught exceptions stop future executions.

28. AbortPolicy → throw exception.

29. CallerRunsPolicy → caller executes task.

30. DiscardPolicy → silently drop new task.

31. DiscardOldestPolicy → remove oldest queued task and retry.

32. shutdown() → orderly shutdown.

33. shutdownNow() → best-effort interruption + return pending tasks.

34. awaitTermination() → wait for termination.

35. Preserve interruption correctly.

36. Prefer bounded resources when overload must be controlled.

37. More threads do not automatically mean more performance.

38. CPU-bound and I/O-bound workloads need different pool-sizing strategies.

39. Name worker threads for debugging.

40. Monitor pool metrics for queue growth and overload.
```

---

# 🔥 One-Minute Revision

> **The Executor Framework is a higher-level Java concurrency system that separates task submission from thread management. Instead of creating one thread for every task, we submit `Runnable` or `Callable` tasks to an `ExecutorService`, which can use reusable worker threads and a work queue. `execute()` accepts a `Runnable` and returns nothing, while `submit()` can accept `Runnable` or `Callable` and returns a `Future`. `Callable` can return a value and throw checked exceptions. `Future.get()` retrieves the result but can block. `ThreadPoolExecutor` provides detailed control through core pool size, maximum pool size, keep-alive time, work queue, thread factory, and rejection handler. The key pool decision order is core threads first, then queue, then extra workers up to maximum, then rejection. Fixed, cached, single-thread, and scheduled executors serve different workload patterns. `shutdown()` provides orderly shutdown, `shutdownNow()` attempts interruption, and `awaitTermination()` can wait for termination. In production, queue capacity, worker limits, rejection behavior, interruption handling, downstream limits, thread naming, and monitoring must be deliberate design decisions.**

---

# 📌 Final Takeaway

The Executor Framework is **not simply a shorter way to create threads**.

It provides a complete task-execution system with:

```text
Thread Reuse
Controlled Concurrency
Task Queuing
Asynchronous Results
Cancellation
Lifecycle Management
Scheduling
Overload Handling
```

The most important mental shift is:

```text
OLD THINKING
"I need to create a thread."

NEW THINKING
"I need to submit a task to an execution policy."
```

This is the core foundation of using Java's concurrency utilities effectively.
