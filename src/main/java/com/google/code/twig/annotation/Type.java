package com.google.code.twig.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
// TODO rename to StoreAs in 3.0
public @interface Type
{
	Class<?> value();
	Class<?>[] parameters() default {};
}
