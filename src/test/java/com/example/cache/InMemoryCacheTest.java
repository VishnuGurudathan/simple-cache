package com.example.cache;

import org.junit.Test;

import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author vishnu.g
 */
public class InMemoryCacheTest {

    @Test
    public void testWithValueLoader() {
        InMemoryCache<String, String> simpleCache = InMemoryCache.builder().build(s -> UUID.randomUUID().toString());
        simpleCache.put("1", "one");
        assertEquals(simpleCache.get("1"), "one");
        simpleCache.put("1", "ONE");
        assertEquals(simpleCache.get("1"), "ONE");
        simpleCache.put("2", "TWO");
        assertNotNull(simpleCache.get("3")); // from value loader
        assertEquals(simpleCache.size(), 3);
        assertNotNull(simpleCache.remove("3"));
        assertEquals(simpleCache.size(), 2);
        simpleCache.clear();
    }
    @Test
    public void testWithOutValueLoader() {
        InMemoryCache<String, String> simpleCache = InMemoryCache.builder().build();
        assertNull(simpleCache.get("3")); // from value loader
        simpleCache.clear();
    }

    @Test
    public void testLRUEviction() {
        InMemoryCache<String, String> simpleCache = InMemoryCache.builder().initialCapacity(4).maximumSize(4).build();
        simpleCache.put("1", "one");
        simpleCache.put("2", "two");
        simpleCache.put("3", "three");
        simpleCache.put("4", "four");
        assertEquals(simpleCache.get("1"), "one"); // access the 1st key here
        simpleCache.put("5", "five");
        assertNull(simpleCache.get("2")); // key 'two' should not be present
        assertEquals(simpleCache.size(), 4);
        simpleCache.clear();
    }

    @Test
    public void testCacheKeyExpiry() throws InterruptedException {
        InMemoryCache<String, String> simpleCache = InMemoryCache.builder()
                .expireAfter(500, ChronoUnit.MILLIS).build();
        simpleCache.put("1", "one");
        Thread.sleep(100);
        simpleCache.put("2", "two");
        Thread.sleep(100);
        simpleCache.put("3", "three");
        Thread.sleep(100);
        simpleCache.put("4", "four");
        Thread.sleep(100);
        simpleCache.put("5", "five");
        Thread.sleep(100);
        assertNull(simpleCache.get("1")); // key 'one' should not be present
        assertEquals(simpleCache.size(), 4);
        simpleCache.clear();
    }

    @Test
    public void testCacheKeyRenewal() throws InterruptedException {
        InMemoryCache<String, String> simpleCache = InMemoryCache.builder()
                .expireAfter(200, ChronoUnit.MILLIS).build();
        // renew is as of now considered as an internal method to match other cache implementation.
        simpleCache.put("1", "one");
        Thread.sleep(100);
        simpleCache.get("1");
        Thread.sleep(100);
        assertNotNull(simpleCache.get("1")); // key 'one' should be present
        simpleCache.clear();
    }

    @Test
    public void testCacheKeyExpiryOverRiddingInMethod() throws InterruptedException {
        InMemoryCache<String, String> simpleCache = InMemoryCache.builder()
                .expireAfter(500, ChronoUnit.MILLIS).build();
        simpleCache.put("1", "one", 700);
        Thread.sleep(100);
        simpleCache.put("2", "two", 50);
        Thread.sleep(100);
        simpleCache.put("3", "three");
        Thread.sleep(100);
        simpleCache.put("4", "four");
        Thread.sleep(100);
        simpleCache.put("5", "five");
        Thread.sleep(100);
        assertEquals(simpleCache.get("1"), "one"); // key 'one' should be present
        assertEquals(simpleCache.size(), 4);
        assertNull(simpleCache.get("2"));
        simpleCache.clear();
    }
}