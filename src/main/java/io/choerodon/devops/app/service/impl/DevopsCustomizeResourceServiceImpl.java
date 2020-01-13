package io.choerodon.devops.app.service.impl;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.google.gson.Gson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.yaml.snakeyaml.Yaml;

import org.springframework.data.domain.Pageable;

import io.choerodon.core.exception.CommonException;
import io.choerodon.devops.api.vo.DevopsCustomizeResourceReqVO;
import io.choerodon.devops.api.vo.DevopsCustomizeResourceVO;
import io.choerodon.devops.app.service.*;
import io.choerodon.devops.infra.dto.*;
import io.choerodon.devops.infra.enums.CommandStatus;
import io.choerodon.devops.infra.enums.CommandType;
import io.choerodon.devops.infra.enums.ObjectType;
import io.choerodon.devops.infra.enums.ResourceType;
import io.choerodon.devops.infra.feign.operator.BaseServiceClientOperator;
import io.choerodon.devops.infra.feign.operator.GitlabServiceClientOperator;
import io.choerodon.devops.infra.gitops.ResourceConvertToYamlHandler;
import io.choerodon.devops.infra.handler.ClusterConnectionHandler;
import io.choerodon.devops.infra.mapper.DevopsCustomizeResourceMapper;
import io.choerodon.devops.infra.util.*;

/**
 * Created by Sheep on 2019/6/26.
 */

@Service
public class DevopsCustomizeResourceServiceImpl implements DevopsCustomizeResourceService {

    public static final String CREATE = "create";
    public static final String UPDATE = "update";
    public static final String DELETE = "delete";
    public static final String CHOERODON_IO_RESOURCE = "choerodon.io/resource";
    public static final String METADATA = "metadata";
    public static final String CUSTOM = "custom";
    public static final String LABELS = "labels";
    public static final String KIND = "kind";
    private static final String FILE_SEPARATOR = System.getProperty("file.separator");
    private static final String FILE_NAME_PATTERN = "custom-%s.yaml";

    private static Gson gson = new Gson();
    private static final List<String> RESOURCE_TYPE = Arrays.asList(ResourceType.SERVICE.getType(), ResourceType.INGRESS.getType(),
            ResourceType.CONFIGMAP.getType(), ResourceType.SECRET.getType(),
            ResourceType.C7NHELMRELEASE.getType(), ResourceType.CERTIFICATE.getType(),
            ResourceType.ENDPOINTS.getType(), ResourceType.PERSISTENT_VOLUME_CLAIM.getType());

