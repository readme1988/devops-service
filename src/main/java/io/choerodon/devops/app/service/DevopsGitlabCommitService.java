package io.choerodon.devops.app.service;

import java.util.Date;
import java.util.List;
import java.util.Map;

import com.github.pagehelper.PageInfo;
import org.springframework.data.domain.Pageable;
import io.choerodon.devops.api.vo.CommitFormRecordVO;
import io.choerodon.devops.api.vo.DevopsGitlabCommitVO;
import io.choerodon.devops.api.vo.PushWebHookVO;
import io.choerodon.devops.infra.dto.DevopsGitlabCommitDTO;
import io.choerodon.devops.infra.dto.iam.IamUserDTO;

public interface DevopsGitlabCommitService {

    void create(PushWebHookVO pushWebHookVO, String token);

    DevopsGitlabCommitVO queryCommits(Long projectId, String appServiceIds, Date startDate, Date endDate);

    PageInfo<CommitFormRecordVO> pageRecordCommits(Long projectId, String appServiceIds, Pageable pageable,
                                                   Date startDate, Date endDate);

    DevopsGitlabCommitDTO baseCreate(DevopsGitlabCommitDTO devopsGitlabCommitDTO);

    DevopsGitlabCommitDTO baseQueryByShaAndRef(String sha, String ref);

    List<DevopsGitlabCommitDTO> baseListByOptions(Long projectId, List<Long> appServiceIds, Date startDate, Date endDate);

    PageInfo<CommitFormRecordVO> basePageByOptions(Long projectId, List<Long> appServiceId,
                                                   Pageable pageable, Map<Long, IamUserDTO> userMap,
                                                   Date startDate, Date endDate);

    void baseUpdate(DevopsGitlabCommitDTO devopsGitlabCommitDTO);

    List<DevopsGitlabCommitDTO> baseListByAppIdAndBranch(Long appServiceId, String branch, Date startDate);

}
