package com.google.code.twig.standard;

import java.net.URI;
import java.net.URL;
import java.util.Currency;
import java.util.Locale;
import java.util.logging.Logger;

import com.google.code.twig.PropertyTranslator;
import com.google.code.twig.Settings;
import com.google.code.twig.configuration.Configuration;
import com.google.code.twig.conversion.CoreConverters;
import com.google.code.twig.conversion.EngineConverters;
import com.google.code.twig.conversion.MapConverters;
import com.google.code.twig.translator.ChainedTranslator;
import com.google.code.twig.translator.ConverterTranslator;
import com.google.code.twig.translator.EnumTranslator;
import com.google.code.twig.translator.NativeDirectTranslator;
import com.vercer.convert.ArrayToIterable;
import com.vercer.convert.ChainedTypeConverter;
import com.vercer.convert.CollectionTypeConverter;
import com.vercer.convert.CombinedTypeConverter;
import com.vercer.convert.Converter;
import com.vercer.convert.ConverterRegistry;
import com.vercer.convert.DateConverters;
import com.vercer.convert.IterableToArray;
import com.vercer.convert.IterableToFirstElement;
import com.vercer.convert.NumberConverters;
import com.vercer.convert.ObjectToString;
import com.vercer.convert.StringToPrimitive;
import com.vercer.convert.TypeConverter;

public class StandardObjectDatastore extends TranslatorObjectDatastore
{
	static final Logger log = Logger.getLogger(StandardObjectDatastore.class.getName());

	public StandardObjectDatastore(Settings settings, Configuration strategy, int activation, boolean index)
	{
		super(settings, strategy, activation, index);
	}

	protected ConverterRegistry createStaticConverterRegistry()
	{
		CombinedTypeConverter converter = new CombinedTypeConverter();

		converters(converter, new NumberConverters());
		converters(converter, new EngineConverters());
		converters(converter, new CoreConverters());
		converters(converter, new DateConverters());
		converters(converter, new StringToPrimitive());

		converter.register(new ObjectToString());
		MapConverters.registerAll(converter);

		return converter;
	}

	private void converters(ConverterRegistry combined, Iterable<Converter<?,?>> converters)
	{
		for (Converter<?, ?> converter : converters)
		{
			combined.register(converter);
		}
	}

	// volatile to allow double checked locking
	private volatile static ConverterRegistry registry;
	
	private TypeConverter converter;

	@Override
	public TypeConverter getTypeConverter()
	{
		if (converter == null)
		{
			// TODO problem if more than one subclass - set converters in constructor
			if (registry == null)
			{
				synchronized (this)
				{
					if (registry == null)
					{
						registry = createStaticConverterRegistry();
					}
				}
			}
			
			// these are added for every OD instance
			ChainedTypeConverter chain = new ChainedTypeConverter();
			chain.add(registry);
			chain.add(new CollectionTypeConverter(chain));
			chain.add(new IterableToArray(chain));
			chain.add(new ArrayToIterable(chain));
			chain.add(new IterableToFirstElement());
			
			converters(chain);
			
			converter = chain;
		}
		return converter;
	}
	
	protected void converters(ChainedTypeConverter chain)
	{
	}

	@Override
	protected ChainedTranslator createValueTranslatorChain()
	{
		ChainedTranslator result = new ChainedTranslator();
		
		// types that will be translated to datastore types automatically
		result.append(new NativeDirectTranslator(registry));
		
		// use a converter to convert to a native type
		result.append(new ConverterTranslator(Class.class, String.class, registry));
		result.append(new ConverterTranslator(URL.class, String.class, registry));
		result.append(new ConverterTranslator(URI.class, String.class, registry));
		result.append(new ConverterTranslator(Locale.class, String.class, registry));
		result.append(new ConverterTranslator(Currency.class, String.class, registry));
		
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
