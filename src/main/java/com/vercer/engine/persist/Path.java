package com.vercer.engine.persist;

import java.util.AbstractList;
import java.util.Iterator;
import java.util.List;

import com.google.appengine.repackaged.com.google.common.collect.AbstractIterator;
import com.vercer.util.Strings;

public class Path
{
	private final static char Field = '.';
	private final static char ARRAY_START = '[';
	private final static char ARRAY_END = ']';
	private final static char[] SEPERATORS = { Field, ARRAY_START };

	public static final Path EMPTY_PATH = new Path("");

	public static class Builder
	{
		private final StringBuilder builder = new StringBuilder();

		public Builder(String property)
		{
			builder.append(property);
		}

		public Builder(Path base)
		{
			builder.append(base.toString());
		}

		public Path build()
		{
			return new Path(builder.toString());
		}

		public Builder field(String name)
		{
			if (builder.length() > 0)
			{
				builder.append(Field);
			}
			builder.append(name);
			return this;
		}

		public Builder array(int index)
		{
			builder.append(ARRAY_START);
			builder.append(index);
			builder.append(ARRAY_END);
			return this;
		}
	}

	public static class Part
	{
		private final String text;

		private Part(String text)
		{
			this.text = text;
		}

		public boolean isArray()
		{
			return text.charAt(0) == ARRAY_START;
		}

		public boolean isField()
		{
			return text.charAt(0) == Field;
		}

		public boolean isRoot()
		{
			char c = text.charAt(0);
			for (char seperator : SEPERATORS)
			{
				if (c == seperator)
				{
					return false;
				}
			}
			return true;
		}

		public String getFieldName()
		{
			if (isRoot())
			{
				return text;
			}
			else
			{
				return text.substring(1);
			}
		}

		public int getArrayIndex()
		{
			assert isArray();
			return Integer.parseInt(text.substring(1, text.length() - 2));
		}
	}

	private final String value;

	public Path(String value)
	{
		this.value = value;
	}

	public List<Part> getParts()
	{
		return new AbstractList<Part>()
		{
			@Override
			public Part get(int index)
			{
				int begin = Strings.nthIndexOf(value, index, SEPERATORS);
				if (begin > 0)
				{
					int end = Strings.firstIndexOf(value, begin, SEPERATORS);
					if (end > 0)
					{
						return new Part(value.substring(begin, end));
					}
					else
					{
						return new Part(value.substring(begin));
					}
				}
				else
				{
					return null;
				}
			}

			@Override
			public int size()
			{
				int index = 0;
				int count = 0;
				do
				{
					index = Strings.firstIndexOf(value, index + 2, SEPERATORS);
					count++;
				}
				while (index > 0);

				return count;
			}

			@Override
			public Iterator<Part> iterator()
			{
				return new AbstractIterator<Part>()
				{
					private int index;

					@Override
					protected Part computeNext()
					{
						if (index < 0)
						{
							return null;
						}

						int nextIndex = Strings.firstIndexOf(value, index + 1, SEPERATORS);
						String substring;
						if (nextIndex > 0)
						{
							substring = value.substring(index, nextIndex);
						}
						else
						{
							substring = value.substring(index);
						}
						index = nextIndex;
						return new Part(substring);
					}
				};
			}
		};
	}

	public Path tail()
	{
		return new Path(value.substring(value.indexOf(Field) + 1));
	}

	public Part firstPart()
	{
		int index = Strings.firstIndexOf(value, SEPERATORS);
		if (index > 0)
		{
			return new Part(value.substring(0, index));
		}
		else
		{
			return new Part(value);
		}
	}

	public boolean isEmpty()
	{
		return value.length() == 0;
	}

	@Override
	public String toString()
	{
		return value;
	}

	public boolean hasPrefix(Path path)
	{
		return value.startsWith(path.value)
			&& value.length() > path.value.length()
			&& isSeperator(value.charAt(path.value.length()));
	}

	private boolean isSeperator(char c)
	{
		for (char sperator : SEPERATORS)
		{
			if (c == sperator)
			{
				return true;
			}
		}
		return false;
	}

	public Part firstPartAfterPrefix(Path prefix)
	{
		assert hasPrefix(prefix);
		return getParts().get(prefix.getParts().size());
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + ((value == null) ? 0 : value.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
		{
			return true;
		}
		if (obj == null)
		{
			return false;
		}
		if (!(obj instanceof Path))
		{
			return false;
		}
		Path other = (Path) obj;
		if (!value.equals(other.value))
		{
			return false;
		}
		return true;
	}

}
