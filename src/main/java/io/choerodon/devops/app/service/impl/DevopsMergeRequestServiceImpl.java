package io.choerodon.devops.app.service.impl;

import java.util.List;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import io.choerodon.core.exception.CommonException;
import io.choerodon.core.oauth.DetailsHelper;
import io.choerodon.devops.api.vo.DevopsMergeRequestVO;
import io.choerodon.devops.app.service.DevopsMergeRequestService;
import io.choerodon.devops.app.service.SendNotificationService;
import io.choerodon.devops.infra.dto.DevopsMergeRequestDTO;
import io.choerodon.devops.infra.enums.MergeRequestState;
import io.choerodon.devops.infra.mapper.DevopsMergeRequestMapper;
import io.choerodon.devops.infra.util.PageRequestUtil;
import io.choerodon.devops.infra.util.TypeUtil;

/**
 * Created by Sheep on 2019/7/15.
 */
@Service
public class DevopsMergeRequestServiceImpl implements DevopsMergeRequestService {
    private static final Logger LOGGER = LoggerFactory.getLogger(DevopsMergeRequestServiceImpl.class);
    @Autowired
    private DevopsMergeRequestMapper devopsMergeRequestMapper;
    @Autowired
    private SendNotificationService sendNotificationService;

    @Override
    public List<DevopsMergeRequestDTO> baseListBySourceBranch(String sourceBranchName, Long gitLabProjectId) {
        return devopsMergeRequestMapper.listBySourceBranch(gitLabProjectId.intValue(), sourceBranchName);
    }

    @Override
    public DevopsMergeRequestDTO baseQueryByAppIdAndMergeRequestId(Long projectId, Long gitlabMergeRequestId) {
        DevopsMergeRequestDTO devopsMergeRequestDTO = new DevopsMergeRequestDTO();
        devopsMergeRequestDTO.setGitlabProjectId(projectId);
        devopsMergeRequestDTO.setGitlabMergeRequestId(gitlabMergeRequestId);
        return devopsMergeRequestMapper
                .selectOne(devopsMergeRequestDTO);
    }

    @Override
    public PageInfo<DevopsMergeRequestDTO> basePageByOptions(Integer gitlabProjectId, String state, Pageable pageable) {
        // 如果传入的state字段是这个值，表明的是查询待这个用户审核的MergeRequest
        if ("assignee".equals(state)) {
            return PageHelper.startPage(pageable.getPageNumber(), pageable.getPageSize(), PageRequestUtil.getOrderBy(pageable)).doSelectPageInfo(() ->
                    devopsMergeRequestMapper.listToBeAuditedByThisUser(gitlabProjectId, DetailsHelper.getUserDetails() == null ? 0L : DetailsHelper.getUserDetails().getUserId()));
        } else {
            // 否则的话按照state字段查询
            DevopsMergeRequestDTO devopsMergeRequestDTO = new DevopsMergeRequestDTO();
            devopsMergeRequestDTO.setGitlabProjectId(gitlabProjectId.longValue());
            devopsMergeRequestDTO.setState(state);
            return PageHelper.startPage(pageable.getPageNumber(), pageable.getPageSize(), PageRequestUtil.getOrderBy(pageable)).doSelectPageInfo(() ->
                    devopsMergeRequestMapper.select(devopsMergeRequestDTO));
        }
    }

    @Override
    public List<DevopsMergeRequestDTO> baseQueryByGitlabProjectId(Integer gitlabProjectId) {
        DevopsMergeRequestDTO devopsMergeRequestDTO = new DevopsMergeRequestDTO();
        devopsMergeRequestDTO.setGitlabProjectId(gitlabProjectId.longValue());
        return devopsMergeRequestMapper
                .select(devopsMergeRequestDTO);
    }

    @Override
    public Integer baseUpdate(DevopsMergeRequestDTO devopsMergeRequestDTO) {
        return devopsMergeRequestMapper.updateByPrimaryKey(devopsMergeRequestDTO);
    }

    @Override
    public void create(DevopsMergeRequestVO devopsMergeRequestVO) {
        baseCreate(devopsMergeRequestVO);
    }

