export default ({ formatMessage, intlPrefix }) => ({
  selection: false,
  pageSize: 10,
  transport: {},
  fields: [
    {
      name: 'realName',
      type: 'string',
      label: formatMessage({ id: `${intlPrefix}.environment.permission.user` }),
    },
    {
      name: 'loginName',
      type: 'string',
      label: formatMessage({ id: `${intlPrefix}.environment.permission.name` }),
    },
    {
      name: 'role',
      type: 'string',
      label: formatMessage({ id: `${intlPrefix}.environment.permission.role` }),
    },
    {
      name: 'creationDate',
      type: 'dateTime',
      label: formatMessage({ id: `${intlPrefix}.environment.permission.addTime` }),
    },
    { name: 'iamUserId', type: 'number', textField: 'realName', valueField: 'iamUserId', label: formatMessage({ id: `${intlPrefix}.project.member` }) },
  ],
  queryFields: [
    {
      name: 'realName',
      type: 'string',
      label: formatMessage({ id: `${intlPrefix}.environment.permission.user` }),
    },
    {
      name: 'loginName',
      type: 'string',
      label: formatMessage({ id: `${intlPrefix}.environment.permission.name` }),
    },
  ],
});
