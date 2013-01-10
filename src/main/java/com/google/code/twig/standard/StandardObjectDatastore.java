package com.google.code.twig.standard;

import java.net.URI;
import java.net.URL;
import java.util.Currency;
import java.util.Date;
import java.util.Locale;
import java.util.Set;
import java.util.logging.Logger;

import com.google.code.twig.ObjectDatastoreFactory;
import com.google.code.twig.PropertyTranslator;
import com.google.code.twig.Settings;
import com.google.code.twig.configuration.Configuration;
import com.google.code.twig.conversion.CoreConverters;
import com.google.code.twig.conversion.EngineConverters;
import com.google.code.twig.translator.ChainedTranslator;
import com.google.code.twig.translator.ConverterTranslator;
import com.google.code.twig.translator.EnumTranslator;
import com.google.code.twig.translator.NativeDirectTranslator;
import com.google.common.primitives.Primitives;
import com.vercer.convert.ArrayToIterable;
import com.vercer.convert.CollectionToArray;
import com.vercer.convert.CollectionTypeConverter;
import com.vercer.convert.CompositeTypeConverter;
import com.vercer.convert.Converter;
import com.vercer.convert.DateConverters;
import com.vercer.convert.NumberConverters;
import com.vercer.convert.ObjectToString;
import com.vercer.convert.StringToPrimitive;
import com.vercer.convert.TypeConverter;

public class StandardObjectDatastore extends TranslatorObjectDatastore
{
	static final Logger log = Logger.getLogger(StandardObjectDatastore.class.getName());

	static
	{
		// polymorphic fields need registered types
		Set<Class<?>> wrapperTypes = Primitives.allWrapperTypes();
		for (Class<?> wrappterType : wrapperTypes)
		{
			ObjectDatastoreFactory.register(wrappterType);
		}
		ObjectDatastoreFactory.register(Date.class);
		ObjectDatastoreFactory.register(String.class);
	}
	
	public StandardObjectDatastore(Settings settings, Configuration strategy, int activation, boolean index)
	{
		super(settings, strategy, activation, index);
	}

	private void converters(CompositeTypeConverter combined, Iterable<Converter<?,?>> converters)
	{
		for (Converter<?, ?> converter : converters)
		{
			combined.register(converter);
		}
	}

	private CompositeTypeConverter converter;

	@Override
	public final TypeConverter getTypeConverter()
	{
		if (converter == null)
		{
			converter = createTypeConverter();
		}
		return converter;
	}

	protected CompositeTypeConverter createTypeConverter()
	{
		CompositeTypeConverter converter = new CompositeTypeConverter();

		converter.register(TypeConverter.DIRECT);
		
		converters(converter, new NumberConverters());
		converters(converter, new EngineConverters());
		
		// can create any type so cannot be a Converter<?, ?>
		converter.register(new EngineConverters.BlobToAnything());
		
		converters(converter, new CoreConverters());
		converters(converter, new DateConverters());
		converters(converter, new StringToPrimitive());

		converter.register(new ObjectToString());
		
		converter.register(new CollectionTypeConverter(converter));
		converter.register(new CollectionToArray(converter));
		converter.register(new ArrayToIterable(converter));
		
		return converter;
//			chain.add(new IterableToFirstElement());`
	}
	
	@Override
	protected ChainedTranslator createValueTranslatorChain()
	{
		ChainedTranslator result = new ChainedTranslator();
		
		// types that will be translated to datastore types automatically
		result.append(new NativeDirectTranslator(converter));
		
		// use a converter to convert to a native type
		result.append(new ConverterTranslator(Class.class, String.class, converter));
		result.append(new ConverterTranslator(URL.class, String.class, converter));
		result.append(new ConverterTranslator(URI.class, String.class, converter));
		result.append(new ConverterTranslator(Locale.class, String.class, converter));
		result.append(new ConverterTranslator(Currency.class, String.class, converter));
		
		// encode enums as their string name
		result.append(new EnumTranslator());
		
		return result;
	}

	@Override
	protected PropertyTranslator getFallbackTranslator()
	{
		return getIndependantTranslator();
	}
}
