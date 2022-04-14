package com.example.cache;

import lombok.Getter;

import java.lang.ref.SoftReference;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;
import java.util.*;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Function;

/**
 * @author vishnu.g
 */
public class InMemoryCache<K, V> implements Cache<K, V> {

    private static final TemporalUnit FALL_BACK_EXPIRY_UNIT = ChronoUnit.MILLIS;
    // Function to load value for cache miss.
    private final Function<K, V> valueLoader;
    // Actual Map to keep cache.
    private final Map<K, SoftReference<V>> cache;
    // Lock for concurrency
    private final ReadWriteLock readWriteLock;
    // Holds the map keys using the given lifetime for expiration.
    private final DelayQueue<DelayedCacheKey<K>> delayQueue;
    // Holds delayedKey object for particular key for accessing.
    private final Map<K, DelayedCacheKey<K>> expiringKeys;
    // The default max lifetime in milliseconds.
    private final long defaultExpiryAfter;
    // The default unit of date-time.
    private final TemporalUnit defaultExpiryUnit;


    private InMemoryCache(Map<K, SoftReference<V>> cache, Function<K, V> valueLoader,
                          long defaultExpiryAfter, TemporalUnit defaultExpiryUnit) {
        this.cache = cache;
        this.valueLoader = valueLoader;
        this.readWriteLock = new ReentrantReadWriteLock();
        this.delayQueue = new DelayQueue<>();
        this.expiringKeys = new HashMap<>();
        this.defaultExpiryAfter = defaultExpiryAfter;
        this.defaultExpiryUnit = (null != defaultExpiryUnit) ? defaultExpiryUnit : FALL_BACK_EXPIRY_UNIT;
    }

    /**
     * Create a new cache builder.
     *
     * @param <K> the key type
     * @param <V> the value type
     * @return a new instance of cache builder
     */
    public static <K, V> CacheBuilder<K, V> builder() {
        return new CacheBuilder<>();
    }

    @Override
    public void put(K key, V value, long ttl) {
        Objects.requireNonNull(key);
        readWriteLock.writeLock().lock();
        try {
            doCleanup();
            doPutValue(key, value, Optional.of(ttl));
        } finally {
            readWriteLock.writeLock().unlock();
        }
    }

    @Override
    public void put(K key, V value) {
        Objects.requireNonNull(key);
        readWriteLock.writeLock().lock();
        try {
            doCleanup();
            doPutValue(key, value, Optional.empty());
        } finally {
            readWriteLock.writeLock().unlock();
        }
    }

    @Override
    public V remove(K key) {
        Objects.requireNonNull(key);
        readWriteLock.writeLock().lock();
        try {
            doCleanup();
            delayQueue.remove(new DelayedCacheKey<>(key));
            expiringKeys.remove(key);
            return this.cache.remove(key).get();
        } finally {
            readWriteLock.writeLock().unlock();
        }
    }

    @Override
    public V get(K key) {
        return doGetValue(key, true);
    }

    @Override
    public void clear() {
        readWriteLock.writeLock().lock();
        try {
            this.cache.clear();
            this.delayQueue.clear();
            this.expiringKeys.clear();
        } finally {
            readWriteLock.writeLock().unlock();
        }
    }

    @Override
    public boolean isEmpty() {
        return this.cache.isEmpty();
    }

    @Override
    public long size() {
        // TODO : need to cleanup before finding actual size.
        return this.cache.size();
    }

    @Override
    @Deprecated
    public int capacity() {
        return 0;
    }

    private V doGetValue(K key, boolean loadIfAbsent) {
        Objects.requireNonNull(key);
        readWriteLock.readLock().lock();
        try {
            doCleanup();
            V value = getReferenceValue(key);

            if (value == null && loadIfAbsent && valueLoader != null) { // cache miss
                readWriteLock.readLock().unlock();// must release read lock before acquiring write lock
                readWriteLock.writeLock().lock();
                try {// recheck state because another thread might have
                    // acquired write lock and changed state before we did.
                    value = getReferenceValue(key);
                    if (value == null) { // not present in the cache
                        value = valueLoader.apply(key);
                        doPutValue(key, value, Optional.empty());
                    }
                } finally { // downgrade by acquiring read lock before releasing write lock
                    readWriteLock.readLock().lock();
                    readWriteLock.writeLock().unlock(); // unlock write, still hold read
                }
            } else {
                renewKey(key);
            }
            return value;
        } finally {
            readWriteLock.readLock().unlock();
        }
    }

    private SoftReference<V> doPutValue(K key, V value, Optional<Long> ttl) {

        long timeToLeave = (ttl.isPresent()) ? ttl.get() : defaultExpiryAfter;
        if (timeToLeave > 0) {
            DelayedCacheKey<K> delayedKey = new DelayedCacheKey<>(key, timeToLeave, defaultExpiryUnit);
            //  previous value associated with key, or null if there was no mapping for key
            DelayedCacheKey<K> oldKey = expiringKeys.put(key, delayedKey);
            if (null != oldKey) {
                delayQueue.remove(oldKey);
            }
            delayQueue.offer(delayedKey);
        }
        return this.cache.put(key, new SoftReference<>(value));
    }

    private boolean renewKey(K key) {
        // TODO : need to check
        DelayedCacheKey<K> delayedKey = expiringKeys.get(key);
        if (null != delayedKey) {
            delayedKey.renew();
            return true;
        }
        return false;
    }

