import React, { createContext, useContext, useMemo } from 'react';
import { DataSet } from 'choerodon-ui/pro';
import { inject } from 'mobx-react';
import { injectIntl } from 'react-intl';
import TableDataSet from './TableDataSet';
import { useDeploymentStore } from '../../../../stores';
import useStore from './useStore';

const Store = createContext();

export function useCustomStore() {
  return useContext(Store);
}

export const StoreProvider = injectIntl(inject('AppState')(
  (props) => {
    const { AppState: { currentMenuType: { id } }, children } = props;
    const {
      intlPrefix,
      intl: { formatMessage },
      deploymentStore: { getSelectedMenu: { parentId } },
    } = useDeploymentStore();
    const customDs = useMemo(() => new DataSet(TableDataSet({
      formatMessage,
      intlPrefix,
      projectId: id,
      envId: parentId,
    })), [formatMessage, id, intlPrefix, parentId]);

    const customStore = useStore();
  
    const value = {
      ...props,
      customDs,
      customStore,
    };
  
    return (
      <Store.Provider value={value}>
        {children}
      </Store.Provider>
    );
  }
));