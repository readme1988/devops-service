package io.choerodon.devops.app.eventhandler.payload;

import io.choerodon.devops.api.vo.DevopsUserPermissionVO;
import io.choerodon.devops.infra.dto.AppServiceDTO;

import java.util.List;

/**
 * @author crcokitwood
 */
public class DevOpsAppServicePayload {

    private Integer userId;
    private String path;
    private Integer gitlabProjectId;
    private Integer groupId;
    private String type;
    private Long organizationId;
    private Long appServiceId;
    private List<Long> userIds;
    private Boolean skipCheckPermission;
    private Long iamProjectId;
    private Long templateAppServiceId;
    private Long templateAppServiceVersionId;

    private AppServiceDTO appServiceDTO;
    private List<DevopsUserPermissionVO> devopsUserPermissionVOS;

    public List<DevopsUserPermissionVO> getDevopsUserPermissionVOS() {
        return devopsUserPermissionVOS;
    }

    public void setDevopsUserPermissionVOS(List<DevopsUserPermissionVO> devopsUserPermissionVOS) {
        this.devopsUserPermissionVOS = devopsUserPermissionVOS;
    }

    public AppServiceDTO getAppServiceDTO() {
        return appServiceDTO;
    }

    public void setAppServiceDTO(AppServiceDTO appServiceDTO) {
        this.appServiceDTO = appServiceDTO;
    }


    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public Integer getGitlabProjectId() {
        return gitlabProjectId;
    }

    public void setGitlabProjectId(Integer gitlabProjectId) {
        this.gitlabProjectId = gitlabProjectId;
    }

    public Integer getGroupId() {
        return groupId;
    }

    public void setGroupId(Integer groupId) {
        this.groupId = groupId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Long getOrganizationId() {
        return organizationId;
    }

    public void setOrganizationId(Long organizationId) {
        this.organizationId = organizationId;
    }

    public Long getAppServiceId() {
        return appServiceId;
    }

    public void setAppServiceId(Long appServiceId) {
        this.appServiceId = appServiceId;
    }

    public List<Long> getUserIds() {
        return userIds;
    }

    public void setUserIds(List<Long> userIds) {
        this.userIds = userIds;
    }

    public Boolean getSkipCheckPermission() {
        return skipCheckPermission;
    }

    public void setSkipCheckPermission(Boolean skipCheckPermission) {
        this.skipCheckPermission = skipCheckPermission;
    }

    public Long getIamProjectId() {
        return iamProjectId;
    }

    public void setIamProjectId(Long iamProjectId) {
        this.iamProjectId = iamProjectId;
    }

    public Long getTemplateAppServiceId() {
        return templateAppServiceId;
    }

    public void setTemplateAppServiceId(Long templateAppServiceId) {
        this.templateAppServiceId = templateAppServiceId;
    }

    public Long getTemplateAppServiceVersionId() {
        return templateAppServiceVersionId;
    }

    public void setTemplateAppServiceVersionId(Long templateAppServiceVersionId) {
        this.templateAppServiceVersionId = templateAppServiceVersionId;
    }
}
