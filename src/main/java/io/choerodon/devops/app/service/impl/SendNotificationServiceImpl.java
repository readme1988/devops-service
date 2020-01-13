package io.choerodon.devops.app.service.impl;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.annotation.Nullable;

import io.choerodon.devops.api.vo.DevopsUserPermissionVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import io.choerodon.core.notify.NoticeSendDTO;
import io.choerodon.devops.app.service.*;
import io.choerodon.devops.infra.constant.NoticeCodeConstants;
import io.choerodon.devops.infra.dto.*;
import io.choerodon.devops.infra.dto.iam.IamUserDTO;
import io.choerodon.devops.infra.dto.iam.OrganizationDTO;
import io.choerodon.devops.infra.dto.iam.ProjectDTO;
import io.choerodon.devops.infra.enums.CommandType;
import io.choerodon.devops.infra.enums.EnvironmentType;
import io.choerodon.devops.infra.feign.NotifyClient;
import io.choerodon.devops.infra.feign.operator.BaseServiceClientOperator;
import io.choerodon.devops.infra.mapper.AppServiceMapper;
import io.choerodon.devops.infra.util.ArrayUtil;
import io.choerodon.devops.infra.util.LogUtil;
import io.choerodon.devops.infra.util.TypeUtil;
import io.choerodon.mybatis.autoconfigure.CustomPageRequest;
import org.springframework.transaction.annotation.Transactional;

/**
 * 发送DevOps相关通知的实现类
 * 其中数字类型的参数要转成字符串，否则在notify-service中会被转为逗号分隔的形式，如`11,111` (0.20版本)
 *
 * @author zmf
 * @since 12/5/19
 */
@Service
public class SendNotificationServiceImpl implements SendNotificationService {
    private static final Logger LOGGER = LoggerFactory.getLogger(SendNotificationServiceImpl.class);
    private static final String PROJECT = "Project";
    private static final String ORGANIZATION = "Organization";
    private static final String NOTIFY_TYPE = "devops";

    @Value("${services.gitlab.url}")
    private String gitlabUrl;

    @Autowired
    private NotifyClient notifyClient;
    @Autowired
    private AppServiceService appServiceService;
    @Autowired
    private BaseServiceClientOperator baseServiceClientOperator;
    @Autowired
    private DevopsMergeRequestService devopsMergeRequestService;
    @Autowired
    private AppServiceMapper appServiceMapper;
    @Autowired
    private UserAttrService userAttrService;
    @Autowired
    private DevopsEnvironmentService devopsEnvironmentService;
    @Autowired
    private DevopsEnvCommandService devopsEnvCommandService;

    /**
     * 发送和应用服务失败、启用和停用的消息(调用此方法时注意在外层捕获异常，此方法不保证无异常抛出)
     *
     * @param appServiceId    应用服务id
     * @param sendSettingCode 消息code
     * @param targetSupplier  转换目标用户
     */
    private void sendNoticeAboutAppService(Long appServiceId, String sendSettingCode, Function<AppServiceDTO, List<NoticeSendDTO.User>> targetSupplier) {
        AppServiceDTO appServiceDTO = appServiceService.baseQuery(appServiceId);
        if (appServiceDTO == null) {
            LogUtil.loggerInfoObjectNullWithId("AppService", appServiceId, LOGGER);
            return;
        }
        ProjectDTO projectDTO = baseServiceClientOperator.queryIamProjectById(appServiceDTO.getProjectId());
        if (projectDTO == null) {
            LogUtil.loggerInfoObjectNullWithId(PROJECT, appServiceDTO.getProjectId(), LOGGER);
            return;
        }
        OrganizationDTO organizationDTO = baseServiceClientOperator.queryOrganizationById(projectDTO.getOrganizationId());
        if (organizationDTO == null) {
            LogUtil.loggerInfoObjectNullWithId(ORGANIZATION, projectDTO.getOrganizationId(), LOGGER);
            return;
        }

        List<NoticeSendDTO.User> targetUsers = targetSupplier.apply(appServiceDTO);
        LOGGER.debug("AppService notice {}. Target users size: {}", sendSettingCode, targetUsers.size());

        sendNotices(sendSettingCode, projectDTO.getId(), targetUsers, makeAppServiceParams(organizationDTO.getId(), projectDTO.getId(), projectDTO.getName(), projectDTO.getCategory(), appServiceDTO.getName()));
    }

