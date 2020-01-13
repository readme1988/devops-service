package io.choerodon.devops.api.controller.v1;

import java.util.List;
import java.util.Optional;

import com.github.pagehelper.PageInfo;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import io.choerodon.core.annotation.Permission;

import org.springframework.data.domain.Pageable;

import io.choerodon.core.enums.ResourceType;
import io.choerodon.core.exception.CommonException;
import io.choerodon.core.iam.InitRoleCode;
import io.choerodon.devops.api.vo.*;
import io.choerodon.devops.app.service.PipelineService;
import io.choerodon.swagger.annotation.CustomPageRequest;

/**
 * Creator: ChangpingShi0213@gmail.com
 * Date:  19:48 2019/4/3
 * Description:
 */
@RestController
@RequestMapping(value = "/v1/projects/{project_id}/pipeline")
public class PipelineController {
    @Autowired
    private PipelineService pipelineService;

    /**
     * 项目下创建流水线
     *
     * @param projectId     项目id
     * @param pipelineReqVO 流水线信息
     * @return PipelineAppDeployDTO
     */
    @Permission(type = ResourceType.PROJECT, roles = {InitRoleCode.PROJECT_OWNER, InitRoleCode.PROJECT_MEMBER})
    @ApiOperation(value = "项目下创建流水线")
    @PostMapping
    public ResponseEntity create(
            @ApiParam(value = "项目id", required = true)
            @PathVariable(value = "project_id") Long projectId,
            @ApiParam(value = "服务信息", required = true)
            @RequestBody PipelineReqVO pipelineReqVO) {
        return Optional.ofNullable(pipelineService.create(projectId, pipelineReqVO))
                .map(target -> new ResponseEntity<>(target, HttpStatus.OK))
                .orElseThrow(() -> new CommonException("error.pipeline.create"));
    }

    /**
     * 项目下更新流水线
     *
     * @param projectId     项目id
     * @param pipelineReqVO 流水线信息
     * @return PipelineAppDeployDTO
     */
    @Permission(type = ResourceType.PROJECT, roles = {InitRoleCode.PROJECT_OWNER, InitRoleCode.PROJECT_MEMBER})
    @ApiOperation(value = "项目下更新流水线")
    @PutMapping
    public ResponseEntity<PipelineReqVO> update(
            @ApiParam(value = "项目id", required = true)
            @PathVariable(value = "project_id") Long projectId,
            @ApiParam(value = "服务信息", required = true)
            @RequestBody PipelineReqVO pipelineReqVO) {
        return Optional.ofNullable(pipelineService.update(projectId, pipelineReqVO))
                .map(target -> new ResponseEntity<>(target, HttpStatus.OK))
                .orElseThrow(() -> new CommonException("error.pipeline.update"));
    }

    /**
     * 项目下删除流水线
     *
     * @param projectId  项目Id
     * @param pipelineId 流水线Id
     * @return PipelineAppDeployDTO
     */
    @Permission(type = ResourceType.PROJECT, roles = {InitRoleCode.PROJECT_OWNER})
    @ApiOperation(value = "项目下删除流水线")
    @DeleteMapping(value = "/{pipeline_id}")
    public ResponseEntity delete(
            @ApiParam(value = "项目id", required = true)
            @PathVariable(value = "project_id") Long projectId,
            @ApiParam(value = "流水线Id", required = true)
            @PathVariable(value = "pipeline_id") Long pipelineId) {
        pipelineService.delete(projectId, pipelineId);
        return new ResponseEntity(HttpStatus.NO_CONTENT);
    }

