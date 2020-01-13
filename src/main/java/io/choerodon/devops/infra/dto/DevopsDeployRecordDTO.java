package io.choerodon.devops.infra.dto;

import java.util.Date;
import javax.persistence.*;

import io.swagger.annotations.ApiModelProperty;

import io.choerodon.mybatis.entity.BaseDTO;

/**
 * Created by Sheep on 2019/7/29.
 */

@Table(name="devops_deploy_record")
public class DevopsDeployRecordDTO extends BaseDTO {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long projectId;
    private String deployType;
    private Long deployId;
    private String env;
    private Date deployTime;

    @Transient
    private String deployStatus;
    @Transient
    private String pipelineName;
    @Transient
    private String pipelineTriggerType;
    @Transient
    private Long deployCreatedBy;

    @Transient
    @ApiModelProperty("手动部署生成的实例id")
    private Long instanceId;

    @Transient
    @ApiModelProperty("手动部署生成的实例的环境id")
    private Long envId;

    @Transient
    @ApiModelProperty("手动部署的实例的应用服务id")
    private Long appServiceId;

    @Transient
    private String errorInfo;

    @Transient
    private String envName;

    public DevopsDeployRecordDTO() {
    }


    public DevopsDeployRecordDTO(Long projectId, String deployType, Long deployId, String env, Date deployTime) {
        this.projectId = projectId;
        this.deployType = deployType;
        this.deployId = deployId;
        this.env = env;
        this.deployTime = deployTime;
    }


    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getDeployType() {
        return deployType;
    }

    public void setDeployType(String deployType) {
        this.deployType = deployType;
    }

    public Long getDeployId() {
        return deployId;
    }

    public void setDeployId(Long deployId) {
        this.deployId = deployId;
    }

    public Long getProjectId() {
        return projectId;
    }

    public void setProjectId(Long projectId) {
        this.projectId = projectId;
    }

    public String getDeployStatus() {
        return deployStatus;
    }

    public void setDeployStatus(String deployStatus) {
        this.deployStatus = deployStatus;
    }

    public String getPipelineName() {
        return pipelineName;
    }

    public void setPipelineName(String pipelineName) {
        this.pipelineName = pipelineName;
    }

    public String getPipelineTriggerType() {
        return pipelineTriggerType;
    }

    public void setPipelineTriggerType(String pipelineTriggerType) {
        this.pipelineTriggerType = pipelineTriggerType;
    }

    public String getEnv() {
        return env;
    }

    public void setEnv(String env) {
        this.env = env;
    }

    public Long getDeployCreatedBy() {
        return deployCreatedBy;
    }

    public void setDeployCreatedBy(Long deployCreatedBy) {
        this.deployCreatedBy = deployCreatedBy;
    }

    public Date getDeployTime() {
        return deployTime;
    }

    public void setDeployTime(Date deployTime) {
        this.deployTime = deployTime;
    }

    public Long getInstanceId() {
        return instanceId;
    }

    public void setInstanceId(Long instanceId) {
        this.instanceId = instanceId;
    }

    public Long getEnvId() {
        return envId;
    }

    public void setEnvId(Long envId) {
        this.envId = envId;
    }

    public Long getAppServiceId() {
        return appServiceId;
    }

    public void setAppServiceId(Long appServiceId) {
        this.appServiceId = appServiceId;
    }

    public void setEnvName(String envName) {
        this.envName = envName;
    }

    public String getEnvName() {

        return envName;
    }

    public String getErrorInfo() {
        return errorInfo;
    }

    public void setErrorInfo(String errorInfo) {
        this.errorInfo = errorInfo;
    }
}
