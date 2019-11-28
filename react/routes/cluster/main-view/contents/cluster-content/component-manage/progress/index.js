import React, { Fragment, Suspense, useEffect, useMemo } from 'react';
import { Tooltip, Icon, Button } from 'choerodon-ui/pro';
import { Spin } from 'choerodon-ui';
import map from 'lodash/map';
import { observer } from 'mobx-react-lite';
import omit from 'lodash/omit';
import find from 'lodash/find';
import { useClusterMainStore } from '../../../../stores';
import { useClusterContentStore } from '../../stores';

import './index.less';

export default observer((props) => {
  const {
    intlPrefix,
    prefixCls,
  } = useClusterMainStore();
  const {
    formatMessage,
    contentStore,
    projectId,
    clusterId,
  } = useClusterContentStore();

  const {
    getPrometheusStatus,
    getPrometheusLoading,
  } = contentStore;

  const progressData = useMemo(() => omit(getPrometheusStatus, ['error']), [getPrometheusStatus]);
  const progressError = useMemo(() => getPrometheusStatus.error, [getPrometheusStatus]);

  useEffect(() => {
    // const { getComponentList } = contentStore;
    // const item = find(getComponentList, { type: 'prometheus', status: 'processing', operate: 'install' });
    // item && contentStore.loadPrometheusStatus(projectId, clusterId);
    contentStore.loadPrometheusStatus(projectId, clusterId);
  }, []);

  return (
    <Spin spinning={getPrometheusLoading}>
      {map(progressData, (value, key) => (
        <div className={`${prefixCls}-install-step`} key={key}>
          <div className={`${prefixCls}-install-step-content`}>
            <span className={`${prefixCls}-install-step-status ${prefixCls}-install-step-status-${value}`} />
            <span className={`${prefixCls}-install-step-text`}>
              {formatMessage({ id: `${intlPrefix}.install.step.${key}` })}
            </span>
            {value === 'failed' && (
              <Fragment>
                {progressError && (
                  <Tooltip title={progressError}>
                    <Icon type="error" className={`${prefixCls}-install-step-icon-failed`} />
                  </Tooltip>
                )}
                <Button
                  funcType="raised"
                  color="primary"
                  size="small"
                  className={`${prefixCls}-install-step-btn`}
                >
                  {formatMessage({ id: `${intlPrefix}.install.step.retry` })}
                </Button>
              </Fragment>
            )}
          </div>
          {key !== 'installPrometheus' && (
            <div className={`${prefixCls}-install-step-line ${prefixCls}-install-step-${value}`} />
          )}
        </div>
      ))}
    </Spin>
  );
});
