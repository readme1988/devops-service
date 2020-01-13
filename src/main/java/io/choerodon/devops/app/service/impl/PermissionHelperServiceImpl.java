package io.choerodon.devops.app.service.impl;

import javax.annotation.Nullable;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import io.choerodon.core.exception.CommonException;
import io.choerodon.core.oauth.DetailsHelper;
import io.choerodon.devops.app.service.PermissionHelper;
import io.choerodon.devops.app.service.UserAttrService;
import io.choerodon.devops.infra.dto.UserAttrDTO;
import io.choerodon.devops.infra.feign.operator.BaseServiceClientOperator;

/**
 * @author zmf
 * @since 19-12-30
 */
@Service
public class PermissionHelperServiceImpl implements PermissionHelper {
    @Autowired
    private UserAttrService userAttrService;
    @Autowired
    private BaseServiceClientOperator baseServiceClientOperator;

    @Override
    public boolean isRoot(Long userId) {
        UserAttrDTO result = userAttrService.baseQueryById(userId);
        return result != null && result.getGitlabAdmin();
    }

    @Override
    public boolean isRoot() {
        if (DetailsHelper.getUserDetails() == null || DetailsHelper.getUserDetails().getUserId() == null) {
            return false;
        }
        return isRoot(DetailsHelper.getUserDetails().getUserId());
    }

    @Override
    public boolean isGitlabProjectOwnerOrRoot(Long projectId) {
        Long iamUserId = DetailsHelper.getUserDetails().getUserId();
        return isRoot(iamUserId) || baseServiceClientOperator.isGitlabProjectOwner(iamUserId, projectId);
    }

    @Override
    public boolean isGitlabProjectOwnerOrRoot(Long projectId, Long iamUserId) {
        return isRoot(iamUserId) || baseServiceClientOperator.isGitlabProjectOwner(iamUserId, projectId);
    }

    @Override
    public boolean isGitlabProjectOwnerOrRoot(Long projectId, @Nullable UserAttrDTO userAttrDTO) {
        if (userAttrDTO == null || userAttrDTO.getIamUserId() == null) {
            return false;
        }
        return userAttrDTO.getGitlabAdmin() || baseServiceClientOperator.isGitlabProjectOwner(userAttrDTO.getIamUserId(), projectId);
    }

    @Override
    public void checkProjectOwnerOrRoot(Long projectId, Long iamUserId) {
        if (!isGitlabProjectOwnerOrRoot(projectId, iamUserId)) {
            throw new CommonException("error.user.not.owner");
        }
    }

    @Override
    public void checkProjectOwnerOrRoot(Long projectId, @Nullable UserAttrDTO userAttrDTO) {
        if (!isGitlabProjectOwnerOrRoot(projectId, userAttrDTO)) {
            throw new CommonException("error.user.not.owner");
        }
    }
}