    private void sendNoticeAboutAppService(AppServiceDTO appServiceDTO, String sendSettingCode, Function<AppServiceDTO, List<NoticeSendDTO.User>> targetSupplier) {
        if (appServiceDTO == null) {
            LogUtil.loggerInfoObjectNullWithId("AppService", null, LOGGER);
            return;
        }
        ProjectDTO projectDTO = baseServiceClientOperator.queryIamProjectById(appServiceDTO.getProjectId());
        if (projectDTO == null) {
            LogUtil.loggerInfoObjectNullWithId(PROJECT, appServiceDTO.getProjectId(), LOGGER);
            return;
        }
        OrganizationDTO organizationDTO = baseServiceClientOperator.queryOrganizationById(projectDTO.getOrganizationId());
        if (organizationDTO == null) {
            LogUtil.loggerInfoObjectNullWithId(ORGANIZATION, projectDTO.getOrganizationId(), LOGGER);
            return;
        }

        List<NoticeSendDTO.User> targetUsers = targetSupplier.apply(appServiceDTO);
        LOGGER.debug("AppService notice {}. Target users size: {}", sendSettingCode, targetUsers.size());

        sendNotices(sendSettingCode, projectDTO.getId(), targetUsers, makeAppServiceParams(organizationDTO.getId(), projectDTO.getId(), projectDTO.getName(), projectDTO.getCategory(), appServiceDTO.getName()));
    }

    /**
     * 应用服务相关模板所需要的参数
     *
     * @param organizationId  组织id
     * @param projectId       项目id
     * @param projectName     项目名称
     * @param projectCategory 项目类别
     * @param appServiceName  应用服务名称
     * @return 参数映射
     */
    private Map<String, Object> makeAppServiceParams(Long organizationId, Long projectId, String projectName, String projectCategory, String appServiceName) {
        Map<String, Object> params = new HashMap<>();
        params.put("organizationId", String.valueOf(organizationId));
        params.put("projectId", String.valueOf(projectId));
        params.put("projectName", projectName);
        params.put("projectCategory", projectCategory);
        params.put("appServiceName", appServiceName);
        return params;
    }

    /**
     * 发送通知
     *
     * @param sendSettingCode 通知code
     * @param sourceId        projectId, organizationId, 0L
     * @param targetUsers     目标用户
     * @param params          参数映射
     */
    public void sendNotices(String sendSettingCode, Long sourceId, List<NoticeSendDTO.User> targetUsers, Map<String, Object> params) {
        notifyClient.sendMessage(constructNotice(sendSettingCode, sourceId, targetUsers, params));
    }

    @Override
    public void sendWhenAppServiceFailure(Long appServiceId) {
        doWithTryCatchAndLog(
                () -> sendNoticeAboutAppService(appServiceId, NoticeCodeConstants.APP_SERVICE_CREATION_FAILED,
                        app -> ArrayUtil.singleAsList(constructTargetUser(app.getCreatedBy()))),
                ex -> LOGGER.info("Error occurred when sending message about failure of app-service. The exception is {}.", ex));
    }

    private static <T> List<T> mapNullListToEmpty(List<T> list) {
        return list == null ? Collections.emptyList() : list;
    }

    @Override
    @Async
    public void sendWhenAppServiceEnabled(Long appServiceId) {
        doWithTryCatchAndLog(
                () -> sendNoticeAboutAppService(appServiceId, NoticeCodeConstants.APP_SERVICE_ENABLED,
                        app -> mapNullListToEmpty(appServiceService.pagePermissionUsers(app.getProjectId(), app.getId(), CustomPageRequest.of(0, 0), null)
                                .getList())
                                .stream()
                                .map(p -> constructTargetUser(p.getIamUserId()))
                                .collect(Collectors.toList())),
                ex -> LOGGER.info("Error occurred when sending message about app-service-enable. The exception is {}.", ex));
    }

    @Override
    @Async
    public void sendWhenAppServiceDisabled(Long appServiceId) {
        doWithTryCatchAndLog(
                () -> sendNoticeAboutAppService(appServiceId, NoticeCodeConstants.APP_SERVICE_DISABLE,
                        app -> mapNullListToEmpty(appServiceService.pagePermissionUsers(app.getProjectId(), app.getId(), CustomPageRequest.of(0, 0), null)
                                .getList())
                                .stream()
                                .map(p -> constructTargetUser(p.getIamUserId()))
                                .collect(Collectors.toList())),
                ex -> LOGGER.info("Error occurred when sending message about app-service-disable. The exception is {}.", ex));
    }

