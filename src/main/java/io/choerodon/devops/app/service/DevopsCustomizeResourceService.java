package io.choerodon.devops.app.service;

import java.util.List;

import com.github.pagehelper.PageInfo;
import org.springframework.data.domain.Pageable;
import io.choerodon.devops.api.vo.DevopsCustomizeResourceVO;
import io.choerodon.devops.api.vo.DevopsCustomizeResourceReqVO;
import io.choerodon.devops.infra.dto.DevopsCustomizeResourceDTO;

import org.springframework.web.multipart.MultipartFile;

/**
 * Created by Sheep on 2019/6/26.
 */
public interface DevopsCustomizeResourceService {

    /**
     * @param projectId                    项目id
     * @param devopsCustomizeResourceReqVO 相关信息
     * @param contentFile                  内容文件
     */
    void createOrUpdateResource(Long projectId, DevopsCustomizeResourceReqVO devopsCustomizeResourceReqVO, MultipartFile contentFile);


    /**
     * @param type                       资源类型
     * @param devopsCustomizeResourceDTO 资源信息
     */
    void createOrUpdateResourceByGitOps(String type, DevopsCustomizeResourceDTO devopsCustomizeResourceDTO, Long userId, Long envId);


    /**
     * @param resourceId 资源id
     */
    void deleteResource(Long resourceId);


    /**
     * @param resourceId 资源id
     */
    void deleteResourceByGitOps(Long resourceId);

    /**
     * @param resourceId 资源id
     * @return
     */
    DevopsCustomizeResourceVO queryDevopsCustomizeResourceDetail(Long resourceId);

    /**
     * @param envId    环境id
     * @param pageable 分页参数
     * @param params   查询参数
     * @return 分页后资源
     */
    PageInfo<DevopsCustomizeResourceVO> pageResources(Long envId, Pageable pageable, String params);

    DevopsCustomizeResourceDTO baseCreate(DevopsCustomizeResourceDTO devopsCustomizeResourceDTO);

    DevopsCustomizeResourceDTO baseQuery(Long resourceId);

    void baseUpdate(DevopsCustomizeResourceDTO devopsCustomizeResourceDTO);

    List<DevopsCustomizeResourceDTO> listByEnvAndFilePath(Long envId, String filePath);

    DevopsCustomizeResourceDTO queryByEnvIdAndKindAndName(Long envId, String kind, String name);

    PageInfo<DevopsCustomizeResourceDTO> pageDevopsCustomizeResourceE(Long envId, Pageable pageable, String params);

    void checkExist(Long envId, String kind, String name);

    List<DevopsCustomizeResourceDTO> baseListByEnvId(Long envId);

    void baseDeleteCustomizeResourceByEnvId(Long envId);
}
