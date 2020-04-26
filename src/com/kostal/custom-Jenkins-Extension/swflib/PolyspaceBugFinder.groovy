package com.kostal.pipelineworks.swflib

import com.cloudbees.groovy.cps.NonCPS
import org.jenkinsci.plugins.workflow.cps.EnvActionImpl
import org.jenkinsci.plugins.workflow.job.WorkflowRun
import hudson.FilePath
import hudson.Functions

import groovy.transform.SourceURI
import java.nio.file.Path
import java.nio.file.Paths
import groovy.json.JsonBuilder
import groovy.json.JsonSlurper

import com.kostal.pipelineworks.CommandBuilder
import com.kostal.pipelineworks.CheckoutTools
import com.kostal.pipelineworks.Checkout
import com.kostal.pipelineworks.DefectNotifier
import com.kostal.pipelineworks.Utility
import com.kostal.pipelineworks.CommonTools
import com.kostal.pipelineworks.CITools
import com.kostal.pipelineworks.customer.CustomerArtifactsPolyspace

import com.kostal.pipelineworks.polyspace.BuildInformation
import com.kostal.pipelineworks.polyspace.BacuRequest

/**
 * Check if the input object is null or an empty string.
**/
boolean isEmpty(Object toCheck) {
    return toCheck == null || toCheck == ''
}

/**
 * Check if toCheck is empty or null, and return defaultValue if yes.
 * Otherwise return toCheck.
**/
String setWithDefault(Object toCheck, String defaultValue) {
    if (isEmpty(toCheck)) {
        return defaultValue
    }
    return toCheck
}

/**
 * Returns Definition of environment variables for the execution of Polyspace
 *
 * @param env : The current Jenkins environment.
 * @return env : new environment
 * */
EnvActionImpl createEnv(EnvActionImpl env) {
    Utility utility = new Utility()
    String projectVariant = env.PROJECT_NAME + '_' + env.VARIANT_NAME

    env.first = setWithDefault(env.first, 'true')
    if (env.first != 'true') {
        return env
    }

    env.first = 'false'

    println('Creating Environment')

    env.ECLIPSE_VERSION      = setWithDefault(env.ECLIPSE_VERSION, env.CI_ECLIPSE_MS16_2)
    env.ECLIPSE_HOME         = setWithDefault(env.ECLIPSE_HOME, env.CI_ECLIPSE_MS16_2)
    env.MINGW                = "${env.ECLIPSE_HOME}/mingw/bin"
    env.MSYS                 = "${env.ECLIPSE_VERSION}/msys64/usr/bin"
    env.PATH                 = "${env.PATH};${env.MSYS};${env.ECLIPSE_VERSION}"
    env.CI_PROJECT           = setWithDefault(env.CI_PROJECT, 'SWF_Project')
    env.PROJECT_VARIANT_NAME = setWithDefault(env.PROJECT_VARIANT_NAME, projectVariant)

    if (isEmpty(env.JENKINS_SERVER_STACK)) {
        env.JENKINS_SERVER_STACK = ((
            isEmpty(env.AUTOSAR_PROJECT_LAYER)
        ) ? '' : (env.AUTOSAR_PROJECT_LAYER + '/')) + 'JenkinsServerStack'
    }

    if (isEmpty(env.JENKINS_SHARE)) {
        env.JENKINS_SHARE = utility.toLinuxPath(
        "${env.WORKSPACE}/${env.CI_PROJECT}/${env.JENKINS_SERVER_STACK}")
    } else {
        env.JENKINS_SHARE = utility.toLinuxPath(env.JENKINS_SHARE)
    }

    env.JENKINS_SHARE_BSLASH = utility.toWindowsPath(env.JENKINS_SHARE)
    env.JENKINS_SHARE_ARCHIVE = setWithDefault(
        env.JENKINS_SHARE_ARCHIVE,
        "${env.CI_PROJECT}/${env.JENKINS_SERVER_STACK}")

    if (isEmpty(env.WORKSPACE_PROJECT)) {
        env.WORKSPACE_PROJECT = env.WORKSPACE + '/' + CI_PROJECT + (
            env.AUTOSAR_PROJECT_LAYER == null ? '' :
                "/${env.AUTOSAR_PROJECT_LAYER}")
    }

    env.WORKSPACE_PROJECT_BSLASH = utility.toWindowsPath(env.WORKSPACE_PROJECT)
    env.PS_LAST_VERSION_ID_BUILT = ''
    env.PS_LAST_VERSION_ID_BUILT_TEMP = ''

    env.PS_RESULT_DIR_FILE = utility.toLinuxPath(
        "${env.JENKINS_SHARE}/ps_result_dir.txt")
    env.PS_RESULT_DIR_FILE_BSLASH = utility.toWindowsPath(
        "${env.JENKINS_SHARE_BSLASH}/ps_result_dir.txt")
    env.PS_PROJECT_VARIANT_NAME_FILE = utility.toWindowsPath(
        "${env.JENKINS_SHARE_BSLASH}/ps_ProjectVariantName.txt")

    env.PS_BUILD_ID = setWithDefault(env.PS_BUILD_ID, env.BUILD_ID)
    env.BRANCH_URL = setWithDefault(
        env.BRANCH_URL, "${env.REPOSITORY_URL}${env.REPOSITORY_DEV_REP}/")

    env.CI_POLY_PATH = env.CI_POLYSPACE
    env.CI_POLY_CP_PATH = env.CI_POLYPSPACE_CP

    // Important: The String CI_Project is the root directory where all
    // sources are checked out from SVN

    env.SOURCES_PATH_G00500 = 'Source'
    env.TARGET_PATH_G00500 = 'Target/release'
    env.TOOLS_PATH_G00500 = 'Tools'

    // DEBUG Switch to not do the checkout
    if (isEmpty(env.DEBUG_DONOT_CHECKOUT)) {
        env.DEBUG_DONOT_CHECKOUT = 'false'
        println('DEBUG_DONOT_CHECKOUT is set to FALSE; normal SVN check-out will be performed.')
    }

    //DO_NOT_DELETE_RESULTSDIR switch set to true, do not delete Bugfinder results directory!
    if (isEmpty(env.DO_NOT_DELETE_RESULTSDIR)) {
        env.DO_NOT_DELETE_RESULTSDIR = 'false'
        println(
            'DO_NOT_DELETE_RESULTSDIR is set to FALSE; the BugFinder results directory ' +
            'will not be deleted.')
    }

    // only add
    if (isEmpty(env.MAKE_VAR)) {
        env.MAKE_VAR = env.POLYSPACE_ANALYSIS_BUILD + ' %WORKSPACE% -j24'
        println('MAKE_VAR not defined, using default:' + env.MAKE_VAR)
    }

    if (!env.MAKE_VAR.contains(env.POLYSPACE_ANALYSIS_BUILD)) {
        env.MAKE_VAR = env.POLYSPACE_ANALYSIS_BUILD + ' ' + env.MAKE_VAR
        println(
            'MAKE_VAR added with POLYSPACE_ANALYSIS_BUILD (pasted in before): ' +
            env.MAKE_VAR)
    }

    if (!isEmpty(env.ADDPARAM) && !env.MAKE_VAR.contains(env.ADDPARAM)) {
        env.MAKE_VAR = env.MAKE_VAR + ' ' + env.ADDPARAM
        println(
            'MAKE_VAR added with ADDPARAM (added after): ' + env.MAKE_VAR)
    }

    if (!isEmpty(env.ADD_LAST_PARAM) && !env.MAKE_VAR.contains(env.ADD_LAST_PARAM)) {
        println(
            'ADD_LAST_PARAM  defined, it will be the last calling parameter of make.bat:' +
            env.ADD_LAST_PARAM)
        println('MAKE_VAR now:' + env.MAKE_VAR)
    }
    else {
        env.ADD_LAST_PARAM = ''
        println(
            'ADD_LAST_PARAM not defined, using default (empty):' +
            env.ADD_LAST_PARAM)
    }

    env.SOURCEFOLDER = setWithDefault(
        env.SOURCEFOLDER, "${env.CI_PROJECT}/${env.SOURCES_PATH}")
    env.TARGET = setWithDefault(env.TARGET, env.VARIANT_NAME)

    env.TOOLS_FOLDER_PATH = setWithDefault(
        env.TOOLS_FOLDER_PATH, "${env.CI_PROJECT}/${env.TOOLS_PATH}")

    env.ALERT_DEVELOPER_TOOL_PATH = setWithDefault(
        env.ALERT_DEVELOPER_TOOL_PATH,
        env.TOOLS_FOLDER_PATH + '/Polyspace/AlertDeveloper')

    env.WORKSPACE_SLASH = utility.toLinuxPath(env.WORKSPACE)

    if (isEmpty(env.POLYSPACE_TOOL_PATH)) {
        env.POLYSPACE_TOOL_PATH =
            env.WORKSPACE_SLASH + '/' + env.TOOLS_FOLDER_PATH + '/Polyspace'
    }

    if (isEmpty(env.REST_PS_RESULT_DIR)) {
        env.REST_PS_RESULT_DIR = 'Polyspace/BugFinder/_result'
    }
    if (isEmpty(env.ARCHIVE_INPUT_FOLDER)) {
        env.ARCHIVE_INPUT_FOLDER =
            env.PROJECT_VARIANT_NAME + '/data/Polyspace/input/BugFinder'
    }

    if (isEmpty(env.ARCHIVE_OUTPUT_FOLDER_BF)) {
        env.ARCHIVE_OUTPUT_FOLDER_BF =
            env.PROJECT_VARIANT_NAME + '/data/Polyspace/output/BugFinder'
    }
    if (isEmpty(env.ARCHIVE_REPORT_FOLDER)) {
        env.ARCHIVE_REPORT_FOLDER =
            env.PROJECT_VARIANT_NAME + '/report/Polyspace/BugFinder'
    }

    /* THRESHOLD_DELTA_DEFECTS  Here one can define a delta threshold value
       for defects. It
       refers to the BugFinder defects trend */
    if (isEmpty(env.THRESHOLD_DELTA_DEFECTS)) {
        env.THRESHOLD_DELTA_DEFECTS = '10'
    }

    if (isEmpty(env.BUILD_SELECTOR)) {
        env.BUILD_SELECTOR = 'lastSuccessful'
    }

    env.MAKE_TOOL_ENV = 'make.bat'

    //check make tool call
    if (isEmpty(env.MAKE_TOOL)) {
        env.MAKE_TOOL = env.MAKE_TOOL_ENV
        println('MAKE_TOOL not defined, using default:' + env.MAKE_TOOL)
    }

    if (isEmpty(env.MAKE_TOOL_PATH)) {
        env.MAKE_TOOL_PATH = 'Make'
        println('MAKE_TOOL_PATH not defined, using default:' + env.MAKE_TOOL_PATH)
    }

    env.QAC_STASH_FOLDER = env.PROJECT_VARIANT_NAME + '/report/QAC/**/*'

    if (env.QAC_VERSION == 'QAC9') {
        env.QAC_outputFile = 'qac_cli_output.qar'
        env.MISRA_vers = '2012'
    }
    else if (env.QAC_VERSION == 'QAC') {
        env.QAC_outputFile = 'QACResult.qar'
        env.MISRA_vers = '2004'
    }

    println('QAC_VERSION is:' + env.QAC_VERSION)

    env.TOOLS_FOLDER_PATH_BSLASH = utility.toWindowsPath(env.TOOLS_FOLDER_PATH)
    env.WORKSPACE_BSLASH = utility.toWindowsPath(env.WORKSPACE)
    env.QAC_outputPath_BSLASH = utility.toWindowsPath(env.QAC_outputPath)
    env.ALERT_DEVELOPER_TOOL_PATH_BSLASH = utility.toWindowsPath(env.ALERT_DEVELOPER_TOOL_PATH)

    if (isEmpty(env.PSCP_ANALYSIS)) {
        env.PSCP_ANALYSIS = 'OFF'
        println(
            'PSCP_ANALYSIS is not defined, so no Polyspace CodeProver Analysis will run. ')
    }
    if (isEmpty(env.PSBF_ANALYSIS)) {
        env.PSBF_ANALYSIS = 'OFF'
    }

    // POLYSPACE_ANALYSIS: Old deprecated flag for enabling Polyspace Bugfinder Analysis Tool
    if (!isEmpty(env.POLYSPACE_ANALYSIS)) {
        println(
            'POLYSPACE_ANALYSIS is defined, but deprecated, please use PSBF_ANALYSIS ' +
            'for BugFinder analysis')

        if (env.PSBF_ANALYSIS == 'OFF') {
            env.PSBF_ANALYSIS = env.POLYSPACE_ANALYSIS == 'ON' ? 'true' : 'false'
        }
        else {
            println(
                'PSBF_ANALYSIS is also defined, so it will be used instead of ' +
                'POLYSPACE_ANALYSIS for BugFinder analysis.'
            )
        }
    }

    // Setting Flags for AlertDeveloper if emails are wanted for

    // Polyspace Alerting tool  config
    // ALERT_DEVELOPER: Old deprecated flag for enabling Polyspace Bugfinder Alerting Tool
    if (isEmpty(env.ALERT_DEVELOPER)) {
        env.PS_CHECK = 'false'
    }
    else {
        env.PS_CHECK = env.ALERT_DEVELOPER == 'ON' ? 'true' : 'false'
        println(
            'ALERT_DEVELOPER is defined, but deprecated, please use ' +
            'POLYSPACE_BF_ANALYSIS_EMAIL ' +
            'for BugFinder for emailing the ' +
            'defects as default (VALUES: ON|OFF)')
    }

    // POLYSPACE_BF_ANALYSIS_EMAIL: New flag for enabling Polyspace Bugfinder Alerting Tool
    if (env.POLYSPACE_BF_ANALYSIS_EMAIL != null && env.POLYSPACE_BF_ANALYSIS_EMAIL != '') {
        env.PS_CHECK = env.POLYSPACE_BF_ANALYSIS_EMAIL == 'ON' ? 'true' : 'false'
        println(
            "POLYSPACE_BF_ANALYSIS_EMAIL is defined, so email alerting depends if it is 'ON'")
    }
    // final decision told to the log file
    if (env.PS_CHECK == 'false') {
        println(
            'POLYSPACE_BF_ANALYSIS_EMAIL is not defined, so email alerting for BugFinder ' +
            "has been switched 'OFF'")
    }

    // Check for QA-C analysis to be monitored via the AlertDeveloper Tool : QA-C config
    if (isEmpty(env.QAC_ANALYSIS_EMAIL)) {
        env.QAC_CHECK = 'false'
    }
    else {
        env.QAC_CHECK = env.QAC_ANALYSIS_EMAIL == 'ON' ? 'true' : 'false'
    }

    if (env.QAC_CHECK == 'false') {
        println(
            "QAC_ANALYSIS_EMAIL is not defined or 'false', so no email alerting will " +
            'run on QAC. Default (VALUES: ON|OFF).')
    }

    // URLs checking
    if (isEmpty(env.ROOT_TAG_URL_G00500)) {
        env.ROOT_TAG_URL_G00500 = 'https://debesvn001/kostal/lk_ae_internal/LK/ToolsSD/' +
            'G00500_Ps_Bf_ExampleProject/trunk/PolyspaceNightly/'
        println('ROOT_TAG_G00500_URL is not defined.')
    }
    else {
        if (!env.ROOT_TAG_URL_G00500.endsWith('/')) {
            env.ROOT_TAG_URL_G00500 = env.ROOT_TAG_URL_G00500 + '/'
            println('ROOT_TAG_G00500_URL has no trailing slash.')
        }
    }

    if (isEmpty(env.REPOSITORY_URL)) {
        println('REPOSITORY_URL not defined.')
        throw new Exception(
            'REPOSITORY_URL not defined. This is necessary for checking out the project.')
    }
    else {
        if (!env.REPOSITORY_URL.endsWith('/')) {
            env.REPOSITORY_URL = env.REPOSITORY_URL + '/'
            println('REPOSITORY_URL has no trailing slash.')
        }
    }

    if (isEmpty(env.PORT_LICENSE)) {
        env.PORT_LICENSE = '27003'
    }

    if (isEmpty(env.SERVER_NAME_LICENSE)) {
        env.SERVER_NAME_LICENSE = 'DEBELUM002'
    }

    /*
    NG_WARNINGS_PLUGIN_DELTA : ON for using the plugin with delta threshold,
    OFF for using without delta threshold (it depends also on the existence of a
    last successful build to compare against it, see scanWarnings()
    */
    if (isEmpty(env.NG_WARNINGS_PLUGIN_DELTA)) {
        env.NG_WARNINGS_PLUGIN_DELTA == 'ON'
    }

    return env
}

