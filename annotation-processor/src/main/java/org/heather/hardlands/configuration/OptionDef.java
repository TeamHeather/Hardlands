package org.heather.hardlands.configuration;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({})
@Retention(RetentionPolicy.SOURCE)
public @interface OptionDef {

    String name();

    String key() default "";

    Class<?> type();

    Class<?> elementType() default Void.class;

    Class<?> keyType() default Void.class;

    Class<?> valueType() default Void.class;

    String[] validators() default {};
}