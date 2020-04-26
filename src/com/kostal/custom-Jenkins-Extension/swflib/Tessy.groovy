/*
 * SW-Factory Library module to run Tessy validation
*/
package com.kostal.pipelineworks.swflib

import com.kostal.pipelineworks.CommandBuilder
import com.kostal.pipelineworks.CommonTools
import com.kostal.pipelineworks.Utility

/**
 * isTessyConfigurationValid is verifiying Tessy config by checking env vars
 * and setting default values for optional env vars
 *
 * @param env The Jenkins build environment. It must contain
 *            the following variables:
 * <ul>
 *   <li>ECLIPSE_VERSION         - The path where MSYS and MinGW are
 *                                 stored</li>
 *   <li>PROJECT_NAME            - name of the project, e.g.: P12345 </li>
 *   <li>VARIANT_NAME            - name of the variant, e.g.: BCM</li>
 *   <li>RECIPIENTS              - list of e-mail recipients </li>
 *   <li>TESSY_NODE              - (optional) Tessy server node, Default: tessy </li>
 *   <li>MAKE_VAR_TESSY          - (optional) String buffer to add parameters to your
 *                                 build process as defined in you make
 *                                 tool</li>
 *    <li>SWF_TESSY              - (optional) Used Tessy Version [CI_TESSY_DEFAULT, CI_TESSY_40,
 *                                 CI_TESSY_41, CI_TESSY_4_0_18, CI_TESSY_4_0_19, CI_TESSY_4_0_25,
 *                                 CI_TESSY_4_1_12, CI_TESSY_4_1_14]</li>
 * </ul>
 **/
boolean isTessyConfigurationValid(Object env) {
    SWFTools swfTools = new SWFTools()

    println('[Tessy.groovy][isTessyConfigurationValid] Verify Tessy Config START')

    boolean configStatus = swfTools.isConfigurationValid(env,
        ['PROJECT_NAME', 'VARIANT_NAME', 'ECLIPSE_VERSION', 'RECIPIENTS'])

    //check Basic project environment
    swfTools.checkBasicProjectSetup()
    env['MAKE_VAR_TESSY'] = swfTools.checkEnvVar('MAKE_VAR_TESSY', '')
    env['TESSY_NODE'] = swfTools.checkEnvVar('TESSY_NODE', 'tessy')
    env['SWF_TESSY'] = swfTools.checkEnvVar('SWF_TESSY', 'CI_TESSY_DEFAULT')

    println('[Tessy.groovy][isTessyConfigurationValid] Verify Tessy Config END')

    return configStatus
}

/**
 * initTessyEnv is initializing Tessy Environment Vars to support all
 * available versions. Based on Env Var SWF_TESSY, workspace Environment var
 * swf_tessy_workspace is set
 *
 * @param env The Jenkins build environment. It must contain
 *            the following variables:
 *  <ul>
 *    <li>SWF_TESSY              - (optional) Used Tessy Version [CI_TESSY_DEFAULT, CI_TESSY_40,
 *                                 CI_TESSY_41, CI_TESSY_4_0_18, CI_TESSY_4_0_19, CI_TESSY_4_0_25,
 *                                 CI_TESSY_4_1_12, CI_TESSY_4_1_14]</li>
 *  </ul>
 **/
void initTessyEnv(Object env) {
    SWFTools swfTools = new SWFTools()

    println('[Tessy.groovy][initTessyEnv] Init Tessy Environment Var START')

    // check Tessy version selection by user
    env['SWF_TESSY'] = swfTools.checkEnvVar('SWF_TESSY', 'CI_TESSY_DEFAULT')
    println(
        '[Tessy.groovy][initTessyEnv] Selected Tessy Version: ' +
        env.SWF_TESSY)

    // define Tessy workspace, based on SWF_TESSY
    tessyWorkspace(env.SWF_TESSY)
    println('[Tessy.groovy][initTessyEnv] Init Tessy Environment Var END')
}

