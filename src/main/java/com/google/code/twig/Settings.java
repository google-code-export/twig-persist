package com.google.code.twig;

import java.util.concurrent.TimeUnit;

import com.google.appengine.api.datastore.ReadPolicy.Consistency;
import com.google.code.twig.LoadCommand.CacheMode;

public final class Settings implements Cloneable
{
	private Long deadline;
	private Consistency consistency;
	private boolean crossGroupTransactions = true;
	private CacheMode cacheMode;
	private int retries = 3;
	
	public static class Builder
	{
		private final Settings settings;
		
		public Builder()
		{
			this.settings = new Settings();
		}
		
		public Builder(Settings settings)
		{
			try
			{
				this.settings = (Settings) settings.clone();
			}
			catch (CloneNotSupportedException e)
			{
				throw new RuntimeException(e);
			}
		}

		public Builder deadline(long value, TimeUnit unit)
		{
			settings.deadline = unit.toMillis(value);
			return this;
		}
		
		public Builder consistency(Consistency consistency)
		{
			settings.consistency = consistency;
			return this;
		}
		
		public Builder cacheMode(CacheMode cacheMode)
		{
			settings.cacheMode = cacheMode;
			return this;
		}

		public Builder retries(int retries)
		{
			settings.retries = retries;
			return this;
		}

		public Builder crossGroupTransactions(boolean xgt)
		{
			settings.crossGroupTransactions = xgt;
			return this;
		}
		
		public Settings build()
		{
			return settings;
		}
	}
	
	public Long getDeadline()
	{
		return this.deadline;
	}
	
	public Consistency getConsistency()
	{
		return this.consistency;
	}
	
	private Settings()
	{
	}

	public static Builder builder()
	{
		return new Builder();
	}
	
	public static Settings defaults()
	{
		return new Settings();
	}
	
	public static Builder copy(Settings settings)
	{
		return new Builder(settings);
	}

	public int getRetries()
	{
		return retries;
	}
	
	public CacheMode getCacheMode()
	{
		return cacheMode;
	}
	
	public boolean isCrossGroupTransactions()
	{
		return crossGroupTransactions;
	}
}
