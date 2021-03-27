package net.cbsolution.crypthru.directives;

import lombok.extern.java.Log;
import net.cbsolution.crypthru.Arguments;
import net.cbsolution.crypthru.ConfigurationDecoder;
import net.cbsolution.crypthru.Directive;
import org.apache.commons.io.IOUtils;

import java.io.*;
import java.util.Locale;

@Log
public class Execute implements Directive {

  private String command;
  private boolean lenient = false;
  private boolean quiet = true;

  @Override
  public void configure(ConfigurationDecoder config) {
    command = config.readString("command");
    quiet = config.read("quiet", true);
    lenient = config.read("lenient", false);
  }

  @Override
  public String print() {
    return "Execute " + command;
  }

  @Override
  public void execute(Arguments args) {
    for (String line : command.split("\n")) {
      line = line.trim();
      if (line.isEmpty())
        continue;
      String action = args.isPreviewMode() ? "Would execute: " : "Executing: ";
      log.info(action + line);
      if (System.getProperty("os.name").toLowerCase(Locale.ROOT).startsWith("windows"))
        execute("cmd", "/c", line);
      else
        execute("bash", "-c", line);
    }
  }

  public void execute(String... tokens) {

    final ProcessBuilder builder = new ProcessBuilder(tokens)
        .redirectErrorStream(true);
    try {
      final Process process = builder.start();
      final StringWriter writer = new StringWriter();

      new Thread(new Runnable() {
        public void run() {
          try {
            IOUtils.copy(process.getInputStream(), writer, "UTF-8");
          } catch (IOException ex) {
            throw new RuntimeException("Error executing: " + String.join(" ", tokens), ex);
          }
        }
      }).start();

      final int exitValue = process.waitFor();
      if (exitValue != 0 && !lenient)
        throw new RuntimeException("Exit code " + exitValue + " when running " + String.join(" ", tokens));
      final String processOutput = writer.toString();
      if (!quiet)
        System.out.println(processOutput);
    } catch (IOException | InterruptedException ex) {
      throw new RuntimeException("Error executing: " + String.join(" ", tokens), ex);
    }
  }

}