/**
 * tessyWorkspace is setting SW-Factory Tessy workspace Environment
 * var swf_tessy_workspace
 *
 * @param swfTessyVersion - Used Tessy Version [CI_TESSY_DEFAULT, CI_TESSY_40,
 * CI_TESSY_41, CI_TESSY_4_0_18, CI_TESSY_4_0_19, CI_TESSY_4_0_25,
 * CI_TESSY_4_1_12, CI_TESSY_4_1_14]
 **/
void tessyWorkspace(String swfTessyVersion) {
    println('[Tessy.groovy][tessyWorkspace] Set Tessy Workspace START')

    if (swfTessyVersion.contains('CI_TESSY_DEFAULT')) {
        env['swf_tessy_workspace'] = env.tessy_workspace_default
    }
    else if (swfTessyVersion.contains('CI_TESSY_40') ||
        swfTessyVersion.contains('CI_TESSY_4_0')) {
        env['swf_tessy_workspace'] = env.tessy_workspace_40
    }
    else if (swfTessyVersion.contains('CI_TESSY_41') ||
        swfTessyVersion.contains('CI_TESSY_4_1')) {
        env['swf_tessy_workspace'] = env.tessy_workspace_41
    }
    else {
        println(
            '[Tessy.groovy][tessyWorkspace] WARNING: Unsupported ' +
                'Tessy Version selected. Going back to default')
        env['swf_tessy_workspace'] = env.tessy_workspace_default
    }

    println(
        '[Tessy.groovy][tessyWorkspace] Tessy Workspace ' +
            'swf_tessy_workspace set to: ' + env.swf_tessy_workspace)
    println('[Tessy.groovy][tessyWorkspace] Set Tessy Workspace END')
}

/**
 * setPathToConsole is setting path to Tessy Console data based on Tessy
 * workspace and User. return value is
 * "${env.USERPROFILE}\\${workspace}\\.metadata\\console.log"
 *
 * @param env The Jenkins build environment. It must contain
 *            the following variables:
 *  <ul>
 *    <li>USERPROFILE - User profile where console data is stored</li>
 *  </ul>
 * @param workspace Used Tessy Workspace
 * [.tessy_40_workspace,.tessy_41_workspace]
 **/
String setPathToConsole(String workspace) {
    println('[Tessy.groovy][setPathToConsole] Set Tessy Console Path START')
    //check Tessy version selection by user
    String consolePath = (
        "${env.USERPROFILE}\\${workspace}\\.metadata\\console.log")

    println(
        '[Tessy.groovy][setPathToConsole] Tessy Console Path set to: ' +
            consolePath)
    println('[Tessy.groovy][setPathToConsole] Set Tessy Console Path END')

    return consolePath
}

/**
 * initTessyNode is initializing Tessy Environment on a test node
 *
 * <p>
 * Following tasks are done: SCM Checkout, Kill and clear Tessy tasks, clear
 * Temp workspace
 *
 * @param env The Jenkins build environment. It must contain
 *            the following variables:
 *  <ul>
 *    <li>TOOLS_PATH_TESSY    - The relative path where the Tools are
 *                              located</li>
 *    <li>swf_tessy_workspace  - Tessy Workspace var(automatically set by
 *                               tessyWorkspace based on SWF_TESSY),
 *                               default: tessy_workspace_default</li>
 *    <li>KA_RTE_GEN         - (optional) Activation of Kostal RTE Gen build [ON/OFF]</li>
 *  </ul>
 **/
