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

import cz.jirutka.spring.web.servlet.exhandler.messages.ErrorMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.DispatcherServlet;

import static org.springframework.http.HttpStatus.METHOD_NOT_ALLOWED;
import static org.springframework.util.ObjectUtils.isEmpty;

public class HttpRequestMethodNotSupportedResponseFactory
        extends LocalizableErrorMessageFactory<HttpRequestMethodNotSupportedException> {

    private static final Logger LOG = LoggerFactory.getLogger(DispatcherServlet.PAGE_NOT_FOUND_LOG_CATEGORY);

    public HttpRequestMethodNotSupportedResponseFactory() {
        super(METHOD_NOT_ALLOWED);
    }

    @Override
    public ResponseEntity<ErrorMessage> createErrorResponse(HttpRequestMethodNotSupportedException ex, WebRequest req) {
        LOG.warn(ex.getMessage());

        return super.createErrorResponse(ex, req);
    }

    @Override
    protected HttpHeaders createHeaders(HttpRequestMethodNotSupportedException ex, WebRequest req) {

        HttpHeaders headers = super.createHeaders(ex, req);

        if (!isEmpty(ex.getSupportedMethods())) {
            headers.setAllow(ex.getSupportedHttpMethods());
        }
        return headers;
    }
}
