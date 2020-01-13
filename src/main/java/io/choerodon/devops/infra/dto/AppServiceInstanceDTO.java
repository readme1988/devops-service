package io.choerodon.devops.infra.dto;

import javax.persistence.*;

import io.swagger.annotations.ApiModelProperty;

import io.choerodon.mybatis.entity.BaseDTO;

/**
 * Created by Zenger on 2018/4/14.
 */
@Table(name = "devops_app_service_instance")
public class AppServiceInstanceDTO extends BaseDTO {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String code;
    private Long appServiceId;
    private Long appServiceVersionId;
    private Long envId;
    private Long commandId;
    private String status;
    private Long valueId;
    @ApiModelProperty("组件所对应的实例的版本/普通实例这个值为null")
    private String componentVersion;
    @ApiModelProperty("组件对应实例的chart名称/普通实例这个值为null")
    private String componentChartName;
    @ApiModelProperty("当前实例生效的commandId")
    private Long effectCommandId;

    @Transient
    private String appServiceName;
    @Transient
    private String publishLevel;
    @Transient
    private String appServiceVersion;
    @Transient
    private String envCode;
    @Transient
    private String envName;
    @Transient
    private Long podCount;
    @Transient
    private Long podRunningCount;
    @Transient
    private Long serviceCount;
    @Transient
    private Long ingressCount;
    @Transient
    private String commandStatus;
    @Transient
    private String commandType;
    @Transient
    private String commandVersion;
    @Transient
    private Long commandVersionId;
    @Transient
    private String error;
    @Transient
    private Long projectId;
    @Transient
    private Integer isEnabled;
    @Transient
    private String appServiceCode;


    public Long getEffectCommandId() {
        return effectCommandId;
    }

    public void setEffectCommandId(Long effectCommandId) {
        this.effectCommandId = effectCommandId;
    }

    public Long getAppServiceVersionId() {
        return appServiceVersionId;
    }

    public void setAppServiceVersionId(Long appServiceVersionId) {
        this.appServiceVersionId = appServiceVersionId;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public Long getAppServiceId() {
        return appServiceId;
    }

    public void setAppServiceId(Long appServiceId) {
        this.appServiceId = appServiceId;
    }

    public Long getEnvId() {
        return envId;
    }

    public void setEnvId(Long envId) {
        this.envId = envId;
    }

    public String getEnvCode() {
        return envCode;
    }

    public void setEnvCode(String envCode) {
        this.envCode = envCode;
    }

    public String getEnvName() {
        return envName;
    }

    public void setEnvName(String envName) {
        this.envName = envName;
    }

    public Long getPodCount() {
        return podCount;
    }

    public void setPodCount(Long podCount) {
        this.podCount = podCount;
    }

    public Long getPodRunningCount() {
        return podRunningCount;
    }

    public void setPodRunningCount(Long podRunningCount) {
        this.podRunningCount = podRunningCount;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getCommandStatus() {
        return commandStatus;
    }

    public void setCommandStatus(String commandStatus) {
        this.commandStatus = commandStatus;
    }

    public String getCommandType() {
        return commandType;
    }

    public void setCommandType(String commandType) {
        this.commandType = commandType;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public String getPublishLevel() {
        return publishLevel;
    }

    public void setPublishLevel(String publishLevel) {
        this.publishLevel = publishLevel;
    }

    public Long getProjectId() {
        return projectId;
    }

    public void setProjectId(Long projectId) {
        this.projectId = projectId;
    }

    public Long getCommandId() {
        return commandId;
    }

    public void setCommandId(Long commandId) {
        this.commandId = commandId;
    }


    public String getCommandVersion() {
        return commandVersion;
    }

    public void setCommandVersion(String commandVersion) {
        this.commandVersion = commandVersion;
    }

    public Long getCommandVersionId() {
        return commandVersionId;
    }

    public void setCommandVersionId(Long commandVersionId) {
        this.commandVersionId = commandVersionId;
    }

    public Integer getIsEnabled() {
        return isEnabled;
    }

    public void setIsEnabled(Integer isEnabled) {
        this.isEnabled = isEnabled;
    }

    public Long getValueId() {
        return valueId;
    }

    public void setValueId(Long valueId) {
        this.valueId = valueId;
    }

    public Long getServiceCount() {
        return serviceCount;
    }

    public void setServiceCount(Long serviceCount) {
        this.serviceCount = serviceCount;
    }

    public Long getIngressCount() {
        return ingressCount;
    }

    public void setIngressCount(Long ingressCount) {
        this.ingressCount = ingressCount;
    }

    public String getAppServiceName() {
        return appServiceName;
    }

    public void setAppServiceName(String appServiceName) {
        this.appServiceName = appServiceName;
    }

    public String getAppServiceVersion() {
        return appServiceVersion;
    }

    public void setAppServiceVersion(String appServiceVersion) {
        this.appServiceVersion = appServiceVersion;
    }

    public String getAppServiceCode() {
        return appServiceCode;
    }

    public void setAppServiceCode(String appServiceCode) {
        this.appServiceCode = appServiceCode;
    }

    public String getComponentVersion() {
        return componentVersion;
    }

    public void setComponentVersion(String componentVersion) {
        this.componentVersion = componentVersion;
    }

    public String getComponentChartName() {
        return componentChartName;
    }

    public void setComponentChartName(String componentChartName) {
        this.componentChartName = componentChartName;
    }
}
