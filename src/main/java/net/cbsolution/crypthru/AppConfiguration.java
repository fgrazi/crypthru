package net.cbsolution.crypthru;


import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.lang.reflect.InvocationTargetException;
import java.util.*;

@Component
@ConfigurationProperties(prefix = "app")
public class AppConfiguration {

  @Getter
  @Setter
  private Map<String, String> directives = new HashMap<>();

  @Getter
  @Setter
  private String version, encoding, javaVersion;

  public Directive instantiateDirective(String directiveName) {
    String directiveClassName = directives.get(directiveName);
    if (directiveClassName == null)
      throw new RuntimeException("There is no directive named " + directiveName);
    Class<? extends Directive> clazz;
    try {
      clazz = (Class<? extends Directive>) Class.forName(directiveClassName);
    } catch (ClassNotFoundException | ClassCastException ex) {
      throw new RuntimeException("Directive class missing: " + directiveClassName, ex);
    }
    Directive result;
    try {
      result = clazz.getDeclaredConstructor().newInstance();
    } catch (NoSuchMethodException | InstantiationException | InvocationTargetException | IllegalAccessException ex) {
      throw new RuntimeException("Internal error instantiating " + directiveName + " directive", ex);
    }
    return result;
  }

  /**
   * @return A list of directives to be inserted in the guide. Each entry contains the directive name
   * and the corresponding markdown resource name if any.
   */
  public List<String[]> listDirectives() {
    List<String[]> result = new ArrayList<>();
    List<String> names = new ArrayList<>(directives.keySet());
    Collections.sort(names);
    for (String name : names) {
      String resource = "/" + directives.get(name).replaceAll("\\.", "/") + ".md";
      if (getClass().getResource(resource) == null)
        resource = null;
      result.add(new String[]{name, resource});
    }
    return result;
  }

}