/**
 * Returns the definition of environment variables for the execution of
 * Polyspace AlertDeveloper
 *
 * @param env : The current Jenkins environment.
 * @return env: new environment
 *
 * */
EnvActionImpl createEnvAlertDeveloper(EnvActionImpl env) {
    Utility utility = new Utility()
    String completePathToolsUserScripts = (
        env.WORKSPACE + '/' + env.CI_PROJECT + '/' +
            env.TOOLS_PATH + '/Polyspace/user_scripts')

    utility.deleteRecreateDir(completePathToolsUserScripts, true)

    utility.debugPrint(
        env, 'PolyspaceBugFinder:createEnvAlertDeveloper',
        'completePathToolsUserScripts: ' + completePathToolsUserScripts, '')
    return env
}

/**
 * @func createTempWorkingDirStruct( )
 * @description creates struct of temporary and working results, which are
 * saved as artifacts or stashed
 *
 * @param env : The current Jenkins environment.
 *
 * */
EnvActionImpl createTempWorkingDirStruct(EnvActionImpl env) {
    Utility utility = new Utility()

    String prefix = env.WORKSPACE + '/' + env.PROJECT_VARIANT_NAME
    utility.deleteRecreateDir(prefix + '/data/Polyspace/input/BugFinder', true)
    utility.deleteRecreateDir(prefix + '/data/Polyspace/output/BugFinder', true)
    utility.deleteRecreateDir(prefix + '/report/Polyspace/BugFinder', true)

    utility.debugPrint(
        env, 'PolyspaceBugFinder:createTempWorkingDirStruct',
        'createTempWorkingDirStruct successfully executed. ' +
        'All directories have been recreated.', '')
}

/**
 * Returns new Environment of PS_RESULT_DIR
 * only call this function in case the  PS_RESULT_DIR_FILE_SLASH is set and
 * the referenced file is available
 *
 * @param env : The current Jenkins environment.
 * @return env : new environment
 * */
@SuppressWarnings('CatchException')
EnvActionImpl setResultsDirEnv(EnvActionImpl env) {
    Utility utility = new Utility()

    Closure debugPrint = { String message ->
        utility.debugPrint(
            env, 'PolyspaceBugFinder:setResultsDirEnv', message, '')
    }

    try {
        utility.getHostName()

        FilePath resultsFile = utility.createFilePath(env.PS_RESULT_DIR_FILE_BSLASH)

        if (!resultsFile.exists()) {
            debugPrint(
                'setResultsDirEnv failed. No PS_RESULT_DIR ' +
                    '(JenkinsServerStack/ps_result_dir.txt) found.')
            return env
        }

        env.PS_RESULT_DIR = (
                env.AUTOSAR_PROJECT_LAYER == 'null' ?
                        '' : env.AUTOSAR_PROJECT_LAYER
        ) + '/' +
                readFile(env.PS_RESULT_DIR_FILE_BSLASH).replaceAll('\\n', '')
        // i.e. '.\\Variant\\RdW\\Target\\Release\\Polyspace\\BugFinder\\_result'

        env.PS_RESULT_DIR = env.PS_RESULT_DIR.replaceAll('/\\.', '')
        env.PS_RESULT_DIR = env.PS_RESULT_DIR.replaceAll('\\.\\.', '')

        // robustness : if target does not contain build target like i.e.
        // release: /Target/release */
        env.BUILD_TARGET_PPATH = 'Target/' + env.POLYSPACE_ANALYSIS_BUILD

        if (!utility.toLinuxPath(env.PS_RESULT_DIR).contains(env.BUILD_TARGET_PPATH)) {
            env.PS_RESULT_DIR = utility.toWindowsPath((
                env.PS_RESULT_DIR).replaceAll('Target', env.BUILD_TARGET_PPATH))
        }

        env.PS_RESULT_DIR_PS = utility.toLinuxPath(env.PS_RESULT_DIR)

        env.PS_RESULT_DIR_PS = env.PS_RESULT_DIR_PS.replaceAll('/+', '/')

        env.PS_RESULT_DIR = utility.toWindowsPath(env.PS_RESULT_DIR_PS)
        env.PS_RESULT_DIR_PS = env.PS_RESULT_DIR + '/Polyspace-Doc'

        env.completePathPS_RESULT_DIR = utility.toWindowsPath(
                env.WORKSPACE + '/' + CI_PROJECT + '/' + env.PS_RESULT_DIR)

        env.completePathPS_RESULT_DIR = utility.toLinuxPath(env.completePathPS_RESULT_DIR)

        env.completePathPS_RESULT_DIR =
                env.completePathPS_RESULT_DIR.replaceAll('/+', '/')
        env.completePathPS_RESULT_DIR = utility.toWindowsPath(env.completePathPS_RESULT_DIR)
    }
    catch (Exception e) {
        debugPrint(
            "Exception ${e.getClass()}: probably an error has occurred handling PS_RESULT_DIR " +
            '(JenkinsServerStack/ps_result_dir.txt. Does the file exist?')
        println Functions.printThrowable(e)
        throw (e)
    }

    return env
}

