package com.vercer.engine.proxy;

import java.io.IOException;
import java.io.ObjectInputStream;

import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.apphosting.api.ApiProxy;

public class RemoteDatastoreServlet extends HttpServlet
{
	private static final long serialVersionUID = 1L;
	
	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException
	{
		ServletInputStream is = req.getInputStream();
		ObjectInputStream ois = new ObjectInputStream(is);
		try
		{
			String arg1 = (String) ois.readObject();
			String arg2 = (String) ois.readObject();
			byte[] arg3 = (byte[]) ois.readObject();
			
			byte[] bs = ApiProxy.makeSyncCall(arg1, arg2, arg3);
		
			resp.getOutputStream().write(bs);
			resp.flushBuffer();
		}
		catch (ClassNotFoundException e)
		{
			throw new IllegalStateException("Could not read datastore request", e);
		}
	}
}
