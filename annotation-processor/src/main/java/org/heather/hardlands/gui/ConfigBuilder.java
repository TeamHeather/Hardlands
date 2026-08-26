package org.heather.hardlands.gui;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
public @interface ConfigBuilder {

    String identifier() default "";

    Class<?> superclass() default Void.class;

    OptionDef[] options() default {};
}