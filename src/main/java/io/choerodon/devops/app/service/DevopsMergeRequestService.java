package io.choerodon.devops.app.service;

import java.util.List;

import com.github.pagehelper.PageInfo;
import org.springframework.data.domain.Pageable;
import io.choerodon.devops.api.vo.DevopsMergeRequestVO;
import io.choerodon.devops.infra.dto.DevopsMergeRequestDTO;

/**
 * Created by Sheep on 2019/7/15.
 */
public interface DevopsMergeRequestService {

    List<DevopsMergeRequestDTO> baseListBySourceBranch(String sourceBranchName, Long gitLabProjectId);

    DevopsMergeRequestDTO baseQueryByAppIdAndMergeRequestId(Long projectId, Long gitlabMergeRequestId);

    PageInfo<DevopsMergeRequestDTO> basePageByOptions(Integer gitlabProjectId, String state, Pageable pageable);

    List<DevopsMergeRequestDTO> baseQueryByGitlabProjectId(Integer gitlabProjectId);

    Integer baseUpdate(DevopsMergeRequestDTO devopsMergeRequestDTO);

    void create(DevopsMergeRequestVO devopsMergeRequestVO);

    void baseCreate(DevopsMergeRequestVO devopsMergeRequestVO);

    DevopsMergeRequestDTO baseCountMergeRequest(Integer gitlabProjectId);
}
