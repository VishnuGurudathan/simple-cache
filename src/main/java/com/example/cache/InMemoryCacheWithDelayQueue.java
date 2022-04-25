package com.example.cache;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.io.Serializable;
import java.lang.ref.SoftReference;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

/**
 * InMemory cache without cache eviction policy.
 * @author vishnu.g
 */
public class InMemoryCacheWithDelayQueue<K, V> implements Cache<K, V>, Serializable {
    private static final long serialVersionUID = -162114643488955218L;

    private final ConcurrentHashMap<K, SoftReference<Object>> cache = new ConcurrentHashMap<>();
    private final DelayQueue<DelayedCacheObject> cleaningUpQueue = new DelayQueue<>();
    private transient int maxSize;
    private static final int DEFAULT_TTL = 1000;

    public InMemoryCacheWithDelayQueue() {
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

    @Override
    public void put(K key, V value, long periodInMillis) {
        if (key == null) {
            return;
        }
        if (value == null) {
            this.cache.remove(key);
        } else {
            long expiryTime = System.currentTimeMillis() + periodInMillis;
            SoftReference<Object> reference = new SoftReference<>(value);
            this.cache.put(key, reference);
            cleaningUpQueue.put(new DelayedCacheObject(key, reference, expiryTime));
        }
    }

    @Override
    public void put(K key, V value) {
        put(key, value, DEFAULT_TTL);
    }

    @Override
    public V remove(K key) {
        return (V) this.cache.remove(key).get();
    }

    @Override
    public V get(K key) {
        return (V) Optional.ofNullable(this.cache.get(key)).map(SoftReference::get).orElse(null);
    }

    @Override
    public void clear() {
        this.cache.clear();
    }

    @Override
    public boolean isEmpty() {
        return this.cache.isEmpty();
    }

    @Override
    public long size() {
        return cache.size();
    }

    @Override
    public int capacity() {
        return 0;
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
