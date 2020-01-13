package io.choerodon.devops.api.controller.v1;

import io.choerodon.core.annotation.Permission;
import io.choerodon.core.enums.ResourceType;
import io.choerodon.core.iam.InitRoleCode;
import io.choerodon.devops.api.vo.ClusterNodeInfoVO;
import io.choerodon.devops.app.service.DevopsClusterService;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * @author zhaotianxin
 * @since 2019/10/28
 */
@RestController
@RequestMapping(value = "/v1/checks")
public class DevopsCheckClusterController {
    @Autowired
    private DevopsClusterService devopsClusterService;

    @Permission(type = ResourceType.SITE, permissionPublic = true)
    @ApiOperation(value = "验证用户是否拥有操作集群的权限")
    @GetMapping(value = "/clusterCheck")
    public ResponseEntity<Boolean> checkUserClusterPermission(
            @ApiParam(value = "集群ID", required = true)
            @RequestParam(value = "cluster_id") Long clusterId,
            @ApiParam(value = "用户ID", required = true)
            @RequestParam(value = "user_id") Long  userId) {
        return new ResponseEntity<>(devopsClusterService.checkUserClusterPermission(clusterId, userId), HttpStatus.OK);
    }
}
