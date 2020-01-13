package io.choerodon.devops.app.service.impl;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.data.domain.Pageable;
import io.choerodon.core.exception.CommonException;
import io.choerodon.devops.api.vo.AppServiceShareRuleVO;
import io.choerodon.devops.app.service.AppServiceShareRuleService;
import io.choerodon.devops.infra.dto.AppServiceShareRuleDTO;
import io.choerodon.devops.infra.dto.iam.ProjectDTO;
import io.choerodon.devops.infra.feign.operator.BaseServiceClientOperator;
import io.choerodon.devops.infra.mapper.AppServiceShareRuleMapper;
import io.choerodon.devops.infra.util.ConvertUtils;
import io.choerodon.devops.infra.util.PageRequestUtil;
import io.choerodon.devops.infra.util.TypeUtil;


/**
 * Created by ernst on 2018/5/12.
 */
@Service
public class AppServiceShareRuleServiceImpl implements AppServiceShareRuleService {

    @Value("${services.gitlab.url}")
    private String gitlabUrl;
    @Value("${services.helm.url}")
    private String helmUrl;

    @Autowired
    private BaseServiceClientOperator baseServiceClientOperator;
    @Autowired
    private AppServiceShareRuleMapper appServiceShareRuleMapper;
    private static final String PROJECT_NAME = "组织下所有项目";

    @Override
    @Transactional
    public AppServiceShareRuleVO createOrUpdate(Long projectId, AppServiceShareRuleVO appServiceShareRuleVO) {
        AppServiceShareRuleDTO appServiceShareRuleDTO = ConvertUtils.convertObject(appServiceShareRuleVO, AppServiceShareRuleDTO.class);
        if (appServiceShareRuleDTO.getVersion() != null && appServiceShareRuleDTO.getVersionType() != null) {
            appServiceShareRuleDTO.setVersionType(null);
        }
        if (appServiceShareRuleDTO.getId() == null) {
            int count = appServiceShareRuleMapper.selectCount(appServiceShareRuleDTO);
            if (count > 0) {
                throw new CommonException("error.share.rule.already.exist");
            }
            if (appServiceShareRuleMapper.insert(appServiceShareRuleDTO) != 1) {
                throw new CommonException("error.insert.application.share.rule.insert");
            }
        } else {
            AppServiceShareRuleDTO oldappServiceShareRuleDTO = appServiceShareRuleMapper.selectByPrimaryKey(appServiceShareRuleDTO.getId());
            appServiceShareRuleDTO.setObjectVersionNumber(oldappServiceShareRuleDTO.getObjectVersionNumber());
            if (appServiceShareRuleMapper.updateByPrimaryKey(appServiceShareRuleDTO) != 1) {
                throw new CommonException("error.insert.application.share.rule.update");
            }
        }
        return ConvertUtils.convertObject(appServiceShareRuleDTO, AppServiceShareRuleVO.class);
    }

    @Override
    public PageInfo<AppServiceShareRuleVO> pageByOptions(Long projectId, Long appServiceId, Pageable pageable, String params) {
        Map<String, Object> mapParams = TypeUtil.castMapParams(params);
        PageInfo<AppServiceShareRuleDTO> devopsProjectConfigDTOPageInfo = PageHelper.startPage(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                PageRequestUtil.getOrderBy(pageable)).doSelectPageInfo(
                () -> appServiceShareRuleMapper.listByOptions(appServiceId,
                        TypeUtil.cast(mapParams.get(TypeUtil.SEARCH_PARAM)),
                        TypeUtil.cast(mapParams.get(TypeUtil.PARAMS))));
        PageInfo<AppServiceShareRuleVO> shareRuleVOPageInfo = ConvertUtils.convertPage(devopsProjectConfigDTOPageInfo, AppServiceShareRuleVO.class);
        List<AppServiceShareRuleVO> appServiceShareRuleVOS = shareRuleVOPageInfo.getList().stream().peek(t -> {
            if (t.getProjectId() != null) {
                ProjectDTO projectDTO = baseServiceClientOperator.queryIamProjectById(t.getProjectId());
                t.setProjectName(projectDTO.getName());
            }
        }).collect(Collectors.toList());
        shareRuleVOPageInfo.setList(appServiceShareRuleVOS);
        return shareRuleVOPageInfo;
    }

    @Override
    public AppServiceShareRuleVO query(Long projectId, Long ruleId) {
        AppServiceShareRuleVO appServiceShareRuleVO = ConvertUtils.convertObject(appServiceShareRuleMapper.selectByPrimaryKey(ruleId), AppServiceShareRuleVO.class);
        if (appServiceShareRuleVO.getProjectId() == null) {
            appServiceShareRuleVO.setProjectName(PROJECT_NAME);
        } else {
            appServiceShareRuleVO.setProjectName(baseServiceClientOperator.queryIamProjectById(appServiceShareRuleVO.getProjectId()).getName());
        }
        return appServiceShareRuleVO;
    }

    @Override
    public void delete(Long ruleId) {
        appServiceShareRuleMapper.deleteByPrimaryKey(ruleId);
    }

}
