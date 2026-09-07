# Java Multithreading — Lecture 11 Notes

## CompletableFuture, ForkJoinPool, ThreadLocal & Virtual Threads

This lecture introduces four important advanced concurrency tools and emphasizes that each solves a different problem:

```text
Future
→ represents the eventual result of one asynchronous task

CompletableFuture
→ builds an asynchronous completion pipeline

ForkJoinPool
→ parallelizes divisible CPU-bound computations

ThreadLocal
→ stores separate contextual state for each thread

Virtual Threads
→ make very large numbers of blocking tasks practical
```

> **Core principle:** Choose the concurrency tool according to the workload and problem, not simply because an API is newer.

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

### Lecture 8–9

```text
Atomic variables
CAS
Lock-free concurrency
ABA problem
```

### Lecture 10

```text
Executor Framework
Thread pools
Future
Callable
ThreadPoolExecutor
```

### Lecture 11

```text
CompletableFuture
ForkJoinPool
ThreadLocal
Virtual Threads
```

---

# PART 1 — FUTURE REVISION

# 2. What Does a `Future` Represent?

A:

```java
Future<T>
```

represents the pending result of an asynchronous task.

Example:

```java
Future<Integer> future = executor.submit(() -> {
    return 50;
});
```

The task may still be running when `submit()` returns.

The `Future` gives the caller a way to:

```text
wait for result
check whether the task has completed
cancel the task
observe an exception raised by the task
```

The reference PDF introduces `Future` as the placeholder for a result that may become available later. fileciteturn11file0L14-L36

---

# 3. Calling `get()` Immediately

Consider:

```java
Future<Integer> future = executor.submit(task);
Integer result = future.get();
```

The task is submitted asynchronously, but the caller immediately waits.

So from the caller's perspective:

```text
submit()
   ↓
async work
   ↓
get()
   ↓
caller may block
```

A better pattern is often:

```java
Future<Integer> future = executor.submit(task);

// independent work here

Integer result = future.get();
```

The reference material explicitly explains that the benefit comes from allowing useful work to continue before the result is actually needed. fileciteturn11file0L72-L89

---

# 4. Important `Future` Methods

```java
future.get();
future.get(timeout, unit);
future.isDone();
future.isCancelled();
future.cancel(true);
```

| Method | Purpose |
|---|---|
| `get()` | Wait for completion and return the result |
| `get(timeout, unit)` | Wait only for a limited duration |
| `isDone()` | Non-blocking check for terminal completion |
| `isCancelled()` | Check whether cancellation occurred |
| `cancel(true)` | Request cancellation and possible interruption |

The reference material also notes that `isDone() == true` can mean successful completion, exceptional completion, or cancellation. fileciteturn11file0L128-L149

---

# 5. Why Basic `Future` Is Limited

A normal `Future` mainly lets us:

```text
wait
poll
cancel
retrieve result
```

It does not directly provide a fluent way to say:

```text
Task completes
   ↓
transform result
   ↓
run another operation
   ↓
combine another result
   ↓
handle failure
```

That is the problem `CompletableFuture` addresses. fileciteturn11file0L182-L200

---

# PART 2 — COMPLETABLEFUTURE

# 6. What Is `CompletableFuture`?

```java
CompletableFuture<T>
```

implements:

```text
Future<T>
CompletionStage<T>
```

Therefore it has two roles:

```text
Future
→ pending result

CompletionStage
→ one stage in a larger asynchronous pipeline
```

One stage can automatically trigger another stage when it completes. fileciteturn11file0L201-L220

---

# 7. Completion Pipeline Mental Model

Think:

```text
Fetch data
    ↓
Transform data
    ↓
Validate data
    ↓
Save data
    ↓
Send response
```

The relationship between these stages can be declared instead of manually blocking between every step.

---

# 8. `runAsync()`

Use `runAsync()` when the asynchronous task returns no value.

```java
CompletableFuture<Void> future =
    CompletableFuture.runAsync(() -> {
        System.out.println("Task running");
    });
```

Mental model:

```text
runAsync()
→ run an action
→ no result
```

The reference material identifies `Runnable` as the input and `CompletableFuture<Void>` as the result type. fileciteturn11file0L224-L245

---

# 9. `supplyAsync()`

Use `supplyAsync()` when the task returns a value.

```java
CompletableFuture<Integer> future =
    CompletableFuture.supplyAsync(() -> 10);
```

Mental model:

```text
supplyAsync()
→ supply a result
```

It uses a `Supplier<T>`.

---

# 10. `runAsync()` vs `supplyAsync()`

| Method | Functional interface | Result |
|---|---|---|
| `runAsync()` | `Runnable` | No value |
| `supplyAsync()` | `Supplier<T>` | Returns `T` |

---

# 11. Default Executor

When no executor is supplied:

```java
CompletableFuture.supplyAsync(() -> 10);
```

asynchronous execution normally uses:

```java
ForkJoinPool.commonPool()
```

A custom executor can be supplied:

```java
ExecutorService executor =
    Executors.newFixedThreadPool(4);

CompletableFuture<Integer> future =
    CompletableFuture.supplyAsync(
        () -> 10,
        executor
    );
```

A custom executor can be useful for:

