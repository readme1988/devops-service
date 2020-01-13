package io.choerodon.devops.app.service.impl;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.google.gson.Gson;
import io.choerodon.core.exception.CommonException;
import io.choerodon.devops.api.vo.*;
import io.choerodon.devops.api.vo.iam.ProjectWithRoleVO;
import io.choerodon.devops.api.vo.iam.RoleVO;
import io.choerodon.devops.app.service.*;
import io.choerodon.devops.infra.dto.*;
import io.choerodon.devops.infra.dto.iam.IamUserDTO;
import io.choerodon.devops.infra.dto.iam.ProjectDTO;
import io.choerodon.devops.infra.feign.operator.BaseServiceClientOperator;
import io.choerodon.devops.infra.handler.ClusterConnectionHandler;
import io.choerodon.devops.infra.mapper.DevopsClusterMapper;
import io.choerodon.devops.infra.mapper.DevopsPvProPermissionMapper;
import io.choerodon.devops.infra.util.*;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;


@Service
public class DevopsClusterServiceImpl implements DevopsClusterService {

    private static final String UPGRADE_MESSAGE = "Version is too low, please upgrade!";
    private static final String ERROR_CLUSTER_NOT_EXIST = "error.cluster.not.exist";
    private static final String PROJECT_OWNER = "role/project/default/project-owner";
    @Value("${agent.version}")
    private String agentExpectVersion;
    @Value("${agent.serviceUrl}")
    private String agentServiceUrl;
    @Value("${agent.repoUrl}")
    private String agentRepoUrl;
    private Gson gson = new Gson();
    @Autowired
    private BaseServiceClientOperator baseServiceClientOperator;
    @Autowired
    private ClusterConnectionHandler clusterConnectionHandler;
    @Autowired
    private ClusterNodeInfoService clusterNodeInfoService;
    @Autowired
    private DevopsEnvPodService devopsEnvPodService;
    @Autowired
    private DevopsClusterMapper devopsClusterMapper;
    @Autowired
    private DevopsClusterProPermissionService devopsClusterProPermissionService;
    @Autowired
    private DevopsEnvironmentService devopsEnvironmentService;
    @Autowired
    private DevopsPvService devopsPvService;
    @Autowired
    private DevopsPvProPermissionMapper devopsPvProPermissionMapper;


    @Override
    @Transactional
    public String createCluster(Long projectId, DevopsClusterReqVO devopsClusterReqVO) {
        ProjectDTO iamProject = baseServiceClientOperator.queryIamProjectById(projectId);

        // 继续判断id是够为空是因为可能会返回 CommonException 但是也会被反序列化为  ProjectDTO
        if (iamProject == null || iamProject.getId() == null) {
            throw new CommonException("error.project.query.by.id", projectId);
        }

        // 插入记录
        DevopsClusterDTO devopsClusterDTO = ConvertUtils.convertObject(devopsClusterReqVO, DevopsClusterDTO.class);
        devopsClusterDTO.setToken(GenerateUUID.generateUUID());
        devopsClusterDTO.setProjectId(projectId);
        devopsClusterDTO.setOrganizationId(iamProject.getOrganizationId());
        devopsClusterDTO.setSkipCheckProjectPermission(true);
        devopsClusterDTO = baseCreateCluster(devopsClusterDTO);


        IamUserDTO iamUserDTO = baseServiceClientOperator.queryUserByUserId(GitUserNameUtil.getUserId().longValue());

        // 渲染激活环境的命令参数
        InputStream inputStream = this.getClass().getResourceAsStream("/shell/cluster.sh");
        Map<String, String> params = new HashMap<>();
        params.put("{VERSION}", agentExpectVersion);
        params.put("{NAME}", "choerodon-cluster-agent-" + devopsClusterDTO.getCode());
        params.put("{SERVICEURL}", agentServiceUrl);
        params.put("{TOKEN}", devopsClusterDTO.getToken());
        params.put("{EMAIL}", iamUserDTO == null ? "" : iamUserDTO.getEmail());
        params.put("{CHOERODONID}", devopsClusterDTO.getChoerodonId());
        params.put("{REPOURL}", agentRepoUrl);
        params.put("{CLUSTERID}", devopsClusterDTO
                .getId().toString());
        return FileUtil.replaceReturnString(inputStream, params);
    }

