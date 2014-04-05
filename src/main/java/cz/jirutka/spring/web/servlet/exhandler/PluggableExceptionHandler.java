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

import cz.jirutka.spring.web.servlet.exhandler.handlers.AbstractRestExceptionHandler;
import cz.jirutka.spring.web.servlet.exhandler.handlers.ResponseStatusRestExceptionHandler;
import cz.jirutka.spring.web.servlet.exhandler.handlers.RestExceptionHandler;
import org.apache.commons.lang3.reflect.TypeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.lang.reflect.TypeVariable;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.web.context.request.WebRequest.SCOPE_REQUEST;
import static org.springframework.web.servlet.HandlerMapping.PRODUCIBLE_MEDIA_TYPES_ATTRIBUTE;

@ControllerAdvice
public class PluggableExceptionHandler {

    private static final Logger LOG = LoggerFactory.getLogger(PluggableExceptionHandler.class);

    private final static RestExceptionHandler<Exception, Void> DEFAULT_FACTORY =
            new ResponseStatusRestExceptionHandler(INTERNAL_SERVER_ERROR);

    private final Map<Class<? extends Exception>, RestExceptionHandler> factories = new LinkedHashMap<>();


    public <E extends Exception>
            void addResponseFactory(Class<? extends E> exceptionClass, RestExceptionHandler<E, ?> factory) {

        LOG.debug("Registering factory for {}: {}", exceptionClass.getName(), factory);
        factories.put(exceptionClass, factory);
    }

    public <E extends Exception> void addResponseFactory(RestExceptionHandler<E, ?> factory) {
        addResponseFactory(determineTargetType(factory), factory);
    }

    public <E extends Exception> void addResponseFactory(AbstractRestExceptionHandler<E, ?> factory) {
        addResponseFactory(factory.getExceptionClass(), factory);
    }


    @org.springframework.web.bind.annotation.ExceptionHandler
    protected ResponseEntity<?> handleException(Exception ex, WebRequest request) {

        // See http://stackoverflow.com/a/12979543/2217862
        request.removeAttribute(PRODUCIBLE_MEDIA_TYPES_ATTRIBUTE, SCOPE_REQUEST);

        RestExceptionHandler<Exception, ?> factory = findErrorResponseFactory(ex.getClass());

        LOG.debug("Handling exception {} with response factory: {}", ex.getClass().getName(), factory);
        return factory.handleException(ex, request);
    }

    @SuppressWarnings("unchecked")
    protected RestExceptionHandler<Exception, ?> findErrorResponseFactory(Class<? extends Exception> exceptionClass) {

        for (Class clazz = exceptionClass; clazz != Throwable.class; clazz = clazz.getSuperclass()) {
            if (factories.containsKey(clazz)) {
                return factories.get(clazz);
            }
        }
        return DEFAULT_FACTORY;
    }


    @SuppressWarnings("unchecked")
    <E extends Exception> Class<E> determineTargetType(RestExceptionHandler<E, ?> factory) {

        TypeVariable<?> typeVar = RestExceptionHandler.class.getTypeParameters()[0];
        return (Class<E>) TypeUtils.getRawType(typeVar, factory.getClass());
    }
}