    @Autowired
    private DevopsCustomizeResourceMapper devopsCustomizeResourceMapper;
    @Autowired
    private BaseServiceClientOperator baseServiceClientOperator;
    @Autowired
    private DevopsCustomizeResourceContentService devopsCustomizeResourceContentService;
    @Autowired
    private DevopsEnvCommandService devopsEnvCommandService;
    @Autowired
    private ClusterConnectionHandler clusterConnectionHandler;
    @Autowired
    private UserAttrService userAttrService;
    @Autowired
    private GitlabServiceClientOperator gitlabServiceClientOperator;
    @Autowired
    private DevopsEnvironmentService devopsEnvironmentService;
    @Autowired
    private DevopsEnvFileResourceService devopsEnvFileResourceService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createOrUpdateResource(Long projectId, DevopsCustomizeResourceReqVO devopsCustomizeResourceReqVO, MultipartFile contentFile) {

        String content = devopsCustomizeResourceReqVO.getContent();

        String resourceFilePath = String.format(FILE_NAME_PATTERN, GenerateUUID.generateUUID().substring(0, 5));

        DevopsEnvironmentDTO devopsEnvironmentDTO = devopsEnvironmentService.baseQueryById(devopsCustomizeResourceReqVO.getEnvId());

        UserAttrDTO userAttrDTO = userAttrService.baseQueryById(TypeUtil.objToLong(GitUserNameUtil.getUserId()));

        //校验环境相关信息
        devopsEnvironmentService.checkEnv(devopsEnvironmentDTO, userAttrDTO);

        if (contentFile != null) {
            try {
                content = new String(contentFile.getBytes());
            } catch (IOException e) {
                throw new CommonException("error.read.multipart.file");
            }
        }

        Yaml yaml = new Yaml();
        List<Object> objects = new ArrayList<>();

        //处理每个k8s资源对象
        try {
            for (Object data : yaml.loadAll(content)) {
                Map<String, Object> datas = (Map<String, Object>) data;

                Object kind = datas.get(KIND);

                LinkedHashMap metadata = (LinkedHashMap) datas.get(METADATA);
                //校验yaml文件内资源属性是否合法,并返回资源的name
                String name = checkResource(metadata, kind);

                //添加自定义标签
                LinkedHashMap labels = (LinkedHashMap) metadata.get(LABELS);

                if (labels == null) {
                    labels = new LinkedHashMap();
                }
                labels.put(CHOERODON_IO_RESOURCE, CUSTOM);
                metadata.put(LABELS, labels);
                datas.put(METADATA, metadata);
                objects.add(datas);

                handleCustomResource(projectId, devopsCustomizeResourceReqVO.getEnvId(), FileUtil.getYaml().dump(datas), kind.toString(), name, devopsCustomizeResourceReqVO.getType(), devopsCustomizeResourceReqVO.getResourceId(), resourceFilePath, null);

            }
        } catch (CommonException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new CommonException("error.load.yaml.content");
        }

        if (devopsCustomizeResourceReqVO.getType().equals(CREATE)) {
            gitlabServiceClientOperator.createFile(devopsEnvironmentDTO.getGitlabEnvProjectId().intValue(), resourceFilePath, FileUtil.getYaml().dumpAll(objects.iterator()),
                    "ADD FILE", TypeUtil.objToInteger(userAttrDTO.getGitlabUserId()));
        } else {
            //判断当前容器目录下是否存在环境对应的gitops文件目录，不存在则克隆
            String gitOpsPath = clusterConnectionHandler.handDevopsEnvGitRepository(
                    devopsEnvironmentDTO.getProjectId(),
                    devopsEnvironmentDTO.getCode(),
                    devopsEnvironmentDTO.getEnvIdRsa(),
                    devopsEnvironmentDTO.getType(),
                    devopsEnvironmentDTO.getClusterCode());

            DevopsCustomizeResourceDTO devopsCustomizeResourceDTO = devopsCustomizeResourceMapper.selectByPrimaryKey(devopsCustomizeResourceReqVO.getResourceId());
            if (!gitlabServiceClientOperator.getFile(TypeUtil.objToInteger(devopsEnvironmentDTO.getGitlabEnvProjectId()), "master",
                    devopsCustomizeResourceDTO.getFilePath())) {
                throw new CommonException("error.fileResource.not.exist");
            }
            //获取更新内容
            ResourceConvertToYamlHandler<Object> resourceConvertToYamlHandler = new ResourceConvertToYamlHandler<>();
            String updateContent = resourceConvertToYamlHandler.getUpdateContent(objects.get(0), false, null, devopsCustomizeResourceDTO.getFilePath(), ResourceType.CUSTOM.getType(), gitOpsPath, CommandType.UPDATE.getType());
            gitlabServiceClientOperator.updateFile(devopsEnvironmentDTO.getGitlabEnvProjectId().intValue(), devopsCustomizeResourceDTO.getFilePath(), updateContent, "UPDATE FILE", TypeUtil.objToInteger(userAttrDTO.getGitlabUserId()));
        }

    }

    @Override
    public void createOrUpdateResourceByGitOps(String type, DevopsCustomizeResourceDTO devopsCustomizeResourceDTO, Long envId, Long userId) {

        DevopsEnvironmentDTO devopsEnvironmentDTO = devopsEnvironmentService.baseQueryById(devopsCustomizeResourceDTO.getEnvId());
        clusterConnectionHandler.checkEnvConnection(devopsEnvironmentDTO.getClusterId());

        handleCustomResource(devopsCustomizeResourceDTO.getProjectId(), devopsCustomizeResourceDTO.getEnvId(), devopsCustomizeResourceDTO.getResourceContent(), devopsCustomizeResourceDTO.getK8sKind(), devopsCustomizeResourceDTO.getName(), type, devopsCustomizeResourceDTO.getId(), devopsCustomizeResourceDTO.getFilePath(), userId);
    }


