package io.choerodon.devops.infra.constant;

/**
 * 和GitOps有关的常量
 *
 * @author zmf
 * @since 10/28/19
 */
public class GitOpsConstants {
    private GitOpsConstants() {
    }

    /**
     * GitLab组的名称和path格式
     */
    public static final String GITLAB_GROUP_NAME_FORMAT = "%s-%s%s";
    public static final String APP_SERVICE_SUFFIX = "";
    public static final String ENV_GROUP_SUFFIX = "-gitops";
    /**
     * 集群环境库的组 ${orgCode}_${projectCode}-cluster_gitops
     * 这是集群环境库组的后缀
     */
    public static final String CLUSTER_ENV_GROUP_SUFFIX = "-cluster_gitops";

    /**
     * gitlab环境库的webhook url相对路径
     */
    public static final String GITOPS_WEBHOOK_RELATIVE_URL = "devops/webhook/git_ops";

    /**
     * choerodon系统配置库的项目名格式为: clusterCode-envCode
     */
    public static final String SYSTEM_ENV_GITLAB_PROJECT_CODE_FORMAT = "%s-%s";

    public static final String MASTER = "master";

    /**
     * local path to store env
     * gitops/${orgCode}/${proCode}/${clusterCode}/${envCode}
     */
    public static final String LOCAL_ENV_PATH = "gitops/%s/%s/%s/%s";

    public static final String YAML_FILE_SUFFIX = ".yaml";

    /**
     * release文件对应的gitlab文件前缀
     */
    public static final String RELEASE_PREFIX = "release-";

    /**
     * 系统环境code
     */
    public static final String SYSTEM_NAMESPACE = "choerodon";

    /**
     * 0.20版本之前用于实现实例类型网络的注解键值
     */
    public static final String SERVICE_INSTANCE_ANNOTATION_KEY = "choerodon.io/network-service-instances";

    /**
     * 分支删除时的after字段会是这个值
     */
    public static final String NO_COMMIT_SHA = "0000000000000000000000000000000000000000";

    public static final String MASTER_REF = "refs/heads/master";
}
