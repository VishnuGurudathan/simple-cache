package com.example.cache.main;

import com.example.cache.*;

import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * @author vishnu.g
 */
public class Main {
    public static void main(String[] args) throws InterruptedException {

        /* How {@com.example.cache.InMemoryCacheWithDelayQueue} is used */
        Cache<String, String> simpleCacheWithDelayQueue = new InMemoryCacheWithDelayQueue();
        simpleCacheWithDelayQueue.put("1", "one");
        simpleCacheWithDelayQueue.get("1"); // returns "one"
        simpleCacheWithDelayQueue.remove("3");
        simpleCacheWithDelayQueue.clear();

        /* How {@com.example.cache.InMemoryCacheWithLRUEviction} is used */
        Cache<String, String> simpleCacheWithLRUEviction = new InMemoryCacheWithLRUEviction();
        simpleCacheWithLRUEviction.put("1", "one");
        simpleCacheWithLRUEviction.get("1"); // returns "one"
        simpleCacheWithLRUEviction.remove("3");
        simpleCacheWithLRUEviction.clear();

        /* How {@com.example.cache.InMemoryCacheWithLRUMap} is used */
        Cache<String, String> simpleCacheWithLRUMap = new InMemoryCacheWithLRUMap();
        simpleCacheWithLRUMap.put("1", "one");
        simpleCacheWithLRUMap.get("1"); // returns "one"
        simpleCacheWithLRUMap.remove("3");
        simpleCacheWithLRUMap.clear();

        /* How {@com.example.cache.InMemoryCacheWithFIFOEviction} is used */
        Cache<String, String> simpleCacheWithFIFOEviction = new InMemoryCacheWithFIFOEviction();
        simpleCacheWithFIFOEviction.put("1", "one");
        simpleCacheWithFIFOEviction.get("1"); // returns "one"
        simpleCacheWithFIFOEviction.remove("3");
        simpleCacheWithFIFOEviction.clear();

        /* How {@com.example.cache.InMemoryCacheWithLFUEviction} is used */
        Cache<String, String> simpleCacheWithLFUEviction = new InMemoryCacheWithLFUEviction();
        simpleCacheWithLFUEviction.put("1", "one");
        simpleCacheWithLFUEviction.get("1"); // returns "one"
        simpleCacheWithLFUEviction.remove("3");
        simpleCacheWithLFUEviction.clear();

        /* How {@com.example.cache.InMemoryCache} is used */
        Cache<String, String> simpleCache = InMemoryCache.builder()
                .initialCapacity(16).maximumSize(100)
                .expireAfter(200, ChronoUnit.MILLIS)
                .build(s -> UUID.randomUUID().toString());
        simpleCache.put("1", "one");
        simpleCache.get("1"); // returns "one"
        simpleCache.remove("3");
        simpleCache.clear();

    }
}