    /**
     * 删除数据消息发送同步执行
     *
     * @param appServiceId
     */
    @Override
    @Async
    public void sendWhenAppServiceDelete(List<DevopsUserPermissionVO> devopsUserPermissionVOS, AppServiceDTO appServiceDTO) {
        doWithTryCatchAndLog(
                () -> sendNoticeAboutAppService(appServiceDTO, NoticeCodeConstants.DELETE_APP_SERVICE,
                        app -> mapNullListToEmpty(devopsUserPermissionVOS)
                                .stream()
                                .map(p -> constructTargetUser(p.getIamUserId()))
                                .collect(Collectors.toList())),
                ex -> LOGGER.info("Error occurred when sending message about app-service-delete. The exception is {}.", ex));
    }


    @Override
    public void sendWhenCDFailure(Long gitlabPipelineId, AppServiceDTO appServiceDTO, String pipelineOperatorUserName) {
        doWithTryCatchAndLog(() -> {
                    if (appServiceDTO == null) {
                        LOGGER.info("Parameter appServiceDTO is null when sending gitlab pipeline failure notice");
                        return;
                    }

                    ProjectDTO projectDTO = baseServiceClientOperator.queryIamProjectById(appServiceDTO.getProjectId());
                    if (projectDTO == null) {
                        LogUtil.loggerInfoObjectNullWithId(PROJECT, appServiceDTO.getProjectId(), LOGGER);
                        return;
                    }

                    OrganizationDTO organizationDTO = baseServiceClientOperator.queryOrganizationById(projectDTO.getOrganizationId());
                    if (organizationDTO == null) {
                        LogUtil.loggerInfoObjectNullWithId(ORGANIZATION, projectDTO.getOrganizationId(), LOGGER);
                        return;
                    }

                    Map<String, Object> params = new HashMap<>();
                    params.put("gitlabUrl", gitlabUrl);
                    params.put("organizationCode", organizationDTO.getCode());
                    params.put("projectCode", projectDTO.getCode());
                    params.put("projectName", projectDTO.getName());
                    params.put("appServiceCode", appServiceDTO.getCode());
                    params.put("appServiceName", appServiceDTO.getName());
                    params.put("gitlabPipelineId", String.valueOf(gitlabPipelineId));

                    IamUserDTO iamUserDTO = baseServiceClientOperator.queryUserByLoginName(pipelineOperatorUserName);

                    sendNotices(NoticeCodeConstants.GITLAB_CONTINUOUS_DELIVERY_FAILURE, projectDTO.getId(), ArrayUtil.singleAsList(constructTargetUser(iamUserDTO.getId())), params);
                },
                ex -> LOGGER.info("Error occurred when sending message about gitlab-pipeline-failure. The exception is {}.", ex));
    }

    /**
     * 构造merge request审核，关闭和合并三个事件的所需参数
     *
     * @param gitlabUrl        gitlab的http(s)地址
     * @param organizationCode 组织code
     * @param projectCode      项目code
     * @param projectName      项目名称
     * @param appServiceCode   应用服务code
     * @param appServiceName   应用服务名称
     * @param realName         对于审核merge request的消息，是合并请求提交者的名称；
     *                         对于merge request关闭和合并的事件，是merge request提出者的名称
     * @param mergeRequestId   合并请求的id
     */
    private Map<String, Object> makeMergeRequestEventParams(String gitlabUrl,
                                                            String organizationCode,
                                                            String projectCode,
                                                            String projectName,
                                                            String appServiceCode,
                                                            String appServiceName,
                                                            String realName,
                                                            Long mergeRequestId) {
        Map<String, Object> params = new HashMap<>();
        params.put("gitlabUrl", gitlabUrl);
        params.put("organizationCode", organizationCode);
        params.put("projectCode", projectCode);
        params.put("projectName", projectName);
        params.put("appServiceCode", appServiceCode);
        params.put("appServiceName", appServiceName);
        params.put("realName", realName);
        params.put("mergeRequestId", String.valueOf(mergeRequestId));
        return params;
    }

