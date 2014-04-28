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
package cz.jirutka.spring.exhandler.handlers;

import cz.jirutka.spring.exhandler.messages.ErrorMessage;
import cz.jirutka.spring.exhandler.messages.ValidationErrorMessage;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import javax.servlet.http.HttpServletRequest;

import static org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY;

public class MethodArgumentNotValidExceptionHandler extends ErrorMessageRestExceptionHandler<MethodArgumentNotValidException> {


    public MethodArgumentNotValidExceptionHandler() {
        super(UNPROCESSABLE_ENTITY);
    }

    @Override
    public ValidationErrorMessage createBody(MethodArgumentNotValidException ex, HttpServletRequest req) {

        ErrorMessage tmpl = super.createBody(ex, req);
        ValidationErrorMessage msg = new ValidationErrorMessage(tmpl);

        BindingResult result = ex.getBindingResult();

        for (ObjectError err : result.getGlobalErrors()) {
            msg.addError(err.getDefaultMessage());
        }
        for (FieldError err : result.getFieldErrors()) {
            msg.addError(err.getField(), err.getRejectedValue(), err.getDefaultMessage());
        }
        return msg;
    }
}
