package org.teacon.slides.http.impl.client.cache;

import java.io.IOException;
import java.lang.reflect.UndeclaredThrowableException;
import java.net.URI;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.Header;
import org.apache.http.HeaderElement;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpMessage;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.ProtocolException;
import org.apache.http.ProtocolVersion;
import org.apache.http.RequestLine;
import org.apache.http.annotation.Contract;
import org.apache.http.annotation.ThreadingBehavior;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpRequestWrapper;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.cookie.DateParseException;
import org.apache.http.impl.cookie.DateUtils;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.Args;
import org.apache.http.util.VersionInfo;
import org.teacon.slides.http.client.cache.CacheResponseStatus;
import org.teacon.slides.http.client.cache.HttpCacheEntry;
import org.teacon.slides.http.client.cache.HttpCacheStorage;
import org.teacon.slides.http.client.cache.ResourceFactory;

@Deprecated
@Contract(
   threading = ThreadingBehavior.SAFE_CONDITIONAL
)
public class CachingHttpClient implements HttpClient {
   public static final String CACHE_RESPONSE_STATUS = "http.cache.response.status";
   private static final boolean SUPPORTS_RANGE_AND_CONTENT_RANGE_HEADERS = false;
   private final AtomicLong cacheHits = new AtomicLong();
   private final AtomicLong cacheMisses = new AtomicLong();
   private final AtomicLong cacheUpdates = new AtomicLong();
   private final Map<ProtocolVersion, String> viaHeaders = new HashMap<>(4);
   private final HttpClient backend;
   private final HttpCache responseCache;
   private final CacheValidityPolicy validityPolicy;
   private final ResponseCachingPolicy responseCachingPolicy;
   private final CachedHttpResponseGenerator responseGenerator;
   private final CacheableRequestPolicy cacheableRequestPolicy;
   private final CachedResponseSuitabilityChecker suitabilityChecker;
   private final ConditionalRequestBuilder conditionalRequestBuilder;
   private final long maxObjectSizeBytes;
   private final boolean sharedCache;
   private final ResponseProtocolCompliance responseCompliance;
   private final RequestProtocolCompliance requestCompliance;
   private final CachingHttpClient.AsynchronousValidator asynchRevalidator;
   private final Log log = LogFactory.getLog(this.getClass());

   CachingHttpClient(HttpClient client, HttpCache cache, CacheConfig config) {
      Args.notNull(client, "HttpClient");
      Args.notNull(cache, "HttpCache");
      Args.notNull(config, "CacheConfig");
      this.maxObjectSizeBytes = config.getMaxObjectSize();
      this.sharedCache = config.isSharedCache();
      this.backend = client;
      this.responseCache = cache;
      this.validityPolicy = new CacheValidityPolicy();
      this.responseCachingPolicy = new ResponseCachingPolicy(
         this.maxObjectSizeBytes, this.sharedCache, config.isNeverCacheHTTP10ResponsesWithQuery(), config.is303CachingEnabled()
      );
      this.responseGenerator = new CachedHttpResponseGenerator(this.validityPolicy);
      this.cacheableRequestPolicy = new CacheableRequestPolicy();
      this.suitabilityChecker = new CachedResponseSuitabilityChecker(this.validityPolicy, config);
      this.conditionalRequestBuilder = new ConditionalRequestBuilder();
      this.responseCompliance = new ResponseProtocolCompliance();
      this.requestCompliance = new RequestProtocolCompliance(config.isWeakETagOnPutDeleteAllowed());
      this.asynchRevalidator = this.makeAsynchronousValidator(config);
   }

   public CachingHttpClient() {
      this(new DefaultHttpClient(), new BasicHttpCache(), new CacheConfig());
   }

   public CachingHttpClient(CacheConfig config) {
      this(new DefaultHttpClient(), new BasicHttpCache(config), config);
   }

   public CachingHttpClient(HttpClient client) {
      this(client, new BasicHttpCache(), new CacheConfig());
   }

   public CachingHttpClient(HttpClient client, CacheConfig config) {
      this(client, new BasicHttpCache(config), config);
   }