void initTessyNode(Object env) {
    // include external libs
    CommandBuilder commandBuilder = new CommandBuilder()
    SWFCheckout swfCheckout = new SWFCheckout()
    KaRte swfKaRte = new KaRte()
    SWFTools swfTools = new SWFTools()
    Utility swfUtility = new Utility()

    //check Basic project environment
    swfTools.checkBasicProjectSetup()
    env['TOOLS_PATH_TESSY'] = swfUtility.toWindowsPath(swfTools.checkEnvVar('TOOLS_PATH_TESSY', env['TOOLS_PATH']))

    //Init Tessy Environment
    println('[Tessy.groovy][initTessyNode] Init Tessy Workspace START')
    println(
        '[Tessy.groovy][initTessyNode] Workspace location: ' +
            env.swf_tessy_workspace)

    swfCheckout.checkout(env)

    // build KA-RTE, if necessary
    if (swfTools.checkON(env.KA_RTE_GEN)) {
        swfKaRte.buildKaRte(env)
    }
    else {
        println('[Tessy.groovy][initTessyNode] KA-RTE deactivated.')
    }

    println('[Tessy.groovy][initTessyNode] Kill Tessy Task')

    // Detects if Tessy is already running and stop it.
    bat '''tasklist /FI "IMAGENAME eq TESSY.exe" 2>NUL | find /I /N "TESSY.exe">NUL
    if "%ERRORLEVEL%"=="0" taskkill /f /im TESSY.exe
    '''.stripIndent()

    /*
     If the project used has the same name of an existent project in the
     workspace, Tessy will not open, for that, the workspace with the list of
     existent projects is deleted and Tessy will configure the actual project
     as a new project in workspace.
    */
    println('[Tessy.groovy][initTessyNode] Check User Profile')
    bat commandBuilder.buildBat([
        '@echo on',
        "@if exist %userprofile%\\${env.swf_tessy_workspace} rd /S /Q " +
            "%userprofile%\\${env.swf_tessy_workspace}"
    ])

    // Cleanup the workspace of artifacts that can alter the report

    println('[Tessy.groovy][initTessyNode] CleanUp Workspace')
    bat commandBuilder.buildBat([
        '@echo on',
        '@set WORKSPACE=%cd%',
        "@set TESSY_HTML_CONTENT=SWF_Project\\${env.TOOLS_PATH_TESSY}\\" +
            'Tessy\\report\\Tessy_html_content',
        '@if exist %WORKSPACE%\\%TESSY_HTML_CONTENT% rd /s /q ' +
            '%WORKSPACE%\\%TESSY_HTML_CONTENT%',
        '@md %WORKSPACE%\\%TESSY_HTML_CONTENT%'
    ])

    println('[Tessy.groovy][initTessyNode] Init Tessy Workspace END')
}

/**
 * buildTessy is building Tessy Validation make scripts
 * <p>
 * make calls are:
 * <ul>
 *  <li>[TOOLS_PATH]/make/[MAKE_TOOL_TESSY] [TESSY_BUILD] [MAKE_VAR_TESSY]
 *      -j[MAKE_CORE_NO_TESSY] preparelists</li>
 *  <li>[TOOLS_PATH]/make/[MAKE_TOOL_TESSY] [TESSY_BUILD] [MAKE_VAR_TESSY]
 *      -j[MAKE_CORE_NO_TESSY] tessy_sync</li>
 *  <li>[TOOLS_PATH]/make/[MAKE_TOOL_TESSY] [TESSY_BUILD] [MAKE_VAR_TESSY]
 *      -j[MAKE_CORE_NO_TESSY] tessy_batch</li>
 * </ul>
 * @param env The Jenkins build environment. It must contain
 *            the following variables:
 *  <ul>
 *    <li>ECLIPSE_VERSION    - The path where MSYS and MinGW are stored</li>
 *    <li>TOOLS_PATH_MAKE    - (optional)The relative path where the Tools are located</li>
 *    <li>PROJECT_NAME       - name of the project, e.g.: P12345 </li>
 *    <li>VARIANT_NAME       - name of the variant, e.g.: BCM</li>
 *    <li>TESSY_BUILD        - (optional) Build mode for Tessy, Default: release</li>
 *    <li>MAKE_VAR_TESSY     - (optional) Parameter for Tessy build</li>
 *    <li>MAKE_TOOL_TESSY    - (optional) used make tool. Default:Make.bat</li>
 *    <li>MAKE_CORE_NO_TESSY - (optional) No of cores to run Tessy process.
 *                             Default: 16</li>
 *  </ul>
 **/
