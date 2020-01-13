package io.choerodon.devops.app.task;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import io.choerodon.asgard.schedule.QuartzDefinition;
import io.choerodon.asgard.schedule.annotation.JobTask;
import io.choerodon.asgard.schedule.annotation.TimedTask;
import io.choerodon.devops.app.service.DevopsCheckLogService;

/**
 * @author zmf
 */
@Component
public class DevopsTask {
    private static final Logger logger = LoggerFactory.getLogger(DevopsTask.class);

    @Autowired
    private DevopsCheckLogService devopsCheckLogService;

    /**
     * 升级0.18.0-0.19.0，迁移数据
     */
    @JobTask(maxRetryCount = 3, code = "upgradeVersionTo20", description = "升级0.19.0-0.20.0，迁移数据")
    @TimedTask(name = "upgradeVersionTo20", description = "升级0.19.0-0.20.0，迁移数据", oneExecution = true,
            repeatCount = 0, repeatInterval = 1, repeatIntervalUnit = QuartzDefinition.SimpleRepeatIntervalUnit.HOURS, params = {})
    public void syncEnvAppRelevance(Map<String, Object> map) {
        logger.info("begin to upgrade 0.19.0 to 0.20.0 without Cluster and Certification migration.");
        devopsCheckLogService.checkLog("0.20.0");
    }

}
