package io.choerodon.devops.app.service.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import io.kubernetes.client.models.V1Endpoints;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import io.choerodon.core.exception.CommonException;
import io.choerodon.devops.api.vo.AppServiceDeployVO;
import io.choerodon.devops.api.vo.AppServiceInstanceVO;
import io.choerodon.devops.api.vo.kubernetes.C7nHelmRelease;
import io.choerodon.devops.api.vo.kubernetes.InstanceValueVO;
import io.choerodon.devops.app.service.*;
import io.choerodon.devops.infra.dto.*;
import io.choerodon.devops.infra.dto.iam.OrganizationDTO;
import io.choerodon.devops.infra.dto.iam.ProjectDTO;
import io.choerodon.devops.infra.enums.GitOpsObjectError;
import io.choerodon.devops.infra.enums.ObjectType;
import io.choerodon.devops.infra.exception.GitOpsExplainException;
import io.choerodon.devops.infra.feign.operator.BaseServiceClientOperator;
import io.choerodon.devops.infra.util.GitOpsUtil;
import io.choerodon.devops.infra.util.ComponentVersionUtil;
import io.choerodon.devops.infra.util.GitUtil;
import io.choerodon.devops.infra.util.TypeUtil;


@Service
public class HandlerC7nReleaseRelationsServiceImpl implements HandlerObjectFileRelationsService<C7nHelmRelease> {
    private static final Logger LOGGER = LoggerFactory.getLogger(HandlerC7nReleaseRelationsServiceImpl.class);

    private static final String C7N_HELM_RELEASE = "C7NHelmRelease";
    private static final String GIT_SUFFIX = "/.git";
    private static final String COMPARE_VALUES = "{}";
    @Autowired
    private DevopsEnvCommandService devopsEnvCommandService;
    @Autowired
    private AppServiceInstanceService appServiceInstanceService;
    @Autowired
    private BaseServiceClientOperator baseServiceClientOperator;
    @Autowired
    private AgentMsgHandlerService agentMsgHandlerService;
    @Autowired
    private AppServiceVersionService appServiceVersionService;
    @Autowired
    private DevopsEnvFileResourceService devopsEnvFileResourceService;
    @Autowired
    private DevopsServiceInstanceService devopsServiceInstanceService;
    @Autowired
    private DevopsEnvironmentService devopsEnvironmentService;

