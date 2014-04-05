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
package cz.jirutka.spring.web.servlet.exhandler;

import org.springframework.util.ClassUtils;

abstract class HttpMessageConverterUtils {

    private static final ClassLoader CLASSLOADER = HttpMessageConverterUtils.class.getClassLoader();

    /**
     * Determine whether a JAXB binder is present on the classpath and can be
     * loaded. Will return <tt>false</tt> if either the
     * {@link javax.xml.bind.Binder} or one of its dependencies is not present
     * or cannot be loaded.
     */
    public static boolean isJaxb2Present() {
        return ClassUtils.isPresent("javax.xml.bind.Binder", CLASSLOADER);
    }

    /**
     * Determine whether Jackson 2.x is present on the classpath and can be
     * loaded. Will return <tt>false</tt> if either the
     * {@link com.fasterxml.jackson.databind.ObjectMapper},
     * {@link com.fasterxml.jackson.core.JsonGenerator} or one of its
     * dependencies is not present or cannot be loaded.
     */
    public static boolean isJackson2Present() {
        return ClassUtils.isPresent("com.fasterxml.jackson.databind.ObjectMapper", CLASSLOADER) &&
                ClassUtils.isPresent("com.fasterxml.jackson.core.JsonGenerator", CLASSLOADER);
    }

    /**
     * Determine whether Jackson 1.x is present on the classpath and can be
     * loaded. Will return <tt>false</tt> if either the
     * {@link org.codehaus.jackson.map.ObjectMapper},
     * {@link org.codehaus.jackson.JsonGenerator} or one of its dependencies is
     * not present or cannot be loaded.
     */
    public static boolean isJacksonPresent() {
        return ClassUtils.isPresent("org.codehaus.jackson.map.ObjectMapper", CLASSLOADER) &&
                ClassUtils.isPresent("org.codehaus.jackson.JsonGenerator", CLASSLOADER);
    }
}
