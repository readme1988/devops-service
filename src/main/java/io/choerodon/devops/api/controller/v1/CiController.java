package io.choerodon.devops.api.controller.v1;

import java.util.Optional;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import io.choerodon.core.annotation.Permission;
import io.choerodon.devops.app.service.AppServiceService;
import io.choerodon.devops.app.service.AppServiceVersionService;

/**
 * Created by younger on 2018/4/13.
 */

@RestController
@RequestMapping(value = "/ci")
public class CiController {

    private AppServiceService applicationService;
    private AppServiceVersionService appServiceVersionService;

    public CiController(AppServiceService applicationService, AppServiceVersionService appServiceVersionService) {
        this.applicationService = applicationService;
        this.appServiceVersionService = appServiceVersionService;
    }

    /**
     * 服务查询ci脚本文件
     *
     * @param token token
     * @param type  类型
     * @return File
     */
    @Permission(
            permissionPublic = true)
    @ApiOperation(value = "根据应用服务的Token和类型查询某个应用服务用于ci的脚本文件")
    @GetMapping
    public ResponseEntity<String> queryFile(
            @ApiParam(value = "token")
            @RequestParam String token,
            @ApiParam(value = "类型")
            @RequestParam(required = false) String type) {
        return Optional.ofNullable(applicationService.queryFile(token, type))
                .map(target -> new ResponseEntity<>(target, HttpStatus.OK)).orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }


    /**
     * 创建应用服务版本
     *
     * @param image   类型
     * @param token   应用服务的token
     * @param version 版本
     * @param commit  commit
     * @param file    tgz包
     * @return File
     */
    @Permission(permissionPublic = true)
    @ApiOperation(value = "创建应用服务版本")
    @PostMapping
    public ResponseEntity create(
            @ApiParam(value = "image", required = true)
            @RequestParam String image,
            @ApiParam(value = "harbor_config_id", required = true)
            @RequestParam(value = "harbor_config_id") String harborConfigId,
            @ApiParam(value = "token", required = true)
            @RequestParam String token,
            @ApiParam(value = "版本", required = true)
            @RequestParam String version,
            @ApiParam(value = "commit", required = true)
            @RequestParam String commit,
            @ApiParam(value = "taz包", required = true)
            @RequestParam MultipartFile file) {
        appServiceVersionService.create(image, harborConfigId, token, version, commit, file);
        return new ResponseEntity<>(HttpStatus.OK);
    }
}
