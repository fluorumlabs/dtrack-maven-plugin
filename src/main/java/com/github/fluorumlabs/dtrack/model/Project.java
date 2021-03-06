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

/*
 * Dependency-Track API
 * No description provided (generated by Swagger Codegen https://github.com/swagger-api/swagger-codegen)
 *
 * OpenAPI spec version: 4.2.1
 * 
 *
 * NOTE: This class is auto generated by the swagger code generator program.
 * https://github.com/swagger-api/swagger-codegen.git
 * Do not edit the class manually.
 */


package com.github.fluorumlabs.dtrack.model;

import java.util.Objects;
import java.util.Arrays;
import com.github.fluorumlabs.dtrack.model.PackageURL;
import com.github.fluorumlabs.dtrack.model.Project;
import com.github.fluorumlabs.dtrack.model.ProjectMetrics;
import com.github.fluorumlabs.dtrack.model.ProjectProperty;
import com.github.fluorumlabs.dtrack.model.Tag;
import com.google.gson.TypeAdapter;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.annotations.SerializedName;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Project
 */
@javax.annotation.Generated(value = "io.swagger.codegen.languages.JavaClientCodegen", date = "2021-05-21T09:42:21.596Z")
public class Project {
  @SerializedName("author")
  private String author = null;

  @SerializedName("publisher")
  private String publisher = null;

  @SerializedName("group")
  private String group = null;

  @SerializedName("name")
  private String name = null;

  @SerializedName("description")
  private String description = null;

  @SerializedName("version")
  private String version = null;

  /**
   * Gets or Sets classifier
   */
  @JsonAdapter(ClassifierEnum.Adapter.class)
  public enum ClassifierEnum {
    APPLICATION("APPLICATION"),
    
    FRAMEWORK("FRAMEWORK"),
    
    LIBRARY("LIBRARY"),
    
    CONTAINER("CONTAINER"),
    
    OPERATING_SYSTEM("OPERATING_SYSTEM"),
    
    DEVICE("DEVICE"),
    
    FIRMWARE("FIRMWARE"),
    
    FILE("FILE");

    private String value;

    ClassifierEnum(String value) {
      this.value = value;
    }

    public String getValue() {
      return value;
    }

    @Override
    public String toString() {
      return String.valueOf(value);
    }

    public static ClassifierEnum fromValue(String text) {
      for (ClassifierEnum b : ClassifierEnum.values()) {
        if (String.valueOf(b.value).equals(text)) {
          return b;
        }
      }
      return null;
    }

    public static class Adapter extends TypeAdapter<ClassifierEnum> {
      @Override
      public void write(final JsonWriter jsonWriter, final ClassifierEnum enumeration) throws IOException {
        jsonWriter.value(enumeration.getValue());
      }

      @Override
      public ClassifierEnum read(final JsonReader jsonReader) throws IOException {
        String value = jsonReader.nextString();
        return ClassifierEnum.fromValue(String.valueOf(value));
      }
    }
  }

  @SerializedName("classifier")
  private ClassifierEnum classifier = null;

  @SerializedName("cpe")
  private String cpe = null;

  @SerializedName("purl")
  private PackageURL purl = null;

  @SerializedName("swidTagId")
  private String swidTagId = null;

  @SerializedName("directDependencies")
  private String directDependencies = null;

  @SerializedName("uuid")
  private UUID uuid = null;

  @SerializedName("parent")
  private Project parent = null;

  @SerializedName("children")
  private List<Project> children = null;

  @SerializedName("properties")
  private List<ProjectProperty> properties = null;

  @SerializedName("tags")
  private List<Tag> tags = null;

  @SerializedName("lastBomImport")
  private Double lastBomImport = null;

  @SerializedName("lastBomImportFormat")
  private String lastBomImportFormat = null;

  @SerializedName("lastInheritedRiskScore")
  private Double lastInheritedRiskScore = null;

  @SerializedName("active")
  private Boolean active = null;

  @SerializedName("metrics")
  private ProjectMetrics metrics = null;

  public Project author(String author) {
    this.author = author;
    return this;
  }

   /**
   * Get author
   * @return author
  **/
  @ApiModelProperty(value = "")
  public String getAuthor() {
    return author;
  }

  public void setAuthor(String author) {
    this.author = author;
  }

  public Project publisher(String publisher) {
    this.publisher = publisher;
    return this;
  }

   /**
   * Get publisher
   * @return publisher
  **/
  @ApiModelProperty(value = "")
  public String getPublisher() {
    return publisher;
  }

  public void setPublisher(String publisher) {
    this.publisher = publisher;
  }

  public Project group(String group) {
    this.group = group;
    return this;
  }

   /**
   * Get group
   * @return group
  **/
  @ApiModelProperty(value = "")
  public String getGroup() {
    return group;
  }

  public void setGroup(String group) {
    this.group = group;
  }

  public Project name(String name) {
    this.name = name;
    return this;
  }

   /**
   * Get name
   * @return name
  **/
  @ApiModelProperty(value = "")
  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public Project description(String description) {
    this.description = description;
    return this;
  }