   public CachingHttpClient(HttpClient client, ResourceFactory resourceFactory, HttpCacheStorage storage, CacheConfig config) {
      this(client, new BasicHttpCache(resourceFactory, storage, config), config);
   }

   public CachingHttpClient(HttpClient client, HttpCacheStorage storage, CacheConfig config) {
      this(client, new BasicHttpCache(new HeapResourceFactory(), storage, config), config);
   }

   CachingHttpClient(
      HttpClient backend,
      CacheValidityPolicy validityPolicy,
      ResponseCachingPolicy responseCachingPolicy,
      HttpCache responseCache,
      CachedHttpResponseGenerator responseGenerator,
      CacheableRequestPolicy cacheableRequestPolicy,
      CachedResponseSuitabilityChecker suitabilityChecker,
      ConditionalRequestBuilder conditionalRequestBuilder,
      ResponseProtocolCompliance responseCompliance,
      RequestProtocolCompliance requestCompliance
   ) {
      CacheConfig config = new CacheConfig();
      this.maxObjectSizeBytes = config.getMaxObjectSize();
      this.sharedCache = config.isSharedCache();
      this.backend = backend;
      this.validityPolicy = validityPolicy;
      this.responseCachingPolicy = responseCachingPolicy;
      this.responseCache = responseCache;
      this.responseGenerator = responseGenerator;
      this.cacheableRequestPolicy = cacheableRequestPolicy;
      this.suitabilityChecker = suitabilityChecker;
      this.conditionalRequestBuilder = conditionalRequestBuilder;
      this.responseCompliance = responseCompliance;
      this.requestCompliance = requestCompliance;
      this.asynchRevalidator = this.makeAsynchronousValidator(config);
   }

   private CachingHttpClient.AsynchronousValidator makeAsynchronousValidator(CacheConfig config) {
      return config.getAsynchronousWorkersMax() > 0 ? new CachingHttpClient.AsynchronousValidator(this, config) : null;
   }

   public long getCacheHits() {
      return this.cacheHits.get();
   }

   public long getCacheMisses() {
      return this.cacheMisses.get();
   }

   public long getCacheUpdates() {
      return this.cacheUpdates.get();
   }

   public HttpResponse execute(HttpHost target, HttpRequest request) throws IOException {
      HttpContext defaultContext = null;
      return this.execute(target, request, defaultContext);
   }

   public <T> T execute(HttpHost target, HttpRequest request, ResponseHandler<? extends T> responseHandler) throws IOException {
      return this.execute(target, request, responseHandler, null);
   }

   public <T> T execute(HttpHost target, HttpRequest request, ResponseHandler<? extends T> responseHandler, HttpContext context) throws IOException {
      HttpResponse resp = this.execute(target, request, context);
      return this.handleAndConsume(responseHandler, resp);
   }

   public HttpResponse execute(HttpUriRequest request) throws IOException {
      HttpContext context = null;
      return this.execute(request, context);
   }

   public HttpResponse execute(HttpUriRequest request, HttpContext context) throws IOException {
      URI uri = request.getURI();
      HttpHost httpHost = new HttpHost(uri.getHost(), uri.getPort(), uri.getScheme());
      return this.execute(httpHost, request, context);
   }

   public <T> T execute(HttpUriRequest request, ResponseHandler<? extends T> responseHandler) throws IOException {
      return this.execute(request, responseHandler, null);
   }

   public <T> T execute(HttpUriRequest request, ResponseHandler<? extends T> responseHandler, HttpContext context) throws IOException {
      HttpResponse resp = this.execute(request, context);
      return this.handleAndConsume(responseHandler, resp);
   }

   private <T> T handleAndConsume(ResponseHandler<? extends T> responseHandler, HttpResponse response) throws Error, IOException {
      T result;
      try {
         result = (T)responseHandler.handleResponse(response);
      } catch (Exception var8) {
         HttpEntity entity = response.getEntity();

         try {
            IOUtils.consume(entity);
         } catch (Exception var7) {
            this.log.warn("Error consuming content after an exception.", var7);
         }

         if (var8 instanceof RuntimeException) {
            throw (RuntimeException)var8;
         }

         if (var8 instanceof IOException) {
            throw (IOException)var8;
         }

         throw new UndeclaredThrowableException(var8);
      }

      HttpEntity entity = response.getEntity();
      IOUtils.consume(entity);
      return result;
   }

