package net.cbsolution.crypthru.util;

import java.io.IOException;
import java.nio.file.*;

/**
 * Watch events in a directory
 */
public class DirectoryWatcher {

  private final Path path;
  private Reaction reaction;
  private final WatchService watchService;
  private Filter filter;
  private String stopper;

  /**
   * Instance constructor reacting to ENTRY_CREATE events.
   * @param path The directory to watch.
   */
  public DirectoryWatcher(Path path) {
    this(path, StandardWatchEventKinds.ENTRY_CREATE);
  }

  /**
   * Instance constructor.
   * @param path The directory to watch.
   * @param eventKinds The kind of events to be handled.
   */
  public DirectoryWatcher(Path path, WatchEvent.Kind<?>... eventKinds) {
    this.path = path;
    try {
      watchService = FileSystems.getDefault().newWatchService();
      path.register(watchService, eventKinds);
    } catch (IOException e) {
      throw new RuntimeException("I/O error watching " + path, e);
    }
  }

  /**
   * Acrivate reactions.
   * @param reaction The reaction to be activated or null to stop reacting.
   * @return self for chaining.
   */
  public DirectoryWatcher react(Reaction reaction) {
    this.reaction = reaction;
    WatchKey key;
    boolean stopped = false;
    while (!stopped) {
      try {
        if (!(reaction != null && (key = watchService.take()) != null)) break;
      } catch (InterruptedException e) {
        stopped = true;
        break;
      }
      for (WatchEvent<?> event : key.pollEvents()) {
        if (stopper != null && stopper.equals(event.context().toString())) {
          stopped = true;
          break;
        }
        Path context = path.resolve(event.context().toString());
        if (filter == null || filter.accept(context))
          reaction.reactTo(event, context);
      }
      key.reset();
    }
    return this;
  }

  /**
   * Apply a filter
   * @param filter The filter to use or null to remove the filter.
   * @return self for chaining.
   */
  public DirectoryWatcher filter(Filter filter) {
    this.filter = filter;
    return this;
  }

  /**
   * Define a stop filter.
   * @param stopper The name of a file that will stop monitoring, for example "STOP".
   * @return self for chaining
   */
  public DirectoryWatcher stopOn(String stopper) {
    this.stopper = stopper;
    return this;
  }

  /**
   * The reaction to a event.
   */
  public interface Reaction {

    /**
     * Execute the actions
     * @param event The occurred event.
     * @param context The associated path.
     */
    void reactTo(WatchEvent event, Path context);
  }

  /**
   * A filter path filter.
   */
  public interface Filter {

    /**
     * @param path The context
     * @return true if the context shall be accepted.
     */
    boolean accept(Path path);
  }

}
