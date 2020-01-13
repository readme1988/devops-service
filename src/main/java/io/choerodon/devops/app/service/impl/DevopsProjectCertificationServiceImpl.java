package io.choerodon.devops.app.service.impl;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import io.choerodon.core.exception.CommonException;
import io.choerodon.devops.api.vo.ProjectCertificationPermissionUpdateVO;
import io.choerodon.devops.api.vo.ProjectCertificationVO;
import io.choerodon.devops.api.vo.ProjectReqVO;
import io.choerodon.devops.app.service.CertificationService;
import io.choerodon.devops.app.service.DevopsCertificationProRelationshipService;
import io.choerodon.devops.app.service.DevopsProjectCertificationService;
import io.choerodon.devops.infra.dto.CertificationDTO;
import io.choerodon.devops.infra.dto.CertificationFileDTO;
import io.choerodon.devops.infra.dto.DevopsCertificationProRelationshipDTO;
import io.choerodon.devops.infra.dto.iam.OrganizationDTO;
import io.choerodon.devops.infra.dto.iam.ProjectDTO;
import io.choerodon.devops.infra.feign.operator.BaseServiceClientOperator;
import io.choerodon.devops.infra.mapper.DevopsCertificationFileMapper;
import io.choerodon.devops.infra.mapper.DevopsCertificationMapper;
import io.choerodon.devops.infra.util.*;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class DevopsProjectCertificationServiceImpl implements DevopsProjectCertificationService {
    private static final String CREATE = "create";
    private static final String UPDATE = "update";
    private static final String FILE_SEPARATOR = System.getProperty("file.separator");
    private static final String ERROR_CERTIFICATION_NOT_EXIST = "error.certification.not.exist";

    private Gson gson = new Gson();
    @Autowired
    private DevopsCertificationProRelationshipService devopsCertificationProRelationshipService;
    @Autowired
    private BaseServiceClientOperator baseServiceClientOperator;
    @Autowired
    private CertificationService certificationService;
    @Autowired
    private DevopsCertificationMapper devopsCertificationMapper;
    @Autowired
    private DevopsCertificationFileMapper devopsCertificationFileMapper;

    @Transactional(propagation = Propagation.REQUIRED)
    @Override
    public void assignPermission(ProjectCertificationPermissionUpdateVO update) {
        CertificationDTO certificationDTO = certificationService.baseQueryById(update.getCertificationId());
        if (certificationDTO == null) {
            throw new CommonException(ERROR_CERTIFICATION_NOT_EXIST, update.getCertificationId());
        }

        if (certificationDTO.getProjectId() == null) {
            throw new CommonException("error.not.project.certification", update.getCertificationId());
        }

        if (certificationDTO.getSkipCheckProjectPermission()) {
            if (update.getSkipCheckProjectPermission()) {
                // 原来跳过，现在也跳过，不处理
                return;
            } else {
                // 原来跳过，现在不跳过，先更新字段，然后插入关联关系
                updateSkipPermissionCheck(
                        update.getCertificationId(),
                        update.getSkipCheckProjectPermission(),
                        update.getObjectVersionNumber());

                devopsCertificationProRelationshipService.batchInsertIgnore(
                        update.getCertificationId(),
                        update.getProjectIds());
            }
        } else {
            // 原来不跳过，现在跳过，更新证书权限字段，再删除所有数据库中与该证书有关的关联关系
            if (update.getSkipCheckProjectPermission()) {
                updateSkipPermissionCheck(
                        update.getCertificationId(),
                        update.getSkipCheckProjectPermission(),
                        update.getObjectVersionNumber());

                devopsCertificationProRelationshipService.baseDeleteByCertificationId(update.getCertificationId());
            } else {
                // 原来不跳过，现在也不跳过，批量添加权限
                devopsCertificationProRelationshipService.batchInsertIgnore(
                        update.getCertificationId(),
                        update.getProjectIds());
            }
        }
    }

    @Override
    public PageInfo<ProjectReqVO> pageRelatedProjects(Long projectId, Long certId, Pageable pageable, String params) {
        CertificationDTO certificationDTO = certificationService.baseQueryById(certId);
        if (certificationDTO == null) {
            throw new CommonException(ERROR_CERTIFICATION_NOT_EXIST, certId);
        }

        Map<String, Object> map = TypeUtil.castMapParams(params);
        List<String> paramList = TypeUtil.cast(map.get(TypeUtil.PARAMS));
        Map<String, Object> searchParamsMap = TypeUtil.cast(map.get(TypeUtil.SEARCH_PARAM));
        String name = null;
        String code = null;
        if (!CollectionUtils.isEmpty(searchParamsMap)) {
            name = TypeUtil.cast(searchParamsMap.get("name"));
            code = TypeUtil.cast(searchParamsMap.get("code"));
        }
        if (CollectionUtils.isEmpty(paramList) && StringUtils.isEmpty(name) && StringUtils.isEmpty(code)) {
            // 如果不搜索，在数据库中进行分页
            PageInfo<DevopsCertificationProRelationshipDTO> relationPage = PageHelper.startPage(
                    pageable.getPageNumber(), pageable.getPageSize())
                    .doSelectPageInfo(() -> devopsCertificationProRelationshipService.baseListByCertificationId(certId));
            return ConvertUtils.convertPage(relationPage, permission -> {
                ProjectDTO projectDTO = baseServiceClientOperator.queryIamProjectById(permission.getProjectId());
                return new ProjectReqVO(permission.getProjectId(), projectDTO.getName(), projectDTO.getCode());
            });
        } else {
            // 如果要搜索，需要手动在程序内分页
            ProjectDTO iamProjectDTO = baseServiceClientOperator.queryIamProjectById(projectId);

            // 手动查出所有组织下的项目
            List<ProjectDTO> filteredProjects = baseServiceClientOperator.listIamProjectByOrgId(
                    iamProjectDTO.getOrganizationId(),
                    name, code,
                    paramList.get(0));

            // 数据库中的有权限的项目
            List<Long> permissions = devopsCertificationProRelationshipService.baseListByCertificationId(certId)
                    .stream()
                    .map(DevopsCertificationProRelationshipDTO::getProjectId)
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

    /**
     * 更新证书的权限校验字段
     *
     * @param certId              证书id
     * @param skipCheckPermission 是否跳过权限校验
     * @param objectVersionNumber 版本号
     */
    private void updateSkipPermissionCheck(Long certId, Boolean skipCheckPermission, Long objectVersionNumber) {
        CertificationDTO toUpdate = new CertificationDTO();
        toUpdate.setId(certId);
        toUpdate.setObjectVersionNumber(objectVersionNumber);
        toUpdate.setSkipCheckProjectPermission(skipCheckPermission);
        devopsCertificationMapper.updateByPrimaryKeySelective(toUpdate);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createOrUpdate(Long projectId, MultipartFile key, MultipartFile cert, ProjectCertificationVO projectCertificationVO) {
        ProjectDTO projectDTO = baseServiceClientOperator.queryIamProjectById(projectId);
        OrganizationDTO organizationDTO = baseServiceClientOperator.queryOrganizationById(projectDTO.getOrganizationId());
        String path = String.format("tmp%s%s%s%s", FILE_SEPARATOR, organizationDTO.getCode(), FILE_SEPARATOR, GenerateUUID.generateUUID().substring(0, 5));
        String certFileName;
        String keyFileName;
        //如果是选择上传文件方式
        if (key != null && cert != null) {
            certFileName = cert.getOriginalFilename();
            keyFileName = key.getOriginalFilename();
            projectCertificationVO.setKeyValue(FileUtil.getFileContent(new File(FileUtil.multipartFileToFile(path, key))));
            projectCertificationVO.setCertValue(FileUtil.getFileContent(new File(FileUtil.multipartFileToFile(path, cert))));
        } else {
            certFileName = String.format("%s.%s", GenerateUUID.generateUUID().substring(0, 5), "crt");
            keyFileName = String.format("%s.%s", GenerateUUID.generateUUID().substring(0, 5), "key");
            FileUtil.saveDataToFile(path, certFileName, projectCertificationVO.getCertValue());
            FileUtil.saveDataToFile(path, keyFileName, projectCertificationVO.getKeyValue());
        }
        File certPath = new File(path + FILE_SEPARATOR + certFileName);
        File keyPath = new File(path + FILE_SEPARATOR + keyFileName);
        try {
            SslUtil.validate(certPath, keyPath);
        } catch (Exception e) {
            FileUtil.deleteFile(certPath);
            FileUtil.deleteFile(keyPath);
            throw new CommonException(e.getMessage());
        }

        FileUtil.deleteFile(certPath);
        FileUtil.deleteFile(keyPath);

        if (projectCertificationVO.getType().equals(CREATE)) {
            CertificationDTO certificationDTO = new CertificationDTO();
            certificationDTO.setName(projectCertificationVO.getName());
            certificationDTO.setProjectId(projectId);
            // 创建项目层证书需要组织id
            certificationDTO.setOrganizationId(organizationDTO.getId());
            certificationDTO.setSkipCheckProjectPermission(true);
            certificationDTO.setDomains(gson.toJson(Collections.singletonList(projectCertificationVO.getDomain())));
            certificationDTO.setCertificationFileId(certificationService.baseStoreCertFile(new CertificationFileDTO(projectCertificationVO.getCertValue(), projectCertificationVO.getKeyValue())));
            certificationService.baseCreate(certificationDTO);
        } else {
            CertificationDTO certificationDTO = new CertificationDTO();
            BeanUtils.copyProperties(projectCertificationVO, certificationDTO);
            certificationDTO.setProjectId(projectId);
            certificationDTO.setDomains(gson.toJson(Collections.singletonList(projectCertificationVO.getDomain())));
            devopsCertificationMapper.updateByPrimaryKeySelective(certificationDTO);

            CertificationFileDTO certificationFileDTO = devopsCertificationFileMapper.queryByCertificationId(certificationDTO.getId());
            certificationFileDTO.setKeyFile(certificationDTO.getKeyValue());
            certificationFileDTO.setCertFile(certificationDTO.getCertValue());
            devopsCertificationFileMapper.updateByPrimaryKeySelective(certificationFileDTO);
        }
    }


    @Override
    public void checkName(Long projectId, String name) {
        if (certificationService.baseQueryByProjectAndName(projectId, name) != null) {
            throw new CommonException("error.cert.name.exist");
        }
    }

    @Override
    public PageInfo<ProjectReqVO> listNonRelatedMembers(Long projectId, Long certId, Long selectedProjectId, Pageable pageable, String params) {
        CertificationDTO certificationDTO = certificationService.baseQueryById(certId);
        if (certificationDTO == null) {
            throw new CommonException(ERROR_CERTIFICATION_NOT_EXIST, certId);
        }

        Map<String, String> searchParamMap = new HashMap<>();
        List<String> paramList = new ArrayList<>();
        if (!StringUtils.isEmpty(params)) {
            Map maps = gson.fromJson(params, Map.class);
            searchParamMap = Optional.ofNullable((Map) TypeUtil.cast(maps.get(TypeUtil.SEARCH_PARAM))).orElse(new HashMap<>());
            paramList = Optional.ofNullable((List) TypeUtil.cast(maps.get(TypeUtil.PARAMS))).orElse(new ArrayList<>());
        }
        //查询出该项目所属组织下的所有项目
        ProjectDTO iamProjectDTO = baseServiceClientOperator.queryIamProjectById(projectId);
        OrganizationDTO organizationDTO = baseServiceClientOperator.queryOrganizationById(iamProjectDTO.getOrganizationId());
        List<ProjectDTO> projectDTOList = baseServiceClientOperator.listIamProjectByOrgId(organizationDTO.getId(),
                searchParamMap.get("name"),
                searchParamMap.get("code"),
                CollectionUtils.isEmpty(paramList) ? null : paramList.get(0));

        //查询已经分配权限的项目
        List<Long> permitted = devopsCertificationProRelationshipService.baseListByCertificationId(certId)
                .stream()
                .map(DevopsCertificationProRelationshipDTO::getProjectId)
                .collect(Collectors.toList());

        //把组织下有权限的项目过滤掉再返回
        List<ProjectReqVO> nonRelatedMembers = projectDTOList.stream()
                .filter(i -> !permitted.contains(i.getId()))
                .map(i -> new ProjectReqVO(i.getId(), i.getName(), i.getCode()))
                .collect(Collectors.toList());

        if (selectedProjectId != null) {
            ProjectDTO selectedProjectDTO = baseServiceClientOperator.queryIamProjectById(selectedProjectId);
            ProjectReqVO projectReqVO = new ProjectReqVO(selectedProjectDTO.getId(), selectedProjectDTO.getName(), selectedProjectDTO.getCode());
            if (!nonRelatedMembers.isEmpty()) {
                nonRelatedMembers.remove(projectReqVO);
                nonRelatedMembers.add(0, projectReqVO);
            } else {
                nonRelatedMembers.add(projectReqVO);
            }
        }

        return PageInfoUtil.createPageFromList(nonRelatedMembers, pageable);
    }

    @Override
    public void deleteCert(Long certId) {
        if (certificationService.baseQueryById(certId) == null) {
            return;
        }

        List<CertificationDTO> certificationDTOS = certificationService.baseListByOrgCertId(certId);
        if (certificationDTOS.isEmpty()) {
            devopsCertificationProRelationshipService.baseDeleteByCertificationId(certId);
            certificationService.baseDeleteById(certId);
        } else {
            throw new CommonException("error.cert.related");
        }
    }

    @Override
    public void deletePermissionOfProject(Long projectId, Long certId) {
        if (projectId == null || certId == null) {
            return;
        }
        DevopsCertificationProRelationshipDTO devopsCertificationProRelationshipDTO = new DevopsCertificationProRelationshipDTO();
        devopsCertificationProRelationshipDTO.setProjectId(projectId);
        devopsCertificationProRelationshipDTO.setCertId(certId);
        devopsCertificationProRelationshipService.baseDelete(devopsCertificationProRelationshipDTO);
    }

    @Override
    public PageInfo<ProjectCertificationVO> pageCerts(Long projectId, Pageable pageable,
                                                      String params) {
        PageInfo<CertificationDTO> certificationDTOS = certificationService
                .basePage(projectId, null, pageable, params);
        PageInfo<ProjectCertificationVO> orgCertificationDTOS = new PageInfo<>();
        BeanUtils.copyProperties(certificationDTOS, orgCertificationDTOS);
        List<ProjectCertificationVO> orgCertifications = new ArrayList<>();

        if (!certificationDTOS.getList().isEmpty()) {
            certificationDTOS.getList().forEach(certificationDTO -> {
                List<String> stringList = gson.fromJson(certificationDTO.getDomains(), new TypeToken<List<String>>() {
                }.getType());
                ProjectCertificationVO orgCertificationVO = new ProjectCertificationVO(certificationDTO.getId(), certificationDTO.getName(), stringList.get(0), certificationDTO.getSkipCheckProjectPermission(), certificationDTO.getObjectVersionNumber());
                orgCertifications.add(orgCertificationVO);
            });
        }
        orgCertificationDTOS.setList(orgCertifications);
        return orgCertificationDTOS;
    }

    @Override
    public ProjectCertificationVO queryCert(Long certId) {
        CertificationDTO certificationDTO = devopsCertificationMapper.queryById(certId);
        List<String> stringList = gson.fromJson(certificationDTO.getDomains(), new TypeToken<List<String>>() {
        }.getType());
        return new ProjectCertificationVO(certificationDTO.getId(), certificationDTO.getName(), stringList.get(0), certificationDTO.getSkipCheckProjectPermission(), certificationDTO.getObjectVersionNumber(), certificationDTO.getKeyValue(), certificationDTO.getCertValue());
    }
}