   /**
   * Get description
   * @return description
  **/
  @ApiModelProperty(value = "")
  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public Project version(String version) {
    this.version = version;
    return this;
  }

   /**
   * Get version
   * @return version
  **/
  @ApiModelProperty(value = "")
  public String getVersion() {
    return version;
  }

  public void setVersion(String version) {
    this.version = version;
  }

  public Project classifier(ClassifierEnum classifier) {
    this.classifier = classifier;
    return this;
  }

   /**
   * Get classifier
   * @return classifier
  **/
  @ApiModelProperty(value = "")
  public ClassifierEnum getClassifier() {
    return classifier;
  }

  public void setClassifier(ClassifierEnum classifier) {
    this.classifier = classifier;
  }

  public Project cpe(String cpe) {
    this.cpe = cpe;
    return this;
  }

   /**
   * Get cpe
   * @return cpe
  **/
  @ApiModelProperty(value = "")
  public String getCpe() {
    return cpe;
  }

  public void setCpe(String cpe) {
    this.cpe = cpe;
  }

  public Project purl(PackageURL purl) {
    this.purl = purl;
    return this;
  }

   /**
   * Get purl
   * @return purl
  **/
  @ApiModelProperty(value = "")
  public PackageURL getPurl() {
    return purl;
  }

  public void setPurl(PackageURL purl) {
    this.purl = purl;
  }

  public Project swidTagId(String swidTagId) {
    this.swidTagId = swidTagId;
    return this;
  }

   /**
   * Get swidTagId
   * @return swidTagId
  **/
  @ApiModelProperty(value = "")
  public String getSwidTagId() {
    return swidTagId;
  }

  public void setSwidTagId(String swidTagId) {
    this.swidTagId = swidTagId;
  }

  public Project directDependencies(String directDependencies) {
    this.directDependencies = directDependencies;
    return this;
  }

   /**
   * Get directDependencies
   * @return directDependencies
  **/
  @ApiModelProperty(value = "")
  public String getDirectDependencies() {
    return directDependencies;
  }

  public void setDirectDependencies(String directDependencies) {
    this.directDependencies = directDependencies;
  }

  public Project uuid(UUID uuid) {
    this.uuid = uuid;
    return this;
  }

   /**
   * Get uuid
   * @return uuid
  **/
  @ApiModelProperty(required = true, value = "")
  public UUID getUuid() {
    return uuid;
  }

  public void setUuid(UUID uuid) {
    this.uuid = uuid;
  }

  public Project parent(Project parent) {
    this.parent = parent;
    return this;
  }

   /**
   * Get parent
   * @return parent
  **/
  @ApiModelProperty(value = "")
  public Project getParent() {
    return parent;
  }

  public void setParent(Project parent) {
    this.parent = parent;
  }

  public Project children(List<Project> children) {
    this.children = children;
    return this;
  }

  public Project addChildrenItem(Project childrenItem) {
    if (this.children == null) {
      this.children = new ArrayList<>();
    }
    this.children.add(childrenItem);
    return this;
  }

   /**
   * Get children
   * @return children
  **/
  @ApiModelProperty(value = "")
  public List<Project> getChildren() {
    return children;
  }

  public void setChildren(List<Project> children) {
    this.children = children;
  }

  public Project properties(List<ProjectProperty> properties) {
    this.properties = properties;
    return this;
  }

  public Project addPropertiesItem(ProjectProperty propertiesItem) {
    if (this.properties == null) {
      this.properties = new ArrayList<>();
    }
    this.properties.add(propertiesItem);
    return this;
  }

   /**
   * Get properties
   * @return properties
  **/
  @ApiModelProperty(value = "")
  public List<ProjectProperty> getProperties() {
    return properties;
  }

  public void setProperties(List<ProjectProperty> properties) {
    this.properties = properties;
  }

  public Project tags(List<Tag> tags) {
    this.tags = tags;
    return this;
  }

  public Project addTagsItem(Tag tagsItem) {
    if (this.tags == null) {
      this.tags = new ArrayList<>();
    }
    this.tags.add(tagsItem);
    return this;
  }

   /**
   * Get tags
   * @return tags
  **/
  @ApiModelProperty(value = "")
  public List<Tag> getTags() {
    return tags;
  }

  public void setTags(List<Tag> tags) {
    this.tags = tags;
  }

  public Project lastBomImport(Double lastBomImport) {
    this.lastBomImport = lastBomImport;
    return this;
  }

   /**
   * Get lastBomImport
   * @return lastBomImport
  **/
  @ApiModelProperty(value = "")
  public Double getLastBomImport() {
    return lastBomImport;
  }

  public void setLastBomImport(Double lastBomImport) {
    this.lastBomImport = lastBomImport;
  }

  public Project lastBomImportFormat(String lastBomImportFormat) {
    this.lastBomImportFormat = lastBomImportFormat;
    return this;
  }

