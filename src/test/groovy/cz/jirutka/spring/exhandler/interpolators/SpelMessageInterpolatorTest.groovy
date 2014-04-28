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
package cz.jirutka.spring.exhandler.interpolators

import org.springframework.context.expression.MapAccessor
import org.springframework.expression.EvaluationContext
import org.springframework.expression.Expression
import org.springframework.expression.ExpressionException
import org.springframework.expression.ExpressionParser
import org.springframework.expression.common.TemplateParserContext
import org.springframework.expression.spel.standard.SpelExpressionParser
import org.springframework.expression.spel.support.ReflectivePropertyAccessor
import org.springframework.expression.spel.support.StandardEvaluationContext
import spock.lang.Specification

class SpelMessageInterpolatorTest extends Specification {

    def evalContext = Mock(EvaluationContext)
    def parser = Mock(ExpressionParser)
    def expression = Mock(Expression)


    def 'create with default evaluation context'() {
        when:
            def mi = new SpelMessageInterpolator()
        then:
            mi.evalContext instanceof StandardEvaluationContext
            mi.evalContext.propertyAccessors*.class.containsAll(MapAccessor, ReflectivePropertyAccessor)
    }

    def 'interpolate message using ExpressionParser'() {
        given:
            def interpolator = new SpelMessageInterpolator(evalContext) {
                ExpressionParser parser() { parser }
            }
            def msgTemplate = 'Allons-y, #{name}!'
            def vars = [name: 'Alonso']
            def interpolated = 'Allons-y, Alonso!'
        when:
            def result = interpolator.interpolate(msgTemplate, vars)
        then:
            1 * parser.parseExpression(msgTemplate, _ as TemplateParserContext) >> expression
            1 * expression.getValue(evalContext, vars, String) >> interpolated
        and:
            result == interpolated
    }

    def 'return empty string when parser or evaluator throws exception'() {
        given:
            def interpolator = new SpelMessageInterpolator(evalContext) {
                ExpressionParser parser() { parser }
            }
            parser.parseExpression(*_) >> { throw new ExpressionException('fail') }
        expect:
            interpolator.interpolate('fail', [:]) == ''
    }

    def 'created parser should be SpelExpressionParser'() {
        given:
            def interpolator = new SpelMessageInterpolator()
        expect:
            interpolator.parser() instanceof SpelExpressionParser
    }
}
