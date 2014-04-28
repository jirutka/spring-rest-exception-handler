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
import org.springframework.web.HttpRequestMethodNotSupportedException
import org.springframework.web.servlet.DispatcherServlet
import spock.lang.Specification

import static org.springframework.http.HttpMethod.POST
import static org.springframework.http.HttpMethod.PUT

class HttpRequestMethodNotSupportedExceptionHandlerTest extends Specification {

    def handler = new HttpRequestMethodNotSupportedExceptionHandler()
    def request = new MockHttpServletRequest()


    def 'create headers with "Allow" when supported methods are specified'() {
        given:
            def exception = new HttpRequestMethodNotSupportedException('PATCH', ['PUT', 'POST'])
        when:
            def headers = handler.createHeaders(exception, request)
        then:
            headers.getAllow() == [PUT, POST] as Set
    }

    def 'create headers without "Allow" when supported methods are not specified'() {
        given:
            def exception = new HttpRequestMethodNotSupportedException('PATCH')
        when:
            def result = handler.createHeaders(exception, request)
        then:
            ! result.get('Allow')
    }

    def 'log exception message in PageNotFound logger'() {
        setup:
            def logAppender = Mock(Appender)
            (LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME) as Logger).addAppender(logAppender)
        and:
            def spiedHandler = Spy(HttpRequestMethodNotSupportedExceptionHandler) {
                createBody(_, _) >> null
            }
            def exception = new HttpRequestMethodNotSupportedException('PUT')
        when:
            spiedHandler.handleException(exception, request)
        then:
            1 * logAppender.doAppend({ LoggingEvent it ->
                it.message    == exception.message &&
                it.loggerName == DispatcherServlet.PAGE_NOT_FOUND_LOG_CATEGORY
            })
    }
}
