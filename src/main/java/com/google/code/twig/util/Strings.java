package com.google.code.twig.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.StringTokenizer;

public final class Strings
{
	private Strings()
	{
	}


	public static int firstIndexOf(String source, char... chars)
	{
		return firstIndexOf(source, 0, chars);
	}
	
	public static int firstIndexOf(String source, int start, char... chars)
	{
		for (int i = start; i < source.length(); i ++)
		{
			char c = source.charAt(i);
			for (char d : chars)
			{
				if (c == d)
				{
					return i;
				}
			}
		}
		return -1;
	}

	public static int lastIndexOf(String source, char... chars)
	{
		for (int i = source.length() - 1; i >= 0; i--)
		{
			char c = source.charAt(i);
			for (char d : chars)
			{
				if (c == d)
				{
					return i;
				}
			}
		}
		return -1;
	}
	
	public static int nthIndexOf(String source, int n, char... chars)
	{
		int index = -1;
		for (int i = 0; i < n; i++)
		{
			index = firstIndexOf(source, index + 1, chars);
		}
		return index;
	}
	
	public static String[] split(String source, boolean include, char... chars)
	{
		List<String> result = new ArrayList<String>();
		for (int start = 0; start < source.length() && start >= 0; )
		{
			int next = firstIndexOf(source, start, chars);
			if (next > 0)
			{
				result.add(source.substring(start, include ? next + 1 : next));
			}
			else
			{
				result.add(source.substring(start));
				break;
			}
			start = next + 1;
		}
		return result.toArray(new String[result.size()]);
	}
	
	public static String onlyCharacterTypes(String source, int... types)
	{
		StringBuilder builder = new StringBuilder(source.length());
		for (int i = 0 ; i < source.length(); i++)
		{
			char c = source.charAt(i);
			for (int j = 0; j < types.length; j++)
			{
				if (Character.getType(c) == types[j])
				{
					builder.append(c);
					break;
				}
			}
		}
		return builder.toString();
	}

	private static final Collection<String> noCaps = new ArrayList<String>();
	static
	{
		noCaps.add("to");
		noCaps.add("a");
		noCaps.add("an");
		noCaps.add("on");
		noCaps.add("to");
		noCaps.add("be");
		noCaps.add("am");
		noCaps.add("are");
		noCaps.add("were");
		noCaps.add("in");
		noCaps.add("near");
		noCaps.add("the");
		noCaps.add("with");
		noCaps.add("and");
		noCaps.add("or");
	}
	
	
	
	/**
	 * See http://answers.google.com/answers/threadview?id=349913 for rules
	 * @param title
	 * @return
	 */
	public static String toTitleCase(String title)
	{
		StringBuilder builder = new StringBuilder();
		StringTokenizer tokenizer = new StringTokenizer(title, " ", true);
		while (tokenizer.hasMoreTokens())
		{
			String token = tokenizer.nextToken();
			
			if (builder.length() == 0 || // always first word 
					tokenizer.hasMoreTokens() == false || // always last word
					noCaps.contains(token) == false) // try to avoid conjunctions and short prepositions
			{
				builder.append(Character.toUpperCase(token.charAt(0)));
				if (token.length() > 1)
				{
					builder.append(token.substring(1));
				}
			}
			else
			{
				builder.append(token);
			}
		}
		return builder.toString();
	}
}
