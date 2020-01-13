package io.choerodon.devops.app.service;

import java.util.List;

import com.github.pagehelper.PageInfo;
import org.springframework.data.domain.Pageable;
import io.choerodon.devops.api.vo.DevopsEnvUserVO;
import io.choerodon.devops.infra.dto.DevopsEnvUserPermissionDTO;

/**
 * Created by Sheep on 2019/7/11.
 */
public interface DevopsEnvUserPermissionService {


    void create(DevopsEnvUserVO devopsEnvUserPermissionE);

    PageInfo<DevopsEnvUserVO> pageByOptions(Long envId, Pageable pageable, String params);

    void deleteByEnvId(Long envId);

    List<DevopsEnvUserVO> listByEnvId(Long envId);

    List<DevopsEnvUserPermissionDTO> listByUserId(Long userId);

    void checkEnvDeployPermission(Long userId, Long envId);

    void baseCreate(DevopsEnvUserPermissionDTO devopsEnvUserPermissionE);

    List<DevopsEnvUserPermissionDTO> baseListByEnvId(Long envId);

    List<DevopsEnvUserPermissionDTO> baseListAll(Long envId);

    void baseUpdate(Long envId, List<Long> addUsersList, List<Long> deleteUsersList);

    void baseDelete(Long envId, Long userId);

    void batchDelete(List<Long> envIds, Long userId);
}
