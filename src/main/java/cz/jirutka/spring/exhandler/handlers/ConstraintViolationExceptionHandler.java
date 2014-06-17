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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.convert.ConversionException;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.util.Assert;

import javax.servlet.http.HttpServletRequest;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.validation.ElementKind;
import javax.validation.Path;
import javax.validation.Path.Node;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import static org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY;
import static org.springframework.util.StringUtils.isEmpty;

public class ConstraintViolationExceptionHandler extends ErrorMessageRestExceptionHandler<ConstraintViolationException> {

    private ConversionService conversionService = new DefaultConversionService();


    public ConstraintViolationExceptionHandler() {
        super(UNPROCESSABLE_ENTITY);
    }

    @Override
    public ValidationErrorMessage createBody(ConstraintViolationException ex, HttpServletRequest req) {

        ErrorMessage tmpl = super.createBody(ex, req);
        ValidationErrorMessage msg = new ValidationErrorMessage(tmpl);

        for (ConstraintViolation<?> violation : ex.getConstraintViolations()) {
            Node pathNode = findLastNonEmptyPathNode(violation.getPropertyPath());

            // path is probably useful only for properties (fields)
            if (pathNode != null && pathNode.getKind() == ElementKind.PROPERTY) {
                msg.addError(pathNode.getName(), convertToString(violation.getInvalidValue()), violation.getMessage());

            // type level constraints etc.
            } else {
                msg.addError(violation.getMessage());
            }
        }
        return msg;
    }

    /**
     * Conversion service used for converting an invalid value to String.
     * When no service provided, the {@link DefaultConversionService} is used.
     *
     * @param conversionService must not be null.
     */
    @Autowired(required=false)
    public void setConversionService(ConversionService conversionService) {
        Assert.notNull(conversionService, "conversionService must not be null");
        this.conversionService = conversionService;
    }


    private Node findLastNonEmptyPathNode(Path path) {

        List<Node> list = new ArrayList<>();
        for (Iterator<Node> it = path.iterator(); it.hasNext(); ) {
            list.add(it.next());
        }
        Collections.reverse(list);
        for (Node node : list) {
            if (!isEmpty(node.getName())) {
                return node;
            }
        }
        return null;
    }

    private String convertToString(Object invalidValue) {

        if (invalidValue == null) {
            return null;
        }
        try {
            return conversionService.convert(invalidValue, String.class);

        } catch (ConversionException ex) {
            return invalidValue.toString();
        }
    }

}
