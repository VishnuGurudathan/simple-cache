package com.example.cache;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author vishnu.g
 */
public class InMemoryCacheWithLRUMapTest {
    @Test
    public void testSimpleCache() {
        Cache<String, String> simpleCache = new InMemoryCacheWithLRUMap<>();
        simpleCache.put("1", "one");
        assertEquals("one",simpleCache.get("1"));
        simpleCache.put("1", "ONE");
        assertEquals("ONE", simpleCache.get("1"));
        simpleCache.put("2", "TWO");
        assertEquals(2, simpleCache.size());
        assertNotNull(simpleCache.remove("2"));
        assertEquals(1, simpleCache.size());
        simpleCache.clear();
    }

    @Test
    public void testLRUEviction() {
        Cache<String, String> simpleCache = new InMemoryCacheWithLRUMap(4);
        simpleCache.put("1", "one");
        simpleCache.put("2", "two");
        simpleCache.put("3", "three");
        simpleCache.put("4", "four");
        assertEquals("one", simpleCache.get("1") ); // access the 1st key here
        simpleCache.put("5", "five");
        assertNull(simpleCache.get("2")); // key 'two' should not be present
        assertEquals(4, simpleCache.size());
        simpleCache.clear();
    }


    @Test
    public void testCacheKeyExpiry() throws InterruptedException {
        Cache<String, String> simpleCache = new InMemoryCacheWithLRUMap();

        simpleCache.put("1", "one", 500);
        Thread.sleep(200);
        simpleCache.put("2", "two", 1000);
        Thread.sleep(200);
        simpleCache.put("3", "three", 2000);
        Thread.sleep(200);
        simpleCache.put("4", "four", 500);
        Thread.sleep(200);
        simpleCache.put("5", "five", 500);
        Thread.sleep(300);
        assertNull(simpleCache.get("1")); // key 'one' should not be present
        assertEquals(4, simpleCache.size());
        simpleCache.clear();
    }
}