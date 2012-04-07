package com.google.code.twig.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Cache
{
	boolean automatic() default true;
	int seconds() default -1;
	int memory() default -1;
}
