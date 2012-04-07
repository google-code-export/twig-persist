package com.google.code.twig.translator;

import java.lang.reflect.Type;
import java.util.Set;

import com.google.appengine.api.datastore.Text;
import com.google.code.twig.Path;
import com.google.code.twig.Property;
import com.google.code.twig.PropertyTranslator;
import com.google.code.twig.util.PropertySets;
import com.vercer.convert.TypeConverter;

public class DirectTranslator implements PropertyTranslator
{
	private final TypeConverter converter;

	public DirectTranslator(TypeConverter converter)
	{
		this.converter = converter;
	}
	
	public Object decode(Set<Property> properties, Path path, Type type)
	{
		if (isDirectType(type))
		{
			Object value;
			if (properties.isEmpty())
			{
				return NULL_VALUE;
			}
			else
			{
				value = PropertySets.firstValue(properties);
			}
			
			if (type == String.class && value != null && value.getClass() == Text.class)
			{
				value = ((Text) value).getValue();
			}
			
			return converter.convert(value, type);
		}
		else
		{
			return null;
		}
	}

	protected boolean isDirectType(Type type)
	{
		return true;
	}

	public Set<Property> encode(Object object, Path path, boolean indexed)
	{
		if (object == null || isDirectType(object.getClass()))
		{
			if (object.getClass() == String.class && ((String) object).length() > 500)
			{
				object = new Text((String) object);
			}
			return PropertySets.singletonPropertySet(path, object, indexed);
		}
		else
		{
			return null;
		}
	}
}