```text
specific concurrency limits
blocking I/O
thread naming/monitoring
workload isolation
avoiding shared common-pool contention
```

fileciteturn11file0L249-L269

---

# 12. `thenApply()` — Transform a Result

Use:

```java
thenApply()
```

when you want to transform the previous result.

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
× 2
 ↓
20
```

It can also transform the type:

```java
CompletableFuture<String> future =
    CompletableFuture
        .supplyAsync(() -> 10)
        .thenApply(value -> "Result: " + value);
```

---

# 13. `thenAccept()` — Consume a Result

`thenAccept()` receives the previous result but does not produce another value.

```java
CompletableFuture<Void> future =
    CompletableFuture
        .supplyAsync(() -> 10)
        .thenAccept(result -> {
            System.out.println(result);
        });
```

Typical uses:

```text
printing
saving
sending notifications
writing to external systems
```

fileciteturn11file0L294-L315

---

# 14. `thenRun()` — Run After Completion

`thenRun()` does not receive the previous result.

```java
CompletableFuture<Void> future =
    CompletableFuture
        .supplyAsync(() -> 10)
        .thenRun(() -> {
            System.out.println("Pipeline completed");
        });
```

Use it when only completion matters. fileciteturn11file0L316-L324

---

# 15. `thenApply()` vs `thenAccept()` vs `thenRun()`

| Method | Receives result? | Produces result? | Main purpose |
|---|---:|---:|---|
| `thenApply()` | Yes | Yes | Transform |
| `thenAccept()` | Yes | No | Consume |
| `thenRun()` | No | No | Run after completion |

### Easy memory trick

```text
thenApply
→ Apply a transformation

thenAccept
→ Accept/consume the result

thenRun
→ Run something afterward
```

fileciteturn11file0L325-L339

---

# 16. `thenApply()` vs `thenApplyAsync()`

This is a common interview topic.

### `thenApply()`

```java
future.thenApply(value -> value * 2);
```

A non-async continuation does **not** promise a new thread.

It may run:

```text
in the thread that completes the previous stage
```

or:

```text
in the thread attaching the continuation
if the stage is already complete
```

Therefore this statement is too simplistic:

> `thenApply()` always runs in the same thread.

### `thenApplyAsync()`

```java
future.thenApplyAsync(value -> value * 2);
```

The async version schedules the continuation using the default asynchronous executor.

A custom executor can be given:

```java
future.thenApplyAsync(
    value -> value * 2,
    executor
);
```

fileciteturn11file0L340-L370

---

# 17. Same Async Pattern for Other Methods

You also have:

```text
thenAccept()
thenAcceptAsync()

thenRun()
thenRunAsync()
```

General mental model:

```text
Non-Async
→ no guarantee of a separate/new thread

Async
→ continuation is scheduled asynchronously
```

---

# 18. `thenCombine()` — Combine Independent Results

Use `thenCombine()` when two independent futures each produce a result.

```java
CompletableFuture<Integer> first =
    CompletableFuture.supplyAsync(() -> 10);

CompletableFuture<Integer> second =
    CompletableFuture.supplyAsync(() -> 20);

CompletableFuture<Integer> combined =
    first.thenCombine(
        second,
        (left, right) -> left + right
    );
```

Result:

```text
30
```

Flow:

```text
First  → 10 ──┐
              ├── combine → 30
Second → 20 ──┘
```

The combining function runs after both stages complete successfully. fileciteturn11file0L377-L418

---

# 19. Real-World `thenCombine()` Example

Suppose user details and order details are independent:

```java
CompletableFuture<String> userFuture =
    CompletableFuture.supplyAsync(
        () -> fetchUser()
    );

CompletableFuture<String> orderFuture =
    CompletableFuture.supplyAsync(
        () -> fetchOrder()
    );

CompletableFuture<String> responseFuture =
    userFuture.thenCombine(
        orderFuture,
        (user, order) ->
            user + " | " + order
    );
```

Both independent operations can run concurrently.

---

# 20. `exceptionally()` — Recovery

Use it to recover from failure with a fallback value.

```java
CompletableFuture<Integer> future =
    CompletableFuture
        .supplyAsync(() -> {
            throw new RuntimeException(
                "Service unavailable"
            );
        })
        .exceptionally(exception -> {
            return 0;
        });
```

Mental model:

```text
Failure
  ↓
Fallback
```

fileciteturn11file0L419-L435

---

# 21. `whenComplete()` — Observe Outcome

`whenComplete()` observes success or failure without normally replacing the result.

```java
CompletableFuture<Integer> future =
    CompletableFuture
        .supplyAsync(() -> 10)
        .whenComplete((result, exception) -> {

            if (exception == null) {
                System.out.println(
                    "Completed: " + result
                );
            } else {
                System.out.println(
                    "Failed: " + exception.getMessage()
                );
            }
        });
```

Useful for:

```text
logging
metrics
cleanup
observing final outcome
```

fileciteturn11file0L436-L460

---

# 22. `handle()` — Success + Failure

`handle()` receives both:

```text
result
exception
```

and can create a new result for either case.

```java
CompletableFuture<String> future =
    CompletableFuture
        .supplyAsync(() -> 10 / 0)
        .handle((result, exception) -> {

            if (exception != null) {
                return "Fallback response";
            }

            return "Result: " + result;
        });
