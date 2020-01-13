package io.choerodon.devops.infra.util;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import io.choerodon.core.exception.CommonException;
import io.choerodon.devops.infra.dto.DevopsPrometheusDTO;
import io.choerodon.devops.infra.enums.ClusterResourceType;

public class ComponentValuesTemplateUtil {
    private static final String TEMPLATE = "/component/template/%s.yml";

    private ComponentValuesTemplateUtil() {
    }

    public static String convert(ClusterResourceType type, Object object, Map<String, Object> extraData) {
        InputStream in = Optional.ofNullable(ComponentValuesTemplateUtil.class.getResourceAsStream(String.format(TEMPLATE, type.getType())))
                .orElseThrow(() -> new CommonException("error.template.config.file.not.exist"));
        switch (type) {
            case PROMETHEUS:
                return convertPrometheus((DevopsPrometheusDTO) object, in, extraData);
        }
        return null;
    }

    /***
     *
     * @param devopsPrometheusDTO 普罗米修斯配置数据
     * @param in                  yaml文件输入流
     * @return 替换完毕的values文件
     */
    public static String convertPrometheus(DevopsPrometheusDTO devopsPrometheusDTO, InputStream in, Map<String, Object> extraData) {
        String apiHost = (String) Optional.ofNullable(extraData.get("apiHost")).orElseThrow(() -> new CommonException("error.api.host"));
        Map<String, String> map = new HashMap<>();
        map.put("{{adminPassword}}", devopsPrometheusDTO.getAdminPassword());
        map.put("{{host}}", devopsPrometheusDTO.getGrafanaDomain());
        map.put("{{clusterName}}", devopsPrometheusDTO.getClusterCode());

        map.put("{{prometheus-pv}}", devopsPrometheusDTO.getPrometheusPv().getName());
        map.put("{{prometheusAccessMode}}", devopsPrometheusDTO.getPrometheusPv().getAccessModes());
        map.put("{{prometheusStorage}}", devopsPrometheusDTO.getPrometheusPv().getRequestResource());

        map.put("{{alertmanager-pv}}", devopsPrometheusDTO.getAltermanagerPv().getName());
        map.put("{{altermanagerAccesssMode}}", devopsPrometheusDTO.getAltermanagerPv().getAccessModes());
        map.put("{{altermanagerStorage}}", devopsPrometheusDTO.getAltermanagerPv().getRequestResource());


        map.put("{{grafana-pv}}", devopsPrometheusDTO.getGrafanaPv().getName());
        map.put("{{grafanaAccessMode}}", devopsPrometheusDTO.getGrafanaPv().getAccessModes());
        map.put("{{grafanaStorage}}", devopsPrometheusDTO.getGrafanaPv().getRequestResource());

        map.put("{{api-host}}", apiHost);

        return FileUtil.replaceReturnString(in, map);
    }

}