@SuppressWarnings('BuilderMethodWithSideEffects')  // Not a builder method
void buildTessy(Object env) {
    // include external libs
    CommandBuilder commandBuilder = new CommandBuilder()
    SWFTools swfTools = new SWFTools()
    Utility swfUtility = new Utility()

    //check Basic project environment
    swfTools.checkBasicProjectSetup()
    env['TOOLS_PATH_MAKE'] = swfUtility.toWindowsPath(swfTools.checkEnvVar('TOOLS_PATH_MAKE', env['TOOLS_PATH']))

    env['PROJECT_VARIANT_NAME'] = env.PROJECT_NAME + '_' + env.VARIANT_NAME
    env['MAKE_VAR_TESSY'] = swfTools.checkEnvVar('MAKE_VAR_TESSY', '')
    env['MAKE_TOOL_TESSY'] = swfTools.checkEnvVar('MAKE_TOOL_TESSY', 'make.bat')
    env['MAKE_CORE_NO_TESSY'] = swfTools.checkEnvVar('MAKE_CORE_NO_TESSY', '16')
    env['TESSY_BUILD'] = swfTools.checkEnvVar('TESSY_BUILD', 'release')

    println('[Tessy.groovy][buildTessy] START Tessy build')

    bat commandBuilder.buildBat([
        '@echo on',
        "@set MINGW=%${env.ECLIPSE_VERSION}%\\mingw\\bin",
        "@set MSYS=%${env.ECLIPSE_VERSION}%\\msys64\\usr\\bin",
        '@set PATH=%MINGW%;%MSYS%',
        '@set WORKSPACE=%cd%',
        "@cd %WORKSPACE%\\SWF_Project\\${env.TOOLS_PATH_MAKE}\\Make",
        '@echo ======== Execution preparelists target ====================',
        "@${env.MAKE_TOOL_TESSY} ${env.TESSY_BUILD} ${env.MAKE_VAR_TESSY} " +
            "-j${env.MAKE_CORE_NO_TESSY} preparelists",
        '@echo ======== Finishing preparelists execution ===================='
    ])

    bat commandBuilder.buildBat([
        '@echo on',
        "@set MINGW=%${env.ECLIPSE_VERSION}%\\mingw\\bin",
        "@set MSYS=%${env.ECLIPSE_VERSION}%\\msys64\\usr\\bin",
        '@set PATH=%MINGW%;%MSYS%',
        '@set WORKSPACE=%cd%',
        "@cd %WORKSPACE%\\SWF_Project\\${env.TOOLS_PATH_MAKE}\\Make",
        '@echo ========   Execution tessy_sync target  ====================',
        "@${env.MAKE_TOOL_TESSY} ${env.TESSY_BUILD} ${env.MAKE_VAR_TESSY} " +
            "-j${env.MAKE_CORE_NO_TESSY} tessy_sync",
        '@echo ======== Finishing tessy_sync execution ===================='
    ])

    bat commandBuilder.buildBat([
        '@echo on',
        "@set MINGW=%${env.ECLIPSE_VERSION}%\\mingw\\bin",
        "@set MSYS=%${env.ECLIPSE_VERSION}%\\msys64\\usr\\bin",
        '@set PATH=%MINGW%;%MSYS%',
        '@set WORKSPACE=%cd%',
        "@cd %WORKSPACE%\\SWF_Project\\${env.TOOLS_PATH_MAKE}\\Make",
        '@echo ========   Execution tessy_batch target   ====================',
        "@${env.MAKE_TOOL_TESSY} ${env.TESSY_BUILD} ${env.MAKE_VAR_TESSY} " +
            "-j${env.MAKE_CORE_NO_TESSY} tessy_batch",
        '@echo ========  Finishing tessy_batch execution ===================='
    ])

    println('[Tessy.groovy][buildTessy] END Tessy build')
}

/**
 * copyOutput copies outputs of tessy make call to Project template folder
 * <p>
 * Copied files are:
 * <ul>
 *  <li>Tessy log file:
 *  C:\\Users\\pvcsad01\\[swf_tessy_workspace]\\.metadata\\console.log</li>
 *  <li>Tessy report file: [TOOLS_PATH]/Tessy/report</li>
 * </ul>
 * @param env The Jenkins build environment. It must contain
 *            the following variables:
 *  <ul>
 *    <li>TOOLS_PATH_TESSY - (optional)The relative path where the Tools are located</li>
 *    <li>PROJECT_NAME - name of the project, e.g.: P12345 </li>
 *    <li>VARIANT_NAME - name of the variant, e.g.: BCM</li>
 *    <li>swf_tessy_workspace - Tessy Workspace var, default:
 *        tessy_workspace_default</li>
 *  </ul>
 **/