    /**
     * Get actual value from cache wrapped in {@code SoftReference} object.
     *
     * @param key the key
     * @return V the actual value
     */
    private V getReferenceValue(K key) {
        return Optional.ofNullable(this.cache.get(key)).map(SoftReference::get).orElse(null);
    }

    /**
     * Clean up cache and queue w.r.t ttl.
     */
    private void doCleanup() {
        DelayedCacheKey<K> delayedKey = delayQueue.poll();

        while (null != delayedKey) {
           // System.out.println("--- " + delayedKey.getKey());
            this.cache.remove(delayedKey.getKey());
            this.expiringKeys.remove(delayedKey.getKey());
            delayedKey = delayQueue.poll();
        }
    }

    private static class DelayedCacheKey<K> implements Delayed {
        @Getter
        private final K key;
        private long expireAfter;
        private TemporalUnit expiryUnit;
        private Instant startTime;

        public DelayedCacheKey(K key) {
            this.key = key;
        }

        public DelayedCacheKey(K key, long expireAfter, TemporalUnit expiryUnit) {
            this(key);
            this.expiryUnit = expiryUnit;
            this.startTime = Instant.now();
            this.expireAfter = expireAfter;
        }

        public void renew() {
            this.startTime = Instant.now();
        }

        @Override
        public long getDelay(TimeUnit timeUnit) {
            long diff = startTime == null ? 0 : Duration.between(Instant.now(),
                    startTime.plus(expireAfter, expiryUnit)).toMillis();
            return timeUnit.convert(diff, TimeUnit.MILLISECONDS);
        }

        @Override
        public int compareTo(Delayed that) {
            return Long.compare(this.getDelay(TimeUnit.NANOSECONDS), that.getDelay(TimeUnit.NANOSECONDS));
        }
    }

    /**
     * A simple cache builder.
     */
    public static final class CacheBuilder<K, V> {
        private int initialCapacity = -1;
        private long maximumSize = -1;
        private long defaultExpiryAfter = 0;
        private TemporalUnit defaultExpiryUnit;

        /**
         * Sets the minimum total size for the internal hash tables.
         *
         * @param initialCapacity the initial capacity
         * @return {@code this} instance to support method chaining
         * @throws IllegalArgumentException if {@code initialCapacity} is negative
         */
        public CacheBuilder<K, V> initialCapacity(int initialCapacity) throws IllegalArgumentException {
            if (initialCapacity < 0) {
                throw new IllegalArgumentException("initialCapacity should be >= 0");
            }
            this.initialCapacity = initialCapacity;
            return this;
        }

        /**
         * Sets the maximum total size for the internal hash tables.
         *
         * @param maximumSize the maximum size
         * @return {@code this} instance to support method chaining
         * @throws IllegalArgumentException if {@code maximumSize} is zero or negative
         */
        public CacheBuilder<K, V> maximumSize(long maximumSize) throws IllegalArgumentException {
            if (maximumSize <= 0) {
                throw new IllegalArgumentException("maximumSize should be greater than zero");
            }
            this.maximumSize = maximumSize;
            return this;
        }

        /**
         * Sets the default time-to-live, in the given unit, for all keys in this cache.
         *
         * @param expiryAfter the time to live period
         * @param expiryUnit  the temporal unit of the expiry amount
         * @return {@code this} instance to support method chaining
         * @throws IllegalArgumentException if {@code expiryAfter} is zero or negative
         */
        public CacheBuilder<K, V> expireAfter(long expiryAfter, TemporalUnit expiryUnit) throws IllegalArgumentException {
            if (expiryAfter <= 0) {
                throw new IllegalArgumentException("value for expiryAfter should be greater than zero");
            }
            this.defaultExpiryAfter = expiryAfter;
            this.defaultExpiryUnit = Objects.requireNonNull(expiryUnit);
            return this;
        }

        /**
         * Build a new instance of the {@link InMemoryCache}.
         *
         * @param <K1> the key type
         * @param <V1> the value type
         * @return a new instance of the cache
         */
        public <K1 extends K, V1 extends V> InMemoryCache<K1, V1> build() {
            return build(null);
        }

        /**
         * Build a new instance of the {@link InMemoryCache} with all configured parameters and value loader.
         *
         * @param valueLoader the value loader
         * @param <K1>        the key type
         * @param <V1>        the value type
         * @return a new instance of the cache
         */
        public <K1 extends K, V1 extends V> InMemoryCache<K1, V1> build(Function<K1, V1> valueLoader) {
            initialCapacity = Math.max(initialCapacity, 0);
            Map<K1, SoftReference<V1>> cacheMap;
            if (maximumSize > 0) {
                // LinkedHashMap as LRU map which uses access order instead of insertion order
                cacheMap = new LinkedHashMap<>(initialCapacity, 0.75f, true) {
                    @Override
                    protected boolean removeEldestEntry(Map.Entry<K1, SoftReference<V1>> eldest) {
                        // remove the eldest entry when map size exceeds the maximum allowed limit
                        return size() > maximumSize;
                    }
                };
            } else {
                cacheMap = new HashMap<>(initialCapacity);
            }
            return new InMemoryCache<>(cacheMap, valueLoader, defaultExpiryAfter, defaultExpiryUnit);
        }
    }
}