    /**
     * 启/停用流水线
     *
     * @param projectId  项目id
     * @param pipelineId 流水线Id
     * @param isEnabled  是否启用
     * @return PipelineDTO
     */
    @Permission(type = ResourceType.PROJECT, roles = {InitRoleCode.PROJECT_OWNER})
    @ApiOperation(value = "启/停用流水线")
    @PutMapping(value = "/{pipeline_id}")
    public ResponseEntity<PipelineVO> updateIsEnabled(
            @ApiParam(value = "项目id", required = true)
            @PathVariable(value = "project_id") Long projectId,
            @ApiParam(value = "流水线Id", required = true)
            @PathVariable(value = "pipeline_id") Long pipelineId,
            @ApiParam(value = "是否启用", required = true)
            @RequestParam Integer isEnabled) {
        return Optional.ofNullable(pipelineService.updateIsEnabled(projectId, pipelineId, isEnabled))
                .map(target -> new ResponseEntity<>(target, HttpStatus.OK))
                .orElseThrow(() -> new CommonException("error.pipeline.update.enable"));
    }

    /**
     * 查询流水线详情
     *
     * @param projectId  项目id
     * @param pipelineId 流水线Id
     * @return PipelineReqVO
     */
    @Permission(type = ResourceType.PROJECT, roles = {InitRoleCode.PROJECT_OWNER, InitRoleCode.PROJECT_MEMBER})
    @ApiOperation(value = "查询流水线详情")
    @GetMapping(value = "/{pipeline_id}")
    public ResponseEntity<PipelineReqVO> queryById(
            @ApiParam(value = "项目id", required = true)
            @PathVariable(value = "project_id") Long projectId,
            @ApiParam(value = "流水线Id", required = true)
            @PathVariable(value = "pipeline_id") Long pipelineId) {
        return Optional.ofNullable(pipelineService.queryById(projectId, pipelineId))
                .map(target -> new ResponseEntity<>(target, HttpStatus.OK))
                .orElseThrow(() -> new CommonException("error.pipeline.query.detail"));
    }

    /**
     * 项目下获取流水线
     *
     * @param projectId        项目Id
     * @param pageable         分页参数
     * @param pipelineSearchVO 查询参数
     * @return 流水线页
     */
    @Permission(type = ResourceType.PROJECT, roles = {InitRoleCode.PROJECT_OWNER, InitRoleCode.PROJECT_MEMBER})
    @ApiOperation(value = "项目下分页流水线")
    @CustomPageRequest
    @PostMapping("/page_by_options")
    public ResponseEntity<PageInfo<PipelineVO>> pageByOptions(
            @ApiParam(value = "项目Id", required = true)
            @PathVariable(value = "project_id") Long projectId,
            @ApiParam(value = "分页参数")
                    Pageable pageable,
            @ApiParam(value = "查询参数")
            @RequestBody(required = false) PipelineSearchVO pipelineSearchVO) {
        return Optional.ofNullable(pipelineService.pageByOptions(projectId, pipelineSearchVO, pageable))
                .map(target -> new ResponseEntity<>(target, HttpStatus.OK))
                .orElseThrow(() -> new CommonException("error.pipeline.list"));
    }


    /**
     * 执行流水线
     *
     * @param projectId  项目id
     * @param pipelineId 流水线Id
     * @return PipelineReqVO
     */
    @Permission(type = ResourceType.PROJECT, roles = {InitRoleCode.PROJECT_OWNER, InitRoleCode.PROJECT_MEMBER})
    @ApiOperation(value = "执行流水线")
    @GetMapping(value = "/{pipeline_id}/execute")
    public ResponseEntity execute(
            @ApiParam(value = "项目id", required = true)
            @PathVariable(value = "project_id") Long projectId,
            @ApiParam(value = "流水线Id", required = true)
            @PathVariable(value = "pipeline_id") Long pipelineId) {
        pipelineService.execute(projectId, pipelineId);
        return new ResponseEntity(HttpStatus.NO_CONTENT);
    }