    @Override
    @Transactional
    public void updateCluster(Long clusterId, DevopsClusterUpdateVO devopsClusterUpdateVO) {
        if (StringUtils.isEmpty(devopsClusterUpdateVO.getName())) {
            devopsClusterUpdateVO.setName(null);
        }
        baseUpdate(ConvertUtils.convertObject(devopsClusterUpdateVO, DevopsClusterDTO.class));
    }

    @Override
    public void checkName(Long projectId, String name) {
        DevopsClusterDTO devopsClusterDTO = new DevopsClusterDTO();
        devopsClusterDTO.setProjectId(projectId);
        devopsClusterDTO.setName(name);
        baseCheckName(devopsClusterDTO);
    }

    @Override
    public String queryShell(Long clusterId) {
        DevopsClusterRepVO devopsClusterRepVO = getDevopsClusterStatus(clusterId);
        InputStream inputStream = this.getClass().getResourceAsStream("/shell/cluster.sh");

        //初始化渲染脚本
        IamUserDTO iamUserDTO = baseServiceClientOperator.queryUserByUserId(devopsClusterRepVO.getCreateBy());
        Map<String, String> params = new HashMap<>();
        params.put("{VERSION}", agentExpectVersion);
        params.put("{NAME}", "choerodon-cluster-agent-" + devopsClusterRepVO.getCode());
        params.put("{SERVICEURL}", agentServiceUrl);
        params.put("{TOKEN}", devopsClusterRepVO.getToken());
        params.put("{EMAIL}", iamUserDTO == null ? "" : iamUserDTO.getEmail());
        params.put("{REPOURL}", agentRepoUrl);
        params.put("{CHOERODONID}", devopsClusterRepVO.getChoerodonId());
        params.put("{CLUSTERID}", devopsClusterRepVO
                .getId().toString());
        return FileUtil.replaceReturnString(inputStream, params);
    }

    @Override
    public void checkCode(Long projectId, String code) {
        ProjectDTO projectDTO = baseServiceClientOperator.queryIamProjectById(projectId);
        DevopsClusterDTO devopsClusterDTO = new DevopsClusterDTO();
        devopsClusterDTO.setOrganizationId(projectDTO.getOrganizationId());
        devopsClusterDTO.setCode(code);
        baseCheckCode(devopsClusterDTO);
    }

    @Override
    public PageInfo<ClusterWithNodesVO> pageClusters(Long projectId, Boolean doPage, Pageable pageable, String params) {
        PageInfo<DevopsClusterRepVO> devopsClusterRepVOPageInfo = ConvertUtils.convertPage(basePageClustersByOptions(projectId, doPage, pageable, params), DevopsClusterRepVO.class);
        PageInfo<ClusterWithNodesVO> devopsClusterRepDTOPage = ConvertUtils.convertPage(devopsClusterRepVOPageInfo, ClusterWithNodesVO.class);

        List<Long> connectedEnvList = clusterConnectionHandler.getConnectedClusterList();
        List<Long> updatedEnvList = clusterConnectionHandler.getUpdatedClusterList();
        devopsClusterRepVOPageInfo.getList().forEach(devopsClusterRepVO -> {
            devopsClusterRepVO.setConnect(isConnect(connectedEnvList, updatedEnvList, devopsClusterRepVO.getId()));
            devopsClusterRepVO.setUpgrade(isToUpgrade(connectedEnvList, updatedEnvList, devopsClusterRepVO.getId()));
            if (devopsClusterRepVO.getUpgrade()) {
                devopsClusterRepVO.setUpgradeMessage(UPGRADE_MESSAGE);
            }
        });

        devopsClusterRepDTOPage.setList(fromClusterE2ClusterWithNodesDTO(devopsClusterRepVOPageInfo.getList(), projectId));
        return devopsClusterRepDTOPage;
    }

