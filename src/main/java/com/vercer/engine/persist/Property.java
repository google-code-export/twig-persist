/**
 * 
 */
package com.vercer.engine.persist;


public interface Property
{
	Path getPath();
	Object getValue();
	boolean isIndexed();
}