    @Override
    public void sendWhenMergeRequestAuditEvent(Integer gitlabProjectId, Long mergeRequestId) {
        doWithTryCatchAndLog(
                () -> {
                    DevopsMergeRequestDTO devopsMergeRequestDTO = devopsMergeRequestService.baseQueryByAppIdAndMergeRequestId(TypeUtil.objToLong(gitlabProjectId), mergeRequestId);
                    if (devopsMergeRequestDTO == null) {
                        LOGGER.info("Merge Request with id {} and gitlab project id {} is null", mergeRequestId, gitlabProjectId);
                        return;
                    }

                    // 如果没有指定审核人，不发送通知
                    if (devopsMergeRequestDTO.getAssigneeId() == null) {
                        LOGGER.info("Abort sending merge request (gitlab project id: {}, gitlab merge request id : {}) audit notification due to the null assigneeId", gitlabProjectId, mergeRequestId);
                        return;
                    }

                    // merge request的发起者
                    UserAttrDTO author = userAttrService.baseQueryByGitlabUserId(devopsMergeRequestDTO.getAuthorId());
                    if (author == null) {
                        LOGGER.info("DevopsUser with gitlab user id {} is null.", devopsMergeRequestDTO.getAuthorId());
                        return;
                    }

                    IamUserDTO authorUser = baseServiceClientOperator.queryUserByUserId(author.getIamUserId());
                    if (authorUser == null) {
                        LogUtil.loggerInfoObjectNullWithId("IamUser", author.getIamUserId(), LOGGER);
                        return;
                    }

                    UserAttrDTO userAttrDTO = userAttrService.baseQueryByGitlabUserId(devopsMergeRequestDTO.getAssigneeId());
                    if (userAttrDTO == null) {
                        LOGGER.info("DevopsUser with gitlab user id {} is null.", devopsMergeRequestDTO.getAssigneeId());
                        return;
                    }

                    IamUserDTO iamUserDTO = baseServiceClientOperator.queryUserByUserId(userAttrDTO.getIamUserId());
                    if (iamUserDTO == null) {
                        LogUtil.loggerInfoObjectNullWithId("IamUser", userAttrDTO.getIamUserId(), LOGGER);
                        return;
                    }

                    AppServiceDTO appServiceDTO = queryAppServiceByGitlabProjectId(TypeUtil.objToInteger(gitlabProjectId));
                    if (appServiceDTO == null) {
                        LOGGER.info("AppService is null with gitlab project id {}", gitlabProjectId);
                        return;
                    }

                    ProjectDTO projectDTO = baseServiceClientOperator.queryIamProjectById(appServiceDTO.getProjectId());
                    if (projectDTO == null) {
                        LogUtil.loggerInfoObjectNullWithId(PROJECT, appServiceDTO.getProjectId(), LOGGER);
                        return;
                    }

                    OrganizationDTO organizationDTO = baseServiceClientOperator.queryOrganizationById(projectDTO.getOrganizationId());
                    if (organizationDTO == null) {
                        LogUtil.loggerInfoObjectNullWithId(ORGANIZATION, projectDTO.getOrganizationId(), LOGGER);
                        return;
                    }

                    Map<String, Object> params = makeMergeRequestEventParams(gitlabUrl, organizationDTO.getCode(), projectDTO.getCode(), projectDTO.getName(), appServiceDTO.getCode(), appServiceDTO.getName(), authorUser.getRealName(), mergeRequestId);

                    sendNotices(NoticeCodeConstants.AUDIT_MERGE_REQUEST, projectDTO.getId(), ArrayUtil.singleAsList(constructTargetUser(iamUserDTO.getId())), params);
                },
                ex -> LOGGER.info("Error occurred when sending message about merge-request-audit. The exception is {}.", ex));
    }

    private AppServiceDTO queryAppServiceByGitlabProjectId(Integer gitlabProjectId) {
        AppServiceDTO appServiceDTO = new AppServiceDTO();
        appServiceDTO.setGitlabProjectId(Objects.requireNonNull(gitlabProjectId));
        return appServiceMapper.selectOne(appServiceDTO);
    }

