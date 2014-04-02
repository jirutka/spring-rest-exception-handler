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
import cz.jirutka.spring.web.servlet.exhandler.interpolators.NoOpMessageInterpolator;
import cz.jirutka.spring.web.servlet.exhandler.messages.ErrorMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.util.Assert;
import org.springframework.web.context.request.WebRequest;

import java.net.URI;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import static org.apache.commons.lang3.ObjectUtils.defaultIfNull;

public class LocalizableErrorMessageFactory<E extends Exception> extends AbstractErrorMessageFactory<E> {

    private static final Logger LOG = LoggerFactory.getLogger(LocalizableErrorMessageFactory.class);

    private static final NoOpMessageInterpolator DEFAULT_INTERPOLATOR = new NoOpMessageInterpolator();

    protected static final String
            DEFAULT_PREFIX = "default",
            TYPE_KEY = "type",
            TITLE_KEY = "title",
            DETAIL_KEY = "detail",
            INSTANCE_KEY = "instance";


    private MessageSource messageSource;
    private MessageInterpolator interpolator = DEFAULT_INTERPOLATOR;
    private boolean fullyQualifiedNames = true;


    public LocalizableErrorMessageFactory(HttpStatus status) {
        super(status);
    }

    public LocalizableErrorMessageFactory(Class<E> exceptionClass, HttpStatus status) {
        super(exceptionClass, status);
    }


    public ErrorMessage createErrorMessage(E ex, WebRequest req) {

        ErrorMessage m = new ErrorMessage();
        m.setType(URI.create(resolveMessage(TYPE_KEY, ex, req)));
        m.setTitle(resolveMessage(TITLE_KEY, ex, req));
        m.setStatus(getStatus());
        m.setDetail(resolveMessage(DETAIL_KEY, ex, req));
        m.setInstance(URI.create(resolveMessage(INSTANCE_KEY, ex, req)));

        return m;
    }


    protected String resolveMessage(String key, E exception, WebRequest request) {

        String template = getMessage(key, request.getLocale());

        Map<String, Object> vars = new HashMap<>(2);
        vars.put("ex", exception);
        vars.put("req", request);

        return interpolateMessage(template, vars);
    }

    protected String interpolateMessage(String messageTemplate, Map<String, Object> variables) {

        LOG.trace("Interpolating message '{}' with variables: {}", messageTemplate, variables);
        return interpolator.interpolate(messageTemplate, variables);
    }

    protected String getMessage(String key, Locale locale) {

        String prefix = fullyQualifiedNames ? getExceptionClass().getName() : getExceptionClass().getSimpleName();

        String message = messageSource.getMessage(prefix + "." + key, null, null, locale);
        if (message == null) {
            message = messageSource.getMessage(DEFAULT_PREFIX + "." + key, null, null, locale);
        }
        if (message == null) {
            message = "";
            LOG.warn("No message found for {}.{}, nor {}.{}", prefix, key, DEFAULT_PREFIX, key);
        }
        return message;
    }


    ////// Accessors //////

    @Required
    public void setMessageSource(MessageSource messageSource) {
        Assert.notNull(messageSource, "messageSource must not be null");
        this.messageSource = messageSource;
    }

    public void setFullyQualifiedNames(boolean fullyQualifiedNames) {
        this.fullyQualifiedNames = fullyQualifiedNames;
    }

    public void setInterpolator(MessageInterpolator interpolator) {
        this.interpolator = defaultIfNull(interpolator, DEFAULT_INTERPOLATOR);
    }
}
