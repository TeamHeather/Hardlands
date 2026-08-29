package team.heather.hardlands.config;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
public @interface ScenarioConfigBuilder {

    OptionDef[] options() default {};

    MinuteOptionDef[] minuteOptions() default {};
}