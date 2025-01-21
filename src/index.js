const core = require('@actions/core');
const { spawn } = require('child_process');
const fs = require('fs').promises;
const path = require('path');

async function run() {
  try {
    // Retrieve inputs from the `with:` block
    let lreAction = core.getInput('lre_action');
    let lreDescription = core.getInput('lre_description');
    let lreServer = core.getInput('lre_server');
    let lreHttpsProtocol = core.getInput('lre_https_protocol');
    let lreAuthenticateWithToken = core.getInput('lre_authenticate_with_token');
    let lreUsername = process.env.lre_username;
    let lrePassword = process.env.lre_password;
    let lreDomain = core.getInput('lre_domain');
    let lreProject = core.getInput('lre_project');
    let lreTest = core.getInput('lre_test');
    let lreTestInstance = core.getInput('lre_test_instance');
    let lreTimeslotDurationHours = parseInt(core.getInput('lre_timeslot_duration_hours'));
    let lreTimeslotDurationMinutes = parseInt(core.getInput('lre_timeslot_duration_minutes'));
    let lrePostRunAction = core.getInput('lre_post_run_action');
    let lreVudsMode = core.getInput('lre_vuds_mode');
    let lreTrendReport = core.getInput('lre_trend_report');
    let lreProxyOutUrl = core.getInput('lre_proxy_out_url');
    let lreUsernameProxy = process.env.lre_username_proxy;
    let lrePasswordProxy = process.env.lre_password_proxy;
    let lreSearchTimeslot = core.getInput('lre_search_timeslot');
    let lreStatusBySla = core.getInput('lre_status_by_sla');
    let lreOutputDir = core.getInput('lre_output_dir');
    let lreEnableStacktrace = core.getInput('lre_enable_stacktrace');

    // Validate 'lre_action' parameter
    if (!lreAction) {
      lreAction = 'ExecuteLreTest';
    }
    
    // Validate 'lre_description' parameter
    if (!lreDescription) {
      lreDescription = 'Executing LRE test';
    }
    
    // Validate 'lre_server' parameter
    if (!lreServer) {
      core.setFailed('Input parameter "lre_server" is required.');
      process.exit(1); // Terminate the action with a failure
    }

    // Validate 'lre_https_protocol' parameter
    if (lreHttpsProtocol !== 'true' && lreHttpsProtocol !== 'false') {
      lreHttpsProtocol = false;
      }
    
    // Validate 'lre_authenticate_with_token' parameter
    if (lreAuthenticateWithToken !== 'true' && lreAuthenticateWithToken !== 'false') {
      lreAuthenticateWithToken = false;
    }

    // Validate 'lre_username' parameter
    if (!lreUsername) {
      core.setFailed('Input parameter "lre_username" is required.');
      process.exit(1); // Terminate the action with a failure
    }
    
    // Validate 'lre_password' parameter
    if (!lrePassword) {
      core.setFailed('Input parameter "lre_password" is required.');
      process.exit(1); // Terminate the action with a failure
    }
    
    // Validate 'lre_domain' parameter
    if (!lreDomain) {
      core.setFailed('Input parameter "lre_domain" is required.');
      process.exit(1); // Terminate the action with a failure
    }
    
    // Validate 'lre_project' parameter
    if (!lreProject) {
      core.setFailed('Input parameter "lre_project" is required.');
      process.exit(1); // Terminate the action with a failure
    }
    
    // Validate 'lre_test' parameter
    if (!lreTest) {
      core.setFailed('Input parameter "lre_test" is required.');
      process.exit(1); // Terminate the action with a failure
    }
    
    // Validate 'lre_test_instance' parameter
    if (!lreTestInstance || lreTestInstance <= 0) {
      lreTestInstance = 'AUTO';
    }
    
    // Validate 'lre_timeslot_duration_hours' parameter
    if (!lreTimeslotDurationHours) {
      lreTimeslotDurationHours = 0;
    }
    
    // Validate 'lre_timeslot_duration_minutes' parameter
    if (!lreTimeslotDurationMinutes || lreTimeslotDurationMinutes < 30) {
      if(lreTimeslotDurationHours < 1) {
        lreTimeslotDurationMinutes = 30;
      } else {
        lreTimeslotDurationMinutes = lreTimeslotDurationMinutes;
      }
    }
    
    // Validate 'lre_post_run_action' parameter
    if (!lrePostRunAction) {
      lrePostRunAction = 'Do Not Collate';
    }
    
    // Validate 'lre_vuds_mode' parameter
    if (lreVudsMode !== 'true' && lreVudsMode !== 'false') {
      lreVudsMode = false;
    }
    
    // Validate 'lre_trend_report' parameter
    if (!lreTrendReport) {
      lreTrendReport = '';
    }
    
    // Validate 'lre_proxy_out_url' parameter
    if (!lreProxyOutUrl) {
      lreProxyOutUrl = '';
    }
    
    // Validate 'lre_username_proxy' parameter
    if (!lreUsernameProxy) {
      lreUsernameProxy ='';
    }
    
    // Validate 'lre_password_proxy' parameter
    if (!lrePasswordProxy) {
      lrePasswordProxy = '';
    }
    
    // Validate 'lre_search_timeslot' parameter
    if (lreSearchTimeslot !== 'true' && lreSearchTimeslot !== 'false') {
      lreSearchTimeslot = false;
    }
    
    // Validate 'lre_status_by_sla' parameter
    if (lreStatusBySla !== 'true' && lreStatusBySla !== 'false') {
      lreStatusBySla = false;
    }
    
    // Validate 'lre_output_dir' parameter
    if (!lreOutputDir) {
      lreOutputDir = './';
    }
    
    // Validate 'lre_enable_stacktrace' parameter
    if (lreEnableStacktrace !== 'true' && lreEnableStacktrace !== 'false') {
      lreEnableStacktrace = false;
    }
    
    // Define configuration object
    const config = {
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
      lre_enable_stacktrace: lreEnableStacktrace
    };
    
    // Write the configuration to a file
    const configFilePath = path.join(process.cwd(), 'config.json');
    await fs.writeFile(configFilePath, JSON.stringify(config, null, 2));

    // Path to the JAR file
    const jarFilePath = path.resolve(__dirname, 'lre-actions-1.1-SNAPSHOT-jar-with-dependencies.jar');
    
    // Java application command and parameters
    const javaAppCommand = 'java';
    const javaAppArgs = ['-cp', jarFilePath, 'com.opentext.lre.actions.Main', configFilePath]; // Add any additional arguments here
    
    // Set the working directory explicitly
    const workingDirectory = __dirname; // Or specify the desired working directory
    
    // Spawn the Java process
    const javaProcess = spawn(javaAppCommand, javaAppArgs, { cwd: workingDirectory });
    javaProcess.on('error', (err) => {
      console.error('Failed to start Java process:', err);
    });
    let lreRunId = null;
    
    // Handle data from Java process
    javaProcess.stdout.on('data', (data) => {
      console.log(`${data}`);
      const match = data.toString().match(/lre_run_id=(\S+)/);
      if (match && match[1]) {
          lreRunId = match[1]; // Save the extracted lre_run_id
      }
    });
    
    // Handle errors from Java process
    javaProcess.stderr.on('data', (data) => {
      console.error(`${data}`);
    });
    
    // Handle Java process exit
    javaProcess.on('close', (code) => {
      if (code !== 0) {
        core.setFailed(`process exited with code ${code}`);
      } else {
        console.log('process completed successfully.');
        if (lreRunId) {
          core.setOutput('lre_run_id', lreRunId);
        } else {
          core.warning('lre_run_id was not found in the process output.');
        }
      }
    });
  } catch (error) {
    core.setFailed(error.message);
  }
}

run();
