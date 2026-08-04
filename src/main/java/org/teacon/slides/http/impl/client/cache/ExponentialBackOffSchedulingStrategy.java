package org.teacon.slides.http.impl.client.cache;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.apache.http.annotation.Contract;
import org.apache.http.annotation.ThreadingBehavior;
import org.apache.http.util.Args;

@Contract(
   threading = ThreadingBehavior.SAFE
)
public class ExponentialBackOffSchedulingStrategy implements SchedulingStrategy {
   public static final long DEFAULT_BACK_OFF_RATE = 10L;
   public static final long DEFAULT_INITIAL_EXPIRY_IN_MILLIS = TimeUnit.SECONDS.toMillis(6L);
   public static final long DEFAULT_MAX_EXPIRY_IN_MILLIS = TimeUnit.SECONDS.toMillis(86400L);
   private final long backOffRate;
   private final long initialExpiryInMillis;
   private final long maxExpiryInMillis;
   private final ScheduledExecutorService executor;

   public ExponentialBackOffSchedulingStrategy(CacheConfig cacheConfig) {
      this(cacheConfig, 10L, DEFAULT_INITIAL_EXPIRY_IN_MILLIS, DEFAULT_MAX_EXPIRY_IN_MILLIS);
   }

   public ExponentialBackOffSchedulingStrategy(CacheConfig cacheConfig, long backOffRate, long initialExpiryInMillis, long maxExpiryInMillis) {
      this(createThreadPoolFromCacheConfig(cacheConfig), backOffRate, initialExpiryInMillis, maxExpiryInMillis);
   }

   private static ScheduledThreadPoolExecutor createThreadPoolFromCacheConfig(CacheConfig cacheConfig) {
      ScheduledThreadPoolExecutor scheduledThreadPoolExecutor = new ScheduledThreadPoolExecutor(cacheConfig.getAsynchronousWorkersMax());
      scheduledThreadPoolExecutor.setExecuteExistingDelayedTasksAfterShutdownPolicy(false);
      return scheduledThreadPoolExecutor;
   }

   ExponentialBackOffSchedulingStrategy(ScheduledExecutorService executor, long backOffRate, long initialExpiryInMillis, long maxExpiryInMillis) {
      this.executor = (ScheduledExecutorService)Args.notNull(executor, "Executor");
      this.backOffRate = Args.notNegative(backOffRate, "BackOffRate");
      this.initialExpiryInMillis = Args.notNegative(initialExpiryInMillis, "InitialExpiryInMillis");
      this.maxExpiryInMillis = Args.notNegative(maxExpiryInMillis, "MaxExpiryInMillis");
   }

   @Override
   public void schedule(AsynchronousValidationRequest revalidationRequest) {
      Args.notNull(revalidationRequest, "RevalidationRequest");
      int consecutiveFailedAttempts = revalidationRequest.getConsecutiveFailedAttempts();
      long delayInMillis = this.calculateDelayInMillis(consecutiveFailedAttempts);
      this.executor.schedule(revalidationRequest, delayInMillis, TimeUnit.MILLISECONDS);
   }

   @Override
   public void close() {
      this.executor.shutdown();
   }

   public long getBackOffRate() {
      return this.backOffRate;
   }

   public long getInitialExpiryInMillis() {
      return this.initialExpiryInMillis;
   }

   public long getMaxExpiryInMillis() {
      return this.maxExpiryInMillis;
   }

   protected long calculateDelayInMillis(int consecutiveFailedAttempts) {
      if (consecutiveFailedAttempts > 0) {
         long delayInSeconds = (long)((double)this.initialExpiryInMillis * Math.pow((double)this.backOffRate, (double)(consecutiveFailedAttempts - 1)));
         return Math.min(delayInSeconds, this.maxExpiryInMillis);
      } else {
         return 0L;
      }
   }

   @Deprecated
   protected static <T> T checkNotNull(String parameterName, T value) {
      if (value == null) {
         throw new IllegalArgumentException(parameterName + " may not be null");
      } else {
         return value;
      }
   }

   @Deprecated
   protected static long checkNotNegative(String parameterName, long value) {
      if (value < 0L) {
         throw new IllegalArgumentException(parameterName + " may not be negative");
      } else {
         return value;
      }
   }
}
