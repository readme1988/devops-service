package io.choerodon.devops.app.service.impl;

import java.util.List;

import io.choerodon.core.exception.CommonException;
import io.choerodon.devops.app.service.DevopsRegistrySecretService;
import io.choerodon.devops.infra.dto.DevopsRegistrySecretDTO;
import io.choerodon.devops.infra.mapper.DevopsRegistrySecretMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Created by Sheep on 2019/7/15.
 */

@Service
public class DevopsRegistrySecretServiceImpl implements DevopsRegistrySecretService {


    @Autowired
    private DevopsRegistrySecretMapper devopsRegistrySecretMapper;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public DevopsRegistrySecretDTO baseCreate(DevopsRegistrySecretDTO devopsRegistrySecretDTO) {
        if (devopsRegistrySecretMapper.insert(devopsRegistrySecretDTO) != 1) {
            throw new CommonException("error.registry.secret.create.error");
        }
        return devopsRegistrySecretDTO;
    }

    @Override
    public DevopsRegistrySecretDTO baseQuery(Long devopsRegistrySecretId) {
        return devopsRegistrySecretMapper.selectByPrimaryKey(devopsRegistrySecretId);
    }

    @Override
    public DevopsRegistrySecretDTO baseUpdate(DevopsRegistrySecretDTO devopsRegistrySecretDTO) {
        DevopsRegistrySecretDTO beforeDevopsRegistrySecretDTO = devopsRegistrySecretMapper.selectByPrimaryKey(devopsRegistrySecretDTO.getId());
        devopsRegistrySecretDTO.setObjectVersionNumber(beforeDevopsRegistrySecretDTO.getObjectVersionNumber());
        if (devopsRegistrySecretMapper.updateByPrimaryKeySelective(devopsRegistrySecretDTO) != 1) {
            throw new CommonException("error.registry.secret.update.error");
        }
        return beforeDevopsRegistrySecretDTO;
    }

    @Override
    public void baseUpdateStatus(Long id, Boolean status) {
        devopsRegistrySecretMapper.updateStatus(id,status);
    }

    @Override
    public DevopsRegistrySecretDTO baseQueryByEnvAndId(Long envId, Long configId) {
        DevopsRegistrySecretDTO devopsRegistrySecretDTO = new DevopsRegistrySecretDTO();
        devopsRegistrySecretDTO.setConfigId(configId);
        devopsRegistrySecretDTO.setEnvId(envId);
        return devopsRegistrySecretMapper.selectOne(devopsRegistrySecretDTO);
    }

    @Override
    public List<DevopsRegistrySecretDTO> baseListByConfig(Long configId) {
        DevopsRegistrySecretDTO devopsRegistrySecretDTO = new DevopsRegistrySecretDTO();
        devopsRegistrySecretDTO.setConfigId(configId);
        return devopsRegistrySecretMapper.select(devopsRegistrySecretDTO);
    }

    @Override
    public DevopsRegistrySecretDTO baseQueryByEnvAndName(Long envId, String name) {
        DevopsRegistrySecretDTO devopsRegistrySecretDTO = new DevopsRegistrySecretDTO();
        devopsRegistrySecretDTO.setSecretCode(name);
        devopsRegistrySecretDTO.setEnvId(envId);
        return devopsRegistrySecretMapper.selectOne(devopsRegistrySecretDTO);
    }

}
