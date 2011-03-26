package org.everpeace.util.reflect;

import static org.junit.Assert.assertTrue;

import java.lang.reflect.Field;
import java.util.List;

import org.junit.Test;

/**
 * test for {@link FieldRawTypeResolver}
 *
 * @author Shingo Omura <everpeace _at_ gmail _dot_ com>
 */
public class FieldRawTypeResolverTest {
	@Test
	public void testWithoutTypeParameters() throws SecurityException,
			NoSuchFieldException {
		NormalSub testee = new NormalSub();
		assertFieldType(fieldOf("stat", NormalSuper.class), testee,
				Integer.class);
		assertFieldType(fieldOf("stat_a", NormalSuper.class), testee,
				Integer[].class);
		assertFieldType(fieldOf("i", NormalSub.class), testee, Integer.class);
		assertFieldType(fieldOf("i_a", NormalSub.class), testee,
				Integer[].class);
		assertFieldType(fieldOf("i_a_a", NormalSub.class), testee,
				Integer[][].class);
		assertFieldType(fieldOf("li", NormalSub.class), testee, List.class);
		assertFieldType(fieldOf("li_a", NormalSub.class), testee, List[].class);
		assertFieldType(fieldOf("li_a_a", NormalSub.class), testee,
				List[][].class);
	}

	@SuppressWarnings("unused")
	private static class NormalSuper {
		public static Integer stat;
		public static Integer[] stat_a;
		public int pi;
		public int[] pi_a;
	}

	@SuppressWarnings("unused")
	private static class NormalSub extends NormalSuper {
		public Integer i;
		public Integer[] i_a;
		public Integer[][] i_a_a;
		public List<Integer> li;
		public List<Integer>[] li_a;
		public List<Integer>[][] li_a_a;

	}

	@Test
	public void testWithTypeParemeters() throws SecurityException,
			NoSuchFieldException {
		TypeVarSub testee = new TypeVarSub();
		assertFieldType(fieldOf("a", TypeVarHyper.class), testee, List.class);
		assertFieldType(fieldOf("b", TypeVarHyper.class), testee,
				List[][][][].class);
		assertFieldType(fieldOf("c", TypeVarSuper.class), testee, Integer.class);
		assertFieldType(fieldOf("c_a", TypeVarSuper.class), testee,
				Integer[].class);
		assertFieldType(fieldOf("c_a_a", TypeVarSuper.class), testee,
				Integer[][].class);
		assertFieldType(fieldOf("ld", TypeVarSuper.class), testee, List.class);
		assertFieldType(fieldOf("ld_a", TypeVarSuper.class), testee,
				List[].class);
		assertFieldType(fieldOf("ld_a_a", TypeVarSuper.class), testee,
				List[][].class);
		assertFieldType(fieldOf("e", TypeVarSuper.class), testee, List.class);
		assertFieldType(fieldOf("f", TypeVarSuper.class), testee, List[].class);
	}

	@SuppressWarnings("unused")
	private static class TypeVarHyper<A, B> {
		public A a;
		public B b;
	}

	@SuppressWarnings("unused")
	private static class TypeVarSuper<C, D, E, F> extends
			TypeVarHyper<List<E>, F[][][]> {
		public C c;
		public C[] c_a;
		public C[][] c_a_a;
		public List<D> ld;
		public List<D>[] ld_a;
		public List<D>[][] ld_a_a;
		public E e;
		public F f;
	}

	private static class TypeVarSub extends
			TypeVarSuper<Integer, Double, List<Integer>, List<Integer>[]> {
	}

	private static <T> void assertFieldType(Field f, T obj, Class<?> expected)
			throws SecurityException, NoSuchFieldException {
		Class<?> resolved = FieldRawTypeResolver.resolve(f, obj.getClass());
		System.out.print("resolved type of field \"" + f.getName() + "\" on "
				+ f.getDeclaringClass().getSimpleName() + " in the context of "
				+ obj.getClass().getSimpleName() + ": ");
		String resolvedName = resolved != null ? resolved.getSimpleName()
				: "null";
		String expectedName = expected != null ? expected.getSimpleName()
				: "null";
		System.out.println("" + resolvedName + "(expected = " + expectedName
				+ ")");
		assertTrue(resolved == expected);
	}

	private static Field fieldOf(String fieldName, Class<?> declaredAt)
			throws SecurityException, NoSuchFieldException {
		return declaredAt.getDeclaredField(fieldName);
	}
}
