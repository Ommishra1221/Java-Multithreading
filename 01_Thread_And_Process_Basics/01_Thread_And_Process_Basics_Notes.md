# Java Multithreading — Lecture 1 Notes
## Program, Process, Thread, CPU, Concurrency & Parallelism

These are revision notes for Lecture 1. The goal is to understand the complete picture from stored Java code to actual execution, then understand threads, concurrency, parallelism, multitasking, and multithreading.

---

# 1. Why Do We Need Multithreading?

Multithreading is important for both real-world applications and technical interviews.

A major point is:

> **Multithreading is not simply a technique for making a program faster.**

It allows multiple execution paths/tasks to make progress within an application.

Example: An IDE can accept typing while performing error checking, suggestions, and background work.

Actual speed improvement depends on the workload and whether the work can truly execute in parallel.

---

# 2. The Big Picture

Remember this chain:

```text
Java Source Code
       ↓
     javac
       ↓
Java Bytecode (.class)
       ↓
   java Main
       ↓
Operating-System Process
       ↓
      JVM
       ↓
    Main Thread
       ↓
 Additional Threads
       ↓
    CPU Cores
       ↓
Execution
```

### Most important mental model

> **A program becomes active as a process, and the actual work inside that process is performed by threads.**

---

# 3. What Is a Program?

A **program** is a set of instructions written to make a computer perform a task.

For Java:

```text
Demo.java
   ↓
source code

Demo.class
   ↓
compiled bytecode
```

Both are stored forms of a program.

## Program is passive

A program is **passive**. It contains instructions, but those instructions are not currently executing.

```text
Program
   ↓
instructions stored in a file
   ↓
not currently executing
```

Examples:

- `Demo.class` stored on disk
- Chrome executable stored on disk
- VS Code application stored on disk

### Interview definition

> **A program is a passive set of instructions stored in a file, whereas a process is a running instance of that program.**

---

# 4. What Is a Process?

A **process is a running instance of a program**.

Suppose:

```text
Demo.java
```

Compile it:

```bash
javac Demo.java
```

You get:

```text
Demo.class
```

Then run:

```bash
java Demo
```

The operating system starts a **process** for that running application.

A useful mental model is:

```text
Program
   ↓ launch
Process
   ↓
JVM
   ↓
Java application
```

### Important

The JVM runs **inside the operating-system process**. It is not a separate process sitting inside the Java process.

---

# 5. What Does a Process Need?

When a process starts, the operating system provides it with an execution environment.

## Memory

A process gets a virtual address space containing things such as:

- Executable code
- Runtime data
- Heap memory
- Thread stacks
- Loaded libraries
- Memory-mapped files

## CPU time

The operating system schedules the process's threads and determines when they get processor time.

## Other resources

A process may use:

- Files
- Network sockets
- Input/output devices
- Database connections
- Shared libraries
- Operating-system handles

---

# 6. Process = Independent Execution Environment

Each process normally has its **own virtual address space**.

```text
Process A
  Memory

Process B
  Memory
```

They normally do not directly access each other's memory.

This provides **stronger isolation**.

If one process crashes, unrelated processes will normally continue running.

## Communication between processes

Processes can communicate using mechanisms such as:

- Pipes
- Sockets
- Shared memory
- Files
- Message queues
- Remote calls

This is called **IPC — Inter-Process Communication**.

---

# 7. What Is a Thread?

A **thread is an independent path of execution inside a process**.

Think:

```text
One Process
│
├── Main Thread
├── Worker Thread 1
├── Worker Thread 2
└── Background Thread
```

The process is the **container/execution environment**.

The threads are the execution paths that actually execute instructions.

A thread has its own execution-related state, including:

- Program counter
- Call stack
- Stack frames
- Local variables
- Current instruction position
- Native execution state

---

# 8. Why Is a Thread Called a "Lightweight Process"?

You may hear:

> **Thread = lightweight process**

This is because threads are generally:

- Cheaper to create than processes
- Able to share the same process memory
- Easier/faster to communicate between
- Generally cheaper to switch between than separate processes

### Important

A thread **is not actually a process**.

A thread normally does **not** have its own independent address space.

So:

```text
Process ≠ Thread
```

A thread belongs to a process.

---

# 9. Process vs Thread

| Process | Thread |
|---|---|
| Running instance of a program | Execution path inside a process |
| Has its own address space | Shares process address space |
| Contains one or more threads | Belongs to one process |
| More expensive to create | Usually cheaper to create |
| Communication generally uses IPC | Can communicate through shared memory |
| Better isolation | Lower isolation |
| Context switching is generally heavier | Generally lighter |

### Interview trap

Do not say:

> "A process is a bigger thread."

Better answer:

> **A process is an independent execution environment, while a thread is an execution path within that process.**

