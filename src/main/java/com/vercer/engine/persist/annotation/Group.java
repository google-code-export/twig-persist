package com.vercer.engine.persist.annotation;


public @interface Group
{
	String value();
	Indexed indexed() default @Indexed(declared=false);
	Embed embed() default @Embed(declared=false);
}
