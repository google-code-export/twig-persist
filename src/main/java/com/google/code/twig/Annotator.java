package com.google.code.twig;

import java.lang.annotation.Annotation;

public class Annotator
{
	public Annotation[] annotations(Class<?> model)
	{
		return model.getAnnotations();
	}

	public <A extends Annotation> A annotation(Class<?> model, Class<A> type)
	{
		return model.getAnnotation(type);
	}
}
