package com.vercer.engine.proxy;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import com.google.appengine.api.quota.QuotaService;
import com.google.appengine.api.quota.QuotaServiceFactory;
import com.google.apphosting.api.ApiProxy.ApiProxyException;
import com.google.apphosting.api.ApiProxy.Delegate;
import com.google.apphosting.api.ApiProxy.Environment;

public class PerformanceApiProxyDelegate extends BaseApiProxyDelegate
{
	public static class Statistics
	{
		long totalCpuMegaCycles;
		long totalTimeMs;
		int calls;
		int longest;
		int shortest;
	}

	private QuotaService service;
	private final Pattern start;
	private final Pattern end;
	private ThreadLocal<Map<StackTraceElement[], Statistics>> localPathStats = new ThreadLocal<Map<StackTraceElement[], Statistics>>();

	public PerformanceApiProxyDelegate(Delegate<Environment> delegate, Pattern start, Pattern end)
	{
		super(delegate);
		this.start = start;
		this.end = end;
		service = QuotaServiceFactory.getQuotaService();
	}

	@Override
	@SuppressWarnings("unused")
	public byte[] makeSyncCall(Environment arg0, String arg1, String arg2, byte[] arg3)
			throws ApiProxyException
	{
		Exception exception = new Exception();
		exception.fillInStackTrace();
		StackTraceElement[] trace = exception.getStackTrace();

		Statistics stats = getPathStats(trace);

		long startTimeMs = System.currentTimeMillis();
		long startCpuMc = service.getCpuTimeInMegaCycles();
		byte[] result = super.makeSyncCall(arg0, arg1, arg2, arg3);
		long totalTimeMs = System.currentTimeMillis() - startTimeMs;
		long totalCpuMc = service.getCpuTimeInMegaCycles();

		return result;
	}

	private Statistics getPathStats(StackTraceElement[] trace)
	{
		Map<StackTraceElement[], Statistics> pathStats = localPathStats.get();
		if (pathStats == null)
		{
			pathStats = new HashMap<StackTraceElement[], Statistics>();
			localPathStats.set(pathStats);
		}
		return pathStats.get(trace);
	}

	@SuppressWarnings("unused")
	private StackTraceElement[] stackTraceToPath(StackTraceElement[] trace)
	{
		int last, first = 0;
		for (last = 0; last < trace.length; last++)
		{
			StackTraceElement element = trace[last];
			if (first == 0 && start.matcher(element.toString()).find())
			{
				first = last;
			}

			if (first >0 && end.matcher(element.toString()).find())
			{
				break;
			}
		}
		StackTraceElement[] shortened = new StackTraceElement[last - first];
		System.arraycopy(trace, first, shortened, 0, last - first);
		return shortened;
	}
}
