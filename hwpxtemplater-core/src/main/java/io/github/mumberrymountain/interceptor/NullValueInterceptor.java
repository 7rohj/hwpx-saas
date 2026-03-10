package io.github.mumberrymountain.interceptor;

@FunctionalInterface
public interface NullValueInterceptor extends Interceptor {
    public String intercept(String value, String field);
}