    @Override
    public PageInfo<ProjectReqVO> listNonRelatedProjects(Long projectId, Long clusterId, Long selectedProjectId, Pageable pageable, String params) {
        DevopsClusterDTO devopsClusterDTO = devopsClusterMapper.selectByPrimaryKey(clusterId);
        if (devopsClusterDTO == null) {
            throw new CommonException(ERROR_CLUSTER_NOT_EXIST, clusterId);
        }

        Map<String, String> searchParamMap = new HashMap<>();
        List<String> paramList = new ArrayList<>();
        if (!StringUtils.isEmpty(params)) {
            Map maps = gson.fromJson(params, Map.class);
            searchParamMap = Optional.ofNullable((Map) TypeUtil.cast(maps.get(TypeUtil.SEARCH_PARAM))).orElse(new HashMap<>());
            paramList = Optional.ofNullable((List) TypeUtil.cast(maps.get(TypeUtil.PARAMS))).orElse(new ArrayList<String>());
        }

        ProjectDTO iamProjectDTO = baseServiceClientOperator.queryIamProjectById(projectId);

        // 查出组织下所有符合条件的项目
        List<ProjectDTO> filteredProjects = baseServiceClientOperator.listIamProjectByOrgId(
                iamProjectDTO.getOrganizationId(),
                searchParamMap.get("name"),
                searchParamMap.get("code"),
                CollectionUtils.isEmpty(paramList) ? null : paramList.get(0));

        // 查出数据库中已经分配权限的项目
        List<Long> permitted = devopsClusterProPermissionService.baseListByClusterId(clusterId)
                .stream()
                .map(DevopsClusterProPermissionDTO::getProjectId)
                .collect(Collectors.toList());

        // 将已经分配权限的项目过滤
        List<ProjectReqVO> projectReqVOS = filteredProjects
                .stream()
                .filter(p -> !permitted.contains(p.getId()))
                .map(p -> new ProjectReqVO(p.getId(), p.getName(), p.getCode()))
                .collect(Collectors.toList());

        if (selectedProjectId != null) {
            ProjectDTO selectedProjectDTO = baseServiceClientOperator.queryIamProjectById(selectedProjectId);
            ProjectReqVO projectReqVO = new ProjectReqVO(selectedProjectDTO.getId(), selectedProjectDTO.getName(), selectedProjectDTO.getCode());
            if (!projectReqVOS.isEmpty()) {
                projectReqVOS.remove(projectReqVO);
                projectReqVOS.add(0, projectReqVO);
            } else {
                projectReqVOS.add(projectReqVO);
            }
        }
        return PageInfoUtil.createPageFromList(projectReqVOS, pageable);
    }

    @Transactional
    @Override
    public void assignPermission(DevopsClusterPermissionUpdateVO update) {
        DevopsClusterDTO devopsClusterDTO = devopsClusterMapper.selectByPrimaryKey(update.getClusterId());
        if (devopsClusterDTO == null) {
            throw new CommonException(ERROR_CLUSTER_NOT_EXIST, update.getClusterId());
        }

        if (devopsClusterDTO.getSkipCheckProjectPermission()) {
            // 原来跳过，现在也跳过，不处理

            if (!update.getSkipCheckProjectPermission()) {
                // 原来跳过，现在不跳过，先更新字段，然后插入关联关系
                updateSkipPermissionCheck(
                        update.getClusterId(),
                        update.getSkipCheckProjectPermission(),
                        update.getObjectVersionNumber());

                devopsClusterProPermissionService.batchInsertIgnore(
                        update.getClusterId(),
                        update.getProjectIds());

                //如果在PV里面有未非配权限的项目，则删除
                List<DevopsPvProPermissionDTO> devopsPvProPermissionDTOList = devopsPvProPermissionMapper.listByClusterId(update.getClusterId());
                if (!devopsPvProPermissionDTOList.isEmpty()) {
                    List<DevopsPvProPermissionDTO> devopsPvProPermissionDTOToDeleteList = devopsPvProPermissionDTOList.stream()
                            .filter(e -> !update.getProjectIds().contains(e.getProjectId()))
                            .collect(Collectors.toList());
                    if (!devopsPvProPermissionDTOToDeleteList.isEmpty()) {
                        devopsPvProPermissionMapper.batchDelete(devopsPvProPermissionDTOToDeleteList);
                    }
                }

            }
        } else {
            // 原来不跳过，现在跳过，更新集群权限字段，再删除所有数据库中与该集群有关的关联关系
            if (update.getSkipCheckProjectPermission()) {
                updateSkipPermissionCheck(
                        update.getClusterId(),
                        update.getSkipCheckProjectPermission(),
                        update.getObjectVersionNumber());

                devopsClusterProPermissionService.baseDeleteByClusterId(update.getClusterId());
            } else {
                // 原来不跳过，现在也不跳过，批量添加权限
                devopsClusterProPermissionService.batchInsertIgnore(
                        update.getClusterId(),
                        update.getProjectIds());
            }
        }
    }

