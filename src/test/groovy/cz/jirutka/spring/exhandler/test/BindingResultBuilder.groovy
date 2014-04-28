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
package cz.jirutka.spring.exhandler.test

import org.springframework.validation.BeanPropertyBindingResult
import org.springframework.validation.BindingResult
import org.springframework.validation.FieldError
import org.springframework.validation.ObjectError

class BindingResultBuilder {

    final BindingResult bindingResult


    private BindingResultBuilder(Object target, String objectName) {
        this.bindingResult = new BeanPropertyBindingResult(target, objectName)
    }

    static BindingResultBuilder createBindingResult(Object target = null, String objectName = 'Test') {
        new BindingResultBuilder(target, objectName)
    }


    BindingResultBuilder addFieldError(
            String defaultMessage, String objectName, String field, Object rejectedValue = null,
            boolean bindingFailure = false, String[] codes = [], Object[] arguments = []) {

        bindingResult.addError(
                new FieldError(objectName, field, rejectedValue, bindingFailure, codes, arguments, defaultMessage))
        this
    }

    BindingResultBuilder addObjectError(
            String defaultMessage, String objectName, String[] codes = [], Object[] arguments = []) {

        bindingResult.addError(new ObjectError(objectName, codes, arguments, defaultMessage))
        this
    }

    BindingResult build() {
        bindingResult
    }
}