   public ClientConnectionManager getConnectionManager() {
      return this.backend.getConnectionManager();
   }

   public HttpParams getParams() {
      return this.backend.getParams();
   }

   public HttpResponse execute(HttpHost target, HttpRequest originalRequest, HttpContext context) throws IOException {
      HttpRequestWrapper request;
      if (originalRequest instanceof HttpRequestWrapper) {
         request = (HttpRequestWrapper)originalRequest;
      } else {
         request = HttpRequestWrapper.wrap(originalRequest);
      }

      String via = this.generateViaHeader(originalRequest);
      this.setResponseStatus(context, CacheResponseStatus.CACHE_MISS);
      if (this.clientRequestsOurOptions(request)) {
         this.setResponseStatus(context, CacheResponseStatus.CACHE_MODULE_RESPONSE);
         return new OptionsHttp11Response();
      } else {
         HttpResponse fatalErrorResponse = this.getFatallyNoncompliantResponse(request, context);
         if (fatalErrorResponse != null) {
            return fatalErrorResponse;
         } else {
            this.requestCompliance.makeRequestCompliant(request);
            request.addHeader("Via", via);
            this.flushEntriesInvalidatedByRequest(target, request);
            if (!this.cacheableRequestPolicy.isServableFromCache(request)) {
               this.log.debug("Request is not servable from cache");
               return this.callBackend(target, request, context);
            } else {
               HttpCacheEntry entry = this.satisfyFromCache(target, request);
               if (entry == null) {
                  this.log.debug("Cache miss");
                  return this.handleCacheMiss(target, request, context);
               } else {
                  return this.handleCacheHit(target, request, context, entry);
               }
            }
         }
      }
   }

   private HttpResponse handleCacheHit(HttpHost target, HttpRequestWrapper request, HttpContext context, HttpCacheEntry entry) throws ClientProtocolException, IOException {
      this.recordCacheHit(target, request);
      HttpResponse out = null;
      Date now = this.getCurrentDate();
      if (this.suitabilityChecker.canCachedResponseBeUsed(target, request, entry, now)) {
         this.log.debug("Cache hit");
         out = this.generateCachedResponse(request, context, entry, now);
      } else {
         if (this.mayCallBackend(request)) {
            this.log.debug("Revalidating cache entry");
            return this.revalidateCacheEntry(target, request, context, entry, now);
         }

         this.log.debug("Cache entry not suitable but only-if-cached requested");
         out = this.generateGatewayTimeout(context);
      }

      if (context != null) {
         context.setAttribute("http.target_host", target);
         context.setAttribute("http.request", request);
         context.setAttribute("http.response", out);
         context.setAttribute("http.request_sent", Boolean.TRUE);
      }

      return out;
   }

   private HttpResponse revalidateCacheEntry(HttpHost target, HttpRequestWrapper request, HttpContext context, HttpCacheEntry entry, Date now) throws ClientProtocolException {
      try {
         if (this.asynchRevalidator != null
            && !this.staleResponseNotAllowed(request, entry, now)
            && this.validityPolicy.mayReturnStaleWhileRevalidating(entry, now)) {
            this.log.trace("Serving stale with asynchronous revalidation");
            HttpResponse resp = this.generateCachedResponse(request, context, entry, now);
            this.asynchRevalidator.revalidateCacheEntry(target, request, context, entry);
            return resp;
         } else {
            return this.revalidateCacheEntry(target, request, context, entry);
         }
      } catch (IOException var7) {
         return this.handleRevalidationFailure(request, context, entry, now);
      } catch (ProtocolException var8) {
         throw new ClientProtocolException(var8);
      }
   }

   private HttpResponse handleCacheMiss(HttpHost target, HttpRequestWrapper request, HttpContext context) throws IOException {
      this.recordCacheMiss(target, request);
      if (!this.mayCallBackend(request)) {
         return new BasicHttpResponse(HttpVersion.HTTP_1_1, 504, "Gateway Timeout");
      } else {
         Map<String, Variant> variants = this.getExistingCacheVariants(target, request);
         return variants != null && variants.size() > 0
            ? this.negotiateResponseFromVariants(target, request, context, variants)
            : this.callBackend(target, request, context);
      }
   }