    /**
     * 更新集群的权限校验字段
     *
     * @param clusterId           集群id
     * @param skipCheckPermission 是否跳过权限校验
     * @param objectVersionNumber 版本号
     */
    private void updateSkipPermissionCheck(Long clusterId, Boolean skipCheckPermission, Long objectVersionNumber) {
        DevopsClusterDTO toUpdate = new DevopsClusterDTO();
        toUpdate.setId(clusterId);
        toUpdate.setObjectVersionNumber(objectVersionNumber);
        toUpdate.setSkipCheckProjectPermission(skipCheckPermission);
        devopsClusterMapper.updateByPrimaryKeySelective(toUpdate);
    }

    @Override
    public void deletePermissionOfProject(Long clusterId, Long relatedProjectId) {
        if (clusterId == null || relatedProjectId == null) {
            return;
        }
        //查出该集群关联的所有PV，删除与relatedProjectId的关联信息
        List<Long> pvIds = devopsPvService.queryByClusterId(clusterId).stream()
                .map(DevopsPvDTO::getId)
                .collect(Collectors.toList());
        if (!pvIds.isEmpty()) {
            devopsPvProPermissionMapper.batchDeleteByPvIdsAndProjectId(pvIds, relatedProjectId);
        }

        DevopsClusterProPermissionDTO permission = new DevopsClusterProPermissionDTO();
        permission.setClusterId(clusterId);
        permission.setProjectId(relatedProjectId);
        devopsClusterProPermissionService.baseDeletePermission(permission);
    }

    @Override
    public List<DevopsClusterBasicInfoVO> queryClustersAndNodes(Long projectId) {
        DevopsClusterDTO devopsClusterDTO = new DevopsClusterDTO();
        devopsClusterDTO.setProjectId(projectId);
        List<DevopsClusterDTO> devopsClusterDTOList = devopsClusterMapper.select(devopsClusterDTO);
        List<DevopsClusterBasicInfoVO> devopsClusterBasicInfoVOList = ConvertUtils.convertList(devopsClusterDTOList, DevopsClusterBasicInfoVO.class);
        List<Long> connectedEnvList = clusterConnectionHandler.getConnectedClusterList();
        List<Long> updatedEnvList = clusterConnectionHandler.getUpdatedClusterList();

        devopsClusterBasicInfoVOList.forEach(devopsClusterBasicInfoVO -> {
            devopsClusterBasicInfoVO.setConnect(isConnect(connectedEnvList, updatedEnvList, devopsClusterBasicInfoVO.getId()));
            devopsClusterBasicInfoVO.setUpgrade(isToUpgrade(connectedEnvList, updatedEnvList, devopsClusterBasicInfoVO.getId()));
            if (devopsClusterBasicInfoVO.getUpgrade()) {
                devopsClusterBasicInfoVO.setUpgradeMessage(UPGRADE_MESSAGE);
            }
        });
        devopsClusterBasicInfoVOList.forEach(devopsClusterBasicInfoVO ->
                devopsClusterBasicInfoVO.setNodes(clusterNodeInfoService.queryNodeName(projectId, devopsClusterBasicInfoVO.getId())));

        return devopsClusterBasicInfoVOList;
    }