/**
 * Checkout project and build resources from template project G00500.
 * Necessary template files like polypspace.mk and polyspace_opt.mk or
 * AlertDeveloperConfig.xml
 *
 * @param env The current Jenkins environment. It must include the
 *            following parameters.
 *  <ul>
 *    <li>ROOT_TAG_URL_G00500 - The SVN URL of template project, where the
 *        instances for polyspace.mk and similar come from </li>
 *    <li>TOOLS_FOLDER_PATH - The location of the tools folder</li>
 *  </ul>
 * */
void checkoutAnalysisG00500ExProject(EnvActionImpl env) {
    Utility utility = new Utility()

    // check out Polyspace folder with the instance files for the analysis
    // like polyspace.mk, and the *.awk files
    // This defined list contains all the repository url's

    // Checking out all necessary sources and tools to run the project
    // (it's the normal stuff needed for a make all_rebuild plus Polyspace)
    new CheckoutTools().checkoutAll(
        Checkout.buildCheckout2(
            env, env.ROOT_TAG_URL_G00500, [
                utility.co(
                    env.TOOLS_PATH_G00500 + '/Polyspace',
                    env.TOOLS_FOLDER_PATH + '_G00500/Polyspace',
                    false),
                utility.co(
                    env.TOOLS_PATH_G00500 + '/Qac',
                    env.TOOLS_FOLDER_PATH + '_G00500/Qac',
                    false)
        ]))

    utility.debugPrint(
            env, 'PolyspaceBugFinder:checkoutAnalysis',
            'Checkout for the analysis part finished.', '')
}

/**
 * Checkout code related to notifying developers about issues discovered
 * using PolySpace.
 *
 * @param env The current Jenkins environment. It must include the
 *            following parameters.
 * <ul>
 *   <li>BRANCH_URL - The SVN URL of the source code branch under test</li>
 *   <li>ALERT_DEVELOPER_TOOL_PATH - The location of the AlertDeveloper Tool</li>
 * </ul>
 **/
boolean checkoutAlertDeveloperTool(EnvActionImpl env) {
    Utility utility = new Utility()
    new CheckoutTools().checkoutAll(
        Checkout.buildCheckout2(
            env, 'https://debesvn001.de.kostal.int/kostal/lk_ae_internal/LK/DistributePSDefects/' +
            'tags/20200228_trunk_IntegratedXMLConfig_v1.6/', [
                utility.co('Release', env.ALERT_DEVELOPER_TOOL_PATH, true)
    ]))
    return true  // This function can only return true: it should be void
}

/* creation of roundtrip data folder JenkinsServerStack */
// Not a builder method
@SuppressWarnings('BuilderMethodWithSideEffects')
@SuppressWarnings('CatchException')
void createJenkinsShareDirectory(EnvActionImpl env) {
    try {
        Utility utility = new Utility()
        utility.deleteRecreateDir(env.JENKINS_SHARE_BSLASH, true)
    }
    catch (Exception e) {
        utility.debugPrint(
            "Exception ${e.getClass()}: failed to create the JenkinsServerStack directory.")
        println Functions.printThrowable(e)
        throw e
    }
}

/**
 * Copy PolySpace results from the artifacts.
 *
 * @param env The current Jenkins environment. It must include the
 *            following parameters.
 * <ul>
 *   <li>JOB_NAME - Path of the of the project from Jenkins Home, used for
 *       round trip process to get the data of Polyspace metric server </li>
 * </ul>
 * */
@SuppressWarnings('CatchException')
void copyResultsFromArtifacts(EnvActionImpl env) {
    Utility utility = new Utility()
    String name = 'PolyspaceBugFinder:copyResultsFromArtifacts'

    try {
        try {
            new CommonTools().copyArtifacts(
                env['JOB_NAME'],
                env.ARCHIVE_INPUT_FOLDER + '/JenkinsServerStack/',
                env.JENKINS_SHARE)
        }
        catch (Exception eDF) {
            utility.debugPrint(
                "Exception ${eDF.getClass()}: CopyArtifacts has failed.")
            Functions.printThrowable(eDF)
        }
        utility.debugPrint('copyResults finished.')
    }
    catch (Exception eb) {
        utility.debugPrint(
            env, name,
            "Exception ${eb.getClass()}: copyArtifacts plugin: initial startup phase, " +
            'some or all copy artifacts are not available.', '')
        println Functions.printThrowable(eb)
    }
}

/**
 * @func : proveAndDeleteDir
 *
 * @desc: delete the folder if exists
 * @param completePath : String to the complete Path (D:\....),
 * preferrable in / writing
 * */
void proveAndDeleteDir(String completePath) {
    Utility utility = new Utility()
    dir(utility.toLinuxPath(completePath)) {
        deleteDir()
    }
}

/**
 * @func : copyTemplatesFromG00500
 *
 * @desc: copies the templates like make files and xml files and so on from
 * G00500 template project into the workspace
 *
 * @param env The current Jenkins environment. It must include the
 *            following parameters.
 * <ul>
 *   <li>TOOLS_FOLDER_PATH_BSLASH - Path to CI_PROJECT/<ToolsFolder>
 *       in backslash notation </li>
 * </ul>
 * */
void copyTemplatesFromG00500(EnvActionImpl env) {
    Utility utility = new Utility()
    String name = 'PolyspaceBugFinder:copyTemplatesFromG00500'

    String fullToolsPath = "${env.WORKSPACE}/${env.TOOLS_FOLDER_PATH}_G00500"
    String completeToolsPath = env.WORKSPACE + '/' + env.TOOLS_FOLDER_PATH

    FilePath toolsFolderG00500 = utility.createFilePath(utility.toWindowsPath(fullToolsPath))
    FilePath toolsFolder = utility.createFilePath(utility.toWindowsPath(completeToolsPath))

    FilePath alertDeveloperConfigXml = utility.createFilePath(
        completeToolsPath + '/Polyspace/AlertDeveloperConfig.xml')
    FilePath polyspaceProjOpt = utility.createFilePath(
        completeToolsPath +
        '/Polyspace/polyspace_project_opt.mk')

    if (toolsFolderG00500.exists() && toolsFolder.exists()) {
        env.DirOfPolyspaceG00500 = utility.toWindowsPath(fullToolsPath + '/Polyspace')

        // copying necessary template data from template project G00500
        // checked out in checkoutAnalysisG00500ExProject(env)

        if (polyspaceProjOpt.exists()) {
            utility.cp(
                completeToolsPath + '/Polyspace/polyspace_project_opt.mk',
                fullToolsPath + '/Polyspace/polyspace_project_opt.mk*',
                'REPLACE_EXISTING')
        }
        else {
            utility.debugPrint(
                env, name,
                'No project specific file exists.', '')
        }

        if (alertDeveloperConfigXml.exists()) {
            utility.cp(
                completeToolsPath + '/Polyspace/AlertDeveloperConfig.xml',
                fullToolsPath + '/Polyspace/AlertDeveloperConfig.xml*',
                'REPLACE_EXISTING')
        }
        else {
            // the command line as is expectedt in the function
            // executeDefectTracker() by now and so far
            utility.deleteFile(
                fullToolsPath + '/Polyspace/AlertDeveloperConfig.xml')
        }

        // the next line deletes the complete directory because its content
        // is really not needed, since we co
        utility.deleteRecreateDir(
            utility.toWindowsPath(completeToolsPath + '/Polyspace'), true)

        utility.cp(
            fullToolsPath + '/Polyspace', completeToolsPath + '/Polyspace/',
            'REPLACE_EXISTING,RECURSIVE')
        utility.cp(
            fullToolsPath + '/Qac/output_qac_file_list.mk',
            completeToolsPath + '/Qac/output_qac_file_list.mk*',
            'REPLACE_EXISTING')

        proveAndDeleteDir(fullToolsPath)

        // delete also .svn folder of G00500 so that the leading project
        // won't have a problem during next checkout (mixed svns), but it is
        // a different layer (the project's .svn-folder is one layer up)
        /*        utility.debugPrint(
                    env, name, 'delete G00500 .svn folder  = ',
                    completeToolsPath.replaceAll('/' + env.TOOLS_PATH, '/.svn'))
                proveAndDeleteDir(
                    completeToolsPath.replaceAll('/' + env.TOOLS_PATH, '/.svn'))*/

        env.DirOfProject = utility.toWindowsPath(
                completeToolsPath.replaceAll('/' + env.TOOLS_PATH, ''))
    }
    else {
        utility.debugPrint(
            env, name, 'Folder ' + utility.toWindowsPath(completeToolsPath) + ' or ' +
            utility.toWindowsPath(fullToolsPath) + ' do not exist.', '')
    }

    env.DirOfPolyspace = utility.toWindowsPath(completeToolsPath + '/Polyspace')
}