   private HttpCacheEntry satisfyFromCache(HttpHost target, HttpRequestWrapper request) {
      HttpCacheEntry entry = null;

      try {
         entry = this.responseCache.getCacheEntry(target, request);
      } catch (IOException var5) {
         this.log.warn("Unable to retrieve entries from cache", var5);
      }

      return entry;
   }

   private HttpResponse getFatallyNoncompliantResponse(HttpRequestWrapper request, HttpContext context) {
      HttpResponse fatalErrorResponse = null;

      for (RequestProtocolError error : this.requestCompliance.requestIsFatallyNonCompliant(request)) {
         this.setResponseStatus(context, CacheResponseStatus.CACHE_MODULE_RESPONSE);
         fatalErrorResponse = this.requestCompliance.getErrorForRequest(error);
      }

      return fatalErrorResponse;
   }

   private Map<String, Variant> getExistingCacheVariants(HttpHost target, HttpRequestWrapper request) {
      Map<String, Variant> variants = null;

      try {
         variants = this.responseCache.getVariantCacheEntriesWithEtags(target, request);
      } catch (IOException var5) {
         this.log.warn("Unable to retrieve variant entries from cache", var5);
      }

      return variants;
   }

   private void recordCacheMiss(HttpHost target, HttpRequestWrapper request) {
      this.cacheMisses.getAndIncrement();
      if (this.log.isTraceEnabled()) {
         RequestLine rl = request.getRequestLine();
         this.log.trace("Cache miss [host: " + target + "; uri: " + rl.getUri() + "]");
      }
   }

   private void recordCacheHit(HttpHost target, HttpRequestWrapper request) {
      this.cacheHits.getAndIncrement();
      if (this.log.isTraceEnabled()) {
         RequestLine rl = request.getRequestLine();
         this.log.trace("Cache hit [host: " + target + "; uri: " + rl.getUri() + "]");
      }
   }

   private void recordCacheUpdate(HttpContext context) {
      this.cacheUpdates.getAndIncrement();
      this.setResponseStatus(context, CacheResponseStatus.VALIDATED);
   }

   private void flushEntriesInvalidatedByRequest(HttpHost target, HttpRequestWrapper request) {
      try {
         this.responseCache.flushInvalidatedCacheEntriesFor(target, request);
      } catch (IOException var4) {
         this.log.warn("Unable to flush invalidated entries from cache", var4);
      }
   }

   private HttpResponse generateCachedResponse(HttpRequestWrapper request, HttpContext context, HttpCacheEntry entry, Date now) {
      HttpResponse cachedResponse;
      if (!request.containsHeader("If-None-Match") && !request.containsHeader("If-Modified-Since")) {
         cachedResponse = this.responseGenerator.generateResponse(request, entry);
      } else {
         cachedResponse = this.responseGenerator.generateNotModifiedResponse(entry);
      }

      this.setResponseStatus(context, CacheResponseStatus.CACHE_HIT);
      if (this.validityPolicy.getStalenessSecs(entry, now) > 0L) {
         cachedResponse.addHeader("Warning", "110 localhost \"Response is stale\"");
      }

      return cachedResponse;
   }

   private HttpResponse handleRevalidationFailure(HttpRequestWrapper request, HttpContext context, HttpCacheEntry entry, Date now) {
      return this.staleResponseNotAllowed(request, entry, now) ? this.generateGatewayTimeout(context) : this.unvalidatedCacheHit(request, context, entry);
   }

   private HttpResponse generateGatewayTimeout(HttpContext context) {
      this.setResponseStatus(context, CacheResponseStatus.CACHE_MODULE_RESPONSE);
      return new BasicHttpResponse(HttpVersion.HTTP_1_1, 504, "Gateway Timeout");
   }

   private HttpResponse unvalidatedCacheHit(HttpRequestWrapper request, HttpContext context, HttpCacheEntry entry) {
      HttpResponse cachedResponse = this.responseGenerator.generateResponse(request, entry);
      this.setResponseStatus(context, CacheResponseStatus.CACHE_HIT);
      cachedResponse.addHeader("Warning", "111 localhost \"Revalidation failed\"");
      return cachedResponse;
   }

