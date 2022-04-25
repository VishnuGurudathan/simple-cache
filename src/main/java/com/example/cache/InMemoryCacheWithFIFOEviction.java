package com.example.cache;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.io.Serializable;
import java.lang.ref.SoftReference;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * @author vishnu.g
 */
public class InMemoryCacheWithFIFOEviction <K, V> implements Cache<K, V>, Serializable {

    private static final long serialVersionUID = -162114643488955218L;

    protected static final int DEFAULT_MAX_SIZE = 100;
    // in milliseconds
    private static final int DEFAULT_TTL = 1000;
    private final transient int initialCapacity;

    private final LinkedHashMap<K, SoftReference<Object>> cache;
    private final DelayQueue<DelayedCacheObject> cleaningUpQueue = new DelayQueue<>();
    private final ReadWriteLock readWriteLock = new ReentrantReadWriteLock();

    public InMemoryCacheWithFIFOEviction(int capacity) {
        this.initialCapacity = capacity;
        cache = new LinkedHashMap<K, SoftReference<Object>>(capacity, 0.75f, false) {
            protected boolean removeEldestEntry(Map.Entry eldest) {
                return size() > capacity;
            }
        };
        Thread cleanerThread = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    DelayedCacheObject delayedCacheObject = cleaningUpQueue.take();
                    cache.remove(delayedCacheObject.getKey(), delayedCacheObject.getReference());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        });
        cleanerThread.setDaemon(true);
        cleanerThread.start();

    }

    public InMemoryCacheWithFIFOEviction() {
        this(DEFAULT_MAX_SIZE);
    }

    @Override
    public void put(K key, V value, long periodInMillis) {
        if (key == null) {
            return;
        }
        readWriteLock.writeLock().lock();
        try {
            if (value == null) {
                cache.remove(key);
            } else {
                long expiryTime = System.currentTimeMillis() + periodInMillis;
                SoftReference<Object> reference = new SoftReference<>(value);
                cache.put(key, reference);
                cleaningUpQueue.put(new DelayedCacheObject(key, reference, expiryTime));
            }
        } finally {
            readWriteLock.writeLock().unlock();
        }

    }

    @Override
    public void put(K key, V value) {
        put(key, value, DEFAULT_TTL);
    }

    @Override
    public V remove(K key) {
        readWriteLock.writeLock().lock();
        try {
            return (V) this.cache.remove(key).get();
        } finally {
            readWriteLock.writeLock().unlock();
        }
    }

    @Override
    public V get(K key) {
        readWriteLock.readLock().lock();
        try {
            return (V) Optional.ofNullable(cache.get(key)).map(SoftReference::get).orElse(null);
            // need to renew. now for simplicity key is not renewed.
        } finally {
            readWriteLock.readLock().unlock();
        }
    }

    @Override
    public void clear() {
        readWriteLock.writeLock().lock();
        try {
            cache.clear();
            cleaningUpQueue.clear();
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
        return this.cache.size();
    }

    @Override
    public int capacity() {
        return this.initialCapacity;
    }

    @AllArgsConstructor
    @EqualsAndHashCode
    private static class DelayedCacheObject<K> implements Delayed {

        @Getter
        private final K key;
        @Getter
        private final SoftReference<Object> reference;
        private final long expiryTime;

        @Override
        public long getDelay(TimeUnit unit) {
            return unit.convert(expiryTime - System.currentTimeMillis(), TimeUnit.MILLISECONDS);
        }

        @Override
        public int compareTo(Delayed o) {
            return Long.compare(expiryTime, ((DelayedCacheObject) o).expiryTime);
        }
    }
}
