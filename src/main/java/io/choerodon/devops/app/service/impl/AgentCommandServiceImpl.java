package io.choerodon.devops.app.service.impl;

import java.io.IOException;
import java.util.*;
import java.util.regex.Pattern;

import com.alibaba.fastjson.JSONArray;
import io.codearte.props2yaml.Props2YAML;
import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;

import io.choerodon.core.exception.CommonException;
import io.choerodon.devops.api.vo.*;
import io.choerodon.devops.api.vo.kubernetes.Command;
import io.choerodon.devops.api.vo.kubernetes.ImagePullSecret;
import io.choerodon.devops.api.vo.kubernetes.Payload;
import io.choerodon.devops.app.eventhandler.constants.CertManagerConstants;
import io.choerodon.devops.app.eventhandler.payload.OperationPodPayload;
import io.choerodon.devops.app.eventhandler.payload.SecretPayLoad;
import io.choerodon.devops.app.service.AgentCommandService;
import io.choerodon.devops.infra.dto.AppServiceDTO;
import io.choerodon.devops.infra.dto.AppServiceVersionDTO;
import io.choerodon.devops.infra.dto.DevopsClusterDTO;
import io.choerodon.devops.infra.dto.DevopsEnvironmentDTO;
import io.choerodon.devops.infra.dto.iam.OrganizationDTO;
import io.choerodon.devops.infra.dto.iam.ProjectDTO;
import io.choerodon.devops.infra.enums.EnvironmentType;
import io.choerodon.devops.infra.enums.HelmType;
import io.choerodon.devops.infra.feign.operator.BaseServiceClientOperator;
import io.choerodon.devops.infra.handler.ClusterConnectionHandler;
import io.choerodon.devops.infra.mapper.DevopsClusterMapper;
import io.choerodon.devops.infra.util.FileUtil;
import io.choerodon.devops.infra.util.GitOpsUtil;
import io.choerodon.devops.infra.util.GitUtil;
import io.choerodon.websocket.helper.WebSocketHelper;
import io.choerodon.websocket.send.SendMessagePayload;
import io.choerodon.websocket.send.relationship.BrokerKeySessionMapper;


/**
 * Created by younger on 2018/4/18.
 */
@Service
public class AgentCommandServiceImpl implements AgentCommandService {

    public static final Logger LOGGER = LoggerFactory.getLogger(AgentCommandServiceImpl.class);


    private static final String ERROR_PAYLOAD_ERROR = "error.payload.error";
    private static final String KEY_FORMAT = "cluster:%d.release:%s";
    private static final String CLUSTER_FORMAT = "cluster:%s";
    private static final String CLUSTER = "cluster:";

    private static final String AGENT_INIT = "agent_init";
    private static final String ENV_DELETE = "env_delete";
    private static final String ENV_CREATE = "env_create";
    private static final String RESOURCE_DESCRIBE = "resource_describe";
    private static final String HELM_RELEASE_UPGRADE = "helm_release_upgrade";
    private static final String OPERATE_POD_COUNT = "operate_pod_count";
    private static final String OPERATE_DOCKER_REGISTRY_SECRET = "operate_docker_registry_secret";


    private Pattern pattern = Pattern.compile("^[-\\+]?[\\d]*$");
    private ObjectMapper mapper = new ObjectMapper();


    @Autowired
    private DevopsClusterMapper devopsClusterMapper;
    @Autowired
    private BaseServiceClientOperator baseServiceClientOperator;
    @Autowired
    private ClusterConnectionHandler clusterConnectionHandler;
    @Autowired
    private GitUtil gitUtil;
    @Autowired
    @Lazy
    private WebSocketHelper webSocketHelper;
    @Autowired
    @Lazy
    private BrokerKeySessionMapper brokerKeySessionMapper;


    @Value("${services.helm.url}")
    private String helmUrl;
    @Value("${agent.repoUrl}")
    private String agentRepoUrl;
    @Value("${services.gitlab.sshUrl}")
    private String gitlabSshUrl;
    @Value("${agent.version}")
    private String agentExpectVersion;
    @Value("${agent.serviceUrl}")
    private String agentServiceUrl;
    @Value("${agent.certManagerUrl}")
    private String certManagerUrl;


