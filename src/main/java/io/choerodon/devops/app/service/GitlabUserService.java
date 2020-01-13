package io.choerodon.devops.app.service;

import java.util.List;

import io.choerodon.devops.api.vo.GitlabUserRequestVO;

/**
 * Created by Zenger on 2018/3/28.
 */
public interface GitlabUserService {

    void createGitlabUser(GitlabUserRequestVO gitlabUserReqDTO);

    void updateGitlabUser(GitlabUserRequestVO gitlabUserReqDTO);

    void isEnabledGitlabUser(Integer userId);

    void disEnabledGitlabUser(Integer userId);

    Boolean doesEmailExists(String email);

    void assignAdmins(List<Long> iamUserIds);

    void deleteAdmin(Long iamUserId);
}
