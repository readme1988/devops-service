package io.choerodon.devops.api.controller.v1;

import com.github.pagehelper.PageInfo;
import io.choerodon.core.annotation.Permission;
import io.choerodon.core.enums.ResourceType;
import io.choerodon.core.exception.CommonException;
import io.choerodon.core.iam.InitRoleCode;
import io.choerodon.devops.api.vo.*;
import io.choerodon.devops.app.service.DevopsPvService;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.SortDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import springfox.documentation.annotations.ApiIgnore;

import javax.validation.Valid;
import java.util.List;
import java.util.Optional;


@RestController
@RequestMapping("/v1/projects/{project_id}/pvs")
public class DevopsPvController {

    private static final String ERROR_PV_QUERY = "error.pv.query";
    private static final String ERROR_PROJECT_QUERY = "error.project.query";

    @Autowired
    DevopsPvService devopsPvService;

    @Permission(type = ResourceType.PROJECT, roles = {InitRoleCode.PROJECT_OWNER})
    @ApiOperation(value = "分页带参数查询项目下所有pv")
    @PostMapping("/page_by_options")
    public ResponseEntity<PageInfo<DevopsPvVO>> queryAll(
            @ApiParam(value = "项目id", required = true)
            @PathVariable(value = "project_id") Long projectId,
            @ApiParam(value = "分页参数")
            @ApiIgnore @SortDefault(value = "id", direction = Sort.Direction.DESC) Pageable pageable,
            @ApiParam(value = "模糊搜索参数")
            @RequestBody(required = false) String params) {
        return Optional.ofNullable(devopsPvService.pageByOptions(projectId, pageable, params))
                .map(target -> new ResponseEntity<>(target, HttpStatus.OK))
                .orElseThrow(() -> new CommonException(ERROR_PV_QUERY));
    }

    @Permission(type = ResourceType.PROJECT, roles = {InitRoleCode.PROJECT_OWNER})
    @ApiOperation(value = "创建pv")
    @PostMapping
    public ResponseEntity createPv(
            @ApiParam(value = "项目id", required = true)
            @PathVariable(value = "project_id") Long projectId,
            @RequestBody @Valid DevopsPvReqVO devopsPvReqVo) {
        devopsPvService.createPv(projectId, devopsPvReqVo);
        return new ResponseEntity(HttpStatus.NO_CONTENT);
    }

    /**
     * 校验pv的名称是否满足所选集群下唯一
     */
    @Permission(type = ResourceType.PROJECT, roles = {InitRoleCode.PROJECT_OWNER})
    @ApiOperation(value = "校验pv的名称是否满足所选集群下唯一")
    @GetMapping("check_name")
    public ResponseEntity checkPvName(
            @ApiParam(value = "项目id", required = true)
            @PathVariable(value = "project_id") Long projectId,
            @ApiParam(value = "集群Id", required = true)
            @RequestParam Long clusterId,
            @ApiParam(value = "pv名称", required = true)
            @RequestParam String pvName) {
        devopsPvService.checkName(clusterId, pvName);
        return new ResponseEntity(HttpStatus.NO_CONTENT);
    }

    @Permission(type = ResourceType.PROJECT, roles = {InitRoleCode.PROJECT_OWNER})
    @ApiOperation(value = "根据pvId删除Pv")
    @DeleteMapping("/{pv_id}")
    public ResponseEntity deletePv(
            @ApiParam(value = "项目id", required = true)
            @PathVariable(value = "project_id") Long projectId,
            @ApiParam(value = "pvId", required = true)
            @PathVariable(value = "pv_id") Long pvId) {
        devopsPvService.deletePvById(pvId);
        return new ResponseEntity(HttpStatus.NO_CONTENT);
    }

    @Permission(type = ResourceType.PROJECT, roles = {InitRoleCode.PROJECT_OWNER})
    @ApiOperation(value = "根据pvId查询相应pv")
    @GetMapping("/{pv_id}")
    public ResponseEntity<DevopsPvVO> queryById(
            @ApiParam(value = "项目id", required = true)
            @PathVariable(value = "project_id") Long projectId,
            @ApiParam(value = "pvId", required = true)
            @PathVariable(value = "pv_id") Long pvId) {
        return Optional.ofNullable(devopsPvService.queryById(pvId))
                .map(target -> new ResponseEntity<>(target, HttpStatus.OK))
                .orElseThrow(() -> new CommonException(ERROR_PV_QUERY));
    }

    /**
     * 列出组织下所有项目中在数据库中没有和当前Pv有权限关联关系的项目(不论当前数据库中是否跳过权限检查)
     *
     * @param projectId 项目ID
     * @param pvId      PVID
     * @param params    搜索参数
     * @return 所有与该证书未分配权限的项目
     */
    @Permission(type = ResourceType.PROJECT, roles = {InitRoleCode.PROJECT_OWNER})
    @ApiOperation(value = "列出组织下所有项目中没有分配权限的项目")
    @PostMapping(value = "/{pv_id}/permission/list_non_related")
    public ResponseEntity<PageInfo<ProjectReqVO>> listAllNonRelatedMembers(
            @ApiParam(value = "项目id", required = true)
            @PathVariable(value = "project_id") Long projectId,
            @ApiParam(value = "PvId", required = true)
            @PathVariable(value = "pv_id") Long pvId,
            @ApiParam(value = "分页参数")
            @ApiIgnore Pageable pageable,
            @ApiParam(value = "指定项目id")
            @RequestParam(value = "id", required = false) Long selectedProjectId,
            @ApiParam(value = "查询参数")
            @RequestBody(required = false) String params) {
        return Optional.ofNullable(devopsPvService.listNonRelatedProjects(projectId, pvId, selectedProjectId, pageable, params))
                .map(target -> new ResponseEntity<>(target, HttpStatus.OK))
                .orElseThrow(() -> new CommonException("error.get.pv.non.related.project"));
    }

