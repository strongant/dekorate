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
package io.dekorate.kubernetes.decorator;

import java.util.Optional;

import io.dekorate.kubernetes.config.IngressRule;
import io.dekorate.kubernetes.config.Port;
import io.dekorate.utils.Strings;
import io.fabric8.kubernetes.api.builder.TypedVisitor;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.networking.v1.HTTPIngressPathBuilder;
import io.fabric8.kubernetes.api.model.networking.v1.IngressRuleBuilder;
import io.fabric8.kubernetes.api.model.networking.v1.IngressServiceBackendBuilder;
import io.fabric8.kubernetes.api.model.networking.v1.IngressSpecBuilder;
import io.fabric8.kubernetes.api.model.networking.v1.ServiceBackendPort;
import io.fabric8.kubernetes.api.model.networking.v1.ServiceBackendPortBuilder;

public class AddIngressRuleDecorator extends NamedResourceDecorator<IngressSpecBuilder> {

  private static final String DEFAULT_PREFIX = "Prefix";
  private static final String DEFAULT_PATH = "/";

  private final Optional<Port> defaultHostPort;
  private final IngressRule rule;

  public AddIngressRuleDecorator(String name, Optional<Port> defaultHostPort, IngressRule rule) {
    super(name);
    this.defaultHostPort = defaultHostPort;
    this.rule = rule;
  }

  @Override
  public void andThenVisit(IngressSpecBuilder spec, ObjectMeta meta) {
    if (!spec.hasMatchingRule(existingRule -> Strings.equals(rule.getHost(), existingRule.getHost()))) {
      spec.addNewRule()
          .withHost(rule.getHost())
          .withNewHttp()
          .addNewPath()
          .withPathType(pathType())
          .withPath(path())
          .withNewBackend()
          .withNewService()
          .withName(serviceName())
          .withPort(createPort(defaultHostPort))
          .endService()
          .endBackend()
          .endPath()
          .endHttp()
          .endRule();
    } else {
      spec.accept(new HostVisitor(defaultHostPort));
    }
  }

  private String serviceName() {
    return Strings.defaultIfEmpty(rule.getServiceName(), name);
  }

  private String path() {
    return Strings.defaultIfEmpty(rule.getPath(), defaultHostPort.map(p -> p.getPath()).orElse(DEFAULT_PATH));
  }

  private String pathType() {
    return Strings.defaultIfEmpty(rule.getPathType(), DEFAULT_PREFIX);
  }

  private ServiceBackendPort createPort(Optional<Port> defaultHostPort) {
    ServiceBackendPortBuilder builder = new ServiceBackendPortBuilder();
    if (Strings.isNotNullOrEmpty(rule.getServicePortName())) {
      builder.withName(rule.getServicePortName());
    } else if (rule.getServicePortNumber() != null && rule.getServicePortNumber() >= 0) {
      builder.withNumber(rule.getServicePortNumber());
    } else if (Strings.isNullOrEmpty(rule.getServiceName()) || Strings.equals(rule.getServiceName(), name)) {
      // Trying to get the port from the service
      Port servicePort = defaultHostPort
          .orElseThrow(() -> new RuntimeException("Could not find any matching port to configure the Ingress Rule. Specify the "
              + "service port using `kubernetes.ingress.service-port-name`"));
      builder.withName(servicePort.getName());
    } else {
      throw new RuntimeException("The service port for '" + rule.getServiceName() + "' was not set. Specify one "
          + "using `kubernetes.ingress.service-port-name`");
    }

    return builder.build();
  }

  private class HostVisitor extends TypedVisitor<IngressRuleBuilder> {

    private final Optional<Port> defaultHostPort;

    public HostVisitor(Optional<Port> defaultHostPort) {
      this.defaultHostPort = defaultHostPort;
    }

    @Override
    public void visit(IngressRuleBuilder existingRule) {
      if (Strings.equals(existingRule.getHost(), rule.getHost())) {
        if (!existingRule.hasHttp()) {
          existingRule.withNewHttp()
              .addNewPath()
              .withPathType(pathType())
              .withPath(path())
              .withNewBackend()
              .withNewService()
              .withName(serviceName())
              .withPort(createPort(defaultHostPort))
              .endService()
              .endBackend()
              .endPath().endHttp();
        } else if (existingRule.getHttp().getPaths().stream()
            .noneMatch(p -> Strings.equals(p.getPath(), path()) && Strings.equals(p.getPathType(), pathType()))) {
          existingRule.editHttp()
              .addNewPath()
              .withPathType(pathType())
              .withPath(path())
              .withNewBackend()
              .withNewService()
              .withName(serviceName())
              .withPort(createPort(defaultHostPort))
              .endService()
              .endBackend()
              .endPath().endHttp();
        } else {
          existingRule.accept(new PathVisitor(defaultHostPort));
        }
      }
    }
  }

  private class PathVisitor extends TypedVisitor<HTTPIngressPathBuilder> {

    private final Optional<Port> defaultHostPort;

    public PathVisitor(Optional<Port> defaultHostPort) {
      this.defaultHostPort = defaultHostPort;
    }

    @Override
    public void visit(HTTPIngressPathBuilder existingPath) {
      if (Strings.equals(existingPath.getPath(), rule.getPath())) {
        if (!existingPath.hasBackend()) {
          existingPath.withNewBackend()
              .withNewService()
              .withName(serviceName())
              .withPort(createPort(defaultHostPort))
              .endService()
              .endBackend();
        } else {
          existingPath.accept(new ServiceVisitor(defaultHostPort));
        }
      }
    }
  }

  private class ServiceVisitor extends TypedVisitor<IngressServiceBackendBuilder> {

    private final Optional<Port> defaultHostPort;

    public ServiceVisitor(Optional<Port> defaultHostPort) {
      this.defaultHostPort = defaultHostPort;
    }

    @Override
    public void visit(IngressServiceBackendBuilder service) {
      service.withName(Strings.defaultIfEmpty(rule.getServiceName(), name)).withPort(createPort(defaultHostPort));
    }
  }
}