```

---

# 23. `exceptionally()` vs `whenComplete()` vs `handle()`

| Method | Success | Failure | Can transform result? |
|---|---:|---:|---:|
| `exceptionally()` | Recovery-oriented | Yes | Yes |
| `whenComplete()` | Yes | Yes | Normally observes only |
| `handle()` | Yes | Yes | Yes |

### Easy memory trick

```text
exceptionally
→ fallback after failure

whenComplete
→ observe what happened

handle
→ inspect result/exception and produce a new result
```

fileciteturn11file0L461-L480

---

# 24. `get()` vs `join()`

A `CompletableFuture` can still be waited on:

```java
future.get();
```

or:

```java
future.join();
```

Both can block when the result is not ready.

Main difference:

```text
get()
→ ExecutionException
→ checked-exception handling required

join()
→ CompletionException
→ unchecked
```

fileciteturn11file0L481-L492

---

# 25. Avoid Blocking Between Every Stage

This:

```java
Integer firstResult = firstFuture.get();
Integer secondResult = secondFuture.get();
Integer total = firstResult + secondResult;
```

introduces blocking into the composition.

Prefer:

```java
CompletableFuture<Integer> totalFuture =
    firstFuture.thenCombine(
        secondFuture,
        Integer::sum
    );
```

This lets the completion relationship be expressed directly. fileciteturn11file0L506-L517

---

# 26. Complete Pipeline

```java
CompletableFuture<Void> pipeline =
    CompletableFuture
        .supplyAsync(() -> "User Data")
        .thenApply(data ->
            data + " processed"
        )
        .thenAccept(result ->
            System.out.println(result)
        );
```

Flow:

```text
Load data
   ↓
Transform
   ↓
Consume
```

The submitting thread does not need to block between every stage. fileciteturn11file0L518-L537

---

# 27. Asynchronous ≠ Non-Blocking

This is a very important distinction.

Example:

```java
CompletableFuture.supplyAsync(() -> {
    return blockingDatabaseCall();
});
```

This is asynchronous from the caller's perspective.

But the worker thread may still spend time blocked during the database call.

Therefore:

```text
Asynchronous
→ caller does not execute the work directly

Non-blocking
→ executing thread is not parked waiting for the operation
```

These are different ideas. fileciteturn11file0L539-L552

---

# 28. Common Pool + Blocking Work

The common ForkJoin pool is shared by different APIs and components.

Long blocking operations can occupy its workers and reduce progress for unrelated tasks.

For blocking I/O, consider:

```text
dedicated executor
bounded I/O pool
virtual threads
```

fileciteturn11file0L553-L561

---

# PART 3 — FORKJOINPOOL

# 29. What Problem Does `ForkJoinPool` Solve?

A normal thread pool works well for independent tasks.

Some computations are naturally recursive:

```text
Large problem
    ↓
Split into smaller problems
    ↓
Split again
    ↓
Solve smaller problems
    ↓
Combine results
```

This is:

> **Divide-and-Conquer Parallelism**

Examples:

```text
large-array sum
merge sort
recursive search
image processing
tree traversal
mathematical divide-and-conquer
```

`ForkJoinPool` is designed for this pattern. fileciteturn11file0L569-L596

---

# 30. Divide-and-Conquer Strategy

The standard strategy is:

```text
1. Check whether the problem is small enough.
2. If yes → solve directly.
3. Otherwise → split into subtasks.
4. Execute subtasks in parallel.
5. Combine results.
```

---

# 31. Threshold / Base Case

You need a threshold:

```text
size <= threshold
→ solve directly

size > threshold
→ split again
```

Why?

If you split too aggressively:

```text
many tiny tasks
→ task-management overhead
→ poor performance
```

If the threshold is too large:

```text
less parallelism
```

So:

> **Threshold is a performance decision.** fileciteturn11file0L607-L613

---

# 32. `fork()`

```java
leftTask.fork();
```

Forking schedules a subtask for asynchronous execution.

Mental model:

```text
Current task
   ↓
fork
   ↓
subtask can execute separately
```

---

# 33. `join()`

```java
Integer leftResult =
    leftTask.join();
```

Joining waits for a subtask and obtains its result.

So:

```text
fork()
→ schedule subtask

join()
→ wait for result
```

fileciteturn11file0L614-L624

---

# 34. Work Stealing

ForkJoinPool workers maintain **local work queues**.

Conceptually:

```text
Worker A → Queue A
Worker B → Queue B
Worker C → Queue C
```

A worker normally processes work from its own queue.

If a worker becomes idle:

```text
Worker B
   ↓
checks another worker's queue
   ↓
steals available work
   ↓
continues processing
```

This is called:

> **Work stealing**

It helps balance irregular recursive workloads. fileciteturn11file0L625-L652

---

# 35. Conceptual ForkJoinPool Flow

```text
Main task submitted
        ↓
Worker starts computation
        ↓
Task splits into subtasks
        ↓
Subtasks enter worker-local queues
        ↓
Idle workers steal work
        ↓
Small tasks solved directly
        ↓
Partial results joined
        ↓
