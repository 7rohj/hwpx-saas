package io.github.mumberrymountain.validate;

import io.github.mumberrymountain.ConfigOption;
import io.github.mumberrymountain.exception.InvalidConfigurationException;

import java.util.Map;

public class AutoTrimValidater implements Validater {
    private static AutoTrimValidater autoTrimValidater;

    public static synchronized AutoTrimValidater getInstance(){
        if (autoTrimValidater == null) autoTrimValidater = new AutoTrimValidater();
        return autoTrimValidater;
    }

    @Override
    public void validate(Map<String, Object> properties) {
        if (!(properties.get(ConfigOption.AUTO_TRIM.getType()) instanceof Boolean)) throw new InvalidConfigurationException("autoTrim must be a boolean");
    }
}
