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
package cz.jirutka.spring.exhandler.fixtures

import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

import static org.springframework.http.HttpStatus.I_AM_A_TEAPOT
import static org.springframework.web.bind.annotation.RequestMethod.GET
import static org.springframework.web.bind.annotation.RequestMethod.POST

@RestController
@RequestMapping('/')
class SampleController {

    @RequestMapping(value='/ping', method=GET, produces='text/plain')
    String getPing() {
        'pong!'
    }

    @RequestMapping(value='/dana', method=GET)
    String getDana() {
        throw new ZuulException()
    }

    @RequestMapping(value='/teapot', method=POST)
    void postFail() {
        throw new TeapotException()
    }


    @ResponseStatus(I_AM_A_TEAPOT)
    @ExceptionHandler(TeapotException)
    TeapotMessage handleException() {
        new TeapotMessage(title: 'Bazinga!')
    }


    static class TeapotException extends RuntimeException { }

    static class TeapotMessage {
        String title
    }
}