/**
 * @func : createPsResultDirFile
 *
 * @desc: creates the ps result dir file with the correct path to the result
 * (based on GnuMakeFile in JenkinsServerStack folder
 *
 * @param env The current Jenkins environment. It must include the
 *            following parameters.
 * <ul>
 *   <li>TOOLS_FOLDER_PATH  - Path to CI_PROJECT/&lt;ToolsFolder&gt;</li>
 *   <li>PS_RESULT_DIR_FILE_BSLASH - Path to ResultDir file</li>
 * </ul>
 * */
@SuppressWarnings('BuilderMethodWithSideEffects')
// Not a builder method
void createPsResultDirFile(EnvActionImpl env) {
    Utility utility = new Utility()

    FilePath psResultsFile = utility.createFilePath(env.PS_RESULT_DIR_FILE_BSLASH)

    if (psResultsFile.exists()) {
        print2console = 'env.PS_RESULT_DIR_FILE_BSLASH = ' +
            env.PS_RESULT_DIR_FILE_BSLASH + ' already exists.'
    }
    else {
        // the next batch file will create the necessary file for the roundtrip
        String psResultDirFileBatch = CommandBuilder.buildBat([
            'cd %TOOLS_FOLDER_PATH%/%MAKE_TOOL_PATH%',
            '%WORKSPACE%\\%TOOLS_FOLDER_PATH_BSLASH%\\make\\%MAKE_TOOL% ^',
            "${env.MAKE_VAR} ^",
            'create_ps_result_dir_file ^',
            "${env.ADD_LAST_PARAM}"
        ])

        // conversion to style of msys64 just for the Makefile
        // environment (/D/ instead of D:\ */
        env.PS_RESULT_DIR_FILE_BACKUP = env.PS_RESULT_DIR_FILE
        env.PS_RESULT_DIR_FILE = '/' + env.PS_RESULT_DIR_FILE.replaceAll(':', '')
        bat psResultDirFileBatch

        // reconversion back to style of groovy with slashes
        // (D:/ instead of /D/ instead of D:\
        env.PS_RESULT_DIR_FILE = env.PS_RESULT_DIR_FILE_BACKUP
        print2console = 'env.PS_RESULT_DIR_FILE_BSLASH = ' +
                env.PS_RESULT_DIR_FILE_BSLASH
    }
}

/**
 * @func: createResultDirs*
 * @desc: create folders for generated files, old result and new result for a
 * fresh run
 *
 * @param env The current Jenkins environment. It must include the
 *            following parameters.
 * <ul>
 *   <li>PS_RESULT_DIR - Path to ResultDir file</li>
 * </ul>
 * */
@SuppressWarnings('BuilderMethodWithSideEffects')
// Not a builder method
void createResultDirs(EnvActionImpl env) {
    Utility utility = new Utility()

    String completeToolsPath = utility.toLinuxPath(
        env.WORKSPACE + '/' + env.TOOLS_FOLDER_PATH_BSLASH)

    String completePathTargetRelease = utility.toLinuxPath(
        env.completePathPS_RESULT_DIR
    ).replaceAll('/Polyspace/BugFinder/_result', '')

    FilePath targetReleaseFolder = utility.createFilePath(completePathTargetRelease)

    if (!targetReleaseFolder.exists()) {
        utility.createDir(completePathTargetRelease)
    }
    if ( env.DO_NOT_DELETE_RESULTSDIR == 'false') {
        utility.deleteRecreateDir(env.completePathPS_RESULT_DIR, true)
        utility.deleteRecreateDir(env.completePathPS_RESULT_DIR + '/../_gen', true)
        utility.deleteRecreateDir(
            env.completePathPS_RESULT_DIR + '/../_old_result', true)
        utility.deleteRecreateDir(completeToolsPath + '/Polyspace/user_scripts', true)
    }
}

/**
 * @func : deleteResultFolder
 *
 * @desc: delete the whole result folder for a fresh start: info about
 * roundtrip comes as artifact to JenkinsServerStack, old_results come from
 * metric server, new results are calulated by Polyspace Bugfinder
 *
 * @param env The current Jenkins environment. It must include the
 *            following parameters.
 * <ul>
 *   <li>completePathPS_RESULT_DIR - Path to ResultDir file</li>
 * </ul>
 * */
void deleteResultFolder(EnvActionImpl env) {
    Utility utility = new Utility()
    utility.debugPrint(
        env, 'PolyspaceBugFinder:deleteResultFolder',
        'env.completePathPS_RESULT_DIR = ' + env.completePathPS_RESULT_DIR, '')

    proveAndDeleteDir(utility.toLinuxPath(env.completePathPS_RESULT_DIR))
}

/**
 * @func : createPsProjectVariantNameFile
 *
 * @desc: creates file with the name of the project in JenkinsServerStack folder
 *
 * @param env The current Jenkins environment. It must include the
 *            following parameters.
 * <ul>
 *   <li>PS_PROJECT_VARIANT_NAME_FILE - Path to
 *   CI_PROJECT/&lt;JenkinsServerStack>&gt;ps_ProjectVariantName.txt </li>
 * </ul>
 * */
@SuppressWarnings('BuilderMethodWithSideEffects')
// Not a builder method
void createPsProjectVariantNameFile(EnvActionImpl env) {
    Utility utility = new Utility()

    FilePath psProjectVariantNameFile = utility.createFilePath(
            env.PS_PROJECT_VARIANT_NAME_FILE)

    if (psProjectVariantNameFile.exists()) {
        return
    }

    if (env.PROJECT_VARIANT_NAME != null) {
        if (env.PROJECT_VARIANT_NAME != '') {
            writeFile([
                file: env.PS_PROJECT_VARIANT_NAME_FILE.toString(),
                text: env.PROJECT_VARIANT_NAME.toString()
            ])
        }
        else {
            utility.debugPrint(
                env, 'PolyspaceBugFinder',
                'createPsProjectVariantNameFile',
                'Creation of' + env.PS_PROJECT_VARIANT_NAME_FILE + ' file. ' +
                'PROJECT_VARIANT_NAME is not set. Please set it ' +
                'accordingly to the name of the project.',
                'execute')
        }
    }
    else {
        utility.debugPrint(
            env, 'PolyspaceBugFinder', 'createPsProjectVariantNameFile',
            'Setting PROJECT_VARIANT_NAME according to ' +
            env.PS_PROJECT_VARIANT_NAME_FILE + ' file. ' +
            'PROJECT_VARIANT_NAME is not set. Please set it ' +
            'accordingly to the name of the project.',
            'execute')

        env.PROJECT_VARIANT_NAME = readFile(
            env.PS_PROJECT_VARIANT_NAME_FILE).replaceAll('\\n', '')
    }
}
/**
 * Define Polyspace Executables path
**/
void setExecutablePaths(Object env) {
    env.POLYSPACE_DIR           = env.CI_POLYSPACE
    env.POLYSPACE_BF            = "${env.CI_POLYSPACE}\\bin\\polyspace-bug-finder-nodesktop.exe"
    env.POLYSPACE_BF_IMPORT     = "${env.CI_POLYSPACE}\\bin\\polyspace-comments-import.exe"
    env.POLYSPACE_RG            = "${env.CI_POLYSPACE}\\bin\\polyspace-report-generator.exe"
    env.POLYSPACE_REPOSITORY    = "${env.CI_POLYSPACE}\\bin\\polyspace-results-repository.exe"
}

/**
 * Set environment variables needed for Polyspace toolchain.
**/
void setVariables(Object env) {
    Date now = new Date()
    String revision

    dir(env.WORKSPACE + "\\${env.TOOLS_FOLDER_PATH}") {
        revision = bat(script: 'svn info | findstr \"Revision\"', returnStdout: true)
    }
    revision = revision.split('\n')[2].split(':')[1].trim()
    env.POLYSPACE_TIMESTAMP   = now.format('yyyyMMdd', TimeZone.getTimeZone('UTC'))
    env.BUILD_ID              = currentBuild.getRawBuild().getId()
    env.SVN_REVISION          = revision
    env.VERSION               = "${env.POLYSPACE_TIMESTAMP}_BuildId_${env.BUILD_ID}"
    env.TARGET_DIR            = "${env.CI_PROJECT}\\${env.TARGET_PATH}\\${env.POLYSPACE_ANALYSIS_BUILD}"
    env.PS_GEN_DIR            = "${env.TARGET_DIR}\\Polyspace\\BugFinder\\_gen"
    env.PS_RESULT_DIR         = "${env.TARGET_DIR}\\Polyspace\\BugFinder\\_result"
    env.PS_OLD_RESULT_DIR     = "${env.TARGET_DIR}\\Polyspace\\BugFinder\\_old_result"
    env.PS_OPTIONS            = "\
                        -prog ${env.PROJECT_VARIANT_NAME} \
                        -verif-version ${env.VERSION} \
                        -author ${env.USERNAME} \
                        -results-dir ${env.PS_RESULT_DIR}"
}

/**
 * Function that build make target.
 *
 * @param target Make target to be build
**/
String buildTarget(String target) {
    return CommandBuilder.buildBat([
        'cd %TOOLS_FOLDER_PATH%/%MAKE_TOOL_PATH%',
        '@%WORKSPACE%\\%TOOLS_FOLDER_PATH_BSLASH%\\make\\%MAKE_TOOL% ^',
        "${env.MAKE_VAR} ^",
        "${target} ^",
        "${env.ADD_LAST_PARAM}"
    ])
}

/**
 * Sends information about the last successful results of the job to BACU.
**/
void sendDataToBacu() {
    int timeout = 1000
    Map<String, String> requestData = [:]
    requestData['job'] = "${env.PROJECT_VARIANT_NAME}"
    requestData['serverName'] = "${env.PS_SERVER}"
    requestData['buildId'] = "${env.VERSION}"
    String jsonBody = new JsonBuilder(requestData)
    String url = 'https://bacu.de.kostal.int/api/v1/polyspace/bugfinder/job/'

    HttpURLConnection httpConnection = new URL(url).openConnection()
    httpConnection.setRequestMethod('POST')
    httpConnection.setConnectTimeout(timeout)
    httpConnection.setDoOutput(true)
    httpConnection.setRequestProperty('Content-Type', 'application/json')
    httpConnection.getOutputStream().write(jsonBody.getBytes('UTF-8'))
    int statusCode = httpConnection.getResponseCode()
    String responseText = httpConnection.getInputStream().getText()
    println statusCode + ':' + responseText
}

