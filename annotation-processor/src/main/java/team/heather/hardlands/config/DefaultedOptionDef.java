package team.heather.hardlands.config;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({})
@Retention(RetentionPolicy.SOURCE)
public @interface DefaultedOptionDef {

    String name();

    String key() default "";

    Class<?> type();

    String[] validators() default {};
}