---

# 10. Main Thread in Java

When a normal Java application starts, the JVM creates a thread that invokes:

```java
public static void main(String[] args)
```

This is called the:

> **Main Thread**

So when you write:

```java
public class Main {
    public static void main(String[] args) {
        System.out.println("Hello");
    }
}
```

`main()` does **not** execute independently.

It executes **inside the main thread**.

---

# 11. How a Java Program Actually Starts

## Step 1 — Write source code

```text
Main.java
```

## Step 2 — Compile

```bash
javac Main.java
```

Output:

```text
Main.class
```

This contains Java bytecode.

## Step 3 — Run

```bash
java Main
```

The Java launcher asks the operating system to start a process.

## Step 4 — JVM initializes

The JVM performs tasks such as:

- Loading classes
- Verifying bytecode
- Linking classes
- Initializing runtime services
- Preparing runtime memory
- Preparing the main thread
- Invoking `main()`

## Step 5 — Main thread executes

```text
Process
   ↓
JVM
   ↓
Main Thread
   ↓
main()
```

---

# 12. Important JVM Memory Concept

A simplified JVM model contains:

```text
JVM
│
├── Heap
├── Method Area / Metaspace
├── Java Stacks
├── PC Registers
└── Native Method Stacks
```

---

# 13. Heap — Shared

The **heap** stores objects and arrays.

Example:

```java
Student student = new Student();
```

The `Student` object is normally created in the heap.

### Multithreading point

> **The heap is shared among threads in the same JVM.**

This shared memory is extremely important because later it leads to concepts such as:

- Race conditions
- Synchronization
- Locks
- Thread safety

---

# 14. Method Area / Metaspace

The JVM specification defines a logical **Method Area**.

It contains information such as:

- Class metadata
- Method metadata
- Runtime constant pools
- Static-field-related class data
- Method bytecode

In HotSpot, **Metaspace** is a common implementation approach for class metadata storage.

### Important distinction

Do not blindly say:

> Method Area = Metaspace

Better:

> **Method Area is a JVM specification concept; Metaspace is an implementation approach used by HotSpot.**

---

# 15. Stack — Private to Each Thread

This is one of the **most important concepts for multithreading**.

Every Java thread has its **own Java stack**.

Example:

```text
Process
│
├── Main Thread
│      └── Main Stack
│
└── Worker Thread
       └── Worker Stack
```

A stack contains stack frames for method calls.

For example:

```text
main()
   ↓
methodA()
   ↓
methodB()
```

The stack may conceptually contain:

```text
Thread Stack
├── methodB() frame
├── methodA() frame
└── main() frame
```

Each frame contains things such as:

- Local variables
- Operand-stack data
- Execution information
- Return information

---

# 16. PC Register — Private to Each Thread

Every JVM thread has its own **program-counter (PC) register**.

Conceptually, it tracks the current/next JVM instruction for that thread.

```text
Thread 1 → own PC
Thread 2 → own PC
Thread 3 → own PC
```

Why?

Because different threads may be executing different instructions or methods at different points in execution.

---

# 17. Most Important Memory Diagram

Keep this mental picture:

```text
                 ONE JAVA PROCESS
┌─────────────────────────────────────────┐
│                   JVM                   │
│                                         │
│   SHARED                          PRIVATE│
│                                         │
│   ┌───────────┐                  ┌─────┐│
│   │   Heap    │                  │Stack││
│   └───────────┘                  │  T1 ││
│                                 └─────┘│
│   ┌───────────┐                  ┌─────┐│
│   │ Method    │                  │Stack││
│   │ Area      │                  │  T2 ││
│   └───────────┘                  └─────┘│
│                                         │
│                                  PC T1  │
│                                  PC T2  │
└─────────────────────────────────────────┘
```

### Remember

> **Threads share process memory such as heap objects and class-related data, but each thread has its own Java stack and PC register.**

This idea becomes extremely important when studying **synchronization and race conditions**.

---

# 18. Single-Core CPU

Suppose the computer has only **one CPU core**.

You have:

```text
Main Thread
Thread-1
Thread-2
```

Can all three execute at exactly the same instant?

### No.

At one exact instant, only one runnable thread can execute on that one core.

Instead, the system can do something like:

```text
Main
  ↓
Thread-1
  ↓
Main
  ↓
Thread-2
  ↓
Thread-1
```

The switching happens very quickly, so it can look like the threads are running simultaneously.

This leads to **concurrency**.

---

# 19. Context Switching

A **context switch** happens when the CPU stops executing one thread and starts executing another.

Conceptually:

```text
Thread A running
      ↓
save A's execution state
      ↓
select Thread B
      ↓
restore B's execution state
      ↓
Thread B runs
```

The execution state can involve things such as:

