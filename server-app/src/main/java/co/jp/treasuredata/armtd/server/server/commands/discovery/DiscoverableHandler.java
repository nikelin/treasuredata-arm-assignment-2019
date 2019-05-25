package co.jp.treasuredata.armtd.server.server.commands.discovery;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface DiscoverableHandler {
    String[] mappings();

    String description() default "";
}
