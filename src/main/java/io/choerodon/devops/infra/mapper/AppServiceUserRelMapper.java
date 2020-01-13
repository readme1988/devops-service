package io.choerodon.devops.infra.mapper;

import java.util.List;

import io.choerodon.devops.infra.dto.AppServiceUserRelDTO;
import io.choerodon.mybatis.common.Mapper;
import org.apache.ibatis.annotations.Param;


/**
 * Created by n!Ck
 * Date: 2018/11/21
 * Time: 11:08
 * Description:
 */

public interface AppServiceUserRelMapper extends Mapper<AppServiceUserRelDTO> {
    List<AppServiceUserRelDTO> listAllUserPermissionByAppId(@Param("appServiceId") Long appServiceId);

    void deleteByUserIdWithAppIds(@Param("appServiceIds") List<Long> appServiceIds, @Param("userId") Long userId);

    void batchDelete(@Param("appServiceIds") List<Long> appServiceIds, @Param("userId") Long userId);
}