void copyOutput(Object env) {
    CommandBuilder commandBuilder = new CommandBuilder()
    SWFTools swfTools = new SWFTools()
    Utility swfUtility = new Utility()

    //check Basic project environment
    swfTools.checkBasicProjectSetup()
    env['TOOLS_PATH_TESSY'] = swfUtility.toWindowsPath(swfTools.checkEnvVar('TOOLS_PATH_TESSY', env['TOOLS_PATH']))

    env['PROJECT_VARIANT_NAME'] = env.PROJECT_NAME + '_' + env.VARIANT_NAME

    println('[Tessy.groovy][copyOutput] copy log files to output')

    env['swf_tessy_console_log'] = setPathToConsole(env.swf_tessy_workspace)

    bat commandBuilder.buildBat([
        '@echo on',
        '@set WORKSPACE=%cd%',
        "@XCOPY \"${env.swf_tessy_console_log}\" ^",
        "\"%WORKSPACE%\\${env.PROJECT_VARIANT_NAME}\\log\\Tessy\\" +
            'TESSY_console.log*" /s /i /Y'
    ])

    println('[Tessy.groovy][copyOutput] copy report files to output')

    bat commandBuilder.buildBat([
        '@echo on',
        '@set WORKSPACE=%cd%',
        "@XCOPY \"%WORKSPACE%\\SWF_Project\\${env.TOOLS_PATH_TESSY}\\" +
            'Tessy\\report\\*" ^',
        "\"%WORKSPACE%\\${env.PROJECT_VARIANT_NAME}\\report\\Tessy\" /s /i /Y",
        "@XCOPY \"%WORKSPACE%\\SWF_Project\\${env.TOOLS_PATH_TESSY}\\" +
            'Tessy\\report\\*" ^',
        "\"%WORKSPACE%\\${env.PROJECT_VARIANT_NAME}\\data\\Tessy\\" +
            'output" /s /i /Y'
   ])

    println('[Tessy.groovy][copyOutput] copy files done')
}

/**
 * archivingTessy is archiving outputs of Tessy stage
 * <p>
 * Following folders are archived:
 * <ul>
 *  <li>[PROJECT_VARIANT_NAME]/data/Tessy/*</li>
 *  <li>[PROJECT_VARIANT_NAME]/log/Tessy/*</li>
 *  <li>[PROJECT_VARIANT_NAME]/report/Tessy/*</li>
 * </ul>
 * @param env The Jenkins build environment. It must contain
 *            the following variables:
 *  <ul>
 *    <li>PROJECT_NAME - name of the project, e.g.: P12345 </li>
 *    <li>VARIANT_NAME - name of the variant, e.g.: BCM</li>
 *  </ul>
 **/
void archivingTessy(Object env) {
    CommonTools commonTools = new CommonTools()
    env['PROJECT_VARIANT_NAME'] = env.PROJECT_NAME + '_' + env.VARIANT_NAME

    //Archive artifacts
    //Archivation of output files.
    println('[Tessy.groovy][archivingTessy] Archiving START')

    commonTools.archiveArtifacts(
        env.PROJECT_VARIANT_NAME + '/log/Tessy/**/*')
    commonTools.archiveArtifacts(
        env.PROJECT_VARIANT_NAME + '/data/Tessy/**/*')
    commonTools.archiveArtifacts(
        env.PROJECT_VARIANT_NAME + '/report/Tessy/**/*')

    println('[Tessy.groovy][archivingTessy] Archiving END')
}

/**
 * stashingTessy is stashing outputs of Tessy stage
 * Following folders are stashed:
 * <ul>
 *  <li>-swflib_tessy_report: [PROJECT_VARIANT_NAME]/report/Tessy/*</li>
 * </ul>
 * @param env The Jenkins build environment. It must contain
 *            the following variables:
 *  <ul>
 *    <li>PROJECT_NAME - name of the project, e.g.: P12345 </li>
 *    <li>VARIANT_NAME - name of the variant, e.g.: BCM</li>
 *  </ul>
 **/
