package io.choerodon.devops.app.service.impl;

import java.util.*;
import java.util.stream.Collectors;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import org.springframework.data.domain.Pageable;
import io.choerodon.core.exception.CommonException;
import io.choerodon.devops.api.vo.CommitFormRecordVO;
import io.choerodon.devops.api.vo.CommitFormUserVO;
import io.choerodon.devops.api.vo.DevopsGitlabCommitVO;
import io.choerodon.devops.api.vo.PushWebHookVO;
import io.choerodon.devops.app.service.AppServiceService;
import io.choerodon.devops.app.service.DevopsGitService;
import io.choerodon.devops.app.service.DevopsGitlabCommitService;
import io.choerodon.devops.infra.dto.AppServiceDTO;
import io.choerodon.devops.infra.dto.DevopsGitlabCommitDTO;
import io.choerodon.devops.infra.dto.gitlab.CommitDTO;
import io.choerodon.devops.infra.dto.iam.IamUserDTO;
import io.choerodon.devops.infra.feign.operator.BaseServiceClientOperator;
import io.choerodon.devops.infra.feign.operator.GitlabServiceClientOperator;
import io.choerodon.devops.infra.mapper.DevopsGitlabCommitMapper;
import io.choerodon.devops.infra.util.PageRequestUtil;
import io.choerodon.devops.infra.util.TypeUtil;
import org.springframework.util.ObjectUtils;


@Service
public class DevopsGitlabCommitServiceImpl implements DevopsGitlabCommitService {

    private static final Gson gson = new Gson();
    private static final Integer ADMIN = 1;

    @Autowired
    private BaseServiceClientOperator baseServiceClientOperator;
    @Autowired
    private DevopsGitlabCommitMapper devopsGitlabCommitMapper;
    @Autowired
    private AppServiceService applicationService;
    @Autowired
    private DevopsGitlabCommitService devopsGitlabCommitService;
    @Autowired
    private DevopsGitService devopsGitService;
    @Autowired
    private GitlabServiceClientOperator gitlabServiceClientOperator;

    @Override
    public void create(PushWebHookVO pushWebHookVO, String token) {
        AppServiceDTO applicationDTO = applicationService.baseQueryByToken(token);
        String ref = pushWebHookVO.getRef().split("/")[2];
        if (!pushWebHookVO.getCommits().isEmpty()) {
            pushWebHookVO.getCommits().forEach(commitDTO -> {
                DevopsGitlabCommitDTO devopsGitlabCommitDTO = devopsGitlabCommitService.baseQueryByShaAndRef(commitDTO.getId(), ref);

                if (devopsGitlabCommitDTO == null) {
                    devopsGitlabCommitDTO = new DevopsGitlabCommitDTO();
                    devopsGitlabCommitDTO.setAppServiceId(applicationDTO.getId());
                    devopsGitlabCommitDTO.setCommitContent(commitDTO.getMessage());
                    devopsGitlabCommitDTO.setCommitSha(commitDTO.getId());
                    devopsGitlabCommitDTO.setRef(ref);
                    devopsGitlabCommitDTO.setUrl(commitDTO.getUrl());
                    if ("root".equals(commitDTO.getAuthor().getName())) {
                        devopsGitlabCommitDTO.setUserId(1L);
                    } else {
                        IamUserDTO iamUserDTO = baseServiceClientOperator.queryByEmail(applicationDTO.getProjectId(),
                                commitDTO.getAuthor().getEmail());
                        if (iamUserDTO != null) {
                            devopsGitlabCommitDTO.setUserId(iamUserDTO.getId());
                        }
                    }
                    devopsGitlabCommitDTO.setCommitDate(commitDTO.getTimestamp());
                    devopsGitlabCommitService.baseCreate(devopsGitlabCommitDTO);
                }
            });
        } else {
            //直接从一个分支切出来另外一个分支，没有commits记录
            DevopsGitlabCommitDTO devopsGitlabCommitDTO = devopsGitlabCommitService.baseQueryByShaAndRef(pushWebHookVO.getCheckoutSha(), ref);
            if (devopsGitlabCommitDTO == null) {
                CommitDTO commitDTO = gitlabServiceClientOperator.queryCommit(TypeUtil.objToInteger(applicationDTO.getGitlabProjectId()), pushWebHookVO.getCheckoutSha(), ADMIN);
                devopsGitlabCommitDTO = new DevopsGitlabCommitDTO();
                devopsGitlabCommitDTO.setAppServiceId(applicationDTO.getId());
                devopsGitlabCommitDTO.setCommitContent(commitDTO.getMessage());
                devopsGitlabCommitDTO.setCommitSha(commitDTO.getId());
                devopsGitlabCommitDTO.setRef(ref);
                devopsGitlabCommitDTO.setUrl(commitDTO.getUrl());
                if ("root".equals(commitDTO.getAuthorName())) {
                    devopsGitlabCommitDTO.setUserId(1L);
                } else {
                    IamUserDTO userE = baseServiceClientOperator.queryByEmail(applicationDTO.getProjectId(),
                            commitDTO.getAuthorEmail());
                    if (userE != null) {
                        devopsGitlabCommitDTO.setUserId(userE.getId());
                    }
                }
                devopsGitlabCommitDTO.setCommitDate(commitDTO.getCommittedDate());
                devopsGitlabCommitService.baseCreate(devopsGitlabCommitDTO);
            }
        }

    }

