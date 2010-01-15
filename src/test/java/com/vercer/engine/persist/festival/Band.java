/**
 *
 */
package com.vercer.engine.persist.festival;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.vercer.engine.persist.ReflectiveObject;
import com.vercer.engine.persist.annotation.Component;
import com.vercer.engine.persist.annotation.Entity;
import com.vercer.engine.persist.annotation.Key;
import com.vercer.engine.persist.annotation.Entity.Relationship;

public class Band extends ReflectiveObject
{
	enum HairStyle { LONG_LIKE_A_GIRL, UNKEMPT_FLOPPY, NAVY_SHORT, BALD };

	public Band()
	{
		super(true);
	}

	@Key
	String name;
	Locale locale;

	ArrayList<Musician> members = new ArrayList<Musician>();

	@Entity(Relationship.CHILD)
	List<Album> albums = new ArrayList<Album>();
	Band.HairStyle hair;
}