/**
 * Use BACU to check the identifier of the last PolySpace build
 *
 * @param jobName The name of the job to check for
**/
String getPreviousBuildId(String jobName) {
    String url = "https://bacu.de.kostal.int/api/v1/polyspace/bugfinder/job/${jobName}/"
    Object response = httpRequest url
    Object data = new JsonSlurper().parseText(response.content)
    return data.buildId
}

/**
 * Use BACU to check what server was used for the last PolySpace build
 *
 * @param jobName The name of the job to check for
**/
String getPreviousBuildServerName(String jobName) {
    String url = "https://bacu.de.kostal.int/api/v1/polyspace/bugfinder/job/${jobName}/"
    Object response = httpRequest url
    Object data = new JsonSlurper().parseText(response.content)
    return data.serverName
}

/**
 * Build target preparelists for Polyspace
**/
void prepareLists() {
    bat buildTarget('preparelists')
}

/**
 * QAC output file list
**/
void qacOutputList() {
    bat buildTarget('qac_output_file_list')
}

/**
 * Generate configuration for polyspace: Directory structure and Polyspace list
**/
void generateConfiguration() {
    bat buildTarget('gen_cfg_polyspace')
}

/**
 * Download results from Metric server
**/
void downloadResults() {
    try {
        String buildId = getPreviousBuildId(env.PROJECT_VARIANT_NAME)
        String server = getPreviousBuildServerName(env.PROJECT_VARIANT_NAME)

        if (server.contains('release')) {
            server += ':9191'
        } else {
            server += ':9090'
        }

        bat CommandBuilder.buildBat([
            "${env.POLYSPACE_REPOSITORY} -download -product \"BugFinder\" -integration -server ${server} \
            -f -prog ${env.PROJECT_VARIANT_NAME} -verif-version ${buildId} ${env.PS_OLD_RESULT_DIR}\\"])
    } catch (java.lang.IllegalStateException e) {
        ansiColor('xterm') {
            echo(
                '[32mThere is no available information about previous build. ' +
                'Probably this is the first build of the job or there is not any successful builds.[0m')
        }
    }
}

/**
 * Execute Polyspace analysis
**/
void executePolyspace() {
    bat buildTarget('polyspace')
}

/**
 * Generates CSV report from Polyspace
**/
void generateCsvRepot() {
    bat CommandBuilder.buildBat([
        "${env.POLYSPACE_RG} -results-dir ${env.WORKSPACE}\\${env.PS_RESULT_DIR} -generate-results-list-file"])
}

/**
 * Filters out foreign code
**/
void filterOutForeignCode() {
    bat buildTarget('filter_out_foreign_code_and_justifications')
}

/**
 * Upload results to metric server
**/
void uploadResults() {
    bat CommandBuilder.buildBat([
        "${env.POLYSPACE_REPOSITORY} -server ${env.PS_SERVER}:${env.PS_METRIC_SERVER_PORT} \
        -upload -f -prog ${env.PROJECT_VARIANT_NAME} -integration -verif-version \
        ${env.VERSION} ${env.PS_RESULT_DIR}\\"])

    sendDataToBacu()
}

/**
 * Create temporary files
**/
void createTmpFiles() {
    bat buildTarget('create_temporary_files')
}

/**
 * execute PolySpace analysis
 *
 * @param env The current Jenkins environment. It must include the
 *            following parameters.
 * <ul>
 *   <li>PROJECT_VARIANT_NAME     - the name of the current project being
 *                                  analysed</li>
 *   <li>SVN_REVISION             - the SVN revision of the project</li>
 *   <li>JENKINS_SHARE_BSLASH     - the folder where all round trip data files
 *                                  is stored</li>
 *   <li>WORKSPACE                - The path to the Jenkins workspace</li>
 *   <li>PS_RESULT_DIR            - The Polyspace results path</li>
 *   <li>TOOLS_FOLDER_PATH        - The location of the tools folder</li>
 *   <li>POLYSPACE_ANALYSIS_BUILD - The build variant</li>
 *   <li>TARGET                   - Build target</li>
 *   <li>ADDPARAM                 - Additional build parameters</li>
 * </ul>
 **/
@SuppressWarnings('CatchException')
void executePsAnalysis(EnvActionImpl env) {
    Utility utility = new Utility()
    Closure debugPrint = { String message ->
        utility.debugPrint(
            env, 'PolyspaceBugFinder:executePsAnalysis', message, '')
    }

    if (env.PS_SERVER.contains('release')) {
        ansiColor('xterm') {
            echo(
                '[34mYou are using the metric server for RELEASE builds; ' +
                'results will be kept indefinitely. You can find them at ' +
                'https://release.metrics.polyspace.de.kostal.int.' +
                'To use the regular builds server instead, unset the ' +
                'RELEASE_BUILD environment variable.[0m')
        }
    } else {
        ansiColor('xterm') {
            echo(
                '[32mYou are using the metric server for non-release ' +
                'builds. It only keeps results for the last thirty builds of ' +
                'each project. You can find the results at ' +
                'https://metrics.polyspace.de.kostal.int. If you want to ' +
                'use the release metrics server you can set an environment ' +
                'variable RELEASE_BUILD=true ' +
                'from the job configuration page.[0m')
        }
    }

    try {
        env.PS_RESULT_DIR_FILE_BACKUP = env.PS_RESULT_DIR_FILE
        env.PS_RESULT_DIR_FILE = '/' + env.PS_RESULT_DIR_FILE.replaceAll('\\:', '')
        env.PS_PROJECT_VARIANT_NAME_FILE_BACKUP = env.PS_PROJECT_VARIANT_NAME_FILE
        env.PS_PROJECT_VARIANT_NAME_FILE =
                '/' + env.PS_PROJECT_VARIANT_NAME_FILE.replaceAll('\\:', '')

        setExecutablePaths(env)
        setVariables(env)

        prepareLists()
        qacOutputList()
        generateConfiguration()
        downloadResults()
        executePolyspace()
        generateCsvRepot()
        filterOutForeignCode()
        uploadResults()
        createTmpFiles()

        env.PS_RESULT_DIR_FILE = env.PS_RESULT_DIR_FILE_BACKUP
        env.PS_PROJECT_VARIANT_NAME_FILE = env.PS_PROJECT_VARIANT_NAME_FILE_BACKUP

        startGeneratingCSVReportInErrorCase(env)
    }
    catch (Exception e) {
        debugPrint(
            'executePsAnalysis somehow failed. Check dirs and Environment ' +
            'variables like TOOLS_FOLDER_PATH and so on.')
        println Functions.printThrowable(e)
        throw e
    }
}

/**
 * Gets Source Code Location of the executed library (this)
 *
 * Encapsuled because of malicious @NonCPS annotation which changes the
 * behaviour of the functions in a non acceptable way which is needed because
 * otherwise it will throw an Serializable Exception (because Jenkins nodes do
 * not implement them)
 */
@NonCPS
void getSourceCodeLocationOfLibrary() {
    Utility utility = new Utility()

    @SourceURI
    URI sourceUri
    Path scriptLocation = Paths.get(sourceUri)

    utility.debugPrint(
        env, 'PolyspaceBugFinder:getSourceCodeLocationOfLibrary',
        'scriptLocation Directory is:' + scriptLocation.toString(), '')
}

/**
 * @func: handleFirstRowMod2UserAssignmentArgument function
 * @desc: handle of FirstRowMod2UserAssignment argument
 *
 * FirstRowMod2UserAssignment is a Jenkins property environment variable,
 * where you can define the starting row of the assignment user to module
 * in the GEN_ModulesList.xlsm
 *
 * @param env The current Jenkins environment. It must include the
 *            following parameters.
 * <ul>
 *   <li>env - environment of Jenkins variables</li>
 * </ul>
 */
void handleFirstRowMod2UserAssignmentArgument(EnvActionImpl env) {
    boolean firstRowMod2UserAssignmentFlag = false

    if (env.FirstRowMod2UserAssignment != null) {
        if (env.FirstRowMod2UserAssignment == '') {
            firstRowMod2UserAssignmentFlag = true
        }
    }
    else {
        firstRowMod2UserAssignmentFlag = true
    }
    if (firstRowMod2UserAssignmentFlag) {
        env.FirstRowMod2UserAssignment = '0'
    }
}

/**
 * @func: handleModuleAssignmentListArgument function
 * @desc: handle of MODULE_ASSIGNMENT_LIST argument, checks out Module
 * assigment list to software developers (basis for Ps defect assignment
 * to the related developer)
 *
 * MODULE_ASSIGNMENT_LIST is a Jenkins property environment variable,
 * where you can define the name of the assignment list like i.e.
 * GEN_ModulesList.xlsm
 *
 * @param env The current Jenkins environment. It must include the
 *            following parameters.
 * <ul>
 *  <li>env     - environment of Jenkins variables</li>
 *  <li>DOC_CDB - Module list document ID from CDB</li>
 * </ul>
 */
void handleModuleAssignmentListArgument(EnvActionImpl env) {
    boolean modulesListFlag = false
    CheckoutTools checkoutTools = new CheckoutTools()

    if (env.MODULE_ASSIGNMENT_LIST != null) {
        if (env.MODULE_ASSIGNMENT_LIST == '') {
            modulesListFlag = true
        }
    }
    else {
        modulesListFlag = true
    }

    if (modulesListFlag) {
        env.MODULE_ASSIGNMENT_LIST = 'GEN_ModulesList.xlsm'
    }

    // finally checkout assignment excel document from cdb into workspace
    checkoutTools.checkoutAll(
        Checkout.buildCdb([[
            env.DOC_CDB,
            'latest-released',
            env.TOOLS_FOLDER_PATH + '/Polyspace',
            env.MODULE_ASSIGNMENT_LIST
        ]], false)
    )
}

