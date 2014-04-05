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
package cz.jirutka.spring.web.servlet.exhandler.handlers

import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.LoggingEvent
import ch.qos.logback.core.Appender
import org.slf4j.LoggerFactory
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.web.servlet.DispatcherServlet
import org.springframework.web.servlet.mvc.multiaction.NoSuchRequestHandlingMethodException
import spock.lang.Specification

class NoSuchRequestHandlingMethodExceptionHandlerTest extends Specification {


    def 'log exception in PageNotFound logger'() {
        setup:
            def logAppender = Mock(Appender)
            (LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME) as Logger).addAppender(logAppender)
        and:
            def handler = Spy(NoSuchRequestHandlingMethodExceptionHandler) {
                createBody(_, _) >> null
            }
            def exception = new NoSuchRequestHandlingMethodException(new MockHttpServletRequest())
        when:
            handler.handleException(exception, null)
        then:
            1 * logAppender.doAppend({ LoggingEvent it ->
                it.message    == exception.message &&
                it.loggerName == DispatcherServlet.PAGE_NOT_FOUND_LOG_CATEGORY
            })
    }
}
