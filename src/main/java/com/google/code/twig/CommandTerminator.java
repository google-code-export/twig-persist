package com.google.code.twig;

import java.util.concurrent.Future;

public interface CommandTerminator<R>
{
	R now();
	Future<R> later();
}