    @Override
    public void sendCommand(DevopsEnvironmentDTO devopsEnvironmentDTO) {
        AgentMsgVO msg = new AgentMsgVO();
        String namespace = GitOpsUtil.getEnvNamespace(Objects.requireNonNull(devopsEnvironmentDTO.getCode()), Objects.requireNonNull(devopsEnvironmentDTO.getType()));
        msg.setKey(CLUSTER + devopsEnvironmentDTO.getClusterId() + ".env:" + namespace + ".envId:" + devopsEnvironmentDTO.getId());
        msg.setType("git_ops_sync");
        msg.setPayload("");
        sendToWebsocket(devopsEnvironmentDTO.getClusterId(), msg);
    }


    @Override
    public void deploy(AppServiceDTO appServiceDTO, AppServiceVersionDTO appServiceVersionDTO, String releaseName, DevopsEnvironmentDTO devopsEnvironmentDTO, String values, Long commandId, String secretCode) {
        AgentMsgVO msg = new AgentMsgVO();
        List<ImagePullSecret> imagePullSecrets = null;
        if (secretCode != null) {
            imagePullSecrets = Arrays.asList(new ImagePullSecret(secretCode));
        }
        Payload payload = new Payload(
                devopsEnvironmentDTO.getCode(),
                appServiceVersionDTO.getRepository(),
                appServiceDTO.getCode(),
                appServiceVersionDTO.getVersion(),
                values, releaseName, imagePullSecrets);

        msg.setKey(String.format("cluster:%d.env:%s.envId:%d.release:%s",
                devopsEnvironmentDTO.getClusterId(),
                devopsEnvironmentDTO.getCode(),
                devopsEnvironmentDTO.getId(),
                releaseName));

        msg.setType(HelmType.HELM_UPGRADE_JOB_INFO.toValue());
        try {
            msg.setPayload(mapper.writeValueAsString(payload));
            msg.setCommandId(commandId);
        } catch (IOException e) {
            throw new CommonException(ERROR_PAYLOAD_ERROR, e);
        }
        sendToWebsocket(devopsEnvironmentDTO.getClusterId(), msg);
    }


    @Override
    public void upgradeCluster(DevopsClusterDTO devopsClusterDTO) {
        AgentMsgVO msg = new AgentMsgVO();
        Map<String, String> configs = new HashMap<>();
        configs.put("config.connect", agentServiceUrl);
        configs.put("config.token", devopsClusterDTO.getToken());
        configs.put("config.clusterId", devopsClusterDTO.getId().toString());
        configs.put("config.choerodonId", devopsClusterDTO.getChoerodonId());
        configs.put("rbac.create", "true");
        Payload payload = new Payload(
                "choerodon",
                agentRepoUrl,
                "choerodon-cluster-agent",
                agentExpectVersion,
                Props2YAML.fromContent(FileUtil.propertiesToString(configs))
                        .convert(), "choerodon-cluster-agent-" + devopsClusterDTO.getCode(), null);
        msg.setKey(String.format(KEY_FORMAT,
                devopsClusterDTO.getId(),
                "choerodon-cluster-agent-" + devopsClusterDTO.getCode()));
        msg.setType(HELM_RELEASE_UPGRADE);
        try {
            msg.setPayload(mapper.writeValueAsString(payload));
            String msgPayload = mapper.writeValueAsString(msg);

            // 暂时不使用新的WebSocket消息格式重写升级消息
            // 一开始没有自动升级
            //0.18.0到0.19.0 为了agent的平滑升级，所以不能以通用的新Msg方式发送，继续用以前的Msg格式发送
            brokerKeySessionMapper.getSessionsByKey(CLUSTER + devopsClusterDTO.getId()).stream().filter(Objects::nonNull).forEach(session -> {
                if (session.isOpen()) {
                    synchronized (session) {
                        try {
                            TextMessage textMessage = new TextMessage(msgPayload);
                            session.sendMessage(textMessage);
                        } catch (IOException e) {
                            LOGGER.warn("error.messageOperator.sendWebSocket.IOException, message: {}", msgPayload, e);
                        }
                    }
                }
            });
        } catch (IOException e) {
            throw new CommonException(e);
        }
    }

