package com.example.cache;

import org.junit.jupiter.api.Test;

import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author vishnu.g
 */
public class InMemoryCacheWithDelayQueueTest {

    @Test
    public void testCacheWithDelayQueue() {
        Cache<String, String> simpleCache = new InMemoryCacheWithDelayQueue();
        simpleCache.put("1", "one");
        assertEquals("one", simpleCache.get("1"));
        simpleCache.put("1", "ONE");
        assertEquals("ONE", simpleCache.get("1"));
        simpleCache.put("2", "TWO");
        assertEquals(2, simpleCache.size());
        assertNotNull(simpleCache.remove("2"));
        assertEquals(1, simpleCache.size());
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
        assertEquals(4, simpleCache.size());
        simpleCache.clear();
    }
}