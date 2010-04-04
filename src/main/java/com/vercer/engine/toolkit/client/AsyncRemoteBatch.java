package com.vercer.engine.toolkit.client;

import com.google.gwt.user.client.rpc.AsyncCallback;

public interface AsyncRemoteBatch
{
	void execute(AsyncCallback<Activated> callback);
}
