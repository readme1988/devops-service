package io.choerodon.devops.infra.dto;

import java.util.List;

import io.choerodon.devops.api.vo.DevopsAppServiceInstanceViewVO;
import io.choerodon.devops.api.vo.DevopsResourceBasicInfoVO;

/**
 * 资源视图下环境目录信息
 *
 * @author zmf
 */
public class DevopsResourceEnvOverviewDTO {
    /**
     * 环境id
     */
    private Long id;
    /**
     * 环境名称
     */
    private String name;
    /**
     * 环境是否连接
     */
    private Boolean connect;

    private Long clusterId;

    /**
     * 实例基本信息
     */
    private List<DevopsAppServiceInstanceViewVO> instances;
    /**
     * 网络基本信息
     */
    private List<DevopsResourceBasicInfoVO> services;
    /**
     * 域名基本信息
     */
    private List<DevopsResourceBasicInfoVO> ingresses;
    /**
     * 证书基本信息
     */
    private List<DevopsResourceBasicInfoVO> certifications;
    /**
     * 配置映射基本信息
     */
    private List<DevopsResourceBasicInfoVO> configMaps;
    /**
     * 密文基本信息
     */
    private List<DevopsResourceBasicInfoVO> secrets;
    /**
     * 自定义资源基本信息
     */
    private List<DevopsResourceBasicInfoVO> customResources;

    /**
     * PVC 基本信息
     */
    private List<DevopsResourceBasicInfoVO> pvcs;

    public List<DevopsResourceBasicInfoVO> getPvcs() {
        return pvcs;
    }

    public void setPvcs(List<DevopsResourceBasicInfoVO> pvcs) {
        this.pvcs = pvcs;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Boolean getConnect() {
        return connect;
    }

    public void setConnect(Boolean connect) {
        this.connect = connect;
    }

    public List<DevopsAppServiceInstanceViewVO> getInstances() {
        return instances;
    }

    public void setInstances(List<DevopsAppServiceInstanceViewVO> instances) {
        this.instances = instances;
    }

    public List<DevopsResourceBasicInfoVO> getServices() {
        return services;
    }

    public void setServices(List<DevopsResourceBasicInfoVO> services) {
        this.services = services;
    }

    public List<DevopsResourceBasicInfoVO> getIngresses() {
        return ingresses;
    }

    public void setIngresses(List<DevopsResourceBasicInfoVO> ingresses) {
        this.ingresses = ingresses;
    }

    public List<DevopsResourceBasicInfoVO> getConfigMaps() {
        return configMaps;
    }

    public void setConfigMaps(List<DevopsResourceBasicInfoVO> configMaps) {
        this.configMaps = configMaps;
    }

    public List<DevopsResourceBasicInfoVO> getSecrets() {
        return secrets;
    }

    public void setSecrets(List<DevopsResourceBasicInfoVO> secrets) {
        this.secrets = secrets;
    }

    public List<DevopsResourceBasicInfoVO> getCustomResources() {
        return customResources;
    }

    public void setCustomResources(List<DevopsResourceBasicInfoVO> customResources) {
        this.customResources = customResources;
    }

    public List<DevopsResourceBasicInfoVO> getCertifications() {
        return certifications;
    }

    public void setCertifications(List<DevopsResourceBasicInfoVO> certifications) {
        this.certifications = certifications;
    }

    public Long getClusterId() {
        return clusterId;
    }

    public void setClusterId(Long clusterId) {
        this.clusterId = clusterId;
    }
}
