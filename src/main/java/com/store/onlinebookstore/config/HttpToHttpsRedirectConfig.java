package com.store.onlinebookstore.config;

import org.apache.catalina.connector.Connector;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class HttpToHttpsRedirectConfig {
    @Bean
    public WebServerFactoryCustomizer<TomcatServletWebServerFactory> httpToHttpsRedirectCustomizer() {
        return factory -> {
            Connector http = new Connector(TomcatServletWebServerFactory.DEFAULT_PROTOCOL);
            http.setScheme("http");
            http.setPort(8080);         // listen on HTTP
            http.setSecure(false);
            http.setRedirectPort(8443); // send 302 to HTTPS
            factory.addAdditionalTomcatConnectors(http);
        };
    }
}
