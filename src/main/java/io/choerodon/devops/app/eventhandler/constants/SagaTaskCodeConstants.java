package io.choerodon.devops.app.eventhandler.constants;

/**
 * 此类放sagaTaskCode常量
 * Created by Sheep on 2019/7/16.
 */


public class SagaTaskCodeConstants {
    /**
     * devops创建环境
     */
    public static final String DEVOPS_CREATE_ENV = "devopsCreateEnv";

    /**
     * 环境创建失败
     */
    public static final String DEVOPS_CREATE_ENV_ERROR = "devopsCreateEnvError";

    /**
     * gitops事件处理
     */
    public static final String DEVOPS_GIT_OPS = "devopsGitOps";

    /**
     * 创建gitlab项目
     */
    public static final String DEVOPS_CREATE_APPLICATION_SERVICE = "devopsCreateApplicationService";

    /**
     * Devops从外部代码平台导入到gitlab项目
     */
    public static final String DEVOPS_CREATE_GITLAB_PROJECT = "devopsCreateGitlabProject";

    /**
     * GitOps 用户权限分配处理
     */
    public static final String DEVOPS_UPDATE_GITLAB_USERS = "devopsUpdateGitlabUsers";

    /**
     * GitOps 应用创建失败处理
     */
    public static final String DEVOPS_CREATE_GITLAB_PROJECT_ERROR = "devopsCreateGitlabProjectErr";

    /**
     * GitOps应用模板创建失败处理
     */
    public static final String DEVOPS_CREATE_GITLAB_PROJECT_TEMPLATE_ERROR = "devopsCreateGitlabProjectTemplateErr";

    /**
     * 模板事件处理
     */
    public static final String DEVOPS_OPERATION_GITLAB_TEMPLATE_PROJECT = "devopsOperationGitlabTemplateProject";

    /**
     * gitlab pipeline事件
     */
    public static final String DEVOPS_GITLAB_PIPELINE = "devopsGitlabPipeline";

    /**
     * 创建流水线自动部署实例
     */
    public static final String DEVOPS_PIPELINE_CREATE_INSTANCE = "devops-pipeline-create-instance";

    /**
     * devops创建分支
     */
    public static final String DEVOPS_CREATE_BRANCH = "devopsCreateBranch";

    /**
     * devops创建实例
     */
    public static final String DEVOPS_CREATE_INSTANCE = "devopsCreateInstance";

    /**
     * devops创建网络
     */
    public static final String DEVOPS_CREATE_SERVICE = "devopsCreateService";

    /**
     * devops创建域名
     */
    public static final String DEVOPS_CREATE_INGRESS = "devopsCreateIngress";

    /**
     * devops创建PVC
     */
    public static final String DEVOPS_CREATE_PERSISTENTVOLUMECLAIM = "devopsCreatePersistentVolumeClaim";

    /**
     * devops创建PV
     */
    public static final String DEVOPS_CREATE_PERSISTENTVOLUME = "devopsCreatePersistentVolume";

    /**
     * 初始化Demo环境的项目相关数据
     */
    public static final String REGISTER_DEVOPS_INIT_DEMO_DATA = "register-devops-init-demo-data";

    /**
     * 创建对应项目的两个gitlab组
     */
    public static final String REGISTER_DEVOPS_INIT_PROJCET = "register-devops-init-projcet";

    /**
     * devops 创建 GitLab Group
     */
    public static final String DEVOPS_CREATE_GITLAB_GROUP = "devopsCreateGitLabGroup";

    /**
     * devops  更新 GitLab Group
     */
    public static final String DEVOPS_UPDATE_GITLAB_GROUP = "devopsUpdateGitLabGroup";


    /**
     * devops 创建 Harbor
     */
    public static final String DEVOPS_CREATE_HARBOR = "devopsCreateHarbor";

    /**
     * 创建组织事件
     */
    public static final String DEVOPS_CREATE_ORGANIZATION = "devopsCreateOrganization";

    /**
     * 创建应用事件
     */
    public static final String IAM_CREATE_APPLICATION = "iamCreateApplication";

    /**
     * Iam删除应用
     */
    public static final String IAM_DELETE_APPLICATION = "IamDeleteApplication";

    /**
     * Iam更新应用事件
     */
    public static final String IAM_UPDATE_APPLICATION = "iamUpdateApplication";

    /**
     * Iam启用应用事件
     */
    public static final String IAM_ENABLE_APPLICATION = "iamEnableApplication";

    /**
     * Iam停用应用事件
     */
    public static final String IAM_DISABLE_APPLICATION = "iamDisableApplication";

    /**
     * 更新角色同步事件
     */
    public static final String IAM_UPDATE_MEMBER_ROLE = "devopsUpdateMemberRole";

    /**
     * 删除角色同步事件
     */
    public static final String IAM_DELETE_MEMBER_ROLE = "devopsDeleteMemberRole";

    /**
     * 创建用户
     */
    public static final String IAM_CREATE_USER = "devopsCreateUser";

    /**
     * 更新用户
     */
    public static final String IAM_UPDATE_USER = "devopsUpdateUser";

    /**
     * 启用用户
     */
    public static final String IAM_ENABLE_USER = "devopsEnableUser";

    /**
     * 禁用用户
     */
    public static final String IAM_DISABLE_USER = "devopsDisableUser";

    /**
     * 应用上传
     */
    public static final String APIM_UPLOAD_APP = "apimUploadApplication";

    /**
     * 应用上传，修复版本
     */
    public static final String APIM_UPLOAD_APP_FIX_VERSION = "apimUploadApplicationFixVersion";

    /**
     * 应用下载
     */
    public static final String APIM_DOWNLOAD_APP = "apimDownloadApplication";


    /**
     * 在gitlab更新环境的权限
     */
    public static final String DEVOPS_UPDATE_ENV_PERMISSION = "devops-update-env-permission";


    /**
     * devops导入内部应用服务
     */
    public static final String DEVOPS_IMPORT_INTERNAL_APPLICATION_SERVICE = "devopsImportInternalAppService";
    /**
     * 删除环境
     */
    public static final String DEVOPS_DELETE_ENV = "devops-delete-env";
    /**
     * devops删除应用服务
     */
    public static final String DEVOPS_APP_DELETE = "devops-delete-app-service";

    /**
     * 应用市场下载失败删除gitlab相关项目
     */
    public static final String DEVOPS_MARKET_DELETE_GITLAB_PRO = "devops-market-failed-delete-gitlab-pro";

    /**
     * DevOps消费添加admin用户事件
     */
    public static final String DEVOPS_ADD_ADMIN = "devops-add-admin";

    /**
     * DevOps消费删除admin用户U事件
     */
    public static final String DEVOPS_DELETE_ADMIN = "devops-delete-admin";


    private SagaTaskCodeConstants() {
    }
}