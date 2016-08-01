/*
 * Copyright 2014-2016 Jakub Jirutka <jakub@jirutka.cz>.
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
@NoArgsConstructor
@ToString(callSuper=true)
@EqualsAndHashCode(callSuper=true)
@JsonInclude(NON_EMPTY) //for Jackson 2.x
@JsonSerialize(include=Inclusion.NON_EMPTY) //for Jackson 1.x
@XmlRootElement(name="problem") //for JAXB
public class ValidationErrorMessage extends ErrorMessage {

    private static final long serialVersionUID = 1L;

    private List<Error> errors = new ArrayList<>(6);


    public ValidationErrorMessage(ErrorMessage orig) {
        super(orig);
    }

    public ValidationErrorMessage addError(String field, Object rejectedValue, String message) {
        errors.add(new Error(field, rejectedValue, message));
        return this;
    }

    public ValidationErrorMessage addError(String message) {
        errors.add(new Error(null, null, message));
        return this;
    }


    @Data
    @JsonInclude(NON_EMPTY)
    public static class Error {
        private final String field;
        private final Object rejected;
        private final String message;
    }
}
