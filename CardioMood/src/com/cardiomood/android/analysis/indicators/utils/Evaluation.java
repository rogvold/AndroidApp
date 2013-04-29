package com.cardiomood.android.analysis.indicators.utils;

/**
 * <p></p>
 *
 * Date: 16.03.2008
 *
 * @author <a href="mailto:ktsibriy@gmail.com">Kirill Y. Tsibriy</a>
 */
public interface Evaluation<T> {
    T evaluate(Training training);
}