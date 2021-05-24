/*
 * Copyright 2021 Artem Godin.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.fluorumlabs.dtrackmavenplugin;

import com.vdurmont.semver4j.Semver;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.maven.plugins.annotations.Parameter;
import org.yaml.snakeyaml.TypeDescription;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PluginConfiguration {
    /**
     * Dependency-Track API URL, for example http://localhost:8081/api
     */
    @Parameter
    private String apiServer;

    /**
     * Dependency-Track API key. Must have the following permissions:
     * BOM_UPLOAD, PORTFOLIO_MANAGEMENT, PROJECT_CREATION_UPLOAD, VIEW_PORTFOLIO
     */
    @Parameter
    private String apiKey;

    /**
     * Custom project name for Dependency-Track. Uses groupId/artifactId format if not specified.
     */
    @Parameter
    private String projectName;

    /**
     * CycloneDX component type for project, defaults to library
     *
     * Possible values: application/framework/library/container/operating-system/device/firmware/file
     */
    @Parameter
    private String projectType;

    /**
     * Specifies which previous versions should be kept in Dependency-Track when
     * a new version is uploaded.
     *
     * Possible values: NONE/MAJOR/MINOR/PATCH/SUFFIX/BUILD
     */
    @Parameter
    private Semver.VersionDiff keepPreviousVersions;

    /**
     * Comma-separated list of maven scopes for dependencies that should be included in SBOM.
     *
     * Possible values: compile/provided/runtime/test/system
     */
    @Parameter
    private List<String> includedScopes;

    /**
     * Skip projects whose artifactIds contain any of specified comma-separated strings.
     */
    @Parameter
    private List<String> excludedProjects;

    /**
     * Skip projects whose artifactIds (or parent projects' artifactIds) contain any of
     * specified comma-separated strings.
     */
    @Parameter
    private List<String> excludedProjectHierarchies;

    /**
     * Additional NPM dependencies
     */
    @Parameter
    private List<NpmDependency> npmDependencies;

    public PluginConfiguration withDefaults() {
        if (includedScopes == null) {
            includedScopes = new ArrayList<>();
        }
        if (excludedProjects == null) {
            excludedProjects = new ArrayList<>();
        }
        if (excludedProjectHierarchies == null) {
            excludedProjectHierarchies = new ArrayList<>();
        }
        if (npmDependencies == null) {
            npmDependencies = new ArrayList<>();
        }
        if (keepPreviousVersions == null) {
            keepPreviousVersions = Semver.VersionDiff.BUILD;
        }
        return this;
    }

    public void mergeInto(PluginConfigurationBuilder builder) {
        if (apiServer != null) {
            builder.apiServer(apiServer);
        }
        if (apiKey != null) {
            builder.apiKey(apiKey);
        }
        if (projectName != null) {
            builder.projectName(projectName);
        }
        if (projectType != null) {
            builder.projectType(projectType);
        }
        if (keepPreviousVersions != null) {
            builder.keepPreviousVersions(keepPreviousVersions);
        }
        if (includedScopes != null) {
            builder.includedScopes = includedScopes;
        }
        if (excludedProjects != null) {
            if (builder.excludedProjects == null) {
                builder.excludedProjects = excludedProjects;
            } else {
                builder.excludedProjects.addAll(excludedProjects);
            }
        }
        if (excludedProjectHierarchies != null) {
            if (builder.excludedProjectHierarchies == null) {
                builder.excludedProjectHierarchies = excludedProjectHierarchies;
            } else {
                builder.excludedProjectHierarchies.addAll(excludedProjectHierarchies);
            }
        }
        if (npmDependencies != null) {
            if (builder.npmDependencies == null) {
                builder.npmDependencies = npmDependencies;
            } else {
                builder.npmDependencies.addAll(npmDependencies);
            }
        }
    }

    public static Optional<PluginConfiguration> readYaml(Path yamlPath) {
        if (!Files.exists(yamlPath)) {
            return Optional.empty();
        }

        Constructor constructor = new Constructor(PluginConfiguration.class);
        TypeDescription descriptor = new TypeDescription(PluginConfiguration.class);
        descriptor.addPropertyParameters("includeScopes", String.class);
        descriptor.addPropertyParameters("excludeProjects", String.class);
        descriptor.addPropertyParameters("excludeProjectHierarchies", String.class);
        descriptor.addPropertyParameters("npmDependencies", NpmDependency.class);
        constructor.addTypeDescription(descriptor);
        Yaml yaml = new Yaml(constructor);

        try {
            return Optional.of(yaml.load(String.join("\n", Files.readAllLines(yamlPath))));
        } catch (IOException ignore) {
            // ignore
            return Optional.empty();
        }
    }
}