    @Override
    public DevopsGitlabCommitVO queryCommits(Long projectId, String appServiceIds, Date startDate, Date
            endDate) {

        List<Long> appServiceIdsMap = gson.fromJson(appServiceIds, new TypeToken<List<Long>>() {
        }.getType());
        if (appServiceIdsMap.isEmpty()) {
            return new DevopsGitlabCommitVO();
        }

        // 查询应用列表下所有commit记录
        List<DevopsGitlabCommitDTO> devopsGitlabCommitDTOS = devopsGitlabCommitService
                .baseListByOptions(projectId, appServiceIdsMap, startDate, endDate);
        if (devopsGitlabCommitDTOS.isEmpty()) {
            return new DevopsGitlabCommitVO();
        }

        // 获得去重后的所有用户信息
        Map<Long, IamUserDTO> userMap = getUserDOMap(devopsGitlabCommitDTOS);

        // 获取用户分别的commit
        List<CommitFormUserVO> commitFormUserVOS = getCommitFormUserDTOList(devopsGitlabCommitDTOS, userMap);

        // 获取总的commit(将所有用户的commit_date放入一个数组)，按照时间先后排序
        List<Date> totalCommitsDate = getTotalDates(commitFormUserVOS);
        Collections.sort(totalCommitsDate);

        return new DevopsGitlabCommitVO(commitFormUserVOS, totalCommitsDate);
    }

    @Override
    public PageInfo<CommitFormRecordVO> pageRecordCommits(Long projectId, String appServiceIds, Pageable
            pageable,
                                                          Date startDate, Date endDate) {

        List<Long> appServiceIdsMap = gson.fromJson(appServiceIds, new TypeToken<List<Long>>() {
        }.getType());
        if (appServiceIdsMap.isEmpty()) {
            return new PageInfo<>();
        }

        // 查询应用列表下所有commit记录
        List<DevopsGitlabCommitDTO> devopsGitlabCommitES = devopsGitlabCommitService
                .baseListByOptions(projectId, appServiceIdsMap, startDate, endDate);
        Map<Long, IamUserDTO> userMap = getUserDOMap(devopsGitlabCommitES);
        // 获取最近的commit(返回所有的commit记录，按时间先后排序，分页查询)
        return getCommitFormRecordDTOS(projectId, appServiceIdsMap, pageable, userMap, startDate, endDate);
    }

