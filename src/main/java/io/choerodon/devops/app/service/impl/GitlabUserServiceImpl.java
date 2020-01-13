package io.choerodon.devops.app.service.impl;

import java.util.List;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import io.choerodon.devops.api.vo.GitlabUserRequestVO;
import io.choerodon.devops.app.service.GitlabUserService;
import io.choerodon.devops.app.service.UserAttrService;
import io.choerodon.devops.infra.config.GitlabConfigurationProperties;
import io.choerodon.devops.infra.dto.UserAttrDTO;
import io.choerodon.devops.infra.dto.gitlab.GitLabUserDTO;
import io.choerodon.devops.infra.dto.gitlab.GitlabUserReqDTO;
import io.choerodon.devops.infra.feign.operator.GitlabServiceClientOperator;
import io.choerodon.devops.infra.util.ConvertUtils;
import io.choerodon.devops.infra.util.TypeUtil;

/**
 * Created by Zenger on 2018/3/28.
 */
@Service
public class GitlabUserServiceImpl implements GitlabUserService {
    private static final String SERVICE_PATTERN = "[a-zA-Z0-9_\\.][a-zA-Z0-9_\\-\\.]*[a-zA-Z0-9_\\-]|[a-zA-Z0-9_]";

    @Autowired
    private GitlabConfigurationProperties gitlabConfigurationProperties;
    @Autowired
    private UserAttrService userAttrService;
    @Autowired
    private GitlabServiceClientOperator gitlabServiceClientOperator;


    @Override
    public void createGitlabUser(GitlabUserRequestVO gitlabUserReqDTO) {

        checkGitlabUser(gitlabUserReqDTO);
        GitLabUserDTO gitLabUserDTO = gitlabServiceClientOperator.queryUserByUserName(gitlabUserReqDTO.getUsername());
        if (gitLabUserDTO == null) {
            gitLabUserDTO = gitlabServiceClientOperator.createUser(
                    gitlabConfigurationProperties.getPassword(),
                    gitlabConfigurationProperties.getProjectLimit(),
                    ConvertUtils.convertObject(gitlabUserReqDTO, GitlabUserReqDTO.class));
        }
        UserAttrDTO userAttrDTO = userAttrService.baseQueryByGitlabUserId(gitLabUserDTO.getId().longValue());
        if (userAttrDTO == null) {
            userAttrDTO = new UserAttrDTO();
            userAttrDTO.setIamUserId(Long.parseLong(gitlabUserReqDTO.getExternUid()));
            userAttrDTO.setGitlabUserId(gitLabUserDTO.getId().longValue());
            userAttrDTO.setGitlabUserName(gitLabUserDTO.getUsername());
            userAttrService.baseInsert(userAttrDTO);
        }
    }

    @Override
    public void updateGitlabUser(GitlabUserRequestVO gitlabUserReqDTO) {

        checkGitlabUser(gitlabUserReqDTO);
        UserAttrDTO userAttrDTO = userAttrService.baseQueryById(TypeUtil.objToLong(gitlabUserReqDTO.getExternUid()));
        if (userAttrDTO != null) {
            gitlabServiceClientOperator.updateUser(TypeUtil.objToInteger(userAttrDTO.getGitlabUserId()),
                    gitlabConfigurationProperties.getProjectLimit(),
                    ConvertUtils.convertObject(gitlabUserReqDTO, GitlabUserReqDTO.class));
        }
    }

    @Override
    public void isEnabledGitlabUser(Integer userId) {
        UserAttrDTO userAttrDTO = userAttrService.baseQueryById(TypeUtil.objToLong(userId));
        if (userAttrDTO != null) {
            gitlabServiceClientOperator.enableUser(TypeUtil.objToInteger(userAttrDTO.getGitlabUserId()));
        }
    }

    @Override
    public void disEnabledGitlabUser(Integer userId) {
        UserAttrDTO userAttrDTO = userAttrService.baseQueryById(TypeUtil.objToLong(userId));
        if (userAttrDTO != null) {
            gitlabServiceClientOperator.disableUser(TypeUtil.objToInteger(userAttrDTO.getGitlabUserId()));
        }
    }


    private void checkGitlabUser(GitlabUserRequestVO gitlabUserRequestVO) {
        String userName = gitlabUserRequestVO.getUsername();
        StringBuilder newUserName = new StringBuilder();
        for (int i = 0; i < userName.length(); i++) {
            if (!Pattern.matches(SERVICE_PATTERN, String.valueOf(userName.charAt(i)))) {
                newUserName.append("_");
            } else {
                newUserName.append(String.valueOf(userName.charAt(i)));
            }
        }
        gitlabUserRequestVO.setUsername(newUserName.toString());
    }

    @Override
    public Boolean doesEmailExists(String email) {
        return gitlabServiceClientOperator.checkEmail(email);
    }

    @Override
    public void assignAdmins(List<Long> iamUserIds) {
        if (CollectionUtils.isEmpty(iamUserIds)) {
            return;
        }

        iamUserIds.stream()
                .map(iamUserId -> userAttrService.checkUserSync(userAttrService.baseQueryById(iamUserId), iamUserId))
                .forEach(user -> {
                    gitlabServiceClientOperator.assignAdmin(user.getIamUserId(), TypeUtil.objToInteger(user.getGitlabUserId()));
                    userAttrService.updateAdmin(user.getIamUserId(), Boolean.TRUE);
                });
    }

    @Override
    public void deleteAdmin(Long iamUserId) {
        if (iamUserId == null) {
            return;
        }

        UserAttrDTO userAttrDTO = userAttrService.checkUserSync(userAttrService.baseQueryById(iamUserId), iamUserId);
        gitlabServiceClientOperator.deleteAdmin(iamUserId, TypeUtil.objToInteger(userAttrDTO.getGitlabUserId()));
        userAttrService.updateAdmin(iamUserId, Boolean.FALSE);
    }
}
