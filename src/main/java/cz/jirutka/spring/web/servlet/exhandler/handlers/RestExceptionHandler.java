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

import org.springframework.http.ResponseEntity;
import org.springframework.web.context.request.WebRequest;

public interface RestExceptionHandler<E extends Exception, T> {

    /**
     * Handles exception and generates {@link ResponseEntity}.
     *
     * @param exception The exception to get data from.
     * @param request The current request.
     * @return A response entity.
     */
    ResponseEntity<T> handleException(E exception, WebRequest request);
}
