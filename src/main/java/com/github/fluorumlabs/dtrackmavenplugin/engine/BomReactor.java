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

package com.github.fluorumlabs.dtrackmavenplugin.engine;

import com.github.fluorumlabs.dtrack.ApiException;
import com.github.fluorumlabs.dtrack.api.BomApi;
import com.github.fluorumlabs.dtrack.model.BomSubmitRequest;
import com.github.packageurl.MalformedPackageURLException;
import com.github.packageurl.PackageURL;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.MailingList;
import org.apache.maven.model.Model;
import org.apache.maven.model.Parent;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.cyclonedx.BomGeneratorFactory;
import org.cyclonedx.CycloneDxSchema;
import org.cyclonedx.exception.GeneratorException;
import org.cyclonedx.generators.json.BomJsonGenerator;
import org.cyclonedx.generators.xml.BomXmlGenerator;
import org.cyclonedx.model.*;
import org.cyclonedx.util.BomUtils;
import org.cyclonedx.util.LicenseResolver;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Predicate;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class BomReactor {
    private final BomApi bomApi = new BomApi();
    private final Bom bom = new Bom();
    private final Metadata metadata = new Metadata();
    private final Map<String, List<Dependency>> dependencies = new HashMap<>();
    private final Log log;
    private String projectName;
    private String projectType;

    public BomReactor(AbstractMojo mojo) throws IOException {
        this.log = mojo.getLog();

        Tool tool = new Tool();
        PluginDescriptor pluginDescriptor = (PluginDescriptor) mojo.getPluginContext().get("pluginDescriptor");
        Artifact pluginArtifact = pluginDescriptor.getPluginArtifact();
        tool.setVendor(pluginArtifact.getGroupId());
        tool.setName(pluginArtifact.getArtifactId());
        tool.setVersion(pluginArtifact.getVersion());
        tool.setHashes(BomUtils.calculateHashes(pluginArtifact.getFile(), CycloneDxSchema.Version.VERSION_12));

        bom.setMetadata(metadata);
        bom.setSerialNumber("urn:uuid:" + UUID.randomUUID());
        bom.setComponents(new ArrayList<>());
        metadata.addTool(tool);
    }

    private static String formatComponent(Component component) {
        if (component.getGroup() != null) {
            return component.getGroup() + "/" + component.getName() + "@" + component.getVersion();
        } else {
            return component.getName() + "@" + component.getVersion();
        }
    }

    private static boolean haveNoExternalReference(Component component, ExternalReference.Type type) {
        if (component.getExternalReferences() != null && !component.getExternalReferences().isEmpty()) {
            for (ExternalReference ref : component.getExternalReferences()) {
                if (type == ref.getType()) {
                    return false;
                }
            }
        }
        return true;
    }

    private static LicenseChoice resolveMavenLicenses(List<org.apache.maven.model.License> projectLicenses) {
        LicenseChoice licenseChoice = new LicenseChoice();
        for (org.apache.maven.model.License artifactLicense : projectLicenses) {
            boolean resolved = false;
            if (artifactLicense.getName() != null) {
                LicenseChoice resolvedByName = LicenseResolver.resolve(artifactLicense.getName(), false);
                resolved = resolveLicense(licenseChoice, resolvedByName);
            }
            if (artifactLicense.getUrl() != null && !resolved) {
                LicenseChoice resolvedByUrl = LicenseResolver.resolve(artifactLicense.getUrl(), false);
                resolved = resolveLicense(licenseChoice, resolvedByUrl);
            }
            if (artifactLicense.getName() != null && !resolved) {
                org.cyclonedx.model.License license = new org.cyclonedx.model.License();
                license.setName(artifactLicense.getName().trim());
                if (artifactLicense.getUrl() != null && !artifactLicense.getUrl().trim().isEmpty()) {
                    try {
                        new URL(artifactLicense.getUrl());
                        license.setUrl(artifactLicense.getUrl().trim());
                    } catch (MalformedURLException e) {
                        // throw it away
                    }
                }
                licenseChoice.addLicense(license);
            }
        }
        return licenseChoice;
    }

    private static boolean resolveLicense(LicenseChoice licenseChoice, LicenseChoice resolvedByUrl) {
        if (resolvedByUrl != null) {
            if (resolvedByUrl.getLicenses() != null && !resolvedByUrl.getLicenses().isEmpty()) {
                licenseChoice.addLicense(resolvedByUrl.getLicenses().get(0));
                return true;
            } else if (resolvedByUrl.getExpression() != null) {
                licenseChoice.setExpression(resolvedByUrl.getExpression());
                return true;
            }
        }
        return false;
    }

    private static void addExternalReference(ExternalReference.Type type, String url, Component component) {
        if (url == null) {
            return;
        }
        ExternalReference ref = new ExternalReference();
        ref.setType(type);
        ref.setUrl(url);
        try {
            new URL(ref.getUrl());
            component.addExternalReference(ref);
        } catch (MalformedURLException e) {
            // throw it away
        }
    }

    private static void fillMetaData(MavenProject project, Component component) {
        if (component.getPublisher() == null) {
            // If we don't already have publisher information, retrieve it.
            if (project.getOrganization() != null) {
                component.setPublisher(project.getOrganization().getName());
            }
        }
        if (component.getDescription() == null) {
            // If we don't already have description information, retrieve it.
            component.setDescription(project.getDescription());
        }
        if (component.getLicenseChoice() == null || component.getLicenseChoice().getLicenses() == null || component.getLicenseChoice().getLicenses().isEmpty()) {
            // If we don't already have license information, retrieve it.
            if (project.getLicenses() != null) {
                component.setLicenseChoice(resolveMavenLicenses(project.getLicenses()));
            }
        }
        if (project.getOrganization() != null && project.getOrganization().getUrl() != null) {
            if (haveNoExternalReference(component, ExternalReference.Type.WEBSITE)) {
                addExternalReference(ExternalReference.Type.WEBSITE, project.getOrganization().getUrl(), component);
            }
        }
        if (project.getCiManagement() != null && project.getCiManagement().getUrl() != null) {
            if (haveNoExternalReference(component, ExternalReference.Type.BUILD_SYSTEM)) {
                addExternalReference(ExternalReference.Type.BUILD_SYSTEM, project.getCiManagement().getUrl(),
                        component);
            }
        }
        if (project.getDistributionManagement() != null && project.getDistributionManagement().getDownloadUrl() != null) {
            if (haveNoExternalReference(component, ExternalReference.Type.DISTRIBUTION)) {
                addExternalReference(ExternalReference.Type.DISTRIBUTION,
                        project.getDistributionManagement().getDownloadUrl(), component);
            }
        }
        if (project.getDistributionManagement() != null && project.getDistributionManagement().getRepository() != null) {
            if (haveNoExternalReference(component, ExternalReference.Type.DISTRIBUTION)) {
                addExternalReference(ExternalReference.Type.DISTRIBUTION,
                        project.getDistributionManagement().getRepository().getUrl(), component);
            }
        }
        if (project.getIssueManagement() != null && project.getIssueManagement().getUrl() != null) {
            if (haveNoExternalReference(component, ExternalReference.Type.ISSUE_TRACKER)) {
                addExternalReference(ExternalReference.Type.ISSUE_TRACKER, project.getIssueManagement().getUrl(),
                        component);
            }
        }
        if (project.getMailingLists() != null && !project.getMailingLists().isEmpty()) {
            for (MailingList list : project.getMailingLists()) {
                if (list.getArchive() != null) {
                    if (haveNoExternalReference(component, ExternalReference.Type.MAILING_LIST)) {
                        addExternalReference(ExternalReference.Type.MAILING_LIST, list.getArchive(), component);
                    }
                } else if (list.getSubscribe() != null) {
                    if (haveNoExternalReference(component, ExternalReference.Type.MAILING_LIST)) {
                        addExternalReference(ExternalReference.Type.MAILING_LIST, list.getSubscribe(), component);
                    }
                }
            }
        }
        if (project.getScm() != null && project.getScm().getUrl() != null) {
            if (haveNoExternalReference(component, ExternalReference.Type.VCS)) {
                addExternalReference(ExternalReference.Type.VCS, project.getScm().getUrl(), component);
            }
        }

    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public void setProjectType(String projectType) {
        this.projectType = projectType;
    }

    public void setMainComponent(Component component) {
        if (projectName == null || projectName.isEmpty()) {
            projectName = component.getGroup() + "/" + component.getName();
        }
        if (projectType == null || projectType.isEmpty()) {
            projectType = "library";
        }

        metadata.setComponent(component);
        List<Dependency> depList = new ArrayList<>();
        dependencies.put(component.getBomRef(), depList);

        Dependency dependency = new Dependency(component.getBomRef());
        dependency.setDependencies(depList);

        bom.addDependency(dependency);

    }

    public void mergeBom(Bom other) {
        mergeBom(other, metadata.getComponent(), c -> true);
    }

    private void mergeBom(Bom other, Component parent, Predicate<Component> filter) {
        for (Component component : other.getComponents()) {
            component.setBomRef(component.getPurl());
            if (filter.test(component)) {
                addDependency(parent, component);
            }
        }
    }

    public void addDependency(Component parent, Component child) {
        if (parent == null) {
            parent = bom.getMetadata().getComponent();
        }
        List<Dependency> depList;
        if (dependencies.containsKey(parent.getBomRef())) {
            depList = dependencies.get(parent.getBomRef());
        } else {
            depList = new ArrayList<>();
            Dependency dependency = new Dependency(parent.getBomRef());
            dependency.setDependencies(depList);
            dependencies.put(parent.getBomRef(), depList);
            bom.addDependency(dependency);
        }
        Dependency childDep = new Dependency(child.getBomRef());
        if (depList.stream().noneMatch(dep -> dep.getRef().equals(child.getBomRef()))) {
            depList.add(childDep);
        }

        if (bom.getComponents().stream().noneMatch(cmp -> cmp.getBomRef().equals(child.getBomRef()))) {
            log.info(String.format(": %60s  <-  %s", formatComponent(parent), formatComponent(child)));
            bom.addComponent(child);
        }
    }

    public void upload() throws ApiException {
        BomJsonGenerator bomGenerator = BomGeneratorFactory.createJson(CycloneDxSchema.Version.VERSION_12, bom);
        String bomJson = bomGenerator.toJsonString();

        BomSubmitRequest submitRequest = new BomSubmitRequest();
        submitRequest.projectName(projectName)
                .projectVersion(bom.getMetadata().getComponent().getVersion())
                .autoCreate(true)
                .bom(Base64.getEncoder().encodeToString(bomJson.getBytes(StandardCharsets.UTF_8)));

        bomApi.uploadBom1(submitRequest);
    }

    public void write(Path target, String groupId, String artifactId, String version) throws ApiException {
        String bomFileName = String.join("-", groupId, artifactId, version, "cyclonedx");
        Path jsonTarget = target.resolve(bomFileName + ".json");
        Path xmlTarget = target.resolve(bomFileName + ".xml");
        try {
            Files.deleteIfExists(jsonTarget);
            Files.deleteIfExists(xmlTarget);

            BomJsonGenerator bomGenerator = BomGeneratorFactory.createJson(CycloneDxSchema.Version.VERSION_12, bom);
            Files.write(jsonTarget, bomGenerator.toJsonString().getBytes(StandardCharsets.UTF_8));
            log.info("> "+bomFileName+".json");

            BomXmlGenerator xmlGenerator = BomGeneratorFactory.createXml(CycloneDxSchema.Version.VERSION_12, bom);
            Files.write(xmlTarget, xmlGenerator.toXmlString().getBytes(StandardCharsets.UTF_8));
            log.info("> "+bomFileName+".xml");
        } catch (IOException | GeneratorException e) {
            log.error("Unable to write SBOM to disk", e);
        }
    }

    public Component buildComponent(Artifact artifact, String projectType) throws IOException {
        return buildComponent(artifact, buildProject(artifact), projectType);
    }

    public Component buildComponent(MavenProject project) throws IOException {
        return buildComponent(project.getArtifact(), project, projectType);
    }

    private MavenProject buildProject(Artifact artifact) {
        if (artifact.getType().equalsIgnoreCase("jar")) {
            return extractPom(artifact);
        }
        return null;
    }

    private Component buildComponent(Artifact artifact, MavenProject project, String projectType) throws IOException {
        Component component = new Component();

        component.setGroup(artifact.getGroupId());
        component.setName(artifact.getArtifactId());
        component.setVersion(artifact.getVersion());
        component.setType(resolveProjectType(projectType));
        component.setPurl(generatePackageUrl(artifact));
        component.setBomRef(component.getPurl());
        component.setHashes(BomUtils.calculateHashes(artifact.getFile(), CycloneDxSchema.Version.VERSION_12));

        if (project != null) {
            fillMetaData(artifact, project, component);
        }

        return component;
    }

    private Component.Type resolveProjectType(String projectType) {
        for (Component.Type type : Component.Type.values()) {
            if (type.getTypeName().equalsIgnoreCase(projectType)) {
                return type;
            }
        }
        log.warn("Invalid project type. Defaulting to 'library'");
        log.warn("Valid types are:");
        for (Component.Type type : Component.Type.values()) {
            log.warn("  " + type.getTypeName());
        }
        return Component.Type.LIBRARY;
    }

    private String generatePackageUrl(Artifact artifact) {
        TreeMap<String, String> qualifiers = null;
        if (artifact.getType() != null || artifact.getClassifier() != null) {
            qualifiers = new TreeMap<>();
            if (artifact.getType() != null) {
                qualifiers.put("type", artifact.getType());
            }
            if (artifact.getClassifier() != null) {
                qualifiers.put("classifier", artifact.getClassifier());
            }
        }
        return generatePackageUrl(artifact.getGroupId(), artifact.getArtifactId(), artifact.getVersion(), qualifiers,
                null);
    }

    private String generatePackageUrl(String groupId, String artifactId, String version,
                                      TreeMap<String, String> qualifiers, String subpath) {
        try {
            return new PackageURL(PackageURL.StandardTypes.MAVEN, groupId, artifactId, version, qualifiers, subpath).canonicalize();
        } catch (MalformedPackageURLException e) {
            log.warn("An unexpected issue occurred attempting to create a PackageURL for "
                    + groupId + ":" + artifactId + ":" + version, e);
        }
        return null;
    }

    private MavenProject extractPom(Artifact artifact) {
        if (artifact.getFile() != null && artifact.getFile().isFile()) {
            try (JarFile jarFile = new JarFile(artifact.getFile())) {
                JarEntry entry =
                        jarFile.getJarEntry("META-INF/maven/" + artifact.getGroupId() + "/" + artifact.getArtifactId() + "/pom.xml");
                if (entry != null) {
                    try (InputStream input = jarFile.getInputStream(entry)) {
                        return readPom(input);
                    }
                } else {
                    // Read the pom.xml directly from the filesystem as a fallback
                    Path artifactPath = Paths.get(artifact.getFile().getPath());
                    String pomFilename = artifactPath.getFileName().toString().replace(".jar", ".pom");
                    Path pomPath = artifactPath.resolveSibling(pomFilename);
                    if (Files.exists(pomPath)) {
                        try (InputStream input = Files.newInputStream(pomPath)) {
                            return readPom(input);
                        }
                    }
                }
            } catch (IOException e) {
                log.error("An error occurred attempting to extract POM from artifact", e);
            }
        }
        return null;
    }

    private MavenProject readPom(InputStream input) {
        try {
            MavenXpp3Reader mavenreader = new MavenXpp3Reader();
            try (InputStreamReader reader = new InputStreamReader(input, StandardCharsets.UTF_8)) {
                Model model = mavenreader.read(reader);
                return new MavenProject(model);
            }
        } catch (XmlPullParserException | IOException e) {
            log.error("An error occurred attempting to read POM", e);
        }
        return null;
    }

    private void fillMetaData(Artifact artifact, MavenProject project, Component component) {
        fillMetaData(project, component);
        if (project.getParent() != null) {
            fillMetaData(artifact, project.getParent(), component);
        } else if (project.getModel().getParent() != null) {
            MavenProject parentProject = retrieveParentProject(artifact, project);
            if (parentProject != null) {
                fillMetaData(artifact, parentProject, component);
            }
        }
    }

    private MavenProject retrieveParentProject(Artifact artifact, MavenProject project) {
        if (artifact.getFile() == null || artifact.getFile().getParentFile() == null || !artifact.getType().equalsIgnoreCase("jar")) {
            return null;
        }
        Model model = project.getModel();
        if (model.getParent() != null) {
            Parent parent = model.getParent();
            // Navigate out of version, artifactId, and first (possibly only) level of groupId
            StringBuilder getout = new StringBuilder("../../../");
            int periods = artifact.getGroupId().length() - artifact.getGroupId().replace(".", "").length();
            for (int i = 0; i < periods; i++) {
                getout.append("../");
            }
            File parentFile = new File(artifact.getFile().getParentFile(), getout + parent.getGroupId().replace('.',
                    '/') + "/" + parent.getArtifactId() + "/" + parent.getVersion() + "/" + parent.getArtifactId() +
                    "-" + parent.getVersion() + ".pom");
            if (parentFile.exists() && parentFile.isFile()) {
                try (InputStream inputStream = new FileInputStream(parentFile.getCanonicalFile())) {
                    return readPom(inputStream);
                } catch (Exception e) {
                    log.error("An error occurred retrieving an artifacts parent pom", e);
                }
            }
        }
        return null;
    }

}
