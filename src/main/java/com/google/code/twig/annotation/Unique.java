package com.google.code.twig.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * When stored, begin or create a transaction to check if an entity with the same
 * key already exists. Throw an IllegalStateException if this key is not unique.
 * 
 * @author John Patterson (john@vercer.com)
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface Unique
{
}
