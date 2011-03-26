package org.everpeace.util.reflect;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.HashMap;
import java.util.Map;

/**
 * This class provides a functionality which resolves a field raw type of a
 * given field in a given class hierarchy.
 * <p/>
 *
 * @author Shingo Omura <everpeace _at_ gmail _dot_ com>
 *
 */
public class FieldRawTypeResolver {
	// constructor is private.
	private FieldRawTypeResolver() {
		// nop
	}

	/**
	 * resolves a f's raw type in context.
	 *
	 * @param f
	 *            field
	 * @param context
	 *            class hierarchy
	 * @return resolved raw type, null if cannot be resolved.
	 */
	public static Class<?> resolve(Field f, Class<?> context) {
		assert f != null && context != null;
		assert f.getDeclaringClass().isAssignableFrom(context);
		return resolveType(f.getGenericType(), context, null);
	}

	/**
	 * resolve type in context (in case context is class)
	 *
	 * @param type
	 *            type declared on some class or field.
	 * @param context
	 *            type hierarchy
	 * @return resolved raw type, null if cannot resolved
	 */
	@SuppressWarnings("unchecked")
	private static Class<?> resolveType(Type type, Type context,
			Map<TypeVariable<Class<?>>, Class<?>> assignmentsOnSub) {
		assert type != null && context != null;
		// case of Class<?>: no problems.
		if (type instanceof Class<?>)
			return (Class<?>) type;

		// case of GenericArrayType (List<X>[], X[]...)
		if (type instanceof GenericArrayType) {
			GenericArrayType casted = (GenericArrayType) type;
			Class<?> resolvedComponentRawType = resolveType(casted
					.getGenericComponentType(), context, assignmentsOnSub);
			return getArrayType(resolvedComponentRawType);
		}

		// case of ParameterizedType (List<X>, Hoge<X,Y,Z> ...)
		if (type instanceof ParameterizedType) {
			ParameterizedType casted = (ParameterizedType) type;
			return getRawType(casted);
		}

		// case of type variable (X someField;)
		if (type instanceof TypeVariable<?>) {
			TypeVariable<?> typeVar = (TypeVariable<?>) type;
			// X must be declared at Class.
			if (!(typeVar.getGenericDeclaration() instanceof Class<?>)) {
				throw new IllegalArgumentException(
						"can resolve type variables which are "
								+ "declared only Class Declarations:"
								+ typeVar.getGenericDeclaration());
			}

			// Class<?> declaredAt = (Class<?>) typeVar.getGenericDeclaration();
			// if (declaredAt.isAssignableFrom(context)) {
			// throw new IllegalArgumentException("the type variable " + typeVar
			// + "must be declared " +
			// "in a given class hierarchy: " + declaredAt +
			// " is not a superclass of " + context);
			// }
			return resolveTypeVariable((TypeVariable<Class<?>>) typeVar,
					context, assignmentsOnSub);
		}

		// wildcards cannot be a type of a field.
		if (type instanceof WildcardType) {
			throw new IllegalArgumentException(
					"wildcards cannot be a type of a field : " + type);
		}

		// should not be reached.
		throw new IllegalArgumentException("illegal type:" + type);
	}

	/**
	 * resolve type parameter on context type.
	 *
	 * @param typeVar
	 *            a type parameter declared on some class
	 * @param context
	 *            a context type
	 * @param assignmentsToPass
	 *            assignments to type variables from subclasses.
	 * @return -
	 */
	private static Class<?> resolveTypeVariable(TypeVariable<Class<?>> typeVar,
			Type context,
			Map<TypeVariable<Class<?>>, Class<?>> assignmentsToPass) {
		if (context instanceof Class<?>) {
			return resolveTypeVariable(typeVar, (Class<?>) context,
					assignmentsToPass);
		}
		if (context instanceof ParameterizedType) {
			return resolveTypeVariable(typeVar, (ParameterizedType) context,
					assignmentsToPass);
		}
		throw new IllegalArgumentException(
				"context must be Class<?> or ParameterizedType: " + context);
	}

	/**
	 * in case of context is Class.
	 *
	 * @param typeVar
	 *            a type variable declared on some class
	 * @param context
	 *            a class
	 * @param assignmentsOnSub
	 *            assignments to type variables from subclasses.
	 * @return -
	 */
	private static Class<?> resolveTypeVariable(TypeVariable<Class<?>> typeVar,
			Class<?> context,
			Map<TypeVariable<Class<?>>, Class<?>> assignmentsOnSub) {
		// the bottom case. Object.class.getSuperclass() is null.
		if (context == null)
			return null;

		Map<TypeVariable<Class<?>>, Class<?>> assignmentsToPass;
		if (assignmentsOnSub == null) {
			assignmentsToPass = new HashMap<TypeVariable<Class<?>>, Class<?>>();
		} else {
			assignmentsToPass = assignmentsOnSub;
		}

		// support to extended resolution for array class
		if (context.isArray()) {
			Class<?> resolvedRawType = resolveType(typeVar, context.getClass(),
					assignmentsOnSub);
			return getArrayType(resolvedRawType);
		}

		// if typeVar is declared at context, we cannot resolve.
		if (isDeclaredAt(typeVar, context)) {
			return null;
		}

		// proceed on the parent.
		// search target can be limited Class because type parameter is
		// prohibited for interface's fields.
		Type parent = context.getGenericSuperclass();
		return resolveTypeVariable(typeVar, parent, assignmentsToPass);
	}