   private boolean staleResponseNotAllowed(HttpRequestWrapper request, HttpCacheEntry entry, Date now) {
      return this.validityPolicy.mustRevalidate(entry)
         || this.isSharedCache() && this.validityPolicy.proxyRevalidate(entry)
         || this.explicitFreshnessRequest(request, entry, now);
   }

   private boolean mayCallBackend(HttpRequestWrapper request) {
      for (Header h : request.getHeaders("Cache-Control")) {
         for (HeaderElement elt : h.getElements()) {
            if ("only-if-cached".equals(elt.getName())) {
               this.log.trace("Request marked only-if-cached");
               return false;
            }
         }
      }

      return true;
   }

   private boolean explicitFreshnessRequest(HttpRequestWrapper request, HttpCacheEntry entry, Date now) {
      for (Header h : request.getHeaders("Cache-Control")) {
         for (HeaderElement elt : h.getElements()) {
            if ("max-stale".equals(elt.getName())) {
               try {
                  int maxstale = Integer.parseInt(elt.getValue());
                  long age = this.validityPolicy.getCurrentAgeSecs(entry, now);
                  long lifetime = this.validityPolicy.getFreshnessLifetimeSecs(entry);
                  if (age - lifetime > (long)maxstale) {
                     return true;
                  }
               } catch (NumberFormatException var17) {
                  return true;
               }
            } else if ("min-fresh".equals(elt.getName()) || "max-age".equals(elt.getName())) {
               return true;
            }
         }
      }

      return false;
   }

   private String generateViaHeader(HttpMessage msg) {
      ProtocolVersion pv = msg.getProtocolVersion();
      String existingEntry = this.viaHeaders.get(pv);
      if (existingEntry != null) {
         return existingEntry;
      } else {
         VersionInfo vi = VersionInfo.loadVersionInfo("org.apache.http.client", this.getClass().getClassLoader());
         String release = vi != null ? vi.getRelease() : "UNAVAILABLE";
         String value;
         if ("http".equalsIgnoreCase(pv.getProtocol())) {
            value = String.format("%d.%d localhost (Apache-HttpClient/%s (cache))", pv.getMajor(), pv.getMinor(), release);
         } else {
            value = String.format("%s/%d.%d localhost (Apache-HttpClient/%s (cache))", pv.getProtocol(), pv.getMajor(), pv.getMinor(), release);
         }

         this.viaHeaders.put(pv, value);
         return value;
      }
   }

   private void setResponseStatus(HttpContext context, CacheResponseStatus value) {
      if (context != null) {
         context.setAttribute("http.cache.response.status", value);
      }
   }

   public boolean supportsRangeAndContentRangeHeaders() {
      return false;
   }

   public boolean isSharedCache() {
      return this.sharedCache;
   }

   Date getCurrentDate() {
      return new Date();
   }

   boolean clientRequestsOurOptions(HttpRequest request) {
      RequestLine line = request.getRequestLine();
      if (!"OPTIONS".equals(line.getMethod())) {
         return false;
      } else if (!"*".equals(line.getUri())) {
         return false;
      } else {
         Header h = request.getFirstHeader("Max-Forwards");
         return "0".equals(h != null ? h.getValue() : null);
      }
   }

   HttpResponse callBackend(HttpHost target, HttpRequestWrapper request, HttpContext context) throws IOException {
      Date requestDate = this.getCurrentDate();
      this.log.trace("Calling the backend");
      HttpResponse backendResponse = this.backend.execute(target, request, context);
      backendResponse.addHeader("Via", this.generateViaHeader(backendResponse));
      return this.handleBackendResponse(target, request, requestDate, this.getCurrentDate(), backendResponse);
   }

   private boolean revalidationResponseIsTooOld(HttpResponse backendResponse, HttpCacheEntry cacheEntry) {
      Header entryDateHeader = cacheEntry.getFirstHeader("Date");
      Header responseDateHeader = backendResponse.getFirstHeader("Date");
      if (entryDateHeader != null && responseDateHeader != null) {
         try {
            Date entryDate = DateUtils.parseDate(entryDateHeader.getValue());
            Date respDate = DateUtils.parseDate(responseDateHeader.getValue());
            if (respDate.before(entryDate)) {
               return true;
            }
         } catch (DateParseException var7) {
         }
      }

      return false;
   }

