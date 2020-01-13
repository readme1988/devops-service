package io.choerodon.devops.app.service;

import java.util.List;

import io.choerodon.devops.infra.dto.DevopsRegistrySecretDTO;

/**
 * Created by Sheep on 2019/7/15.
 */
public interface DevopsRegistrySecretService {

     DevopsRegistrySecretDTO baseCreate(DevopsRegistrySecretDTO devopsRegistrySecretDTO);

     DevopsRegistrySecretDTO baseQuery(Long devopsRegistrySecretId);

     DevopsRegistrySecretDTO baseUpdate(DevopsRegistrySecretDTO devopsRegistrySecretDTO);

     void baseUpdateStatus(Long id, Boolean status);

     DevopsRegistrySecretDTO baseQueryByEnvAndId(Long envId, Long configId);

     List<DevopsRegistrySecretDTO> baseListByConfig(Long configId);

     DevopsRegistrySecretDTO baseQueryByEnvAndName(Long envId, String name);

}