    @Override
    public void deleteResource(Long resourceId) {
        DevopsCustomizeResourceDTO devopsCustomizeResourceDTO = devopsCustomizeResourceMapper.selectByPrimaryKey(resourceId);

        if (devopsCustomizeResourceDTO == null) {
            return;
        }

        DevopsEnvironmentDTO devopsEnvironmentDTO = devopsEnvironmentService.baseQueryById(devopsCustomizeResourceDTO.getEnvId());
        UserAttrDTO userAttrDTO = userAttrService.baseQueryById(TypeUtil.objToLong(GitUserNameUtil.getUserId()));

        //校验环境相关信息
        devopsEnvironmentService.checkEnv(devopsEnvironmentDTO, userAttrDTO);

        handleCustomResource(null, null, null, null, null, DELETE, resourceId, null, null);

        //判断当前容器目录下是否存在环境对应的gitops文件目录，不存在则克隆
        String gitOpsPath = clusterConnectionHandler.handDevopsEnvGitRepository(
                devopsEnvironmentDTO.getProjectId(),
                devopsEnvironmentDTO.getCode(),
                devopsEnvironmentDTO.getEnvIdRsa(),
                devopsEnvironmentDTO.getType(),
                devopsEnvironmentDTO.getClusterCode());

        //判断gitops库里面是否有该文件，没有文件直接删除对象
        if (!gitlabServiceClientOperator.getFile(TypeUtil.objToInteger(devopsEnvironmentDTO.getGitlabEnvProjectId()), "master",
                devopsCustomizeResourceDTO.getFilePath())) {
            devopsCustomizeResourceMapper.deleteByPrimaryKey(resourceId);
            devopsCustomizeResourceContentService.baseDelete(devopsCustomizeResourceDTO.getContentId());
            DevopsEnvFileResourceDTO devopsEnvFileResourceDTO = devopsEnvFileResourceService
                    .baseQueryByEnvIdAndResourceId(devopsEnvironmentDTO.getId(), resourceId, ObjectType.CUSTOM.getType());
            if (devopsEnvFileResourceDTO != null) {
                devopsEnvFileResourceService.baseDeleteById(devopsEnvFileResourceDTO.getId());
            }
            return;
        }

        List<DevopsCustomizeResourceDTO> devopsCustomizeResourceDTOS = listByEnvAndFilePath(devopsEnvironmentDTO.getId(), devopsCustomizeResourceDTO.getFilePath());

        //如果对象所在文件只有一个对象，则直接删除文件,否则把对象从文件中去掉，更新文件
        if (devopsCustomizeResourceDTOS.size() == 1) {
            if (devopsCustomizeResourceDTOS.get(0).getId().equals(resourceId)) {
                gitlabServiceClientOperator.deleteFile(
                        TypeUtil.objToInteger(devopsEnvironmentDTO.getGitlabEnvProjectId()),
                        devopsCustomizeResourceDTO.getFilePath(),
                        "DELETE FILE",
                        TypeUtil.objToInteger(userAttrDTO.getGitlabUserId()));
            } else {
                devopsCustomizeResourceMapper.deleteByPrimaryKey(resourceId);
                devopsCustomizeResourceContentService.baseDelete(devopsCustomizeResourceDTO.getContentId());
            }
        } else {
            //获取更新内容
            DevopsCustomizeResourceContentDTO devopsCustomizeResourceContentDTO = devopsCustomizeResourceContentService.baseQuery(devopsCustomizeResourceDTO.getContentId());
            ResourceConvertToYamlHandler<Object> resourceConvertToYamlHandler = new ResourceConvertToYamlHandler<>();
            String updateContent = resourceConvertToYamlHandler.getUpdateContent(FileUtil.getYaml().load(devopsCustomizeResourceContentDTO.getContent()), false, null, devopsCustomizeResourceDTO.getFilePath(), ResourceType.CUSTOM.getType(), gitOpsPath, CommandType.DELETE.getType());
            gitlabServiceClientOperator.updateFile(devopsEnvironmentDTO.getGitlabEnvProjectId().intValue(), devopsCustomizeResourceDTO.getFilePath(), updateContent, "UPDATE FILE", TypeUtil.objToInteger(userAttrDTO.getGitlabUserId()));
        }
    }


