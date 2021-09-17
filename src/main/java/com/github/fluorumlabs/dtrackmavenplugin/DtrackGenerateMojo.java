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

import com.github.fluorumlabs.dtrack.ApiException;
import com.github.fluorumlabs.dtrackmavenplugin.engine.BomReactor;
import com.github.fluorumlabs.dtrackmavenplugin.engine.DependencyTree;
import com.github.fluorumlabs.dtrackmavenplugin.engine.NpmReactor;
import com.vdurmont.semver4j.Semver;
import lombok.Getter;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.*;
import org.cyclonedx.model.Component;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Predicate;

/**
 * A goal to generate SBOM (software bill of materials).
 * <p>
 * It is specifically designed for simplified CI/CD integration and can work
 * without any changes to project `pom.xml` files. One of the key differences
 * from the traditional cyclonedx-maven-plugin/dependency-track-maven-plugin
 * combination is that dtrack-maven-plugin can be used to extract NPM
 * dependencies, specified inside Vaadin projects.
 * <p>
 * Note that NPM dependency resolution requires NPM installation.
 */
@Mojo(name = "generate", requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME, requiresDependencyCollection = ResolutionScope.COMPILE_PLUS_RUNTIME)
public class DtrackGenerateMojo extends AbstractMojo {
    private final Map<String, Component> componentMap = new HashMap<>();
    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    @Getter
    private MavenProject project;
    @Parameter(defaultValue = "${project.build.directory}", required = true, readonly = true)
    private File buildDirectory;
    /**
     * Plug-in configuration. dtrack-maven-plugin reads configuration from several
     * sources:
     * <p>
     * 1. System environment variables 2. ~/.dtrack.yml in the user home directory
     * 3. Plug-in configuration in pom.xml 4. .dtrack.yml in the project directory
     * (or in the parent projects) 5. Properties (either in pom.xml or passed via
     * command line)
     * <p>
     * See https://github.com/fluorumlabs/dtrack-maven-plugin/README.md for more
     * details.
     */
    @Parameter
    private PluginConfiguration settings;
    @Getter
    private PluginConfiguration configuration;
    private List<MavenProject> parents;
    @Getter
    private BomReactor bomReactor;

    private static List<String> getStringList(Properties properties, String propertyName, String defaultValue) {
        String scopes = properties.getProperty(propertyName, defaultValue);
        if (scopes != null) {
            return Arrays.asList(scopes.split(","));
        }
        return Collections.emptyList();
    }

    private static void applyConfiguration(PluginConfiguration.PluginConfigurationBuilder builder,
            Properties properties) throws MojoFailureException {
        builder.apiServer(properties.getProperty("dtrack.apiServer"));
        builder.apiKey(properties.getProperty("dtrack.apiKey"));
        builder.includedScopes(
                new ArrayList<>(getStringList(properties, "dtrack.includedScopes", "compile,runtime,system")));
        builder.npmDependencies(new ArrayList<>());
        builder.excludedProjects(new ArrayList<>(getStringList(properties, "dtrack.excludedProjects", null)));
        builder.excludedProjectHierarchies(
                new ArrayList<>(getStringList(properties, "dtrack.excludedProjectHierarchies", null)));

        String value = properties.getProperty("dtrack.keepPreviousVersions");
        if (value != null) {
            boolean success = false;
            for (Semver.VersionDiff versionDiff : Semver.VersionDiff.values()) {
                if (versionDiff.name().equalsIgnoreCase(value)) {
                    builder.keepPreviousVersions(versionDiff);
                    success = true;
                }
            }
            if (!success) {
                throw new MojoFailureException("Unknown value for 'dtrack.keepPreviousVersions': '" + value + "'");
            }
        }
    }

    private static void applyConfigutationFromEnv(PluginConfiguration.PluginConfigurationBuilder builder)
            throws MojoFailureException {
        Properties env = new Properties();
        for (Map.Entry<String, String> stringStringEntry : System.getenv().entrySet()) {
            env.setProperty(stringStringEntry.getKey(), stringStringEntry.getValue());
        }
        applyConfiguration(builder, env);
    }

    private void buildProjectParents() {
        MavenProject currentProject = project;
        List<MavenProject> parentsProjects = new ArrayList<>();
        while (currentProject != null && currentProject.getModel().getPomFile() != null) {
            parentsProjects.add(currentProject);
            currentProject = currentProject.getParent();
        }
        Collections.reverse(parentsProjects);
        this.parents = parentsProjects;

    }

    private void applyConfigurationFromProperties(PluginConfiguration.PluginConfigurationBuilder builder)
            throws MojoFailureException {
        applyConfiguration(builder, project.getProperties());
        builder.projectType(project.getProperties().getProperty("dtrack.projectType", "library"));
        builder.projectName(project.getProperties().getProperty("dtrack.projectName",
                project.getGroupId() + "/" + project.getArtifactId()));
    }

