/**
 * 
 */
package com.vercer.engine.persist;

public interface Restriction<T>
{
	boolean allow(T candidate);
}