Final result returned
```

fileciteturn12file0L16-L23

---

# 36. `RecursiveTask<V>`

Use when the computation returns a result.

```java
class SumTask extends RecursiveTask<Integer> {

    @Override
    protected Integer compute() {
        return 10;
    }
}
```

---

# 37. `RecursiveAction`

Use when the computation does not return a result.

```java
class PrintTask extends RecursiveAction {

    @Override
    protected void compute() {
        System.out.println("Processing");
    }
}
```

---

# 38. `RecursiveTask` vs `RecursiveAction`

| Type | Returns value? |
|---|---:|
| `RecursiveTask<V>` | Yes |
| `RecursiveAction` | No |

Both are based on:

```text
ForkJoinTask
```

fileciteturn11file0L672-L696

---

# 39. Parallel Array Sum Example

Suppose:

```java
int[] numbers = {
    1, 2, 3, 4,
    5, 6, 7, 8
};
```

Split conceptually:

```text
[1 2 3 4 5 6 7 8]
          ↓
       split
       /   \
[1 2 3 4] [5 6 7 8]
    ↓           ↓
  split       split
    ↓           ↓
 small        small
 tasks        tasks
    \           /
       combine
          ↓
         36
```

The reference example uses threshold `2` and produces:

```text
Final sum: 36
```

fileciteturn11file0L697-L773

---

# 40. Standard Fork-Join Pattern

The reference example uses:

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
1. forks one subtask
2. computes the other locally
3. joins the forked task
4. combines results
```

This generally reduces unnecessary scheduling overhead compared with immediately forking both sides. fileciteturn11file0L786-L806

---

# 41. `invoke()`, `fork()`, `join()`, `compute()`

| Method | Purpose |
|---|---|
| `pool.invoke(task)` | Submit top-level task and wait for its result |
| `task.fork()` | Schedule a subtask asynchronously |
| `task.join()` | Wait for a subtask and get its result |
| `task.compute()` | Execute task logic directly in current worker |

fileciteturn11file0L807-L812

---

# 42. Common ForkJoinPool

Java maintains:

```java
ForkJoinPool.commonPool()
```

It is used by APIs such as:

```text
many CompletableFuture async methods
parallel streams
some ForkJoinTask usage
```

Because it is shared:

> Long-running blocking work should not occupy the common pool carelessly. fileciteturn11file0L813-L820

---

# 43. `newWorkStealingPool()`

Java also provides:

```java
Executors.newWorkStealingPool();
```

or:

```java
Executors.newWorkStealingPool(4);
```

This creates a ForkJoinPool-oriented work-stealing executor.

It is useful for:

```text
many small independent tasks
recursively generated tasks
```

It does not guarantee task execution order. fileciteturn11file0L825-L834

---

# 44. When ForkJoinPool Works Well

Good fit:

```text
CPU-bound
independent subtasks
recursive/naturally decomposable
many relatively small computations
partial results can be combined
```

Examples:

```text
recursive algorithms
array computations
tree processing
parallel transformations
parallel streams
```

fileciteturn11file0L835-L847

---

# 45. When ForkJoinPool Is Not the Best Default

Usually avoid using it as the universal executor for tasks that:

```text
spend most of their time waiting on network I/O
block on database calls
hold locks for long durations
depend heavily on task ordering
cannot be divided into independent computations
```

Blocking a worker can reduce the pool's ability to process other tasks.

For blocking workloads, a dedicated executor or virtual threads may be more appropriate. fileciteturn11file0L851-L861

---

# PART 4 — THREADLOCAL

# 46. What Problem Does `ThreadLocal` Solve?

A:

```java
ThreadLocal<T>
```

associates a separate value with each thread.

Mental model:

```text
One ThreadLocal
      │
 ┌────┼────┐
 ↓    ↓    ↓
T1   T2   T3
A    B    C
```

Every thread gets its own stored value.

---

# 47. Basic `ThreadLocal` Example

```java
private static final ThreadLocal<String> USER_NAME =
    new ThreadLocal<>();
```

Thread-1:

```java
USER_NAME.set("Harshita");
```

Thread-2:

```java
USER_NAME.set("Aditya");
```

Possible output:

```text
Thread-1 -> Harshita
Thread-2 -> Aditya
```

The reference example demonstrates the same ThreadLocal object holding different values for different threads. fileciteturn11file0L862-L921

---

# 48. Important `ThreadLocal` Methods

```java
set(value)
get()
remove()
```

Recommended pattern:

```java
CONTEXT.set(value);

try {
    performWork();
} finally {
    CONTEXT.remove();
}
```

---

# 49. Why Not Always Use Local Variables?

A local variable is usually best when the value can simply be passed explicitly:

```java
void processRequest(String userName) {
    validate(userName);
    save(userName);
}
```

`ThreadLocal` becomes useful when contextual information must be available across several method calls without passing it through every method.

Example flow:

```text
Controller
   ↓
Service
   ↓
Repository
```

Possible contextual values:

```text
request ID
tracing information
security context
transaction context
tenant information
```

The downside is hidden data flow, so explicit parameters are often easier to understand when practical. fileciteturn11file0L922-L945

---

# 50. ThreadLocal + Thread Pools — VERY IMPORTANT

Thread pools reuse worker threads.

Example:

