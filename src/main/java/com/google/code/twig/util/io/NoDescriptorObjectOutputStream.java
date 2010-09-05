package com.google.code.twig.util.io;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamClass;
import java.io.OutputStream;

public class NoDescriptorObjectOutputStream extends ObjectOutputStream
{
	public NoDescriptorObjectOutputStream(OutputStream out) throws IOException
	{
		super(out);
	}

	@Override
	protected void writeClassDescriptor(ObjectStreamClass desc) throws IOException
	{
		writeUTF(desc.getName());
	}
}