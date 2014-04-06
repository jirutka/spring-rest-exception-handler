Spring REST Exception handler
=============================
[![Build Status](https://travis-ci.org/jirutka/spring-rest-exception-handler.svg)](https://travis-ci.org/jirutka/spring-rest-exception-handler)
[![Coverage Status](https://img.shields.io/coveralls/jirutka/spring-rest-exception-handler.svg)](https://coveralls.io/r/jirutka/spring-rest-exception-handler)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/cz.jirutka.spring/spring-rest-exception-handler/badge.svg)](https://maven-badges.herokuapp.com/maven-central/cz.jirutka.spring/spring-rest-exception-handler)

TODO


Configuration
-------------

### Java-based configuration

```java
@EnableWebMvc
@Configuration
public class RestContextConfig extends WebMvcConfigurerAdapter {

    @Override
    public void configureHandlerExceptionResolvers(List<HandlerExceptionResolver> resolvers) {
        resolvers.add( exceptionHandlerExceptionResolver() ); // resolves @ExceptionHandler
        resolvers.add( restExceptionResolver() );
    }

    @Bean
    public RestHandlerExceptionResolver restExceptionResolver() {
        return RestHandlerExceptionResolver.builder()
                .messageSource( httpErrorMessageSource() )
                .addErrorMessageHandler(EmptyResultDataAccessException.class, HttpStatus.NOT_FOUND)
                .addHandler(MyException.class, new MyExceptionHandler())
                .build();
    }

    @Bean
    public MessageSource httpErrorMessageSource() {
        ReloadableResourceBundleMessageSource m = new ReloadableResourceBundleMessageSource();
        m.setBasename("classpath:/org/example/messages");
        m.setDefaultEncoding("UTF-8");
        return m;
    }

    @Bean
    public ExceptionHandlerExceptionResolver exceptionHandlerExceptionResolver() {
        ExceptionHandlerExceptionResolver resolver = new ExceptionHandlerExceptionResolver();
        resolver.setMessageConverters(HttpMessageConverterUtils.getDefaultHttpMessageConverters());
        return resolver;
    }
}
```

### XML-based configuration

```xml
<bean id="compositeExceptionResolver"
      class="org.springframework.web.servlet.handler.HandlerExceptionResolverComposite">
    <property name="order" value="0" />
    <property name="exceptionResolvers">
        <list>
            <ref bean="exceptionHandlerExceptionResolver" />
            <ref bean="restExceptionResolver" />
        </list>
    </property>
</bean>

<bean id="restExceptionResolver"
      class="cz.jirutka.spring.web.servlet.exhandler.RestHandlerExceptionResolverFactoryBean">
    <property name="messageSource" ref="httpErrorMessageSource" />
    <property name="exceptionHandlers">
        <map>
            <entry key="org.springframework.dao.EmptyResultDataAccessException" value="404" />
            <entry key="org.example.MyException">
                <bean class="org.example.MyExceptionHandler" />
            </entry>
        </map>
    </property>
</bean>

<bean id="exceptionHandlerExceptionResolver"
      class="org.springframework.web.servlet.mvc.method.annotation.ExceptionHandlerExceptionResolver" />

<bean id="httpErrorMessageSource"
      class="org.springframework.context.support.ReloadableResourceBundleMessageSource"
      p:basename="classpath:/org/example/errorMessages"
      p:defaultEncoding="UTF-8" />
```

### Notes

The ExceptionHandlerExceptionResolver is used to resolve exceptions through [@ExceptionHandler] methods. It must be
registered _before_ the RestHandlerExceptionResolver. If you don’t have any [@ExceptionHandler], then you can omit
`exceptionHandlerExceptionResolver` bean declaration.

Builder and FactoryBean registers set of the default handlers by default. This can be disabled by setting
`withDefaultHandlers` to false.


Maven
-----

Released versions are available in The Central Repository. Just add this artifact to your project:

```xml
<dependency>
    <groupId>cz.jirutka.spring</groupId>
    <artifactId>spring-rest-exception-handler</artifactId>
    <version>1.0-SNAPSHOT</version>
</dependency>
```

However if you want to use the last snapshot version, you have to add the Sonatype OSS repository:

```xml
<repository>
    <id>sonatype-snapshots</id>
    <name>Sonatype repository for deploying snapshots</name>
    <url>https://oss.sonatype.org/content/repositories/snapshots</url>
    <snapshots>
        <enabled>true</enabled>
    </snapshots>
</repository>
```


License
-------

This project is licensed under [Apache License 2.0](http://www.apache.org/licenses/LICENSE-2.0.html).


[@ExceptionHandler]: http://docs.spring.io/spring/docs/current/javadoc-api/org/springframework/web/bind/annotation/ExceptionHandler.html