	/**
	 * in case of ParametrizedType.
	 *
	 * @param typeVar
	 *            a type variable declared on some class.
	 * @param context
	 *            a ParametrizedType
	 * @param assginmentsOnSub
	 *            assignments to type variables from subclasses
	 * @return -
	 */
	@SuppressWarnings("unchecked")
	private static Class<?> resolveTypeVariable(TypeVariable<Class<?>> typeVar,
			ParameterizedType context,
			Map<TypeVariable<Class<?>>, Class<?>> assginmentsOnSub) {
		if (assginmentsOnSub.containsKey(typeVar))
			return assginmentsOnSub.get(typeVar);

		Class<?> contextRawType = getRawType(context);

		TypeVariable<?>[] varsDeclared = contextRawType.getTypeParameters();
		Type[] actualTypes = context.getActualTypeArguments();
		if (varsDeclared == null || actualTypes == null
				|| varsDeclared.length != actualTypes.length) {
			throw new IllegalStateException(
					"the numbers of variables and actual type argument differs or "
							+ "both null..:" + varsDeclared + "," + actualTypes);
		}

		for (int i = 0; i < varsDeclared.length; i++) {
			if (varsDeclared[i] == typeVar) {
				// case: B<T>, A extends B<Integer>
				if (actualTypes[i] instanceof Class<?>) {
					return resolveType((Class<?>) actualTypes[i], context,
							assginmentsOnSub);
				}
				// case:
				// B<T>, A extends B<Double{}>
				// B<T>, A extends B<LIst<Integer>[]>
				// B<T>, A<Y> extends B<Y[][][][][][]>, Z extends A<Integer[]>
				// B<T>, A<Y> extends B<Y[]>, Z extends A<Integer>
				if (actualTypes[i] instanceof GenericArrayType) {
					GenericArrayType casted = (GenericArrayType) actualTypes[i];
					Type componentType = casted.getGenericComponentType();
					return getArrayType(resolveType(componentType, context, assginmentsOnSub));
				}

				// case: B<T>, A extends <List<Integer>>
				if (actualTypes[i] instanceof ParameterizedType) {
					return resolveType((ParameterizedType) actualTypes[i],
							context, assginmentsOnSub);
				}

				// case: B<T>, A<X> extends B<X>
				if (actualTypes[i] instanceof TypeVariable<?>) {
					if (assginmentsOnSub
							.containsKey((TypeVariable<Class<?>>) actualTypes[i])) {
						// X is assigned to T.
						// T can resolved via X.
						return assginmentsOnSub
								.get((TypeVariable<Class<?>>) actualTypes[i]);
					} else {
						// cannot resolved.
						return null;
					}
				}
			} else {
				// case of varDeclared[i] is not typeVar.
				// need to construct assignment map for parents.
				assginmentsOnSub.put((TypeVariable<Class<?>>) varsDeclared[i],
						resolveType(actualTypes[i], context, assginmentsOnSub));
			}
		}

		// proceed on the parent.
		// search target can be limited Class because type parameter is
		// prohibited for interface's fields.
		Type parent = getRawType(context).getGenericSuperclass();
		return resolveTypeVariable(typeVar, parent, assginmentsOnSub);
	}

	/**
	 * exposes a raw type of a given parameterized typpe
	 *
	 * @param type
	 *            a parameterized type.
	 * @return a raw type of input type, null if input type is null..
	 */
	private static Class<?> getRawType(ParameterizedType type) {
		if (type == null)
			return null;
		if (!(type.getRawType() instanceof Class<?>)) {
			throw new IllegalStateException(
					"something wrong... the raw type of a parameterized type must be a Class");
		}

		return (Class<?>) type.getRawType();
	}

	/**
	 * generate Class<T[]> from Class<T>.
	 *
	 * @param componentRawType
	 *            Class<T> object.
	 * @param <T>
	 *            T
	 * @return Class<T[]> object, null if componentRawType is null.
	 */
	@SuppressWarnings("unchecked")
	private static <T> Class<T[]> getArrayType(Class<T> componentRawType) {
		if (componentRawType == null) {
			return null;
		} else {
			return (Class<T[]>) Array.newInstance(componentRawType, 0)
					.getClass();
		}
	}

	/**
	 * return typeVar is declared at context.
	 *
	 * @param typeVar
	 *            a type variable declared at some class.
	 * @param context
	 *            a class
	 * @return -
	 */

	private static boolean isDeclaredAt(TypeVariable<Class<?>> typeVar,
			Class<?> context) {
		if (typeVar == null || context == null)
			return false;
		for (TypeVariable<?> var : context.getTypeParameters()) {
			if (typeVar == var)
				return true;
		}
		return false;
	}
}
