package net.cbsolution.crypthru.crypt;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

@Component
public class SpringContext implements ApplicationContextAware {

  private static ApplicationContext context;

  public static ApplicationContext getContext() {
    if (context == null)
      throw new IllegalStateException("Spring application context still not initialized.");
    return context;
  }

  /**
   * Returns the Spring managed bean instance of the given class type if it exists.
   * Returns null otherwise.
   *
   * @param beanClass
   * @return
   */
  public static <T extends Object> T getBean(Class<T> beanClass) {
    return getContext().getBean(beanClass);
  }

  @Override
  public void setApplicationContext(ApplicationContext context) throws BeansException {

    // store ApplicationContext reference to access required beans later on
    SpringContext.context = context;
  }

}