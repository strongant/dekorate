/**
 * Copyright 2018 The original authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.dekorate;

import static io.dekorate.kubernetes.decorator.Decorator.ANY;

import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.dekorate.utils.Strings;

/**
 * A config reference is a way to find configuration values using json path from the generated manifests by Dekorate.
 * This can be handy for some extensions like Helm.
 */
public class ConfigReference {
  private String property;
  private String[] paths;
  private Object value;
  private String expression;
  private String profile;

  public ConfigReference(String property, String path) {
    this(property, new String[] { path });
  }

  public ConfigReference(String property, String[] paths) {
    this(property, paths, null, null, null);
  }

  public ConfigReference(String property, String path, Object value) {
    this(property, new String[] { path }, value, null, null);
  }

  public ConfigReference(String property, String[] paths, Object value, String expression, String profile) {
    this.property = property;
    this.paths = paths;
    this.value = value;
    this.expression = expression;
    this.profile = profile;
  }

  /**
   * @return key name of the config reference to resolve.
   */
  public String getProperty() {
    return property;
  }

  /**
   * @return the expression paths to resolve the property in the generated YAML manifest.
   */
  public String[] getPaths() {
    return paths;
  }

  /**
   * If the value is null, then the framework will check the actual value of the generated JSON manifest.
   *
   * @return value of the config reference.
   */
  public Object getValue() {
    return value;
  }

  /**
   * If not provided, it will use `{{ .Values.<root alias>.<property> }}`.
   *
   * @return The complete Helm expression to be replaced with.
   */
  public String getExpression() {
    return expression;
  }

  /**
   * @return get the profile where the config reference belongs.
   */
  public String getProfile() {
    return profile;
  }

  /**
   * Will generate a config reference name by appending the properties if they are not null or any.
   *
   * For example, if `properties` are [`first`, null, `image`], it will generate: `first.image`.
   */
  public static String joinProperties(String... properties) {
    if (properties == null) {
      return null;
    }

    return Stream.of(properties).filter(p -> !Strings.equals(ANY, p)).collect(Collectors.joining("."));
  }
}
