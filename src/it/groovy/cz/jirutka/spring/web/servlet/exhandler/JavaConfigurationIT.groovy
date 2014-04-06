/*
 * Copyright 2014 Jakub Jirutka <jakub@jirutka.cz>.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package cz.jirutka.spring.web.servlet.exhandler

import cz.jirutka.spring.web.servlet.exhandler.fixtures.SampleController
import cz.jirutka.spring.web.servlet.exhandler.fixtures.ZuulException
import cz.jirutka.spring.web.servlet.exhandler.fixtures.ZuulExceptionHandler
import cz.jirutka.spring.web.servlet.exhandler.support.HttpMessageConverterUtils
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.support.ReloadableResourceBundleMessageSource
import org.springframework.test.context.ContextConfiguration
import org.springframework.web.HttpRequestMethodNotSupportedException
import org.springframework.web.servlet.HandlerExceptionResolver
import org.springframework.web.servlet.config.annotation.EnableWebMvc
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter
import org.springframework.web.servlet.mvc.method.annotation.ExceptionHandlerExceptionResolver

import static org.springframework.http.HttpStatus.I_AM_A_TEAPOT

@ContextConfiguration(classes=ContextConfig)
class JavaConfigurationIT extends AbstractConfigurationIT {

    @EnableWebMvc
    @Configuration
    static class ContextConfig extends WebMvcConfigurerAdapter {

        void configureHandlerExceptionResolvers(List<HandlerExceptionResolver> resolvers) {
            resolvers.add(exceptionHandlerExceptionResolver())
            resolvers.add(restExceptionResolver())
        }

        @Bean
        restExceptionResolver() {
            RestHandlerExceptionResolver.builder()
                    .messageSource(httpErrorMessageSource())
                    .addErrorMessageHandler(HttpRequestMethodNotSupportedException, I_AM_A_TEAPOT)
                    .addHandler(ZuulException, new ZuulExceptionHandler())
                    .build()
        }

        @Bean
        httpErrorMessageSource() {
            new ReloadableResourceBundleMessageSource(
                    basename: 'classpath:/testMessages',
                    defaultEncoding: 'UTF-8'
            )
        }

        @Bean
        exceptionHandlerExceptionResolver() {
            new ExceptionHandlerExceptionResolver(
                    messageConverters: HttpMessageConverterUtils.getDefaultHttpMessageConverters()
            )
        }

        @Bean
        SampleController sampleController() {
            new SampleController()
        }
    }
}
