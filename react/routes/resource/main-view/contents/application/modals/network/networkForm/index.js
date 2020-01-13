import React, { Component, Fragment } from 'react';
import { observer, inject } from 'mobx-react';
import { withRouter } from 'react-router-dom';
import { injectIntl, FormattedMessage } from 'react-intl';
import {
  Button,
  Form,
  Select,
  Input,
  Popover,
  Icon,
  Radio,
} from 'choerodon-ui';
import uuidv1 from 'uuid/v1';
import classnames from 'classnames';
import _ from 'lodash';

import '../../../../../../../main.less';
import './index.less';

const { Item: FormItem } = Form;
const { Option, OptGroup } = Select;
const { Group: RadioGroup } = Radio;
const formItemLayout = {
  labelCol: {
    xs: { span: 24 },
    sm: { span: 100 },
  },
  wrapperCol: {
    xs: { span: 24 },
    sm: { span: 26 },
  },
};

@Form.create({})
@injectIntl
@withRouter
@inject('AppState')
@observer
export default class CreateNetwork extends Component {
  constructor(props) {
    super(props);
    /* **************
     *                        state              this
     * portKeys/targetKeys | 用于radio选择模式 | 生成一组表单项的唯一表示
     *
     ************** */
    this.state = {
      targetKeys: 'instance',
      portKeys: 'ClusterIP',
      initName: '',
      validIp: {},
      targetIp: {},
      initIst: '',
      initIstOption: [],
      selectEnv: props.envId,
    };
    this.portKeys = 1;
    this.targetKeys = 0;
    this.endPoints = 0;
  }

  componentDidMount() {
    const {
      AppState: { currentMenuType: { projectId } },
      store,
      envId,
      appServiceId,
      intl: { formatMessage },
    } = this.props;
    store.loadInstance(projectId, envId, appServiceId)
      .then((data) => {
        if (data) {
          const initIst = '';
          // 将默认选项直接生成，避免加载带来的异步问题
          const initIstOption = [];
          if (data && data.length) {
            _.forEach(data, (item) => {
              const { id: istIds, code } = item;
              initIstOption.push(
                <Option key={istIds} value={code}>
                  {code}
                </Option>,
              );
            });
          }
          initIstOption.unshift(
            <Option key="all_instance" value="all_instance">
              {formatMessage({ id: 'all_instance' })}
            </Option>
          );
          this.setState({
            initIst,
            initIstOption,
          });
        }
      });

    store.loadPorts(projectId, envId, appServiceId);
  }

  /**
   * 检查名字的唯一性
   * @param rule
   * @param value
   * @param callback
   */
  checkName = _.debounce((rule, value, callback) => {
    const {
      intl: { formatMessage },
      store,
      AppState: { currentMenuType: { projectId } },
    } = this.props;
    const { selectEnv } = this.state;
    const pattern = /^[a-z]([-a-z0-9]*[a-z0-9])?$/;
    if (value && !pattern.test(value)) {
      callback(formatMessage({ id: 'network.name.check.failed' }));
    } else if (value && pattern.test(value)) {
      store.checkNetWorkName(projectId, selectEnv, value)
        .then((data) => {
          if (data) {
            callback();
          } else {
            callback(formatMessage({ id: 'network.name.check.exist' }));
          }
        });
    } else {
      callback();
    }
  }, 1000);

  /**
   * 验证ip
   * @param rule
   * @param value
   * @param callback
   */
  checkIP = (value, name, record) => {
    const { intl: { formatMessage } } = this.props;
    const p = /^((\d|[1-9]\d|1\d{2}|2[0-4]\d|25[0-5])\.){3}(\d|[1-9]\d|1\d{2}|2[0-4]\d|25[0-5])$/;
    const validIp = {};
    let errorMsg;
    if (value && value.length) {
      _.forEach(value, (item) => {
        if (!p.test(item)) {
          errorMsg = formatMessage({ id: 'network.ip.check.failed' });
          validIp[item] = true;
        }
      });
      return errorMsg;
    }
  };