    @Permission(type = ResourceType.PROJECT, roles = {InitRoleCode.PROJECT_OWNER})
    @ApiOperation(value = "根据projectId删除和Pv关联的权限记录")
    @DeleteMapping(value = "/{pv_id}/permission")
    public ResponseEntity deleteRelateProjectById(
            @ApiParam(value = "项目id", required = true)
            @PathVariable(value = "project_id") Long projectId,
            @ApiParam(value = "PvId", required = true)
            @PathVariable(value = "pv_id") Long pvId,
            @ApiParam(value = "要删除的proejctId")
            @RequestParam(value = "related_project_id") Long relatedProjectId) {
        devopsPvService.deleteRelatedProjectById(pvId, relatedProjectId);
        return new ResponseEntity(HttpStatus.NO_CONTENT);
    }

    @Permission(type = ResourceType.PROJECT, roles = {InitRoleCode.PROJECT_OWNER})
    @ApiOperation(value = "给当前pv分配项目权限")
    @PostMapping(value = "/{pv_id}/permission")
    public ResponseEntity assignPermission(
            @ApiParam(value = "项目id", required = true)
            @PathVariable(value = "project_id") Long projectId,
            @ApiParam(value = "PvId", required = true)
            @PathVariable(value = "pv_id") Long pvId,
            @RequestBody @Valid DevopsPvPermissionUpdateVO devopsPvPermissionUpdateVO) {
        devopsPvService.assignPermission(devopsPvPermissionUpdateVO);
        return new ResponseEntity(HttpStatus.NO_CONTENT);
    }


    @Permission(type = ResourceType.PROJECT, roles = {InitRoleCode.PROJECT_OWNER})
    @ApiOperation(value = "PV跳过权限校验，查询所属集群下的所有项目")
    @PostMapping("/{pv_id}/page_projects")
    public ResponseEntity<PageInfo<ProjectReqVO>> pageProjects(
            @ApiParam(value = "项目ID", required = true)
            @PathVariable(value = "project_id") Long projectId,
            @ApiParam(value = "PvId")
            @PathVariable(value = "pv_id") Long pvId,
            @ApiParam(value = "分页参数")
            @ApiIgnore Pageable pageable,
            @ApiParam(value = "模糊搜索参数")
            @RequestBody(required = false) String params) {
        return Optional.ofNullable(devopsPvService.pageProjects(projectId, pvId, pageable, params))
                .map(target -> new ResponseEntity<>(target, HttpStatus.OK))
                .orElseThrow(() -> new CommonException(ERROR_PROJECT_QUERY));
    }

    @Permission(type = ResourceType.PROJECT, roles = {InitRoleCode.PROJECT_OWNER})
    @ApiOperation(value = "PV不跳过权限校验，查询有关联的项目")
    @PostMapping("/{pv_id}/page_related")
    public ResponseEntity<PageInfo<ProjectReqVO>> pageRelatedProjects(
            @ApiParam(value = "项目ID", required = true)
            @PathVariable(value = "project_id") Long projectId,
            @ApiParam(value = "PvId")
            @PathVariable(value = "pv_id") Long pvId,
            @ApiParam(value = "分页参数")
            @ApiIgnore Pageable pageable,
            @ApiParam(value = "模糊搜索参数")
            @RequestBody(required = false) String params) {
        return Optional.ofNullable(devopsPvService.pageRelatedProjects(projectId, pvId, pageable, params))
                .map(target -> new ResponseEntity<>(target, HttpStatus.OK))
                .orElseThrow(() -> new CommonException(ERROR_PROJECT_QUERY));
    }

    @Permission(type = ResourceType.PROJECT, roles = {InitRoleCode.PROJECT_OWNER})
    @ApiOperation(value = "根据pvc的条件筛选查询可用的pv")
    @PostMapping("/pv_available")
    public ResponseEntity<List<DevopsPvVO>> queryPvcRelatedPv(
            @ApiParam(value = "项目ID", required = true)
            @PathVariable(value = "project_id") Long projectId,
            @ApiParam(value = "环境id")
            @RequestParam(value = "env_id", required = false) Long envId,
            @ApiParam(value = "集群id")
            @RequestParam(value = "cluster_id", required = false) Long clusterId,
            @ApiParam(value = "模糊搜索参数")
            @RequestBody(required = false) String params) {
        return Optional.ofNullable(devopsPvService.queryPvcRelatedPv(projectId, envId, clusterId, params))
                .map(target -> new ResponseEntity<>(target, HttpStatus.OK))
                .orElseThrow(() -> new CommonException(ERROR_PV_QUERY));
    }
}