    private void readConfiguration() throws MojoFailureException {
        PluginConfiguration.PluginConfigurationBuilder configurationBuilder = PluginConfiguration.builder();

        // Priority 5 (lowest) - settings defined in ENV
        applyConfigutationFromEnv(configurationBuilder);

        // Priority 4 - overrides in ~/.dtrack.yml
        String homeDirectory = System.getProperty("user.home");
        if (homeDirectory != null) {
            Path dtrackYml = Paths.get(homeDirectory, ".dtrack.yml");
            if (dtrackYml.toFile().exists()) {
                PluginConfiguration.readYaml(dtrackYml).orElseGet(PluginConfiguration::new).withDefaults()
                        .mergeInto(configurationBuilder);
            }
        }

        // Priority 3 - overrides in project pom.xml
        if (settings != null) {
            settings.mergeInto(configurationBuilder);
        }

        // Priority 2 - overrides in current .dtrack.yml, or .dtrack.yml in one of the
        // parents
        for (MavenProject mavenProject : parents) {
            Path dtrackYml = mavenProject.getModel().getPomFile().toPath().getParent().resolve(".dtrack.yml");
            if (dtrackYml.toFile().exists()) {
                PluginConfiguration.readYaml(dtrackYml).orElseGet(PluginConfiguration::new).withDefaults()
                        .mergeInto(configurationBuilder);
            }
        }

        // Priority 1 (highest) - overrides in properties (-Ddtrack.xxxx=yyyy)
        PluginConfiguration.PluginConfigurationBuilder propBuilder = PluginConfiguration.builder();
        applyConfigurationFromProperties(propBuilder);
        propBuilder.build().mergeInto(configurationBuilder);

        configuration = configurationBuilder.build();
    }

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        buildProjectParents();
        readConfiguration();

        if (shouldSkip()) {
            getLog().info("Skipping excluded artifact");
        } else {

            try {
                startBomReactor();
                processDependencies();
                processBom();
            } catch (IOException | ApiException | ProjectBuildingException e) {
                getLog().error("Cannot create or upload BOM to Dependency-Track", e);
            }
        }

    }

    private boolean shouldSkip() {
        // Skip BOMs
        if (project.getPackaging().equalsIgnoreCase("pom")) {
            return true;
        }

        // Skip parent module
        if (!project.getModules().isEmpty()) {
            return true;
        }
        // Skip if property dtrack.skip is set to true
        if (project.getProperties().getProperty("dtrack.skip", "false").equalsIgnoreCase("true")) {
            return true;
        }
        // Skip if current artifactId contains substrings from excludedProjects
        if (configuration.getExcludedProjects().stream().anyMatch(ep -> project.getArtifactId().contains(ep))) {
            return true;
        }
        for (MavenProject mavenProject : parents) {
            // Skip if current project directory (or one of the parents) contains
            // .dtrackignore file
            if (Files.exists(
                    mavenProject.getOriginalModel().getPomFile().toPath().getParent().resolve(".dtrackignore"))) {
                return true;
            }
            // Skip if current artifactId or it's parents contain substrings from
            // excludedProjectHierarchies
            if (configuration.getExcludedProjectHierarchies().stream()
                    .anyMatch(ep -> mavenProject.getArtifactId().contains(ep))) {
                return true;
            }
        }
        return false;
    }

    private void startBomReactor() throws IOException {
        getLog().info("Starting BOM reactor...");
        bomReactor = new BomReactor(this);
        bomReactor.setProjectName(configuration.getProjectName());
        bomReactor.setProjectType(configuration.getProjectType());
        bomReactor.setMainComponent(bomReactor.buildComponent(project));
    }

    protected void processDependencies() throws IOException, ProjectBuildingException {
        MavenProject projectToScan = project;

        getLog().info("Collecting dependencies...");

        DependencyTree dependencyTree = new DependencyTree(projectToScan.getArtifacts(), getArtifactFilter());
        dependencyTree.forEachDependencyPair(projectToScan.getArtifact(), this::processDependency);

        processNpmDependencies(projectToScan);
    }

    private void processNpmDependencies(MavenProject mavenProject) {
        if (!configuration.getNpmDependencies().isEmpty()) {
            NpmReactor npmReactor = new NpmReactor(this, bomReactor);
            URL[] classLoaderURLs = getClassLoaderURLs(mavenProject);

            boolean wasAdded = false;

            for (NpmDependency npmDependency : configuration.getNpmDependencies()) {
                wasAdded |= npmDependency.addToReactor(classLoaderURLs, getLog(), npmReactor);
            }

            if (wasAdded) {
                npmReactor.resolveDependencies();
            }
        }
    }

    private URL[] getClassLoaderURLs(MavenProject project) {
        try {
            List<String> classpathElements = project.getCompileClasspathElements();
            classpathElements.add(project.getBuild().getOutputDirectory());
            URL[] urls = new URL[classpathElements.size()];
            for (int i = 0; i < classpathElements.size(); ++i) {
                urls[i] = new File(classpathElements.get(i)).toURI().toURL();
            }
            return urls;
        } catch (MalformedURLException | DependencyResolutionRequiredException e) {
            getLog().error("Cannot create class loader", e);
            return new URL[0];
        }
    }

    protected void processBom() throws ApiException, MojoFailureException {
        getLog().info("Writing BOM...");
        bomReactor.write(buildDirectory.toPath(), project.getGroupId(), project.getArtifactId(), project.getVersion());
    }

    private Predicate<Artifact> getArtifactFilter() {
        return artifact -> configuration.getIncludedScopes().contains(artifact.getScope()) && !artifact.hasClassifier();
    }

    protected boolean processDependency(Artifact parent, Artifact child) {
        try {
            Component parentComponent = getComponent(parent);
            Component childComponent = getComponent(child);

            bomReactor.addDependency(parentComponent, childComponent);

            return true;
        } catch (IOException e) {
            getLog().error("Cannot describe artifact", e);
        }
        return false;
    }

    protected Component getComponent(Artifact artifact) throws IOException {
        if (componentMap.containsKey(artifact.getId())) {
            return componentMap.get(artifact.getId());
        } else {
            Component component = bomReactor.buildComponent(artifact, "library");
            componentMap.put(artifact.getId(), component);
            return component;
        }
    }

}