   HttpResponse negotiateResponseFromVariants(HttpHost target, HttpRequestWrapper request, HttpContext context, Map<String, Variant> variants) throws IOException {
      HttpRequestWrapper conditionalRequest = this.conditionalRequestBuilder.buildConditionalRequestFromVariants(request, variants);
      Date requestDate = this.getCurrentDate();
      HttpResponse backendResponse = this.backend.execute(target, conditionalRequest, context);
      Date responseDate = this.getCurrentDate();
      backendResponse.addHeader("Via", this.generateViaHeader(backendResponse));
      if (backendResponse.getStatusLine().getStatusCode() != 304) {
         return this.handleBackendResponse(target, request, requestDate, responseDate, backendResponse);
      } else {
         Header resultEtagHeader = backendResponse.getFirstHeader("ETag");
         if (resultEtagHeader == null) {
            this.log.warn("304 response did not contain ETag");
            return this.callBackend(target, request, context);
         } else {
            String resultEtag = resultEtagHeader.getValue();
            Variant matchingVariant = variants.get(resultEtag);
            if (matchingVariant == null) {
               this.log.debug("304 response did not contain ETag matching one sent in If-None-Match");
               return this.callBackend(target, request, context);
            } else {
               HttpCacheEntry matchedEntry = matchingVariant.getEntry();
               if (this.revalidationResponseIsTooOld(backendResponse, matchedEntry)) {
                  IOUtils.consume(backendResponse.getEntity());
                  return this.retryRequestUnconditionally(target, request, context, matchedEntry);
               } else {
                  this.recordCacheUpdate(context);
                  HttpCacheEntry responseEntry = this.getUpdatedVariantEntry(
                     target, conditionalRequest, requestDate, responseDate, backendResponse, matchingVariant, matchedEntry
                  );
                  HttpResponse resp = this.responseGenerator.generateResponse(request, responseEntry);
                  this.tryToUpdateVariantMap(target, request, matchingVariant);
                  return (HttpResponse)(this.shouldSendNotModifiedResponse(request, responseEntry)
                     ? this.responseGenerator.generateNotModifiedResponse(responseEntry)
                     : resp);
               }
            }
         }
      }
   }

   private HttpResponse retryRequestUnconditionally(HttpHost target, HttpRequestWrapper request, HttpContext context, HttpCacheEntry matchedEntry) throws IOException {
      HttpRequestWrapper unconditional = this.conditionalRequestBuilder.buildUnconditionalRequest(request, matchedEntry);
      return this.callBackend(target, unconditional, context);
   }

   private HttpCacheEntry getUpdatedVariantEntry(
      HttpHost target,
      HttpRequestWrapper conditionalRequest,
      Date requestDate,
      Date responseDate,
      HttpResponse backendResponse,
      Variant matchingVariant,
      HttpCacheEntry matchedEntry
   ) {
      HttpCacheEntry responseEntry = matchedEntry;

      try {
         responseEntry = this.responseCache
            .updateVariantCacheEntry(target, conditionalRequest, matchedEntry, backendResponse, requestDate, responseDate, matchingVariant.getCacheKey());
      } catch (IOException var10) {
         this.log.warn("Could not update cache entry", var10);
      }

      return responseEntry;
   }

   private void tryToUpdateVariantMap(HttpHost target, HttpRequestWrapper request, Variant matchingVariant) {
      try {
         this.responseCache.reuseVariantEntryFor(target, request, matchingVariant);
      } catch (IOException var5) {
         this.log.warn("Could not update cache entry to reuse variant", var5);
      }
   }

   private boolean shouldSendNotModifiedResponse(HttpRequestWrapper request, HttpCacheEntry responseEntry) {
      return this.suitabilityChecker.isConditional(request) && this.suitabilityChecker.allConditionalsMatch(request, responseEntry, new Date());
   }

