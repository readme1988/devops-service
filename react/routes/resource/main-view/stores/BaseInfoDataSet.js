export default () => ({
  selection: false,
  paging: false,
  dataKey: null,
  fields: [
    { name: 'name', type: 'string' },
    { name: 'connect', type: 'boolean' },
    { name: 'id', type: 'number' },
  ],
  transport: {
    read: {
      method: 'get',
    },
  },
});
