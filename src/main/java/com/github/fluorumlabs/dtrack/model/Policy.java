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
import com.github.fluorumlabs.dtrack.model.PolicyCondition;
import com.github.fluorumlabs.dtrack.model.Project;
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
 * Policy
 */
@javax.annotation.Generated(value = "io.swagger.codegen.languages.JavaClientCodegen", date = "2021-05-21T09:42:21.596Z")
public class Policy {
  @SerializedName("name")
  private String name = null;

  /**
   * Gets or Sets operator
   */
  @JsonAdapter(OperatorEnum.Adapter.class)
  public enum OperatorEnum {
    ALL("ALL"),
    
    ANY("ANY");

    private String value;

    OperatorEnum(String value) {
      this.value = value;
    }

    public String getValue() {
      return value;
    }

    @Override
    public String toString() {
      return String.valueOf(value);
    }

    public static OperatorEnum fromValue(String text) {
      for (OperatorEnum b : OperatorEnum.values()) {
        if (String.valueOf(b.value).equals(text)) {
          return b;
        }
      }
      return null;
    }

    public static class Adapter extends TypeAdapter<OperatorEnum> {
      @Override
      public void write(final JsonWriter jsonWriter, final OperatorEnum enumeration) throws IOException {
        jsonWriter.value(enumeration.getValue());
      }

      @Override
      public OperatorEnum read(final JsonReader jsonReader) throws IOException {
        String value = jsonReader.nextString();
        return OperatorEnum.fromValue(String.valueOf(value));
      }
    }
  }

  @SerializedName("operator")
  private OperatorEnum operator = null;

  /**
   * Gets or Sets violationState
   */
  @JsonAdapter(ViolationStateEnum.Adapter.class)
  public enum ViolationStateEnum {
    INFO("INFO"),
    
    WARN("WARN"),
    
    FAIL("FAIL");

    private String value;

    ViolationStateEnum(String value) {
      this.value = value;
    }

    public String getValue() {
      return value;
    }

    @Override
    public String toString() {
      return String.valueOf(value);
    }

    public static ViolationStateEnum fromValue(String text) {
      for (ViolationStateEnum b : ViolationStateEnum.values()) {
        if (String.valueOf(b.value).equals(text)) {
          return b;
        }
      }
      return null;
    }

    public static class Adapter extends TypeAdapter<ViolationStateEnum> {
      @Override
      public void write(final JsonWriter jsonWriter, final ViolationStateEnum enumeration) throws IOException {
        jsonWriter.value(enumeration.getValue());
      }

      @Override
      public ViolationStateEnum read(final JsonReader jsonReader) throws IOException {
        String value = jsonReader.nextString();
        return ViolationStateEnum.fromValue(String.valueOf(value));
      }
    }
  }

  @SerializedName("violationState")
  private ViolationStateEnum violationState = null;

  @SerializedName("policyConditions")
  private List<PolicyCondition> policyConditions = null;

  @SerializedName("projects")
  private List<Project> projects = null;

  @SerializedName("uuid")
  private UUID uuid = null;

  @SerializedName("global")
  private Boolean global = null;

  public Policy name(String name) {
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

  public Policy operator(OperatorEnum operator) {
    this.operator = operator;
    return this;
  }

   /**
   * Get operator
   * @return operator
  **/
  @ApiModelProperty(value = "")
  public OperatorEnum getOperator() {
    return operator;
  }

  public void setOperator(OperatorEnum operator) {
    this.operator = operator;
  }

  public Policy violationState(ViolationStateEnum violationState) {
    this.violationState = violationState;
    return this;
  }

   /**
   * Get violationState
   * @return violationState
  **/
  @ApiModelProperty(value = "")
  public ViolationStateEnum getViolationState() {
    return violationState;
  }

  public void setViolationState(ViolationStateEnum violationState) {
    this.violationState = violationState;
  }

  public Policy policyConditions(List<PolicyCondition> policyConditions) {
    this.policyConditions = policyConditions;
    return this;
  }

  public Policy addPolicyConditionsItem(PolicyCondition policyConditionsItem) {
    if (this.policyConditions == null) {
      this.policyConditions = new ArrayList<>();
    }
    this.policyConditions.add(policyConditionsItem);
    return this;
  }

   /**
   * Get policyConditions
   * @return policyConditions
  **/
  @ApiModelProperty(value = "")
  public List<PolicyCondition> getPolicyConditions() {
    return policyConditions;
  }

  public void setPolicyConditions(List<PolicyCondition> policyConditions) {
    this.policyConditions = policyConditions;
  }

  public Policy projects(List<Project> projects) {
    this.projects = projects;
    return this;
  }

  public Policy addProjectsItem(Project projectsItem) {
    if (this.projects == null) {
      this.projects = new ArrayList<>();
    }
    this.projects.add(projectsItem);
    return this;
  }

   /**
   * Get projects
   * @return projects
  **/
  @ApiModelProperty(value = "")
  public List<Project> getProjects() {
    return projects;
  }

  public void setProjects(List<Project> projects) {
    this.projects = projects;
  }

  public Policy uuid(UUID uuid) {
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

  public Policy global(Boolean global) {
    this.global = global;
    return this;
  }

   /**
   * Get global
   * @return global
  **/
  @ApiModelProperty(value = "")
  public Boolean isGlobal() {
    return global;
  }

  public void setGlobal(Boolean global) {
    this.global = global;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Policy policy = (Policy) o;
    return Objects.equals(this.name, policy.name) &&
        Objects.equals(this.operator, policy.operator) &&
        Objects.equals(this.violationState, policy.violationState) &&
        Objects.equals(this.policyConditions, policy.policyConditions) &&
        Objects.equals(this.projects, policy.projects) &&
        Objects.equals(this.uuid, policy.uuid) &&
        Objects.equals(this.global, policy.global);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, operator, violationState, policyConditions, projects, uuid, global);
  }


  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class Policy {\n");
    
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    operator: ").append(toIndentedString(operator)).append("\n");
    sb.append("    violationState: ").append(toIndentedString(violationState)).append("\n");
    sb.append("    policyConditions: ").append(toIndentedString(policyConditions)).append("\n");
    sb.append("    projects: ").append(toIndentedString(projects)).append("\n");
    sb.append("    uuid: ").append(toIndentedString(uuid)).append("\n");
    sb.append("    global: ").append(toIndentedString(global)).append("\n");
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

