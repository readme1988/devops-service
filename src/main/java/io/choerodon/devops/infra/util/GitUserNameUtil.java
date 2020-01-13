package io.choerodon.devops.infra.util;

import io.choerodon.core.convertor.ApplicationContextHelper;
import io.choerodon.core.oauth.CustomUserDetails;
import io.choerodon.core.oauth.DetailsHelper;
import io.choerodon.devops.infra.dto.gitlab.GitLabUserDTO;
import io.choerodon.devops.infra.feign.operator.GitlabServiceClientOperator;

public class GitUserNameUtil {

    private GitUserNameUtil() {
    }

    private static int adminId = -1;

    /**
     * 获得gitlab管理员id
     */
    public static int getAdminId() {
        if (adminId == -1) {
            synchronized (GitUserNameUtil.class) {
                if (adminId == -1) {
                    GitlabServiceClientOperator gitlabServiceClientOperator = (GitlabServiceClientOperator) ApplicationContextHelper.getContext().getBean("gitlabServiceClientOperator");
                    GitLabUserDTO gitLabUserDTO = gitlabServiceClientOperator.queryAdminUser();
                    adminId = gitLabUserDTO.getId();
                }
                return adminId;
            }
        } else {
            return adminId;
        }
    }

    /**
     * 获取登录用户名
     *
     * @return username
     */
    public static String getUsername() {
        CustomUserDetails details = DetailsHelper.getUserDetails();
        return getGitlabRealUsername(details.getUsername());
    }

    /**
     * 获取登录用户Id
     *
     * @return userId
     */
    public static Integer getUserId() {
        CustomUserDetails details = DetailsHelper.getUserDetails();
        Long userId = details.getUserId();
        return TypeUtil.objToInteger(userId);
    }

    public static String getEmail(){
        CustomUserDetails details = DetailsHelper.getUserDetails();
        return  details.getEmail();

    }


    /**
     * 获取登录用户名
     *
     * @return username
     */
    public static String getRealUsername() {
        CustomUserDetails details = DetailsHelper.getUserDetails();
        return details.getUsername();
    }

    public static Long getOrganizationId() {
        CustomUserDetails details = DetailsHelper.getUserDetails();
        return details.getOrganizationId();
    }

    private static String getGitlabRealUsername(String username) {
        //现在框架组那边获取UserDetail返回的name是realName,所以暂时用管理员匹配
        if ("admin".equals(username)) {
            return "admin1";
        }
        return username;
    }
}
