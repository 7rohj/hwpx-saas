package io.github.mumberrymountain;

import io.github.mumberrymountain.model.CharRole;
import io.github.mumberrymountain.validate.Validater;
import io.github.mumberrymountain.validate.ValidaterFactory;

import java.util.HashMap;
import java.util.Map;
public class Config {
    private final Map<String, Object> properties = new HashMap<>();

    public Config() {
        properties.put(ConfigOption.DELIM_PREFIX.getType(), "{{");
        properties.put(ConfigOption.DELIM_SUFFIX.getType(), "}}");
        properties.put(ConfigOption.CHAR_ROLE_SETTER.getType(), new CharRole());
        properties.put(ConfigOption.AUTO_TRIM.getType(), false);
    }

    public Config validate() throws Exception {
        for (Validater validater : ValidaterFactory.validaters()) validater.validate(properties);
        return this;
    }

    public void set(ConfigOption opt, Object value) {
        properties.put(opt.getType(), value);
    }
    public void set(String key, Object value) {
        properties.put(key, value);
    }
    public Object get(String key) {
        return properties.get(key);
    }
}
