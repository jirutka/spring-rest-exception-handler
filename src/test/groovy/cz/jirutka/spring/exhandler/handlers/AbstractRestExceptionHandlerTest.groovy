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
package cz.jirutka.spring.exhandler.handlers

import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.LoggingEvent
import ch.qos.logback.core.Appender
import cz.jirutka.spring.exhandler.messages.ErrorMessage
import org.slf4j.LoggerFactory
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.mock.web.MockHttpServletRequest
import spock.lang.Ignore
import spock.lang.Specification
import spock.lang.Unroll

import javax.servlet.http.HttpServletRequest

import static ch.qos.logback.classic.Level.*
import static org.springframework.http.HttpStatus.BAD_REQUEST

class AbstractRestExceptionHandlerTest extends Specification {

    def 'determine exception class from generic type'() {
        given:
            def factory = new AbstractRestExceptionHandler<IOException, ErrorMessage>(BAD_REQUEST) {
                ErrorMessage createBody(IOException ex, HttpServletRequest req) { null}
            }
        expect:
            factory.exceptionClass == IOException
    }

    def 'handle exception using createHeaders, createBody and getStatus methods'() {
        setup:
            def ex = new IOException()
            def req = new MockHttpServletRequest()
            def expected = new ResponseEntity(new ErrorMessage(), new HttpHeaders(date: 123), BAD_REQUEST)
        and:
            def factory = Spy(AbstractRestExceptionHandler, constructorArgs: [IOException, expected.statusCode]) {
                createHeaders(ex, req) >> expected.headers
                createBody(ex, req) >> expected.body
            }
        when:
            def actual = factory.handleException(ex, req)
        then:
            actual == expected
    }

    @Ignore
    @Unroll
    def 'log exception with status #status on level #expectedLevel #stackTrace'() {
        setup:
            def factory = new AbstractRestExceptionHandler<Exception, ErrorMessage>(HttpStatus.valueOf(status)) {
                ErrorMessage createBody(Exception ex, HttpServletRequest req) { null }
            }
            def exception = new IOException()
            def logAppender = Mock(Appender)
            LoggingEvent actual = null
        and:
            (LoggerFactory.getLogger(RestExceptionHandler) as Logger).with {
                level = loggerLevel
                addAppender(logAppender)
            }
        when:
            factory.handleException(exception, new MockHttpServletRequest())
        then:
            //1 * logAppender.doAppend({ actual = it }) // FIXME does not compile
            actual.level == expectedLevel
            actual.marker.name == exception.class.name
            actual.throwableProxy == null ^ hasThrowable
        where:
            status | loggerLevel | expectedLevel | hasThrowable
            503    | INFO        | ERROR         | true
            404    | INFO        | INFO          | false
            404    | TRACE       | DEBUG         | true

            stackTrace = "${hasThrowable ? 'with' : 'without'} stack trace"
    }
}