    @Override
    public DevopsCustomizeResourceVO queryDevopsCustomizeResourceDetail(Long resourceId) {
        DevopsCustomizeResourceDTO devopsCustomizeResourceDTO = devopsCustomizeResourceMapper.queryDetail(resourceId);
        if (devopsCustomizeResourceDTO == null) {
            return null;
        }
        DevopsCustomizeResourceVO resource = ConvertUtils.convertObject(devopsCustomizeResourceDTO, DevopsCustomizeResourceVO.class);
        DevopsEnvironmentDTO devopsEnvironmentDTO = devopsEnvironmentService.baseQueryById(resource.getEnvId());
        resource.setEnvCode(devopsEnvironmentDTO.getCode());
        resource.setClusterId(devopsEnvironmentDTO.getClusterId());
        if (devopsCustomizeResourceDTO.getCreatedBy() != null && devopsCustomizeResourceDTO.getCreatedBy() != 0) {
            resource.setCreatorName(ResourceCreatorInfoUtil.getOperatorName(baseServiceClientOperator, devopsCustomizeResourceDTO.getCreatedBy()));
        }
        if (devopsCustomizeResourceDTO.getLastUpdatedBy() != null && devopsCustomizeResourceDTO.getLastUpdatedBy() != 0) {
            resource.setLastUpdaterName(ResourceCreatorInfoUtil.getOperatorName(baseServiceClientOperator, devopsCustomizeResourceDTO.getLastUpdatedBy()));
        }
        return resource;
    }


    @Override
    public PageInfo<DevopsCustomizeResourceVO> pageResources(Long envId, Pageable pageable, String params) {
        PageInfo<DevopsCustomizeResourceDTO> devopsCustomizeResourceDTOPageInfo = pageDevopsCustomizeResourceE(envId, pageable, params);
        List<Long> updatedEnvList = clusterConnectionHandler.getUpdatedClusterList();
        PageInfo<DevopsCustomizeResourceVO> devopsCustomizeResourceVOPageInfo = ConvertUtils.convertPage(devopsCustomizeResourceDTOPageInfo, DevopsCustomizeResourceVO.class);
        devopsCustomizeResourceVOPageInfo.getList().forEach(devopsCustomizeResourceVO -> {
            DevopsEnvironmentDTO devopsEnvironmentDTO = devopsEnvironmentService.baseQueryById(envId);
            devopsCustomizeResourceVO.setEnvStatus(updatedEnvList.contains(devopsEnvironmentDTO.getClusterId()));
        });
        return devopsCustomizeResourceVOPageInfo;
    }


    @Override
    public void deleteResourceByGitOps(Long resourceId) {

        DevopsCustomizeResourceDTO devopsCustomizeResourceDTO = devopsCustomizeResourceMapper.selectByPrimaryKey(resourceId);

        DevopsEnvironmentDTO devopsEnvironmentDTO = devopsEnvironmentService.baseQueryById(devopsCustomizeResourceDTO.getEnvId());
        clusterConnectionHandler.checkEnvConnection(devopsEnvironmentDTO.getClusterId());

        devopsEnvCommandService.baseListByObject(ObjectType.CUSTOM.getType(), resourceId).forEach(devopsEnvCommandE -> devopsEnvCommandService.baseDeleteByEnvCommandId(devopsEnvCommandE));
        devopsCustomizeResourceMapper.deleteByPrimaryKey(resourceId);
        devopsCustomizeResourceContentService.baseDelete(devopsCustomizeResourceDTO.getContentId());
    }


