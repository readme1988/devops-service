import { axios } from '@choerodon/boot';

export default ({ formatMessage, intlPrefix, projectId, store, envId }) => {
  const handleUpdate = ({ name, value }) => {
    if (name === 'appServiceId' && value) {
      if (value) {
        store.loadValue(projectId, value);
      } else {
        store.setValue('');
      }
    }
  };
  const nameValidator = async (value, name, record) => {
    const id = record.get('id');
    const param = id ? `&deploy_value_id=${id}` : '';
    try {
      const res = await axios.get(`/devops/v1/projects/${projectId}/deploy_value/check_name?name=${encodeURIComponent(value)}&env_id=${envId}${param}`);
      if (res.failed) {
        if (res.code === 'error.devops.pipeline.value.name.exit') {
          return '名称已存在';
        }
        return '名称校验失败，请稍后再试';
      }
      return true;
    } catch (err) {
      return '名称校验失败，请稍后再试';
    }
  };

  return {
    paging: false,
    dataKey: null,
    fields: [{
      name: 'name',
      type: 'string',
      label: '部署配置名称',
      required: true,
      maxLength: 30,
      validator: nameValidator,
    }, {
      name: 'description',
      type: 'string',
      required: true,
      label: '描述',
      maxLength: 200,
    }, {
      name: 'appServiceId',
      type: 'number',
      textField: 'text',
      label: '应用服务',
      required: true,
    }, {
      name: 'appServiceName',
      type: 'string',
      label: '应用服务',
      readOnly: true,
      ignore: 'always',
    }, {
      name: 'value',
      type: 'string',
    }, {
      name: 'envId',
      type: 'number',
    }],
    transport: {
      read: {
        method: 'get',
      },
      submit: ({ data: [data] }) => ({
        url: `/devops/v1/projects/${projectId}/deploy_value`,
        method: 'post',
        data,
      }),
    },
    events: {
      load: ({ dataSet }) => {
        const [data] = dataSet.toData();
        store.setValue(data.value || '');
      },
      update: handleUpdate,
    },
  };
};
