/**
 *
 */
package com.google.code.twig.test.festival;

class RockBand extends Band
{
	boolean chargedForBrokenTelevisions;

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + (chargedForBrokenTelevisions ? 1231 : 1237);
		return result;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
		{
			return true;
		}
		if (!super.equals(obj))
		{
			return false;
		}
		if (!(obj instanceof RockBand))
		{
			return false;
		}
		RockBand other = (RockBand) obj;
		if (chargedForBrokenTelevisions != other.chargedForBrokenTelevisions)
		{
			return false;
		}
		return true;
	}
}