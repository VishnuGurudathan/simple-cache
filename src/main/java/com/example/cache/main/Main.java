package com.example.cache.main;

import com.example.cache.Cache;
import com.example.cache.InMemoryCache;
import com.example.cache.InMemoryCacheWithLRUEviction;

import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * @author vishnu.g
 */
public class Main {
    public static void main(String[] args) throws InterruptedException {

//        Cache cache = new InMemoryCacheWithDelayQueue();
//
//        cache.add("a", "a", 10L);
//        cache.add("b", "b", 10L);
//        cache.add("c", "c", 10L);
//        cache.add("e", "e", 10L);
//        cache.add("f", "ff", 10L);
//
//        System.out.println(cache.get("a"));
//        System.out.println(cache.get("b"));
//        System.out.println(cache.get("c"));
//        System.out.println(cache.get("e"));
//        System.out.println(cache.size());
//        cache.add("d", "12", 10L);
//
//        System.out.println(cache.get("f"));
//Thread.sleep(11L);
//        System.out.println("###"+cache.get("f"));

//        System.out.println("#########################");
//
//        Cache lruCache = new InMemoryCacheWithLRUEviction(4);
//
//        lruCache.put("a", "a", 10L);
//        lruCache.put("b", "b", 10L);
//        System.out.println(lruCache.get("a"));
//        lruCache.put("c", "c", 10L);
//        lruCache.put("e", "e", 10L);
//        lruCache.put("f", "ff", 10L);
//
//        System.out.println(lruCache.get("a"));
//        System.out.println(lruCache.get("b"));
//        System.out.println(lruCache.get("c"));
//        System.out.println(lruCache.get("e"));
//        System.out.println(lruCache.get("f"));
//        System.out.println(lruCache.size());
//        lruCache.put("d", "12", 10L);
//
//        System.out.println(lruCache.get("f"));
//        Thread.sleep(11L);
//        System.out.println("###"+lruCache.get("f"));
//

//        System.out.println("#########################");
//
//        Cache lruCache = new InMemoryCacheWithLRUMap<String, String >(1L,4);
//
//        lruCache.add("a", "a", 1000L);
//        lruCache.add("b", "b", 10L);
//        System.out.println(lruCache.get("a"));
//        lruCache.add("c", "c", 10L);
//        lruCache.add("e", "e", 10L);
//        lruCache.add("f", "ff", 10L);
//
//        System.out.println(lruCache.get("a"));
//        System.out.println(lruCache.get("b"));
//        System.out.println(lruCache.get("c"));
//        System.out.println(lruCache.get("e"));
//        System.out.println(lruCache.get("f"));
//      //  System.out.println(lruCache.size());
//        lruCache.add("d", "12", 10L);
//
//        System.out.println(lruCache.get("f"));
//        Thread.sleep(11000L);
//        System.out.println("###"+lruCache.get("f"));
        Cache<String, String> simpleCache = InMemoryCache.builder()
                .initialCapacity(16).maximumSize(100)
                .expireAfter(200, ChronoUnit.MILLIS)
                .build(s -> UUID.randomUUID().toString());
        simpleCache.put("1", "one", 2L);

        System.out.println( simpleCache.get("1")); // returns "one"
        System.out.println(simpleCache.get("3"));
        System.out.println("___________________");
        simpleCache.put("12", "one22");
        System.out.println("-----------------");
        System.out.println(simpleCache.get("12"));
        simpleCache.remove("3");
        simpleCache.clear();
    }
}