```text
Task A
  ↓
Worker-1
  ↓
Task A completes
  ↓
Worker-1 reused
  ↓
Task B
```

If Task A leaves a ThreadLocal value behind:

```text
Task B
→ may observe stale data
```

Therefore:

```java
CONTEXT.set(value);

try {
    performWork();
} finally {
    CONTEXT.remove();
}
```

This cleanup rule is extremely important in server applications. fileciteturn11file0L946-L961

---

# 51. ThreadLocal + Virtual Threads

Virtual threads support ThreadLocal values.

However, applications may create huge numbers of virtual threads.

Therefore:

```text
large ThreadLocal values
→ potentially large memory consumption
```

The reference material recommends:

```text
avoid large ThreadLocal objects
remove values when no longer needed
do not use ThreadLocal as a resource pool
prefer explicit parameters when practical
```

fileciteturn11file0L963-L970

---

# PART 5 — VIRTUAL THREADS

# 52. Platform Threads

Traditional Java threads are called:

> **Platform threads**

Conceptually:

```text
Java Platform Thread
        ↓
OS Thread
        ↓
CPU
```

Platform threads are relatively expensive resources.

Very high numbers can cause:

```text
memory consumption
thread creation overhead
context switching
OS thread limits
```

---

# 53. Why Blocking Applications Need Many Threads

Consider:

```text
Receive request
     ↓
Call database
     ↓
Wait
     ↓
Call another API
     ↓
Wait
     ↓
Return result
```

The thread may spend most of its lifetime waiting rather than using CPU.

With platform threads, high concurrency therefore requires many OS-backed threads just to represent all those waiting requests. fileciteturn11file0L971-L1006

---

# 54. What Is a Virtual Thread?

A virtual thread is a lightweight Java thread scheduled by the Java runtime rather than directly by the operating system.

Mental model:

```text
Many Virtual Threads
        ↓
JVM Scheduler
        ↓
Carrier Platform Threads
        ↓
OS / CPU
```

A platform thread temporarily running a virtual thread is called its:

> **Carrier thread**

fileciteturn11file0L1007-L1020

---

# 55. Mounting and Unmounting

When a virtual thread is ready:

```text
Virtual thread ready
      ↓
Mounted on carrier
      ↓
Executes Java code
```

When it reaches a supported blocking operation:

```text
Virtual thread waits for I/O
      ↓
Unmounted from carrier
      ↓
Carrier becomes available
      ↓
Another virtual thread can run
```

When the operation becomes ready, the virtual thread can resume on the same or a different carrier.

Inside application code:

```java
Thread.currentThread()
```

still refers to the virtual thread, not the carrier thread. fileciteturn11file0L1021-L1044

---

# 56. Virtual Threads Do NOT Create More CPU

Virtual threads improve:

```text
concurrency/scalability for waiting work
```

They do not increase:

```text
CPU core count
```

Therefore:

```text
CPU-bound task
→ still limited by available CPU

Blocking task
→ virtual threads can improve concurrency
```

Creating thousands of virtual threads for CPU-intensive calculations does not create thousands of simultaneous CPU executions.

For CPU-bound divide-and-conquer work, `ForkJoinPool` is generally more appropriate. fileciteturn11file0L1045-L1058

---

# 57. Java Version

Virtual threads became a permanent Java feature in:

```text
Java 21
```

Important APIs:

```java
Thread.ofVirtual()
Thread.startVirtualThread(...)
Executors.newVirtualThreadPerTaskExecutor()
```

fileciteturn11file0L1059-L1065

---

# 58. `Thread.startVirtualThread()`

Example:

```java
Thread thread =
    Thread.startVirtualThread(() -> {
        System.out.println(
            "Running in: " +
            Thread.currentThread()
        );
    });

thread.join();
```

This creates and starts a virtual thread.

Equivalent builder-style form:

```java
Thread.ofVirtual().start(task);
```

---

# 59. Virtual Thread Builder

```java
Thread thread =
    Thread.ofVirtual()
        .name("order-task")
        .start(() -> {
            System.out.println(
                Thread.currentThread()
            );
        });
```

You can also create an unstarted virtual thread:

```java
Thread thread =
    Thread.ofVirtual()
        .name("order-task")
        .unstarted(task);

thread.start();
```

fileciteturn11file0L1084-L1101

---

# 60. Check Whether a Thread Is Virtual

```java
boolean virtual =
    Thread.currentThread().isVirtual();
```

Example:

```java
Thread.startVirtualThread(() -> {
    System.out.println(
        Thread.currentThread().isVirtual()
    );
});
```

Output:

```text
true
```

fileciteturn11file0L1102-L1114

---

# 61. Virtual-Thread-Per-Task Executor

Create it with:

```java
ExecutorService executor =
    Executors.newVirtualThreadPerTaskExecutor();
```

This creates a new virtual thread for each submitted task:

```text
Task 1 → Virtual Thread 1
Task 2 → Virtual Thread 2
Task 3 → Virtual Thread 3
```

### Extremely important

This is **not a traditional thread pool**.

Virtual threads are cheap enough that the intended model is:

```text
One task
   ↓
One new virtual thread
   ↓
Task completes
   ↓
Virtual thread ends
```

