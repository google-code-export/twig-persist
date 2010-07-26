/**
 *
 */
package com.vercer.engine.persist;


public interface Property extends Comparable<Property>
{
	Path getPath();
	Object getValue();
	boolean isIndexed();
}