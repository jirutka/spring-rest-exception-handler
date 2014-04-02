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

import cz.jirutka.spring.web.servlet.exhandler.messages.ErrorMessage
import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseEntity
import org.springframework.web.context.request.WebRequest
import spock.lang.Specification

import static org.springframework.http.HttpStatus.BAD_REQUEST

class AbstractErrorMessageFactoryTest extends Specification {

    def 'determine exception class from generic type'() {
        given:
            def factory = new AbstractErrorMessageFactory<IOException>(BAD_REQUEST) {
                ErrorMessage createErrorMessage(IOException ex, WebRequest req) { null}
            }
        expect:
            factory.exceptionClass == IOException
    }

    def 'create error response using createHeaders, createErrorMessage and getStatus methods'() {
        setup:
            def ex = new IOException()
            def req = Mock(WebRequest)
            def expected = new ResponseEntity(new ErrorMessage(), new HttpHeaders(date: 123), BAD_REQUEST)
        and:
            def factory = Spy(AbstractErrorMessageFactory, constructorArgs: [IOException, expected.statusCode]) {
                createHeaders(ex, req) >> expected.headers
                createErrorMessage(ex, req) >> expected.body
            }
        when:
            def actual = factory.createErrorResponse(ex, req)
        then:
            actual == expected
    }
}
