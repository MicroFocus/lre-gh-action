const core = require('@actions/core');
const { spawn } = require('child_process');
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
    let lreRunId = null;

    javaProcess.on('error', (err) => reject(new Error(`Failed to start Java process: ${err.message}`)));

    javaProcess.stdout.on('data', (data) => {
      const output = data.toString();
      console.log(output);
      const match = output.match(/lre_run_id=(\S+)/);
      if (match && match[1]) {
        lreRunId = match[1];
      }
    });

    javaProcess.stderr.on('data', (data) => {
      console.error(data.toString());
    });

    javaProcess.on('close', (code) => {
      if (code !== 0) {
        reject(new Error(`process exited with code ${code}`));
        return;
      }

      resolve(lreRunId);
    });
  });
}

async function run() {
  try {
    const config = buildConfig();

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

run();
