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

import groovy.json.JsonSlurper
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.test.context.web.WebAppConfiguration
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext
import spock.lang.Specification

@WebAppConfiguration
abstract class AbstractConfigurationIT extends Specification {

    static final JSON_UTF8 = 'application/json;charset=UTF-8'

    static {
        MockHttpServletResponse.metaClass.getContentAsJson = {
            new JsonSlurper().parseText(delegate.contentAsString)
        }
        MockHttpServletResponse.metaClass.getContentAsXml = {
            new XmlSlurper().parseText(delegate.contentAsString)
        }
    }

    @Autowired WebApplicationContext context

    MockMvc mockMvc
    MockHttpServletResponse response

    static GET = MockMvcRequestBuilders.&get
    static POST = MockMvcRequestBuilders.&post

    void setup() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).build()
    }


    def 'Perform request that results in success response'() {
        when:
            perform GET('/ping').with {
                accept 'text/plain'
            }
        then:
            response.status == 200
    }

    def 'Perform request that causes built-in exception handled by default handler'() {
        when:
            perform GET('/ping').with {
                accept 'application/json'
            }
        then:
            response.status      == 406
            response.contentType == JSON_UTF8
        and:
            with (response.contentAsJson) {
                type   == 'http://httpstatus.es/406'
                title  == 'This sucks!'
                status == 406
                detail == "This resource provides only text/plain, but you've sent request with Accept application/json."
            }
    }

    def 'Perform request that causes user-defined exception with custom exception handler'() {
        when:
            perform GET('/dana')
        then:
            response.status == 404
            response.contentAsString == "There's no Dana, only Zuul!"
    }

    def 'Perform request that causes built-in exception handled by default handler remapped to different status'() {
        when:
            perform POST('/ping')
        then:
            response.status == 418
        and:
            with (response.contentAsXml) {
                title == 'Method Not Allowed'
            }
    }

    def 'Perform request that causes user-defined exception handled by @ExceptionHandler method'() {
        when:
            perform POST('/teapot').with {
                accept 'application/json'
            }
        then:
            response.status      == 418
            response.contentType == JSON_UTF8
            response.contentAsJson.title == 'Bazinga!'
    }


    def perform(builder) {
        response = mockMvc.perform(builder).andReturn().response
    }

    def parseJson(string) {
        new JsonSlurper().parseText(string)
    }
}
