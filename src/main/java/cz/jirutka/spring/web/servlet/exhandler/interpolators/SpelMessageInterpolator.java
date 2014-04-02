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
package cz.jirutka.spring.web.servlet.exhandler.interpolators;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.expression.MapAccessor;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionException;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.common.TemplateParserContext;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.util.Assert;

import java.util.Map;

public class SpelMessageInterpolator implements MessageInterpolator {

    private static final Logger LOG = LoggerFactory.getLogger(SpelMessageInterpolator.class);

    private final EvaluationContext evalContext;


    public SpelMessageInterpolator(EvaluationContext evalContext) {
        Assert.notNull(evalContext, "EvaluationContext must not be null");
        this.evalContext = evalContext;
    }

    public SpelMessageInterpolator() {
        StandardEvaluationContext ctx = new StandardEvaluationContext();
        ctx.addPropertyAccessor(new MapAccessor());
        this.evalContext = ctx;
    }


    public String interpolate(String messageTemplate, Map<String, Object> variables) {
        Assert.notNull(messageTemplate, "messageTemplate must not be null");

        try {
            Expression expression = parser().parseExpression(messageTemplate, new TemplateParserContext());

            return expression.getValue(evalContext, variables, String.class);

        } catch (ExpressionException ex) {
            LOG.error("Failed to interpolate message template: {}", messageTemplate, ex);
            return "";
        }
    }

    ExpressionParser parser() {
        return new SpelExpressionParser();
    }
}