The executor does not reuse virtual threads as a conventional platform-thread pool does. fileciteturn11file0L1116-L1126

---

# 62. Basic Virtual Thread Executor Example

```java
try (ExecutorService executor =
        Executors.newVirtualThreadPerTaskExecutor()) {

    for (int i = 1; i <= 5; i++) {

        int taskId = i;

        executor.submit(() -> {
            System.out.println(
                "Task " + taskId +
                " executed by " +
                Thread.currentThread()
            );
        });
    }
}
```

The reference material uses try-with-resources to close the executor after task submission and completion. fileciteturn11file0L1127-L1153

---

# 63. Blocking I/O Example

A simplified pattern:

```java
try (ExecutorService executor =
        Executors.newVirtualThreadPerTaskExecutor()) {

    for (int i = 1; i <= 10_000; i++) {

        int taskId = i;

        executor.submit(() -> {
            try {
                Thread.sleep(
                    Duration.ofSeconds(1)
                );

                System.out.println(
                    "Completed task " + taskId
                );

            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
            }
        });
    }
}
```

The sleep represents waiting for an external operation; while a virtual thread is waiting, its carrier can normally execute another virtual thread. fileciteturn11file0L1154-L1197

---

# 64. When to Use Virtual Threads

Especially suitable for:

```text
web requests
REST API calls
database operations
file/network I/O
blocking queues
message processing
request-per-thread designs
many concurrent waiting tasks
```

The main advantage is that straightforward blocking code can scale to much higher concurrency without requiring one expensive platform thread per waiting operation. fileciteturn11file0L1198-L1210

---

# 65. Less Suitable Workloads

Virtual threads do not provide a major advantage when work is:

```text
long-running and CPU-intensive
calculation-heavy
limited by a small downstream resource
already naturally handled as data parallelism
```

Examples:

```text
video encoding
image rendering
encryption
large numerical calculations
recursive array processing
```

For CPU-bound work, limit concurrency close to available processing capacity. fileciteturn11file0L1211-L1226

---

# 66. Virtual Threads Are Not Unlimited Work

Virtual threads make the **thread** cheap.

They do not make dependent resources unlimited.

Still-limited resources include:

```text
database connection pools
external API rate limits
memory
file descriptors
sockets
downstream service capacity
```

Creating 100,000 virtual threads does not safely create 100,000 database connections.

The reference material illustrates resource limiting with:

```java
Semaphore databaseLimit =
    new Semaphore(50);
```

### Core principle

> **Limit concurrency around the scarce resource, not simply around the number of virtual threads.**

fileciteturn11file0L1227-L1245

---

# 67. Do Not Pool Virtual Threads

Platform-thread pooling exists mainly because platform-thread creation is relatively expensive.

With virtual threads, the intended model is:

```text
One task
 ↓
One virtual thread
 ↓
Task finishes
 ↓
Thread ends
```

So don't create a small fixed pool of virtual threads just to imitate a platform-thread pool.

Use:

```java
Executors.newVirtualThreadPerTaskExecutor();
```

and limit scarce resources separately. fileciteturn11file0L1249-L1262

---

# 68. Virtual Threads and `synchronized`

Modern JDKs have reduced important sources of virtual-thread pinning, including monitor-related improvements introduced after virtual threads first became permanent.

However, long blocking operations while executing native or foreign code can still occupy a carrier thread.

Practical rule:

```text
Keep critical sections small
Do not hold locks during long external I/O
Measure actual application behavior
```

Excessive carrier blocking can reduce scalability. fileciteturn11file0L1263-L1275

---

# 69. Virtual Threads + ThreadLocal

Virtual threads support `ThreadLocal`.

Because a program may create very many virtual threads:

```text
large ThreadLocal values
→ large memory usage
```

Recommendations:

```text
avoid large ThreadLocal objects
remove values when no longer needed
do not use ThreadLocal as a resource pool
prefer explicit parameters when practical
```

Each virtual thread should be treated as an independent task context. fileciteturn11file0L1279-L1288

---

# PART 6 — CHOOSING THE CORRECT TOOL

# 70. Future vs CompletableFuture

| Requirement | Better choice |
|---|---|
| Track one task's result | `Future` |
| Wait/cancel one task | `Future` |
| Build dependent operation chain | `CompletableFuture` |
| Transform result automatically | `CompletableFuture` |
| Combine independent async results | `CompletableFuture` |
| Declarative completion pipeline | `CompletableFuture` |

fileciteturn11file0L1289-L1297

---

# 71. ForkJoinPool vs Virtual Threads

| ForkJoinPool | Virtual Threads |
|---|---|
| Primarily CPU-bound parallel work | Primarily high-concurrency blocking work |
| Splits computation into subtasks | Gives each task its own lightweight thread |
| Uses work stealing | Uses JVM scheduling over carrier threads |
| Best for divide-and-conquer | Best for thread-per-task/request |
| Parallelism limited by useful CPU capacity | Concurrency can be much larger than CPU count |
| Avoid long blocking operations | Blocking is a primary use case |

fileciteturn11file0L1298-L1315

---

# 72. CompletableFuture vs Virtual Threads

These are **not direct replacements**.

## CompletableFuture

Think:

