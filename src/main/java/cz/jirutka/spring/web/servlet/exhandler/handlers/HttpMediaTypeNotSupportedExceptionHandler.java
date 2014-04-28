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

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.HttpMediaTypeNotSupportedException;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

import static org.springframework.http.HttpStatus.UNSUPPORTED_MEDIA_TYPE;
import static org.springframework.util.CollectionUtils.isEmpty;

public class HttpMediaTypeNotSupportedExceptionHandler extends ErrorMessageRestExceptionHandler<HttpMediaTypeNotSupportedException> {


    public HttpMediaTypeNotSupportedExceptionHandler() {
        super(UNSUPPORTED_MEDIA_TYPE);
    }

    @Override
    protected HttpHeaders createHeaders(HttpMediaTypeNotSupportedException ex, HttpServletRequest req) {

        HttpHeaders headers = super.createHeaders(ex, req);
        List<MediaType> mediaTypes = ex.getSupportedMediaTypes();

        if (!isEmpty(mediaTypes)) {
            headers.setAccept(mediaTypes);
        }
        return headers;
    }
}
