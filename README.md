![OpenText Logo](https://upload.wikimedia.org/wikipedia/commons/1/1b/OpenText_logo.svg)

# OpenText Enterprise Performance Engineering GitHub Action

## Overview

This repository is used to build and maintain OpenText Enterprise Performance Engineering GitHub Action.

## What This GitHub Action Does

### **ExecuteLreTest** action - Execute load tests on OpenText Enterprise Performance Engineering server. At runtime, the action:

1. Authenticates with an OpenText Enterprise Performance Engineering server
2. Triggers a performance test execution
3. Allocates or reuses a timeslot (based on configuration)
4. Monitors test execution until completion
5. Optionally collates and analyzes results
6. Writes logs and result artifacts to configured directories
7. Exits with a deterministic status code suitable for pipeline gating

### **WorkspaceSync** action (Technology Preview) - Synchronize local workspace scripts to OpenText Enterprise Performance Engineering server. At runtime, the action:

1. Authenticates with an OpenText Enterprise Performance Engineering server
2. Scans the workspace for folders considered as supported scripts. Folders identified as scripts are:
    * VuGen scripts: folders containing a .usr file
    * JMeter scripts: folders containing a .jmx file
    * Gatling scripts: folders containing a .scala file
    * Selenium or unit test scripts: folders containing a .java file
    * DevWeb scripts: folders containing both main.js AND rts.yml files
3. Zips each folder identified as script and uploads it to OpenText Enterprise Performance Engineering project
4. Failure handling:
    * If 5 consecutive script uploads fail, the action is interrupted with failure
    * If at least 50% of the scripts found in the workspace are uploaded successfully, the action reports success
    * Otherwise, the action reports failure
5. Writes logs for each upload in console and in workspace
6. Exits with a deterministic status code suitable for pipeline gating

## Requirements

1. Any workflow including this action will need to have preliminary steps such as (see example below):
 - actions/checkout@v6 to checkout git repository into GitHub job workspace.
 - actions/setup-java@v5 (version 17) to setup environment with java 17.
 - actions/setup-node@v6 (version 25 for example or latest) to setup environment with nodejs .
2. Usernames and passwords will need to be saved as secrets and used as environment variables in the workflow.
3. If you want to save reports as build artifacts, you can add the following step: actions/upload-artifact@v7 (see example below).
4. Writable workspace and output directories.
5. Network access to the OpenText Enterprise Performance Engineering server
6. Valid OpenText Enterprise Performance Engineering credentials  (username/password or token).

## Action Inputs

The action supports two operation modes:
- `ExecuteLreTest`: trigger and monitor a performance test run.
- `WorkspaceSync`: scan a local workspace and upload detected scripts to OpenText Enterprise Performance Engineering.

### Required Parameters

| Input | Description | Action |
|---|---|---|
| **lre_server** | Server, port (when not mentioned, default is 80 or 443 for secure), and tenant (when not mentioned, default is `?tenant=fa128c06-5436-413d-9cfa-9f04bb738df3`). Example: `mylreserver.mydomain.com:81/?tenant=fa128c06-5436-413d-9cfa-9f04bb738df3` | Both |
| **lre_username** | Username (or ID part of token)  | Both |
| **lre_password** | Password (or secret part of token) | Both |
| **lre_domain** | Domain (case sensitive) | Both |
| **lre_project** | Project (case sensitive) | Both |
| **lre_test** | Required when `lre_action=ExecuteLreTest`. Valid test ID# or relative path to a YAML file in the workspace that defines new test design creation | ExecuteLreTest |


\* Either username/password **or** token authentication must be used.


### Optional Parameters

| Input | Description | Action | Default |
|---|---|---|---|
| **lre_action** | Action to be triggered. Supported values: ExecuteLreTest, WorkspaceSync. If not defined, ExecuteLreTest is used. | Both | ExecuteLreTest |
| **lre_description** | Description of the action (displayed in logs) | Both | |
| **lre_https_protocol** | Use secured protocol for connecting to the server (`true` / `false`) | Both | true |
| **lre_authenticate_with_token** | Use token authentication (required for SSO). (`true` / `false`) | Both | false |
| **lre_workspace_dir** | Local workspace root path. For WorkspaceSync this is the directory scanned for scripts and where logs are written to `<workspace>/logs` | Both | If omitted, resolved with `lre_output_dir`; if both are omitted, `./` |
| **lre_test_instance** | `AUTO` or specific instance ID | ExecuteLreTest | AUTO |
| **lre_timeslot_duration_hours** | Timeslot duration (hours) | ExecuteLreTest | 0 |
| **lre_timeslot_duration_minutes** | Timeslot duration (minutes) | ExecuteLreTest | 30 |
| **lre_post_run_action** | `Collate Results`, `Collate and Analyze`, `Do Not Collate` | ExecuteLreTest | Do Not Collate |
| **lre_vuds_mode** | Use VUDS licenses (`true` / `false`) | ExecuteLreTest | false |
| **lre_trend_report** | `ASSOCIATED` - the trend report defined in the test design will be used', Valid report ID - Report ID will be used for trend, No value or not defined - no trend monitoring. | ExecuteLreTest | |
| **lre_proxy_out_url** | Proxy URL | Both | |
| **lre_username_proxy** | Proxy username | Both | |
| **lre_password_proxy** | Proxy password | Both | |
| **lre_search_timeslot** | Experimental: Search for matching timeslot instead of creating a new timeslot (`true` / `false`) | ExecuteLreTest | false |
| **lre_status_by_sla** | Report success based on SLA (`true` / `false`) | ExecuteLreTest | false |
| **lre_output_dir** | Directory to read the checkout folder and to save results (use `${{ github.workspace }}`) | ExecuteLreTest | ./ |
| **lre_runtime_only** | Scripts upload mode (Runtime files only for true, All files for false) (`true` / `false`) | WorkspaceSync | true |
| **lre_enable_stacktrace** | Print stacktrace on errors (`true` / `false`) | Both | false |

### WorkspaceSync behavior

When `lre_action` is set to `WorkspaceSync`, the action scans `lre_workspace_dir` recursively and uploads script folders found in the workspace hierarchy. A folder is treated as a script folder when it contains one of the supported script formats (for example `.usr`, `.jmx`, `.java`) or DevWeb script markers (`main.js` and `rts.yml`).

Each detected script folder is zipped and uploaded to the matching subject path in OpenText Enterprise Performance Engineering, preserving the relative folder structure under `Subject`.

The sync result is considered successful when at least 50% of script uploads succeed. The process also stops early after 5 consecutive upload failures.

Directory resolution note: if `lre_output_dir` and `lre_workspace_dir` are both omitted, both resolve to `./`.


### Outputs

| Path | Purpose |
|-----|---------|
| **lre_output_dir** | Result files and summarized outputs |
| **lre_workspace_dir** | Workspace for logs, reports, and checkout |

These directories **must be writable**.

---

## Examples

### Authenticate using a `:token` (access key), create a performance test from `YamlTest/createTestFromYaml.yaml` in the GitHub repository, execute it, wait for analysis and trending to complete, download reports, and upload them as build artifacts

```yml
name: Create performance test according to YamlTest/createTestFromYaml.yaml and execute it

on:
  push:
    branches:
      - main

jobs:
  test:
    runs-on: ubuntu-latest
    name: Start a performance test
    env:
      lre_username: ${{ secrets.LRE_USERNAME }}
      lre_password: ${{ secrets.LRE_PASSWORD }}
    steps:
      - name: Checkout code
        uses: actions/checkout@v6

      - name: Setup Java
        uses: actions/setup-java@v5
        with:
          distribution: adopt
          java-version: '17'

      - name: Set up Node.js
        uses: actions/setup-node@v6
        with:
          node-version: '25'

      - name: Use GitHub Action
        uses: MicroFocus/lre-gh-action@v1.0.4
        with:
          lre_action: ExecuteLreTest
          lre_description: running new yaml test
          lre_server: mylreserver.mydomain.com/?tenant=fa128c06-5436-413d-9cfa-9f04bb738df3
          lre_https_protocol: true
          lre_authenticate_with_token: true
          lre_domain: DANIEL
          lre_project: proj1
          lre_test: YamlTest/createTestFromYaml.yaml
          lre_test_instance: AUTO
          lre_timeslot_duration_hours: 0
          lre_timeslot_duration_minutes: 30
          lre_post_run_action: Collate and Analyze
          lre_vuds_mode: false
          lre_trend_report: ASSOCIATED
          lre_status_by_sla: false
          lre_output_dir: ${{ github.workspace }}

      - name: Upload build artifacts
        uses: actions/upload-artifact@v7
        with:
          name: build-artifacts
          path: ${{ github.workspace }}/LreResult
```

Content of `YamlTest/createTestFromYaml.yaml` from the example (when using `script_path`, the script with the mentioned path must exist under the `Subject` root of Test Plan. Otherwise, you can use `script_id` to refer to the script by its ID):
```yml
##################################################
group:
  - group_name: "demo_script_new"
    vusers: '2'
    #script_id: 175
    script_path: "daniel\\scripts\\demo_script_new"
    lg_name:
      - "LG1"
scheduler:
  rampup: '10'
  duration: '60'
automatic_trending:
  report_id: 6
  max_runs_in_report: 3
##################################################
```

### Authenticate using username and password (`lre_authenticate_with_token: false`), execute a performance test, wait for analysis and trending to complete, download reports, and upload them as build artifacts.

```yml
name: test

on:
  push:
    branches:
      - main

jobs:
  test:
    runs-on: ubuntu-latest
    name: Execute test
    env:
      lre_username: ${{ secrets.LRE_USERNAME }}
      lre_password: ${{ secrets.LRE_PASSWORD }}
    steps:
      - name: Checkout code
        uses: actions/checkout@v6

      - name: Setup Java
        uses: actions/setup-java@v5
        with:
          distribution: adopt
          java-version: '17'

      - name: Set up Node.js
        uses: actions/setup-node@v6
        with:
          node-version: '25'

      - name: Use My GitHub Action
        uses: MicroFocus/lre-gh-action@v1.0.4
        with:
          lre_action: ExecuteLreTest
          lre_description: running new yaml test
          lre_server: myserver.mydomain.com/?tenant=fa128c06-5436-413d-9cfa-9f04bb738df3
          lre_https_protocol: true
          lre_authenticate_with_token: false
          lre_domain: DANIEL
          lre_project: proj1
          lre_test: 176
          lre_test_instance: AUTO
          lre_timeslot_duration_hours: 0
          lre_timeslot_duration_minutes: 30
          lre_post_run_action: Collate and Analyze
          lre_vuds_mode: false
          lre_trend_report: ASSOCIATED
          lre_status_by_sla: false
          lre_output_dir: ${{ github.workspace }}

      - name: Upload build artifacts
        uses: actions/upload-artifact@v4
        with:
          name: build-artifacts
          path: ${{ github.workspace }}/LreResult
```

### Synchronize scripts from repository workspace (located in 'scripts' folder) to OpenText Enterprise Performance Engineering (WorkspaceSync)

```yml
name: Synchronize scripts to OpenText Enterprise Performance Engineering

on:
  workflow_dispatch:

jobs:
  workspace-sync:
    runs-on: ubuntu-latest
    env:
      lre_username: ${{ secrets.LRE_USERNAME }}
      lre_password: ${{ secrets.LRE_PASSWORD }}
    steps:
      - name: Checkout code
        uses: actions/checkout@v6

      - name: Setup Java
        uses: actions/setup-java@v5
        with:
          distribution: adopt
          java-version: '17'

      - name: Set up Node.js
        uses: actions/setup-node@v6
        with:
          node-version: '25'

      - name: Synchronize scripts
        uses: MicroFocus/lre-gh-action@v1.0.4
        with:
          lre_action: WorkspaceSync
          lre_description: synchronize scripts from workspace
          lre_server: myserver.mydomain.com/?tenant=fa128c06-5436-413d-9cfa-9f04bb738df3
          lre_https_protocol: true
          lre_authenticate_with_token: false
          lre_domain: DANIEL
          lre_project: proj1
          lre_workspace_dir: ${{ github.workspace }}/scripts
          lre_runtime_only: true
          lre_enable_stacktrace: true
      - name: Upload build artifacts
        uses: actions/upload-artifact@v7
        with:
          name: build-artifacts
          path: '${{ github.workspace }}/scripts/logs'
```

How to import tests from YAML files saved in the Git repository
===============================================================

Copy your YAML files to a folder in your Git repository (YAML files under the root of the Git repository will be ignored). The plugin will create the test in the project according to:

-   The file name (without extension) which will be used as test name.
-   The location of the file in the Git repository which will be the location of the test under the root folder ('Subject') in the **Test Management** tree.
-   The content of the YAML file which must be composed according to the parameters described in the tables below.

**Note:**

-   All parameters must be in lowercase.
-   When a backslash (\) occurs in a value provided to a parameter (for example, a folder separator in a file path), a double backslash (\\) must be used instead.
-   This feature is supported with Performance Center 12.61 and later versions.

* * * * *

Root parameters of the YAML file:

| Parameter | Description | Required |
|-----------|-------------|----------|
| controller | Defines the Controller to be used during the test run (it must be an available host in the project). If not specified, a Controller will be chosen from the different controllers available in the project. | No |
| lg_amount | Number of load generators to allocate to the test (every group in the test will be run by the same load generators). | Not required if each group defined in the 'group' parameter defines the load generators it will be using via the 'lg_name' parameter (see 'group' table below). |
| group | Lists all groups or scripts defined in the test. The parameters used in each group are specified in the 'group' table below. | Yes |
| scheduler | Defines the duration of a test, and determines whether virtual users are started simultaneously or gradually. See the 'scheduler' table below. | No |
| lg_elastic_configuration | Defines the image to be used in order to provision load generators. See the 'lg_elastic_configuration' table below. Available from Performance Center 12.62 and plugin version 1.1.1. | Yes, if a load generator is defined to be provisioned from a Docker image. |
| automatic_trending | Defines association to existing trend report. | No |

* * * * *

**group:**

| Parameter | Description | Required |
|-----------|-------------|----------|
| group_name | Name of the group (it must be a unique name if several groups are defined). | Yes |
| vusers | Number of virtual users to allocate to the group for running the script. | Yes |
| script_id | ID of the script in the project. | Not required if the 'script_path' parameter is specified. |
| script_path | Path and name of the script to be added to the group, separated by double backslashes (\\). For example "MyMainFolder\\MySubFolder\\MyScriptName'. Do not include the root folder (named "Subject"). | Not required if 'script_id' parameter is specified |
| lg_name | List of load generators to allocate to the group for running the script. The supported values are: <br> -   The hostname of an existing load generator allocated as a host. <br> -   **"LG"** followed by a number, to use an automatically matched load generator (recommended). <br> -   **"DOCKER"** followed by a number, to use a dynamic load generator (available from Performance Center 12.62, if your project is set to work with Docker). This option requires the 'lg_elastic_configuration' parameter to be defined (see the 'lg_elastic_configuration' table below). | No |
| command_line | The command line applied to the group. | No |
| rts | Object defining the runtime settings of the script. See the 'rts' table below. | No |

* * * * *

**rts:**

| Parameter | Description | Required |
|-----------|-------------|----------|
| pacing | Can be used to define the number of iterations the script will run and the required delay between iterations (see the 'pacing' table below). | No |
| thinktime | Can be used to define think time (see the 'thinktime' table below). | No |
| java_vm | Can be used when defining Java environment runtime settings (see the 'java_vm' table below). | No |
| jmeter | Can be used to define JMeter environment runtime settings (see the 'jmeter' table below). | No |

* * * * *

**pacing:**

| Parameter | Description | Required |
|-----------|-------------|----------|
| number_of_iterations | Specifies the number of iterations to run; this must be a positive number. | Yes |
| type | Possible values for type attribute are: <br> -   **"immediately"**: ignores 'delay' and 'delay_random_range' parameters. This is the default value when no type is specified. <br> -   **"fixed delay"**: 'delay' parameter is mandatory. <br> -   **"random delay"**: 'delay' and 'delay_random_range' parameters are mandatory. <br> -   **"fixed interval"**: 'delay' parameter is mandatory. <br> -   **"random interval"**: 'delay' and 'delay_random_range' parameters are mandatory. | No |
| delay | Non-negative number (less than 'delay_at_range_to_seconds' when specified). | Depends on the value provided for the 'type' parameter. |
| delay_random_range | Non-negative number. It will be added to the value given to the 'delay' parameter (the value will be randomly chosen between the value given to 'delay' parameter and the same value to which is added the value of this parameter). | Depends on the value provided for the 'type' parameter. |

* * * * *

**thinktime:**

| Parameter | Description | Required |
|-----------|-------------|----------|
| type | The ThinkTime Type attribute is one of: <br> -   **"ignore"**: This is the default value when no type is specified. <br> -   **"replay"**: Provide 'limit_seconds' parameter with value. <br> -   **"modify"**: Provide 'limit_seconds' and 'multiply_factor' parameters with values. <br> -   **"random"**: Provide 'limit_seconds', 'min_percentage' and 'max_percentage' parameters with values. | No |
| min_percentage | This must be a positive number. | Depends on the value provided for the 'type' parameter. |
| max_percentage | This must be a positive number (it must be larger than the value provided for the 'min_percentage' parameter). | Depends on the value provided for the 'type' parameter. |
| limit_seconds | This must be a positive number. | Depends on the value provided for the 'type' parameter. |
| multiply_factor | The recorded think time is multiplied by this factor at runtime. | Depends on the value provided for the 'type' parameter. |

* * * * *

**java_vm:**

| Parameter | Description | Required |
|-----------|-------------|----------|
| jdk_home | The JDK installation path. | No |
| java_vm_parameters | List the Java command line parameters. These parameters can be any JVM argument. Common arguments are the debug flag (-verbose) or memory settings (-ms, -mx). In addition, you can pass properties to Java applications in the form of a -D flag. | No |
| use_xboot | Boolean: Instructs VuGen to add the Classpath before the Xbootclasspath (prepend the string). | No |
| enable_classloader_per_vuser | Boolean: Loads each Virtual User using a dedicated class loader (runs Vusers as threads). | No |
| java_env_class_paths | A list of classpath entries. Use a double backslash (\\) for folder separators. | No |

* * * * *

**jmeter:**

| Parameter | Description | Required |
|-----------|-------------|----------|
| start_measurements | Boolean value to enable JMX measurements during performance test execution. | No |
| jmeter_home_path | Path to JMeter home. If not defined, the path from the %JMETER_HOME% environment variable is used. | No |
| jmeter_min_port | This number must be lower than the value provided in the 'jmeter_max_port' parameter. Both 'jmeter_min_port' and 'jmeter_max_port' parameters must be specified otherwise the default port values is used. | No |
| jmeter_max_port | This number must be higher than the value provided in the 'jmeter_min_port' parameter. Both 'jmeter_min_port' and 'jmeter_max_port' parameters must be specified otherwise the default port values is used. | No |
| jmeter_additional_properties | JMeter additional properties file. Use double backslashes (\\) for folder separators. | No |

* * * * *

**scheduler:**

| Parameter | Description | Required |
|-----------|-------------|----------|
| rampup | Time, in seconds, to gradually start all virtual users. Additional virtual users are added every 15 seconds until the time specified in the parameter ends. If no value is specified, all virtual users are started simultaneously at the beginning of the test. | No |
| duration | Time, in seconds, that it will take to run the test after all virtual users are started. After this time, the test run ends. If not specified, the test will run until completion. | No |

* * * * *

**automatic_trending:**

| Parameter | Description | Required |
|-----------|-------------|----------|
| report_id | ID of the trend report to associate the test run analysis with. | No |
| max_runs_in_report | Maximum trends in a report (default is 10 if not specified). | No |

* * * * *

**lg_elastic_configuration:** (available from Performance Center 12.62 and plugin version 1.1.1)

| Parameter | Description | Required |
|-----------|-------------|----------|
| image_id | This number can be retrieved from: <br> -   The Administration page (you might need to turn to your administrator, as accessing this page requires admin privileges): select the **Orchestration** section -> switch to **Docker Images** tab -> you will have the list of all available Docker images for Load Generator purposes with their ID. You can make sure the images are available to your project from the **Orchestrators** tab. <br> -   An OpenText Enterprise Performance Engineering REST API command applied on the project (replace the bracketed values): GET - [http(s)://(PCServer):(PortNumber)/LoadTest/rest/domains/(DomainName)/projects/(ProjectName)/dockerimages/](file:///C:/GIT/plugins/micro-focus-performance-center-integration-plugin/src/main/resources/com/microfocus/performancecenter/integration/pcgitsync/PcGitSyncBuilder/help-importTests.html#) and select any valid image not having the value 'controller' for purpose. | Yes, if one of the load generators is defined to be provisioned from a Docker image. |
| memory_limit | This parameter can be retrieved from **Application** -> **Test Management** -> edit a test -> Press **Assign LG** button -> in the **Elastic** section, select **DOCKER1** -> select the relevant image (based on the image name) -> use the values provided in the 'Memory(GB)' dropdown list (if not specified, this parameter should be ignored). | Yes, if the image is defined with resource limits |
| cpu_limit | This parameter can be retrieved from **Application** -> **Test Management** -> edit a test -> Press **Assign LG** button -> in the **Elastic** section, select **DOCKER1** -> select the relevant image (based on the image name) -> use the values provided in the 'CPUs' dropdown list (if not specified, this parameter should be ignored). | Yes, if the image is defined with resource limits |

* * * * *

In the example below:

-   The plugin automatically assigns the file name as the test name, and the folder path of the file in the Git repository is used to create the location of the test under the root folder ('Subject') in the project.
-   In the content:
    -   Since no Controller and no load generator amount were specified, a random available Controller will be allocated to the test just before it is run and the 'lg_name' parameter specified in each group will be used.
    -   In the 'group' parameter:
        -   We added two scripts. For each, we provided a unique value in the 'group_name' parameter, and the number of virtual users to run the group.
        -   Since we did not know the ID of the scripts, we used the 'script_path' parameter in which we entered the script path (without "Subject") followed by the script name, and used double backslashes for separators.
        -   We specified the load generators that will be used by each group (in this case, load generators will automatically be matched as we use the 'LG' prefix).
-   In the scheduler:
    -   We want all Virtual Users to be initialized gradually (45 seconds).
    -   We want the test to stop after 5 minutes (300 seconds).

```

##################################################
group:
- group_name: "TEstInt"
  vusers: '20'
  script_path: "plugin\\TEstInt"
  lg_name:
  - "LG1"
  - "LG2"
- group_name: "Mtours"
  vusers: '20'
  script_path: "plugin\\mtours"
  lg_name:
  - "LG3"
  - "LG4"
scheduler:
  rampup: '45'
  Duration: '300'
##################################################

```

* * * * *

In the example below:

-   The plugin automatically assigns the file name as the test name, and the folder path of the file in the Git repository is used to create the location of the test under the root folder ('Subject') in the project.
-   Since the 'controller' and the 'lg_amount' parameters are specified, the specified Controller will be used to run the test and three automatch load generators will be used and shared by all groups.
-   The content of the file is defined with seven groups, all being set with the "rts" parameter:
    -   The "pacing" parameter is used with different options for all groups.
    -   The "java_vm" parameter is used for five scripts with JavaVM for protocol.
    -   The "thinktime" parameter is used with different options for four groups.
    -   The "jmeter" parameter is used for two scripts with JMeter for protocol.
-   In the scheduler:
    -   We want all Virtual Users to be initialized gradually (120 seconds).
    -   We want the test to stop after 10 minutes (600 seconds).

```

##################################################
controller: "mycontroller"
lg_amount: 3
group:
  - group_name: "JavaVuser_LR_Information_pacing_immediately_thinktime_ignore"
    vusers: 50
    script_id: 394
    rts:
      pacing:
        number_of_iterations: 2
        type: "immediately"
      java_vm:
        jdk_home: "C:\\Program Files\\Java\\jdk1.8.0_191"
        java_vm_parameters: "java_vm_parameters"
        enable_classloader_per_vuser: true
        use_xboot: true
        java_env_class_paths:
          - "java_env_class_path1"
          - "java_env_class_path2"
      thinktime:
        type: "ignore"

  - group_name: "JavaHTTP_BigXML_pacing_fixed_delay_thinktime_replay"
    vusers: 50
    script_path: "scripts\\java_protocols\\JavaHTTP_BigXML"
    rts:
      pacing:
        number_of_iterations: 2
        type: "fixed delay"
        delay: 10
      java_vm:
        jdk_home: "C:\\Program Files\\Java\\jdk1.8.0_191"
        java_vm_parameters: "java_vm_parameters"
        enable_classloader_per_vuser: true
      thinktime:
        type: "replay"
        limit_seconds: 30

  - group_name: "JavaVuser_LR_Information_immediately_pacing_random_delay_thinktime_modify"
    vusers: 50
    script_id: 394
    rts:
      pacing:
        number_of_iterations: 2
        type: "random delay"
        delay: 10
        delay_random_range: 20
      java_vm:
        jdk_home: "C:\\Program Files\\Java\\jdk1.8.0_191"
        java_vm_parameters: "java_vm_parameters"
        enable_classloader_per_vuser: true
        java_env_class_paths:
          - "java_env_class_path1"
          - "java_env_class_path2"
      thinktime:
        type: "modify"
        limit_seconds: 30
        multiply_factor: 2

  - group_name: "JavaHTTP_BigXML_pacing_fixed_interval_thinktime_random"
    vusers: 50
    #script_id: 392
    script_path: "scripts\\java_protocols\\JavaHTTP_BigXML"
    rts:
      pacing:
        number_of_iterations: 2
        type: "fixed interval"
        delay: 10
      java_vm:
        jdk_home: "C:\\Program Files\\Java\\jdk1.8.0_191"
        java_vm_parameters: "java_vm_parameters"
        enable_classloader_per_vuser: true
        java_env_class_paths:
          - "java_env_class_path1"
          - "java_env_class_path2"
      thinktime:
        type: "random"
        limit_seconds: 30
        min_percentage: 2
        max_percentage: 3

  - group_name: "JavaHTTP_BigXML_pacing_random_interval"
    vusers: 50
    script_path: "scripts\\java_protocols\\JavaHTTP_BigXML"
    rts:
      pacing:
        number_of_iterations: 2
        type: "random interval"
        delay: 10
        delay_random_range: 20
      java_vm:
        jdk_home: "C:\\Program Files\\Java\\jdk1.8.0_191"
        java_vm_parameters: "java_vm_parameters"
        enable_classloader_per_vuser: true
        java_env_class_paths:
          - "java_env_class_path1"
          - "java_env_class_path2"

  - group_name: "Mtours_pacing_random_interval"
    vusers: 50
    script_path: "scripts\\Mtours"
    rts:
      pacing:
        number_of_iterations: 2
        type: "random interval"
        delay: 10
        delay_random_range: 20
      jmeter:
        start_measurements: true
        jmeter_home_path: "c:\\jmeter"
        jmeter_min_port: 2001
        jmeter_max_port: 3001
        jmeter_additional_properties: "jmeter_additional_properties"
  - group_name: "Mtours_pacing_random_interval_Jmeter_default_port"
    vusers: 50
    script_path: "scripts\\Mtours"
    rts:
      pacing:
        number_of_iterations: 2
        type: "random interval"
        delay: 10
        delay_random_range: 20
      jmeter:
        start_measurements: true

scheduler:
  rampup: 120
  duration: 600
##################################################
```
