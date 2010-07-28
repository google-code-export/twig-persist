package com.google.code.twig;

import java.util.concurrent.Future;

public interface Terminator<R>
{
	R now();
	Future<R> later();
}