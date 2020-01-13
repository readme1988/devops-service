import React, { createContext, useMemo, useContext, useEffect } from 'react';
import { inject } from 'mobx-react';
import { observer } from 'mobx-react-lite';
import { injectIntl } from 'react-intl';
import { DataSet } from 'choerodon-ui/pro';
import FormDataSet from './FormDataSet';
import PvOptionsDataSet from './PvOptionsDataSet';

const Store = createContext();

export function usePrometheusStore() {
  return useContext(Store);
}

export const StoreProvider = injectIntl(inject('AppState')(
  observer((props) => {
    const {
      children,
      intl: { formatMessage },
      intlPrefix,
      type,
      AppState: { currentMenuType: { projectId } },
      clusterId,
    } = props;

    const pvDs = useMemo(() => new DataSet(PvOptionsDataSet(projectId, clusterId)), [projectId, clusterId]);
    const formDs = useMemo(() => new DataSet(FormDataSet({ formatMessage, intlPrefix, projectId, clusterId, pvDs })), [projectId, clusterId]);

    useEffect(() => {
      if (type === 'create') {
        formDs.create();
      } else {
        formDs.query();
      }
    }, [type]);

    const value = {
      ...props,
      pvSelect: ['prometheusPvId', 'grafanaPvId', 'alertmanagerPvId'],
      pvSelectEdit: {
        prometheusPvId: 'prometheusPvName',
        grafanaPvId: 'grafanaPvName',
        alertmanagerPvId: 'alertmanagerPvName',
      },
      formDs,
    };
    return (
      <Store.Provider value={value}>
        {children}
      </Store.Provider>
    );
  })
));
