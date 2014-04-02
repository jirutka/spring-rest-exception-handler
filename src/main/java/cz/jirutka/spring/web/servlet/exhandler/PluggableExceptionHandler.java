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

import cz.jirutka.spring.web.servlet.exhandler.factories.AbstractErrorMessageFactory;
import cz.jirutka.spring.web.servlet.exhandler.factories.ErrorResponseFactory;
import org.apache.commons.lang3.reflect.TypeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;

import java.lang.reflect.TypeVariable;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;

@ControllerAdvice
public class PluggableExceptionHandler {

    private static final Logger LOG = LoggerFactory.getLogger(PluggableExceptionHandler.class);

    private final static ErrorResponseFactory DEFAULT_FACTORY = new ErrorResponseFactory() {

        public ResponseEntity<?> createErrorResponse(Exception ex, WebRequest request) {
            LOG.warn("No ErrorResponseFactory found for {}", ex.getClass().getName());
            return new ResponseEntity<>(INTERNAL_SERVER_ERROR);
        }
    };

    private final Map<Class<? extends Exception>, ErrorResponseFactory> factories = new LinkedHashMap<>();


    public <E extends Exception>
            void addResponseFactory(Class<? extends E> exceptionClass, ErrorResponseFactory<E> factory) {

        LOG.debug("Registering factory for {}: {}", exceptionClass.getName(), factory);
        factories.put(exceptionClass, factory);
    }

    public <E extends Exception> void addResponseFactory(ErrorResponseFactory<E> factory) {
        addResponseFactory(determineTargetType(factory), factory);
    }

    public <E extends Exception> void addResponseFactory(AbstractErrorMessageFactory<E> factory) {
        addResponseFactory(factory.getExceptionClass(), factory);
    }


    @ExceptionHandler
    protected ResponseEntity<?> handleException(Exception ex, WebRequest request) {

        ErrorResponseFactory factory = findErrorMessageFactory(ex.getClass());

        LOG.debug("Handling exception {} with response factory: {}", ex.getClass().getName(), factory);
        return factory.createErrorResponse(ex, request);
    }

    protected ErrorResponseFactory findErrorMessageFactory(Class<? extends Exception> exceptionClass) {

        for (Class clazz = exceptionClass; clazz != Throwable.class; clazz = clazz.getSuperclass()) {
            if (factories.containsKey(clazz)) {
                return factories.get(clazz);
            }
        }
        return DEFAULT_FACTORY;
    }


    @SuppressWarnings("unchecked")
    <T extends Exception> Class<T> determineTargetType(ErrorResponseFactory<T> factory) {

        TypeVariable<?> typeVar = ErrorResponseFactory.class.getTypeParameters()[0];
        return (Class<T>) TypeUtils.getRawType(typeVar, factory.getClass());
    }
}
