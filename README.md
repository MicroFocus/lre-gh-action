![OpenText Logo](https://upload.wikimedia.org/wikipedia/commons/1/1b/OpenText_logo.svg)

# OpenText Enterprise Performance Engineering Test Execution Action
This GitHub Action has for purpose to trigger and monitor a performance test execution (preexisting or designed according to a yaml file in the git repo) in server, eventually collect reports (analysis and trend reports) and report status.

## Prerequisites

1. Any workflow including this action will need to have preliminary steps such as (see example below):
 - actions/checkout@v4
 - actions/setup-java@v4 (version 11)
 - actions/setup-node@v4 followed by another step performing npm install .
2. Usernames and passwords will need to be saved as secrets and used as environment variables in the workflow.
3. if you wish to save the reports to build artifact, you can add the following step: actions/upload-artifact@v4 (see example below).

## Action Inputs

| Input                             | Description                                                                                                                                                                                                                      | Required | Default                                |
|-----------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|----------|----------------------------------------|
| **lre_action** | action to be triggered. Current supported action: ExecuteLreTest | false | ExecuteLreTest |
| **lre_description** | Description of the action (will be displayed in console logs) | false | |
| **lre_server** | Server, port (when not mentionned, default is 80 or 433 for secured) and tenant (when not mentionned, default is ?tenant=fa128c06-5436-413d-9cfa-9f04bb738df3). e.g.: mylreserver.mydomain.com:81/?tenant=fa128c06-5436-413d-9cfa-9f04bb738df3' | true |                                        |
| **lre_https_protocol** | Use secured protocol for connecting to the server. Possible values: true or false | false | false |
| **lre_authenticate_with_token** | Authenticate with token (access key). Required when SSO is configured in the server. Possible values: true or false | false | false |
| **lre_username** | Username | true | |
| **lre_password** | Password | true | |
| **lre_domain** | domain (case sensitive) | true | |
| **lre_project** |  project (case sensitive) | true | |
| **lre_test** |valid test ID# or relative path to yaml file in git repo defining new test design creation | true | |
| **lre_test_instance** | either specify AUTO to use any instance or a specific instance ID | false | AUTO |
| **lre_timeslot_duration_hours** | timeslot duration in hours | false | 0 |
| **lre_timeslot_duration_minutes** | timeslot duration in minutes | false | 30 |
| **lre_post_run_action** | Possible values for post run action: 'Collate Results', 'Collate and Analyze' or 'Do Not Collate' | false | Do Not Collate |
| **lre_vuds_mode** | Use VUDS licenses. Possible values: true or false | false | false |
| **lre_trend_report** | The possible values (no value or not defined means no trend monitoring in build but will not cancel trend report defined in LRE): ASSOCIATED (the trend report defined in the test design will be used') or specify valid report ID# | false | |
| **lre_proxy_out_url** | proxy URL | false | |
| **lre_username_proxy** | proxy username | false | |
| **lre_password_proxy** | proxy password | false | |
| **lre_search_timeslot** | Experimental: Search for matching timeslot instead of creating a new timeslot. Possible values: true or false | false | false |
| **lre_status_by_sla** | Report success status according to SLA. Possible values: true or false | false | false |
| **lre_output_dir** | The directory to read the checkout folder and to save results (use ${{ github.workspace }}) | false | ./ |
| **lre_enable_stacktrace** | if set to true, stacktrace of exception will be displayed with error occur reported in console logs  | false | false |

## Examples

### Authenticate using :token (access key) provided user, create performance test according to YamlTest/createTestFromYaml.yaml file available in git repo, execute it, wait for analysis and trending to complete, download the reports and upload them to build artifact

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
        uses: actions/checkout@v4

      - name: Setup Java
        uses: actions/setup-java@v4
        with:
          distribution: adopt
          java-version: '11'

      - name: Set up Node.js
        uses: actions/setup-node@v4
        with:
          node-version: '20'

      - name: Use GitHub Action
        uses: MicroFocus/lre-gh-action@v1.0.3
        with:
          lre_action: ExecuteLreTest
          lre_description: running new yaml test
          lre_server: mylreserver.mydomain.com/?tenant=fa128c06-5436-413d-9cfa-9f04bb738df3
          lre_https_protocol: true
          lre_authenticate_with_token: false
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
        uses: actions/upload-artifact@v4
        with:
          name: build-artifacts
          path: ${{ github.workspace }}/LreResult
```

content of YamlTest/createTestFromYaml.yaml from the exemple (when using script_path parameter, the script with the mentionned path must be existing under the "subject" root of Test Plan otherwise you could use script_id parameter to refer to the script via its ID instead):
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

### Authenticate using username and password (with lre_authenticate_with_token set to true which requires providing tokens in credentials), execute performance test, wait for analysis and trending to complete, download the reports and upload them to build artifact.

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
      lre_username: ${{ secrets.LRE_USERNAME_TOKEN }}
      lre_password: ${{ secrets.LRE_PASSWORD_TOKEN }}
    steps:
      - name: Checkout code
        uses: actions/checkout@v4

      - name: Setup Java
        uses: actions/setup-java@v4
        with:
          distribution: adopt
          java-version: '11'

      - name: Set up Node.js
        uses: actions/setup-node@v4
        with:
          node-version: '20'

      - name: Use My GitHub Action
        uses: MicroFocus/lre-gh-action@v1.0.3
        with:
          lre_action: ExecuteLreTest
          lre_description: running new yaml test
          lre_server: myserver.mydomain.com/?tenant=fa128c06-5436-413d-9cfa-9f04bb738df3
          lre_https_protocol: true
          lre_authenticate_with_token: true
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
| group | Lists all groups or scripts defined in the test. The parameter to be used in each group are specified in the 'group' table below. | Yes |
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
| java_vm_parameters | List the Java command line parameters. These parameters can be any JVM argument. The common arguments are the debug flag (-verbose) or memory settings (-ms, -mx). In additional, you can also pass properties to Java applications in the form of a -D flag. | No |
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
| jmeter_additional_properties | JMeter additional properties file. Use double slash (\\) for folder separator. | No |

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
| report_id | Id of the trend report to associate the test run analysis with. | No |
| max_runs_in_report | Maximum trends in a report (default is 10 if not specified). | No |

* * * * *

**lg_elastic_configuration:** (available from Performance Center 12.62 and plugin version 1.1.1)

| Parameter | Description | Required |
|-----------|-------------|----------|
| image_id | This number can be retrieved from: <br> -   The Administration page (you might need to turn to your administrator as accessing this page requires admin privileges): select the **Orchestration** section -> switch to **Docker Images** tab -> you will have the list of all available Docker images for Load Generator purposes with their ID. You can make sure the images are available to your project from the **Orchestrators** tab. <br> -   A OpenText Enterprise Performance Engineering Rest API command applied on the project (replace the bracketed values): GET - [http(s)://(PCServer):(PortNumber)/LoadTest/rest/domains/(DomainName)/projects/(ProjectName)/dockerimages/](file:///C:/GIT/plugins/micro-focus-performance-center-integration-plugin/src/main/resources/com/microfocus/performancecenter/integration/pcgitsync/PcGitSyncBuilder/help-importTests.html#) and select any valid image not having the value 'controller' for purpose. | Yes if one of the load generator is defined to be provisioned from Docker image. |
| memory_limit | This parameter can be retrieved from **Application** -> **Test Management** -> edit a test -> Press **Assign LG** button -> in the **Elastic** section, select **DOCKER1** -> select the relevant image (based on the image name) -> use the values provided in the 'Memory(GB)' dropdown list (if not specified, this parameter should be ignored). | Yes, if the image is defined with resource limits |
| cpu_limit | This parameter can be retrieved from **Application** -> **Test Management** -> edit a test -> Press **Assign LG** button -> in the **Elastic** section, select **DOCKER1** -> select the relevant image (based on the image name) -> use the values provided in the 'CUPs' dropdown list (if not specified, this parameter should be ignored). | Yes, if the image is defined with resource limits |

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