    @Override
    public void createCertManager(Long clusterId) {
        AgentMsgVO msg = new AgentMsgVO();
        Payload payload = new Payload(
                CertManagerConstants.CERT_MANAGER_REALASE_NAME_C7N,
                certManagerUrl,
                "cert-manager",
                CertManagerConstants.CERT_MANAGER_CHART_VERSION,
                null, CertManagerConstants.CERT_MANAGER_REALASE_NAME, null);
        msg.setKey(String.format(KEY_FORMAT,
                clusterId,
                CertManagerConstants.CERT_MANAGER_REALASE_NAME));
        msg.setType(HelmType.CERT_MANAGER_INSTALL.toValue());
        try {
            msg.setPayload(mapper.writeValueAsString(payload));
        } catch (IOException e) {
            throw new CommonException(ERROR_PAYLOAD_ERROR, e);
        }
        sendToWebsocket(clusterId, msg);
    }

    @Override
    public void operatePodCount(String deploymentName, String namespace, Long clusterId, Long count) {
        AgentMsgVO msg = new AgentMsgVO();
        OperationPodPayload operationPodPayload = new OperationPodPayload();
        operationPodPayload.setCount(count);
        operationPodPayload.setDeploymentName(deploymentName);
        operationPodPayload.setNamespace(namespace);
        try {
            msg.setPayload(mapper.writeValueAsString(operationPodPayload));
        } catch (IOException e) {
            throw new CommonException(ERROR_PAYLOAD_ERROR, e);
        }
        msg.setType(OPERATE_POD_COUNT);
        msg.setKey(String.format(CLUSTER_FORMAT, clusterId
        ));
        sendToWebsocket(clusterId, msg);
    }

    @Override

    public void operateSecret(Long clusterId, String namespace, String secretName, ConfigVO configVO, String type) {
        AgentMsgVO msg = new AgentMsgVO();
        SecretPayLoad secretPayLoad = new SecretPayLoad();
        secretPayLoad.setEmail(configVO.getEmail());
        secretPayLoad.setName(secretName);
        secretPayLoad.setNamespace(namespace);
        secretPayLoad.setServer(configVO.getUrl());
        secretPayLoad.setUsername(configVO.getUserName());
        secretPayLoad.setPassword(configVO.getPassword());

        try {
            msg.setPayload(mapper.writeValueAsString(secretPayLoad));
        } catch (IOException e) {
            throw new CommonException(ERROR_PAYLOAD_ERROR, e);
        }

        msg.setType(OPERATE_DOCKER_REGISTRY_SECRET);
        msg.setKey(String.format("cluster:%s.env:%s.Secret:%s", clusterId, namespace, secretName
        ));
        sendToWebsocket(clusterId, msg);
    }

    @Override
    public void gitopsSyncCommandStatus(Long clusterId, String envCode, Long envId, List<Command> commands) {

        AgentMsgVO msg = new AgentMsgVO();
        msg.setKey(String.format("cluster:%d.env:%s.envId:%d",
                clusterId,
                envCode,
                envId));
        msg.setType(HelmType.RESOURCE_STATUS_SYNC.toValue());
        try {
            msg.setPayload(JSONArray.toJSONString(commands));
        } catch (Exception e) {
            throw new CommonException(ERROR_PAYLOAD_ERROR, e);
        }
        sendToWebsocket(clusterId, msg);
    }

