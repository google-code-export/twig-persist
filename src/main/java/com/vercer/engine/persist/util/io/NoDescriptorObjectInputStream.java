package com.vercer.engine.persist.util.io;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectStreamClass;

public class NoDescriptorObjectInputStream extends ObjectInputStream
{
	public NoDescriptorObjectInputStream(InputStream in) throws IOException
	{
		super(in);
	}

	@Override
	protected ObjectStreamClass readClassDescriptor() throws IOException, ClassNotFoundException
	{
		String name = readUTF();
		return ObjectStreamClass.lookup(Class.forName(name));
	}
}