package io.choerodon.devops.api.controller.v1;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

import io.choerodon.core.annotation.Permission;
import io.choerodon.devops.app.service.AppServiceInstanceService;
import io.choerodon.devops.app.service.GitlabWebHookService;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/webhook")
public class GitlabWebHookController {

    @Autowired
    private GitlabWebHookService gitlabWebHookService;
    @Autowired
    private AppServiceInstanceService appServiceInstanceService;

    @Permission(permissionPublic = true)
    @ApiOperation(value = "webhook转发")
    @PostMapping
    public ResponseEntity forwardGitlabWebHook(HttpServletRequest httpServletRequest, @RequestBody String body) {
        gitlabWebHookService.forwardingEventToPortal(body, httpServletRequest.getHeader("X-Gitlab-Token"));
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @Permission(permissionPublic = true)
    @ApiOperation(value = "gitops webhook转发")
    @PostMapping(value = "/git_ops")
    public ResponseEntity gitOpsWebHook(HttpServletRequest httpServletRequest, @RequestBody String body) {
        gitlabWebHookService.gitOpsWebHook(body, httpServletRequest.getHeader("X-Gitlab-Token"));
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @ApiOperation(value = "查询自动化测试应用实例状态")
    @Permission(permissionPublic = true)
    @PostMapping("/get_test_status")
    public void getTestStatus(
            @ApiParam(value = "releaseName", required = true)
            @RequestBody Map<Long, List<String>> testReleases) {
        appServiceInstanceService.getTestAppStatus(testReleases);
    }
}
