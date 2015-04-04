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
package cz.jirutka.spring.exhandler.messages;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.codehaus.jackson.map.annotate.JsonSerialize;
import org.codehaus.jackson.map.annotate.JsonSerialize.Inclusion;

import javax.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.List;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_EMPTY;

@Data
@ToString(callSuper = true)
@NoArgsConstructor
@EqualsAndHashCode(callSuper=true)
@JsonInclude(Include.NON_EMPTY) //for Jackson 2.x
@JsonSerialize(include= Inclusion.NON_EMPTY) //for Jackson 1.x
@XmlRootElement(name="problem") //for JAXB
public class ValidationErrorMessage extends ErrorMessage {

    private static final long serialVersionUID = 1L;

    private List<Error> errors = new ArrayList<>(6);


    public ValidationErrorMessage(ErrorMessage orig) {
        super(orig);
    }

    public ValidationErrorMessage addError(String field, Object rejectedValue, String message) {
        Error error = new Error();
        error.field = field;
        error.rejected = rejectedValue;
        error.message = message;

        errors.add(error);
        return this;
    }

    public ValidationErrorMessage addError(String message) {
        Error error = new Error();
        error.message = message;

        errors.add(error);
        return this;
    }


    @Data
    @JsonInclude(NON_EMPTY)
    static class Error {
        private String field;
        private Object rejected;
        private String message;
    }
}
