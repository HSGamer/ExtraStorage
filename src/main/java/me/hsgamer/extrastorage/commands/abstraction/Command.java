package me.hsgamer.extrastorage.commands.abstraction;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(value = RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Command {

    String[] value();

    String usage() default "";

    String permission() default "";

    CommandTarget target() default CommandTarget.BOTH;

    int minArgs() default -1;

}