   /**
   * Get lastBomImportFormat
   * @return lastBomImportFormat
  **/
  @ApiModelProperty(value = "")
  public String getLastBomImportFormat() {
    return lastBomImportFormat;
  }

  public void setLastBomImportFormat(String lastBomImportFormat) {
    this.lastBomImportFormat = lastBomImportFormat;
  }

  public Project lastInheritedRiskScore(Double lastInheritedRiskScore) {
    this.lastInheritedRiskScore = lastInheritedRiskScore;
    return this;
  }

   /**
   * Get lastInheritedRiskScore
   * @return lastInheritedRiskScore
  **/
  @ApiModelProperty(value = "")
  public Double getLastInheritedRiskScore() {
    return lastInheritedRiskScore;
  }

  public void setLastInheritedRiskScore(Double lastInheritedRiskScore) {
    this.lastInheritedRiskScore = lastInheritedRiskScore;
  }

  public Project active(Boolean active) {
    this.active = active;
    return this;
  }

   /**
   * Get active
   * @return active
  **/
  @ApiModelProperty(value = "")
  public Boolean isActive() {
    return active;
  }

  public void setActive(Boolean active) {
    this.active = active;
  }

  public Project metrics(ProjectMetrics metrics) {
    this.metrics = metrics;
    return this;
  }

   /**
   * Get metrics
   * @return metrics
  **/
  @ApiModelProperty(value = "")
  public ProjectMetrics getMetrics() {
    return metrics;
  }

  public void setMetrics(ProjectMetrics metrics) {
    this.metrics = metrics;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Project project = (Project) o;
    return Objects.equals(this.author, project.author) &&
        Objects.equals(this.publisher, project.publisher) &&
        Objects.equals(this.group, project.group) &&
        Objects.equals(this.name, project.name) &&
        Objects.equals(this.description, project.description) &&
        Objects.equals(this.version, project.version) &&
        Objects.equals(this.classifier, project.classifier) &&
        Objects.equals(this.cpe, project.cpe) &&
        Objects.equals(this.purl, project.purl) &&
        Objects.equals(this.swidTagId, project.swidTagId) &&
        Objects.equals(this.directDependencies, project.directDependencies) &&
        Objects.equals(this.uuid, project.uuid) &&
        Objects.equals(this.parent, project.parent) &&
        Objects.equals(this.children, project.children) &&
        Objects.equals(this.properties, project.properties) &&
        Objects.equals(this.tags, project.tags) &&
        Objects.equals(this.lastBomImport, project.lastBomImport) &&
        Objects.equals(this.lastBomImportFormat, project.lastBomImportFormat) &&
        Objects.equals(this.lastInheritedRiskScore, project.lastInheritedRiskScore) &&
        Objects.equals(this.active, project.active) &&
        Objects.equals(this.metrics, project.metrics);
  }

  @Override
  public int hashCode() {
    return Objects.hash(author, publisher, group, name, description, version, classifier, cpe, purl, swidTagId, directDependencies, uuid, parent, children, properties, tags, lastBomImport, lastBomImportFormat, lastInheritedRiskScore, active, metrics);
  }


  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class Project {\n");
    
    sb.append("    author: ").append(toIndentedString(author)).append("\n");
    sb.append("    publisher: ").append(toIndentedString(publisher)).append("\n");
    sb.append("    group: ").append(toIndentedString(group)).append("\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    description: ").append(toIndentedString(description)).append("\n");
    sb.append("    version: ").append(toIndentedString(version)).append("\n");
    sb.append("    classifier: ").append(toIndentedString(classifier)).append("\n");
    sb.append("    cpe: ").append(toIndentedString(cpe)).append("\n");
    sb.append("    purl: ").append(toIndentedString(purl)).append("\n");
    sb.append("    swidTagId: ").append(toIndentedString(swidTagId)).append("\n");
    sb.append("    directDependencies: ").append(toIndentedString(directDependencies)).append("\n");
    sb.append("    uuid: ").append(toIndentedString(uuid)).append("\n");
    sb.append("    parent: ").append(toIndentedString(parent)).append("\n");
    sb.append("    children: ").append(toIndentedString(children)).append("\n");
    sb.append("    properties: ").append(toIndentedString(properties)).append("\n");
    sb.append("    tags: ").append(toIndentedString(tags)).append("\n");
    sb.append("    lastBomImport: ").append(toIndentedString(lastBomImport)).append("\n");
    sb.append("    lastBomImportFormat: ").append(toIndentedString(lastBomImportFormat)).append("\n");
    sb.append("    lastInheritedRiskScore: ").append(toIndentedString(lastInheritedRiskScore)).append("\n");
    sb.append("    active: ").append(toIndentedString(active)).append("\n");
    sb.append("    metrics: ").append(toIndentedString(metrics)).append("\n");
    sb.append("}");
    return sb.toString();
  }

  /**
   * Convert the given object to string with each line indented by 4 spaces
   * (except the first line).
   */
  private String toIndentedString(java.lang.Object o) {
    if (o == null) {
      return "null";
    }
    return o.toString().replace("\n", "\n    ");
  }

}

