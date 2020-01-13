package io.choerodon.devops.app.service;

import java.util.List;
import java.util.Map;

import io.choerodon.devops.api.vo.ConfigVO;
import io.choerodon.devops.api.vo.DescribeResourceVO;
import io.choerodon.devops.api.vo.PipeRequestVO;
import io.choerodon.devops.api.vo.kubernetes.Command;
import io.choerodon.devops.infra.dto.*;


/**
 * Created by younger on 2018/4/18.
 */
public interface AgentCommandService {
    void sendCommand(DevopsEnvironmentDTO devopsEnvironmentDTO);

    void deploy(AppServiceDTO applicationDTO, AppServiceVersionDTO appServiceVersionDTO,
                String releaseName, DevopsEnvironmentDTO devopsEnvironmentDTO, String values,
                Long commandId, String secretCode);

    void initCluster(Long clusterId);

    void deleteEnv(Long envId, String code, Long clusterId);

    void initEnv(DevopsEnvironmentDTO devopsEnvironmentDTO, Long clusterId);

    void deployTestApp(AppServiceDTO applicationDTO, AppServiceVersionDTO appServiceVersionDTO, String releaseName, String secretName, Long clusterId, String values);

    void getTestAppStatus(Map<Long, List<String>> testReleases);

    void upgradeCluster(DevopsClusterDTO devopsClusterDTO);

    void createCertManager(Long clusterId);

    void operatePodCount(String deploymentName, String namespace, Long clusterId, Long count);

    void operateSecret(Long clusterId, String namespace, String secretName, ConfigVO configVO, String type);

    void gitopsSyncCommandStatus(Long clusterId, String envCode, Long envId, List<Command> commands);

    void startOrStopInstance(String payload, String name,
                             String type, String namespace,
                             Long commandId, Long envId,
                             Long clusterId);

    void startLogOrExecConnection(String type, String key, PipeRequestVO pipeRequest, Long clusterId);

    void startDescribeConnection(String key, DescribeResourceVO describeResourceVO, Long clusterId);

    void deletePod(String podName, String namespace, Long clusterId);

    void unloadCertManager(Long clusterId);
}
