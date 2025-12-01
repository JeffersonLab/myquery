package org.jlab.myquery;

import jakarta.servlet.*;
import jakarta.servlet.annotation.WebListener;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This ServletContextListener conditionally enables and configures the Tomcat built-in CorsFilter
 * using origins from the environment variable CORS_ALLOWED_ORIGINS.
 */
@WebListener
public class CorsConfigListener implements ServletContextListener {

  private static final Logger LOGGER = Logger.getLogger(CorsConfigListener.class.getName());

  @Override
  public void contextInitialized(ServletContextEvent event) {
    String origins = System.getenv("CORS_ALLOWED_ORIGINS");

    if (origins != null && !origins.isEmpty()) {
      LOGGER.log(Level.INFO, "CORS_ALLOWED_ORIGINS: " + origins);

      ServletContext context = event.getServletContext();

      FilterRegistration registration =
          context.addFilter("CorsFilter", "org.apache.catalina.filters.CorsFilter");

      registration.addMappingForUrlPatterns(null, false, "/*");

      registration.setInitParameter("cors.allowed.origins", origins);
    }
  }
}
