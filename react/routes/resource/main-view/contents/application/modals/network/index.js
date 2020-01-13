/* eslint-disable no-useless-return */
import React, { Component, Fragment } from 'react';
import { observer, inject } from 'mobx-react';
import { withRouter } from 'react-router-dom';
import { injectIntl, FormattedMessage } from 'react-intl';
import { Modal } from 'choerodon-ui';
import { Choerodon } from '@choerodon/boot';
import _ from 'lodash';
import InterceptMask from '../../../../../../../components/intercept-mask';
import NetworkForm from './networkForm';

import '../../../../../../main.less';
import './index.less';
import { handlePromptError } from '../../../../../../../utils';

const { Sidebar } = Modal;

@injectIntl
@withRouter
@inject('AppState')
@observer
class CreateNetwork extends Component {
  state = {
    submitting: false,
  };

  handleSubmit = (e) => {
    e.preventDefault();

    const {
      AppState: { currentMenuType: { projectId } },
      store,
      appServiceId,
      envId,
    } = this.props;
    this.setState({ submitting: true });

    this.formRef.props.form.validateFieldsAndScroll((err, data) => {
      if (!err) {
        const {
          name,
          appInstance,
          endPoints: endps,
          targetIps,
          targetport,
          externalIps,
          portKeys,
          port,
          tport,
          nport,
          protocol,
          targetKeys,
          keywords,
          config,
          values,
        } = data;
        let targetAppServiceId;
        let targetInstanceCode;
        if (!_.isEmpty(appInstance)) {
          if (appInstance === 'all_instance') {
            targetAppServiceId = appServiceId;
          } else {
            targetInstanceCode = appInstance;
          }
        }
        const ports = [];
        const label = {};
        const endPoints = {};

        if (portKeys) {
          _.forEach(portKeys, (item) => {
            if (item || item === 0) {
              const node = {
                port: Number(port[item]),
                targetPort: Number(tport[item]),
                nodePort: nport ? Number(nport[item]) : null,
              };
              config === 'NodePort' && (node.protocol = protocol[item]);
              ports.push(node);
            }
          });
        }

        if (targetKeys) {
          _.forEach(targetKeys, (item) => {
            if (item || item === 0) {
              const key = keywords[item];
              label[key] = values[item];
            }
          });
        }

        // targetIps是必填项，当输入值不为空，但是记录值为空时，点击提交会判断为空
        // 此时会改变为表单校验未通过，但是又会自动把输入值填入记录值，使该项正常
        // 造成需要点击两次才能提交。属于不当操作造成，暂未做处理
        if (endps && endps.length && targetIps) {
          endPoints[targetIps.join(',')] = _.map(
            _.filter(endps, (item) => item || item === 0),
            (item) => ({
              name: null,
              port: Number(targetport[item]),
            }),
          );
        }

        const network = {
          name,
          appServiceId,
          targetAppServiceId,
          targetInstanceCode,
          envId,
          externalIp: externalIps && externalIps.length ? externalIps.join(',') : null,
          ports,
          selectors: !_.isEmpty(label) ? label : null,
          type: config,
          endPoints: !_.isEmpty(endPoints) ? endPoints : null,
        };

        store
          .createNetwork(projectId, network)
          .then((res) => {
            this.setState({ submitting: false });
            if (handlePromptError(res)) {
              this.handleClose();
            }
          })
          .catch((error) => {
            this.setState({ submitting: false });
            Choerodon.handleResponseError(error);
          });
      } else {
        this.setState({ submitting: false });
      }
    });
  };

  handleClose = (isload = true) => {
    const { onClose, store } = this.props;
    store.setIst([]);
    store.setLabels([]);
    store.setPorts([]);
    onClose(isload);
  };

  render() {
    const {
      visible,
      envId,
      store,
      appServiceId,
    } = this.props;
    const {
      submitting,
    } = this.state;

    return (
      <div className="c7n-region">
        <Sidebar
          destroyOnClose
          cancelText={<FormattedMessage id="cancel" />}
          okText={<FormattedMessage id="create" />}
          title={<FormattedMessage id="network.header.create" />}
          visible={visible}
          onOk={this.handleSubmit}
          onCancel={this.handleClose.bind(this, false)}
          confirmLoading={submitting}
          maskClosable={false}
          width={740}
        >
          <NetworkForm
            wrappedComponentRef={(form) => {
              this.formRef = form;
            }}
            envId={envId}
            appServiceId={appServiceId}
            store={store}
          />
          <InterceptMask visible={submitting} />
        </Sidebar>
      </div>
    );
  }
}

export default CreateNetwork;
