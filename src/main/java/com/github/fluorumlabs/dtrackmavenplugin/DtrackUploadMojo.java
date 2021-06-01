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
import com.github.fluorumlabs.dtrack.Configuration;
import com.github.fluorumlabs.dtrack.api.ProjectApi;
import com.github.fluorumlabs.dtrack.model.Project;
import com.vdurmont.semver4j.Semver;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;

/**
 * A goal to generate SBOM (software bill of materials) and upload them to Dependency-Track SCA.
 * <p>
 * It is specifically designed for simplified CI/CD integration and can work without any
 * changes to project `pom.xml` files. One of the key differences from the traditional
 * cyclonedx-maven-plugin/dependency-track-maven-plugin combination is that dtrack-maven-plugin
 * can be used to extract NPM dependencies, specified inside Vaadin projects.
 * <p>
 * Note that NPM dependency resolution requires NPM installation.
 */
@Mojo(name = "upload",
        requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME,
        requiresDependencyCollection = ResolutionScope.COMPILE_PLUS_RUNTIME)
public class DtrackUploadMojo extends DtrackGenerateMojo {
    private final ProjectApi projectApi = new ProjectApi();

    @Override
    protected void processBom() throws ApiException, MojoFailureException {
        getLog().info("Uploading to Dependency-Track...");
        // Fail fast if API is not defined
        if (getConfiguration().getApiKey() == null || getConfiguration().getApiServer() == null) {
            throw new MojoFailureException("Dependency-Track API server and API key must be defined");
        }

        Configuration.getDefaultApiClient().setApiKey(getConfiguration().getApiKey());
        Configuration.getDefaultApiClient().setBasePath(getConfiguration().getApiServer());

        getBomReactor().upload();
        cleanupPreviousVersions();
    }

    protected void cleanupPreviousVersions() {
        if (getConfiguration().getKeepPreviousVersions() == Semver.VersionDiff.BUILD) {
            return;
        }
        getLog().info("Cleaning up previous versions...");

        Semver newVersion = new Semver(getProject().getVersion(), Semver.SemverType.LOOSE);

        try {
            for (Project otherProject : projectApi.getProjects(getConfiguration().getProjectName(), null)) {
                Semver otherVersion = new Semver(otherProject.getVersion(), Semver.SemverType.LOOSE);

                if (otherVersion.diff(newVersion).compareTo(getConfiguration().getKeepPreviousVersions()) > 0 && otherVersion.isLowerThan(newVersion)) {
                    projectApi.deleteProject(otherProject.getUuid().toString());
                }
            }
        } catch (ApiException e) {
            getLog().warn("Unable to delete previous versions", e);
        }
    }

}
