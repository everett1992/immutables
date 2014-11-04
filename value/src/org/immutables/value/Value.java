/*
    Copyright 2014 Ievgen Lukash

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 */
package org.immutables.value;

import com.google.common.annotations.Beta;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This annotation provides namespace for annotations that models generated value objects.
 * Use one of the nested annotation.
 * @see Value.Immutable
 * @see Value.Nested
 */
@Beta
@Retention(RetentionPolicy.SOURCE)
public @interface Value {
  /**
   * Instruct processor to generate immutable implementation of abstract value type.
   * <p>
   * <em>Be warned that such immutable object may contain attributes that are not recursively immutable, thus
   * not every object will be completely immutable. While this may be useful for some workarounds,
   * one should generally avoid creating immutable object with attribute values that could be mutated</em>
   * <p>
   * Generated accessor methods have annotation copied from original accessor method. However
   * {@code org.immutables.*} and {@code java.lang.*} are not copied.
   */
  @Documented
  @Target(ElementType.TYPE)
  @Retention(RetentionPolicy.SOURCE)
  public @interface Immutable {

    /**
     * Generate non-public (i.e. package private) immutable subclass out of public
     * abstract value type.
     */
    @Beta
    boolean nonpublic() default false;

    /**
     * If {@code singleton=true}, generates internal singleton object constructed without any
     * specified parameters. Default is {@literal false}.
     */
    boolean singleton() default false;

    /**
     * If {@code intern=true} then instances will be strong interned on construction.
     * Default is {@literal false}.
     * @see com.google.common.collect.Interners#newStrongInterner()
     */
    boolean intern() default false;

    /**
     * If {@code withers=false} then generation of copying methods starting with
     * "withAttributeName" will be disabled. Default is {@literal true}.
     */
    boolean withers() default true;

    /**
     * If {@code prehash=true} then {@code hashCode} will be precomputed during construction.
     * This could speed up collection lookups for objects with lots of attributes and nested
     * objects.
     * In general, use this when {@code hashCode} computation is expensive and will be used a lot.
     */
    boolean prehash() default false;

    /**
     * If {@code builder=false}, disables generation of {@code builder()}. Default is
     * {@literal true}.
     */
    boolean builder() default true;
  }

  /**
   * This annotation could be applied to top level class which contains nested abstract value types.
   * Immutable implementation classes will be generated as classes nested into special "umbrella"
   * top
   * level class, essentialy named after annotated class with "Immutable" prefix. This could mix
   * with {@link Value.Immutable} annotation, so immutable implementation class will contains
   * nested immutable implementation classes.
   * <p>
   * Implementation classes nested under top level class with "Immutable" prefix
   * <ul>
   * <li>Have simple names without "Immutable" prefix
   * <li>Could be star-imported for easy clutter-free usage.
   * </ul>
   * <p>
   * 
   * <pre>
   * {@literal @}Value.Nested
   * class GraphPrimitives {
   *   {@literal @}Value.Immutable
   *   interace Vertex {}
   *   {@literal @}Value.Immutable
   *   static class Edge {}
   * }
   * ...
   * import ...ImmutableGraphPrimitives.*;
   * ...
   * Edge.builder().build();
   * Vertex.builder().build();
   * </pre>
   */
  @Documented
  @Target(ElementType.TYPE)
  @Retention(RetentionPolicy.SOURCE)
  public @interface Nested {}

  /**
   * Generate transformer for a set of nested classes.
   */
  @Documented
  @Retention(RetentionPolicy.SOURCE)
  @Target(ElementType.TYPE)
  @Beta
  public @interface Transformer {}

  /**
   * This kind of attribute cannot be set during building, but they are eagerly computed from other
   * attributes and stored in field. Should be applied to non-abstract method - attribute value
   * initializer.
   */
  @Documented
  @Retention(RetentionPolicy.SOURCE)
  @Target(ElementType.METHOD)
  public @interface Derived {}

  /**
   * Annotates accessor that should be turned in set-able generated attribute. However, it is
   * non-mandatory to set it via builder. Default value will be assigned to attribute if none
   * supplied, this value will be obtained by calling method annotated this annotation.
   */
  @Documented
  @Retention(RetentionPolicy.SOURCE)
  @Target(ElementType.METHOD)
  public @interface Default {}

