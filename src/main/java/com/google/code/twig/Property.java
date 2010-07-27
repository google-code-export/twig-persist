/**
 *
 */
package com.google.code.twig;


public interface Property extends Comparable<Property>
{
	Path getPath();
	Object getValue();
	boolean isIndexed();
}