    /**
     * 批量执行流水线
     *
     * @param projectId   项目id
     * @param pipelineIds 流水线Id
     * @return PipelineReqVO
     */
    @Permission(type = ResourceType.PROJECT, roles = {InitRoleCode.PROJECT_OWNER, InitRoleCode.PROJECT_MEMBER})
    @ApiOperation(value = "批量执行流水线")
    @GetMapping(value = "/batch_execute")
    public ResponseEntity batchExecute(
            @ApiParam(value = "项目id", required = true)
            @PathVariable(value = "project_id") Long projectId,
            @ApiParam(value = "流水线Ids", required = true)
            @RequestParam Long[] pipelineIds) {
        pipelineService.batchExecute(projectId, pipelineIds);
        return new ResponseEntity(HttpStatus.NO_CONTENT);
    }

    @Permission(type = ResourceType.PROJECT, roles = {InitRoleCode.PROJECT_OWNER, InitRoleCode.PROJECT_MEMBER})
    @ApiOperation(value = "人工审核")
    @PostMapping("/audit")
    public ResponseEntity<List<PipelineUserVO>> audit(
            @ApiParam(value = "项目Id", required = true)
            @PathVariable(value = "project_id") Long projectId,
            @ApiParam(value = "PipelineUserRelDTO", required = true)
            @RequestBody PipelineUserRecordRelationshipVO userRecordRelDTO) {
        return Optional.ofNullable(pipelineService.audit(projectId, userRecordRelDTO))
                .map(target -> new ResponseEntity<>(target, HttpStatus.OK))
                .orElseThrow(() -> new CommonException("error.pipeline.audit"));
    }

    @Permission(type = ResourceType.PROJECT, roles = {InitRoleCode.PROJECT_OWNER, InitRoleCode.PROJECT_MEMBER})
    @ApiOperation(value = "校验人工审核")
    @PostMapping("/check_audit")
    public ResponseEntity<CheckAuditVO> checkAudit(
            @ApiParam(value = "项目Id", required = true)
            @PathVariable(value = "project_id") Long projectId,
            @ApiParam(value = "PipelineUserRelDTO", required = true)
            @RequestBody PipelineUserRecordRelationshipVO userRecordRelDTO) {
        return Optional.ofNullable(pipelineService.checkAudit(projectId, userRecordRelDTO))
                .map(target -> new ResponseEntity<>(target, HttpStatus.OK))
                .orElseThrow(() -> new CommonException("error.pipeline.audit.check"));
    }

    @Permission(type = ResourceType.PROJECT, roles = {InitRoleCode.PROJECT_OWNER, InitRoleCode.PROJECT_MEMBER})
    @ApiOperation(value = "条件校验")
    @GetMapping("/check_deploy")
    public ResponseEntity<PipelineCheckDeployVO> checkDeploy(
            @ApiParam(value = "项目Id", required = true)
            @PathVariable(value = "project_id") Long projectId,
            @ApiParam(value = "记录Id", required = true)
            @RequestParam(value = "pipeline_id") Long pipelineId) {
        return Optional.ofNullable(pipelineService.checkDeploy(projectId, pipelineId))
                .map(target -> new ResponseEntity<>(target, HttpStatus.OK))
                .orElseThrow(() -> new CommonException("error.pipeline.check.deploy"));
    }

    /**
     * 查询流水线记录详情
     *
     * @param projectId 项目id
     * @param recordId  流水线记录Id
     * @return PipelineRecordReqVO
     */
    @Permission(type = ResourceType.PROJECT, roles = {InitRoleCode.PROJECT_OWNER, InitRoleCode.PROJECT_MEMBER})
    @ApiOperation(value = "查询流水线记录详情")
    @GetMapping(value = "/{pipeline_record_id}/record_detail")
    public ResponseEntity<PipelineRecordReqVO> getRecordById(
            @ApiParam(value = "项目id", required = true)
            @PathVariable(value = "project_id") Long projectId,
            @ApiParam(value = "流水线Id", required = true)
            @PathVariable(value = "pipeline_record_id") Long recordId) {
        return Optional.ofNullable(pipelineService.getRecordById(projectId, recordId))
                .map(target -> new ResponseEntity<>(target, HttpStatus.OK))
                .orElseThrow(() -> new CommonException("error.pipeline.record.query"));
    }

