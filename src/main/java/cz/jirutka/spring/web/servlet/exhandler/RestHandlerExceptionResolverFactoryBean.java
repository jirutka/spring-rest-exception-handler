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
package cz.jirutka.spring.web.servlet.exhandler;

import cz.jirutka.spring.web.servlet.exhandler.handlers.RestExceptionHandler;
import cz.jirutka.spring.web.servlet.exhandler.interpolators.MessageInterpolator;
import lombok.Setter;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.accept.ContentNegotiationManager;

import java.util.List;
import java.util.Map;

@Setter
public class RestHandlerExceptionResolverFactoryBean implements FactoryBean<RestHandlerExceptionResolver> {

    private MessageSource messageSource;
    private MessageInterpolator messageInterpolator;
    private List<HttpMessageConverter<?>> httpMessageConverters;
    private ContentNegotiationManager contentNegotiationManager;
    private boolean withDefaultHandlers;
    private boolean withDefaultMessageSource;
    private Map<Class<? extends Exception>, ?> exceptionHandlers;


    @SuppressWarnings("unchecked")
    public RestHandlerExceptionResolver getObject() {

        RestHandlerExceptionResolverBuilder builder = new RestHandlerExceptionResolverBuilder()
                .messageSource(messageSource)
                .messageInterpolator(messageInterpolator)
                .httpMessageConverters(httpMessageConverters)
                .contentNegotiationManager(contentNegotiationManager)
                .withDefaultHandlers(withDefaultHandlers)
                .withDefaultMessageSource(withDefaultMessageSource);

        for (Map.Entry<Class<? extends Exception>, ?> entry : exceptionHandlers.entrySet()) {
            Class<? extends Exception> exceptionClass = entry.getKey();
            Object value = entry.getValue();

            if (value instanceof RestExceptionHandler) {
                builder.addHandler(exceptionClass, (RestExceptionHandler) value);

            } else {
                builder.addErrorMessageHandler(exceptionClass, parseHttpStatus(value));
            }
        }

        return builder.build();
    }

    public Class<?> getObjectType() {
        return RestHandlerExceptionResolver.class;
    }

    public boolean isSingleton() {
        return false;
    }


    private HttpStatus parseHttpStatus(Object value) {

        if (value instanceof HttpStatus) {
            return (HttpStatus) value;

        } else if (value instanceof Integer) {
            return HttpStatus.valueOf((int) value);

        } else {
            throw new IllegalArgumentException(
                    "Handlers map value must be instance of ErrorResponseFactory, HttpStatus, or int.");
        }
    }
}