    private Map<Long, IamUserDTO> getUserDOMap(List<DevopsGitlabCommitDTO> devopsGitlabCommitDTOS) {
        // 获取users
        List<IamUserDTO> userEList = baseServiceClientOperator.listUsersByIds(devopsGitlabCommitDTOS.stream().map(
                DevopsGitlabCommitDTO::getUserId).distinct().collect(Collectors.toList()));

        return userEList.stream().collect(Collectors.toMap(IamUserDTO::getId, u -> u, (u1, u2) -> u1));
    }

    private List<CommitFormUserVO> getCommitFormUserDTOList
            (List<DevopsGitlabCommitDTO> devopsGitlabCommitDTOS,
             Map<Long, IamUserDTO> userMap) {
        List<CommitFormUserVO> commitFormUserVOS = new ArrayList<>();
        // 遍历list，key为userid，value为list
        Map<Long, List<DevopsGitlabCommitDTO>> map = new HashMap<>();
        for (DevopsGitlabCommitDTO devopsGitlabCommitDTO : devopsGitlabCommitDTOS) {
            Long userId = devopsGitlabCommitDTO.getUserId();
            if (userId == null && !map.containsKey(0L)) {
                List<DevopsGitlabCommitDTO> commitDTOS = new ArrayList<>();
                commitDTOS.add(devopsGitlabCommitDTO);
                map.put(0L, commitDTOS);
            } else if (userId == null && map.containsKey(0L)) {
                map.get(0L).add(devopsGitlabCommitDTO);
            } else if (userId != null && !map.containsKey(userId)) {
                List<DevopsGitlabCommitDTO> commitDTOS = new ArrayList<>();
                commitDTOS.add(devopsGitlabCommitDTO);
                map.put(userId, commitDTOS);
            } else {
                map.get(userId).add(devopsGitlabCommitDTO);
            }
        }
        map.forEach((userId, list) -> {
            IamUserDTO iamUserDTO = userMap.get(userId);
            if(!ObjectUtils.isEmpty(iamUserDTO)){
                String loginName = iamUserDTO.getLdap() ? iamUserDTO.getLoginName() : iamUserDTO.getEmail() ;
                String name = iamUserDTO.getRealName() + "("+loginName+")";
                String imgUrl = iamUserDTO.getImageUrl();
                // 遍历list，将每个用户的所有commit date取出放入List<Date>，然后保存为DTO
                List<Date> date = new ArrayList<>();
                list.forEach(e -> date.add(e.getCommitDate()));
                commitFormUserVOS.add(new CommitFormUserVO(userId, name, imgUrl, date));
            }
        });
        return commitFormUserVOS;
    }

    private PageInfo<CommitFormRecordVO> getCommitFormRecordDTOS(Long projectId, List<Long> appServiceIds, Pageable pageable,
                                                                 Map<Long, IamUserDTO> userMap, Date startDate, Date endDate) {
        return devopsGitlabCommitService.basePageByOptions(projectId, appServiceIds, pageable, userMap, startDate, endDate);
    }

    private List<Date> getTotalDates(List<CommitFormUserVO> commitFormUserVOS) {
        List<Date> totalCommitsDate = new ArrayList<>();
        commitFormUserVOS.forEach(e -> totalCommitsDate.addAll(e.getCommitDates()));
        return totalCommitsDate;
    }

    @Override
    public DevopsGitlabCommitDTO baseCreate(DevopsGitlabCommitDTO devopsGitlabCommitDTO) {
        if (!checkExist(devopsGitlabCommitDTO)) {
            if (devopsGitlabCommitMapper.insert(devopsGitlabCommitDTO) != 1) {
                throw new CommonException("error.gitlab.commit.create");
            }
        }
        return devopsGitlabCommitDTO;
    }

    @Override
    public DevopsGitlabCommitDTO baseQueryByShaAndRef(String sha, String ref) {
        DevopsGitlabCommitDTO devopsGitlabCommitDTO = new DevopsGitlabCommitDTO();
        devopsGitlabCommitDTO.setCommitSha(sha);
        devopsGitlabCommitDTO.setRef(ref);
        return devopsGitlabCommitMapper.selectOne(devopsGitlabCommitDTO);
    }

