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
 * 
**/

package io.dekorate.docker.config;

import io.dekorate.ConfigurationRegistry;
import io.dekorate.WithProject;
import io.dekorate.config.DefaultConfiguration;
import io.dekorate.project.ApplyProjectInfo;
import io.dekorate.kubernetes.configurator.ApplyBuildToImageConfiguration;

public class DefaultDockerBuildConfigGenerator implements DockerBuildConfigGenerator, WithProject {

  private final ConfigurationRegistry configurationRegistry;

  public DefaultDockerBuildConfigGenerator(ConfigurationRegistry configurationRegistry) {
    this.configurationRegistry = configurationRegistry;
    this.configurationRegistry.add(new ApplyProjectInfo(getProject()));
    this.configurationRegistry.add(new ApplyBuildToImageConfiguration());
    on(new DefaultConfiguration<DockerBuildConfig>(new DockerBuildConfigBuilder()));
  }

  @Override
  public ConfigurationRegistry getConfigurationRegistry() {
    return this.configurationRegistry;
  }
}