  /**
   * 验证端口号
   * @param rule
   * @param value
   * @param callback
   * @param type
   */
  checkPort = (rule, value, callback, type) => {
    const {
      intl: { formatMessage },
      form: { getFieldValue },
    } = this.props;
    const p = /^([1-9]\d*|0)$/;
    const count = _.countBy(getFieldValue(type));
    const data = {
      typeMsg: '',
      min: 0,
      max: 65535,
      failedMsg: 'network.port.check.failed',
    };
    switch (type) {
      case 'tport':
        data.typeMsg = 'network.tport.check.repeat';
        break;
      case 'nport':
        data.typeMsg = 'network.nport.check.repeat';
        data.min = 30000;
        data.max = 32767;
        data.failedMsg = 'network.nport.check.failed';
        break;
      case 'targetport':
        data.typeMsg = 'network.tport.check.repeat';
        break;
      default:
        data.typeMsg = 'network.port.check.repeat';
    }
    if (value) {
      if (
        p.test(value)
        && parseInt(value, 10) >= data.min
        && parseInt(value, 10) <= data.max
      ) {
        if (count[value] < 2) {
          callback();
        } else {
          callback(formatMessage({ id: data.typeMsg }));
        }
      } else {
        callback(formatMessage({ id: data.failedMsg }));
      }
    } else {
      callback();
    }
  };

  /**
   * 关键字检查
   * @param rule
   * @param value
   * @param callback
   */
  checkKeywords = (rule, value, callback) => {
    const {
      intl: { formatMessage },
      form: { getFieldValue },
    } = this.props;

    // 必须由字母数字字符，' - '，'_'或'.'组成，并且必须以字母数字开头和结尾
    // 并且包括可选的DNS子域前缀(包括一级、二级域名)和'/'（例如'example.com/MyName'）
    const p = /^((?=^.{3,255}$)[a-zA-Z0-9][-a-zA-Z0-9]{0,62}(\.[a-zA-Z0-9][-a-zA-Z0-9]{0,62})+\/)*([A-Za-z0-9][-A-Za-z0-9_.]*)?[A-Za-z0-9]$/;
    const keyCount = _.countBy(getFieldValue('keywords'));
    if (value) {
      if (p.test(value)) {
        if (keyCount[value] < 2) {
          callback();
        } else {
          callback(formatMessage({ id: 'network.label.check.repeat' }));
        }
      } else {
        callback(formatMessage({ id: 'network.label.check.failed' }));
      }
    } else {
      callback();
    }
  };

  checkValue = (rule, value, callback) => {
    const { intl: { formatMessage } } = this.props;
    const p = /^(([A-Za-z0-9][-A-Za-z0-9_.]*)?[A-Za-z0-9])?$/;
    if (value) {
      if (p.test(value)) {
        callback();
      } else {
        callback(formatMessage({ id: 'network.label.check.failed' }));
      }
    } else {
      callback();
    }
  };

  /**
   * 目标和网络配置类型选择
   * @param e
   * @param key
   */
  handleTypeChange = (e, key) => {
    const {
      form: {
        getFieldDecorator,
        resetFields,
        setFieldsValue,
      },
      store,
      envId,
      appServiceId,
      AppState: { currentMenuType: { projectId } },
    } = this.props;

    if (key === 'portKeys') {
      // 清除多组port映射
      this.portKeys = 1;
      // 清空表单项
      resetFields(['port', 'tport', 'nport', 'protocol']);
      setFieldsValue({
        [key]: [0],
      });
    } else {
      // 切换到“选择实例”时，清空标签、endPoints生成的表单项
      let value = e.target.value;
      if (e.target.value === 'param') {
        store.loadLabels(projectId, envId, appServiceId);
        value = 'targetKeys';
      }
      _.map(['targetKeys', 'endPoints'], (item) => {
        if (value === item) {
          getFieldDecorator(item, { initialValue: [0] });
          this[item] = 1;
          setFieldsValue({
            [item]: [0],
          });
        } else {
          const list = {
            targetKeys: ['keywords', 'values'],
            endPoints: ['targetport'],
          };
          this[item] = 0;
          getFieldDecorator(item, { initialValue: [] });
          setFieldsValue({
            [item]: [],
          });
          resetFields(list[item]);
        }
      });
    }
    this.setState({ [key]: e.target.value });
  };

