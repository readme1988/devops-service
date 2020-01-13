package io.choerodon.devops.api.controller.v1;

import java.util.Optional;
import javax.validation.Valid;

import com.github.pagehelper.PageInfo;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.SortDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import springfox.documentation.annotations.ApiIgnore;

import io.choerodon.core.annotation.Permission;
import io.choerodon.core.enums.ResourceType;
import io.choerodon.core.exception.CommonException;
import io.choerodon.core.iam.InitRoleCode;
import io.choerodon.devops.api.vo.SecretReqVO;
import io.choerodon.devops.api.vo.SecretRespVO;
import io.choerodon.devops.api.vo.SecretUpdateVO;
import io.choerodon.devops.app.service.DevopsSecretService;
import io.choerodon.devops.infra.util.ConvertUtils;
import io.choerodon.swagger.annotation.CustomPageRequest;

/**
 * Created by n!Ck
 * Date: 18-12-4
 * Time: 上午9:14
 * Description:
 */

@RestController
@RequestMapping(value = "/v1/projects/{project_id}/secret")
public class DevopsSecretController {

    private DevopsSecretService devopsSecretService;

    public DevopsSecretController(DevopsSecretService devopsSecretService) {
        this.devopsSecretService = devopsSecretService;
    }

    /**
     * 创建密钥
     *
     * @param secretReqVO 请求体
     * @return SecretRespVO
     */
    @Permission(type = ResourceType.PROJECT,
            roles = {InitRoleCode.PROJECT_OWNER, InitRoleCode.PROJECT_MEMBER})
    @ApiOperation(value = "创建密钥")
    @PostMapping
    public ResponseEntity<SecretRespVO> create(
            @ApiParam(value = "项目id", required = true)
            @PathVariable(value = "project_id") Long projectId,
            @ApiParam(value = "请求体", required = true)
            @RequestBody @Valid SecretReqVO secretReqVO) {
        secretReqVO.setType("create");
        return Optional.ofNullable(devopsSecretService.createOrUpdate(secretReqVO))
                .map(result -> new ResponseEntity<>(result, HttpStatus.OK))
                .orElseThrow(() -> new CommonException("error.secret.create"));
    }

    /**
     * 更新密钥
     *
     * @param secretUpdateVO 请求体
     * @return SecretRespVO
     */
    @Permission(type = ResourceType.PROJECT,
            roles = {InitRoleCode.PROJECT_OWNER, InitRoleCode.PROJECT_MEMBER})
    @ApiOperation(value = "更新密钥")
    @PutMapping
    public ResponseEntity<SecretRespVO> update(
            @ApiParam(value = "项目id", required = true)
            @PathVariable(value = "project_id") Long projectId,
            @ApiParam(value = "请求体", required = true)
            @RequestBody @Valid SecretUpdateVO secretUpdateVO) {
        secretUpdateVO.setType("update");
        return Optional.ofNullable(devopsSecretService.createOrUpdate(ConvertUtils.convertObject(secretUpdateVO, SecretReqVO.class)))
                .map(result -> new ResponseEntity<>(result, HttpStatus.OK))
                .orElseThrow(() -> new CommonException("error.secret.update"));
    }

    /**
     * 删除密钥
     *
     * @param envId    环境id
     * @param secretId 密钥id
     * @return Boolean
     */
    @Permission(type = ResourceType.PROJECT,
            roles = {InitRoleCode.PROJECT_OWNER, InitRoleCode.PROJECT_MEMBER})
    @ApiOperation(value = "删除密钥")
    @DeleteMapping("/{env_id}/{secret_id}")
    public ResponseEntity<Boolean> deleteSecret(
            @ApiParam(value = "项目id", required = true)
            @PathVariable(value = "project_id") Long projectId,
            @ApiParam(value = "环境id", required = true)
            @PathVariable(value = "env_id") Long envId,
            @ApiParam(value = "密钥id", required = true)
            @PathVariable(value = "secret_id") Long secretId) {
        return Optional.ofNullable(devopsSecretService.deleteSecret(envId, secretId))
                .map(result -> new ResponseEntity<>(result, HttpStatus.OK))
                .orElseThrow(() -> new CommonException("error.secret.delete"));
    }

    /**
     * 分页查询secret
     *
     * @param envId        环境id
     * @param pageable     分页参数
     * @param params       查询参数
     * @param appServiceId 服务id
     * @return Page
     */
    @Permission(type = ResourceType.PROJECT,
            roles = {InitRoleCode.PROJECT_OWNER, InitRoleCode.PROJECT_MEMBER})
    @CustomPageRequest
    @ApiOperation(value = "分页查询secret")
    @PostMapping("/page_by_options")
    public ResponseEntity<PageInfo<SecretRespVO>> pageByOption(
            @ApiParam(value = "项目id", required = true)
            @PathVariable(value = "project_id") Long projectId,
            @ApiParam(value = "环境id")
            @RequestParam(value = "env_id", required = false) Long envId,
            @ApiParam(value = "服务id")
            @RequestParam(value = "app_service_id", required = false) Long appServiceId,
            @ApiParam(value = "分页参数")
            @SortDefault(value = "id", direction = Sort.Direction.DESC)
            @ApiIgnore Pageable pageable,
            @ApiParam(value = "是否解码值")
            @RequestParam(value = "to_decode", required = false, defaultValue = "false") boolean toDecode,
            @ApiParam(value = "查询参数")
            @RequestBody(required = false) String params) {
        return Optional.ofNullable(devopsSecretService.pageByOption(envId, pageable, params, appServiceId, toDecode))
                .map(result -> new ResponseEntity<>(result, HttpStatus.OK))
                .orElseThrow(() -> new CommonException("error.secret.list"));
    }

    /**
     * 根据密钥id查询密钥
     *
     * @param secretId 密钥id
     * @return SecretRespVO
     */
    @Permission(type = ResourceType.PROJECT,
            roles = {InitRoleCode.PROJECT_OWNER, InitRoleCode.PROJECT_MEMBER})
    @ApiOperation(value = "根据密钥id查询密钥")
    @GetMapping("/{secret_id}")
    public ResponseEntity<SecretRespVO> querySecret(
            @ApiParam(value = "项目id", required = true)
            @PathVariable(value = "project_id") Long projectId,
            @ApiParam(value = "密钥id", required = true)
            @PathVariable(value = "secret_id") Long secretId,
            @ApiParam(value = "是否解码值")
            @RequestParam(value = "to_decode", required = false, defaultValue = "false") boolean toDecode) {
        return Optional.ofNullable(devopsSecretService.querySecret(secretId, toDecode))
                .map(result -> new ResponseEntity<>(result, HttpStatus.OK))
                .orElseThrow(() -> new CommonException("error.secret.query"));
    }

    /**
     * 校验名字唯一性
     *
     * @param envId      环境id
     * @param secretName 密钥名
     */
    @Permission(type = ResourceType.PROJECT,
            roles = {InitRoleCode.PROJECT_OWNER, InitRoleCode.PROJECT_MEMBER})
    @ApiOperation(value = "校验名字唯一性")
    @GetMapping("/{env_id}/check_name")
    public void checkName(
            @ApiParam(value = "项目id", required = true)
            @PathVariable(value = "project_id") Long projectId,
            @ApiParam(value = "环境id", required = true)
            @PathVariable(value = "env_id") Long envId,
            @ApiParam(value = "密钥名")
            @RequestParam(value = "secret_name") String secretName) {
        devopsSecretService.checkName(envId, secretName);
    }
}
