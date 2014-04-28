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
package cz.jirutka.spring.exhandler.messages

import com.fasterxml.jackson.databind.ObjectMapper
import groovy.json.JsonSlurper
import spock.lang.Specification

class ErrorMessageTest extends Specification {

    def jackson = new ObjectMapper()
    def jsonParser = new JsonSlurper()


    def 'convert to JSON using Jackson2 and ignore empty fields'() {
        given:
            def object = new ErrorMessage(
                    type: new URI('http://httpstatus.es/400'),
                    title: 'Type Mismatch',
                    status: 400,
                    detail: '')
        when:
            def result = jackson.writeValueAsString(object)
        then:
            with (jsonParser.parseText(result)) {
                type == object.type.toString()
                title == object.title
                status == object.status
                ! detail
                ! instance
            }
    }
}