   HttpResponse revalidateCacheEntry(HttpHost target, HttpRequestWrapper request, HttpContext context, HttpCacheEntry cacheEntry) throws IOException, ProtocolException {
      HttpRequestWrapper conditionalRequest = this.conditionalRequestBuilder.buildConditionalRequest(request, cacheEntry);
      Date requestDate = this.getCurrentDate();
      HttpResponse backendResponse = this.backend.execute(target, conditionalRequest, context);
      Date responseDate = this.getCurrentDate();
      if (this.revalidationResponseIsTooOld(backendResponse, cacheEntry)) {
         IOUtils.consume(backendResponse.getEntity());
         HttpRequest unconditional = this.conditionalRequestBuilder.buildUnconditionalRequest(request, cacheEntry);
         requestDate = this.getCurrentDate();
         backendResponse = this.backend.execute(target, unconditional, context);
         responseDate = this.getCurrentDate();
      }

      backendResponse.addHeader("Via", this.generateViaHeader(backendResponse));
      int statusCode = backendResponse.getStatusLine().getStatusCode();
      if (statusCode == 304 || statusCode == 200) {
         this.recordCacheUpdate(context);
      }

      if (statusCode == 304) {
         HttpCacheEntry updatedEntry = this.responseCache.updateCacheEntry(target, request, cacheEntry, backendResponse, requestDate, responseDate);
         return this.suitabilityChecker.isConditional(request) && this.suitabilityChecker.allConditionalsMatch(request, updatedEntry, new Date())
            ? this.responseGenerator.generateNotModifiedResponse(updatedEntry)
            : this.responseGenerator.generateResponse(request, updatedEntry);
      } else if (this.staleIfErrorAppliesTo(statusCode)
         && !this.staleResponseNotAllowed(request, cacheEntry, this.getCurrentDate())
         && this.validityPolicy.mayReturnStaleIfError(request, cacheEntry, responseDate)) {
         HttpResponse cachedResponse = this.responseGenerator.generateResponse(request, cacheEntry);
         cachedResponse.addHeader("Warning", "110 localhost \"Response is stale\"");
         HttpEntity errorBody = backendResponse.getEntity();
         if (errorBody != null) {
            IOUtils.consume(errorBody);
         }

         return cachedResponse;
      } else {
         return this.handleBackendResponse(target, conditionalRequest, requestDate, responseDate, backendResponse);
      }
   }

   private boolean staleIfErrorAppliesTo(int statusCode) {
      return statusCode == 500 || statusCode == 502 || statusCode == 503 || statusCode == 504;
   }

   HttpResponse handleBackendResponse(HttpHost target, HttpRequestWrapper request, Date requestDate, Date responseDate, HttpResponse backendResponse) throws IOException {
      this.log.trace("Handling Backend response");
      this.responseCompliance.ensureProtocolCompliance(request, backendResponse);
      boolean cacheable = this.responseCachingPolicy.isResponseCacheable(request, backendResponse);
      this.responseCache.flushInvalidatedCacheEntriesFor(target, request, backendResponse);
      if (cacheable && !this.alreadyHaveNewerCacheEntry(target, request, backendResponse)) {
         try {
            this.storeRequestIfModifiedSinceFor304Response(request, backendResponse);
            return this.responseCache.cacheAndReturnResponse(target, request, backendResponse, requestDate, responseDate);
         } catch (IOException var9) {
            this.log.warn("Unable to store entries in cache", var9);
         }
      }

      if (!cacheable) {
         try {
            this.responseCache.flushCacheEntriesFor(target, request);
         } catch (IOException var8) {
            this.log.warn("Unable to flush invalid cache entries", var8);
         }
      }

      return backendResponse;
   }

   private void storeRequestIfModifiedSinceFor304Response(HttpRequest request, HttpResponse backendResponse) {
      if (backendResponse.getStatusLine().getStatusCode() == 304) {
         Header h = request.getFirstHeader("If-Modified-Since");
         if (h != null) {
            backendResponse.addHeader("Last-Modified", h.getValue());
         }
      }
   }

