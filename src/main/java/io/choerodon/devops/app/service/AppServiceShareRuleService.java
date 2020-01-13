package io.choerodon.devops.app.service;

import com.github.pagehelper.PageInfo;

import org.springframework.data.domain.Pageable;
import io.choerodon.devops.api.vo.AppServiceShareRuleVO;

/**
 * Created by ernst on 2018/5/12.
 */
public interface AppServiceShareRuleService {

    AppServiceShareRuleVO createOrUpdate(Long projectId, AppServiceShareRuleVO appServiceShareRuleVO);

    PageInfo<AppServiceShareRuleVO> pageByOptions(Long projectId, Long appServiceId, Pageable pageable, String params);

    AppServiceShareRuleVO query(Long projectId, Long ruleId);

    void delete(Long ruleId);

}
