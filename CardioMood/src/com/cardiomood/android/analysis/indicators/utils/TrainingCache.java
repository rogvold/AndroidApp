
package com.cardiomood.android.analysis.indicators.utils;

import java.util.HashMap;
import java.util.Map;


/**
 * <p></p>
 *
 * Date: 16.03.2008
 *
 * @author <a href="mailto:ktsibriy@gmail.com">Kirill Y. Tsibriy</a>
 */
final class TrainingCache {
    private final Map<Class, Object> cache = new HashMap<Class, Object>();

    TrainingCache() {
    }

    public <T> boolean contains(Evaluation<T> evaluation) {
        return cache.containsKey(evaluation.getClass());
    }

    public <T> void add(Evaluation<T> evaluation, T result) {
        cache.put(evaluation.getClass(), result);
    }

    public <T> T get(Evaluation<T> evaluation) {
        return (T) cache.get(evaluation.getClass());
    }
}