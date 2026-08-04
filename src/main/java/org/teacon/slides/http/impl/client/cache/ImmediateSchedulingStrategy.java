package org.teacon.slides.http.impl.client.cache;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.apache.http.annotation.Contract;
import org.apache.http.annotation.ThreadingBehavior;
import org.apache.http.util.Args;

@Contract(
   threading = ThreadingBehavior.SAFE
)
public class ImmediateSchedulingStrategy implements SchedulingStrategy {
   private final ExecutorService executor;

   public ImmediateSchedulingStrategy(CacheConfig cacheConfig) {
      this(
         new ThreadPoolExecutor(
            cacheConfig.getAsynchronousWorkersCore(),
            cacheConfig.getAsynchronousWorkersMax(),
            (long)cacheConfig.getAsynchronousWorkerIdleLifetimeSecs(),
            TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(cacheConfig.getRevalidationQueueSize())
         )
      );
   }

   ImmediateSchedulingStrategy(ExecutorService executor) {
      this.executor = executor;
   }

   @Override
   public void schedule(AsynchronousValidationRequest revalidationRequest) {
      Args.notNull(revalidationRequest, "AsynchronousValidationRequest");
      this.executor.execute(revalidationRequest);
   }

   @Override
   public void close() {
      this.executor.shutdown();
   }

   void awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
      this.executor.awaitTermination(timeout, unit);
   }
}
