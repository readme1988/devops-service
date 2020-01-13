package io.choerodon.devops.infra.mapper;

import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Param;

import io.choerodon.devops.infra.dto.DevopsBranchDTO;
import io.choerodon.mybatis.common.Mapper;

public interface DevopsBranchMapper extends Mapper<DevopsBranchDTO> {

    DevopsBranchDTO queryByAppAndBranchName(@Param("appServiceId") Long appServiceId, @Param("branchName") String name);

    List<DevopsBranchDTO> list(@Param("appServiceId") Long appServiceId,
                               @Param("sortString") String sortString,
                               @Param("searchParam") Map<String, Object> searchParam,
                               @Param("params") List<String> params);


    void deleteByIsDelete();

    void deleteDuplicateBranch();

    void deleteByAppServiceId(@Param("appServiceId") Long appServiceId);
}
