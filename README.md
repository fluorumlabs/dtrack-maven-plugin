# dtrack-maven-plugin

[![Maven Central](https://img.shields.io/maven-central/v/com.github.fluorumlabs/dtrack-maven-plugin)](https://repo.maven.apache.org/maven2/com/github/fluorumlabs/dtrack-maven-plugin/)

`dtrack-maven-plugin` is an easy-to-use tool to generate SBOM (software bill of materials) and upload them to [Dependency-Track](https://dependencytrack.org/) SCA.
It is specifically designed for simplified CI/CD integration and can work without any changes to project `pom.xml` files.
One of the key differences from the traditional `cyclonedx-maven-plugin`/`dependency-track-maven-plugin` combination is that `dtrack-maven-plugin` can be used to extract NPM dependencies, specified inside [Vaadin](https://vaadin.com) projects. Note that NPM dependency resolution requires NPM installation.

## Running

Even though this plug-in is not bound by default to any of the life-cycle stages, it requires the projects to be built before running:

`mvn package com.github.fluorumlabs:dtrack-maven-plugin:1.1.2:upload`

Alternatively, you can add plug-in to you `pom.xml`:

```xml
...
<plugin>
    <groupId>com.github.fluorumlabs</groupId>
    <artifactId>dtrack-maven-plugin</artifactId>
    <version>1.1.2</version>
</plugin>
...
```

And use shorthand

`mvn package dtrack:upload`

If you don't need to upload generated SBOM to Dependency-Track, you can use `dtrack:generate` goal, which will create CycloneDX 1.2 files in the `/target` directory.

## Configuration

There are several alternative ways how `dtrack-maven-plugin` can be configured. Different configuration sources can be combined and are processed in the following order:

1. System environment variables
2. `~/.dtrack.yml`
3. Plug-in configuration in `pom.xml`
4. `.dtrack.yml` in the project directory (or in the parent projects)
5. Properties (either in `pom.xml` or passed via command line)

### Configuring using `.dtrack.yml`

`dtrack-maven-plugin` can be configured using `.dtrack.yml` located in the project root (next to `pom.xml`), one if it's parents projects or in the user home directory (`~/.dtrack.yml`).

```yaml
###############################
# Dependency-Track connection #
###############################

# Dependency-Track API URL, for example http://localhost:8081/api
# 
# Also available as dtrack.apiServer property/environment variable
apiServer: http://localhost:8081/api

# Dependency-Track API key. Must have the following permissions: 
# BOM_UPLOAD, PORTFOLIO_MANAGEMENT, PROJECT_CREATION_UPLOAD, VIEW_PORTFOLIO
#
# Also available as dtrack.apiKey property/environment variable
apiKey: secretSECRETsecret

###########################
# SBOM generation options #
###########################

# Custom project name for Dependency-Track. Uses groupId/artifactId format if not specified.
projectName: Ultimate Tool

# CycloneDX component type for project, defaults to library
# Possible values: application/framework/library/container/operating-system/device/firmware/file
projectType: library

# Comma-separated list of maven scopes for dependencies that should be included in SBOM.
# Possible values: compile/provided/runtime/test/system
#
# Also available as dtrack.includedScopes property/environment variable
includedScopes: compile, runtime, system

# Skip projects whose artifactIds contain any of specified comma-separated strings
#
# Also available as dtrack.excludedProjects property/environment variable
excludedProjects: 
  - -test
  - -demo
  - -it

# Skip projects whose artifactIds (or parent projects' artifactIds) contain any of 
# specified comma-separated strings
#
# Also available as dtrack.excludedProjectHierarchies property/environment variable
excludedProjectHierarchies: -tests

# Specifies which previous versions should be kept in Dependency-Track when 
# a new version is uploaded.
# Possible values: NONE/MAJOR/MINOR/PATCH/SUFFIX/BUILD
#
# Also available as dtrack.keepPreviousVersions property/environment variable
keepPreviousVersions: MAJOR

###############################
# Additional NPM dependencies #
###############################

npmDependencies:

  # Specify additional NPM dependency as is
  - packageName: "@vaadin/vaadin"
    version: 14.5.0

  # Extract additional NPM dependencies from annotations
  - annotationClassName: com.vaadin.flow.component.dependency.NpmPackage
    annotationPackageNameField: value
    annotationVersionField: version

  # Extract additional NPM dependencies by calling static method without arguments, 
  # returning Map<String,String>      
  - staticMethodClassName: com.vaadin.flow.server.frontend.NodeUpdater
    staticMethodName: getDefaultDependencies

  - staticMethodClassName: com.vaadin.flow.server.frontend.NodeUpdater
    staticMethodName: getDefaultDevDependencies
```

### Configuring using `pom.xml` plug-in configuration

```xml
...
<plugin>
    <groupId>com.github.fluorumlabs</groupId>
    <artifactId>dtrack-maven-plugin</artifactId>
    <version>1.1.2</version>
    <configuration>
        <settings>
            <!--
            ###############################
            # Dependency-Track connection #
            ###############################
            -->

            <!--
            Dependency-Track API URL, for example http://localhost:8081/api
            -->
            <apiServer>http://localhost:8081/api</apiServer>

            <!--
            Dependency-Track API key. Must have the following permissions:
            BOM_UPLOAD, PORTFOLIO_MANAGEMENT, PROJECT_CREATION_UPLOAD, VIEW_PORTFOLIO
            -->
            <apiKey>secretSECRETsecret</apiKey>

            <!--
            ###########################
            # SBOM generation options #
            ###########################
            -->

            <!--
            Custom project name for Dependency-Track. Uses groupId/artifactId format if not specified.
            -->
            <projectName>${project.groupId}/${project.artifactId}</projectName>

            <!--
            CycloneDX component type for project, defaults to library
            Possible values: application/framework/library/container/operating-system/device/firmware/file
            -->
            <projectType>library</projectType>

            <!--
            Comma-separated list of maven scopes for dependencies that should be included in SBOM.
            Possible values: compile/provided/runtime/test/system. Defaults to compile/runtime/system.
            -->
            <includedScopes>compile,runtime,system</includedScopes>

            <!--
            Skip projects whose artifactIds contain any of specified comma-separated strings
            -->
            <excludedProjects>-test,-demo,-it</excludedProjects>

            <!--
            Skip projects whose artifactIds (or parent projects' artifactIds) contain any of
            specified comma-separated strings
            -->
            <excludedProjectHierarchies>-tests</excludedProjectHierarchies>

            <!--
            Specifies which previous versions should be kept in Dependency-Track when
            a new version is uploaded.
            Possible values: NONE/MAJOR/MINOR/PATCH/SUFFIX/BUILD
            Defaults to BUILD (all previous versions are retained)
            -->
            <keepPreviousVersions>MAJOR</keepPreviousVersions>

            <!--
            ###############################
            # Additional NPM dependencies #
            ###############################
            -->
            <npmDependencies>
                <!-- 
                Specify additional NPM dependency as is
                -->
                <npmDependency>
                    <packageName>@vaadin/vaadin</packageName>
                    <version>14.5.0</version>
                </npmDependency>

                <!--
                Extract additional NPM dependencies from annotations
                -->
                <npmDependency>
                    <annotationClassName>com.vaadin.flow.component.dependency.NpmPackage</annotationClassName>
                    <annotationPackageNameField>value</annotationPackageNameField>
                    <annotationVersionField>version</annotationVersionField>
                </npmDependency>

                <!--
                Extract additional NPM dependencies by calling static method without arguments,
                returning Map<String,String>
                -->
                <npmDependency>
                    <staticMethodClassName>com.vaadin.flow.server.frontend.NodeUpdater</staticMethodClassName>
                    <staticMethodName>getDefaultDependencies</staticMethodName>
                </npmDependency>
            </npmDependencies>
        </settings>
    </configuration>
</plugin>
...
```

### Configuring using properties in `pom.xml`

```xml
...
<properties>
    <!--
    ###############################
    # Dependency-Track connection #
    ###############################
    -->

    <!--
    Dependency-Track API URL, for example http://localhost:8081/api
    -->
    <dtrack.apiServer>http://localhost:8081/api</dtrack.apiServer>

    <!--
    Dependency-Track API key. Must have the following permissions:
    BOM_UPLOAD, PORTFOLIO_MANAGEMENT, PROJECT_CREATION_UPLOAD, VIEW_PORTFOLIO
    -->
    <dtrack.apiKey>secretSECRETsecret</dtrack.apiKey>

    <!--
    ###########################
    # SBOM generation options #
    ###########################
    -->

    <!--
    Custom project name for Dependency-Track. Uses groupId/artifactId format if not specified.
    
    This property can't be set in system environment
    -->
    <dtrack.projectName>${project.groupId}/${project.artifactId}</dtrack.projectName>

    <!--
    CycloneDX component type for project, defaults to library
    Possible values: application/framework/library/container/operating-system/device/firmware/file
    
    This property can't be set in system environment
    -->
    <dtrack.projectType>library</dtrack.projectType>

    <!--
    Comma-separated list of maven scopes for dependencies that should be included in SBOM.
    Possible values: compile/provided/runtime/test/system. Defaults to compile/runtime/system.
    -->
    <dtrack.includedScopes>compile,runtime,system</dtrack.includedScopes>

    <!--
    Skip projects whose artifactIds contain any of specified comma-separated strings
    -->
    <dtrack.excludedProjects>-test,-demo,-it</dtrack.excludedProjects>

    <!--
    Skip projects whose artifactIds (or parent projects' artifactIds) contain any of
    specified comma-separated strings
    -->
    <dtrack.excludedProjectHierarchies>-tests</dtrack.excludedProjectHierarchies>

    <!--
    Specifies which previous versions should be kept in Dependency-Track when
    a new version is uploaded.
    Possible values: NONE/MAJOR/MINOR/PATCH/SUFFIX/BUILD
    Defaults to BUILD (all previous versions are retained)
    -->
    <dtrack.keepPreviousVersions>MAJOR</dtrack.keepPreviousVersions>
</properties>
...
```

Same property names can be also used for system environment variables.

## Skipping projects

In addition to project exclusion using `excludedProjects` and `excludedProjectHierarchies`, it is possible to skip BOM generation/upload with `dtrack.skip` property set to `true`, or file `.dtrackignore` located either in the project directory or in the root directory for one of the parents.
