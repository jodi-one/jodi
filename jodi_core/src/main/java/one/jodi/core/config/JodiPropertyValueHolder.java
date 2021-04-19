package one.jodi.core.config;

import java.util.List;
import java.util.Map;

public class JodiPropertyValueHolder implements PropertyValueHolder {
   private final String key;
   private final JodiProperties properties;

   public JodiPropertyValueHolder(String key, JodiProperties properties) {
      this.key = key;
      this.properties = properties;
   }

   @Override
   public String getString() {
      return properties.getProperty(key);
   }

   @Override
   public String getKey() {
      return key;
   }

   @Override
   public List<String> getList() {
      return properties.getPropertyList(key);
   }

   @Override
   public Map<String, String> getMap() {
      return properties.getPropertyMap(key);
   }

   @Override
   public String getListAsString() {
      return properties.getListAsString(key);
   }
}
