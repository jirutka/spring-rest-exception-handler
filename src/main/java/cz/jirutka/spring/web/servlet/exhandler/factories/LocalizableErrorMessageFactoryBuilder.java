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

import cz.jirutka.spring.web.servlet.exhandler.interpolators.MessageInterpolator;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.springframework.beans.ConversionNotSupportedException;
import org.springframework.beans.TypeMismatchException;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.util.ArrayList;
import java.util.List;

import static org.springframework.http.HttpStatus.*;

@Setter
@Accessors(fluent=true)
public final class LocalizableErrorMessageFactoryBuilder {

    private final List<LocalizableErrorMessageFactory> factories = new ArrayList<>();

    private MessageSource messageSource;

    private MessageInterpolator interpolator;

    private boolean withDefaults = true;


    public List<LocalizableErrorMessageFactory> build() {

        if (withDefaults) {
            add( new NoSuchRequestHandlingMethodResponseFactory() );
            add( new HttpRequestMethodNotSupportedResponseFactory() );
            add( new HttpMediaTypeNotSupportedResponseFactory() );
            add( new MethodArgumentNotValidResponseFactory() );

            map( HttpMediaTypeNotAcceptableException.class, NOT_ACCEPTABLE );
            map( MissingServletRequestParameterException.class, BAD_REQUEST );
            map( ServletRequestBindingException.class, BAD_REQUEST );
            map( ConversionNotSupportedException.class, INTERNAL_SERVER_ERROR );
            map( TypeMismatchException.class, BAD_REQUEST );
            map( HttpMessageNotReadableException.class, UNPROCESSABLE_ENTITY );
            map( HttpMessageNotWritableException.class, INTERNAL_SERVER_ERROR );
            map( MethodArgumentNotValidException.class, BAD_REQUEST );
            map( MissingServletRequestPartException.class, BAD_REQUEST );
            map( NoHandlerFoundException.class, NOT_FOUND );
        }
        return factories;
    }

    public LocalizableErrorMessageFactoryBuilder add(LocalizableErrorMessageFactory factory) {

        initialize(factory);
        factories.add(factory);
        return this;
    }

    public LocalizableErrorMessageFactoryBuilder map(Class<? extends Exception> exceptionClass, HttpStatus status) {

        factories.add(initialize(new LocalizableErrorMessageFactory<>(exceptionClass, status)));
        return this;
    }


    private LocalizableErrorMessageFactory initialize(LocalizableErrorMessageFactory factory) {

        factory.setMessageSource(messageSource);
        factory.setInterpolator(interpolator);

        return factory;
    }
}
