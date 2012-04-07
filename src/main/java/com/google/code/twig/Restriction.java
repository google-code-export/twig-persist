/**
 * 
 */
package com.google.code.twig;

/**
 * Simple filter interface for restricting sets of elements.
 * 
 * @author John Patterson <john@vercer.com>
 * 
 * @param <T>
 *            the type of element to restrict
 */
public interface Restriction<T>
{
	/**
	 * @param candidate
	 *            A potential element to include
	 * @return true if the candidate element should be included
	 */
	boolean allow(T candidate);
}