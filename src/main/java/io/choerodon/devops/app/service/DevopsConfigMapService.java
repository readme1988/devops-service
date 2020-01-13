package io.choerodon.devops.app.service;

import java.util.List;

import com.github.pagehelper.PageInfo;

import org.springframework.data.domain.Pageable;
import io.choerodon.devops.api.vo.DevopsConfigMapVO;
import io.choerodon.devops.api.vo.DevopsConfigMapRespVO;
import io.choerodon.devops.infra.dto.DevopsConfigMapDTO;

public interface DevopsConfigMapService {

    void createOrUpdate(Long projectId, Boolean isSync, DevopsConfigMapVO devopsConfigMapVO);

    void deleteByGitOps(Long configMapId);

    void delete(Long configMapId);

    void checkName(Long envId, String name);

    DevopsConfigMapRespVO createOrUpdateByGitOps(DevopsConfigMapVO devopsConfigMapVO, Long userId);

    DevopsConfigMapRespVO query(Long configMapId);

    PageInfo<DevopsConfigMapRespVO> pageByOptions(Long projectId, Long envId, Pageable pageable, String searchParam, Long appServiceId);

    DevopsConfigMapDTO baseQueryByEnvIdAndName(Long envId, String name);

    DevopsConfigMapDTO baseCreate(DevopsConfigMapDTO devopsConfigMapE);

    DevopsConfigMapDTO baseUpdate(DevopsConfigMapDTO devopsConfigMapE);

    DevopsConfigMapDTO baseQueryById(Long id);

    void baseDelete(Long id);

    PageInfo<DevopsConfigMapDTO> basePageByEnv(Long envId, Pageable pageable, String params, Long appServiceId);

    List<DevopsConfigMapDTO> baseListByEnv(Long envId);

    void baseDeleteByEnvId(Long envId);

}
