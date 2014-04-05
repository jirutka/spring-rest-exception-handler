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
package cz.jirutka.spring.web.servlet.exhandler.handlers;

import org.apache.commons.lang3.reflect.TypeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.context.request.WebRequest;

import java.lang.reflect.TypeVariable;

public abstract class AbstractRestExceptionHandler<E extends Exception, T> implements RestExceptionHandler<E, T> {

    private static final Logger LOG = LoggerFactory.getLogger(AbstractRestExceptionHandler.class);

    private final Class<E> exceptionClass;
    private final HttpStatus status;


    protected AbstractRestExceptionHandler(HttpStatus status) {
        this.exceptionClass = determineTargetType();
        this.status = status;
        LOG.trace("Determined generic exception type: {}", exceptionClass.getName());
    }

    protected AbstractRestExceptionHandler(Class<E> exceptionClass, HttpStatus status) {
        this.exceptionClass = exceptionClass;
        this.status = status;
    }


    ////// Abstract methods //////

    public abstract T createBody(E ex, WebRequest req);


    ////// Template methods //////

    public ResponseEntity<T> handleException(E ex, WebRequest req) {

        T body = createBody(ex, req);
        HttpHeaders headers = createHeaders(ex, req);

        return new ResponseEntity<>(body, headers, getStatus());
    }

    public Class<E> getExceptionClass() {
        return exceptionClass;
    }

    public HttpStatus getStatus() {
        return status;
    }


    protected HttpHeaders createHeaders(E ex, WebRequest req) {
        return new HttpHeaders();
    }

    @SuppressWarnings("unchecked")
    private Class<E> determineTargetType() {
        TypeVariable<?> typeVar = AbstractRestExceptionHandler.class.getTypeParameters()[0];
        return (Class<E>) TypeUtils.getRawType(typeVar, this.getClass());
    }
}