- CPU registers
- Instruction pointer
- Stack pointer
- Processor state
- Scheduling information

### Important

Context switching is **not free**.

It has overhead.

Too many threads can actually hurt performance because the system may spend too much time switching rather than doing useful work.

---

# 20. Concurrency

### Definition

> **Concurrency means multiple tasks make progress during overlapping periods of time, but they do not necessarily execute at the exact same instant.**

For a single-core CPU:

```text
Time →
──────────────────────────

Main      ███       ███
Thread1       ███
Thread2               ███
```

The CPU switches between them.

So:

> **Concurrency can happen even on one CPU core.**

---

# 21. Parallelism

Suppose the machine has multiple CPU cores.

Example:

```text
Core 1 → Main Thread
Core 2 → Thread 1
Core 3 → Thread 2
Core 4 → Thread 3
```

Multiple threads can execute **at the same physical instant**.

This is:

> **Parallelism**

### Definition

> **Parallelism means multiple tasks execute simultaneously on different processing units.**

---

# 22. Concurrency vs Parallelism

| Concurrency | Parallelism |
|---|---|
| Multiple tasks make progress during overlapping periods | Multiple tasks execute at the same instant |
| Can happen on one core | Usually requires multiple cores/hardware execution units |
| Focuses on managing/interleaving tasks | Focuses on simultaneous execution |
| Uses scheduling/interleaving | Uses actual hardware execution |
| Does not guarantee speed improvement | Can improve throughput for parallelizable work |

### Easy way to remember

**Concurrency = dealing with many things**

**Parallelism = doing many things at the same time**

---

# 23. Concurrency ≠ Parallelism

A program can be:

## Concurrent but not parallel

```text
Multiple threads
       ↓
One CPU core
       ↓
Context switching
```

## Concurrent and parallel

```text
Multiple threads
       ↓
Multiple CPU cores
       ↓
True simultaneous execution
```

---

# 24. Multitasking

**Multitasking** is primarily an operating-system concept.

It means the OS manages multiple tasks/applications during the same period.

Example:

```text
Chrome
+
VS Code
+
Music Player
```

These applications may use separate processes, and the OS schedules their threads on available CPU cores.

---

# 25. Multithreading

**Multithreading** means:

> **One process contains multiple threads of execution.**

Example:

```text
One Java Process
│
├── Main Thread
├── Request-processing Thread
├── Database Worker Thread
└── Background Monitoring Thread
```

These threads share the process's resources and memory.

---

# 26. Multitasking vs Multithreading

| Multitasking | Multithreading |
|---|---|
| Usually multiple processes/applications | Multiple threads inside one process |
| Processes have separate address spaces | Threads share process address space |
| Communication is relatively heavier | Shared-memory communication is easier |
| Stronger isolation | Lower isolation |
| Process creation generally more expensive | Thread creation generally cheaper |

### Example

```text
MULTITASKING

Chrome       → Process
VS Code      → Process
Spotify      → Process
```

versus:

```text
MULTITHREADING

Java Process
│
├── Main Thread
├── Worker Thread 1
├── Worker Thread 2
└── Background Thread
```

---

# 27. Real-World Java Example

Imagine a Spring Boot application.

```text
application.jar
       ↓
stored program
       ↓
java -jar application.jar
       ↓
Operating-system process
       ↓
JVM
       ↓
├── Main thread
├── Web-server threads
├── Database-pool threads
├── Garbage-collection threads
└── Application worker threads
```

Several request-processing threads may execute the same controller code for different HTTP requests while sharing application objects stored in the heap.

This is why **thread safety** becomes important in server applications.

---

# 28. Program vs Process vs Thread

This is worth memorizing:

```text
PROGRAM
↓
Stored instructions

PROCESS
↓
Running instance of a program

THREAD
↓
Execution path inside a process
```

Another easy mental model:

```text
Program  = What to execute
Process  = Running environment
Thread   = Who/what executes the work
```

The second version is a learning aid, not a formal definition.

---

# 29. Complete Java Execution Flow

For revision, remember this sequence:

```text
1. Write Main.java
        ↓
2. javac Main.java
        ↓
3. Main.class generated
        ↓
4. java Main
        ↓
5. OS starts a process
        ↓
6. JVM initializes
        ↓
7. JVM loads/verifies classes
        ↓
8. Runtime memory is prepared
        ↓
9. JVM invokes main() on main thread
        ↓
10. Main thread executes
        ↓
11. Additional threads may be created
        ↓
12. Scheduler assigns runnable threads to CPU cores
        ↓
13. JVM interprets/JIT-compiles code
        ↓
14. CPU executes native machine instructions
```

---

# 30. Important Technical Detail: Java Bytecode and CPU

A common oversimplification is:

> "The CPU directly executes Java bytecode."

That is not the complete picture.

A better model is:

```text
Java Bytecode
     ↓
interpreted OR
JIT compiled
     ↓
native machine instructions
     ↓
CPU
```

Ultimately, the CPU executes **native machine instructions**, not Java source code directly.

---

# 31. JVM vs Operating-System Memory Concepts

Do not mix these abstraction levels:

```text
Operating System
        ↓
Process memory

JVM
        ↓
Heap
Method Area
Java Stacks
etc.
```

The JVM's **Method Area** is not simply the same thing as an operating-system "code segment" or "data segment".

They belong to different abstraction layers.

Just remember:

> **OS memory concepts and JVM memory concepts are related, but they are not identical abstractions.**

---

# 32. Main Mental Model of Lecture 1

Imagine this:

```text
                    COMPUTER
                       │
                       ▼
                OPERATING SYSTEM
                       │
                       ▼
                 JAVA PROCESS
                       │
                       ▼
                      JVM
                       │
         ┌─────────────┴─────────────┐
         │                           │
      SHARED                      THREADS
      MEMORY                         │
         │                ┌──────────┼──────────┐
      Heap               T1         T2         T3
      Classes             │          │          │
                          ▼          ▼          ▼
                        Stack      Stack      Stack
                          │          │          │
                         PC         PC         PC
                           \         |         /
                            \        |        /
                             ▼       ▼       ▼
                                  CPU
                                   │
                         ┌─────────┴─────────┐
                         │                   │
                     1 Core              Multi-Core
                         │                   │
                   Concurrency         Parallelism
                   (switching)       (simultaneous)
```

This is the picture to carry forward into the next lectures.

---

# 33. Fresher Interview Questions From Lecture 1

## Basic

### 1. What is a program?

A passive set of instructions stored in a file.

### 2. What is a process?

A running instance of a program.

### 3. What is a thread?

An independent path of execution inside a process.

### 4. Why is a thread called a lightweight process?

Because threads generally require fewer resources to create/switch and share process memory.

### 5. What is the main thread in Java?

The JVM-created thread that invokes the application's `main()` method.

## Memory

### 6. What memory is shared between threads?

The shared process/JVM memory includes the heap and class-related data.

### 7. What is private to each Java thread?

Its Java stack and PC register.

## Execution

### 8. What is context switching?

Switching CPU execution from one thread to another by saving/restoring execution state.

### 9. Can concurrency happen on a single-core CPU?

Yes.

### 10. Can parallelism happen on a single-core CPU?

Not in the sense of multiple tasks executing simultaneously on separate cores.

## Conceptual

### 11. Difference between concurrency and parallelism?

Concurrency is overlapping progress/interleaving; parallelism is simultaneous execution.

### 12. Difference between multitasking and multithreading?

Multitasking generally refers to managing multiple applications/processes; multithreading refers to multiple threads inside one process.

---

# 34. Common Mistakes to Avoid

## Mistake 1

> "A program is running in RAM."

Not necessarily.

A program is stored instructions. A **process** is the active running instance.

## Mistake 2

> "Each thread has its own heap."

❌ No.

Threads in the same process share the heap.

## Mistake 3

> "Threads have everything in common."

❌ No.

They share process memory, but each thread has its own execution-related state such as its Java stack and PC register.

## Mistake 4

> "Multithreading always makes a program faster."

❌ No.

It can improve responsiveness, throughput, or parallel performance, but thread scheduling and context switching have costs.

## Mistake 5

> "Concurrency means simultaneous execution."

❌ Not necessarily.

That's closer to **parallelism**.

---

# 35. Lecture 1 — 10 Things You Must Remember

```text
1. Program = stored/passive instructions.

2. Process = running instance of a program.

3. Thread = execution path inside a process.

4. A process contains one or more threads.

5. Java applications have a main thread that executes main().

6. Threads share process memory, especially heap objects.

7. Each Java thread has its own Java stack and PC register.

8. One CPU core → threads may execute through interleaving/context switching.

9. Multiple CPU cores → threads can execute in parallel.

10. Multitasking = multiple processes/applications;
    Multithreading = multiple threads inside one process.
```

---

# 36. Final One-Minute Revision

Before moving to Lecture 2, make sure this entire story makes sense:

> **I write a Java program → compile it into bytecode → run it → the OS creates a process → the JVM runs inside that process → the JVM starts the main thread → `main()` executes on that thread → additional threads can be created → threads share process memory but have their own execution state → the OS schedules them onto CPU cores → on one core they may execute concurrently through switching → on multiple cores they may execute in parallel.**

### Lecture 1 Core Chain

**Program → Process → Thread → CPU → Concurrency / Parallelism**

Keep this chain firmly in mind before going deeper.