  /**
   * Annotate attribute as <em>auxiliary</em> and it will be stored and will be accessible, but will
   * be excluded from generated {@code equals}, {@code hashCode} and {@code toString} methods.
   * {@link Lazy Lazy} attributes are always <em>auxiliary</em>.
   * @see Value.Immutable
   * @see Value.Derived
   * @see Value.Default
   */
  @Documented
  @Retention(RetentionPolicy.SOURCE)
  @Target(ElementType.METHOD)
  public @interface Auxiliary {}

  /**
   * This kind of attribute cannot be set during building, but they are lazily computed from other
   * attributes and stored in non-final field, but initialization is guarded by synchronization with
   * volatile field check. Should be applied to non-abstract method - attribute value initializer.
   */
  @Documented
  @Retention(RetentionPolicy.SOURCE)
  @Target(ElementType.METHOD)
  public @interface Lazy {}

  /**
   * Works with {@link Value.Immutable} classes to mark abstract accessor method be included as
   * "{@code of(..)}" constructor parameter.
   * <p>
   * Following rules applies:
   * <ul>
   * <li>No constructor generated, if none of methods have {@link Value.Parameter} annotation</li>
   * <li>For object to be constructible with a constructor - all non-default and non-derived
   * attributes should be annotated with {@link Value.Parameter}.
   * </ul>
   */
  @Documented
  @Target(ElementType.METHOD)
  @Retention(RetentionPolicy.SOURCE)
  public @interface Parameter {
    /**
     * Used to specify order of constructor argument. It's defaults to zero and allows for
     * non-contiguous order values (arguments are sorted ascending by this order value).
     * <p>
     * <em>This attribute was introduced as JDT annotation processor internally tracks alphabetical order
     * of members (non-standard as of Java 6), this differs from Javac, which uses order of declaration appearance
     * in a source file. Thus, in order to support portable constructor argument definitions,
     * developer should supply argument order explicitly.</em>
     * @return order
     */
    int order() default 0;
  }

  /**
   * Annotates method that should be invoked internally to validate invariants
   * after instance had been created, but before returned to a client.
   * Annotated method must be protected parameter-less method and have a {@code void} return type,
   * which also should not throw a checked exceptions.
   */
  @Documented
  @Retention(RetentionPolicy.SOURCE)
  @Target(ElementType.METHOD)
  public @interface Check {}

  /**
   * Generate bean-style getter accesor method for each attribute.
   * It needed sometimes for interoperability.
   * Methods with 'get' (or 'is') prefixes and attribute name joined in camel-case.
   * <p>
   * Generated accessor methods have annotation copied from original accessor method. However
   * {@code org.immutables.*} and {@code java.lang.*} are not copied. This allow some frameworks to
   * work with immutable types as they can with beans, using getters and annotations on them.
   */
  @Documented
  @Target(ElementType.TYPE)
  @Retention(RetentionPolicy.SOURCE)
  public @interface Getters {}

  /**
   * Naming convention could be used to customize naming convention of the generated immutable
   * implementations and companion classes.
   */
  @Target({ElementType.TYPE, ElementType.PACKAGE, ElementType.ANNOTATION_TYPE})
  @Retention(RetentionPolicy.SOURCE)
  public @interface NamingStyle {
    String[] get() default {};

    /**
     * Builder initialization method. i.e. "setter" in builder.
     * Placeholder.
     * @return naming template
     */
    String init() default "";

    /**
     * Modifiable object "setter" method.
     * @return naming template
     */
    String set() default "set*";

    /**
     * Modify-by-copying "wither" method.
     * @return naming template
     */
    String with() default "with*";

    String hasSet() default "hasSet*";

    String unset() default "unset*";

    String add() default "add*";

    String addAll() default "addAll*";

    /**
     * Copy constructor method name.
     * @return naming template
     */
    String copyOf() default "copyOf";

    /**
     * Constructor method name.
     * @return naming template
     */
    String of() default "of";

    String create() default "create";

    String build() default "build";

    String builder() default "builder";

    String toImmutable() default "toImmutable*";

    String builderType() default "Builder";

    String immutableType() default "Immutable*";

    String modifiableType() default "Modifiable*";
  }

  /**
   * Annotations that applies Java Bean-style naming convention to the generated immutable.
   * It works by being annotated with {@litera @}{@link NamingStyle} annotation which
   * specifies.
   */
  @NamingStyle(get = {"is", "get"}, init = "set")
  @Target({ElementType.TYPE, ElementType.PACKAGE, ElementType.ANNOTATION_TYPE})
  @Retention(RetentionPolicy.SOURCE)
  public @interface BeanStyle {}
}