/**
 * @func: convertReport2CSV function
 * @desc: converts tab delimited and lf separated table text file to a semicolon and CRLF separated
 *        "german" CSV
 *
 * Here the analysis is prepared to be assigned and emailed to the
 * related developer
 *
 * @param env The current Jenkins environment. It must include the
 *            following parameters.
 */
@SuppressWarnings('EmptyMethod')
@SuppressWarnings('UnusedMethodParameter')
void convertReport2CSV(EnvActionImpl env) {
    /* will be filled with a tool of converting a txt file to a non SYLK csv file according to
     * German Standard
     * */
}

/**
 * @func: isDirEmptyResultsCSVDir function
 * @desc: proves if Directory ${env.PS_RESULT_DIR}/Polyspace-Doc/ contains a file like
 *        ResultsBL_List_annotationfilter.txt
 *
 * @param env The current Jenkins environment. It must include the
 *            following parameters.
 * @return false, if it does not contain ResultsBL_List_annotationfilter.txt, otherwise true
 */
@SuppressWarnings('ExplicitArrayListInstantiation')
@SuppressWarnings('CatchException')
boolean isDirEmptyResultsCSVDir(EnvActionImpl env) {
    Utility utility = new Utility()
    boolean containsResultsBLListAnnotation = false
    String name = 'PolyspaceBugFinder:isDirEmptyResultsCSVDir'
    List<String> stdout = new ArrayList<String>()

    try {
        String result = 'ResultsBL_List_annotationfilter.txt found'
        String ifCSVReport = CommandBuilder.buildBat([
            "@if exist ${env.completePathPS_RESULT_DIR}" +
            '\\Polyspace-Doc\\ResultsBL_List_annotationfilter.txt ^',
            "echo ${result}"
        ])
        stdout = Arrays.asList(bat(script: ifCSVReport, returnStdout: true))

        println('stdout ----')
        int i = 0
        stdout.each { a -> println 'Line ' + ( i++ ) + ' : ' + a }
        println('stdout ----')

        if (stdout.size() == 1) {
            result = stdout.get(0)
        }
        else {
            result = ''
        }

        println('result ----------------- ' + result)
        if (result.length() == 0) {
            containsResultsBLListAnnotation = true
        }
    }
    catch (Exception eb) {
        utility.debugPrint(
            env, name,
            'Exception ' +  eb.getClass() + ' isDirEmpty fails somehow, ' +
            'script error?', '')
        println '---------'
        stdout.each { a -> println a }
        println Functions.printThrowable(eb)
    }
    return containsResultsBLListAnnotation
}

/**
 * @func: startGeneratingCSVReportInErrorCase function
 * @desc: generate CSV report
 *
 * Here the analysis is prepared to be assigned and emailed to the
 * related developer
 *
 * @param env The current Jenkins environment. It must include the
 *            following parameters.
 */
@SuppressWarnings('ThrowRuntimeException')
void startGeneratingCSVReportInErrorCase(EnvActionImpl env) {
    Utility utility = new Utility()

    // Check if ResultsBL_List_annotationfilter exists
    if (!isDirEmptyResultsCSVDir(env)) {
        return
    }

    println('ResultsBL_List_annotationfilter.txt does not exist; try to create it')
    utility.deleteRecreateDir(
        utility.toLinuxPath(env.completePathPS_RESULT_DIR) + '/Polyspace-Doc')

    bat executeCreateCSVReport = CommandBuilder.buildBat([
        'cd %TOOLS_FOLDER_PATH%/%MAKE_TOOL_PATH%',
        '%WORKSPACE%\\%TOOLS_FOLDER_PATH_BSLASH%\\make\\%MAKE_TOOL% ^',
        "${env.MAKE_VAR} ^",
        'gen_polyspace_csv ^',
        env.ADD_LAST_PARAM
    ])

    bat CommandBuilder.buildBat([
        'cd %TOOLS_FOLDER_PATH%/%MAKE_TOOL_PATH%',
        '%WORKSPACE%\\%TOOLS_FOLDER_PATH_BSLASH%\\make\\%MAKE_TOOL% ^',
        "${env.MAKE_VAR} ^",
        'filter_out_foreign_code_and_justifications ^',
        env.ADD_LAST_PARAM
    ])

    if (isDirEmptyResultsCSVDir(env)) {
        println('Creating CSV report files for the second time; potential license issues.')
        throw new RuntimeException('Creating CSV Report failed repeatedly.')
    }
}

/**
 * @func: executeDefectTracker function
 * @desc: Polyspace analysis report
 *
 * Here the analysis is prepared to be assigned and emailed to the
 * related developer
 *
 * @param env The current Jenkins environment. It must include the
 *            following parameters.
 * <ul>
 *  <li>TOOLS_FOLDER_PATH_BSLASH - The location of the tools folder in
 *      backslash notation</li>
 *  <li>WORKSPACE_PROJECT_BSLASH - The workspace directory in backslash
 *      notation</li>
 *  <li>ALERT_DEVELOPER_TOOL_PATH_BSLASH - path to AlertDeveloper tool to
 *      generate the assignment binding between the excel file
 *      (moduel2user binding) and the defects list</li>
 *  <li>PS_SERVER_METRICS - the http address of the metric server where
 *      finally the results are stored </li>
 * </ul>
 */
@SuppressWarnings('CatchException')
void executeDefectTracker(EnvActionImpl env) {
    Utility utility = new Utility()
    String name = 'PolyspaceBugFinder:executeDefectTracker'

    try {
        // next call was only for debug purposes ; it serves a println of the
        // location of this libraries absolute Path
        getSourceCodeLocationOfLibrary()

        handleFirstRowMod2UserAssignmentArgument(env)
        handleModuleAssignmentListArgument(env)

        CustomerArtifactsPolyspace.chooseMetricServer(env)

        BacuRequest bacuRequest = new BacuRequest(env.PROJECT_VARIANT_NAME)
        BuildInformation buildInformation = bacuRequest.populateBuildData()

        String alertDeveloperBatch = CommandBuilder.buildBat([
            "@set /p LAST_ID_BUILT=<${env.WORKSPACE_BSLASH}\\" +
                    "${env.JENKINS_SHARE_ARCHIVE}/ps_last_version_id_built.txt",

            '@echo PS_RESULT_DIR=%PS_RESULT_DIR%',
            "@echo WORKSPACE_PROJECT=${env.WORKSPACE_PROJECT}",
            '@echo WORKSPACE_PROJECT_BSLASH=%WORKSPACE_PROJECT_BSLASH%',
            '@echo working_path = %cd%',
            '@echo PS_PROJECT_VARIANT_NAME_FILE : ' +
                    "${env.JENKINS_SHARE}/ps_ProjectVariantName.txt",
            'echo End of Debug.',
            'echo ALERT_DEVELOPER_TOOL_PATH_BSLASH=' +
                    env.ALERT_DEVELOPER_TOOL_PATH_BSLASH,

            "\"${env.ALERT_DEVELOPER_TOOL_PATH_BSLASH}\\AlertDeveloper.exe\" ^",
            "ProjectName=\"${env.PROJECT_VARIANT_NAME}\" ^",
            "Subversion=\"${buildInformation.getBuildId()}\" ^",
            "Gen_ModuleListPath=\"${env.TOOLS_FOLDER_PATH_BSLASH}\\Polyspace\" ^",
            "SheetName=\"${env.SHEETNAME}\" ^",
            "ModuleAssignmentList=\"${env.MODULE_ASSIGNMENT_LIST}\" ^",
            "resultBL_annotationPath=\".\\${CI_PROJECT}\\${env.PS_RESULT_DIR_PS}\" ^",
            'ResultBL=\"ResultsBL_List.txt\" ^',
            "QAC_outputPath=\"${env.QAC_outputPath_BSLASH}\" ^",
            "QAC_outputFile=\"${env.QAC_outputFile}\" ^",
            "SVN_URL=\"${env.BRANCH_URL}\" ^",
            "JWorkspace=\"${env.WORKSPACE_PROJECT_BSLASH}\" ^",
            "Integrator=\"${env.INTEGRATOR}\" ^",
            "PolyspaceServer=\"${env.PS_SERVER}:${env.PS_METRIC_SERVER_PORT}\" ^",
            "PS_check=\"${env.PS_CHECK}\" ^",
            "QAC_check=\"${env.QAC_CHECK}\" ^",
            "MISRA_vers=\"${env.MISRA_vers}\" ^",
            "ResponsibleColumn=\"${env.ResponsibleColumn}\" ^",
            "SoftwareModulesColumn=\"${env.SoftwareModulesColumn}\" ^",
            "oldCompatibleFlag=\"${env.oldCompatibleFlag}\" ",
            "for %%a in (${env.INTEGRATOR}) do " +
                    "(stcopy .\\${env.ALERT_DEVELOPER_TOOL_PATH}\\log.log " +
                    ".\\${env.TOOLS_FOLDER_PATH_BSLASH}\\Polyspace\\" +
                    'user_scripts\\%%a.log.log)'
        ])
        bat alertDeveloperBatch
    }
    catch (Exception e) {
        utility.debugPrint(
        env, name,
        'executeDefectTracker somehow failed. Check dirs and ' +
            'Environment variables like WORKSPACE_PROJECT_BSLASH, ' +
            'PS_RESULT_DIR_PS, NOTIFY_IN_BCC and NOTIFIY_IN_BCC_FLAG.', '')
        println Functions.printThrowable(e)
        throw e
    }
}

/**
 * getLastsuccessfulRun checks for compiler warnings.
 *
 * @param env The current Jenkins environment. It must include the
 *            following parameters.
 * @return boolean : true if there is a successful predecessor run
 *                   false if there is no successful predecessor run
 * */
boolean getLastSuccessfulRun() {
    Utility utility = new Utility()
    WorkflowRun lastsuccessfulRunt = currentBuild.getRawBuild().getPreviousSuccessfulBuild()
    if (lastsuccessfulRunt != null) {
        utility.debugPrint(
            'Last successful build ID is ' + lastsuccessfulRunt.getId())
        return true
    }
    utility.debugPrint('No prior build has succeeded.')
    return false
}