void stashingTessy(Object env) {
    env['PROJECT_VARIANT_NAME'] = env.PROJECT_NAME + '_' + env.VARIANT_NAME

    println('[Tessy.groovy][stashingTessy] stashing Tessy output START')

    stash([
        includes: env.PROJECT_VARIANT_NAME + '/report/Tessy/**/*',
        name: 'swflib_tessy_report'
    ])

    println('[Tessy.groovy][stashingTessy] Stash swflib_tessy_report for ' +
        'Tessy output done.')
    println('[Tessy.groovy][stashingTessy] stashing Tessy output END')
}

/**
 * swflib_qac to build a SW-Factory Lib Tessy Report.
 * <p>
 * Following pipeline stages are created:
 * <ul>
 *  <li>- Tessy Report</li>
 * </ul>
 * <p>
 * @param env The Jenkins build environment. It must contain
 *            the following variables:
 *  <ul>
 *    <li>PROJECT_NAME - name of the project, e.g.: P12345 </li>
 *    <li>VARIANT_NAME - name of the variant, e.g.: BCM</li>
 *    <li>TOOLS_PATH_MAKE - (optional) The relative path where the Tools are located</li>
 *    <li>TOOLS_PATH_TESSY - (optional) The relative path where the Tools are located</li>
 *    <li>ECLIPSE_VERSION - The path where MSYS and MinGW are stored</li>
 *    <li>SOURCES_PATH - The relative path where the Sources are located</li>
 *    <li>SWF_TESSY - Used Tessy Version [CI_TESSY_DEFAULT, CI_TESSY_40,
 *                    CI_TESSY_41, CI_TESSY_4_0_18, CI_TESSY_4_0_19,
 *                    CI_TESSY_4_0_25, CI_TESSY_4_1_12, CI_TESSY_4_1_14]</li>
 *    <li>MAKE_TOOL_TESSY - (optional) used make tool. Default:Make.bat</li>
 *    <li>MAKE_CORE_NO_TESSY - (optional) No of cores to run Tessy process.
 *                             Default: 16</li>
 *    <li>TESSY_ANALYSIS - Activation of Tessy [ON/OFF]</li>
 *    <li>KA_RTE_GEN - Activation of Kostal RTE Gen build [ON/OFF]</li>
 *   <li>RECIPIENTS              - list of e-mail recipients </li>
 *   <li>TESSY_NODE              - (optional) Tessy server node, Default: tessy </li>
 *   <li>MAKE_VAR_TESSY          - (optional) String buffer to add parameters to your
 *                                 build process as defined in you make
 *                                 tool</li>
 *  </ul>
 **/
void generateReport(Object env = this.env) {
    Notifier notifier = new Notifier()
    SWFTools swfTools = new SWFTools()

    if (!swfTools.checkON(env.TESSY_ANALYSIS)) {
        //Tessy is OFF
        env['TESSY_ANALYSIS'] = 'OFF'
        println('[generateReport.groovy] Tessy deactivated.')
        return
    }
    // verify Configuration first
    if (!isTessyConfigurationValid(env)) {
        // Config Error, throw error
        error 'Tessy failed due to an incorrect configuration of the ' +
        'environment variables. -> Check the environment variables in the job configuration.'
    }
    // execute on TESSY_NODE
    node (TESSY_NODE) {
        // add seperate Stage for Tessy
        stage('Tessy Report') {
            try {
                println('[Tessy.groovy] Tessy Validation start.')
                // init Tessy Environment
                initTessyEnv(env)
                // init Test node
                initTessyNode(env)
                // build Tessy report
                buildTessy(env)
                // copy output into Tmp workspace
                copyOutput(env)
                // archive Test results
                archivingTessy(env)
                // stash results for reporting
                stashingTessy(env)
                println('[Tessy.groovy] Tessy Validation  end.')
            } catch (e) {
                println('[Tessy.groovy] generateReport failed. Check Project Config')
                notifier.onFailure(env, e)
            }
        }
    }
}