```text
Represent work as stages
        ↓
Attach continuations
        ↓
Compose results
```

Useful when:

```text
asynchronous pipelines are natural
independent results must be combined
framework already uses completion stages
callback-style composition is desired
```

## Virtual Threads

Think:

```text
Write normal sequential blocking code
        ↓
Run each task on a virtual thread
        ↓
JVM handles large concurrency
```

Useful when:

```text
workflow is naturally sequential
code makes blocking I/O calls
readability of imperative code matters
thread-per-request programming is preferred
```

fileciteturn11file0L1316-L1343

---

# 73. CompletableFuture vs Virtual Thread Example

### CompletableFuture approach

```java
CompletableFuture<String> response =
    CompletableFuture
        .supplyAsync(() -> fetchUser())
        .thenCombine(
            CompletableFuture.supplyAsync(
                () -> fetchOrders()
            ),
            (user, orders) ->
                buildResponse(user, orders)
        );
```

This naturally expresses two independent async operations and their combination.

### Simple virtual-thread/imperative version

```java
String user = fetchUser();
String orders = fetchOrders();

String response =
    buildResponse(user, orders);
```

This is easier to read, but the two calls are sequential unless separate virtual threads or another concurrency mechanism is used. fileciteturn11file0L1344-L1362

---

# 74. Final Mental Model

```text
Future
→ Submit one task
→ Receive a placeholder
→ Wait/check/cancel
```

```text
CompletableFuture
→ Represent a stage
→ Transform/combine stages
→ React to completion/failure
```

```text
ForkJoinPool
→ Split CPU-bound problem
→ Process subtasks
→ Steal work when idle
→ Join partial results
```

```text
ThreadLocal
→ One value per thread
→ Useful for contextual state
→ Clean up when thread/task finishes
```

```text
Virtual Threads
→ One lightweight thread per task
→ Blocking virtual threads can release carriers when possible
→ Large numbers of waiting tasks become practical
```

fileciteturn12file0L9-L29

---

# 75. 🎤 Interview Questions

## CompletableFuture

### Q1. What is CompletableFuture?

`CompletableFuture` represents an asynchronous computation and also acts as a `CompletionStage`, allowing dependent operations to be composed.

### Q2. What does CompletableFuture implement?

```text
Future
CompletionStage
```

### Q3. `runAsync()` vs `supplyAsync()`?

```text
runAsync()
→ no result

supplyAsync()
→ returns a result
```

### Q4. `thenApply()` vs `thenAccept()`?

```text
thenApply()
→ transform

thenAccept()
→ consume
```

### Q5. `thenApply()` vs `thenApplyAsync()`?

```text
thenApply()
→ non-async continuation; no guarantee of a new thread

thenApplyAsync()
→ continuation scheduled asynchronously
```

### Q6. What does `thenCombine()` do?

Combines successful results from two independent futures.

### Q7. Difference between `get()` and `join()`?

```text
get()
→ ExecutionException
→ checked exception handling

join()
→ CompletionException
→ unchecked
```

---

## ForkJoinPool

### Q8. What problem does ForkJoinPool solve?

Divide-and-conquer, usually CPU-bound, parallel computations.

### Q9. What is work stealing?

An idle worker takes available work from another worker's local queue.

### Q10. What is RecursiveTask<V>?

A fork-join task that returns a value.

### Q11. What is RecursiveAction?

A fork-join task that does not return a value.

### Q12. Why use a threshold?

To stop recursive splitting once the task becomes small enough and avoid excessive task overhead.

---

## ThreadLocal

### Q13. What does ThreadLocal do?

It associates a separate value with each thread.

### Q14. Why is ThreadLocal dangerous with thread pools?

Workers are reused, so stale values can leak from one task to another if they are not removed.

### Q15. How should ThreadLocal values be cleaned?

Use:

```java
finally {
    threadLocal.remove();
}
```

---

## Virtual Threads

### Q16. What is a virtual thread?

A lightweight Java thread scheduled by the Java runtime over carrier platform threads.

### Q17. What is a carrier thread?

A platform thread that temporarily runs a virtual thread.

### Q18. Do virtual threads create more CPU?

No. They improve concurrency for waiting/blocking work, not CPU capacity.

### Q19. When are virtual threads a good fit?

For many concurrent tasks that spend significant time blocked on I/O or other waiting operations.

### Q20. What is `newVirtualThreadPerTaskExecutor()`?

An executor that creates a new virtual thread for each submitted task rather than pooling reusable virtual threads like traditional platform-thread pools.

### Q21. Should virtual threads be pooled?

Generally no. Their low creation cost enables a one-virtual-thread-per-task model.

---

# 76. ⚠️ Common Beginner Mistakes

### Mistake 1

> "CompletableFuture means the whole pipeline is non-blocking."

Not necessarily.

A stage can still perform blocking I/O.

---

### Mistake 2

> "thenApply() always creates a new thread."

No.

It is a non-async continuation and does not guarantee a new thread.

---

### Mistake 3

> "thenApply() always runs in the same thread."

Also too simplistic.

It may run in a thread involved in completing or registering the stage.

---

### Mistake 4

> "ForkJoinPool is the best executor for everything."

No.

It is mainly designed for CPU-bound divide-and-conquer work.

