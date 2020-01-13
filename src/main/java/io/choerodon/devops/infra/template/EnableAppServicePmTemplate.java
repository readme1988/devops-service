package io.choerodon.devops.infra.template;

import org.springframework.stereotype.Component;

import io.choerodon.core.notify.*;
import io.choerodon.devops.infra.constant.NoticeCodeConstants;

/**
 * @author zmf
 * @since 12/4/19
 */
@NotifyBusinessType(code = NoticeCodeConstants.APP_SERVICE_ENABLED, name = "启用应用服务", level = Level.PROJECT,
        description = "启用应用服务通知", isManualRetry = true, categoryCode = "app-service-notice",
        pmEnabledFlag = true,
        emailEnabledFlag = true,
        proPmEnabledFlag = true,
        notifyType = ServiceNotifyType.DEVOPS_NOTIFY,
        targetUserType = {TargetUserType.TARGET_USER_APPLICATION_SERVICE_PERMISSION_OWNER})
@Component
public class EnableAppServicePmTemplate implements PmTemplate {
    @Override
    public String code() {
        return "EnableAppServicePm";
    }

    @Override
    public String name() {
        return "启用应用服务站内信模板";
    }

    @Override
    public String businessTypeCode() {
        return NoticeCodeConstants.APP_SERVICE_ENABLED;
    }

    @Override
    public String title() {
        return "应用服务已启用";
    }

    /**
     * projectName appServiceName projectId projectCategory organizationId
     */
    @Override
    public String content() {
        return "<p>项目“${projectName}”下的应用服务“${appServiceName}”已被启用</p>\n" +
                "<p><a href=#/devops/app-service?type=project&id=${projectId}&name=${projectName}&category=${projectCategory}&organizationId=${organizationId}>查看详情</a></p>";
    }
}