    @Override
    public List<DevopsGitlabCommitDTO> baseListByOptions(Long projectId, List<Long> appServiceIds, Date
            startDate, Date endDate) {
        List<DevopsGitlabCommitDTO> devopsGitlabCommitDOList = devopsGitlabCommitMapper
                .listCommits(projectId, appServiceIds, new java.sql.Date(startDate.getTime()), new java.sql.Date(endDate.getTime()));
        if (devopsGitlabCommitDOList == null || devopsGitlabCommitDOList.isEmpty()) {
            return new ArrayList<>();
        }
        return devopsGitlabCommitDOList;
    }

    @Override
    public PageInfo<CommitFormRecordVO> basePageByOptions(Long projectId, List<Long> appServiceIds,
                                                          Pageable pageable, Map<Long, IamUserDTO> userMap,
                                                          Date startDate, Date endDate) {
        List<CommitFormRecordVO> commitFormRecordVOList = new ArrayList<>();

        PageInfo<DevopsGitlabCommitDTO> devopsGitlabCommitDTOPage = PageHelper.startPage(pageable.getPageNumber(), pageable.getPageSize(),
                PageRequestUtil.getOrderBy(pageable)).doSelectPageInfo(
                () -> devopsGitlabCommitMapper.listCommits(projectId, appServiceIds, new java.sql.Date(startDate.getTime()), new java.sql.Date(endDate.getTime())));

        devopsGitlabCommitDTOPage.getList().forEach(e -> {
            Long userId = e.getUserId();
            IamUserDTO user = userMap.get(userId);
            CommitFormRecordVO commitFormRecordVO;
            if (user != null) {
                String loginName = user.getLdap() ? user.getLoginName() : user.getEmail();
                commitFormRecordVO = new CommitFormRecordVO(
                        userId, user.getImageUrl(), user.getRealName() + " " + loginName, e);
            } else {
                commitFormRecordVO = new CommitFormRecordVO(
                        null, null, null, e);
            }
            commitFormRecordVOList.add(commitFormRecordVO);
        });
        PageInfo<CommitFormRecordVO> commitFormRecordVOPageInfo = new PageInfo<>();
        BeanUtils.copyProperties(devopsGitlabCommitDTOPage, commitFormRecordVOPageInfo);
        commitFormRecordVOPageInfo.setList(commitFormRecordVOList);

        return commitFormRecordVOPageInfo;
    }

    @Override
    public void baseUpdate(DevopsGitlabCommitDTO devopsGitlabCommitDTO) {
        DevopsGitlabCommitDTO oldDevopsGitlabCommitDO = devopsGitlabCommitMapper.selectByPrimaryKey(devopsGitlabCommitDTO.getId());
        devopsGitlabCommitDTO.setObjectVersionNumber(oldDevopsGitlabCommitDO.getObjectVersionNumber());
        if (devopsGitlabCommitMapper.updateByPrimaryKeySelective(devopsGitlabCommitDTO) != 1) {
            throw new CommonException("error.gitlab.commit.update");
        }
    }

    @Override
    public List<DevopsGitlabCommitDTO> baseListByAppIdAndBranch(Long appServiceIds, String branch, Date
            startDate) {
        return devopsGitlabCommitMapper.queryByAppIdAndBranch(appServiceIds, branch, startDate == null ? null : new java.sql.Date(startDate.getTime()));
    }

    private boolean checkExist(DevopsGitlabCommitDTO devopsGitlabCommitDTO) {
        devopsGitlabCommitDTO.setCommitSha(devopsGitlabCommitDTO.getCommitSha());
        devopsGitlabCommitDTO.setRef(devopsGitlabCommitDTO.getRef());
        return devopsGitlabCommitMapper.selectOne(devopsGitlabCommitDTO) != null;
    }
}
