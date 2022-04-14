package com.example.cache;

/**
 * @author vishnu.g
 */
public interface Cache<K, V> {

    /**
     * Put the given key and value into cache.
     *
     * @param key   the key
     * @param value the value
     * @param ttl   time-to-leave in millisecond
     * @return a previously associated value, if it was present; can be {@code null}
     */
    void put(K key, V value, long ttl);

    /**
     * Put the given key and value into cache.
     *
     * @param key   the key
     * @param value the value
     * @return a previously associated value, if it was present; can be {@code null}
     */
    void put(K key, V value);

    /**
     * Remove the key and value from the cache, if present.
     *
     * @param key the key
     * @return the removed value if present; can be {@code null}
     */
    V remove(K key);

    /**
     * Get the value in the cache for the given key. If the value is {@code null}, it tries to load it from
     * the @{code valueLoader} if configured.
     *
     * @param key the key
     * @return the value from the cache or from value loader
     */
    V get(K key);

    /**
     * Clear all key-value entries from this cache.
     */
    void clear();

    /**
     * Check if cache is empty.
     *
     * @return {@code true} if cache is empty else return {@code false}
     */
    boolean isEmpty();

    /**
     * Returns the number of elements in the cache, not to be confused with
     * the {@link #capacity()} which returns the number
     * of elements that can be held in the cache at one time.
     * <p>
     *
     * @return The current size of the cache (i.e., the number of elements
     * currently cached).
     */
    long size();

    /**
     * Returns the maximum number of elements that can be cached at one time.
     * <p>
     *
     * @return The maximum number of elements that can be cached at one time.
     */
    int capacity();
}
