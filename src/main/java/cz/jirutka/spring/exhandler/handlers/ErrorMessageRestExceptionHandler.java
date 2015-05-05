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

import cz.jirutka.spring.exhandler.interpolators.MessageInterpolator;
import cz.jirutka.spring.exhandler.interpolators.MessageInterpolatorAware;
import cz.jirutka.spring.exhandler.interpolators.NoOpMessageInterpolator;
import cz.jirutka.spring.exhandler.interpolators.SpelMessageInterpolator;
import cz.jirutka.spring.exhandler.messages.ErrorMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.context.MessageSourceAware;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.util.Assert;

import javax.servlet.http.HttpServletRequest;
import java.net.URI;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * {@link RestExceptionHandler} that produces {@link ErrorMessage}.
 *
 * @param <E> Type of the handled exception.
 */
public class ErrorMessageRestExceptionHandler<E extends Exception>
        extends AbstractRestExceptionHandler<E, ErrorMessage> implements MessageSourceAware, MessageInterpolatorAware {

    private static final Logger LOG = LoggerFactory.getLogger(ErrorMessageRestExceptionHandler.class);

    protected static final String
            DEFAULT_PREFIX = "default",
            TYPE_KEY = "type",
            TITLE_KEY = "title",
            DETAIL_KEY = "detail",
            INSTANCE_KEY = "instance";

    private MessageSource messageSource;

    private MessageInterpolator interpolator = new SpelMessageInterpolator();


    /**
     * @param exceptionClass Type of the handled exceptions; it's used as a prefix of key to
     *                       resolve messages (via MessageSource).
     * @param status HTTP status that will be sent to client.
     */
    public ErrorMessageRestExceptionHandler(Class<E> exceptionClass, HttpStatus status) {
        super(exceptionClass, status);
    }

    /**
     * @see AbstractRestExceptionHandler#AbstractRestExceptionHandler(HttpStatus) AbstractRestExceptionHandler
     */
    protected ErrorMessageRestExceptionHandler(HttpStatus status) {
        super(status);
    }


    public ErrorMessage createBody(E ex, HttpServletRequest req) {

        ErrorMessage m = new ErrorMessage();
        m.setType(URI.create(resolveMessage(TYPE_KEY, ex, req)));
        m.setTitle(resolveMessage(TITLE_KEY, ex, req));
        m.setStatus(getStatus());
        m.setDetail(resolveMessage(DETAIL_KEY, ex, req));
        m.setInstance(URI.create(resolveMessage(INSTANCE_KEY, ex, req)));

        return m;
    }


    protected String resolveMessage(String key, E exception, HttpServletRequest request) {

        String template = getMessage(key, LocaleContextHolder.getLocale());

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

        String prefix = getExceptionClass().getName();

        String message = messageSource.getMessage(prefix + "." + key, null, null, locale);
        if (message == null) {
            message = messageSource.getMessage(DEFAULT_PREFIX + "." + key, null, null, locale);
        }
        if (message == null) {
            message = "";
            LOG.info("No message found for {}.{}, nor {}.{}", prefix, key, DEFAULT_PREFIX, key);
        }
        return message;
    }


    ////// Accessors //////

    public void setMessageSource(MessageSource messageSource) {
        Assert.notNull(messageSource, "messageSource must not be null");
        this.messageSource = messageSource;
    }

    public void setMessageInterpolator(MessageInterpolator interpolator) {
        this.interpolator = interpolator != null ? interpolator : new NoOpMessageInterpolator();
    }
}
