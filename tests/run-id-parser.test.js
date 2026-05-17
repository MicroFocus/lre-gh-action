const assert = require('assert');
const {
  extractRunId,
  updateRunIdParseState,
  resolveWorkspaceSyncDiffRange,
  selectChangedFilesUnderWorkspace
} = require('../src/index');

function runTests() {
  assert.strictEqual(extractRunId('foo lre_run_id=12345 bar'), '12345');
  assert.strictEqual(extractRunId('Run started (TestID: 8, RunID: 91, TimeslotID: 3)'), '91');
  assert.strictEqual(extractRunId('no run id here'), null);

  let state = { lreRunId: null, parseTail: '' };
  state = updateRunIdParseState(state, 'prefix lre_run_');
  state = updateRunIdParseState(state, 'id=777 suffix');
  assert.strictEqual(state.lreRunId, '777');

  state = { lreRunId: null, parseTail: '' };
  state = updateRunIdParseState(state, 'Run started (TestID: 8, Run');
  state = updateRunIdParseState(state, 'ID: 444, TimeslotID: 3)');
  assert.strictEqual(state.lreRunId, '444');

  state = { lreRunId: null, parseTail: '' };
  state = updateRunIdParseState(state, 'line on stdout with nothing useful');
  state = updateRunIdParseState(state, 'line on stderr: lre_run_id=555');
  assert.strictEqual(state.lreRunId, '555');

  const pushRange = resolveWorkspaceSyncDiffRange('', 'push', { before: 'abc123', after: 'def456' }, 'def456');
  assert.deepStrictEqual(pushRange, { baseSha: 'abc123', headSha: 'def456', source: 'push event' });

  const explicitRange = resolveWorkspaceSyncDiffRange('base999', 'workflow_dispatch', null, 'head888');
  assert.deepStrictEqual(explicitRange, {
    baseSha: 'base999',
    headSha: 'head888',
    source: 'lre_workspace_sync_base_sha'
  });

  const noRange = resolveWorkspaceSyncDiffRange('', 'workflow_dispatch', null, 'head888');
  assert.strictEqual(noRange, null);

  const selected = selectChangedFilesUnderWorkspace(
    ['scripts/s1/file.txt', 'scripts/s2/a.usr', 'README.md'],
    '/repo',
    '/repo/scripts'
  );
  assert.deepStrictEqual(selected.sort(), ['s1/file.txt', 's2/a.usr']);

  const outsideWorkspace = selectChangedFilesUnderWorkspace(['scripts/s1/file.txt'], '/repo', '/other/workspace');
  assert.strictEqual(outsideWorkspace, null);

  console.log('run-id parser tests passed');
}

runTests();

