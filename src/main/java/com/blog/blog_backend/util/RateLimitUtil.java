package com.blog.blog_backend.util;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Stats API용 Rate Limit (기존 Next.js와 동일: 분당 60회, 429 시 "요청이 너무 많습니다.")
 */
public class RateLimitUtil {

    private static final int MAX_REQUESTS = 60;
    private static final long WINDOW_MS = 60_000L;

    private static class Window {
        int count;
        long resetTime;
        Window(long now) {
            this.count = 1;
            this.resetTime = now + WINDOW_MS;
        }
    }

    private static final Map<String, Window> store = new ConcurrentHashMap<>();

    /** 클라이언트 식별 (기존 Next.js getClientIdentifier와 동일: X-Forwarded-For → X-Real-IP → unknown) */
    public static String getClientIdentifier(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp;
        }
        return "unknown";
    }

    /** stats 전용: 분당 60회 초과 시 false */
    public static boolean allowStatsRequest(String clientId) {
        long now = System.currentTimeMillis();
        String key = "stats-" + clientId;
        synchronized (store) {
            Window w = store.get(key);
            if (w == null || w.resetTime < now) {
                store.put(key, new Window(now));
                return true;
            }
            w.count++;
            return w.count <= MAX_REQUESTS;
        }
    }
}
