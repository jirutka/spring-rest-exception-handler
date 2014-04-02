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
package cz.jirutka.spring.web.servlet.exhandler.factories;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.context.request.WebRequest;

/**
 * Simple implementation of {@link ErrorResponseFactory} that returns response
 * with status code and no content.
 */
public class StatusErrorResponseFactory implements ErrorResponseFactory<Exception> {

    private final HttpStatus status;


    public StatusErrorResponseFactory(HttpStatus status) {
        this.status = status;
    }

    public ResponseEntity<?> createErrorResponse(Exception ex, WebRequest request) {
        return new ResponseEntity<>(status);
    }
}
