package com.cardiomood.android.air.db.annotations;

import com.cardiomood.android.air.db.tools.ParseValueConverter;
import com.cardiomood.android.air.db.tools.SimpleParseValueConverter;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Created by antondanhsin on 08/10/14.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ParseClass {

    String name() default "";

    Class<? extends ParseValueConverter> valueConverterClass() default SimpleParseValueConverter.class;

}
