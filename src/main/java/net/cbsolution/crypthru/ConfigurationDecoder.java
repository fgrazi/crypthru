package net.cbsolution.crypthru;

import net.cbsolution.crypthru.util.FileGrabber;
import org.springframework.util.AntPathMatcher;

import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ConfigurationDecoder {

  static final Pattern REPLACEABLE_SEQUENCE = Pattern.compile("\\$\\{([^\\}]+)\\}");
  static final Pattern EXPAND_PATTERN = Pattern.compile("^expand: *(.+)$");
  private static AntPathMatcher antPatternMatcher = new AntPathMatcher();


  private final Map<String, Object> map;
  private final String sourcePath;

  ConfigurationDecoder(Object o, String sourcePath) {
    this.sourcePath = sourcePath;
    if (!(o instanceof Map))
      throw new RuntimeException("Not a map: " + o + " in file " + sourcePath);
    map = (Map) o;
  }

  public String at() {
    return abbrev(map.toString(), 40);
  }

  private static String abbrev(String s, int len) {
    return s.length() > len ? s.substring(0, len - 3) + "..." : s;
  }

  public String readString(String key) {
    Object result = map.get(key);
    if (result == null)
      throw new RuntimeException("Missing [" + key + "] entry at " + at() + ", file: " + sourcePath);
    return replaceSequences(result.toString());
  }

  public String readString(String key, String defaultValue) {
    Object result = map.get(key);
    if (result == null)
      return replaceSequences(defaultValue);
    return replaceSequences(result.toString());
  }

  public String[] readStrings(String key) {
    Object result = map.get(key);
    if (result == null)
      return new String[]{};
    else if (result instanceof List) {
      List<String> l = new ArrayList<>();
      for (Object o : (List) result)
        l.add(replaceSequences(o.toString()));
      return l.toArray(new String[l.size()]);
    } else
      return new String[]{replaceSequences(result.toString())};
  }

  public boolean read(String key, boolean defaultValue) {
    Object result = map.get(key);
    if (result == null)
      return defaultValue;
    if (!(result instanceof Boolean))
      throw new RuntimeException("Entry [" + key + "] can only be defined as true or false at: " + sourcePath);
    return (Boolean) result;
  }

  public List<Map<String, Object>> readMapList(String key) {
    List<Map<String, Object>> result = new ArrayList<>();
    Object entry = map.get(key);
    if (entry == null)
      return result;
    if (!(entry instanceof List))
        throw new RuntimeException("Entry [" + key + "] is not a list at: " + sourcePath);
    for (Object listEl: (List)entry) {
      if (!(listEl instanceof Map))
        throw new RuntimeException("Not a map (" + listEl + ") at: " + sourcePath);
      result.add((Map)listEl);
    }
    return result;
  }

  public void captureFilters(FileGrabber fileGrabber) {
    for (Map<String, Object> map: readMapList("filter")) {
      Object pattern;
      if ((pattern = map.get("include")) != null)
        fileGrabber.filter(FileGrabber.GlobFilter.Action.INCLUDE, pattern.toString());
      else if ((pattern = map.get("exclude")) != null)
        fileGrabber.filter(FileGrabber.GlobFilter.Action.EXCLUDE, pattern.toString());
    }
  }

  private String replaceSequences(String original) {
    if (original == null)
      return null;
    Matcher m = REPLACEABLE_SEQUENCE.matcher(original);
    StringBuilder result = new StringBuilder();
    int s = 0;
    while (m.find()) {
      String replacement = evaluate(m.group(1));
      if (replacement == null)
        replacement = m.group(0);
      result.append(original, s, m.start());
      result.append(replacement);
      s = m.end();
    }
    result.append(original, s, original.length());
    return result.toString();
  }

 protected String evaluate(String expression) {
    Matcher m = EXPAND_PATTERN.matcher(expression);
    if (m.matches())
      return expand(m.group(1));
    String result = System.getProperty(expression);
    if (result == null)
      result = System.getenv(expression);
    return result;
  }

  private static String expand(String wildcard) {
    List<String> result = new ArrayList<>();
    try (DirectoryStream<Path> dirStream = Files.newDirectoryStream(
        Paths.get("."))) {
      dirStream.forEach(path -> {
        if (antPatternMatcher.match(wildcard, path.toString()))
          result.add(path.toString());
      });
    } catch (IOException ex) {
      throw new RuntimeException("Error expanding " + wildcard, ex);
    }
    return String.join(" ", result);
  }


}
