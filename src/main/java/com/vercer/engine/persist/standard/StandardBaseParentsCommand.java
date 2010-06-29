package com.vercer.engine.persist.standard;

import java.util.Iterator;
import java.util.concurrent.Future;

import com.vercer.engine.persist.FindCommand.ParentsCommand;

public abstract class StandardBaseParentsCommand<P> extends StandardBaseFindCommand<P, ParentsCommand<P>> implements ParentsCommand<P>
{
	protected final StandardTypedFindCommand<?, ?> command;

	public StandardBaseParentsCommand(StandardTypedFindCommand<?, ?> command)
	{
		super(command.datastore);
		this.command = command;
	}

	public Future<Iterator<P>> returnParentsLater()
	{
		// TODO depends on async get being implemented
		throw new UnsupportedOperationException("Not implemented yet");
	}
}