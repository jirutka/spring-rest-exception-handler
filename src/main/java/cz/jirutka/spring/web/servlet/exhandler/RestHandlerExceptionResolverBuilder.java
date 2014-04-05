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

import cz.jirutka.spring.web.servlet.exhandler.handlers.*;
import cz.jirutka.spring.web.servlet.exhandler.interpolators.MessageInterpolator;
import cz.jirutka.spring.web.servlet.exhandler.interpolators.MessageInterpolatorAware;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.springframework.beans.ConversionNotSupportedException;
import org.springframework.beans.TypeMismatchException;
import org.springframework.context.HierarchicalMessageSource;
import org.springframework.context.MessageSource;
import org.springframework.context.MessageSourceAware;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.accept.ContentNegotiationManager;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.mvc.multiaction.NoSuchRequestHandlingMethodException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static cz.jirutka.spring.web.servlet.exhandler.MapUtils.putAllIfAbsent;
import static org.springframework.http.HttpStatus.*;

@Setter
@Accessors(fluent=true)
@SuppressWarnings("unchecked")
public final class RestHandlerExceptionResolverBuilder {

    public static final String DEFAULT_MESSAGES_BASENAME = "classpath:/cz/jirutka/spring/web/servlet/exhandler/messages";

    private final Map<Class, RestExceptionHandler> exceptionHandlers = new HashMap<>();

    private MessageSource messageSource;

    private MessageInterpolator messageInterpolator;

    private List<HttpMessageConverter<?>> httpMessageConverters;

    private ContentNegotiationManager contentNegotiationManager;

    private boolean withDefaultHandlers = true;

    private boolean withDefaultMessageSource = true;


    public RestHandlerExceptionResolver build() {

        if (withDefaultMessageSource) {
            if (messageSource != null) {
                // set default message source as top parent
                HierarchicalMessageSource messages = resolveRootMessageSource(messageSource);
                if (messages != null) {
                    messages.setParentMessageSource(createDefaultMessageSource());
                }
            } else {
                messageSource = createDefaultMessageSource();
            }
        }

        if (withDefaultHandlers) {
            // add default handlers
            putAllIfAbsent(exceptionHandlers, getDefaultHandlers());
        }

        // initialize handlers
        for (RestExceptionHandler handler : exceptionHandlers.values()) {
            if (messageSource != null && handler instanceof MessageSourceAware) {
                ((MessageSourceAware) handler).setMessageSource(messageSource);
            }
            if (messageInterpolator != null && handler instanceof MessageInterpolatorAware) {
                ((MessageInterpolatorAware) handler).setMessageInterpolator(messageInterpolator);
            }
        }

        RestHandlerExceptionResolver resolver = new RestHandlerExceptionResolver();
        resolver.setExceptionHandlers((Map) exceptionHandlers);

        if (httpMessageConverters != null) {
            resolver.setMessageConverters(httpMessageConverters);
        }
        if (contentNegotiationManager != null) {
            resolver.setContentNegotiationManager(contentNegotiationManager);
        }

        return resolver;
    }

    public <E extends Exception> RestHandlerExceptionResolverBuilder addHandler(
            Class<? extends E> exceptionClass, RestExceptionHandler<E, ?> exceptionHandler) {

        exceptionHandlers.put(exceptionClass, exceptionHandler);
        return this;
    }

    public <E extends Exception>
            RestHandlerExceptionResolverBuilder addHandler(AbstractRestExceptionHandler<E, ?> exceptionHandler) {

        return addHandler(exceptionHandler.getExceptionClass(), exceptionHandler);
    }

    public RestHandlerExceptionResolverBuilder addErrorMessageHandler(
            Class<? extends Exception> exceptionClass, HttpStatus status) {

        return addHandler(new ErrorMessageRestExceptionHandler<>(exceptionClass, status));
    }


    HierarchicalMessageSource resolveRootMessageSource(MessageSource messageSource) {

        if (messageSource instanceof HierarchicalMessageSource) {
            MessageSource parent = ((HierarchicalMessageSource) messageSource).getParentMessageSource();

            return parent != null ? resolveRootMessageSource(parent) : (HierarchicalMessageSource) messageSource;

        } else {
            return null;
        }
    }

    private Map<Class, RestExceptionHandler> getDefaultHandlers() {

        Map<Class, RestExceptionHandler> map = new HashMap<>();

        map.put( NoSuchRequestHandlingMethodException.class, new NoSuchRequestHandlingMethodExceptionHandler() );
        map.put( HttpRequestMethodNotSupportedException.class, new HttpRequestMethodNotSupportedExceptionHandler() );
        map.put( HttpMediaTypeNotSupportedException.class, new HttpMediaTypeNotSupportedExceptionHandler() );
        map.put( MethodArgumentNotValidException.class, new MethodArgumentNotValidExceptionHandler() );

        addHandlerTo( map, HttpMediaTypeNotAcceptableException.class, NOT_ACCEPTABLE );
        addHandlerTo( map, MissingServletRequestParameterException.class, BAD_REQUEST );
        addHandlerTo( map, ServletRequestBindingException.class, BAD_REQUEST );
        addHandlerTo( map, ConversionNotSupportedException.class, INTERNAL_SERVER_ERROR );
        addHandlerTo( map, TypeMismatchException.class, BAD_REQUEST );
        addHandlerTo( map, HttpMessageNotReadableException.class, UNPROCESSABLE_ENTITY );
        addHandlerTo( map, HttpMessageNotWritableException.class, INTERNAL_SERVER_ERROR );
        addHandlerTo( map, MissingServletRequestPartException.class, BAD_REQUEST );
        addHandlerTo( map, NoHandlerFoundException.class, NOT_FOUND );

        return map;
    }

    private void addHandlerTo(Map<Class, RestExceptionHandler> map, Class exceptionClass, HttpStatus status) {
        map.put(exceptionClass, new ErrorMessageRestExceptionHandler(exceptionClass, status));
    }

    private MessageSource createDefaultMessageSource() {

        ReloadableResourceBundleMessageSource messages = new ReloadableResourceBundleMessageSource();
        messages.setBasename(DEFAULT_MESSAGES_BASENAME);
        messages.setDefaultEncoding("UTF-8");
        messages.setFallbackToSystemLocale(false);

        return messages;
    }
}
