package io.github.mumberrymountain.interceptor;

import io.github.mumberrymountain.model.Text;

public interface ValueStylingInterceptor extends Interceptor {
    public Text intercept(Text value, String field);
}
