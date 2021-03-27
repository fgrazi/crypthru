package net.cbsolution.crypthru.util;


import java.io.Console;
import java.text.MessageFormat;
import java.util.Optional;

public class Dialog {
  private Console console;


  private final static String CANCEL_STRING = "\\q";

  // see https://howtodoinjava.com/java-examples/console-input-output/
  // see https://stackoverflow.com/questions/5762491/how-to-print-color-in-console-using-system-out-println
  public static final String ANSI_RESET = "\u001B[0m";
  public static final String ANSI_BLACK = "\u001B[30m";
  public static final String ANSI_RED = "\u001B[31m";
  public static final String ANSI_GREEN = "\u001B[32m";
  public static final String ANSI_YELLOW = "\u001B[33m";
  public static final String ANSI_BLUE = "\u001B[34m";
  public static final String ANSI_PURPLE = "\u001B[35m";
  public static final String ANSI_CYAN = "\u001B[36m";
  public static final String ANSI_WHITE = "\u001B[37m";


  public Dialog() {
    console = System.console();
    if (console == null)
      throw new RuntimeException("Your system seems not support system console.");
  }


  /**
   * Ask a question
   *
   * @param prompt The prompt displayed to user,
   * @param args   Argukents to format the prompt.
   * @return The answer or empty to cancel.
   */
  public Optional<String> ask(String prompt, Object... args) {
    return ask(false, prompt, args);
  }

  public Optional<String> askPassword(String prompt, Object... args) {
    return ask(true, prompt, args);
  }

  private Optional<String> ask(boolean isPassword, String prompt, Object... args) {
    String formatted = "\n" + ANSI_GREEN + MessageFormat.format(prompt, args) + " (" +
        ANSI_CYAN + CANCEL_STRING + ANSI_GREEN + " to cancel): " + ANSI_RESET;
    String result;
    if (isPassword)
      result = new String(console.readPassword(formatted));
    else
      result = console.readLine(formatted);
    if (CANCEL_STRING.equals(result))
      return Optional.empty();
    return Optional.of(result);
  }

  public void say(String prompt, Object... args) {
    String formatted = "\n" + ANSI_GREEN + MessageFormat.format(prompt, args) + ANSI_RESET + "\n";
    console.printf(formatted);
  }

  public void error(String prompt, Object... args) {
    String formatted = "\n" + ANSI_RED + MessageFormat.format(prompt, args) + ANSI_RESET + "\n";
    console.printf(formatted);
  }

  public void warn(String prompt, Object... args) {
    String formatted = "\n" + ANSI_YELLOW + MessageFormat.format(prompt, args) + ANSI_RESET + "\n";
    console.printf(formatted);
  }


}
