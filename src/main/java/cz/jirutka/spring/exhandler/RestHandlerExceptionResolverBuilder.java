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
package cz.jirutka.spring.exhandler;

import cz.jirutka.spring.exhandler.handlers.*;
import cz.jirutka.spring.exhandler.interpolators.MessageInterpolator;
import cz.jirutka.spring.exhandler.interpolators.MessageInterpolatorAware;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.springframework.beans.ConversionNotSupportedException;
import org.springframework.beans.TypeMismatchException;
import org.springframework.context.HierarchicalMessageSource;
import org.springframework.context.MessageSource;
import org.springframework.context.MessageSourceAware;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.util.ClassUtils;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.accept.ContentNegotiationManager;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.mvc.multiaction.NoSuchRequestHandlingMethodException;

import javax.validation.ConstraintViolationException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static cz.jirutka.spring.exhandler.MapUtils.putAllIfAbsent;
import static lombok.AccessLevel.NONE;
import static org.springframework.http.HttpStatus.*;
import static org.springframework.util.StringUtils.hasText;

@Setter
@Accessors(fluent=true)
@SuppressWarnings("unchecked")
public class RestHandlerExceptionResolverBuilder {

    public static final String DEFAULT_MESSAGES_BASENAME = "classpath:/cz/jirutka/spring/exhandler/messages";

    private final Map<Class, RestExceptionHandler> exceptionHandlers = new HashMap<>();

    @Setter(NONE) // to not conflict with overloaded setter
    private MediaType defaultContentType;

    /**
     * The {@link ContentNegotiationManager} to use to resolve acceptable media types.
     * If not provided, the default instance of {@code ContentNegotiationManager} with
     * {@link org.springframework.web.accept.HeaderContentNegotiationStrategy HeaderContentNegotiationStrategy}
     * and {@link org.springframework.web.accept.FixedContentNegotiationStrategy FixedContentNegotiationStrategy}
     * (with {@link #defaultContentType(MediaType) defaultContentType}) will be used.
     */
    private ContentNegotiationManager contentNegotiationManager;

    /**
     * The message body converters to use for converting an error message into HTTP response body.
     * If not provided, the default converters will be used (see
     * {@link cz.jirutka.spring.exhandler.support.HttpMessageConverterUtils#getDefaultHttpMessageConverters()
     * getDefaultHttpMessageConverters()}).
     */
    private List<HttpMessageConverter<?>> httpMessageConverters;

    /**
     * The message interpolator to set into all exception handlers implementing
     * {@link cz.jirutka.spring.exhandler.interpolators.MessageInterpolatorAware}
     * interface, e.g. {@link ErrorMessageRestExceptionHandler}. Built-in exception handlers uses
     * {@link cz.jirutka.spring.exhandler.interpolators.SpelMessageInterpolator
     * SpelMessageInterpolator} by default.
     */
    private MessageInterpolator messageInterpolator;

    /**
     * The message source to set into all exception handlers implementing
     * {@link org.springframework.context.MessageSourceAware MessageSourceAware} interface, e.g.
     * {@link ErrorMessageRestExceptionHandler}. Required for built-in exception handlers.
     */
    private MessageSource messageSource;

    /**
     * Whether to register default exception handlers for Spring exceptions. These are registered
     * <i>before</i> the provided exception handlers, so you can overwrite any of the default
     * mappings. Default is <tt>true</tt>.
     */
    private boolean withDefaultHandlers = true;

    /**
     * Whether to use the default (built-in) message source as a fallback to resolve messages that
     * the provided message source can't resolve. In other words, it sets the default message
     * source as a <i>parent</i> of the provided message source. Default is <tt>true</tt>.
     */
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
        if (defaultContentType != null) {
            resolver.setDefaultContentType(defaultContentType);
        }
        resolver.afterPropertiesSet();