  /**
   * 移除一组表单项
   * @param k
   * @param type
   */
  removeGroup = (k, type) => {
    const {
      form: {
        getFieldValue,
        setFieldsValue,
        validateFields,
      },
    } = this.props;
    const { portKeys } = this.state;
    const keys = getFieldValue(type);
    if (keys.length === 1) {
      return;
    }

    let list = [];
    switch (type) {
      case 'portKeys':
        list = ['port', 'tport'];
        portKeys !== 'ClusterIP' && list.push('nport');
        break;
      case 'endPoints':
        list = ['targetport'];
        break;
      case 'targetKeys':
        list = ['keywords'];
        break;
      default:
        break;
    }

    setFieldsValue({
      [type]: _.filter(keys, (key) => key !== k),
    }, () => validateFields(list, { force: true }));
  };

  /**
   * 动态生成一组表单项
   * @param type
   */
  addGroup = (type) => {
    const {
      form: {
        getFieldValue,
        setFieldsValue,
      },
    } = this.props;
    const keys = getFieldValue(type);
    const uuid = this[type];
    const nextKeys = _.concat(keys, uuid);
    this[type] = uuid + 1;
    setFieldsValue({
      [type]: nextKeys,
    });
  };

  /**
   * 每当节点端口、端口、目标端口、关键字等输入改变，强制校验，消除重复的报错信息
   */
  changeValue = _.debounce((type, keyFiled, valueFiled, value) => {
    const {
      form: {
        validateFields,
        getFieldValue,
      },
    } = this.props;

    if (type === 'keywords') {
      this.selectLabel(value, keyFiled, valueFiled);
    }

    validateFields([type], { force: true });
  }, 400);

  /**
   * 处理输入的内容并返回给value
   * @param liNode
   * @param value
   * @returns {*}
   */
  handleChoiceRender = (liNode, value, type) => React.cloneElement(liNode, {
    className: classnames(liNode.props.className, {
      'ip-check-error': this.state[type || 'validIp'][value],
    }),
  });

  /**
   * 删除ip选择框中的标签校验标识
   * @param value
   */
  handleChoiceRemove = (value, type) => {
    const data = this.state[type || 'validIp'];
    // 直接删除
    if (value in data) {
      delete data[value];
    }
  };

  /**
   * ip选择框监听键盘按下事件
   * @param e
   */
  handleInputKeyDown = (e, type) => {
    const { value } = e.target;
    if (e.keyCode === 13 && !e.isDefaultPrevented() && value) {
      this.setIpInSelect(value, type);
    }
  };

  setIpInSelect = (value, type) => {
    const {
      form: {
        getFieldValue,
        validateFields,
        setFieldsValue,
      },
    } = this.props;
    const itemType = type || 'externalIps';
    const ip = getFieldValue(itemType) || [];
    if (!ip.includes(value)) {
      ip.push(value);
      setFieldsValue({
        [itemType]: ip,
      });
    }
    validateFields([itemType]);
    const data = type === 'targetIps' ? this.targetIpSelect : this.ipSelect;
    if (data) {
      data.setInputValue('');
    }
  };

  ipSelectRef = (node, type) => {
    const data = type === 'targetIps' ? 'targetIpSelect' : 'ipSelect';
    if (node) {
      this[data] = node.rcSelect;
    }
  };

