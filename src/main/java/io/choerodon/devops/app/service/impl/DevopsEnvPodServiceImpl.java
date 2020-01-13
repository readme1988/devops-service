package io.choerodon.devops.app.service.impl;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.google.common.collect.Lists;
import io.kubernetes.client.JSON;
import io.kubernetes.client.models.V1Pod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import io.choerodon.devops.api.vo.ContainerVO;
import io.choerodon.devops.api.vo.DevopsEnvPodInfoVO;
import io.choerodon.devops.api.vo.DevopsEnvPodVO;
import io.choerodon.devops.api.vo.PodMetricsRedisInfoVO;
import io.choerodon.devops.app.service.*;
import io.choerodon.devops.infra.dto.*;
import io.choerodon.devops.infra.enums.ResourceType;
import io.choerodon.devops.infra.handler.ClusterConnectionHandler;
import io.choerodon.devops.infra.mapper.DevopsEnvPodMapper;
import io.choerodon.devops.infra.util.*;

/**
 * Created by Zenger on 2018/4/17.
 */
@Service
public class DevopsEnvPodServiceImpl implements DevopsEnvPodService {

    private static JSON json = new JSON();
    private final Logger logger = LoggerFactory.getLogger(DevopsEnvPodServiceImpl.class);

    @Autowired
    private ClusterConnectionHandler clusterConnectionHandler;
    @Autowired
    private DevopsEnvironmentService devopsEnvironmentService;
    @Autowired
    private DevopsEnvResourceService devopsEnvResourceService;
    @Autowired
    private DevopsEnvResourceDetailService devopsEnvResourceDetailService;
    @Autowired
    private DevopsEnvPodMapper devopsEnvPodMapper;
    @Autowired
    private AgentPodService agentPodService;
    @Autowired
    private DevopsClusterService devopsClusterService;
    @Autowired
    private AgentCommandService agentCommandService;
    @Autowired
    private UserAttrService userAttrService;

    @Override
    public PageInfo<DevopsEnvPodVO> pageByOptions(Long projectId, Long envId, Long appServiceId, Long instanceId, Pageable pageable, String searchParam) {
        List<Long> updatedEnvList = clusterConnectionHandler.getUpdatedClusterList();
        PageInfo<DevopsEnvPodDTO> devopsEnvPodDTOPageInfo = basePageByIds(projectId, envId, appServiceId, instanceId, pageable, searchParam);
        PageInfo<DevopsEnvPodVO> devopsEnvPodVOPageInfo = ConvertUtils.convertPage(devopsEnvPodDTOPageInfo, DevopsEnvPodVO.class);

        devopsEnvPodVOPageInfo.setList(devopsEnvPodDTOPageInfo.getList().stream().map(devopsEnvPodDTO -> {
            DevopsEnvironmentDTO devopsEnvironmentDTO = devopsEnvironmentService.baseQueryById(devopsEnvPodDTO.getEnvId());
            DevopsEnvPodVO devopsEnvPodVO = ConvertUtils.convertObject(devopsEnvPodDTO, DevopsEnvPodVO.class);
            devopsEnvPodVO.setClusterId(devopsEnvironmentDTO.getClusterId());
            devopsEnvPodVO.setConnect(updatedEnvList.contains(devopsEnvironmentDTO.getClusterId()));
            //给pod设置containers
            fillContainers(devopsEnvPodVO);
            return devopsEnvPodVO;
        }).collect(Collectors.toList()));

        return devopsEnvPodVOPageInfo;
    }

    @Override
    public void fillContainers(DevopsEnvPodVO devopsEnvPodVO) {

        //解析pod的yaml内容获取container的信息
        String message = devopsEnvResourceService.getResourceDetailByNameAndTypeAndInstanceId(devopsEnvPodVO.getInstanceId(), devopsEnvPodVO.getName(), ResourceType.POD);

        if (StringUtils.isEmpty(message)) {
            return;
        }

        try {
            V1Pod pod = K8sUtil.deserialize(message, V1Pod.class);
            List<ContainerVO> containers = pod.getStatus().getContainerStatuses()
                    .stream()
                    .map(container -> {
                        ContainerVO containerVO = new ContainerVO();
                        containerVO.setName(container.getName());
                        containerVO.setReady(container.isReady());
                        return containerVO;
                    })
                    .collect(Collectors.toList());

            // 将不可用的容器置于靠前位置
            Map<Boolean, List<ContainerVO>> containsByStatus = containers.stream().collect(Collectors.groupingBy(container -> container.getReady() == null ? Boolean.FALSE : container.getReady()));
            List<ContainerVO> result = new ArrayList<>();
            if (!ArrayUtil.isEmpty(containsByStatus.get(Boolean.FALSE))) {
                result.addAll(containsByStatus.get(Boolean.FALSE));
            }
            if (!ArrayUtil.isEmpty(containsByStatus.get(Boolean.TRUE))) {
                result.addAll(containsByStatus.get(Boolean.TRUE));
            }
            devopsEnvPodVO.setContainers(result);
        } catch (Exception e) {
            logger.info("名为 '{}' 的Pod的资源解析失败", devopsEnvPodVO.getName());
        }
    }