    /**
     * 流水线重试
     *
     * @param projectId 项目id
     * @param recordId  流水线记录Id
     * @return PipelineRecordReqVO
     */
    @Permission(type = ResourceType.PROJECT, roles = {InitRoleCode.PROJECT_OWNER, InitRoleCode.PROJECT_MEMBER})
    @ApiOperation(value = "流水线重试")
    @GetMapping(value = "/{pipeline_record_id}/retry")
    public ResponseEntity retry(
            @ApiParam(value = "项目id", required = true)
            @PathVariable(value = "project_id") Long projectId,
            @ApiParam(value = "流水线记录Id", required = true)
            @PathVariable(value = "pipeline_record_id") Long recordId) {
        pipelineService.retry(projectId, recordId);
        return new ResponseEntity(HttpStatus.NO_CONTENT);
    }

    /**
     * 流水线所有记录
     *
     * @param projectId  项目id
     * @param pipelineId 流水线记录Id
     * @return List<PipelineRecordDTO>
     */
    @Permission(type = ResourceType.PROJECT, roles = {InitRoleCode.PROJECT_OWNER, InitRoleCode.PROJECT_MEMBER})
    @ApiOperation(value = "流水线所有记录")
    @GetMapping(value = "/{pipeline_id}/list_record")
    public ResponseEntity<List<PipelineRecordListVO>> queryByPipelineId(
            @ApiParam(value = "项目id", required = true)
            @PathVariable(value = "project_id") Long projectId,
            @ApiParam(value = "流水线Id", required = true)
            @PathVariable(value = "pipeline_id") Long pipelineId) {
        return Optional.ofNullable(pipelineService.queryByPipelineId(pipelineId))
                .map(target -> new ResponseEntity<>(target, HttpStatus.OK))
                .orElseThrow(() -> new CommonException("error.pipeline.record.list"));
    }

    @Permission(type = ResourceType.PROJECT, roles = {InitRoleCode.PROJECT_OWNER, InitRoleCode.PROJECT_MEMBER})
    @ApiOperation(value = "名称校验")
    @GetMapping(value = "/check_name")
    public ResponseEntity checkName(
            @ApiParam(value = "项目id", required = true)
            @PathVariable(value = "project_id") Long projectId,
            @ApiParam(value = "流水线名称", required = true)
            @RequestParam(value = "name") String name) {
        pipelineService.checkName(projectId, name);
        return new ResponseEntity(HttpStatus.NO_CONTENT);
    }

    @Permission(type = ResourceType.PROJECT, roles = {InitRoleCode.PROJECT_OWNER, InitRoleCode.PROJECT_MEMBER})
    @ApiOperation(value = "获取所有流水线")
    @GetMapping(value = "/list_all")
    public ResponseEntity<List<PipelineVO>> listPipelineDTO(
            @ApiParam(value = "项目id", required = true)
            @PathVariable(value = "project_id") Long projectId) {
        return Optional.ofNullable(pipelineService.listPipelineDTO(projectId))
                .map(target -> new ResponseEntity<>(target, HttpStatus.OK))
                .orElseThrow(() -> new CommonException("error.pipeline.all.list"));
    }


    @Permission(type = ResourceType.PROJECT, roles = {InitRoleCode.PROJECT_OWNER})
    @ApiOperation(value = "停止流水线")
    @GetMapping(value = "/failed")
    public ResponseEntity failed(
            @ApiParam(value = "项目id", required = true)
            @PathVariable(value = "project_id") Long projectId,
            @ApiParam(value = "流水线记录Id", required = true)
            @RequestParam(value = "pipeline_record_id") Long recordId) {
        pipelineService.failed(projectId, recordId);
        return new ResponseEntity(HttpStatus.NO_CONTENT);
    }
}