    private void handleCustomResource(Long projectId, Long envId, String content, String kind, String name, String type, Long resourceId, String filePath, Long userId) {

        if (CREATE.equals(type)) {

            //校验新增的类型是否已存在
            checkExist(envId, kind, name);

            //创建自定义资源的yaml文件内容
            DevopsCustomizeResourceContentDTO devopsCustomizeResourceContentE = new DevopsCustomizeResourceContentDTO();
            devopsCustomizeResourceContentE.setContent(content);

            //自定义资源关联command
            DevopsEnvCommandDTO devopsEnvCommandDTO = initDevopsEnvCommandDTO(type, userId);
            devopsEnvCommandDTO = devopsEnvCommandService.baseCreate(devopsEnvCommandDTO);

            //创建自定义资源
            DevopsCustomizeResourceDTO devopsCustomizeResourceDTO = new DevopsCustomizeResourceDTO(projectId, envId, devopsCustomizeResourceContentService.baseCreate(devopsCustomizeResourceContentE).getId(), devopsEnvCommandDTO.getId(), kind, name, filePath, userId);

            devopsCustomizeResourceDTO = baseCreate(devopsCustomizeResourceDTO);

            devopsEnvCommandDTO.setObjectId(devopsCustomizeResourceDTO.getId());
            devopsEnvCommandService.baseUpdate(devopsEnvCommandDTO);
        } else if (UPDATE.equals(type)) {
            DevopsCustomizeResourceDTO devopsCustomizeResourceDTO = devopsCustomizeResourceMapper.selectByPrimaryKey(resourceId);
            devopsCustomizeResourceDTO.setLastUpdatedBy(userId);
            if (kind == null) {
                throw new CommonException("error.custom.resource.kind.null");
            }
            if (!kind.equals(devopsCustomizeResourceDTO.getK8sKind())) {
                throw new CommonException("error.custom.resource.kind.modify");
            }
            if (!name.equals(devopsCustomizeResourceDTO.getName())) {
                throw new CommonException("error.custom.resource.name.modify");
            }

            //更新自定义资源的yaml文件内容
            DevopsCustomizeResourceContentDTO devopsCustomizeResourceContentDTO = devopsCustomizeResourceContentService.baseQuery(devopsCustomizeResourceDTO.getContentId());
            devopsCustomizeResourceContentDTO.setContent(content);
            devopsCustomizeResourceContentService.baseUpdate(devopsCustomizeResourceContentDTO);

            //自定义资源关联command
            DevopsEnvCommandDTO devopsEnvCommandDTO = initDevopsEnvCommandDTO(type, userId);
            devopsEnvCommandDTO.setObjectId(devopsCustomizeResourceDTO.getId());
            devopsEnvCommandDTO = devopsEnvCommandService.baseCreate(devopsEnvCommandDTO);


            //更新自定义资源关联的最新command
            devopsCustomizeResourceDTO.setCommandId(devopsEnvCommandDTO.getId());
            baseUpdate(devopsCustomizeResourceDTO);

        } else {
            DevopsCustomizeResourceDTO devopsCustomizeResourceDTO = devopsCustomizeResourceMapper.selectByPrimaryKey(resourceId);

            //自定义资源关联command
            DevopsEnvCommandDTO devopsEnvCommandDTO = initDevopsEnvCommandDTO(DELETE, userId);
            devopsEnvCommandDTO.setObjectId(resourceId);
            devopsEnvCommandDTO = devopsEnvCommandService.baseCreate(devopsEnvCommandDTO);

            //更新自定义资源关联的最新command
            devopsCustomizeResourceDTO.setCommandId(devopsEnvCommandDTO.getId());
            baseUpdate(devopsCustomizeResourceDTO);
        }
    }


    private DevopsEnvCommandDTO initDevopsEnvCommandDTO(String type, Long userId) {
        DevopsEnvCommandDTO devopsEnvCommandDTO = new DevopsEnvCommandDTO();
        switch (type) {
            case CREATE:
                devopsEnvCommandDTO.setCommandType(CommandType.CREATE.getType());
                break;
            case UPDATE:
                devopsEnvCommandDTO.setCommandType(CommandType.UPDATE.getType());
                break;
            default:
                devopsEnvCommandDTO.setCommandType(CommandType.DELETE.getType());
                break;
        }
        devopsEnvCommandDTO.setCreatedBy(userId);
        devopsEnvCommandDTO.setObject(ObjectType.CUSTOM.getType());
        devopsEnvCommandDTO.setStatus(CommandStatus.OPERATING.getStatus());
        return devopsEnvCommandDTO;
    }


    private String checkResource(LinkedHashMap metadata, Object kind) {
        if (kind == null) {
            throw new CommonException("custom.resource.kind.not.found");
        }
        //禁止创建平台已有的资源
        if (RESOURCE_TYPE.contains(kind.toString())) {
            throw new CommonException("error.kind.is.forbidden");
        }
        if (metadata == null) {
            throw new CommonException("custom.resource.metadata.not.found");
        }

        Object name = metadata.get("name");
        if (name == null) {
            throw new CommonException("custom.resource.name.not.found");
        }
        return name.toString();
    }


