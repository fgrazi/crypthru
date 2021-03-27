package net.cbsolution.crypthru;

import org.springframework.beans.factory.annotation.Autowired;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Responsible for loading directives from source files.
 */
public class DirectiveLoader {

  @Autowired
  private AppConfiguration appConfiguration;

  public List<Directive> load(Path sourcePath) {
    Yaml yaml = new Yaml();
    Object obj;
    try (InputStream in = Files.newInputStream(sourcePath)) {
      obj = yaml.load(in);
    } catch (Exception ex) {
      throw new RuntimeException("I/O Error reading directives from " + sourcePath, ex);
    }
    if (!(obj instanceof List))
      throw new RuntimeException("File " + sourcePath + " does not contain a sequence of directives");
    List<Directive> result = new ArrayList<>();
    for (Object listElement : (List) obj) {
      Directive directive = buildDirective(listElement, sourcePath.toString());
      result.add(directive);
    }
    return result;
  }

  private Directive buildDirective(Object configObject, String sourcePath) {
    ConfigurationDecoder config = new ConfigurationDecoder(configObject, sourcePath);
    Directive result = appConfiguration.instantiateDirective(config.readString("directive"));
    result.configure(config);
    return result;
  }

  public Directive load(String text) {
    Yaml yaml = new Yaml();
    Object obj;
    try {
      obj = yaml.load(text);
    } catch (Exception ex) {
      throw new RuntimeException("Error parsing yaml [" + text + "]", ex);
    }
    return buildDirective(obj, "<command line>");
  }

  List<Directive> convert(List<String> arguments) {
    List<Directive> result = new ArrayList<>();
    int i = 0;
    Map<String, Object> parameters = null;
    for (String arg: arguments) {
      boolean isParameter = arg.contains("=");
      if (isParameter) {
        if (parameters == null)
          throw new RuntimeException("Unrecognized argument: " + arg);
        Parameter parameter = new Parameter(arg);
        parameters.put(parameter.name, parameter.value);
      } else {
        if (parameters != null)
          result.add(buildDirective(parameters, "(command line)"));
        parameters = new HashMap<>();
        parameters.put("directive", arg);
      }
    }
    if (parameters != null)
      result.add(buildDirective(parameters, "(command line)"));
    return result;
  }

  private static class Parameter {
    private String name;
    private Object value;

    Parameter(String s) {
      String[] pair = s.split("=", 2);
      name = pair[0];
      if ("true".equals(pair[1]))
        value = Boolean.TRUE;
      else if ("false".equals(pair[1]))
        value = Boolean.FALSE;
      else if (isInteger(pair[1]))
        value = Integer.parseInt(pair[1]);
      else if (isDouble(pair[1]))
        value = Double.parseDouble(pair[1]);
      else
        value = pair[1];
    }

    boolean isInteger(String s) {
      return s.matches("^-?\\d+$");
    }

    boolean isDouble(String s) {
      return s.matches("^-?\\d+\\.\\d*$");
    }
  }

}



