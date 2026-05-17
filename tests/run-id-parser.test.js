const assert = require('assert');
const { extractRunId, updateRunIdParseState } = require('../src/index');

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

  console.log('run-id parser tests passed');
}

runTests();

