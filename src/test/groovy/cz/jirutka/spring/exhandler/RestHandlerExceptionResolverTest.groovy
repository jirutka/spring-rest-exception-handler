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
package cz.jirutka.spring.exhandler

import cz.jirutka.spring.exhandler.handlers.RestExceptionHandler
import org.springframework.http.ResponseEntity
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.web.HttpMediaTypeNotAcceptableException
import org.springframework.web.bind.ServletRequestBindingException
import org.springframework.web.method.support.HandlerMethodReturnValueHandler
import org.springframework.web.method.support.ModelAndViewContainer
import org.springframework.web.servlet.mvc.method.annotation.HttpEntityMethodProcessor
import spock.lang.Specification

import javax.servlet.http.HttpServletRequest
import java.security.InvalidParameterException

import static org.springframework.http.HttpStatus.BAD_REQUEST
import static org.springframework.http.MediaType.APPLICATION_JSON
import static org.springframework.web.servlet.HandlerMapping.PRODUCIBLE_MEDIA_TYPES_ATTRIBUTE

class RestHandlerExceptionResolverTest extends Specification {

    def resolver = new RestHandlerExceptionResolver()
    def request = new MockHttpServletRequest()
    def response = new MockHttpServletResponse()
    def respEntity = new ResponseEntity(BAD_REQUEST)

    def responseProc = Mock(HandlerMethodReturnValueHandler)
    def fallbackResponseProc = Mock(HandlerMethodReturnValueHandler)
    def responseFactory = Mock(RestExceptionHandler)

    void setup() {
        resolver.responseProcessor = responseProc
        resolver.fallbackResponseProcessor = fallbackResponseProc
        resolver.exceptionHandlers[Exception] = responseFactory
    }


    def 'initialize responseProcessor and fallbackResponseProcessor after properties set'() {
        setup:
            def newResolver = new RestHandlerExceptionResolver()
        when:
            newResolver.afterPropertiesSet()
        then:
            newResolver.responseProcessor instanceof HttpEntityMethodProcessor
            newResolver.fallbackResponseProcessor instanceof HttpEntityMethodProcessor
    }

    def 'resolve exception and process error response'() {
        setup:
            def exception = new ServletRequestBindingException('')
        when:
            resolver.doResolveException(request, response, null, exception)
        then:
            1 * responseFactory.handleException(exception, request) >> respEntity
            1 * responseProc.handleReturnValue(respEntity, _, _ as ModelAndViewContainer, { req ->
                req.request == request && req.response == response
            })
    }

    def 'resolve exception handler when multiple are available'() {
        setup:
            def factories = new RestExceptionHandler[3].collect{ Mock(RestExceptionHandler) }
        and:
            resolver.exceptionHandlers = [
                    (NumberFormatException): factories[2],
                    (IllegalArgumentException): factories[1],
                    (Exception): factories[0]
            ]
        when:
            resolver.doResolveException(request, response, null, exception)
        then:
            1 * factories[factoryNum].handleException(exception, _ as HttpServletRequest) >> respEntity
        where:
            exception                       | factoryNum
            new NumberFormatException()     | 2
            new InvalidParameterException() | 1
            new FileNotFoundException()     | 0
    }

    def 'fallback to default media type when requested media type is not supported'() {
        setup:
            resolver.defaultContentType = APPLICATION_JSON
        and:
            responseFactory.handleException(*_) >> respEntity
            responseProc.handleReturnValue(*_) >> { throw new HttpMediaTypeNotAcceptableException([]) }
        when:
            resolver.doResolveException(request, response, null, new Exception())
        then:
            1 * fallbackResponseProc.handleReturnValue(respEntity, _, _ as ModelAndViewContainer, { req ->
                req.request == request && req.response == response
            })
    }

    def 'return null when no exception handler is found'() {
        setup:
            resolver.exceptionHandlers = [:]
        expect:
            resolver.doResolveException(request, response, null, new IOException()) == null
    }

    def 'return null when response processor throws an exception'() {
        setup:
            responseProc.handleReturnValue(*_) >> { throw new IllegalStateException() }
        expect:
            resolver.doResolveException(request, response, null, new IOException()) == null
    }

    def 'remove PRODUCIBLE_MEDIA_TYPES_ATTRIBUTE from the request'() {
        setup:
            resolver.exceptionHandlers[Exception] = responseFactory
            request.setAttribute(PRODUCIBLE_MEDIA_TYPES_ATTRIBUTE, ['image/png'])
        when:
            resolver.doResolveException(request, response, null, new Exception())
        then:
            ! request.getAttribute(PRODUCIBLE_MEDIA_TYPES_ATTRIBUTE)
    }
}
