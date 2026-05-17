const core = require('@actions/core');
const { spawn, execFileSync } = require('child_process');
const fsSync = require('fs');
const fs = require('fs').promises;
const path = require('path');

const ACTION_EXECUTE_TEST = 'ExecuteLreTest';
const ACTION_WORKSPACE_SYNC = 'WorkspaceSync';

function resolveAction(rawAction) {
  return normalize(rawAction) || ACTION_EXECUTE_TEST;
}

function normalize(value) {
  return typeof value === 'string' ? value.trim() : '';
}

function resolveBooleanInput(rawValue, defaultValue = false) {
  if (rawValue === 'true') {
    return true;
  }
  if (rawValue === 'false') {
    return false;
  }
  return defaultValue;
}

function parseNonNegativeInt(rawValue, defaultValue) {
  const parsed = Number.parseInt(rawValue, 10);
  if (Number.isNaN(parsed) || parsed < 0) {
    return defaultValue;
  }
  return parsed;
}

function requireValue(value, key) {
  if (!normalize(value)) {
    throw new Error(`Input parameter "${key}" is required.`);
  }
  return value;
}

function getEnvOrEmpty(key) {
  return normalize(process.env[key]);
}

function resolveWorkspaceAndOutputDirs(rawOutputDir, rawWorkspaceDir) {
  const outputDir = normalize(rawOutputDir);
  const workspaceDir = normalize(rawWorkspaceDir);

  if (!outputDir && !workspaceDir) {
    return { lreOutputDir: './', lreWorkspaceDir: './' };
  }

  if (outputDir && !workspaceDir) {
    return { lreOutputDir: outputDir, lreWorkspaceDir: outputDir };
  }

  if (!outputDir && workspaceDir) {
    return { lreOutputDir: workspaceDir, lreWorkspaceDir: workspaceDir };
  }

  return { lreOutputDir: outputDir, lreWorkspaceDir: workspaceDir };
}

function extractRunId(text) {
  const markerMatch = text.match(/lre_run_id=(\d+)/i);
  if (markerMatch && markerMatch[1]) {
    return markerMatch[1];
  }

  const runIdMatch = text.match(/RunID:\s*(\d+)/i);
  if (runIdMatch && runIdMatch[1]) {
    return runIdMatch[1];
  }

  return null;
}

function updateRunIdParseState(state, output) {
  const parseTail = state.parseTail || '';
  const combined = parseTail + output;
  const foundRunId = extractRunId(combined);

  return {
    lreRunId: foundRunId || state.lreRunId || null,
    // Keep a short tail so regex can still match when tokens are split across chunks.
    parseTail: combined.slice(-200)
  };
}

