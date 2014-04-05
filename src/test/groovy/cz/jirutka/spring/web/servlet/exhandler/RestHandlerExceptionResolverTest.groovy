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

import cz.jirutka.spring.web.servlet.exhandler.handlers.RestExceptionHandler
import org.springframework.http.ResponseEntity
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.web.bind.ServletRequestBindingException
import org.springframework.web.context.request.WebRequest
import org.springframework.web.method.support.HandlerMethodReturnValueHandler
import org.springframework.web.method.support.ModelAndViewContainer
import org.springframework.web.servlet.mvc.method.annotation.HttpEntityMethodProcessor
import spock.lang.Specification

import java.security.InvalidParameterException

import static org.springframework.http.HttpStatus.BAD_REQUEST
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR

class RestHandlerExceptionResolverTest extends Specification {

    def resolver = new RestHandlerExceptionResolver()
    def request = new MockHttpServletRequest()
    def response = new MockHttpServletResponse()

    def returnValueHandler = Mock(HandlerMethodReturnValueHandler)
    def responseFactory = Mock(RestExceptionHandler)

    void setup() {
        resolver.returnValueHandler = returnValueHandler
    }


    def 'initialize returnValueHandler after properties set'() {
        setup:
            def newResolver = new RestHandlerExceptionResolver()
        when:
            newResolver.afterPropertiesSet()
        then:
            newResolver.returnValueHandler instanceof HttpEntityMethodProcessor
    }

    def 'resolve exception'() {
        setup:
            def exception = new ServletRequestBindingException('')
            def entity = new ResponseEntity(BAD_REQUEST)
        and:
            resolver.exceptionHandlers[Exception] = responseFactory
        when:
            resolver.doResolveException(request, response, null, exception)
        then:
            1 * responseFactory.handleException(exception, { req ->
                req.request == request && req.response == response
            }) >> entity
            1 * returnValueHandler.handleReturnValue(entity, _, _ as ModelAndViewContainer, { req ->
                req.request == request && req.response == response
            })
    }

    def 'find response factory and handle exception'() {
        setup:
            def factories = new RestExceptionHandler[3].collect{ Mock(RestExceptionHandler) }
            def expected = new ResponseEntity(BAD_REQUEST)
        and:
            resolver.exceptionHandlers = [
                    (NumberFormatException): factories[2],
                    (IllegalArgumentException): factories[1],
                    (Exception): factories[0]
            ]
        when:
            resolver.doResolveException(request, response, null, exception)
        then:
            1 * factories[factoryNum].handleException(exception, _ as WebRequest) >> expected
        where:
            exception                       | factoryNum
            new NumberFormatException()     | 2
            new InvalidParameterException() | 1
            new FileNotFoundException()     | 0
    }

    def 'return 500 when no handler found'() {
        setup:
            def entity = new ResponseEntity(INTERNAL_SERVER_ERROR)
        when:
            resolver.doResolveException(request, response, null, new IOException())
        then:
           1 * returnValueHandler.handleReturnValue(entity, _, _, _)
    }

    def 'return null when HttpEntityMethodProcessor throws an exception'() {
        setup:
            returnValueHandler.handleReturnValue(*_) >> { throw new IllegalStateException() }
        expect:
            resolver.doResolveException(request, response, null, new IOException()) == null
    }
}