    @Override
    public PageInfo<ProjectReqVO> pageRelatedProjects(Long projectId, Long clusterId, Pageable pageable, String params) {
        DevopsClusterDTO devopsClusterDTO = devopsClusterMapper.selectByPrimaryKey(clusterId);
        if (devopsClusterDTO == null) {
            throw new CommonException(ERROR_CLUSTER_NOT_EXIST, clusterId);
        }

        Map<String, Object> map = TypeUtil.castMapParams(params);
        Map<String, Object> searchParamsMap = TypeUtil.cast(map.get(TypeUtil.SEARCH_PARAM));
        String name = null;
        String code = null;
        if (!CollectionUtils.isEmpty(searchParamsMap)) {
            name = TypeUtil.cast(searchParamsMap.get("name"));
            code = TypeUtil.cast(searchParamsMap.get("code"));
        }
        List<String> paramList = TypeUtil.cast(map.get(TypeUtil.PARAMS));
        if (CollectionUtils.isEmpty(paramList)) {
            //如果不分页
            if (pageable.getPageSize() == 0) {
                Set<Long> devopsProjectIds = devopsClusterProPermissionService.baseListByClusterId(clusterId).stream()
                        .map(DevopsClusterProPermissionDTO::getProjectId)
                        .collect(Collectors.toSet());
                List<ProjectReqVO> projectReqVOList = baseServiceClientOperator.queryProjectsByIds(devopsProjectIds).stream()
                        .map(i -> new ProjectReqVO(i.getId(), i.getName(), i.getCode()))
                        .collect(Collectors.toList());
                return PageInfoUtil.createPageFromList(projectReqVOList, pageable);
            } else {
                // 如果不搜索
                PageInfo<DevopsClusterProPermissionDTO> relationPage = PageHelper.startPage(
                        pageable.getPageNumber(), pageable.getPageSize())
                        .doSelectPageInfo(() -> devopsClusterProPermissionService.baseListByClusterId(clusterId));
                return ConvertUtils.convertPage(relationPage, permission -> {
                    if (permission.getProjectId() == null) {
                        return null;
                    }
                    ProjectDTO projectDTO = baseServiceClientOperator.queryIamProjectById(permission.getProjectId());
                    return new ProjectReqVO(permission.getProjectId(), projectDTO.getName(), projectDTO.getCode());
                });
            }
        } else {
            // 如果要搜索，需要手动在程序内分页
            ProjectDTO iamProjectDTO = baseServiceClientOperator.queryIamProjectById(projectId);

            // 手动查出所有组织下的项目
            List<ProjectDTO> filteredProjects = baseServiceClientOperator.listIamProjectByOrgId(
                    iamProjectDTO.getOrganizationId(),
                    name, code,
                    paramList.get(0));

            // 数据库中的有权限的项目
            List<Long> permissions = devopsClusterProPermissionService.baseListByClusterId(clusterId)
                    .stream()
                    .map(DevopsClusterProPermissionDTO::getProjectId)
                    .collect(Collectors.toList());

            // 过滤出在数据库中有权限的项目信息
            List<ProjectReqVO> allMatched = filteredProjects
                    .stream()
                    .filter(p -> permissions.contains(p.getId()))
                    .map(p -> ConvertUtils.convertObject(p, ProjectReqVO.class))
                    .collect(Collectors.toList());

            return PageInfoUtil.createPageFromList(allMatched, pageable);
        }
    }

    private boolean isConnect(List<Long> connectedEnvList, List<Long> updatedEnvList, Long clusterId) {
        if (connectedEnvList.contains(clusterId)) {
            return updatedEnvList.contains(clusterId);
        }
        return false;
    }

    /**
     * 集群是否需要升级
     *
     * @param connectedEnvList 已连接的集群id
     * @param updatedEnvList   up-to-date的集群id
     * @param clusterId        待判断的集群id
     * @return true 如果需要升级
     */
    private boolean isToUpgrade(List<Long> connectedEnvList, List<Long> updatedEnvList, Long clusterId) {
        if (connectedEnvList.contains(clusterId)) {
            return !updatedEnvList.contains(clusterId);
        }
        return false;
    }


