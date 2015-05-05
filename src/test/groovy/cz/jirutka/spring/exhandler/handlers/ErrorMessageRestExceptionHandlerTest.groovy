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

import cz.jirutka.spring.exhandler.interpolators.MessageInterpolator
import cz.jirutka.spring.exhandler.messages.ErrorMessage
import org.springframework.beans.TypeMismatchException
import org.springframework.context.MessageSource
import org.springframework.context.i18n.LocaleContextHolder
import org.springframework.mock.web.MockHttpServletRequest
import spock.lang.Shared
import spock.lang.Specification

import static ErrorMessageRestExceptionHandler.DEFAULT_PREFIX
import static java.util.Locale.ENGLISH
import static java.util.Locale.JAPANESE
import static org.springframework.http.HttpStatus.BAD_REQUEST

class ErrorMessageRestExceptionHandlerTest extends Specification {

    @Shared exceptionClass = TypeMismatchException

    def messageSource = Mock(MessageSource)
    def interpolator = Mock(MessageInterpolator)
    def request = new MockHttpServletRequest()

    def handler = Spy(ErrorMessageRestExceptionHandler, constructorArgs: [exceptionClass, BAD_REQUEST])

    void setup() {
        handler.messageSource = messageSource
        handler.messageInterpolator = interpolator
    }


    def 'createBody: create ErrorMessage using resolveMessage'() {
        setup:
            def exception = new TypeMismatchException(1, String)
            def expected = new ErrorMessage(
                    type: new URI('http://httpstatus.es/400'),
                    title: 'Type Mismatch',
                    status: 400,
                    detail: "You're screwed!",
                    instance: new URI('http://example.org/type-mismatch'))
        when:
            def actual = handler.createBody(exception, request)
        then:
            ['type', 'title', 'detail', 'instance'].each { key ->
                1 * handler.resolveMessage(key, exception, request) >> expected.properties[key].toString()
            }
        and:
            actual == expected
    }

    def 'resolveMessage: obtain message using getMessage() and interpolate it'() {
        setup:
            def ex = new TypeMismatchException(1, String)
            def msgTemplate = 'Type mismatch on value: #{value}'
            def msg = 'Type mismatch on value: 1'
            LocaleContextHolder.locale = JAPANESE
        when:
            def result = handler.resolveMessage('detail', ex, request)
        then:
            1 * handler.getMessage('detail', JAPANESE) >> msgTemplate
        then:
            1 * interpolator.interpolate(msgTemplate, [ex: ex, req: request]) >> msg
        and:
            result == msg
    }

    def 'getMessage: find message for this exception class'() {
        setup:
            def expected = 'Chunky bacon'
        when:
            def actual = handler.getMessage('title', ENGLISH)
        then:
            1 * messageSource.getMessage("${exceptionClass.name}.title", null, _, ENGLISH) >> expected
        and:
            actual == expected
    }

    def 'getMessage: find default message when no message for the exception class is found'() {
        given:
            def expected = 'Chunky bacon'
        when:
            def actual = handler.getMessage('type', ENGLISH)
        then:
            1 * messageSource.getMessage("${exceptionClass.name}.type", null, _, ENGLISH) >> null
        then:
            1 * messageSource.getMessage("${DEFAULT_PREFIX}.type", null, _, ENGLISH) >> expected
        and:
            actual == expected
    }

    def 'getMessage: return empty string if no message is found'() {
        setup:
            messageSource._ >> null
        expect:
            handler.getMessage('type', ENGLISH) == ''
    }

}
