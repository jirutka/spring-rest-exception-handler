/*
 * Copyright 2014-2015 Jakub Jirutka <jakub@jirutka.cz>.
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

import cz.jirutka.spring.exhandler.handlers.RestExceptionHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.accept.ContentNegotiationManager;
import org.springframework.web.accept.FixedContentNegotiationStrategy;
import org.springframework.web.accept.HeaderContentNegotiationStrategy;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.method.support.HandlerMethodReturnValueHandler;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.handler.AbstractHandlerExceptionResolver;
import org.springframework.web.servlet.mvc.method.annotation.HttpEntityMethodProcessor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static cz.jirutka.spring.exhandler.support.HttpMessageConverterUtils.getDefaultHttpMessageConverters;
import static org.springframework.http.MediaType.APPLICATION_XML;
import static org.springframework.web.servlet.HandlerMapping.PRODUCIBLE_MEDIA_TYPES_ATTRIBUTE;

/**
 * A {@link org.springframework.web.servlet.HandlerExceptionResolver HandlerExceptionResolver}
 * for RESTful APIs that resolves exceptions through the provided {@link RestExceptionHandler
 * RestExceptionHandlers}.
 *
 * @see #builder()
 * @see RestHandlerExceptionResolverBuilder
 * @see RestHandlerExceptionResolverFactoryBean
 */
public class RestHandlerExceptionResolver extends AbstractHandlerExceptionResolver implements InitializingBean {

    private static final Logger LOG = LoggerFactory.getLogger(RestHandlerExceptionResolver.class);

    private final MethodParameter returnTypeMethodParam;

    private List<HttpMessageConverter<?>> messageConverters = getDefaultHttpMessageConverters();

    private Map<Class<? extends Exception>, RestExceptionHandler> handlers = new LinkedHashMap<>();

    private MediaType defaultContentType = APPLICATION_XML;

    private ContentNegotiationManager contentNegotiationManager;

    // package visibility for tests
    HandlerMethodReturnValueHandler responseProcessor;

    // package visibility for tests
    HandlerMethodReturnValueHandler fallbackResponseProcessor;


    /**
     * Returns a builder to build and configure instance of {@code RestHandlerExceptionResolver}.
     */
    public static RestHandlerExceptionResolverBuilder builder() {
        return new RestHandlerExceptionResolverBuilder();
    }


    public RestHandlerExceptionResolver() {

        Method method = ClassUtils.getMethod(
            RestExceptionHandler.class, "handleException", Exception.class, HttpServletRequest.class);

        returnTypeMethodParam = new MethodParameter(method, -1);
        // This method caches the resolved value, so it's convenient to initialize it
        // only once here.
        returnTypeMethodParam.getGenericParameterType();
    }


    @Override
    public void afterPropertiesSet() {
        if (contentNegotiationManager == null) {
            contentNegotiationManager = new ContentNegotiationManager(
                    new HeaderContentNegotiationStrategy(), new FixedContentNegotiationStrategy(defaultContentType));
        }
        responseProcessor = new HttpEntityMethodProcessor(messageConverters, contentNegotiationManager);
        fallbackResponseProcessor = new HttpEntityMethodProcessor(messageConverters,
                new ContentNegotiationManager(new FixedContentNegotiationStrategy(defaultContentType)));
    }

    @Override
    protected ModelAndView doResolveException(
            HttpServletRequest request, HttpServletResponse response, Object handler, Exception exception) {

        ResponseEntity<?> entity;
        try {
            entity = handleException(exception, request);
        } catch (NoExceptionHandlerFoundException ex) {
            LOG.warn("No exception handler found to handle exception: {}", exception.getClass().getName());
            return null;
        }
        try {
            processResponse(entity, new ServletWebRequest(request, response));
        } catch (Exception ex) {
            LOG.error("Failed to process error response: {}", entity, ex);
            return null;
        }
        return new ModelAndView();
    }

    protected ResponseEntity<?> handleException(Exception exception, HttpServletRequest request) {
        // See http://stackoverflow.com/a/12979543/2217862
        // This attribute is never set in MockMvc, so it's not covered in integration test.
        request.removeAttribute(PRODUCIBLE_MEDIA_TYPES_ATTRIBUTE);

        exception = resolveExceptionCause(exception);
        RestExceptionHandler<Exception, ?> handler = resolveExceptionHandler(exception.getClass());

        LOG.debug("Handling exception {} with response factory: {}", exception.getClass().getName(), handler);
        return handler.handleException(exception, request);
    }

    protected Exception resolveExceptionCause(Exception exception) {

        Throwable lastCause = null;

        for(Throwable cause = exception.getCause(); cause != null && (lastCause == null || !cause.equals(lastCause) && !cause.getClass().equals(lastCause.getClass())); cause = cause.getCause()) {
            Class clazz1 = cause.getClass();
            if(this.handlers.containsKey(clazz1)) {
                return (Exception) cause;
            }

            lastCause = cause;
        }

        return exception;

    }

    @SuppressWarnings("unchecked")
    protected RestExceptionHandler<Exception, ?> resolveExceptionHandler(Class<? extends Exception> exceptionClass) {

        for (Class clazz = exceptionClass; clazz != Throwable.class; clazz = clazz.getSuperclass()) {
            if (handlers.containsKey(clazz)) {
                return handlers.get(clazz);
            }
        }
        throw new NoExceptionHandlerFoundException();
    }

    protected void processResponse(ResponseEntity<?> entity, NativeWebRequest webRequest) throws Exception {

        // XXX: Create MethodParameter from the actually used subclass of RestExceptionHandler?
        MethodParameter methodParameter = new MethodParameter(returnTypeMethodParam);
        ModelAndViewContainer mavContainer = new ModelAndViewContainer();

        try {
            responseProcessor.handleReturnValue(entity, methodParameter, mavContainer, webRequest);

        } catch (HttpMediaTypeNotAcceptableException ex) {
            LOG.debug("Requested media type is not supported, falling back to default one");
            fallbackResponseProcessor.handleReturnValue(entity, methodParameter, mavContainer, webRequest);
        }
    }


    //////// Accessors ////////

    // Note: We're not using Lombok in this class to make it clear for debugging.

    public List<HttpMessageConverter<?>> getMessageConverters() {
        return messageConverters;
    }

    public void setMessageConverters(List<HttpMessageConverter<?>> messageConverters) {
        Assert.notNull(messageConverters, "messageConverters must not be null");
        this.messageConverters = messageConverters;
    }

    public ContentNegotiationManager getContentNegotiationManager() {
        return this.contentNegotiationManager;
    }

    public void setContentNegotiationManager(ContentNegotiationManager contentNegotiationManager) {
        this.contentNegotiationManager = contentNegotiationManager != null
                ? contentNegotiationManager : new ContentNegotiationManager();
    }

    public MediaType getDefaultContentType() {
        return defaultContentType;
    }

    public void setDefaultContentType(MediaType defaultContentType) {
        this.defaultContentType = defaultContentType;
    }

    public Map<Class<? extends Exception>, RestExceptionHandler> getExceptionHandlers() {
        return handlers;
    }

    public void setExceptionHandlers(Map<Class<? extends Exception>, RestExceptionHandler> handlers) {
        this.handlers = handlers;
    }


    //////// Inner classes ////////

    public static class NoExceptionHandlerFoundException extends RuntimeException {}
}