    @Override
    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRED)
    public void deleteCluster(Long clusterId) {
        DevopsClusterDTO devopsClusterDTO = devopsClusterMapper.selectByPrimaryKey(clusterId);
        if (devopsClusterDTO == null) {
            return;
        }

        // 校验集群是否能够删除
        checkConnectEnvsAndPV(clusterId);

        if (!ObjectUtils.isEmpty(devopsClusterDTO.getClientId())) {
            baseServiceClientOperator.deleteClient(devopsClusterDTO.getOrganizationId(), devopsClusterDTO.getClientId());
        }
        devopsEnvironmentService.deleteSystemEnv(devopsClusterDTO.getProjectId(), devopsClusterDTO.getId(), devopsClusterDTO.getCode(), devopsClusterDTO.getSystemEnvId());

        baseDelete(clusterId);
    }

    @Override
    public ClusterMsgVO checkConnectEnvsAndPV(Long clusterId) {
        ClusterMsgVO clusterMsgVO = new ClusterMsgVO(false, false);
        List<Long> connectedEnvList = clusterConnectionHandler.getConnectedClusterList();
        List<DevopsEnvironmentDTO> devopsEnvironmentDTOS = devopsEnvironmentService.baseListUserEnvByClusterId(clusterId);

        if (connectedEnvList.contains(clusterId)) {
            clusterMsgVO.setCheckEnv(true);
        }
        if (!devopsEnvironmentDTOS.isEmpty()) {
            throw new CommonException("error.cluster.delete");
        }
        //集群是否存在PV
        List<DevopsPvDTO> clusterDTOList = devopsPvService.queryByClusterId(clusterId);
        if (!Objects.isNull(clusterDTOList) && !clusterDTOList.isEmpty()) {
            clusterMsgVO.setCheckPV(true);
        }
        return clusterMsgVO;
    }


    @Override
    public DevopsClusterRepVO query(Long clusterId) {
        DevopsClusterRepVO result = ConvertUtils.convertObject(baseQuery(clusterId), DevopsClusterRepVO.class);
        if (result == null) {
            return null;
        }
        List<Long> connectedList = clusterConnectionHandler.getConnectedClusterList();
        List<Long> upToDateList = clusterConnectionHandler.getUpdatedClusterList();
        result.setConnect(isConnect(connectedList, upToDateList, clusterId));
        result.setUpgrade(isToUpgrade(connectedList, upToDateList, clusterId));
        result.setUpgradeMessage(result.getUpgrade() ? UPGRADE_MESSAGE : null);

        return result;
    }

    @Override
    public PageInfo<DevopsEnvPodVO> pagePodsByNodeName(Long clusterId, String nodeName, Pageable pageable, String searchParam) {
        PageInfo<DevopsEnvPodDTO> devopsEnvPodDTOPageInfo = basePageQueryPodsByNodeName(clusterId, nodeName, pageable, searchParam);
        PageInfo<DevopsEnvPodVO> envPodVOPageInfo = ConvertUtils.convertPage(devopsEnvPodDTOPageInfo, DevopsEnvPodVO.class);

        envPodVOPageInfo.setList(devopsEnvPodDTOPageInfo.getList().stream().map(this::podDTO2VO).collect(Collectors.toList()));
        return envPodVOPageInfo;
    }

    @Override
    public DevopsClusterRepVO queryByCode(Long projectId, String code) {
        return ConvertUtils.convertObject(baseQueryByCode(projectId, code), DevopsClusterRepVO.class);
    }


    @Override
    public DevopsClusterDTO baseCreateCluster(DevopsClusterDTO devopsClusterDTO) {
        List<DevopsClusterDTO> devopsClusterDTOS = devopsClusterMapper.selectAll();
        String choerodonId = GenerateUUID.generateUUID().split("-")[0];
        if (!devopsClusterDTOS.isEmpty()) {
            devopsClusterDTO.setChoerodonId(devopsClusterDTOS.get(0).getChoerodonId());
        } else {
            devopsClusterDTO.setChoerodonId(choerodonId);
        }
        if (devopsClusterMapper.insert(devopsClusterDTO) != 1) {
            throw new CommonException("error.devops.cluster.insert");
        }
        return devopsClusterDTO;
    }

    @Override
    public void baseCheckName(DevopsClusterDTO devopsClusterDTO) {
        if (devopsClusterMapper.selectOne(devopsClusterDTO) != null) {
            throw new CommonException("error.cluster.name.exist");
        }
    }

    @Override
    public void baseCheckCode(DevopsClusterDTO devopsClusterDTO) {
        if (devopsClusterMapper.selectOne(devopsClusterDTO) != null) {
            throw new CommonException("error.cluster.code.exist");
        }
    }

    @Override
    public List<DevopsClusterDTO> baseListByProjectId(Long projectId, Long organizationId) {
        return devopsClusterMapper.listByProjectId(projectId, organizationId);
    }

    @Override
    public DevopsClusterDTO baseQuery(Long clusterId) {
        return devopsClusterMapper.selectByPrimaryKey(clusterId);
    }

    @Override
    public void baseUpdate(DevopsClusterDTO inputClusterDTO) {
        DevopsClusterDTO devopsClusterDTO = devopsClusterMapper.selectByPrimaryKey(inputClusterDTO.getId());
        inputClusterDTO.setObjectVersionNumber(devopsClusterDTO.getObjectVersionNumber());
        devopsClusterMapper.updateByPrimaryKeySelective(inputClusterDTO);
    }

    @Override
    public PageInfo<DevopsClusterDTO> basePageClustersByOptions(Long projectId, Boolean doPage, Pageable pageable, String params) {
        Map<String, Object> searchParamMap = TypeUtil.castMapParams(params);
        return PageHelper.startPage(pageable.getPageNumber(), pageable.getPageSize(), PageRequestUtil.getOrderBy(pageable))
                .doSelectPageInfo(
                        () -> devopsClusterMapper.listClusters(
                                projectId,
                                TypeUtil.cast(searchParamMap.get(TypeUtil.SEARCH_PARAM)),
                                TypeUtil.cast(searchParamMap.get(TypeUtil.PARAMS))));
    }

    @Override
    public void baseDelete(Long clusterId) {
        devopsClusterMapper.deleteByPrimaryKey(clusterId);
    }

    @Override
    public DevopsClusterDTO baseQueryByToken(String token) {
        DevopsClusterDTO devopsClusterDTO = new DevopsClusterDTO();
        devopsClusterDTO.setToken(token);
        return devopsClusterMapper.selectOne(devopsClusterDTO);
    }

    @Override
    public List<DevopsClusterDTO> baseList() {
        return devopsClusterMapper.selectAll();
    }

    @Override
    public PageInfo<DevopsEnvPodDTO> basePageQueryPodsByNodeName(Long clusterId, String nodeName, Pageable pageable, String searchParam) {
        Map<String, Object> paramMap = TypeUtil.castMapParams(searchParam);
        return PageHelper.startPage(pageable.getPageNumber(), pageable.getPageSize(), PageRequestUtil.getOrderBy(pageable)).doSelectPageInfo(() -> devopsClusterMapper.pageQueryPodsByNodeName(
                clusterId, nodeName,
                TypeUtil.cast(paramMap.get(TypeUtil.SEARCH_PARAM)),
                TypeUtil.cast(paramMap.get(TypeUtil.PARAMS))));
    }

    @Override
    public DevopsClusterDTO baseQueryByCode(Long projectId, String code) {
        DevopsClusterDTO devopsClusterDTO = new DevopsClusterDTO();
        devopsClusterDTO.setProjectId(projectId);
        devopsClusterDTO.setCode(code);
        return devopsClusterMapper.selectOne(devopsClusterDTO);
    }

    @Override
    public void baseUpdateProjectId(Long orgId, Long proId) {
        devopsClusterMapper.updateProjectId(orgId, proId);
    }

    @Override
    public Boolean checkUserClusterPermission(Long clusterId, Long userId) {
        DevopsClusterDTO devopsClusterDTO = new DevopsClusterDTO();
        devopsClusterDTO.setId(clusterId);
        DevopsClusterDTO devopsClusterDTO1 = devopsClusterMapper.selectByPrimaryKey(devopsClusterDTO);
        if (ObjectUtils.isEmpty(devopsClusterDTO1)) {
            throw new CommonException("error.devops.cluster.is.not.exist");
        }
        // 获取用户的项目权限为项目所有者的所有项目Ids
        List<ProjectWithRoleVO> projectWithRoleVOS = baseServiceClientOperator.listProjectWithRole(userId, 0, 0);
        if (CollectionUtils.isEmpty(projectWithRoleVOS)) {
            return false;
        }
        Set<Long> ownerRoleProjectIds = new HashSet<>();
        projectWithRoleVOS.stream().forEach(v -> {
            if (CollectionUtils.isEmpty(v.getRoles())) {
                return;
            }
            Set<Long> collect = v.getRoles().stream().filter(role -> PROJECT_OWNER.equals(role.getCode())).map(RoleVO::getId).collect(Collectors.toSet());
            if (!CollectionUtils.isEmpty(collect)) {
                ownerRoleProjectIds.add(v.getId());
            }
        });
        if (CollectionUtils.isEmpty(ownerRoleProjectIds)) {
            return false;
        }
        // 获取集群和集群分配的项目Ids
        List<DevopsClusterProPermissionDTO> devopsClusterProPermissionDTOS = devopsClusterProPermissionService.baseListByClusterId(clusterId);
        Set<Long> clusterBelongToProjectIds = devopsClusterProPermissionDTOS.stream().map(DevopsClusterProPermissionDTO::getProjectId).collect(Collectors.toSet());
        clusterBelongToProjectIds.add(devopsClusterDTO1.getProjectId());

        // 集合做差集处理
        clusterBelongToProjectIds.retainAll(ownerRoleProjectIds);
        return !CollectionUtils.isEmpty(clusterBelongToProjectIds);
    }

    /**
     * pod dto to cluster pod vo
     *
     * @param devopsEnvPodDTO pod dto
     * @return the cluster pod vo
     */
    private DevopsEnvPodVO podDTO2VO(final DevopsEnvPodDTO devopsEnvPodDTO) {
        DevopsEnvPodVO devopsEnvPodVO = ConvertUtils.convertObject(devopsEnvPodDTO, DevopsEnvPodVO.class);
        devopsEnvPodService.fillContainers(devopsEnvPodVO);
        return devopsEnvPodVO;
    }

    /**
     * convert cluster entity to instances of {@link ClusterWithNodesVO}
     *
     * @param devopsClusterRepVOS the cluster entities
     * @param projectId           the project id
     * @return the instances of the return type
     */
    private List<ClusterWithNodesVO> fromClusterE2ClusterWithNodesDTO(List<DevopsClusterRepVO> devopsClusterRepVOS, Long projectId) {
        // default three records of nodes in the instance
        Pageable pageable = PageRequest.of(1, 3);

        return devopsClusterRepVOS.stream().map(cluster -> {
            ClusterWithNodesVO clusterWithNodesDTO = new ClusterWithNodesVO();
            BeanUtils.copyProperties(cluster, clusterWithNodesDTO);
            if (Boolean.TRUE.equals(clusterWithNodesDTO.getConnect())) {
                clusterWithNodesDTO.setNodes(clusterNodeInfoService.pageClusterNodeInfo(cluster.getId(), projectId, pageable));
            }
            return clusterWithNodesDTO;
        }).collect(Collectors.toList());
    }

    private DevopsClusterRepVO getDevopsClusterStatus(Long clusterId) {
        DevopsClusterRepVO devopsClusterRepVO = ConvertUtils.convertObject(baseQuery(clusterId), DevopsClusterRepVO.class);
        List<Long> connectedEnvList = clusterConnectionHandler.getConnectedClusterList();
        List<Long> updatedEnvList = clusterConnectionHandler.getUpdatedClusterList();

        devopsClusterRepVO.setConnect(isConnect(connectedEnvList, updatedEnvList, devopsClusterRepVO.getId()));
        devopsClusterRepVO.setUpgrade(isToUpgrade(connectedEnvList, updatedEnvList, devopsClusterRepVO.getId()));
        if (devopsClusterRepVO.getUpgrade()) {
            devopsClusterRepVO.setUpgradeMessage(UPGRADE_MESSAGE);
        }
        return devopsClusterRepVO;
    }


}
