package io.choerodon.devops.api.controller.v1;

import java.util.Optional;

import com.github.pagehelper.PageInfo;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import springfox.documentation.annotations.ApiIgnore;

import io.choerodon.core.annotation.Permission;
import io.choerodon.core.enums.ResourceType;
import io.choerodon.core.exception.CommonException;
import io.choerodon.core.iam.InitRoleCode;
import io.choerodon.devops.api.vo.DevopsDeployRecordVO;
import io.choerodon.devops.app.service.DevopsDeployRecordService;
import io.choerodon.swagger.annotation.CustomPageRequest;

/**
 * Created by Sheep on 2019/7/30.
 */
@RestController
@RequestMapping("/v1/projects/{project_id}/deploy_record")
public class DevopsDeployRecordController {
    @Autowired
    private DevopsDeployRecordService devopsDeployRecordService;

    /**
     * 项目下获取部署记录
     *
     * @param projectId 项目Id
     * @param pageable  分页参数
     * @param params    查询参数
     * @return 部署纪录页
     */
    @Permission(type = ResourceType.PROJECT, roles = {InitRoleCode.PROJECT_OWNER, InitRoleCode.PROJECT_MEMBER})
    @ApiOperation(value = "项目下分页查询部署记录")
    @CustomPageRequest
    @PostMapping("/page_by_options")
    public ResponseEntity<PageInfo<DevopsDeployRecordVO>> pageByOptions(
            @ApiParam(value = "项目Id", required = true)
            @PathVariable(value = "project_id") Long projectId,
            @ApiParam(value = "分页参数")
            @ApiIgnore Pageable pageable,
            @ApiParam(value = "查询参数")
            @RequestBody(required = false) String params) {
        return Optional.ofNullable(devopsDeployRecordService.pageByProjectId(projectId, params, pageable))
                .map(target -> new ResponseEntity<>(target, HttpStatus.OK))
                .orElseThrow(() -> new CommonException("error.pipeline.value.list"));
    }
}
