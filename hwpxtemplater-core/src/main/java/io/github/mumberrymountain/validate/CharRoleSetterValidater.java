package io.github.mumberrymountain.validate;

import io.github.mumberrymountain.ConfigOption;
import io.github.mumberrymountain.exception.InvalidConfigurationException;
import io.github.mumberrymountain.model.CharRole;
import io.github.mumberrymountain.render.placeholder.PlaceHolderCharRole;
import io.github.mumberrymountain.render.placeholder.PlaceHolderType;

import java.util.Map;

public class CharRoleSetterValidater implements Validater{
    private static CharRoleSetterValidater charRoleSetterValidater;

    public static synchronized CharRoleSetterValidater getInstance(){
        if (charRoleSetterValidater == null) charRoleSetterValidater = new CharRoleSetterValidater();
        return charRoleSetterValidater;
    }

    @Override
    public void validate(Map<String, Object> properties) throws InvalidConfigurationException {
        if (!(properties.get(ConfigOption.CHAR_ROLE_SETTER.getType()) instanceof CharRole)) throw new InvalidConfigurationException("charRoleSetter must be a CharRole");
        CharRole charRole = (CharRole) properties.get(ConfigOption.CHAR_ROLE_SETTER.getType());
        validateCharRoleIndividual(charRole);
        resetCharRoleMap(charRole);
    }

    private void validateCharRoleIndividual(CharRole charRole){
        if (charRole.get(PlaceHolderType.CONDITION) == null) throw new InvalidConfigurationException("charRole for placeHolder type condition must not be null");
        if (charRole.get(PlaceHolderType.LOOP) == null) throw new InvalidConfigurationException("charRole for placeHolder type loop must not be null");
        if (charRole.get(PlaceHolderType.CLOSURE) == null) throw new InvalidConfigurationException("charRole for placeHolder type closure must not be null");
        if (charRole.get(PlaceHolderType.IMAGE_REPLACEMENT) == null) throw new InvalidConfigurationException("charRole for placeHolder type image must not be null");
        if (charRole.get(PlaceHolderType.TABLE_REPLACEMENT) == null) throw new InvalidConfigurationException("charRole for placeHolder type table must not be null");
    }

    private void resetCharRoleMap(CharRole charRole){
        PlaceHolderCharRole.reset(charRole.getCharRoleMap());
    }
}
