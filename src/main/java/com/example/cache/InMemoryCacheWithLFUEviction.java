package com.example.cache;

import lombok.Getter;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * @author vishnu.g
 */
public class InMemoryCacheWithLFUEviction<K, V> implements Cache<K, V>, Serializable {

    private static final long serialVersionUID = -162114643488955218L;

    protected static final int DEFAULT_MAX_SIZE = 100;
    // in milliseconds
    private static final int DEFAULT_TTL = 1000;
    private final transient int initialCapacity;

    private final LinkedHashMap<K, CacheEntry<V>> cache;
    private final ReadWriteLock readWriteLock = new ReentrantReadWriteLock();

    /**
     * Clean up interval is multiple of 1sec, by default is 1 sec.
     */
    public InMemoryCacheWithLFUEviction() {
        this(DEFAULT_MAX_SIZE);
    }

    // TODO : validate arguments

    /**
     * Clean up interval is multiple of 1sec, by default is 1 sec.
     * @param initialCapacity
     */
    public InMemoryCacheWithLFUEviction(int initialCapacity) {
        this(1L, initialCapacity);
    }

    /**
     * Clean up interval is multiple of 1sec
     * @param initialCapacity
     */
    public InMemoryCacheWithLFUEviction(final long timerInterval, int initialCapacity) {

        this.initialCapacity = initialCapacity;
        cache = new LinkedHashMap<>(initialCapacity);

        if (timerInterval > 0) {

            Thread t = new Thread(new Runnable() {
                public void run() {
                    while (true) {
                        try {
                            Thread.sleep(timerInterval * 1000);
                        } catch (InterruptedException ex) {
                            ex.printStackTrace();
                        }
                        cleanup();
                    }
                }
            });

            t.setDaemon(true);
            t.start();
        }
    }

    @Override
    public void put(K key, V value, long ttl) {
        if (null == key) {
            return;
        }
        readWriteLock.writeLock().lock();
        try {
            if (null == value) {
                this.cache.remove(key);
                return;
            }
            if (isFull()) {
                K entryKeyToBeRemoved = getLFUKey();
                cache.remove(entryKeyToBeRemoved);
            }
            CacheEntry entry = new CacheEntry(value, ttl);
            cache.put(key, entry);

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
            return this.cache.remove(key).getValue();
        } finally {
            readWriteLock.writeLock().unlock();
        }
    }

    @Override
    public V get(K key) {
        readWriteLock.readLock().lock();
        try {
            if (this.cache.containsKey(key))  // cache hit
            {
                CacheEntry entry = this.cache.get(key);
                entry.frequency++;
                this.cache.put(key, entry);
                return (V) entry.getValue();
            }
        } finally {
            readWriteLock.readLock().unlock();
        }
        return null; // cache miss
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
        return this.cache.size();
    }

    @Override
    public int capacity() {
        return this.initialCapacity;
    }

    private K getLFUKey() {
        K key = null;
        int minFreq = Integer.MAX_VALUE;

        for (Map.Entry<K, CacheEntry<V>> entry : this.cache.entrySet()) {
            if (minFreq > entry.getValue().getFrequency()) {
                key = entry.getKey();
                minFreq = entry.getValue().getFrequency();
            }
        }

        return key;
    }

    private boolean isFull() {
        return this.cache.size() == initialCapacity;
    }

    private void cleanup() {

        long now = System.currentTimeMillis();
        ArrayList<K> deleteKey = null;

        CacheEntry c = null;
        synchronized (cache) {
            deleteKey = new ArrayList<K>((this.cache.size() / 2) + 1);
            for (Map.Entry<K, CacheEntry<V>> entry : this.cache.entrySet()) {
                c = entry.getValue();

                if (c != null && (now > (c.timeToLive + c.lastAccessed))) {

                    deleteKey.add(entry.getKey());
                }
            }
        }

        for (K key : deleteKey) {
            synchronized (cache) {
                this.cache.remove(key);
            }
            Thread.yield();
        }
    }

    @Getter
    protected class CacheEntry<V> {
        public long lastAccessed = System.currentTimeMillis();
        // in sec
        public long timeToLive;
        private final V value;
        private int frequency;

        protected CacheEntry(V value, long ttl) {
            this(value, 0, ttl);
        }

        protected CacheEntry(V value, int frequency, long ttl) {
            this.value = value;
            this.timeToLive = ttl ;//* 1000;
            this.frequency = 0;
        }
    }
}