    @Override
    public DevopsEnvPodDTO baseQueryById(Long id) {
        return devopsEnvPodMapper.selectByPrimaryKey(id);
    }

    @Override
    public DevopsEnvPodDTO baseQueryByPod(DevopsEnvPodDTO devopsEnvPodDTO) {
        List<DevopsEnvPodDTO> devopsEnvPodDOS =
                devopsEnvPodMapper.select(devopsEnvPodDTO);
        if (devopsEnvPodDOS.isEmpty()) {
            return null;
        }
        return devopsEnvPodDOS.get(0);
    }

    @Override
    public void baseCreate(DevopsEnvPodDTO devopsEnvPodDTO) {
        DevopsEnvPodDTO envPodDTO = new DevopsEnvPodDTO();
        envPodDTO.setName(devopsEnvPodDTO.getName());
        envPodDTO.setNamespace(devopsEnvPodDTO.getNamespace());
        if (devopsEnvPodMapper.selectOne(envPodDTO) == null) {
            MapperUtil.resultJudgedInsert(devopsEnvPodMapper, devopsEnvPodDTO, "error.insert.env.pod");
        }
    }

    @Override
    public void baseUpdate(DevopsEnvPodDTO devopsEnvPodDTO) {
        devopsEnvPodMapper.updateByPrimaryKey(devopsEnvPodDTO);
    }

    @Override
    public List<DevopsEnvPodDTO> baseListByInstanceId(Long instanceId) {
        DevopsEnvPodDTO devopsEnvPodDTO = new DevopsEnvPodDTO();
        devopsEnvPodDTO.setInstanceId(instanceId);
        return devopsEnvPodMapper.select(devopsEnvPodDTO);
    }

    @Override
    public PageInfo<DevopsEnvPodDTO> basePageByIds(Long projectId, Long envId, Long appServiceId, Long instanceId, Pageable pageable, String searchParam) {

        Sort sort = pageable.getSort();
        String sortResult = "";
        if (sort != null) {
            sortResult = Lists.newArrayList(pageable.getSort().iterator()).stream()
                    .map(t -> {
                        String property = t.getProperty();
                        if (property.equals("name")) {
                            property = "dp.`name`";
                        } else if (property.equals("ip")) {
                            property = "dp.ip";
                        } else if (property.equals("creationDate")) {
                            property = "dp.creation_date";
                        }

                        return property + " " + t.getDirection();
                    })
                    .collect(Collectors.joining(","));
        }
        PageInfo<DevopsEnvPodDTO> devopsEnvPodDOPage;
        if (!org.apache.commons.lang.StringUtils.isEmpty(searchParam)) {
            Map<String, Object> searchParamMap = json.deserialize(searchParam, Map.class);
            devopsEnvPodDOPage = PageHelper.startPage(
                    pageable.getPageNumber(), pageable.getPageSize(), sortResult).doSelectPageInfo(() -> devopsEnvPodMapper.listAppServicePod(
                    projectId,
                    envId,
                    appServiceId,
                    instanceId,
                    TypeUtil.cast(searchParamMap.get(TypeUtil.SEARCH_PARAM)),
                    TypeUtil.cast(searchParamMap.get(TypeUtil.PARAMS))));
        } else {
            devopsEnvPodDOPage = PageHelper.startPage(
                    pageable.getPageNumber(), pageable.getPageSize(), sortResult).doSelectPageInfo(() -> devopsEnvPodMapper.listAppServicePod(projectId, envId, appServiceId, instanceId, null, null));
        }

        return devopsEnvPodDOPage;
    }

    @Override
    public void baseDeleteByName(String name, String namespace) {
        DevopsEnvPodDTO devopsEnvPodDO = new DevopsEnvPodDTO();
        devopsEnvPodDO.setName(name);
        devopsEnvPodDO.setNamespace(namespace);
        List<DevopsEnvPodDTO> devopsEnvPodDOs = devopsEnvPodMapper.select(devopsEnvPodDO);
        if (!devopsEnvPodDOs.isEmpty()) {
            devopsEnvPodMapper.delete(devopsEnvPodDOs.get(0));
        }
    }

    @Override
    public void baseDeleteById(Long id) {
        devopsEnvPodMapper.deleteByPrimaryKey(id);
    }

    @Override
    public DevopsEnvPodDTO queryByNameAndEnvName(String name, String namespace) {
        DevopsEnvPodDTO devopsEnvPodDTO = new DevopsEnvPodDTO();
        devopsEnvPodDTO.setName(name);
        devopsEnvPodDTO.setNamespace(namespace);
        return devopsEnvPodMapper.selectOne(devopsEnvPodDTO);
    }

    private static Map<String, DevopsEnvResourceDTO> listToMap(List<DevopsEnvResourceDTO> resources) {
        Map<String, DevopsEnvResourceDTO> map = new HashMap<>();
        for (DevopsEnvResourceDTO resource : resources) {
            if (map.get(resource.getName()) == null) {
                map.put(resource.getName(), resource);
            } else {
                map.put(resource.getName(), compareRevision(map.get(resource.getName()), resource));
            }
        }
        return map;
    }

