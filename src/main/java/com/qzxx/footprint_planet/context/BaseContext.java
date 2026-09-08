package com.qzxx.footprint_planet.context;

public class BaseContext {
    private static final ThreadLocal<Long> USER_ID = new ThreadLocal<>();
    private static final ThreadLocal<String> ROLE = new ThreadLocal<>();

    public static void setCurrentUserId(Long id) {
        USER_ID.set(id);
    }
    public static void setCurrentUserRole(String role) {
        ROLE.set(role);
    }
    public static Long getCurrentId() { return USER_ID.get(); }
    public static String getCurrentRole() { return ROLE.get(); }

    public static void remove() {
        USER_ID.remove();
        ROLE.remove();
    }
}
