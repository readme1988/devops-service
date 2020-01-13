package io.choerodon.devops.api.vo;

import io.swagger.annotations.ApiModelProperty;

import javax.validation.constraints.NotNull;
import java.util.List;

/***
 * 添加pv权限分配：公开/仅对特定项目公开
 */
public class DevopsPvPermissionUpdateVO {

    public DevopsPvPermissionUpdateVO() {

    }

    public DevopsPvPermissionUpdateVO(@NotNull(message = "error.pv.id.null") Long pvId, @NotNull(message = "error.project.ids.null") List<Long> projectIds, @NotNull(message = "error.skip.check.project.permission.null") Boolean skipCheckProjectPermission, Long objectVersionNumber) {
        this.pvId = pvId;
        this.projectIds = projectIds;
        this.skipCheckProjectPermission = skipCheckProjectPermission;
        this.objectVersionNumber = objectVersionNumber;
    }

    @ApiModelProperty("pvId/必填")
    @NotNull(message = "error.pv.id.null")
    private Long pvId;

    @ApiModelProperty("pv公开范围，指定项目id/必填，可为空数组")
    @NotNull(message = "error.project.ids.null")
    private List<Long> projectIds;

    @ApiModelProperty("是否跳过项目权限校验/必填")
    @NotNull(message = "error.skip.check.project.permission.null")
    private Boolean skipCheckProjectPermission;

    @ApiModelProperty("更新了skipCheckProjectPermission字段")
    private Long objectVersionNumber;

    public Long getPvId() {
        return pvId;
    }

    public void setPvId(Long pvId) {
        this.pvId = pvId;
    }

    public List<Long> getProjectIds() {
        return projectIds;
    }

    public void setProjectIds(List<Long> projectIds) {
        this.projectIds = projectIds;
    }

    public Boolean getSkipCheckProjectPermission() {
        return skipCheckProjectPermission;
    }

    public void setSkipCheckProjectPermission(Boolean skipCheckProjectPermission) {
        this.skipCheckProjectPermission = skipCheckProjectPermission;
    }

    public Long getObjectVersionNumber() {
        return objectVersionNumber;
    }

    public void setObjectVersionNumber(Long objectVersionNumber) {
        this.objectVersionNumber = objectVersionNumber;
    }
}
