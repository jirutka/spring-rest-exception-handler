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

import cz.jirutka.spring.web.servlet.exhandler.messages.ValidationErrorMessage
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.context.request.WebRequest
import spock.lang.Specification

import static cz.jirutka.spring.web.servlet.exhandler.test.BindingResultBuilder.createBindingResult

class MethodArgumentNotValidResponseFactoryTest extends Specification {

    def factory = Spy(MethodArgumentNotValidResponseFactory) {
        resolveMessage(*_) >> ''
    }
    def request = Stub(WebRequest)


    def 'create ValidationProblem with validation errors'() {
        given:
            def expected = new ValidationErrorMessage()
                .addError('Houston, we have a problem!')
                .addError('fillet', 666, 'This value is wrong!')
            def error0 = expected.errors[0]
            def error1 = expected.errors[1]
        and:
            def bindingResult = createBindingResult()
                    .addObjectError(error0.message, 'Test')
                    .addFieldError(error1.message, 'Test', error1.field, error1.rejected)
                    .build()
            def exception = new MethodArgumentNotValidException(null, bindingResult)
        when:
            def actual = factory.createBody(exception, request) as ValidationErrorMessage
        then:
            actual.errors == expected.errors
    }
}
