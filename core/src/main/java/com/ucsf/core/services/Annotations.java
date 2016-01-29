package com.ucsf.core.services;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;

/**
 * List of custom annotations used by the applications. Annotations are used to access methods by
 * key without relying on their full name which can be obfuscated.
 *
 * @author  Julien Jacquemot
 * @version 1.0
 */
public abstract class Annotations {
    /**
     * Returns the method of the given class annotated by the given annotation. If no method is
     * found, returns null.
     */
    public static Method getMethod(Class<?> annotatedClass, String annotation) {
        for (Method method : annotatedClass.getDeclaredMethods()) {
            if (method.isAnnotationPresent(MappedMethod.class)) {
                if (annotation.equals(method.getAnnotation(MappedMethod.class).value()))
                    return method;
            }
        }
        return null;
    }

    /**
     * Defines the annotation to use to be able to retrieve a given method.
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    public @interface MappedMethod {
        String value();
    }
}
