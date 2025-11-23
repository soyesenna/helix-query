package com.soyesenna.helixquery.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks an entity (or any type) for which a {Entity}Fields class should be generated.
 * When applied with {@code value=false}, generation is skipped even if the type is annotated with @Entity.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
public @interface GenerateFields {

    /**
     * Whether to generate the Fields class for the annotated type.
     * Default: true.
     */
    boolean value() default true;
}
