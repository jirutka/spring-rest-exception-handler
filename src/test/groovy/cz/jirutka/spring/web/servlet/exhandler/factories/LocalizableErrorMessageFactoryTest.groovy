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
package cz.jirutka.spring.web.servlet.exhandler.factories

import cz.jirutka.spring.web.servlet.exhandler.interpolators.MessageInterpolator
import cz.jirutka.spring.web.servlet.exhandler.messages.ErrorMessage
import org.springframework.beans.TypeMismatchException
import org.springframework.context.MessageSource
import org.springframework.web.context.request.WebRequest
import spock.lang.Shared
import spock.lang.Specification

import static cz.jirutka.spring.web.servlet.exhandler.factories.LocalizableErrorMessageFactory.DEFAULT_PREFIX
import static java.util.Locale.ENGLISH
import static java.util.Locale.JAPANESE
import static org.springframework.http.HttpStatus.BAD_REQUEST

class LocalizableErrorMessageFactoryTest extends Specification {

    @Shared exceptionClass = TypeMismatchException

    def messageSource = Mock(MessageSource)
    def interpolator = Mock(MessageInterpolator)
    def request = Mock(WebRequest)

    def factory = Spy(LocalizableErrorMessageFactory, constructorArgs: [exceptionClass, BAD_REQUEST])

    void setup() {
        factory.messageSource = messageSource
        factory.interpolator = interpolator
    }


    def 'create ErrorMessage using resolveMessage'() {
        setup:
            def exception = new TypeMismatchException(1, String)
            def expected = new ErrorMessage(
                    type: new URI('http://httpstatus.es/400'),
                    title: 'Type Mismatch',
                    status: 400,
                    detail: "You're screwed!",
                    instance: new URI('http://example.org/type-mismatch'))
        when:
            def actual = factory.createBody(exception, request)
        then:
            ['type', 'title', 'detail', 'instance'].each { key ->
                1 * factory.resolveMessage(key, exception, request) >> expected.properties[key].toString()
            }
        and:
            actual == expected
    }

    def 'resolveMessage: obtain message using getMessage() and interpolate it'() {
        setup:
            def ex = new TypeMismatchException(1, String)
            def msgTemplate = 'Type mismatch on value: #{value}'
            def msg = 'Type mismatch on value: 1'
            request.getLocale() >> JAPANESE
        when:
            def result = factory.resolveMessage('detail', ex, request)
        then:
            1 * factory.getMessage('detail', JAPANESE) >> msgTemplate
        then:
            1 * interpolator.interpolate(msgTemplate, [ex: ex, req: request]) >> msg
        and:
            result == msg
    }

    def 'getMessage: find message for this exception class'() {
        setup:
            factory.fullyQualifiedNames = fullyQualifiedNames
        and:
            def expected = 'Chunky bacon'
        when:
            def actual = factory.getMessage('title', ENGLISH)
        then:
            1 * messageSource.getMessage("${prefix}.title", null, _, ENGLISH) >> expected
        and:
            actual == expected
        where:
            prefix                    | fullyQualifiedNames
            exceptionClass.simpleName | false
            exceptionClass.name       | true
    }

    def 'getMessage: find default message when no message for the exception class is found'() {
        given:
            def expected = 'Chunky bacon'
        when:
            def actual = factory.getMessage('type', ENGLISH)
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
            factory.getMessage('type', ENGLISH) == ''
    }

}
