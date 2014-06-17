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
package cz.jirutka.spring.exhandler.handlers

import cz.jirutka.spring.exhandler.messages.ValidationErrorMessage
import org.hibernate.validator.internal.engine.ConstraintViolationImpl
import org.hibernate.validator.internal.engine.path.PathImpl
import org.springframework.core.convert.ConversionService
import org.springframework.core.convert.ConverterNotFoundException
import org.springframework.mock.web.MockHttpServletRequest
import spock.lang.Specification

import javax.validation.ConstraintViolation
import javax.validation.ConstraintViolationException

import static cz.jirutka.spring.exhandler.handlers.ConstraintViolationExceptionHandlerTest.PathBuilder.propertyPath
import static java.lang.annotation.ElementType.FIELD
import static java.lang.annotation.ElementType.TYPE
import static org.hibernate.validator.internal.engine.path.NodeImpl.*

class ConstraintViolationExceptionHandlerTest extends Specification {

    def handler = Spy(ConstraintViolationExceptionHandler) {
        resolveMessage(*_) >> ''
    }
    def conversionService = Mock(ConversionService)
    def request = new MockHttpServletRequest()

    def service = new DummyService()
    def bean = new DummyBean()


    def 'create Error for violations on fields of simple type'() {
        setup:
            def errorMessage = new ValidationErrorMessage()
                    .addError('number', '42', 'less than 10')
                    .addError('text', null, 'not empty')
        and:
            def violation1 = ConstraintViolationImpl.forBeanValidation(
                    'less than {value}', 'less than 10', bean.class, bean, bean, 42, propertyPath('number'), null, FIELD)
            def violation2 = ConstraintViolationImpl.forBeanValidation(
                    'not empty', 'not empty', bean.class, bean, bean, null, propertyPath('text'), null, FIELD)
        expect:
            assertError([violation1, violation2], errorMessage)
    }

    def 'create Error for violation on field of simple types collection'() {
        setup:
            def errorMessage = new ValidationErrorMessage()
                    .addError('list', 'foo', 'message')
        and:
            def path = new PathBuilder().addPropertyNode('list', 2).addBeanNode().build()
            def violation = ConstraintViolationImpl.forBeanValidation(
                    'message', 'message', bean.class, bean, bean, ['foo'], path, null, FIELD)
        expect:
            assertError violation, errorMessage
    }

    def 'create Error for violation on type level'() {
        setup:
            def errorMessage = new ValidationErrorMessage()
                    .addError('bean invalid')
        and:
            def path = new PathBuilder().build()
            def violation = ConstraintViolationImpl.forBeanValidation(
                    'bean invalid', 'bean invalid', bean.class, bean, bean, bean, path, null, TYPE)
        expect:
            assertError violation, errorMessage
    }

    def 'create Error for method violation on object with field of simple type'() {
        setup:
            def errorMessage = new ValidationErrorMessage()
                    .addError('text', null, 'not null')
        and:
            def path = new PathBuilder()
                    .addMethodNode('myMethod', [bean.class])
                    .addParameterNode('arg0', 0)
                    .addPropertyNode('text')
                    .build()
            def violation = ConstraintViolationImpl.forBeanValidation(
                    'not null', 'not null', service.class, service, bean, null, path, null, FIELD)
        expect:
            assertError violation, errorMessage
    }


    def 'convert invalid value using given Conversion Service'() {
        setup:
            handler.conversionService = conversionService
        and:
            def invalidValue = [42]
            def exception = buildSimpleViolationException(invalidValue)
        when:
            def body = handler.createBody(exception, request) as ValidationErrorMessage
        then:
            1 * conversionService.convert(invalidValue, String) >> '42'
        and:
            body.errors[0].rejected == '42'
    }

    def 'convert invalid value using its toString() when Conversion Service throws exception'() {
        setup:
            handler.conversionService = conversionService
        and:
            def invalidValue = new DummyBean()
            def exception = buildSimpleViolationException(invalidValue)
        when:
            def body = handler.createBody(exception, request) as ValidationErrorMessage
        then:
            1 * conversionService.convert(invalidValue, String) >> { throw new ConverterNotFoundException(null, null) }
        and:
            body.errors[0].rejected == 'dummy'
    }



    void assertError(ConstraintViolation violation, ValidationErrorMessage expected) {
        assertError([violation], expected)
    }

    void assertError(Collection<ConstraintViolation> violations, ValidationErrorMessage expected) {
        def exception = new ConstraintViolationException(violations as Set)
        def actual = handler.createBody(exception, request) as ValidationErrorMessage

        assert actual.errors as Set == expected.errors as Set
    }

    def buildSimpleViolationException(invalidValue) {
        def violation = ConstraintViolationImpl.forBeanValidation(
                'foo', 'bar', bean.class, bean, bean, invalidValue, propertyPath('text'), null, FIELD)

        new ConstraintViolationException([violation] as Set)
    }


    // Fields and methods in these dummy classes aren't actually used, it's
    // just for a reference.
    @SuppressWarnings('GroovyUnusedDeclaration')
    static class DummyService {
        def myMethod(DummyBean arg0) {
        }
    }
    @SuppressWarnings('GroovyUnusedDeclaration')
    static class DummyBean {
        int number = 42
        String text = null
        List<String> list = []

        String toString() { 'dummy' }
    }

    static class PathBuilder {
        private nodes = [ createBeanNode(null) ]

        static propertyPath(String name) {
            new PathBuilder().addPropertyNode(name).build()
        }

        def addMethodNode(String name, List<Class> types) {
            nodes << createMethodNode(name, nodes.last(), types)
            this
        }

        def addParameterNode(String name, int index) {
            nodes << createParameterNode(name, nodes.last(), index)
            this
        }

        def addPropertyNode(String name, int index = -1) {
            def node = createPropertyNode(name, nodes.last())
            if (index >= 0) node = setIndex(node, index)
            nodes << node
            this
        }

        def addBeanNode() {
            nodes << createBeanNode(nodes.last())
            this
        }

        def build() {
            // Don't care that the constructor is private, I'm not gonna
            // commit a harakiri just to create fixtures I need in a "clean way"...
            new PathImpl(nodes)
        }
    }
}
