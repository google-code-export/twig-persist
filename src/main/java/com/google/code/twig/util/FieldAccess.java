package com.google.code.twig.util;

public interface FieldAccess
{
	void setFieldValue(String name, Object value);
	Object getFieldValue(String name);
}
