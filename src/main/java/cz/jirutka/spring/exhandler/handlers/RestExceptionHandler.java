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

import org.springframework.http.ResponseEntity;

import javax.servlet.http.HttpServletRequest;

/**
 * Contract for classes generating a {@link ResponseEntity} for an instance of the specified
 * Exception type, used in {@link cz.jirutka.spring.exhandler.RestHandlerExceptionResolver
 * RestHandlerExceptionResolver}.
 *
 * @param <E> Type of the handled exception.
 * @param <T> Type of the response message (entity body).
 */
public interface RestExceptionHandler<E extends Exception, T> {

    /**
     * Handles exception and generates {@link ResponseEntity}.
     *
     * @param exception The exception to handle and get data from.
     * @param request The current request.
     * @return A response entity.
     */
    ResponseEntity<T> handleException(E exception, HttpServletRequest request);
}