    @Override
    public void baseCreate(DevopsMergeRequestVO devopsMergeRequestVO) {
        DevopsMergeRequestDTO devopsMergeRequestDTO = voToDto(devopsMergeRequestVO);
        Long gitlabProjectId = devopsMergeRequestDTO.getGitlabProjectId();
        Long gitlabMergeRequestId = devopsMergeRequestDTO.getGitlabMergeRequestId();
        DevopsMergeRequestDTO mergeRequestETemp = baseQueryByAppIdAndMergeRequestId(gitlabProjectId, gitlabMergeRequestId);
        Long mergeRequestId = mergeRequestETemp != null ? mergeRequestETemp.getId() : null;
        if (mergeRequestId == null) {
            try {
                devopsMergeRequestMapper.insert(devopsMergeRequestDTO);
            } catch (Exception e) {
                LOGGER.info("error.save.merge.request");
            }
        } else {
            devopsMergeRequestDTO.setId(mergeRequestId);
            devopsMergeRequestDTO.setObjectVersionNumber(mergeRequestETemp.getObjectVersionNumber());
            if (baseUpdate(devopsMergeRequestDTO) == 0) {
                throw new CommonException("error.update.merge.request");
            }
        }

        // 发送关于Merge Request的相关通知
        String operatorUserLoginName = devopsMergeRequestVO.getUser() == null ? null : devopsMergeRequestVO.getUser().getUsername();
        Integer gitProjectId = TypeUtil.objToInteger(gitlabProjectId);
        if (MergeRequestState.OPENED.getValue().equals(devopsMergeRequestDTO.getState())) {
            sendNotificationService.sendWhenMergeRequestAuditEvent(gitProjectId, devopsMergeRequestDTO.getGitlabMergeRequestId());
        } else if (MergeRequestState.CLOSED.getValue().equals(devopsMergeRequestDTO.getState())) {
            sendNotificationService.sendWhenMergeRequestClosed(gitProjectId, devopsMergeRequestDTO.getGitlabMergeRequestId(), operatorUserLoginName);
        } else if (MergeRequestState.MERGED.getValue().equals(devopsMergeRequestDTO.getState())) {
            sendNotificationService.sendWhenMergeRequestPassed(gitProjectId, devopsMergeRequestDTO.getGitlabMergeRequestId(), operatorUserLoginName);
        }
    }

    @Override
    public DevopsMergeRequestDTO baseCountMergeRequest(Integer gitlabProjectId) {
        return devopsMergeRequestMapper.countMergeRequest(gitlabProjectId, DetailsHelper.getUserDetails() == null ? 0L : DetailsHelper.getUserDetails().getUserId());
    }

    private DevopsMergeRequestDTO voToDto(DevopsMergeRequestVO devopsMergeRequestVO) {
        DevopsMergeRequestDTO devopsMergeRequestDTO = new DevopsMergeRequestDTO();
        devopsMergeRequestDTO.setGitlabProjectId(devopsMergeRequestVO.getProject().getId());
        devopsMergeRequestDTO.setGitlabMergeRequestId(devopsMergeRequestVO.getObjectAttributes().getIid());
        devopsMergeRequestDTO.setSourceBranch(devopsMergeRequestVO.getObjectAttributes().getSourceBranch());
        devopsMergeRequestDTO.setTargetBranch(devopsMergeRequestVO.getObjectAttributes().getTargetBranch());
        devopsMergeRequestDTO.setAuthorId(devopsMergeRequestVO.getObjectAttributes().getAuthorId());
        devopsMergeRequestDTO.setAssigneeId(devopsMergeRequestVO.getObjectAttributes().getAssigneeId());
        devopsMergeRequestDTO.setState(devopsMergeRequestVO.getObjectAttributes().getState());
        devopsMergeRequestDTO.setTitle(devopsMergeRequestVO.getObjectAttributes().getTitle());
        devopsMergeRequestDTO.setCreatedAt(devopsMergeRequestVO.getObjectAttributes().getCreatedAt());
        devopsMergeRequestDTO.setUpdatedAt(devopsMergeRequestVO.getObjectAttributes().getUpdatedAt());
        return devopsMergeRequestDTO;
    }
}