    @Override
    public void startOrStopInstance(String payload, String name, String type, String namespace, Long commandId, Long envId, Long clusterId) {
        AgentMsgVO msg = new AgentMsgVO();
        String key = CLUSTER + clusterId + ".env:" + namespace + ".envId:" + envId + ".release:" + name;
        msg.setKey(key);
        msg.setType(type);
        msg.setPayload(payload);
        msg.setCommandId(commandId);
        LOGGER.debug("Sending {} command. The key is: {}. THe commandId is: {}. The payload is {}. ", type, key, commandId, payload);
        sendToWebsocket(clusterId, msg);
    }

    @Override
    public void startLogOrExecConnection(String type, String key, PipeRequestVO pipeRequest, Long clusterId) {
        AgentMsgVO agentMsgVO = new AgentMsgVO();
        agentMsgVO.setKey(key);
        agentMsgVO.setType(type);
        try {
            agentMsgVO.setPayload(mapper.writeValueAsString(pipeRequest));
        } catch (IOException e) {
            throw new CommonException(ERROR_PAYLOAD_ERROR, e);
        }
        sendToWebsocket(clusterId, agentMsgVO);
    }


    @Override
    public void startDescribeConnection(String key, DescribeResourceVO describeResourceVO, Long clusterId) {
        AgentMsgVO agentMsgVO = new AgentMsgVO();
        agentMsgVO.setKey(key);
        agentMsgVO.setType(RESOURCE_DESCRIBE);
        try {
            agentMsgVO.setPayload(mapper.writeValueAsString(describeResourceVO));
        } catch (IOException e) {
            throw new CommonException(ERROR_PAYLOAD_ERROR, e);
        }
        sendToWebsocket(clusterId, agentMsgVO);
    }

    @Override
    public void initCluster(Long clusterId) {
        GitConfigVO gitConfigVO = gitUtil.getGitConfig(clusterId);
        AgentMsgVO msg = new AgentMsgVO();
        try {
            msg.setPayload(mapper.writeValueAsString(gitConfigVO));
        } catch (IOException e) {
            throw new CommonException("read envId from agent session failed", e);
        }
        msg.setType(AGENT_INIT);
        msg.setKey(String.format(CLUSTER_FORMAT, clusterId
        ));
        sendToWebsocket(clusterId, msg);
    }

    @Override
    public void initEnv(DevopsEnvironmentDTO devopsEnvironmentDTO, Long clusterId) {
        GitConfigVO gitConfigVO = gitUtil.getGitConfig(clusterId);
        List<GitEnvConfigVO> gitEnvConfigVOS = new ArrayList<>();
        ProjectDTO projectDTO = baseServiceClientOperator.queryIamProjectById(devopsEnvironmentDTO.getProjectId());
        OrganizationDTO organization = baseServiceClientOperator.queryOrganizationById(projectDTO.getOrganizationId());
        String repoUrl = GitUtil.getGitlabSshUrl(pattern, gitlabSshUrl, organization.getCode(),
                projectDTO.getCode(), devopsEnvironmentDTO.getCode(),
                EnvironmentType.forValue(devopsEnvironmentDTO.getType()),
                devopsClusterMapper.selectByPrimaryKey(devopsEnvironmentDTO.getClusterId()).getCode());

        GitEnvConfigVO gitEnvConfigVO = new GitEnvConfigVO();
        gitEnvConfigVO.setEnvId(devopsEnvironmentDTO.getId());
        gitEnvConfigVO.setGitRsaKey(devopsEnvironmentDTO.getEnvIdRsa());
        gitEnvConfigVO.setGitUrl(repoUrl);
        gitEnvConfigVO.setNamespace(devopsEnvironmentDTO.getCode());
        gitEnvConfigVOS.add(gitEnvConfigVO);
        gitConfigVO.setEnvs(gitEnvConfigVOS);
        gitConfigVO.setGitHost(gitlabSshUrl);
        AgentMsgVO msg = new AgentMsgVO();
        try {
            msg.setPayload(mapper.writeValueAsString(gitConfigVO));
        } catch (IOException e) {
            throw new CommonException("read envId from agent session failed", e);
        }
        msg.setType(ENV_CREATE);
        msg.setKey(String.format(CLUSTER_FORMAT, clusterId
        ));
        sendToWebsocket(clusterId, msg);
    }

