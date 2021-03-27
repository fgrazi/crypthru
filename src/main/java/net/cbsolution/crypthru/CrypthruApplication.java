package net.cbsolution.crypthru;

import com.beust.jcommander.JCommander;
import lombok.extern.java.Log;
import net.cbsolution.crypthru.util.PathKit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@SpringBootApplication
@Log
public class CrypthruApplication implements CommandLineRunner {

  private Arguments arguments;
  private DirectiveLoader directiveLoader;
  private AppConfiguration appConfiguration;
  private JCommander commander;

  @Autowired
  public void setArguments(Arguments arguments) {
    this.arguments = arguments;
  }

  @Autowired
  public void setDirectiveLoader(DirectiveLoader directiveLoader) {
    this.directiveLoader = directiveLoader;
  }

  @Autowired
  public void setAppConfiguration(AppConfiguration appConfiguration) {
    this.appConfiguration = appConfiguration;
  }

  @Override
  public void run(String... args) {
    log.info("Crypthru V. " + appConfiguration.getVersion() + ". JVM: " +
        System.getProperty("java.vm.name") + " by " + System.getProperty("java.vm.vendor") +
        " v. " + System.getProperty("java.vm.version"));
    commander = JCommander.newBuilder().addObject(arguments).build();
    commander.setProgramName("crypthru");
    commander.parse(args);

    if (arguments.isGuide())
      printGuide();
    else if (arguments.isHelp())
      commander.usage();
    else if (arguments.getDirectives().size() + arguments.getDirectiveFile().size() == 0) {
      log.warning("Nothing to do! Use -help list command line arguments, -guide to print User's Guide.");
      commander.usage();
    }
    for (String s : arguments.getDirectiveFile()) {
      process(PathKit.replaceHome(s), arguments);
    }
    processCommandLine(arguments.getDirectives());
    arguments.getEncryptPerformance().logFigures();
    arguments.getDecryptPerformance().logFigures();
  }

  public static void main(String[] args) {
    SpringApplication.run(CrypthruApplication.class, args);
  }

  private void process(Path directiveFile, Arguments arguments) {
    if (!Files.exists(directiveFile)) {
      throw new RuntimeException("No such directive file: " + directiveFile);
    }
    List<Directive> directives = directiveLoader.load(directiveFile);
    process(directives);
  }

  private void processCommandLine(List<String> commandLineArgs) {
    List<Directive> directives = directiveLoader.convert(commandLineArgs);
    process(directives);
  }

  private void process(List<Directive> directives) {
    for (Directive d : directives) {
      log.info("Directive --> " + d.print() + " ...");
      d.execute(arguments);
    }
  }

  private void printGuide() {
    PathKit.dump(getClass().getResource("UserGuide.md"), System.out);
    for (String[] entry : appConfiguration.listDirectives()) {
      System.out.println("\n## " + entry[0] + " directive\n\n");
      if (entry[1] != null)
        PathKit.dump(getClass().getResource(entry[1]), System.out);
    }
    System.out.println("\n## Command line arguments");
    commander.usage();
  }

}
