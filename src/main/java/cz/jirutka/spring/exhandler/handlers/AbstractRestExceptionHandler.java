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
package cz.jirutka.spring.exhandler.handlers;

import org.apache.commons.lang3.reflect.TypeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.TypeVariable;

/**
 * The base implementation of the {@link RestExceptionHandler} interface.
 */
public abstract class AbstractRestExceptionHandler<E extends Exception, T> implements RestExceptionHandler<E, T> {

    private static final Logger LOG = LoggerFactory.getLogger(RestExceptionHandler.class);

    private final Class<E> exceptionClass;
    private final HttpStatus status;


    /**
     * This constructor determines the exception class from the generic class parameter {@code E}.
     *
     * @param status HTTP status
     */
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

    public abstract T createBody(E ex, HttpServletRequest req);


    ////// Template methods //////

    public ResponseEntity<T> handleException(E ex, HttpServletRequest req) {

        logException(ex, req);

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


    protected HttpHeaders createHeaders(E ex, HttpServletRequest req) {
        return new HttpHeaders();
    }

    /**
     * Logs the exception; on ERROR level when status is 5xx, otherwise on INFO level without stack
     * trace, or DEBUG level with stack trace. The logger name is
     * {@code cz.jirutka.spring.exhandler.handlers.RestExceptionHandler}.
     *
     * @param ex The exception to log.
     * @param req The current web request.
     */
    protected void logException(E ex, HttpServletRequest req) {

        if (LOG.isErrorEnabled() && getStatus().value() >= 500 || LOG.isInfoEnabled()) {
            Marker marker = MarkerFactory.getMarker(ex.getClass().getName());

            String uri = req.getRequestURI();
            if (req.getQueryString() != null) {
                uri += '?' + req.getQueryString();
            }
            String msg = String.format("%s %s ~> %s", req.getMethod(), uri, getStatus());

            if (getStatus().value() >= 500) {
                LOG.error(marker, msg, ex);

            } else if (LOG.isDebugEnabled()) {
                LOG.debug(marker, msg, ex);

            } else {
                LOG.info(marker, msg);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private Class<E> determineTargetType() {
        TypeVariable<?> typeVar = AbstractRestExceptionHandler.class.getTypeParameters()[0];
        return (Class<E>) TypeUtils.getRawType(typeVar, this.getClass());
    }
}