---

### Mistake 5

> "Work stealing means one global queue."

The important model is worker-local queues, where idle workers can steal work from another worker.

---

### Mistake 6

> "Virtual threads are faster CPUs."

No.

They do not increase CPU-core capacity.

---

### Mistake 7

> "Virtual threads remove database/API limits."

No.

Scarce resources still need separate concurrency limits.

---

### Mistake 8

> "Virtual threads should be placed in a fixed pool."

Generally no.

The intended model is one virtual thread per task.

---

### Mistake 9

> "ThreadLocal is always better than parameters."

No.

ThreadLocal is useful for contextual state, but explicit parameters are often clearer.

---

### Mistake 10

> "ThreadLocal is automatically safe in a thread pool."

No.

Worker reuse makes cleanup essential.

---

# 77. Master Comparison Table

| Tool | Core problem it solves | Typical workload |
|---|---|---|
| `Future` | One async result | One submitted task |
| `CompletableFuture` | Async composition | Pipelines / dependent stages |
| `ForkJoinPool` | Divide-and-conquer parallelism | CPU-bound recursive work |
| `ThreadLocal` | Per-thread context | Request/tracing/context data |
| Virtual Threads | Large-scale blocking concurrency | I/O-heavy / waiting tasks |

---

# 78. ⭐ Lecture 11 — Must Remember

```text
1. Future → one asynchronous result.

2. CompletableFuture → asynchronous completion pipeline.

3. CompletableFuture implements Future + CompletionStage.

4. runAsync() → no result.

5. supplyAsync() → returns a result.

6. thenApply() → transform.

7. thenAccept() → consume.

8. thenRun() → run after completion.

9. thenApplyAsync() → asynchronously scheduled continuation.

10. thenCombine() → combine independent successful results.

11. exceptionally() → fallback/recovery after failure.

12. whenComplete() → observe success/failure.

13. handle() → inspect result/exception and produce a new result.

14. get() → blocking + ExecutionException.

15. join() → blocking + CompletionException.

16. Avoid get() between every CompletableFuture stage.

17. Asynchronous does not mean non-blocking execution.

18. ForkJoinPool → CPU-bound divide-and-conquer.

19. Work stealing balances irregular recursive workloads.

20. RecursiveTask<V> → result.

21. RecursiveAction → no result.

22. Threshold controls recursive splitting.

23. fork() → schedule subtask.

24. join() → wait for/get subtask result.

25. ThreadLocal → separate value per thread.

26. Clean ThreadLocal values in reused worker threads.

27. Platform threads are relatively expensive OS-backed resources.

28. Virtual threads are lightweight JVM-scheduled threads.

29. Carrier thread → platform thread temporarily running a virtual thread.

30. Virtual threads are excellent for high-concurrency blocking tasks.

31. Virtual threads do not create more CPU capacity.

32. Virtual-thread-per-task executor creates one virtual thread per task.

33. Do not pool virtual threads like platform threads.

34. Virtual threads do not remove database/API/memory/resource limits.

35. Future vs CompletableFuture → result handle vs composition pipeline.

36. ForkJoinPool vs Virtual Threads → CPU parallelism vs blocking-task concurrency.
```

---

# 🔥 One-Minute Revision

> **`Future` represents the eventual result of one asynchronous task, while `CompletableFuture` adds a completion-stage model that lets us transform, consume, combine, and recover from asynchronous operations without manually blocking between every stage. `ForkJoinPool` solves a different problem: divide-and-conquer CPU-bound computations using recursive task splitting, `fork()`, `join()`, and work stealing. `ThreadLocal` gives each thread its own contextual value, but values must be removed carefully when workers are reused in a thread pool. Virtual threads solve large-scale blocking concurrency by allowing many lightweight JVM-scheduled threads to share a much smaller number of carrier platform threads. They improve concurrency for waiting/I/O-heavy workloads, not CPU capacity. Therefore, choose the tool based on the workload: `Future` for a simple async result, `CompletableFuture` for composition, `ForkJoinPool` for CPU-bound divide-and-conquer parallelism, `ThreadLocal` for per-thread context, and virtual threads for high-concurrency blocking tasks.**

---

# 📌 Final Concept Map

```text
                       ADVANCED CONCURRENCY
                               │
        ┌──────────────────────┼──────────────────────┐
        │                      │                      │
      Future           CompletableFuture        ForkJoinPool
        │                      │                      │
    one result           completion stages      divide & conquer
        │                      │                      │
 get / cancel         transform / combine      fork / join
                               │                      │
                         error handling         work stealing

                               │
                               │
                           ThreadLocal
                               │
                       per-thread context
                               │
                         set / get / remove

                               │
                               │
                        Virtual Threads
                               │
                      one lightweight thread/task
                               │
                        JVM scheduler
                               │
                       carrier platform threads
                               │
                       blocking-friendly scale
```

# Lecture 11 Core Principle

> **Different concurrency tools solve different layers of the problem: `Future` represents a result, `CompletableFuture` composes asynchronous stages, `ForkJoinPool` parallelizes divisible CPU-bound work, `ThreadLocal` provides per-thread context, and virtual threads make very large-scale blocking concurrency practical. Choose according to workload, not API popularity.**
