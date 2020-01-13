package io.choerodon.devops.infra.template;

import org.springframework.stereotype.Component;

import io.choerodon.core.notify.Level;
import io.choerodon.core.notify.NotifyBusinessType;
import io.choerodon.core.notify.PmTemplate;
import io.choerodon.devops.infra.enums.PipelineNoticeType;

/**
 * Creator: ChangpingShi0213@gmail.com
 * Date:  14:38 2019/6/6
 * Description:
 */
@NotifyBusinessType(code = "pipelinestop", name = "流水线被终止通知", level = Level.PROJECT,
        pmEnabledFlag = true,
        emailEnabledFlag = true,
        description = "流水线被终止通知", isAllowConfig = false, isManualRetry = true, categoryCode = "stream-change-notice")
@Component
public class PipelineStopPmTemplate implements PmTemplate {
    @Override
    public String businessTypeCode() {
        return PipelineNoticeType.PIPELINESTOP.toValue();
    }

    @Override
    public String code() {
        return PipelineNoticeType.PIPELINESTOP.toValue();
    }

    @Override
    public String name() {
        return "流水线被终止通知";
    }

    @Override
    public String title() {
        return "流水线被终止";
    }

    @Override
    public String content() {
        return "<p>流水线“${pipelineName}”在【${stageName}】阶段被${auditName}:${realName}终止<p>" +
                "<p><a href=#/devops/deployment-operation?type=project&id=${projectId}&name=${projectName}&category=undefined&organizationId=${organizationId}&orgId=${organizationId}&pipelineRecordId=${pipelineRecordId}>查看详情</a >";
    }
}
