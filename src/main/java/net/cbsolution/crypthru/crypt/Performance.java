package net.cbsolution.crypthru.crypt;

import lombok.extern.java.Log;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.Date;

@Log
public class Performance {

  private final String caption;
  private long bytes;
  private long millis;

  public Performance(String caption) {
    this.caption = caption;
  }

  public void record(Date start, Date end, Path file) {
    millis += end.getTime() - start.getTime();
    try {
      this.bytes += Files.size(file);
    } catch (IOException e) {
      throw new RuntimeException("Can't measure size of " + file, e);
    }
  }

  public String getFigures() {
    if (bytes == 0)
      return null;
    double rate = (double) bytes / millis / 1000;
    return MessageFormat.format("{0} - {1} bytes in {2} seconds = {3} KB/sec.",
        caption, bytes, millis/1000, rate);
  }

  public void logFigures() {
    String figures = getFigures();
    if (figures != null)
      log.info(figures);
  }



}
