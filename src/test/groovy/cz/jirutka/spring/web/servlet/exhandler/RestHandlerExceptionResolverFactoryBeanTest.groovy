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

import cz.jirutka.spring.web.servlet.exhandler.handlers.ErrorMessageRestExceptionHandler
import cz.jirutka.spring.web.servlet.exhandler.interpolators.MessageInterpolator
import org.springframework.beans.factory.NoSuchBeanDefinitionException
import org.springframework.context.MessageSource
import org.springframework.web.accept.ContentNegotiationManager
import spock.lang.Specification
import spock.lang.Unroll

import static org.springframework.http.HttpStatus.*

class RestHandlerExceptionResolverFactoryBeanTest extends Specification {

    def factory = new RestHandlerExceptionResolverFactoryBean()

    def builder = Spy(RestHandlerExceptionResolverBuilder)
    def hackedFactory = new RestHandlerExceptionResolverFactoryBean() {
        def RestHandlerExceptionResolverBuilder createBuilder() { builder }
    }


    def 'ensure that factory produces RestHandlerExceptionResolver'() {
        expect:
            factory.objectType == RestHandlerExceptionResolver
            factory.getObject() instanceof RestHandlerExceptionResolver
    }

    @Unroll
    def 'parse HttpStatus from type: #type'() {
        expect:
            factory.parseHttpStatus(value) == expected
        where:
            value       | expected
            BAD_REQUEST | BAD_REQUEST
            404         | NOT_FOUND
            '409'       | CONFLICT

            type = value.getClass().getSimpleName()
    }

    @Unroll
    def 'fail to parse HttpStatus when given: #value'() {
        when:
            factory.parseHttpStatus(value)
        then:
            thrown IllegalArgumentException
        where:
            value << [null, '999', 3.14]
    }

    def 'process exception handlers map and add handlers to builder'() {
        setup:
            def malformedUrlHandler = new ErrorMessageRestExceptionHandler(MalformedURLException, UNPROCESSABLE_ENTITY)
        and:
            hackedFactory.exceptionHandlers = [
                    (NoSuchBeanDefinitionException): '500',
                    (MalformedURLException): malformedUrlHandler
            ]
        when:
            hackedFactory.getObject()
        then:
            1 * builder.addErrorMessageHandler(NoSuchBeanDefinitionException, INTERNAL_SERVER_ERROR)
            1 * builder.addHandler(MalformedURLException, malformedUrlHandler)
    }

    def 'set properties on builder'() {
        setup:
            hackedFactory.with {
                messageSource = Stub(MessageSource)
                messageInterpolator = Stub(MessageInterpolator)
                contentNegotiationManager = Stub(ContentNegotiationManager)
                withDefaultHandlers = false
                withDefaultMessageSource = true
            }
        when:
            hackedFactory.getObject()
        then:
            1 * builder.messageSource(_ as MessageSource)
            1 * builder.messageInterpolator(_ as MessageInterpolator)
            1 * builder.contentNegotiationManager(_ as ContentNegotiationManager)
            1 * builder.withDefaultHandlers(false)
            1 * builder.withDefaultMessageSource(true)
    }
}
