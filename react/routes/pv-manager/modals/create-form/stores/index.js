import React, { createContext, useContext, useEffect, useMemo } from 'react';
import { inject } from 'mobx-react';
import { injectIntl } from 'react-intl';
import { DataSet } from 'choerodon-ui/pro';
import FormDataSet from './FormDataSet';

const Store = createContext();

export function usePVCreateStore() {
  return useContext(Store);
}

export const StoreProvider = injectIntl(inject('AppState')(
  (props) => {
    const {
      AppState: { currentMenuType: { projectId } },
      intl: { formatMessage },
      children,
      intlPrefix,
    } = props;
    const formDs = useMemo(() => new DataSet(FormDataSet(intlPrefix, formatMessage, projectId)), [projectId]);

    const value = {
      ...props,
      formDs,
    };
    return (
      <Store.Provider value={value}>
        {children}
      </Store.Provider>
    );
  },
));
