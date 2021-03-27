package net.cbsolution.crypthru;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class InjectedBeans {

  @Bean
  Arguments arguments() {
    return new Arguments();
  }

  @Bean
  FSKeystore cTHome() {
    return new FSKeystore();
  }

  @Bean
  DirectiveLoader directiveLoader() {
    return new DirectiveLoader();
  }

}