  selectLabel = (value, keyFiled, valueFiled) => {
    const { form: { setFieldsValue, validateFields } } = this.props;
    if (_.includes(value, '__')) {
      setFieldsValue({
        [keyFiled]: value.split('__')[0],
        [valueFiled]: value.split('__')[1],
      });
      validateFields(['keywords'], { force: true });
    }
  };

  render() {
    const {
      form: {
        getFieldDecorator,
        getFieldValue,
      },
      intl: { formatMessage },
      store,
      envId,
      isInstancePage,
    } = this.props;
    const {
      targetKeys: targetType,
      portKeys: configType,
      initIst,
      initIstOption,
      initName,
      selectEnv,
    } = this.state;

    const ist = store.getIst;
    const keyOption = [];
    _.forEach(store.getLabels, (item) => {
      const data = _.map(item, (value, key) => (
        <Option key={`${key}__${value}__${uuidv1()}`}>{key}:{value}</Option>
      ));
      keyOption.push(...data);
    });

    // 生成多组 port
    getFieldDecorator('portKeys', { initialValue: [0] });
    const portKeys = getFieldValue('portKeys');
    const portItems = _.map(portKeys, (k) => (
      <div key={`port-${k}`} className="network-port-wrap">
        {configType !== 'ClusterIP' && (
          <FormItem
            className="c7n-select_115 network-panel-form network-port-form"
            {...formItemLayout}
          >
            {getFieldDecorator(`nport[${k}]`, {
              rules: [
                {
                  validator: (rule, value, callback) => this.checkPort(rule, value, callback, 'nport'),
                },
              ],
            })(
              <Input
                type="text"
                maxLength={5}
                onChange={this.changeValue.bind(this, 'nport')}
                label={<FormattedMessage id="network.config.nodePort" />}
              />,
            )}
          </FormItem>
        )}
        <FormItem
          className="c7n-select_115 network-panel-form network-port-form"
          {...formItemLayout}
        >
          {getFieldDecorator(`port[${k}]`, {
            rules: [
              {
                required: true,
                message: formatMessage({ id: 'required' }),
              },
              {
                validator: (rule, value, callback) => this.checkPort(rule, value, callback, 'port'),
              },
            ],
          })(
            <Input
              type="text"
              maxLength={5}
              disabled={!selectEnv}
              onChange={this.changeValue.bind(this, 'port')}
              label={<FormattedMessage id="network.config.port" />}
            />,
          )}
        </FormItem>
        <FormItem
          className="c7n-select_115 network-panel-form network-port-form"
          {...formItemLayout}
        >
          {getFieldDecorator(`tport[${k}]`, {
            rules: [
              {
                required: true,
                message: formatMessage({ id: 'required' }),
              },
              {
                validator: (rule, value, callback) => this.checkPort(rule, value, callback, 'tport'),
              },
            ],
          })(
            <Select
              mode="combobox"
              maxLength={5}
              onChange={this.changeValue.bind(this, 'tport')}
              disabled={!selectEnv}
              label={<FormattedMessage id="network.config.targetPort" />}
              dropdownMatchSelectWidth={false}
            >
              {_.map(store.getPorts, ({ resourceName, portValue }) => (
                <Option key={portValue}>{resourceName}: {portValue}</Option>
              ))}
            </Select>,
          )}
        </FormItem>
        {configType === 'NodePort' && (
          <FormItem
            className="c7n-select_115 network-panel-form network-port-form"
            {...formItemLayout}
          >
            {getFieldDecorator(`protocol[${k}]`, {
              rules: [
                {
                  required: true,
                  message: formatMessage({ id: 'required' }),
                },
              ],
            })(
              <Select
                label={<FormattedMessage id="ist.deploy.ports.protocol" />}
              >
                {_.map(['TCP', 'UDP'], (item) => (
                  <Option value={item} key={item}>
                    {item}
                  </Option>
                ))}
              </Select>,
            )}
          </FormItem>
        )}
        {portKeys.length > 1 && (
          <Icon
            className="network-group-icon"
            type="delete"
            onClick={() => this.removeGroup(k, 'portKeys')}
          />
        )}
      </div>
    ));

    // endPoints生成多组 port
    getFieldDecorator('endPoints');
    const endPoints = getFieldValue('endPoints');
    const targetPortItems = _.map(endPoints, (k) => (
      <div key={`endPoints-${k}`} className="network-port-wrap">
        <FormItem
          className="network-panel-form network-port-form"
          {...formItemLayout}
        >
          {getFieldDecorator(`targetport[${k}]`, {
            rules: [
              {
                required: true,
                message: formatMessage({ id: 'required' }),
              },
              {
                validator: (rule, value, callback) => this.checkPort(rule, value, callback, 'targetport'),
              },
            ],
          })(
            <Input
              type="text"
              maxLength={5}
              disabled={!selectEnv}
              onChange={this.changeValue.bind(this, 'targetport')}
              label={<FormattedMessage id="network.config.targetPort" />}
            />,
          )}
        </FormItem>
        {endPoints.length > 1 && (
          <Icon
            className="network-group-icon"
            type="delete"
            onClick={() => this.removeGroup(k, 'endPoints')}
          />
        )}
      </div>
    ));

    // 生成多组 target
    getFieldDecorator('targetKeys');
    const targetKeys = getFieldValue('targetKeys');
    const targetItems = _.map(targetKeys, (k) => (
      <div key={`target-${k}`} className="network-port-wrap">
        <FormItem
          className={`c7n-select_${
            targetKeys.length > 1 ? 'entryS' : 'entryL'
          } network-panel-form network-port-form`}
          {...formItemLayout}
        >
          {getFieldDecorator(`keywords[${k}]`, {
            rules: [
              {
                required: true,
                message: formatMessage({ id: 'required' }),
              },
              {
                validator: this.checkKeywords,
              },
            ],
          })(
            <Select
              mode="combobox"
              className="network-select-instance"
              onChange={(value) => this.changeValue('keywords', `keywords[${k}]`, `values[${k}]`, value)}
              disabled={!selectEnv}
              label={<FormattedMessage id="network.config.keyword" />}
              dropdownMatchSelectWidth={false}
            >
              {keyOption}
            </Select>,
          )}
        </FormItem>
        <Icon className="network-group-icon" type="drag_handle" />
        <FormItem
          className={`c7n-select_${
            targetKeys.length > 1 ? 'entryS' : 'entryL'
          } network-panel-form network-port-form`}
          {...formItemLayout}
        >
          {getFieldDecorator(`values[${k}]`, {
            rules: [
              {
                required: true,
                message: formatMessage({ id: 'required' }),
              },
              {
                validator: this.checkValue,
              },
            ],
          })(
            <Select
              mode="combobox"
              disabled={!selectEnv}
              onChange={(value) => this.changeValue('keywords', `keywords[${k}]`, `values[${k}]`, value)}
              label={<FormattedMessage id="network.config.value" />}
              dropdownMatchSelectWidth={false}
            >
              {keyOption}
            </Select>,
          )}
        </FormItem>
        {targetKeys.length > 1 && (
          <Icon
            className="network-group-icon"
            type="delete"
            onClick={() => this.removeGroup(k, 'targetKeys')}
          />
        )}
      </div>
    ));

    return (
      <Form layout="vertical" className="c7n-network-form-wrap">
        <FormItem
          className="network-form-name"
          {...formItemLayout}
        >
          {getFieldDecorator('name', {
            initialValue: initName,
            rules: [
              {
                required: true,
                message: formatMessage({ id: 'required' }),
              },
              {
                validator: this.checkName,
              },
            ],
          })(
            <Input
              disabled={!selectEnv}
              maxLength={30}
              type="text"
              label={<FormattedMessage id="network.form.name" />}
              autoFocus
            />,
          )}
        </FormItem>
        <div
          className={`network-panel-title ${
            !selectEnv ? 'network-panel-title_disabled' : ''
          }`}
        >
          <FormattedMessage id="network.target" />
        </div>
        <FormItem
          className="network-radio-form"
          label={<FormattedMessage id="chooseType" />}
          {...formItemLayout}
        >
          {getFieldDecorator('target', {
            initialValue: targetType,
          })(
            <RadioGroup
              name="target"
              disabled={!selectEnv}
              onChange={(e) => this.handleTypeChange(e, 'targetKeys')}
            >
              <Radio value="instance">
                <FormattedMessage id="network.target.instance" />
              </Radio>
              <Radio value="param">
                <FormattedMessage id="network.target.param" />
              </Radio>
            </RadioGroup>,
          )}
        </FormItem>
        <div className="network-panel">
          {targetType === 'instance' && (
            <FormItem
              className="network-panel-form"
              {...formItemLayout}
            >
              {getFieldDecorator('appInstance', {
                initialValue: initIst || undefined,
                trigger: ['onChange', 'onSubmit'],
                rules: [
                  {
                    required: true,
                    message: formatMessage({ id: 'required' }),
                  },
                ],
              })(
                <Select
                  filter
                  className="network-select-instance"
                  optionFilterProp="children"
                  optionLabelProp="children"
                  disabled={!selectEnv}
                  label={
                    <FormattedMessage id="network.target.instance" />
                  }
                  notFoundContent={formatMessage({
                    id: 'network.form.instance.disable',
                  })}
                  getPopupContainer={(triggerNode) => triggerNode.parentNode}
                  filterOption={(input, option) => option.props.children
                    .toLowerCase()
                    .indexOf(input.toLowerCase()) >= 0}
                >
                  {initIstOption}
                </Select>,
              )}
            </FormItem>
          )}
          {targetType === 'param' && (
            <Fragment>
              {targetItems}
              <Button
                disabled={!selectEnv}
                type="primary"
                funcType="flat"
                onClick={() => this.addGroup('targetKeys')}
                icon="add"
              >
                <FormattedMessage id="network.config.addtarget" />
              </Button>
            </Fragment>
          )}
        </div>
        <div
          className={`network-panel-title ${
            !selectEnv ? 'network-panel-title_disabled' : ''
          }`}
        >
          <FormattedMessage id="network.config" />
        </div>
        <div>
          <FormItem
            className="network-radio-form"
            {...formItemLayout}
          >
            {getFieldDecorator('config', {
              initialValue: configType,
            })(
              <RadioGroup
                name="config"
                disabled={!selectEnv}
                onChange={(e) => this.handleTypeChange(e, 'portKeys')}
              >
                <Radio value="ClusterIP">ClusterIP</Radio>
                <Radio value="NodePort">NodePort</Radio>
                <Radio value="LoadBalancer">LoadBalancer</Radio>
              </RadioGroup>,
            )}
          </FormItem>
          <div className="network-panel">
            {configType === 'ClusterIP' && (
              <FormItem
                className="network-panel-form"
                {...formItemLayout}
              >
                {getFieldDecorator('externalIps', {
                  rules: [
                    {
                      validator: this.checkIP,
                    },
                  ],
                })(
                  <Select
                    mode="tags"
                    ref={this.ipSelectRef}
                    disabled={!selectEnv}
                    label={<FormattedMessage id="network.config.ip" />}
                    onInputKeyDown={this.handleInputKeyDown}
                    choiceRender={this.handleChoiceRender}
                    onChoiceRemove={this.handleChoiceRemove}
                    filterOption={false}
                    notFoundContent={false}
                    showNotFindInputItem={false}
                    showNotFindSelectedItem={false}
                    allowClear
                  />,
                )}
              </FormItem>
            )}
            {portItems}
            <Button
              disabled={!selectEnv}
              type="primary"
              funcType="flat"
              onClick={() => this.addGroup('portKeys')}
              icon="add"
            >
              <FormattedMessage id="network.config.addport" />
            </Button>
          </div>
        </div>
      </Form>
    );
  }
}