    @Override
    public void deployTestApp(AppServiceDTO appServiceDTO, AppServiceVersionDTO appServiceVersionDTO, String releaseName, String secretName, Long clusterId, String values) {
        AgentMsgVO msg = new AgentMsgVO();
        List<ImagePullSecret> imagePullSecrets = Arrays.asList(new ImagePullSecret(secretName));
        Payload payload = new Payload(
                null,
                appServiceVersionDTO.getRepository(),
                appServiceDTO.getCode(),
                appServiceVersionDTO.getVersion(),
                values, releaseName, imagePullSecrets);
        msg.setKey(String.format(KEY_FORMAT, clusterId, releaseName));
        msg.setType(HelmType.TEST_EXECUTE.toValue());
        try {
            msg.setPayload(mapper.writeValueAsString(payload));
        } catch (IOException e) {
            throw new CommonException(ERROR_PAYLOAD_ERROR, e);
        }
        sendToWebsocket(clusterId, msg);
    }

    @Override
    public void getTestAppStatus(Map<Long, List<String>> testReleases) {
        List<Long> connected = clusterConnectionHandler.getConnectedClusterList();
        testReleases.forEach((key, value) -> {
            if (connected.contains(key)) {
                AgentMsgVO msg = new AgentMsgVO();
                msg.setKey(String.format("cluster:%d",
                        key));
                msg.setPayload(JSONArray.toJSONString(value));
                msg.setType(HelmType.TEST_STATUS.toValue());
                sendToWebsocket(key, msg);
            }
        });
    }

    @Override
    public void deleteEnv(Long envId, String code, Long clusterId) {
        GitEnvConfigVO gitEnvConfigVO = new GitEnvConfigVO();
        gitEnvConfigVO.setEnvId(envId);
        gitEnvConfigVO.setNamespace(code);
        AgentMsgVO msg = new AgentMsgVO();
        try {
            msg.setPayload(mapper.writeValueAsString(gitEnvConfigVO));
        } catch (IOException e) {
            throw new CommonException("error get envId and code", e);
        }
        msg.setType(ENV_DELETE);
        msg.setKey(String.format(CLUSTER_FORMAT, clusterId));
        sendToWebsocket(clusterId, msg);
    }

    private void sendToWebsocket(Long clusterId, AgentMsgVO msg) {
        SendMessagePayload<AgentMsgVO> webSocketSendPayload = new SendMessagePayload<>();
        webSocketSendPayload.setKey(CLUSTER + clusterId);
        webSocketSendPayload.setType("agent");
        webSocketSendPayload.setData(msg);
        webSocketHelper.sendMessageByKey(CLUSTER + clusterId, webSocketSendPayload);
    }

    @Override
    public void deletePod(String podName, String namespace, Long clusterId) {
        AgentMsgVO msg = new AgentMsgVO();
        msg.setKey(String.format(CLUSTER_FORMAT, clusterId));
        DeletePodVO payload = new DeletePodVO(podName, namespace);
        try {
            msg.setPayload(mapper.writeValueAsString(payload));
        } catch (IOException e) {
            throw new CommonException("Unexpected error occurred when serializing DeletePodVO {}", payload.toString());
        }
        msg.setType(HelmType.DELETE_POD.toValue());
        sendToWebsocket(clusterId, msg);
    }

    @Override
    public void unloadCertManager(Long clusterId) {
        AgentMsgVO msg = new AgentMsgVO();
        msg.setKey(String.format(KEY_FORMAT,
                clusterId,
                CertManagerConstants.CERT_MANAGER_REALASE_NAME));
        msg.setType(HelmType.CERT_MANAGER_UNINSTALL.toValue());
        msg.setPayload("{" + "\"" + CertManagerConstants.RELEASE_NAME + "\"" + ":" + "\"" + CertManagerConstants.CERT_MANAGER_REALASE_NAME + "\"" + "}");
        sendToWebsocket(clusterId, msg);
    }
}
