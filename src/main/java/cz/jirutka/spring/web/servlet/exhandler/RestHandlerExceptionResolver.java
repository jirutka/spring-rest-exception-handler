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

import cz.jirutka.spring.web.servlet.exhandler.handlers.ResponseStatusRestExceptionHandler;
import cz.jirutka.spring.web.servlet.exhandler.handlers.RestExceptionHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.util.Assert;
import org.springframework.web.accept.ContentNegotiationManager;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.support.HandlerMethodReturnValueHandler;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.handler.AbstractHandlerExceptionResolver;
import org.springframework.web.servlet.mvc.method.annotation.HttpEntityMethodProcessor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static cz.jirutka.spring.web.servlet.exhandler.support.HttpMessageConverterUtils.getDefaultHttpMessageConverters;
import static org.apache.commons.lang3.ObjectUtils.defaultIfNull;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;

public class RestHandlerExceptionResolver extends AbstractHandlerExceptionResolver implements InitializingBean {

    private static final Logger LOG = LoggerFactory.getLogger(RestHandlerExceptionResolver.class);

    private List<HttpMessageConverter<?>> messageConverters = getDefaultHttpMessageConverters();

    private ContentNegotiationManager contentNegotiationManager = new ContentNegotiationManager();

    private Map<Class<? extends Exception>, RestExceptionHandler> handlers = new LinkedHashMap<>();

    // package visibility for tests
    HandlerMethodReturnValueHandler returnValueHandler;



    public static RestHandlerExceptionResolverBuilder builder() {
        return new RestHandlerExceptionResolverBuilder();
    }


    @Override
    public void afterPropertiesSet() {
        returnValueHandler = new HttpEntityMethodProcessor(messageConverters, contentNegotiationManager);
    }

    @Override
    protected ModelAndView doResolveException(
            HttpServletRequest request, HttpServletResponse response, Object handler, Exception exception) {

        ServletWebRequest webRequest = new ServletWebRequest(request, response);
        ModelAndViewContainer mavContainer = new ModelAndViewContainer();

        ResponseEntity<?> entity = handleException(exception, webRequest);

        try {
            returnValueHandler.handleReturnValue(entity, null, mavContainer, webRequest);
        } catch (Exception ex) {
            LOG.error("Failed to process error response: {}", entity, ex);
            return null;
        }
        return new ModelAndView();
    }

    protected ResponseEntity<?> handleException(Exception exception, WebRequest request) {

        RestExceptionHandler<Exception, ?> handler = resolveExceptionHandler(exception.getClass());

        LOG.debug("Handling exception {} with response factory: {}", exception.getClass().getName(), handler);
        return handler.handleException(exception, request);
    }

    @SuppressWarnings("unchecked")
    protected RestExceptionHandler<Exception, ?> resolveExceptionHandler(Class<? extends Exception> exceptionClass) {

        for (Class clazz = exceptionClass; clazz != Throwable.class; clazz = clazz.getSuperclass()) {
            if (handlers.containsKey(clazz)) {
                return handlers.get(clazz);
            }
        }
        return new ResponseStatusRestExceptionHandler(INTERNAL_SERVER_ERROR);
    }


    //////// Accessors ////////

    /**
     * Set the message body converters to use.
     * These converters are used to convert from and to HTTP requests and
     * responses.
     */
    public void setMessageConverters(List<HttpMessageConverter<?>> messageConverters) {
        Assert.notNull(messageConverters, "messageConverters must not be null");
        this.messageConverters = messageConverters;
    }

    /**
     * Return the configured message body converters.
     */
    public List<HttpMessageConverter<?>> getMessageConverters() {
        return messageConverters;
    }

    /**
     * Set the {@link ContentNegotiationManager} to use to determine requested
     * media types. If not set, the default constructor is used.
     */
    public void setContentNegotiationManager(ContentNegotiationManager contentNegotiationManager) {
        this.contentNegotiationManager = defaultIfNull(contentNegotiationManager, new ContentNegotiationManager());
    }

    /**
     * Return the configured {@link ContentNegotiationManager}.
     */
    public ContentNegotiationManager getContentNegotiationManager() {
        return this.contentNegotiationManager;
    }

    public Map<Class<? extends Exception>, RestExceptionHandler> getExceptionHandlers() {
        return handlers;
    }

    public void setExceptionHandlers(Map<Class<? extends Exception>, RestExceptionHandler> handlers) {
        this.handlers = handlers;
    }
}
