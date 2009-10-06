/**
 *
 */
package com.vercer.engine.persist.festival;

import java.util.ArrayList;
import java.util.List;

import com.vercer.engine.persist.annotation.Entity;


public class Festival extends ReflectiveObject
{
	public Festival()
	{
		super(true);
	}

	@Entity
	List<Band> performances = new ArrayList<Band>();
}