/**
 * scanWarnings checks for compiler warnings.
 *
 * @param env The current Jenkins environment. It must include the
 *            following parameters.
 * <ul>
 *   <li>CI_PROJECT    - The path to the project folder like 'SWF_Project'</li>
 *   <li>PS_RESULT_DIR - The rest path to the project polyspace folder like
 *       APPL_C/Target/release/Polyspace/_result'</li>
 * </ul>
 * */
@SuppressWarnings('CatchException')
void scanWarnings(EnvActionImpl env) {
    Utility utility = new Utility()
    try {
        String patternPath = (
            utility.toLinuxPath(env.PS_RESULT_DIR) +
            '/Polyspace-Doc/ResultsBL_List_annotationfilter.txt'
        ).replaceAll('/+', '/')

        String parserName = 'LK_Polyspace_Bf'
        String encoding = 'UTF-8'

        /** Use threshold only if there is a successful predecessor job id
         *  Reason: threshold is otherwise compared to zero and therefore unstable
         **/
        if (getLastSuccessfulRun() && env.NG_WARNINGS_PLUGIN_DELTA == 'ON') {
            /* use threshold warnings capability of plugin */
            new CITools().checkForCompilerWarnings(patternPath, parserName, encoding,
                    Integer.parseInt(env.THRESHOLD_DELTA_DEFECTS), 'DELTA', false)
        }
        else {
            /* use new plain plugin without threshold capability */
            new CITools().checkForCompilerWarnings(patternPath, parserName, encoding)
        }

        if (env.NOTIFY_IN_BCC_FLAG == 'true') {
            step([
                $class                  : 'Mailer',
                notifyEveryUnstableBuild: true,
                recipients              : env.NOTIFY_IN_BCC,
                sendToIndividuals       : true
            ])
        }
    }
    catch (Exception e) {
        utility.debugPrint(
            env, 'PolyspaceBugfinder:scanWarnings',
            'WarningsPublisher Plugin somehow failed.',
            'Check dirs and Environment variables like CI_PROJECT, ' +
                'PS_RESULT_DIR, NOTIFY_IN_BCC and NOTIFIY_IN_BCC_FLAG.')
        println Functions.printThrowable(e)
    }
}

/**
 * Send email notifications to developers.
 *
 * @param env The current Jenkins environment. It must include the
 *            following parameters.
 * <ul>
 *   <li>WORKSPACE         - The path to the Jenkins workspace</li>
 *   <li>TOOLS_FOLDER_PATH - The location of the tools folder</li>
 *   <li>NOTIFY_IN_BCC     - A list of email addresses to send blind
 *      copies to</li>
 *   <li>NOTIFY_BCC_ONLY_FLAG - Set to true to only send the emails
 *      to the BCC list</li>
 * </ul>
 * */
void notifyDevelopers(EnvActionImpl env) {
    String path = (
        "${env.WORKSPACE}/${env.CI_PROJECT}/${env.TOOLS_PATH}/" +
            'Polyspace/user_scripts/emailsubjectbody.json'
    )

    String emailConfiguration = readFile path
    DefectNotifier notifier = new DefectNotifier()
    Object configuration = notifier.buildConfiguration(emailConfiguration)

    boolean notifyOnlyBcc = true

    if (env.NOTIFY_BCC_ONLY_FLAG == 'false') {
        notifyOnlyBcc = false
    }
    else {
        notifyOnlyBcc = true
    }
    notifier.notifyDevelopers(configuration, env.NOTIFY_IN_BCC, notifyOnlyBcc)
}

/**
 * publishHtml publishes results.
 *
 * @param env The current Jenkins environment. It must include the
 *            following parameters.
 * <ul>
 *   <li>ARCHIVE_INPUT_FOLDER - The path to the polyspace_report.html
 *       to publish results</li>
 * </ul>
 * */
void publishHtml(EnvActionImpl env) {
    Utility utility = new Utility()
    utility.getHostName()
    publishHTML([
        allowMissing: false,
        alwaysLinkToLastBuild: true,
        keepAll: true,
        reportDir: env.ARCHIVE_INPUT_FOLDER + '/JenkinsServerStack',
        reportFiles: 'polyspace_report.html',
        reportName: 'Project on Polyspace Metric Server'
    ])
}

/**
 * swfPublishHTML publishes results in HTML page.
 *
 * @param env The current Jenkins environment. It must include the
 *            following parameters.
 * <ul>
 *   <li>ARCHIVE_INPUT_FOLDER - The path to the polyspace_report.html
 *       to publish results</li>
 * </ul>
 * */
void swfPublishHTML(EnvActionImpl env) {
    String reportsDirectory = env.ARCHIVE_INPUT_FOLDER + '/JenkinsServerStack'

    publishHTML(
        reportDir: reportsDirectory,
        reportFiles: 'polyspace_report.html',
        reportName: "SWF Reports ${env.BUILD_NUMBER}",
        reportTitles: 'Polyspace Analysis',
        allowMissing: true)
}

/**
 * run_analysis runs a complete Bugfinder Polyspace Analysis
 *
 * @param env The current Jenkins environment. It must include the
 *            following parameters.
 * @deprecated implementation of this function was split up in a customizable
 *             part and a default implementation (look at GIT project ProjectWorksDefault)
 *             http://debegits001.de.kostal.int:7990/scm/aep/projectworksdefault.git
 * */
void run_analysis(EnvActionImpl env) {
    runAnalysis(env)
}

/**
 * commonPreparation4AnalysisAndAlerting prepares Environment to run both the
 * analysis of bugfinder as the alerting tool
 *
 * @param env : The current Jenkins environment. It must include the
 *                            following parameters.
 * @param name : string for debug messages to show the actual context (Anlaysis||Alerting)
 * @return the newEnv
 * @deprecated implementation of this function was split up in a customizable
 *             part and a default implementation (look at GIT project ProjectWorksDefault)
 *             http://debegits001.de.kostal.int:7990/scm/aep/projectworksdefault.git
 **/
EnvActionImpl commonPreparation4AnalysisAndAlerting(EnvActionImpl env, String name) {
    EnvActionImpl newEnv
    Utility utility = new Utility()

    newEnv = this.createEnv(env)

    SWFCheckout swflibtoolsCheckout = new SWFCheckout()

    // checkout of project
    if (newEnv.DEBUG_DONOT_CHECKOUT == 'true') {
        utility.debugPrint(
            newEnv, name,
            'DEBUG_DONOT_CHECKOUT is set to TRUE, so no checkout of ' +
                'sources from SVN.:' + newEnv.DEBUG_DONOT_CHECKOUT, '')
    }
    else {
        swflibtoolsCheckout.checkout(newEnv)
    }

    this.createTempWorkingDirStruct(newEnv)

    this.checkoutAnalysisG00500ExProject(newEnv)
    this.createJenkinsShareDirectory(newEnv)
    this.copyResultsFromArtifacts(newEnv)

    this.copyTemplatesFromG00500(newEnv)
    this.createPsResultDirFile(newEnv)

    // this next step is necessary because the known environment
    // variable PS_RESULT_DIR is created and known in the gnu
    // make file, but not here!
    newEnv = this.setResultsDirEnv(newEnv)

    // create folders for generated files, old result and new
    // result for a fresh run
    this.createResultDirs(newEnv)
    return newEnv
}

/**
 * runAnalysis runs a complete Bugfinder Polyspace Analysis
 *
 * @param env The current Jenkins environment. It must include the
 *            following parameters.
 * @deprecated implementation of this function was split up in a customizable
 *             part and a default implementation (look at GIT project ProjectWorksDefault)
 *             http://debegits001.de.kostal.int:7990/scm/aep/projectworksdefault.git
 * */
void runAnalysis(EnvActionImpl env) {
    CustomerArtifactsPolyspace customerArtifactsPolyspace = new CustomerArtifactsPolyspace(this)
    customerArtifactsPolyspace.runAnalysis(env)
}

/**
 * alerts developers by email for polyspace and qa-c
 *
 * @param env The current Jenkins environment. It must include the
 *            following parameters.
 * @deprecated implementation of this function was split up in a customizable
 *             part and a default implementation (look at GIT project ProjectWorksDefault)
 *             http://debegits001.de.kostal.int:7990/scm/aep/projectworksdefault.git
 * */
void runAlertDeveloper(EnvActionImpl env) {
    CustomerArtifactsPolyspace customerArtifactsPolyspace = new CustomerArtifactsPolyspace(this)
    customerArtifactsPolyspace.runAlertDeveloper(env)
}

/**
 * @func cp_WorkingFolder2stashedResultsFolder( )
 * @description copies content to be stashed and archived finally to the
 *              related archiving
 *              folder outside the project structure into archive folder like
 *              PROJECT_VARIANT_NAME/data/Polyspace/input/Bugfinder/ in WS
 *              (working data like emails and user_scripts or Modules
 *              assignmend list) and
 *              PROJECT_VARIANT_NAME/results/Polyspace/input/Bugfinder/ for
 *              the results of the analysis
 *
 * @param env The current Jenkins environment. It must include the
 *            following parameters.
 * <ul>
 *   <li>TOOLS_FOLDER_PATH - The path to the tools folder of the project</li>
 * </ul>
 * */
void cp2Archive4AlertDeveloper(EnvActionImpl env) {
    Utility utility = new Utility()

    // for now we copy all Bugfinder data to input folder
    String completeToolsPath = env.WORKSPACE + '/' + env.TOOLS_FOLDER_PATH
    String archivePath = env.WORKSPACE + '/' + env.ARCHIVE_OUTPUT_FOLDER_BF

    utility.cp(
        completeToolsPath + '/Polyspace',
        archivePath + '/Polyspace/',
        'REPLACE_EXISTING,RECURSIVE'
    )
}

/**
 * @func copyArchivedFolders4ALertDeveloper( )
 * @description copies archived folders content to working folder
 *              PROJECT_VARIANT_NAME/data/Polyspace/input/Bugfinder/ in WS
 *              (working data like emails and user_scripts or Modules
 *              assignment list) and
 *              PROJECT_VARIANT_NAME/results/Polyspace/input/Bugfinder/ for the
 *              results of the analysis
 *
 * @param env The current Jenkins environment. It must include the
 *            following parameters.
 * <ul>
 *      <li>TOOLS_FOLDER_PATH - The path to the tools folder of the project</li>
 * </ul>
 * */