function toPosixPath(inputPath) {
  return normalize(inputPath).replace(/\\/g, '/').replace(/^\.\//, '');
}

function isZeroSha(value) {
  return /^0+$/.test(value || '');
}

function readGithubEventPayload() {
  const eventPath = normalize(process.env.GITHUB_EVENT_PATH);
  if (!eventPath) {
    return null;
  }

  try {
    const content = fsSync.readFileSync(eventPath, 'utf8');
    return JSON.parse(content);
  } catch (error) {
    core.warning(`Failed to read GitHub event payload from ${eventPath}: ${error.message}`);
    return null;
  }
}

function resolveWorkspaceSyncDiffRange(baseShaInput, eventName, payload, githubSha) {
  const explicitBaseSha = normalize(baseShaInput);
  const resolvedHeadSha = normalize(githubSha);

  if (explicitBaseSha) {
    return {
      baseSha: explicitBaseSha,
      headSha: resolvedHeadSha || null,
      source: 'lre_workspace_sync_base_sha'
    };
  }

  if (!payload) {
    return null;
  }

  if (eventName === 'push') {
    const baseSha = normalize(payload.before);
    const headSha = normalize(payload.after) || resolvedHeadSha;
    if (!baseSha || isZeroSha(baseSha) || !headSha) {
      return null;
    }
    return { baseSha, headSha, source: 'push event' };
  }

  if (eventName === 'pull_request' || eventName === 'pull_request_target') {
    const baseSha = normalize(payload.pull_request && payload.pull_request.base && payload.pull_request.base.sha);
    const headSha = normalize(payload.pull_request && payload.pull_request.head && payload.pull_request.head.sha)
      || resolvedHeadSha;
    if (!baseSha || !headSha) {
      return null;
    }
    return { baseSha, headSha, source: `${eventName} event` };
  }

  return null;
}

function selectChangedFilesUnderWorkspace(changedFiles, repoRoot, workspaceDir) {
  const normalizedRepoRoot = path.resolve(repoRoot);
  const normalizedWorkspaceRoot = path.resolve(workspaceDir);
  const workspaceRelativeRoot = toPosixPath(path.relative(normalizedRepoRoot, normalizedWorkspaceRoot));

  if (workspaceRelativeRoot.startsWith('..') || path.isAbsolute(workspaceRelativeRoot)) {
    return null;
  }

  const prefix = workspaceRelativeRoot ? `${workspaceRelativeRoot}/` : '';
  const selected = new Set();

  for (const changedFile of changedFiles) {
    const normalizedChangedFile = toPosixPath(changedFile);
    if (!normalizedChangedFile) {
      continue;
    }

    if (!prefix) {
      selected.add(normalizedChangedFile);
      continue;
    }

    if (normalizedChangedFile === workspaceRelativeRoot || normalizedChangedFile.startsWith(prefix)) {
      const workspaceRelativeFile = normalizedChangedFile.slice(prefix.length);
      if (workspaceRelativeFile) {
        selected.add(workspaceRelativeFile);
      }
    }
  }

  return Array.from(selected);
}

function resolveWorkspaceRelativeChangedFiles(diffRange, workspaceDir, repoRoot) {
  const gitArgs = ['diff', '--name-only', diffRange.baseSha];
  if (diffRange.headSha) {
    gitArgs.push(diffRange.headSha);
  }

  const output = execFileSync('git', gitArgs, {
    cwd: repoRoot,
    encoding: 'utf8',
    stdio: ['ignore', 'pipe', 'pipe']
  });

  const changedFiles = output
    .split(/\r?\n/)
    .map((item) => item.trim())
    .filter(Boolean);

  return selectChangedFilesUnderWorkspace(changedFiles, repoRoot, workspaceDir);
}

function resolveWorkspaceRelativeDeletedFiles(diffRange, workspaceDir, repoRoot) {
  const gitArgs = ['diff', '--diff-filter=D', '--name-only', diffRange.baseSha];
  if (diffRange.headSha) {
    gitArgs.push(diffRange.headSha);
  }

  const output = execFileSync('git', gitArgs, {
    cwd: repoRoot,
    encoding: 'utf8',
    stdio: ['ignore', 'pipe', 'pipe']
  });

  const deletedFiles = output
    .split(/\r?\n/)
    .map((item) => item.trim())
    .filter(Boolean);

  return selectChangedFilesUnderWorkspace(deletedFiles, repoRoot, workspaceDir);
}

function enrichWorkspaceSyncConfig(config) {
   if (config.lre_action !== ACTION_WORKSPACE_SYNC) {
     return config;
   }

   const deleteRemovedScripts = resolveBooleanInput(core.getInput('lre_workspace_sync_delete_removed_scripts'), false);
   config.lre_workspace_sync_delete_removed_scripts = deleteRemovedScripts;
   config.lre_workspace_sync_changes_determined = false;
   config.lre_workspace_sync_changed_files = [];
   config.lre_workspace_sync_deleted_files = [];

   // Automatically attempt incremental sync based on available diff context
   const payload = readGithubEventPayload();
   const diffRange = resolveWorkspaceSyncDiffRange(
     core.getInput('lre_workspace_sync_base_sha'),
     normalize(process.env.GITHUB_EVENT_NAME),
     payload,
     process.env.GITHUB_SHA
   );

   if (!diffRange) {
     core.info('WorkspaceSync: no incremental sync context available, performing full sync.');
     return config;
   }

   try {
     const changedFiles = resolveWorkspaceRelativeChangedFiles(diffRange, config.lre_workspace_dir, process.cwd());
     const deletedFiles = resolveWorkspaceRelativeDeletedFiles(diffRange, config.lre_workspace_dir, process.cwd());
     if (changedFiles === null) {
       core.info('WorkspaceSync: workspace is outside the checked-out repository, falling back to full sync.');
       return config;
     }

     if (deletedFiles === null) {
       core.info('WorkspaceSync: workspace is outside the checked-out repository, falling back to full sync.');
       return config;
     }

      config.lre_workspace_sync_incremental = true;
      config.lre_workspace_sync_changes_determined = true;
      config.lre_workspace_sync_changed_files = changedFiles;
      config.lre_workspace_sync_deleted_files = deletedFiles;
      core.info(`WorkspaceSync: performing incremental sync with ${changedFiles.length} changed file(s), ${deletedFiles.length} deleted file(s) found using ${diffRange.source}.`);
     return config;
    } catch (error) {
      const hint = error.message && error.message.includes('git diff')
        ? ' Tip: make sure your checkout step uses fetch-depth: 0 so that both base and head commits are available locally.'
        : '';
      core.warning(`WorkspaceSync: failed to compute changed files: ${error.message}.${hint} Falling back to full sync.`);
      return config;
    }
}

function buildConfig() {
  const lreAction = resolveAction(core.getInput('lre_action'));
  if (lreAction !== ACTION_EXECUTE_TEST && lreAction !== ACTION_WORKSPACE_SYNC) {
    throw new Error(`Input parameter "lre_action" must be either "${ACTION_EXECUTE_TEST}" or "${ACTION_WORKSPACE_SYNC}".`);
  }

  const lreDescription = normalize(core.getInput('lre_description')) ||
      (lreAction === ACTION_WORKSPACE_SYNC ? 'Synchronizing workspace scripts' : 'Executing LRE test');

  const lreServer = requireValue(core.getInput('lre_server'), 'lre_server');
  const lreUsername = requireValue(getEnvOrEmpty('lre_username'), 'lre_username');
  const lrePassword = requireValue(getEnvOrEmpty('lre_password'), 'lre_password');
  const lreDomain = requireValue(core.getInput('lre_domain'), 'lre_domain');
  const lreProject = requireValue(core.getInput('lre_project'), 'lre_project');

  const lreHttpsProtocol = resolveBooleanInput(core.getInput('lre_https_protocol'), true);
  const lreAuthenticateWithToken = resolveBooleanInput(core.getInput('lre_authenticate_with_token'), false);

  const lreTest = normalize(core.getInput('lre_test'));
  if (lreAction === ACTION_EXECUTE_TEST) {
    requireValue(lreTest, 'lre_test');
  }

  const rawTestInstance = normalize(core.getInput('lre_test_instance'));
  const parsedTestInstance = Number.parseInt(rawTestInstance, 10);
  const lreTestInstance = (!rawTestInstance || (!Number.isNaN(parsedTestInstance) && parsedTestInstance <= 0))
      ? 'AUTO'
      : rawTestInstance;

  const lreTimeslotDurationHours = parseNonNegativeInt(core.getInput('lre_timeslot_duration_hours'), 0);
  let lreTimeslotDurationMinutes = parseNonNegativeInt(core.getInput('lre_timeslot_duration_minutes'), 30);
  if (lreTimeslotDurationHours < 1 && lreTimeslotDurationMinutes < 30) {
    lreTimeslotDurationMinutes = 30;
  }

  const lrePostRunAction = normalize(core.getInput('lre_post_run_action')) || 'Do Not Collate';
  const lreVudsMode = resolveBooleanInput(core.getInput('lre_vuds_mode'), false);
  const lreTrendReport = normalize(core.getInput('lre_trend_report'));
  const lreProxyOutUrl = normalize(core.getInput('lre_proxy_out_url'));
  const lreUsernameProxy = getEnvOrEmpty('lre_username_proxy');
  const lrePasswordProxy = getEnvOrEmpty('lre_password_proxy');
  const lreSearchTimeslot = resolveBooleanInput(core.getInput('lre_search_timeslot'), false);
  const lreStatusBySla = resolveBooleanInput(core.getInput('lre_status_by_sla'), false);
  const lreRuntimeOnly = resolveBooleanInput(core.getInput('lre_runtime_only'), true);
  const parsedLreWorkspaceSyncSuccessThreshold = parseNonNegativeInt(core.getInput('lre_workspace_sync_success_threshold'), 50);
  const lreWorkspaceSyncSuccessThreshold = parsedLreWorkspaceSyncSuccessThreshold <= 100 ? parsedLreWorkspaceSyncSuccessThreshold : 50;
  const lreEnableStacktrace = resolveBooleanInput(core.getInput('lre_enable_stacktrace'), false);

  const { lreOutputDir, lreWorkspaceDir } = resolveWorkspaceAndOutputDirs(
      core.getInput('lre_output_dir'),
      core.getInput('lre_workspace_dir')
  );

  return {
    lre_action: lreAction,
    lre_description: lreDescription,
    lre_server: lreServer,
    lre_https_protocol: lreHttpsProtocol,
    lre_authenticate_with_token: lreAuthenticateWithToken,
    lre_domain: lreDomain,
    lre_project: lreProject,
    lre_test: lreTest,
    lre_test_instance: lreTestInstance,
    lre_timeslot_duration_hours: lreTimeslotDurationHours,
    lre_timeslot_duration_minutes: lreTimeslotDurationMinutes,
    lre_post_run_action: lrePostRunAction,
    lre_vuds_mode: lreVudsMode,
    lre_trend_report: lreTrendReport,
    lre_proxy_out_url: lreProxyOutUrl,
    lre_search_timeslot: lreSearchTimeslot,
    lre_status_by_sla: lreStatusBySla,
    lre_output_dir: lreOutputDir,
    lre_workspace_dir: lreWorkspaceDir,
    lre_runtime_only: lreRuntimeOnly,
    lre_workspace_sync_success_threshold: lreWorkspaceSyncSuccessThreshold,
    lre_enable_stacktrace: lreEnableStacktrace
  };
}

function runJavaProcess(jarFilePath, configFilePath) {
  return new Promise((resolve, reject) => {
    const javaAppArgs = [
      '-Djava.util.logging.manager=org.apache.logging.log4j.jul.LogManager',
      `-Dlog4j.configurationFile=jar:file:///${jarFilePath.replace(/\\/g, '/')}!/log4j2.xml`,
      '-jar',
      jarFilePath,
      configFilePath
    ];

    const javaProcess = spawn('java', javaAppArgs, { cwd: __dirname });
    let parseState = { lreRunId: null, parseTail: '' };

    javaProcess.on('error', (err) => reject(new Error(`Failed to start Java process: ${err.message}`)));

    javaProcess.stdout.on('data', (data) => {
      const output = data.toString();
      console.log(output);
      parseState = updateRunIdParseState(parseState, output);
    });

    javaProcess.stderr.on('data', (data) => {
      const output = data.toString();
      console.error(output);
      parseState = updateRunIdParseState(parseState, output);
    });

    javaProcess.on('close', (code) => {
      if (code !== 0) {
        reject(new Error(`process exited with code ${code}`));
        return;
      }

      resolve(parseState.lreRunId);
    });
  });
}

async function run() {
  try {
    const config = enrichWorkspaceSyncConfig(buildConfig());

    // Write the configuration to a file
    const configFilePath = path.join(process.cwd(), 'config.json');
    await fs.writeFile(configFilePath, JSON.stringify(config, null, 2));

    // Path to the JAR file
    const jarFilePath = path.resolve(__dirname, 'lre-actions-1.2-SNAPSHOT-jar-with-dependencies.jar');

    const lreRunId = await runJavaProcess(jarFilePath, configFilePath);
    console.log('process completed successfully.');
    if (lreRunId) {
      core.setOutput('lre_run_id', lreRunId);
    } else if (config.lre_action === ACTION_EXECUTE_TEST) {
      core.warning('lre_run_id was not found in the process output.');
    }
  } catch (error) {
    core.setFailed(error.message);
  }
}

if (require.main === module) {
  run();
}

module.exports = {
  extractRunId,
  updateRunIdParseState,
  resolveWorkspaceSyncDiffRange,
  selectChangedFilesUnderWorkspace
};