   private boolean alreadyHaveNewerCacheEntry(HttpHost target, HttpRequest request, HttpResponse backendResponse) {
      HttpCacheEntry existing = null;

      try {
         existing = this.responseCache.getCacheEntry(target, request);
      } catch (IOException var10) {
      }

      if (existing == null) {
         return false;
      } else {
         Header entryDateHeader = existing.getFirstHeader("Date");
         if (entryDateHeader == null) {
            return false;
         } else {
            Header responseDateHeader = backendResponse.getFirstHeader("Date");
            if (responseDateHeader == null) {
               return false;
            } else {
               try {
                  Date entryDate = DateUtils.parseDate(entryDateHeader.getValue());
                  Date responseDate = DateUtils.parseDate(responseDateHeader.getValue());
                  return responseDate.before(entryDate);
               } catch (DateParseException var9) {
                  return false;
               }
            }
         }
      }
   }

   static class AsynchronousValidationRequest implements Runnable {
      private final CachingHttpClient.AsynchronousValidator parent;
      private final CachingHttpClient cachingClient;
      private final HttpHost target;
      private final HttpRequestWrapper request;
      private final HttpContext context;
      private final HttpCacheEntry cacheEntry;
      private final String identifier;
      private final Log log = LogFactory.getLog(this.getClass());

      AsynchronousValidationRequest(
         CachingHttpClient.AsynchronousValidator parent,
         CachingHttpClient cachingClient,
         HttpHost target,
         HttpRequestWrapper request,
         HttpContext context,
         HttpCacheEntry cacheEntry,
         String identifier
      ) {
         this.parent = parent;
         this.cachingClient = cachingClient;
         this.target = target;
         this.request = request;
         this.context = context;
         this.cacheEntry = cacheEntry;
         this.identifier = identifier;
      }

      @Override
      public void run() {
         try {
            this.cachingClient.revalidateCacheEntry(this.target, this.request, this.context, this.cacheEntry);
         } catch (IOException var6) {
            this.log.debug("Asynchronous revalidation failed due to exception: " + var6);
         } catch (ProtocolException var7) {
            this.log.error("ProtocolException thrown during asynchronous revalidation: " + var7);
         } finally {
            this.parent.markComplete(this.identifier);
         }
      }

      String getIdentifier() {
         return this.identifier;
      }
   }

   static class AsynchronousValidator {
      private final CachingHttpClient cachingClient;
      private final ExecutorService executor;
      private final Set<String> queued;
      private final CacheKeyGenerator cacheKeyGenerator;
      private final Log log = LogFactory.getLog(this.getClass());

      public AsynchronousValidator(CachingHttpClient cachingClient, CacheConfig config) {
         this(
            cachingClient,
            new ThreadPoolExecutor(
               config.getAsynchronousWorkersCore(),
               config.getAsynchronousWorkersMax(),
               (long)config.getAsynchronousWorkerIdleLifetimeSecs(),
               TimeUnit.SECONDS,
               new ArrayBlockingQueue<>(config.getRevalidationQueueSize())
            )
         );
      }

      AsynchronousValidator(CachingHttpClient cachingClient, ExecutorService executor) {
         this.cachingClient = cachingClient;
         this.executor = executor;
         this.queued = new HashSet<>();
         this.cacheKeyGenerator = new CacheKeyGenerator();
      }

      public synchronized void revalidateCacheEntry(HttpHost target, HttpRequestWrapper request, HttpContext context, HttpCacheEntry entry) {
         String uri = this.cacheKeyGenerator.getVariantURI(target, request, entry);
         if (!this.queued.contains(uri)) {
            CachingHttpClient.AsynchronousValidationRequest revalidationRequest = new CachingHttpClient.AsynchronousValidationRequest(
               this, this.cachingClient, target, request, context, entry, uri
            );

            try {
               this.executor.execute(revalidationRequest);
               this.queued.add(uri);
            } catch (RejectedExecutionException var8) {
               this.log.debug("Revalidation for [" + uri + "] not scheduled: " + var8);
            }
         }
      }

      synchronized void markComplete(String identifier) {
         this.queued.remove(identifier);
      }

      Set<String> getScheduledIdentifiers() {
         return Collections.unmodifiableSet(this.queued);
      }

      ExecutorService getExecutor() {
         return this.executor;
      }
   }
}