    /**
     * 当merge request关闭或者合并时发送消息
     *
     * @param sendSettingCode 发送消息的code
     * @param gitlabProjectId merge request 所属gitlab项目id
     * @param mergeRequestId  merge request id
     */
    private void doSendWhenMergeRequestClosedOrMerged(String sendSettingCode, Integer gitlabProjectId, Long mergeRequestId, String userIdFromGitlab) {
        doWithTryCatchAndLog(
                () -> {
                    DevopsMergeRequestDTO devopsMergeRequestDTO = devopsMergeRequestService.baseQueryByAppIdAndMergeRequestId(TypeUtil.objToLong(gitlabProjectId), mergeRequestId);
                    if (devopsMergeRequestDTO == null) {
                        LOGGER.info("Merge Request with id {} and gitlab project id {} is null", mergeRequestId, gitlabProjectId);
                        return;
                    }

                    // merge request的操作者
                    IamUserDTO authorUser = baseServiceClientOperator.queryUserByLoginName(TypeUtil.objToString(userIdFromGitlab));
                    if (authorUser == null) {
                        LogUtil.loggerInfoObjectNullWithId("IamUser", TypeUtil.objToLong(userIdFromGitlab), LOGGER);
                        return;
                    }

                    // merge request的接收者
                    UserAttrDTO userAttrDTO = userAttrService.baseQueryByGitlabUserId(devopsMergeRequestDTO.getAuthorId());
                    if (userAttrDTO == null) {
                        LOGGER.info("DevopsUser with gitlab user id {} is null.", devopsMergeRequestDTO.getAuthorId());
                        return;
                    }

                    IamUserDTO iamUserDTO = baseServiceClientOperator.queryUserByUserId(userAttrDTO.getIamUserId());
                    if (iamUserDTO == null) {
                        LogUtil.loggerInfoObjectNullWithId("IamUser", userAttrDTO.getIamUserId(), LOGGER);
                        return;
                    }

                    AppServiceDTO appServiceDTO = queryAppServiceByGitlabProjectId(TypeUtil.objToInteger(gitlabProjectId));
                    if (appServiceDTO == null) {
                        LOGGER.info("AppService is null with gitlab project id {}", gitlabProjectId);
                        return;
                    }

                    ProjectDTO projectDTO = baseServiceClientOperator.queryIamProjectById(appServiceDTO.getProjectId());
                    if (projectDTO == null) {
                        LogUtil.loggerInfoObjectNullWithId(PROJECT, appServiceDTO.getProjectId(), LOGGER);
                        return;
                    }

                    OrganizationDTO organizationDTO = baseServiceClientOperator.queryOrganizationById(projectDTO.getOrganizationId());
                    if (organizationDTO == null) {
                        LogUtil.loggerInfoObjectNullWithId(ORGANIZATION, projectDTO.getOrganizationId(), LOGGER);
                        return;
                    }

                    Map<String, Object> params = makeMergeRequestEventParams(gitlabUrl, organizationDTO.getCode(), projectDTO.getCode(), projectDTO.getName(), appServiceDTO.getCode(), appServiceDTO.getName(), authorUser.getRealName(), mergeRequestId);

                    sendNotices(sendSettingCode, projectDTO.getId(), ArrayUtil.singleAsList(constructTargetUser(iamUserDTO.getId())), params);
                },
                ex -> LOGGER.info("Error occurred when sending message about {}. The exception is {}.", sendSettingCode, ex));
    }

    @Override
    public void sendWhenMergeRequestClosed(Integer gitlabProjectId, Long mergeRequestId, String userLoginName) {
        doSendWhenMergeRequestClosedOrMerged(NoticeCodeConstants.MERGE_REQUEST_CLOSED, gitlabProjectId, mergeRequestId, userLoginName);
    }


    @Override
    public void sendWhenMergeRequestPassed(Integer gitlabProjectId, Long mergeRequestId, String userLoginName) {
        doSendWhenMergeRequestClosedOrMerged(NoticeCodeConstants.MERGE_REQUEST_PASSED, gitlabProjectId, mergeRequestId, userLoginName);
    }

