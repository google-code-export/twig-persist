/**
 *
 */
package com.google.code.twig;


public interface Property
{
	Path getPath();
	Object getValue();
	boolean isIndexed();
}