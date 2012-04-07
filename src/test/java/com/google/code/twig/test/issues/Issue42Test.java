package com.google.code.twig.test.issues;

import static com.google.appengine.api.datastore.Query.FilterOperator.EQUAL;
import static com.google.code.twig.test.issues.Issue42Test.Sex.FEMALE;
import static com.google.code.twig.test.issues.Issue42Test.Sex.MALE;
import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import com.google.appengine.api.datastore.QueryResultIterator;
import com.google.code.twig.LocalDatastoreTestCase;
import com.google.code.twig.FindCommand.MergeFindCommand;
import com.google.code.twig.FindCommand.MergeOperator;
import com.google.code.twig.FindCommand.RootFindCommand;
import com.google.code.twig.annotation.AnnotationObjectDatastore;
import com.google.common.collect.Iterators;

public class Issue42Test extends LocalDatastoreTestCase
{
	private AnnotationObjectDatastore ods;

	public enum Sex {
		MALE, FEMALE
	};

	public static class UserEntity
	{
		@SuppressWarnings("unused")
		private UserEntity()
		{
		}
		public UserEntity(String first, String second, Sex sex)
		{
			this.firstName = first;
			this.lastName = second;
			this.gender = sex;
		}

		String firstName;
		String lastName;
		Sex gender;
	}

	@Before
	public void setup()
	{
		ods = new AnnotationObjectDatastore();
	}

	@Test
	public void unsortedMergedQueriesShouldNotReturnDeuplicates()
	{
		ods.storeAll(getUserEntities());
		ods.disassociateAll();
		RootFindCommand<UserEntity> root = ods.find().type(UserEntity.class);
		MergeFindCommand branch = root.merge(MergeOperator.OR);
		branch.addChildCommand().addFilter("firstName", EQUAL, "A");
		branch.addChildCommand().addFilter("lastName", EQUAL, "B");
		branch.addChildCommand().addFilter("gender", EQUAL, MALE.name());

		QueryResultIterator<UserEntity> results = root.now();
		
		assertEquals(5, Iterators.size(results));
	}

	private List<UserEntity> getUserEntities()
	{
		List<UserEntity> list = Arrays.asList(
				new UserEntity("A", "A", MALE), // or2
				new UserEntity("A", "B", FEMALE), // or2
				new UserEntity("A", "C", MALE), // or2
				new UserEntity("B", "A", FEMALE), 
				new UserEntity("B", "B", MALE), // or2
				new UserEntity("B", "C", MALE) // or2
				);
		return list;
	}
}