    private static DevopsEnvResourceDTO compareRevision(DevopsEnvResourceDTO one, DevopsEnvResourceDTO theOther) {
        if (one == null || one.getReversion() == null) {
            return theOther;
        }
        if (theOther == null || theOther.getReversion() == null) {
            return one;
        }
        return one.getReversion() > theOther.getReversion() ? one : theOther;
    }

    @Override
    public List<DevopsEnvPodInfoVO> queryEnvPodInfo(Long envId, String sort) {
        DevopsEnvironmentDTO devopsEnvironmentDTO = devopsEnvironmentService.baseQueryById(envId);
        DevopsClusterDTO devopsClusterDTO = devopsClusterService.baseQuery(devopsEnvironmentDTO.getClusterId());
        List<DevopsEnvPodInfoVO> devopsEnvPodInfoVOList = devopsEnvPodMapper.queryEnvPodIns(envId);

        // 根据devopsEnvPodInfoVOList获取name集合，批量查询devopsEnvResourceDTO和DevopsEnvResourceDetailDTO
        List<String> podNames = devopsEnvPodInfoVOList.stream().map(DevopsEnvPodInfoVO::getName).collect(Collectors.toList());
        List<DevopsEnvResourceDTO> devopsEnvResourceDTOList = devopsEnvResourceService.listEnvResourceByOptions(envId, ResourceType.POD.getType(), podNames);
        Set<Long> resourceDetailIds = devopsEnvResourceDTOList.stream().map(DevopsEnvResourceDTO::getResourceDetailId).collect(Collectors.toSet());
        Map<String, DevopsEnvResourceDTO> devopsEnvResourceMap = listToMap(devopsEnvResourceDTOList);
        List<DevopsEnvResourceDetailDTO> devopsEnvResourceDetailDTOS = devopsEnvResourceDetailService.listByMessageIds(resourceDetailIds);
        Map<Long, DevopsEnvResourceDetailDTO> devopsEnvResourceDetailMap = devopsEnvResourceDetailDTOS.stream().collect(Collectors.toMap(DevopsEnvResourceDetailDTO::getId, Function.identity()));

        devopsEnvPodInfoVOList.forEach(devopsEnvPodInfoVO -> {
            PodMetricsRedisInfoVO podMetricsRedisInfoVO = agentPodService.queryLatestPodSnapshot(devopsEnvPodInfoVO.getName(), devopsEnvPodInfoVO.getNamespace(), devopsClusterDTO.getCode());
            DevopsEnvResourceDTO devopsEnvResourceDTO = devopsEnvResourceMap.get(devopsEnvPodInfoVO.getName());
            DevopsEnvResourceDetailDTO devopsEnvResourceDetailDTO = devopsEnvResourceDetailMap.get(devopsEnvResourceDTO.getResourceDetailId());
            V1Pod v1Pod = json.deserialize(devopsEnvResourceDetailDTO.getMessage(), V1Pod.class);
            devopsEnvPodInfoVO.setStatus(K8sUtil.changePodStatus(v1Pod));
            devopsEnvPodInfoVO.setPodIp(v1Pod == null ? null : v1Pod.getStatus().getPodIP());
            if (podMetricsRedisInfoVO != null) {
                devopsEnvPodInfoVO.setCpuUsed(podMetricsRedisInfoVO.getCpu());
                devopsEnvPodInfoVO.setMemoryUsed(podMetricsRedisInfoVO.getMemory());
            }
        });

        // 根据cpu进行逆序排序，考虑为null值的情况
        if ("cpu".equals(sort)) {
            devopsEnvPodInfoVOList = devopsEnvPodInfoVOList.stream()
                    .sorted(Comparator.comparing(DevopsEnvPodInfoVO::getCpuUsed, Comparator.nullsFirst(String::compareTo)).reversed())
                    .collect(Collectors.toList());
        }

        // 默认根据memory进行逆序排序，考虑为null值的情况
        if ("memory".equals(sort)) {
            devopsEnvPodInfoVOList = devopsEnvPodInfoVOList.stream()
                    .sorted(Comparator.comparing(DevopsEnvPodInfoVO::getMemoryUsed, Comparator.nullsFirst(String::compareTo)).reversed())
                    .collect(Collectors.toList());
        }

        return devopsEnvPodInfoVOList;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteEnvPodById(Long envId, Long podId) {
        DevopsEnvPodDTO devopsEnvPodDTO = baseQueryById(podId);
        // 查询不到pod直接返回
        if (devopsEnvPodDTO == null) {
            return;
        }
        //检验环境相关信息
        DevopsEnvironmentDTO devopsEnvironmentDTO = devopsEnvironmentService.baseQueryById(envId);
        UserAttrDTO userAttrDTO = userAttrService.baseQueryById(TypeUtil.objToLong(GitUserNameUtil.getUserId()));
        devopsEnvironmentService.checkEnv(devopsEnvironmentDTO, userAttrDTO);

        agentCommandService.deletePod(devopsEnvPodDTO.getName(), devopsEnvironmentDTO.getCode(), devopsEnvironmentDTO.getClusterId());
    }
}