    @Override
    public void handlerRelations(Map<String, String> objectPath, List<DevopsEnvFileResourceDTO> beforeSync, List<C7nHelmRelease> c7nHelmReleases, List<V1Endpoints> v1Endpoints, Long envId, Long projectId, String path, Long userId) {
        List<String> beforeC7nRelease = beforeSync.stream()
                .filter(devopsEnvFileResourceDTO -> devopsEnvFileResourceDTO.getResourceType().equals(C7N_HELM_RELEASE))
                .map(devopsEnvFileResourceDTO -> {
                    AppServiceInstanceDTO appServiceInstanceDTO = appServiceInstanceService
                            .baseQuery(devopsEnvFileResourceDTO.getResourceId());
                    if (appServiceInstanceDTO == null) {
                        devopsEnvFileResourceService
                                .baseDeleteByEnvIdAndResourceId(envId, devopsEnvFileResourceDTO.getResourceId(), C7N_HELM_RELEASE);
                        return null;
                    }
                    return appServiceInstanceDTO.getCode();
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        //比较已存在实例和新增要处理的实例,获取新增实例，更新实例，删除实例
        List<C7nHelmRelease> addC7nHelmRelease = new ArrayList<>();
        List<C7nHelmRelease> updateC7nHelmRelease = new ArrayList<>();
        c7nHelmReleases.forEach(c7nHelmRelease -> {
            if (beforeC7nRelease.contains(c7nHelmRelease.getMetadata().getName())) {
                updateC7nHelmRelease.add(c7nHelmRelease);
                beforeC7nRelease.remove(c7nHelmRelease.getMetadata().getName());
            } else {
                addC7nHelmRelease.add(c7nHelmRelease);
            }
        });

        //新增instance
        addC7nHelmRelease(objectPath, envId, projectId, addC7nHelmRelease, path, userId);
        //更新instance
        updateC7nHelmRelease(objectPath, envId, projectId, updateC7nHelmRelease, path, userId);
        //删除instance,和文件对象关联关系
        beforeC7nRelease.forEach(releaseName -> {
            AppServiceInstanceDTO appServiceInstanceDTO = appServiceInstanceService.baseQueryByCodeAndEnv(releaseName, envId);
            if (appServiceInstanceDTO != null) {
                appServiceInstanceService.instanceDeleteByGitOps(appServiceInstanceDTO.getId());
                devopsEnvFileResourceService
                        .baseDeleteByEnvIdAndResourceId(envId, appServiceInstanceDTO.getId(), C7N_HELM_RELEASE);
            }
        });
    }

    @Override
    public Class<C7nHelmRelease> getTarget() {
        return C7nHelmRelease.class;
    }


    private void updateC7nHelmRelease(Map<String, String> objectPath, Long envId, Long projectId, List<C7nHelmRelease> updateC7nHelmRelease, String path, Long userId) {
        updateC7nHelmRelease.forEach(c7nHelmRelease -> {
                    String filePath = "";
                    try {
                        filePath = objectPath.get(TypeUtil.objToString(c7nHelmRelease.hashCode()));
                        //初始化实例参数,更新时判断实例是否真的修改，没有修改则直接更新文件关联关系
                        AppServiceDeployVO appServiceDeployVO = getApplicationDeployDTO(
                                c7nHelmRelease,
                                projectId,
                                envId,
                                filePath,
                                "update");
                        if (appServiceDeployVO == null) {
                            return;
                        }
                        DevopsEnvCommandDTO devopsEnvCommandDTO = devopsEnvCommandService.baseQuery(appServiceDeployVO.getCommandId());
                        if (!appServiceDeployVO.getIsNotChange()) {
                            AppServiceInstanceVO appServiceInstanceVO = appServiceInstanceService
                                    .createOrUpdateByGitOps(appServiceDeployVO, userId);
                            devopsEnvCommandDTO = devopsEnvCommandService.baseQuery(appServiceInstanceVO.getCommandId());
                        }

                        devopsEnvCommandDTO.setSha(GitUtil.getFileLatestCommit(path + GIT_SUFFIX, filePath));
                        devopsEnvCommandService.baseUpdateSha(devopsEnvCommandDTO.getId(), devopsEnvCommandDTO.getSha());
                        DevopsEnvFileResourceDTO devopsEnvFileResourceDTO = devopsEnvFileResourceService
                                .baseQueryByEnvIdAndResourceId(envId, appServiceDeployVO.getInstanceId(), c7nHelmRelease.getKind());
                        devopsEnvFileResourceService.updateOrCreateFileResource(objectPath, envId,
                                devopsEnvFileResourceDTO,
                                c7nHelmRelease.hashCode(), appServiceDeployVO.getInstanceId(),
                                c7nHelmRelease.getKind());
                    } catch (GitOpsExplainException ex) {
                        throw ex;
                    } catch (CommonException e) {
                        throw new GitOpsExplainException(e.getMessage(), filePath, "", e.getParameters());
                    }
                }
        );
    }

    private void addC7nHelmRelease(Map<String, String> objectPath, Long envId, Long projectId, List<C7nHelmRelease> addC7nHelmRelease, String path, Long userId) {
        addC7nHelmRelease.forEach(c7nHelmRelease -> {
            String filePath = "";
            try {
                filePath = objectPath.get(TypeUtil.objToString(c7nHelmRelease.hashCode()));
                AppServiceInstanceDTO appServiceInstanceDTO = appServiceInstanceService
                        .baseQueryByCodeAndEnv(c7nHelmRelease.getMetadata().getName(), envId);
                AppServiceDeployVO appServiceDeployVO;

                AppServiceInstanceVO appServiceInstanceVO = new AppServiceInstanceVO();
                //初始化实例参数,创建时判断实例是否存在，存在则直接创建文件对象关联关系
                if (appServiceInstanceDTO == null) {
                    appServiceDeployVO = getApplicationDeployDTO(
                            c7nHelmRelease,
                            projectId,
                            envId,
                            filePath,
                            "create");
                    appServiceInstanceVO = appServiceInstanceService.createOrUpdateByGitOps(appServiceDeployVO, userId);
                } else {
                    appServiceInstanceVO.setId(appServiceInstanceDTO.getId());
                    appServiceInstanceVO.setCommandId(appServiceInstanceDTO.getCommandId());
                }
                DevopsEnvCommandDTO devopsEnvCommandDTO = devopsEnvCommandService.baseQuery(appServiceInstanceVO.getCommandId());


                List<DevopsServiceInstanceDTO> devopsServiceInstanceDTOS = devopsServiceInstanceService.baseListByEnvIdAndInstanceCode(envId, c7nHelmRelease.getMetadata().getName());

                //删除实例之后，重新创建同名的实例，如果之前的实例关联的网络，此时需要把网络关联上新的实例
                Long instanceId = appServiceInstanceVO.getId();
                if (devopsServiceInstanceDTOS != null && !devopsServiceInstanceDTOS.isEmpty()) {
                    devopsServiceInstanceDTOS.stream().filter(devopsServiceAppInstanceDTO -> !devopsServiceAppInstanceDTO.getInstanceId().equals(instanceId)).forEach(devopsServiceAppInstanceDTO -> devopsServiceInstanceService.baseUpdateInstanceId(devopsServiceAppInstanceDTO.getId(), instanceId));
                }

                devopsEnvCommandDTO.setSha(GitUtil.getFileLatestCommit(path + GIT_SUFFIX, filePath));
                devopsEnvCommandService.baseUpdateSha(devopsEnvCommandDTO.getId(), devopsEnvCommandDTO.getSha());

                devopsEnvFileResourceService.updateOrCreateFileResource(objectPath, envId, null, c7nHelmRelease.hashCode(), instanceId,
                        c7nHelmRelease.getKind());

            } catch (GitOpsExplainException ex) {
                throw ex;
            } catch (CommonException e) {
                throw new GitOpsExplainException(e.getMessage(), filePath, "", e.getParameters());
            }
        });
    }


    /**
     * 校验版本是否为空
     *
     * @param appServiceVersionDTO 应用服务版本
     * @param filePath             文件路径
     * @param c7nHelmRelease       release信息
     */
    private void validateVersion(AppServiceVersionDTO appServiceVersionDTO, String filePath, C7nHelmRelease c7nHelmRelease) {
        if (appServiceVersionDTO == null) {
            throw new GitOpsExplainException("appversion.not.exist.in.database", filePath, c7nHelmRelease.getSpec().getChartVersion());
        }
    }

    private AppServiceDeployVO getApplicationDeployDTO(C7nHelmRelease c7nHelmRelease,
                                                       Long projectId, Long envId, String filePath, String type) {
        ProjectDTO projectDTO = baseServiceClientOperator.queryIamProjectById(projectId);
        OrganizationDTO organization = baseServiceClientOperator.queryOrganizationById(projectDTO.getOrganizationId());
        DevopsEnvironmentDTO devopsEnvironmentDTO = devopsEnvironmentService.baseQueryById(envId);
        boolean isClusterComponent = GitOpsUtil.isClusterComponent(devopsEnvironmentDTO.getType(), c7nHelmRelease);

        AppServiceVersionDTO appServiceVersionDTO = null;
        Long releaseAppServiceId = null;
        String versionValue;
        // 根据不同的release情况处理release所属应用服务及其版本
        if (!isClusterComponent) {

            List<AppServiceDTO> appServices = agentMsgHandlerService.getApplication(c7nHelmRelease.getSpec().getChartName(), projectId, organization.getId());

            if (appServices.isEmpty()) {
                throw new GitOpsExplainException("app.not.exist.in.database", filePath, c7nHelmRelease.getSpec().getChartName());
            }
            for (AppServiceDTO appServiceDTO : appServices) {
                appServiceVersionDTO = appServiceVersionService
                        .baseQueryByAppServiceIdAndVersion(appServiceDTO.getId(), c7nHelmRelease.getSpec().getChartVersion());
                if (appServiceVersionDTO != null) {
                    releaseAppServiceId = appServiceDTO.getId();
                    break;
                }
            }

            // 如果本身是使用共享规则共享的版本部署的实例，后续共享规则删除后，也允许实例修改values更新实例时,继续使用这个版本
            if (appServiceVersionDTO == null && "update".equals(type)) {
                AppServiceInstanceDTO appServiceInstanceDTO = appServiceInstanceService
                        .baseQueryByCodeAndEnv(c7nHelmRelease.getMetadata().getName(), envId);
                AppServiceVersionDTO versionUnShared = appServiceVersionService.baseQuery(appServiceInstanceDTO.getAppServiceId());
                if (Objects.equals(versionUnShared.getVersion(), c7nHelmRelease.getSpec().getChartVersion())) {
                    appServiceVersionDTO = versionUnShared;
                    releaseAppServiceId = versionUnShared.getAppServiceId();
                }
            }

            validateVersion(appServiceVersionDTO, filePath, c7nHelmRelease);
            versionValue = appServiceVersionService.baseQueryValue(appServiceVersionDTO.getId());
        } else {
            appServiceVersionDTO = ComponentVersionUtil.getComponentVersion(c7nHelmRelease.getSpec().getChartName());
            validateVersion(appServiceVersionDTO, filePath, c7nHelmRelease);
            versionValue = appServiceVersionDTO.getValues();
        }

        // 校验应用服务id是实例的实际应用服务id
        if (c7nHelmRelease.getSpec().getAppServiceId() != null
                && !Objects.equals(releaseAppServiceId, c7nHelmRelease.getSpec().getAppServiceId())) {
            throw new GitOpsExplainException(GitOpsObjectError.RELEASE_APP_SERVICE_ID_MISMATCH.getError(), filePath);
        }

        AppServiceDeployVO appServiceDeployVO = new AppServiceDeployVO();
        appServiceDeployVO.setEnvironmentId(envId);
        appServiceDeployVO.setType(type);
        appServiceDeployVO.setValues(appServiceInstanceService.getReplaceResult(versionValue, c7nHelmRelease.getSpec().getValues()).getYaml());
        appServiceDeployVO.setAppServiceId(releaseAppServiceId);
        appServiceDeployVO.setAppServiceVersionId(appServiceVersionDTO.getId());
        appServiceDeployVO.setInstanceName(c7nHelmRelease.getMetadata().getName());
        if (type.equals("update")) {
            DevopsEnvCommandDTO devopsEnvCommandDTO;
            AppServiceInstanceDTO appServiceInstanceDTO = appServiceInstanceService
                    .baseQueryByCodeAndEnv(c7nHelmRelease.getMetadata().getName(), envId);
            if (appServiceInstanceDTO.getCommandId() == null) {
                devopsEnvCommandDTO = devopsEnvCommandService.baseQueryByObject(ObjectType.INSTANCE.getType(), appServiceInstanceDTO.getId());
            } else {
                devopsEnvCommandDTO = devopsEnvCommandService.baseQuery(appServiceInstanceDTO.getCommandId());
            }
            String deployValue = appServiceInstanceService.baseQueryValueByInstanceId(appServiceInstanceDTO.getId());
            InstanceValueVO instanceValueVO = appServiceInstanceService.getReplaceResult(deployValue, appServiceDeployVO.getValues());
            if (deployValue != null
                    && (instanceValueVO.getDeltaYaml() == null || instanceValueVO.getDeltaYaml().equals("") || instanceValueVO.getDeltaYaml().trim().equals(COMPARE_VALUES))
                    && Objects.equals(appServiceVersionDTO.getId(), devopsEnvCommandDTO.getObjectVersionId())) {
                appServiceDeployVO.setIsNotChange(true);
            }
            appServiceDeployVO.setInstanceId(appServiceInstanceDTO.getId());
            appServiceDeployVO.setCommandId(appServiceInstanceDTO.getCommandId());
        }
        return appServiceDeployVO;
    }
}