@SuppressWarnings('CatchException')
void copyArchivedFolders4ALertDeveloper(EnvActionImpl env) {
    Utility utility = new Utility()
    String name = 'PolyspaceBugFinder:copyArchivedFolders4ALertDeveloper'

    try {
        String completeJenkinsSharePath =
            env.WORKSPACE + '/' + env.JENKINS_SHARE_ARCHIVE
        String completeToolsPath = env.WORKSPACE + '/' + env.TOOLS_FOLDER_PATH
        String archiveInputPath = env.WORKSPACE + '/' + env.ARCHIVE_INPUT_FOLDER
        String archiveOutputPath = env.WORKSPACE + '/' + env.ARCHIVE_OUTPUT_FOLDER_BF
        String psBugFinderResultDir = env.completePathPS_RESULT_DIR

        utility.listFiles(completeToolsPath)
        utility.cp(
            archiveOutputPath + '/_result',
            psBugFinderResultDir + '/',
            'REPLACE_EXISTING,RECURSIVE'
        )

        utility.listFiles(psBugFinderResultDir)
        utility.cp(
            archiveInputPath + '/JenkinsServerStack',
            completeJenkinsSharePath + '/',
            'REPLACE_EXISTING,RECURSIVE'
        )
        utility.listFiles(completeJenkinsSharePath)
    }
    catch (Exception e) {
        utility.debugPrint(
            env, name,
            'Exception ' + e.getClass() + 'No round-trip data available', '')
        println Functions.printThrowable(e)
        throw (e)
    }
}

/**
 * @func unstash4ALertDeveloper( )
 * @description unstashes to archived folders to
 *              PROJECT_VARIANT_NAME/data/Polyspace/input/Bugfinder/ in WS
 *              (working data like emails and user_scripts or Modules
 *              assignment list) and
 *              PROJECT_VARIANT_NAME/results/Polyspace/input/Bugfinder/ for the
 *              results of the analysis
 *
 * @param env The current Jenkins environment. It must include the
 *            following parameters.
 * <ul>
 *      <li>TOOLS_FOLDER_PATH - The path to the tools folder of the project</li>
 * </ul>
 *
 * @return env
 **/
@SuppressWarnings('CatchException')
EnvActionImpl unstash4ALertDeveloper(EnvActionImpl env) {
    Utility utility = new Utility()
    String name = 'PolyspaceBugFinder:unstash4ALertDeveloper'

    bat 'cd'

    boolean reportFlag = false

    try {
        unstash 'swflib_qac_report'
    }
    catch (Exception e) {
        utility.debugPrint(env, name, 'Exception ' + e.getClass() + ': No results QA-C found.', '')
        reportFlag = true
        println e.toString() + ' :: \r\n'
        println '---------'
        println Functions.printThrowable(e)
    }

    // check for compatibility, if old or new project
    FilePath qacResultsPath = utility.createFilePath(
            env.WORKSPACE + '/' + env.QAC_outputPath_BSLASH)

    if (!qacResultsPath.exists()) {
        env.QAC_CHECK = 'false'
        utility.debugPrint(
            env, name,
            'No QA-C results found. Check QAC ' +
            'analysis results folder and configuration.',
            '')
    }

    try {
        unstash 'swflib_report_Ps'
    }
    catch (Exception e) {
        utility.debugPrint(
            env, name, 'No results Polyspace_Bugfinder found.', '')
        println Functions.printThrowable(e)

        if (reportFlag) {
            utility.debugPrint(
                env,
                name,
                'Neither Polyspace_Bugfinder nor QA-C results could be found.',
                ''
            )
            throw (e)
        }
    }

    try {
        unstash 'swflib_JenkinsShare_Ps'
    }
    catch (Exception e) {
        utility.debugPrint(
            env, name,
            "Exception ${e.getClass()}: no round-trip data available.", '')
        println Functions.printThrowable(e)
        throw (e)
    }

    bat 'cd'

    return env
}

/**
 * @func cpAndStash4Ps( )
 * @description copies to archived folders and stashes  to
 *              PROJECT_VARIANT_NAME/data/Polyspace/input/Bugfinder/ in WS
 *              (working data like emails and user_scripts or Modules
 *              assignment list) and
 *              PROJECT_VARIANT_NAME/results/Polyspace/input/Bugfinder/ for the
 *              results of the analysis
 *
 * @param env The current Jenkins environment. It must include the
 *            following parameters.
 * <ul>
 *      <li>TOOLS_FOLDER_PATH - The path to the tools folder of the project</li>
 * </ul>
 *
 * @return env
 **/
void cpAndStash4Ps(EnvActionImpl env) {
    Utility utility = new Utility()

    String completeJenkinsSharePath =
            env.WORKSPACE + '/' + env.JENKINS_SHARE_ARCHIVE

    String workspace = env.WORKSPACE + '/'
    String completeToolsPath = workspace + env.TOOLS_FOLDER_PATH
    String archiveInputPath = workspace + env.ARCHIVE_INPUT_FOLDER
    String archiveOutputPath = workspace + env.ARCHIVE_OUTPUT_FOLDER_BF
    String archiveReportPath = workspace + env.ARCHIVE_REPORT_FOLDER

    String psBugFinderResultDir = env.completePathPS_RESULT_DIR.replaceAll(
            '\\\\_result', ''
    ).replaceAll('_result', '')

    FilePath psResultsFile = utility.createFilePath(
            env.completePathPS_RESULT_DIR)

    if (!psResultsFile.exists()) {
        return
    }

    utility.cp(
        completeToolsPath + '/Polyspace',
        archiveInputPath + '/Polyspace/',
        'REPLACE_EXISTING,RECURSIVE'
    )

    utility.cp(
        psBugFinderResultDir,
        archiveOutputPath + '/',
        'REPLACE_EXISTING,RECURSIVE'
    )

    utility.cp(
        completeJenkinsSharePath,
        archiveInputPath + '/JenkinsServerStack/',
        'REPLACE_EXISTING,RECURSIVE'
    )

    utility.cp(
        completeJenkinsSharePath + '/polyspace_report.html',
        archiveReportPath + '/',
        'REPLACE_EXISTING,RECURSIVE'
    )

    stash([
        includes: env.ARCHIVE_OUTPUT_FOLDER_BF + '/**/*',
        name: 'swflib_report_Ps'
    ])

    stash([
        includes: env.ARCHIVE_INPUT_FOLDER + '/' + 'JenkinsServerStack' + '/**/*',
        name: 'swflib_JenkinsShare_Ps'
    ])

    String archivePolyspace = utility.removeDoubleSlash(
        env.ARCHIVE_INPUT_FOLDER + '/' + 'Polyspace' + '/**/*')

    stash([
        includes: archivePolyspace,
        name: 'swflib_Polyspace_Ps'
    ])

    stash([
        includes: env.ARCHIVE_REPORT_FOLDER + '/**/*',
        name: 'swflib_PsBf_Analysis_report'
    ])
}

/**
 * create html report
 *
 * @param env The current Jenkins environment
**/

void createReport(EnvActionImpl env) {
    String fileContent = """
    <html>
        <iframe src=\"https://${env.PS_SERVER}/metrics.html#Product=Bug%20Finder&Prog=\
${env.PROJECT_VARIANT_NAME}&Mode=Integration&ComparisonMode=false&NewFindingsMode=false&\
QualityObjectivesMode=ON&DisplayMode=progress_percent&CodingRuleTab_GroupBy=Files&\
RunTimeTab_GroupBy=Files&BugFinderTab_GroupBy=Files&CodeCoverageTab_GroupBy=Files&NumItems=8&\
MetricsTab=BugFinderTab&SQOLegend=false\" style=\"position:fixed; top:0; left:0; bottom:0; \
right:0; width:100%; height:100%; border:none; margin:0; \
padding:0; overflow:hidden; z-index:999999;\">
        </iframe>
    </html>
    """

    writeFile file: "${env.JENKINS_SHARE}\\polyspace_report.html", text: fileContent
}

/**
 * archive PolySpace-related artifacts.
 *
 * @param env The current Jenkins environment. It must include the
 *            following parameters.
 * <ul>
 *   <li>ARCHIVE_OUTPUT_FOLDER_BF - The path to the archived output folder
 *       (where results for example are stored)</li>
 *   <li>ARCHIVE_INPUT_FOLDER - The path to the archived input folder, where
 *       input data files lie</li>
 * </ul>
 **/
@SuppressWarnings('CatchException')
void archivePsArtifacts(EnvActionImpl env) {
    // cp the next to the new place and archive the new structure in
    // env.PROJECT_VARIANT_NAME
    String paths2archive = (
        env.ARCHIVE_OUTPUT_FOLDER_BF + '/**/*,' +
        env.ARCHIVE_INPUT_FOLDER + '/**/*,' +
        env.ARCHIVE_REPORT_FOLDER + '/**/*'
    )
    Utility utility = new Utility()

    try {
        new CommonTools().archiveArtifacts(paths2archive)
    }
    catch (Exception e) {
        String message = "Exception ${e.getClass()}: no PolySpace results " +
            'have been produced, therefore nothing will be archived.'

        utility.debugPrint(
            env, 'PolyspaceBugFinder:archivePsArtifacts', message, '')
        println Functions.printThrowable(e)
        throw (e)
    }
}

/**
 * archive PolySpace-related artifacts.
 *
 * @param env The current Jenkins environment. It must include the
 *            following parameters.
 * <ul>
 *   <li>ARCHIVE_INPUT_FOLDER - The path to the archived artifacts folder</li>
 * </ul>
 **/
@SuppressWarnings('CatchException')
void archiveAlertArtifacts(EnvActionImpl env) {
    Utility utility = new Utility()
    String path = env.ARCHIVE_OUTPUT_FOLDER_BF + '/Polyspace/**/*,'

    try {
        new CommonTools().archiveArtifacts(path)
    }
    catch (Exception e) {
        utility.debugPrint('Could not archive files for the alert-developers job.')
        println Functions.printThrowable(e)
    }
}