    @Override
    public DevopsCustomizeResourceDTO baseCreate(DevopsCustomizeResourceDTO devopsCustomizeResourceDTO) {
        if (devopsCustomizeResourceMapper.insert(devopsCustomizeResourceDTO) != 1) {
            throw new CommonException("error.customize.resource.create.error");
        }
        return devopsCustomizeResourceDTO;
    }

    @Override
    public DevopsCustomizeResourceDTO baseQuery(Long resourceId) {
        return devopsCustomizeResourceMapper.selectByPrimaryKey(resourceId);
    }

    @Override
    public void baseUpdate(DevopsCustomizeResourceDTO devopsCustomizeResourceDTO) {
        devopsCustomizeResourceDTO.setObjectVersionNumber(devopsCustomizeResourceMapper.selectByPrimaryKey(devopsCustomizeResourceDTO.getId()).getObjectVersionNumber());
        if (devopsCustomizeResourceMapper.updateByPrimaryKey(devopsCustomizeResourceDTO) != 1) {
            throw new CommonException("error.customize.resource.update.error");
        }
    }

    @Override
    public List<DevopsCustomizeResourceDTO> listByEnvAndFilePath(Long envId, String filePath) {
        DevopsCustomizeResourceDTO devopsCustomizeResourceDTO = new DevopsCustomizeResourceDTO();
        devopsCustomizeResourceDTO.setEnvId(envId);
        devopsCustomizeResourceDTO.setFilePath(filePath);
        return devopsCustomizeResourceMapper.select(devopsCustomizeResourceDTO);
    }

    @Override
    public DevopsCustomizeResourceDTO queryByEnvIdAndKindAndName(Long envId, String kind, String name) {
        DevopsCustomizeResourceDTO devopsCustomizeResourceDO = new DevopsCustomizeResourceDTO(envId, kind, name);
        return devopsCustomizeResourceMapper.selectOne(devopsCustomizeResourceDO);
    }

    @Override
    public PageInfo<DevopsCustomizeResourceDTO> pageDevopsCustomizeResourceE(Long envId, Pageable pageable, String params) {
        Map maps = TypeUtil.castMapParams(params);
        return PageHelper.startPage(pageable.getPageNumber(), pageable.getPageSize(), PageRequestUtil.getOrderBy(pageable))
                .doSelectPageInfo(() -> devopsCustomizeResourceMapper.pageResources(envId,
                        maps == null ? null : TypeUtil.cast(maps.get(TypeUtil.SEARCH_PARAM)),
                        maps == null ? null : TypeUtil.cast(maps.get(TypeUtil.PARAMS))));
    }

    @Override
    public void checkExist(Long envId, String kind, String name) {
        DevopsCustomizeResourceDTO devopsCustomizeResourceDTO = new DevopsCustomizeResourceDTO(envId, kind, name);
        if (devopsCustomizeResourceMapper.selectOne(devopsCustomizeResourceDTO) != null) {
            throw new CommonException("error.kind.name.exist");
        }
    }

    @Override
    public List<DevopsCustomizeResourceDTO> baseListByEnvId(Long envId) {
        DevopsCustomizeResourceDTO devopsCustomizeResourceDTO = new DevopsCustomizeResourceDTO();
        devopsCustomizeResourceDTO.setEnvId(envId);
        return devopsCustomizeResourceMapper.select(devopsCustomizeResourceDTO);
    }

    @Override
    public void baseDeleteCustomizeResourceByEnvId(Long envId) {
        DevopsCustomizeResourceDTO devopsCustomizeResourceDTO = new DevopsCustomizeResourceDTO();
        devopsCustomizeResourceDTO.setEnvId(envId);
        List<Long> resourceContentIds = devopsCustomizeResourceMapper.select(devopsCustomizeResourceDTO).stream()
                .map(DevopsCustomizeResourceDTO::getContentId).collect(Collectors.toList());
        devopsCustomizeResourceMapper.delete(devopsCustomizeResourceDTO);
        if (!resourceContentIds.isEmpty()) {
            devopsCustomizeResourceContentService.baseDeleteByContentIds(resourceContentIds);
        }
    }
}
