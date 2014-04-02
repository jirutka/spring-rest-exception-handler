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
package cz.jirutka.spring.web.servlet.exhandler

import cz.jirutka.spring.web.servlet.exhandler.factories.AbstractErrorResponseFactory
import cz.jirutka.spring.web.servlet.exhandler.factories.ErrorResponseFactory
import cz.jirutka.spring.web.servlet.exhandler.messages.ErrorMessage
import org.springframework.http.ResponseEntity
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.web.context.request.ServletWebRequest
import org.springframework.web.context.request.WebRequest
import spock.lang.Specification

import java.security.InvalidParameterException

import static org.springframework.http.HttpStatus.BAD_REQUEST
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR

class PluggableExceptionHandlerTest extends Specification {

    def request = new MockHttpServletRequest()
    def webRequest = new ServletWebRequest(request)
    def handler = new PluggableExceptionHandler()



    def 'add instance of ErrorResponseFactory and determine exception type using generics'() {
        setup:
            def factory = new ErrorResponseFactory<NumberFormatException, ?>() {
                ResponseEntity<?> createErrorResponse(NumberFormatException ex, WebRequest req) { }
            }
        when:
            handler.addResponseFactory(factory)
        then:
            handler.factories.get(NumberFormatException) == factory
    }

    def 'add instance of AbstractErrorResponseFactory'() {
        setup:
            def factory = new AbstractErrorResponseFactory<Exception, ErrorMessage>(IOException, BAD_REQUEST) {
                ErrorMessage createBody(Exception ex, WebRequest req) { null }
            }
        when:
            handler.addResponseFactory(factory as AbstractErrorResponseFactory)
        then:
           handler.factories.get(IOException) == factory
    }

    def 'return 500 when no response factory found'() {
        when:
            def result = handler.handleException(new IllegalArgumentException(), webRequest)
        then:
            result.statusCode == INTERNAL_SERVER_ERROR
    }

    def 'find response factory and handle exception'() {
        setup:
            def factories = new ErrorResponseFactory[3].collect{ Mock(ErrorResponseFactory) }
            def expected = new ResponseEntity(BAD_REQUEST)
        and:
            handler.addResponseFactory(NumberFormatException, factories[2])
            handler.addResponseFactory(IllegalArgumentException, factories[1])
            handler.addResponseFactory(Exception, factories[0])
        when:
            handler.handleException(exception, webRequest) == expected
        then:
            1 * factories[factoryNum].createErrorResponse(exception, webRequest) >> expected
            0 * _
        where:
            exception                       | factoryNum
            new NumberFormatException()     | 2
            new InvalidParameterException() | 1
            new FileNotFoundException()     | 0
    }
}
