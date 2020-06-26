package org.robolectric.android.util.concurrent;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * Utility class for running code off the main looper thread aka Robolectric test thread.
 */
public class BackgroundExecutor {

  private BackgroundExecutor() {}

  // use an inner class reference to lazy load the singleton in a thread-safe manner
  private static class SingletonHolder {
    private static final BackgroundExecutor instance = new BackgroundExecutor();
  }

  private final InlineExecutorService backgroundExecutorService =
      new InlineExecutorService();

  /**
   * A helper method intended for testing production code that needs to run off the main Looper.
   *
   * Will execute given runnable in a background thread and will do a best-effort attempt at
   * propagating any exception back up to caller in their original form.
   */
  public static void runInBackground(Runnable runnable) {
    SingletonHolder.instance.backgroundExecutorService.execute(runnable);
  }

  /**
   * A helper method intended for testing production code that needs to run off the main Looper.
   *
   * <p>Will execute given callable in a background thread and will do a best-effort attempt at
   * propagating any exception back up to caller in their original form.
   */
  public static <T> T runInBackground(Callable<T> callable) {
    Future<T> future = SingletonHolder.instance.backgroundExecutorService.submit(callable);
    return getFutureResultWithExceptionPreserved(future);
  }

  /**
   * Run given callable on the given executor and try to preserve original exception if possible.
   */
  static <T> T getFutureResultWithExceptionPreserved(Future<T> future) {
    try {
      return future.get();
    } catch (ExecutionException e) {
      // try to preserve original exception if possible
      Throwable cause = e.getCause();
      if (cause == null) {
        throw new RuntimeException(e);
      } else if (cause instanceof RuntimeException) {
        throw (RuntimeException) cause;
      } else {
        throw new RuntimeException(cause);
      }
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
  }
}
