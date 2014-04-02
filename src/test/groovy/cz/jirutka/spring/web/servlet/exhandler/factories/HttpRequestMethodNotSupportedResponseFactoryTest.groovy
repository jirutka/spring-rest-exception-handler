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

import org.springframework.web.HttpRequestMethodNotSupportedException
import spock.lang.Specification

import static org.springframework.http.HttpMethod.POST
import static org.springframework.http.HttpMethod.PUT

class HttpRequestMethodNotSupportedResponseFactoryTest extends Specification {

    def factory = new HttpRequestMethodNotSupportedResponseFactory()


    def 'create headers with "Allow" when supported methods are specified'() {
        given:
            def exception = new HttpRequestMethodNotSupportedException('PATCH', ['PUT', 'POST'])
        when:
            def headers = factory.createHeaders(exception, null)
        then:
            headers.getAllow() == [PUT, POST] as Set
    }

    def 'create headers without "Allow" when supported methods are not specified'() {
        given:
            def exception = new HttpRequestMethodNotSupportedException('PATCH')
        when:
            def result = factory.createHeaders(exception, null)
        then:
            ! result.get('Allow')
    }
}
