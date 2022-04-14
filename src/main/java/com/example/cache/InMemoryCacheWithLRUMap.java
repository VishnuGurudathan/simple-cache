package com.example.cache;

import org.apache.commons.collections4.MapIterator;
import org.apache.commons.collections4.map.LRUMap;

import java.util.ArrayList;

/**
 * @author vishnu.g
 */
public class InMemoryCacheWithLRUMap<K, V> implements Cache<K, V> {

    protected static final int DEFAULT_MAX_SIZE = 100;
    private final LRUMap cacheMap;

    public InMemoryCacheWithLRUMap() {
        this(1L, DEFAULT_MAX_SIZE);
    }

    public InMemoryCacheWithLRUMap(final long timerInterval, int capacity) {

        cacheMap = new LRUMap(capacity);

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

    public void put(K key, V value, long ttl) {
        synchronized (cacheMap) {

            cacheMap.put(key, new CacheObject(value, ttl));
        }
    }

    @Override
    public void put(K key, V value) {
        // TODO : to be implemented
    }

    public void add(K key, V value) {
        put(key, value, 1L);
    }

    public V get(K key) {
        synchronized (cacheMap) {
            CacheObject c;
            c = (CacheObject) cacheMap.get(key);

            if (c == null)
                return null;
            else {
                c.lastAccessed = System.currentTimeMillis();
                return c.value;
            }
        }
    }

    @Override
    public void clear() {
        this.cacheMap.clear();
    }

    @Override
    public boolean isEmpty() {
        return this.cacheMap.isEmpty();
    }

    public V remove(K key) {
        synchronized (cacheMap) {
            return (V) this.cacheMap.remove(key);
        }
    }

    public long size() {
        synchronized (cacheMap) {
            return cacheMap.size();
        }
    }

    @Override
    public int capacity() {
        return 0;
    }

    private void cleanup() {

        long now = System.currentTimeMillis();
        ArrayList<K> deleteKey = null;

        synchronized (cacheMap) {
            MapIterator itr = cacheMap.mapIterator();

            deleteKey = new ArrayList<K>((cacheMap.size() / 2) + 1);
            K key = null;
            CacheObject c = null;

            while (itr.hasNext()) {
                key = (K) itr.next();
                c = (CacheObject) itr.getValue();

                if (c != null && (now > (c.timeToLive + c.lastAccessed))) {

                    deleteKey.add(key);
                }
            }
        }

        for (K key : deleteKey) {
            synchronized (cacheMap) {

                cacheMap.remove(key);
            }
            Thread.yield();
        }
    }

    protected class CacheObject {

        public long lastAccessed = System.currentTimeMillis();
        // in sec
        public long timeToLive;
        public V value;

        protected CacheObject(V value, long ttl) {
            this.value = value;
            this.timeToLive = ttl * 1000;
        }
    }
}
