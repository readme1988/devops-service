import React from 'react';
import { Route, Switch } from 'react-router-dom';
import { inject } from 'mobx-react';
import { asyncRouter, asyncLocaleProvider, nomatch } from '@choerodon/boot';
import { ModalContainer } from 'choerodon-ui/pro';

const AppService = asyncRouter(() => import('./routes/app-service'));
const Code = asyncRouter(() => import('./routes/code-manager'));
const Resource = asyncRouter(() => import('./routes/resource'));
const Deployment = asyncRouter(() => import('./routes/deployment'));
const Pipeline = asyncRouter(() => import('./routes/pipeline'));
const Certificate = asyncRouter(() => import('./routes/certificate'));
const Cluster = asyncRouter(() => import('./routes/cluster'));

function DEVOPSIndex({ match, AppState: { currentLanguage: language } }) {
  const IntlProviderAsync = asyncLocaleProvider(language, () => import(`./locale/${language}`),);
  return (
    <IntlProviderAsync>
      <div>
        <Switch>
          <Route path={`${match.url}/app-service`} component={AppService} />
          <Route path={`${match.url}/app-service`} component={Code} />
          <Route path={`${match.url}/app-service`} component={Resource} />
          <Route path={`${match.url}/app-service`} component={Deployment} />
          <Route path={`${match.url}/app-service`} component={Pipeline} />
          <Route path={`${match.url}/app-service`} component={Certificate} />
          <Route path={`${match.url}/app-service`} component={Cluster} />
          <Route path="*" component={nomatch} />
        </Switch>
        <ModalContainer />
      </div>
    </IntlProviderAsync>
  );
}

export default inject('AppState')(DEVOPSIndex);
