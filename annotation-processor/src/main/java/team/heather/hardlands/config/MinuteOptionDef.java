package team.heather.hardlands.config;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({})
@Retention(RetentionPolicy.SOURCE)
public @interface MinuteOptionDef {

    int NO_DEFAULT_VALUE = Integer.MIN_VALUE;

    String name();

    int defaultValue() default NO_DEFAULT_VALUE;
}