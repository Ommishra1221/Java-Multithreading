# Contributing — Java Multithreading Lectures

## Naming Convention

Every Java file must follow this format:

```
_<two-digit>_<DescriptiveConceptName>.java
```

**Examples:**
- `_01_ThreadExtendDemo.java`
- `_02_RunnableInterfaceDemo.java`
- `_07_ThreadPriorityDemo.java`

**Class name must match filename exactly:**
```java
public class _01_ThreadExtendDemo {
    // ...
}
```

## Folder Structure

```
lecture 1/
  notes/
lecture 2/
  _01_ThreadExtendDemo.java
  _02_RunnableInterfaceDemo.java
  ...
  notes/
lecture 3/
  _01_ThreadSleepDemo.java
  _02_ThreadJoinDemo.java
  ...
  notes/
```

## Rules for Adding New Files

1. **Check existing files** in the target lecture folder first
2. **Number sequentially** — if the last file is `_06_`, the new one is `_07_`
3. **Public class name** must exactly match the filename (including `_XX_` prefix)
4. **Do not rename** existing files
5. **Verify compilation** — run `javac <filename>` to confirm 0 errors
6. **Delete `.class` files** after compilation — only `.java` source files belong in the repo

## Quick Prompt (for AI assistants)

When adding new Java files, use this prompt:

> Inspect the full project structure. For any new Java file I add:
> 1. Name it with a two-digit underscore prefix matching its learning order (e.g. _01_, _02_, _03_...)
> 2. Set the public class name to exactly match the filename
> 3. Preserve the existing folder layout (lecture N/)
> 4. Do not rename or reorder existing files
> 5. Verify it compiles with 0 errors