    /**
     * 发送资源创建相关的失败通知
     *
     * @param sendSettingCode   通知的code
     * @param envId             环境的id
     * @param resourceName      资源的名称
     * @param creatorId         创建者的id
     * @param resourceCommandId 资源commandId用于判断资源是否是在创建时失败的
     */
    private void doSendWhenResourceCreationFailure(String sendSettingCode, Long envId, String resourceName, Long creatorId, @Nullable Long resourceCommandId) {
        DevopsEnvironmentDTO devopsEnvironmentDTO = devopsEnvironmentService.baseQueryById(envId);

        // 校验资源是否是创建时失败
        if (resourceCommandId != null) {
            DevopsEnvCommandDTO devopsEnvCommandDTO = devopsEnvCommandService.baseQuery(resourceCommandId);
            if (devopsEnvCommandDTO == null) {
                LogUtil.loggerInfoObjectNullWithId("DevOpsEnvCommand", resourceCommandId, LOGGER);
                return;
            } else {
                if (!CommandType.CREATE.getType().equals(devopsEnvCommandDTO.getCommandType())) {
                    LOGGER.debug("Resource {} with name {} failed after updating instead of creating.", devopsEnvCommandDTO.getObject(), resourceName);
                    return;
                }
            }
        }

        if (devopsEnvironmentDTO == null) {
            LogUtil.loggerInfoObjectNullWithId("Environment", envId, LOGGER);
            return;
        }

        // 系统环境的实例失败不发送通知
        if (EnvironmentType.SYSTEM.getValue().equals(devopsEnvironmentDTO.getType())) {
            return;
        }

        ProjectDTO projectDTO = baseServiceClientOperator.queryIamProjectById(devopsEnvironmentDTO.getProjectId());
        if (projectDTO == null) {
            LogUtil.loggerInfoObjectNullWithId(PROJECT, devopsEnvironmentDTO.getProjectId(), LOGGER);
            return;
        }

        Map<String, Object> params = new HashMap<>();
        params.put("projectName", Objects.requireNonNull(projectDTO.getName()));
        params.put("envName", Objects.requireNonNull(devopsEnvironmentDTO.getName()));
        params.put("resourceName", Objects.requireNonNull(resourceName));

        sendNotices(sendSettingCode, projectDTO.getId(), ArrayUtil.singleAsList(constructTargetUser(Objects.requireNonNull(creatorId))), params);
    }

    @Override
    public void sendWhenInstanceCreationFailure(Long envId, String resourceName, Long creatorId, Long resourceCommandId) {
        doSendWhenResourceCreationFailure(NoticeCodeConstants.INSTANCE_CREATION_FAILURE, envId, resourceName, creatorId, resourceCommandId);
    }

    @Override
    public void sendWhenServiceCreationFailure(Long envId, String resourceName, Long creatorId, Long resourceCommandId) {
        doSendWhenResourceCreationFailure(NoticeCodeConstants.SERVICE_CREATION_FAILURE, envId, resourceName, creatorId, resourceCommandId);
    }

    @Override
    public void sendWhenIngressCreationFailure(Long envId, String resourceName, Long creatorId, Long resourceCommandId) {
        doSendWhenResourceCreationFailure(NoticeCodeConstants.INGRESS_CREATION_FAILURE, envId, resourceName, creatorId, resourceCommandId);
    }

    @Override
    public void sendWhenCertificationCreationFailure(Long envId, String resourceName, Long creatorId, Long resourceCommandId) {
        doSendWhenResourceCreationFailure(NoticeCodeConstants.CERTIFICATION_CREATION_FAILURE, envId, resourceName, creatorId, resourceCommandId);
    }


    /**
     * 保证在执行逻辑时不抛出异常的包装方法
     *
     * @param actionInTry   正常处理的逻辑
     * @param actionInCatch 处理异常的逻辑
     */
    private static void doWithTryCatchAndLog(Runnable actionInTry, Consumer<Exception> actionInCatch) {
        if (actionInTry == null) {
            LOGGER.info("Internal fault: parameter actionInTry is unexpectedly null. Action abort.");
            return;
        }
        if (actionInCatch == null) {
            LOGGER.info("Internal fault: parameter actionInCatch is unexpectedly null. Action abort.");
            return;
        }

        try {
            actionInTry.run();
        } catch (Exception ex) {
            try {
                actionInCatch.accept(ex);
            } catch (Exception e) {
                LOGGER.info("Exception occurred in actionInCatch.accept...");
            }
        }
    }

    private static NoticeSendDTO.User constructTargetUser(Long id) {
        NoticeSendDTO.User targetUser = new NoticeSendDTO.User();
        targetUser.setId(id);
        return targetUser;
    }

    private static NoticeSendDTO constructNotice(String sendSettingCode, Long sourceId, List<NoticeSendDTO.User> targetUsers, Map<String, Object> params) {
        NoticeSendDTO noticeSendDTO = new NoticeSendDTO();
        noticeSendDTO.setCode(Objects.requireNonNull(sendSettingCode));
        noticeSendDTO.setSourceId(Objects.requireNonNull(sourceId));
        noticeSendDTO.setTargetUsers(Objects.requireNonNull(targetUsers));
        noticeSendDTO.setParams(Objects.requireNonNull(params));
        noticeSendDTO.setNotifyType(NOTIFY_TYPE);
        return noticeSendDTO;
    }
}
