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
package cz.jirutka.spring.exhandler;

import java.util.Map;

final class MapUtils {

    private MapUtils() {}

    /**
     * Puts entries from the {@code source} map into the {@code target} map, but without overriding
     * any existing entry in {@code target} map, i.e. put only if the key does not exist in the
     * {@code target} map.
     *
     * @param target The target map where to put new entries.
     * @param source The source map from which read the entries.
     */
    static <K, V> void putAllIfAbsent(Map<K, V> target, Map<K, V> source) {

        for (Map.Entry<K, V> entry : source.entrySet()) {
            if (!target.containsKey(entry.getKey())) {
                target.put(entry.getKey(), entry.getValue());
            }
        }
    }
}
