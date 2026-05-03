package com.example.aikb.infra.cache;

public final class AdminCacheKeys {

    public static final String CHAT_STATS = "aikb:admin:chat:stats";
    public static final String HOT_QUESTIONS_PREFIX = "aikb:admin:chat:hot_questions:";

    public static String hotQuestions(int limit) {
        return HOT_QUESTIONS_PREFIX + limit;
    }

    private AdminCacheKeys() {
    }
}