        return resolver;
    }

    /**
     * The default content type that will be used as a fallback when the requested content type is
     * not supported.
     */
    public RestHandlerExceptionResolverBuilder defaultContentType(MediaType mediaType) {
        this.defaultContentType = mediaType;
        return this;
    }

    /**
     * The default content type that will be used as a fallback when the requested content type is
     * not supported.
     */
    public RestHandlerExceptionResolverBuilder defaultContentType(String mediaType) {
        defaultContentType( hasText(mediaType) ? MediaType.parseMediaType(mediaType) : null );
        return this;
    }

    /**
     * Registers the given exception handler for the specified exception type. This handler will be
     * also used for all the exception subtypes, when no more specific mapping is found.
     *
     * @param exceptionClass The exception type handled by the given handler.
     * @param exceptionHandler An instance of the exception handler for the specified exception
     *                         type or its subtypes.
     */
    public <E extends Exception> RestHandlerExceptionResolverBuilder addHandler(
            Class<? extends E> exceptionClass, RestExceptionHandler<E, ?> exceptionHandler) {

        exceptionHandlers.put(exceptionClass, exceptionHandler);
        return this;
    }

    /**
     * Same as {@link #addHandler(Class, RestExceptionHandler)}, but the exception type is
     * determined from the handler.
     */
    public <E extends Exception>
            RestHandlerExceptionResolverBuilder addHandler(AbstractRestExceptionHandler<E, ?> exceptionHandler) {

        return addHandler(exceptionHandler.getExceptionClass(), exceptionHandler);
    }

    /**
     * Registers {@link ErrorMessageRestExceptionHandler} for the specified exception type.
     * This handler will be also used for all the exception subtypes, when no more specific mapping
     * is found.
     *
     * @param exceptionClass The exception type to handle.
     * @param status The HTTP status to map the specified exception to.
     */
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

        // this class does not exist in Spring 5
        if (ClassUtils.isPresent(
            "org.springframework.web.servlet.mvc.multiaction.NoSuchRequestHandlingMethodException",
            getClass().getClassLoader())) {
            map.put( NoSuchRequestHandlingMethodException.class, new NoSuchRequestHandlingMethodExceptionHandler() );
        }
        map.put( HttpRequestMethodNotSupportedException.class, new HttpRequestMethodNotSupportedExceptionHandler() );
        map.put( HttpMediaTypeNotSupportedException.class, new HttpMediaTypeNotSupportedExceptionHandler() );
        map.put( MethodArgumentNotValidException.class, new MethodArgumentNotValidExceptionHandler() );

        if (ClassUtils.isPresent("javax.validation.ConstraintViolationException", getClass().getClassLoader())) {
            map.put( ConstraintViolationException.class, new ConstraintViolationExceptionHandler() );
        }

        addHandlerTo( map, HttpMediaTypeNotAcceptableException.class, NOT_ACCEPTABLE );
        addHandlerTo( map, MissingServletRequestParameterException.class, BAD_REQUEST );
        addHandlerTo( map, ServletRequestBindingException.class, BAD_REQUEST );
        addHandlerTo( map, ConversionNotSupportedException.class, INTERNAL_SERVER_ERROR );
        addHandlerTo( map, TypeMismatchException.class, BAD_REQUEST );
        addHandlerTo( map, HttpMessageNotReadableException.class, UNPROCESSABLE_ENTITY );
        addHandlerTo( map, HttpMessageNotWritableException.class, INTERNAL_SERVER_ERROR );
        addHandlerTo( map, MissingServletRequestPartException.class, BAD_REQUEST );
        addHandlerTo(map, Exception.class, INTERNAL_SERVER_ERROR);

        // this class didn't exist before Spring 4.0
        try {
            Class clazz = Class.forName("org.springframework.web.servlet.NoHandlerFoundException");
            addHandlerTo(map, clazz, NOT_FOUND);
        } catch (ClassNotFoundException ex) {
            // ignore
        }
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
