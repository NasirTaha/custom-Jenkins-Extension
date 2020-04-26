/*
 * SW-Factory Library module to analyze usage of Kostal Standard
 * Software modules
*/
package com.kostal.pipelineworks.swflib

import com.kostal.pipelineworks.CommandBuilder
import com.kostal.pipelineworks.CommonTools
import com.kostal.pipelineworks.reporting.StandardSoftwareReport
import com.kostal.pipelineworks.Utility

/**
 * isStdSwConfigurationValid is verifiying StandardSoftware config by checking env vars
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
 * </ul>
 **/
boolean isStdSwConfigurationValid(Object env) {
    SWFTools swfTools = new SWFTools()

    println('[StandardSoftware.groovy][isStdSwConfigurationValid] Verify StandardSoftware Config START')

    boolean configStatus = swfTools.isConfigurationValid(env,
        ['PROJECT_NAME', 'VARIANT_NAME', 'ECLIPSE_VERSION', 'RECIPIENTS'])

    //check Basic project environment
    swfTools.checkBasicProjectSetup()

    println('[StandardSoftware.groovy][isStdSwConfigurationValid] Verify StandardSoftware Config END')

    return configStatus
}

/**
 * makeStandardSoftware is executing the STD-SW Trace analysis by calling
 * make tool
 *
 * <p>
 * make tool needs to support build target 'AutoConfigCreator' and 'csValidate'
 * @param env The Jenkins build environment. It must contain
 *            the following variables:
 *  <ul>
 *    <li>TOOLS_PATH_MAKE  - (optional) The relative path where the Tools are located</li>
 *    <li>SOURCES_PATH     - (optional) Relative path to the Sources folder, Default: Source</li>
 *    <li>ECLIPSE_VERSION  - The path where MSYS and MinGW are stored</li>
 *    <li>MAKE_VAR         - (optional) String buffer to add parameters to your build
 *                           process as defined in you make tool</li>
 *    <li>MAKE_TOOL        - (optional) used make tool. Default: Make.bat</li>
 *    <li>MAKE_CORE_NO_STDSW - (optional) No of cores to run STDSW process.
 *                             Default: 16</li>
 *    <li>STD_SW_BUILD       - (optional) Build target for STD-SW build</li>
 *  </ul>
**/
@SuppressWarnings('BuilderMethodWithSideEffects') // Not a builder method
void makeStandardSoftware(Object env) {
    CommandBuilder commandBuilder = new CommandBuilder()
    SWFTools swfTools = new SWFTools()
    Utility swfUtility = new Utility()

    env['MAKE_TOOL'] = swfTools.checkEnvVar('MAKE_TOOL', 'make.bat')
    env['MAKE_VAR'] = swfTools.checkEnvVar('MAKE_VAR', '')
    env['MAKE_CORE_NO_STDSW'] = swfTools.checkEnvVar(
        'MAKE_CORE_NO_STDSW', '16')
    env['STD_SW_BUILD'] = swfTools.checkEnvVar('STD_SW_BUILD', 'release')

    //check Basic project environment
    swfTools.checkBasicProjectSetup()
    env['TOOLS_PATH_MAKE'] = swfUtility.toWindowsPath(swfTools.checkEnvVar('TOOLS_PATH_MAKE', env['TOOLS_PATH']))

    /*
    Execution of the AutoConfigCreator target
    defined as environment variable (SOURCES) is the location of the
    analysed sources
    */
    println(
        '[StandardSoftware.groovy][makeStandardSoftware] ' +
        'call STD-SW AutoConfigCreator')
    bat commandBuilder.buildBat([
        '@echo off',
        "@set MINGW=%${env.ECLIPSE_VERSION}%/mingw/bin",
        "@set MSYS=%${env.ECLIPSE_VERSION}%/msys64/usr/bin",
        '@set PATH=%MINGW%;%MSYS%',
        '@set WORKSPACE=%cd%',
        "@set SOURCES=%WORKSPACE%\\SWF_Project\\${env.SOURCES_PATH}",
        "cd %WORKSPACE%\\SWF_Project\\${env.TOOLS_PATH_MAKE}\\make",
        "@echo ${env.MAKE_TOOL} ${env.STD_SW_BUILD} ${env.MAKE_VAR} " +
            "-j${env.MAKE_CORE_NO_STDSW} AutoConfigCreator 2>&1 | " +
            "\"%MSYS%/tee\" ${env.PROJECT_VARIANT_NAME}_StdSW_" +
            'AutoConfigCreator.log',

        "${env.MAKE_TOOL} ${env.STD_SW_BUILD} ${env.MAKE_VAR} " +
            "-j${env.MAKE_CORE_NO_STDSW} AutoConfigCreator 2>&1 | " +
            "\"%MSYS%/tee\" ${env.PROJECT_VARIANT_NAME}_StdSW_" +
            'AutoConfigCreator.log'
   ])

    /*
    Execution of the csValidate target
    defined as environment variable (SOURCES) is the location of the
    analysed sources
    */
    println(
        '[StandardSoftware.groovy][makeStandardSoftware] call ' +
        'STD-SW csValidate')

    bat commandBuilder.buildBat([
        '@echo off',
        "@set MINGW=%${env.ECLIPSE_VERSION}%/mingw/bin",
        "@set MSYS=%${env.ECLIPSE_VERSION}%/msys64/usr/bin",
        '@set PATH=%MINGW%;%MSYS%',
        '@set WORKSPACE=%cd%',
        "@set SOURCES=%WORKSPACE%\\SWF_Project\\${env.SOURCES_PATH}",
        "cd %WORKSPACE%\\SWF_Project\\${env.TOOLS_PATH_MAKE}\\make",
        "@echo ${env.MAKE_TOOL} ${env.STD_SW_BUILD} ${env.MAKE_VAR} " +
            "-j${env.MAKE_CORE_NO_STDSW} csValidate 2>&1 | " +
            "\"%MSYS%/tee\" ${env.PROJECT_VARIANT_NAME}_StdSW_csValidate.log",

        "${env.MAKE_TOOL} ${env.STD_SW_BUILD} ${env.MAKE_VAR} " +
            "-j${env.MAKE_CORE_NO_STDSW} csValidate 2>&1 | " +
            "\"%MSYS%/tee\" ${env.PROJECT_VARIANT_NAME}_StdSW_csValidate.log"
    ])

    println(
        '[StandardSoftware.groovy][makeStandardSoftware] ' +
        'call STD-SW make done.')
}

/**
 * buildReport is calling the STD-SW Trace report builder
 *
 * <p>
 * buildReport is using [TARGET_PATH]/[STD_SW_BUILD]/bin/componentList.xml file
 * as input. Headerlist is given by file [TOOLS_PATH]/Make/lists/headerlist.mk
 * @param env The Jenkins build environment. It must contain
 *            the following variables:
 *  <ul>
 *    <li>TOOLS_PATH_MAKE   - (optional) The relative path where the Tools are located, Default: Tools</li>
 *    <li>PROJECT_NAME - name of the project, e.g.: P12345 </li>
 *    <li>VARIANT_NAME - name of the variant, e.g.: BCM</li>
 *    <li>STD_SW_BUILD       - (optional) Build target for STD-SW build</li>
 *    <li>STD_SW_HEADER_L    - (optional) Header List for STD-SW tooling,
 *                             Default: headerlist.mk </li>
 *    <li>TARGET_PATH   - (optional) The relative path where the Target folder are located</li>
 *  </ul>
**/
@SuppressWarnings('BuilderMethodWithSideEffects')  // Not a builder method
void buildReport(Object env) {
    SWFTools swfTools = new SWFTools()
    Utility swfUtility = new Utility()

    env['PROJECT_VARIANT_NAME'] = env.PROJECT_NAME + '_' + env.VARIANT_NAME
    //check generated header list, old: headerlist.mk, new:h_filelist.txt
    env['STD_SW_HEADER_L'] = swfTools.checkEnvVar('STD_SW_HEADER_L', 'headerlist.mk')

    //check Basic project environment
    swfTools.checkBasicProjectSetup()
    env['TOOLS_PATH_MAKE'] = swfUtility.toWindowsPath(swfTools.checkEnvVar('TOOLS_PATH_MAKE', env['TOOLS_PATH']))

    println(
        '[StandardSoftware.groovy][buildReport] build StandardSoftwareReport')

    StandardSoftwareReport reporter = new StandardSoftwareReport(
        "%WORKSPACE%\\SWF_Project\\${env.TARGET_PATH}\\${env.STD_SW_BUILD}" +
        '\\bin\\componentList.xml')
    .withProjectId(env.PROJECT_NAME)
    .withDescription(env.PROJECT_VARIANT_NAME)
    .withHeaderPath(
        "%WORKSPACE%\\SWF_Project\\${env.TOOLS_PATH_MAKE}\\Make" +
        "\\lists\\${env.STD_SW_HEADER_L}")

    bat reporter.getCommand()
    println(
        '[StandardSoftware.groovy][buildReport] build ' +
        'StandardSoftwareReport done.')
}

/**
 * archivingStandardSoftware is archiving and stashing outputs of Std-SW stage
 *
 * <p>
 * Following files are archived:
 * <ul>
 *  <li>[PROJECT_VARIANT_NAME]/data/StdSW/*</li>
 *  <li>[PROJECT_VARIANT_NAME]/log/StdSW/*</li>
 *  <li>[PROJECT_VARIANT_NAME]/report/StdSW/*</li>
 * </ul>
 * Following files are included in stash:
 * <ul>
 *  <li>swflib_stdsw_report: [PROJECT_VARIANT_NAME]/report/StdSW/*</li>
 * </ul>
 * @param env The Jenkins build environment. It must contain
 *            the following variables:
 *  <ul>
 *    <li>TOOLS_PATH   - (optional) The relative path where the Tools are located</li>
 *    <li>TARGET_PATH   -(optional)  The relative path where the Target folder are located</li>
 *    <li>PROJECT_NAME - name of the project, e.g.: P12345 </li>
 *    <li>VARIANT_NAME - name of the variant, e.g.: BCM</li>
 *    <li>STD_SW_BUILD       - (optional) Build target for STD-SW build</li>
 **/
void archivingStandardSoftware(Object env) {
    CommonTools commonTools = new CommonTools()
    CommandBuilder commandBuilder = new CommandBuilder()
    SWFTools swfTools = new SWFTools()
    Utility swfUtility = new Utility()

    env['PROJECT_VARIANT_NAME'] = env.PROJECT_NAME + '_' + env.VARIANT_NAME

    //check Basic project environment
    swfTools.checkBasicProjectSetup()
    env['TOOLS_PATH_MAKE'] = swfUtility.toWindowsPath(swfTools.checkEnvVar('TOOLS_PATH_MAKE', env['TOOLS_PATH']))

    println(
        '[StandardSoftware.groovy][archivingStandardSoftware] Copy logs START')

    bat commandBuilder.buildBat([
        '@echo off',
        "XCOPY \"%WORKSPACE%\\SWF_Project\\${env.TOOLS_PATH_MAKE}\\" +
            "Make\\${env.PROJECT_VARIANT_NAME}_StdSW_AutoConfigCreator.log\" ^",

        "\"%WORKSPACE%\\${env.PROJECT_VARIANT_NAME}\\log\\StdSW\\" +
            "${env.PROJECT_VARIANT_NAME}_StdSW_AutoConfigCreator.log*\" /Y",

        "XCOPY \"%WORKSPACE%\\SWF_Project\\${env.TOOLS_PATH_MAKE}\\" +
            "Make\\${env.PROJECT_VARIANT_NAME}_StdSW_csValidate.log\" ^",

        "\"%WORKSPACE%\\${env.PROJECT_VARIANT_NAME}\\log\\StdSW\\" +
            "${env.PROJECT_VARIANT_NAME}_StdSW_csValidate.log*\" /Y"
   ])

    println(
        '[StandardSoftware.groovy][archivingStandardSoftware] ' +
        'Copy logs END')

    //copy report
    println('[StandardSoftware.groovy][archivingStandardSoftware] ' +
        'Copy Reports START')

    bat commandBuilder.buildBat([
        '@echo off',
        "XCOPY \"%WORKSPACE%\\SWF_Project\\${env.TARGET_PATH}\\${env.STD_SW_BUILD}" +
            '\\bin\\componentList.xml" ^',

        "\"%WORKSPACE%\\${env.PROJECT_VARIANT_NAME}\\data\\StdSW" +
            '\\componentList.xml*" /Y',

        'XCOPY \"%WORKSPACE%\\standard_components.html\" ^',
        "\"%WORKSPACE%\\${env.PROJECT_VARIANT_NAME}\\report\\StdSW" +
            '\\StdSW_report.html*" /Y'
    ])

    println(
        '[StandardSoftware.groovy][archivingStandardSoftware] ' +
        'Copy Reports END')

    println(
        '[StandardSoftware.groovy][archivingStandardSoftware] ' +
        'Archiving START')

    commonTools.archiveArtifacts(
        env.PROJECT_VARIANT_NAME + '/data/StdSW/**/*')
    commonTools.archiveArtifacts(
        env.PROJECT_VARIANT_NAME + '/log/StdSW/**/*')
    commonTools.archiveArtifacts(
        env.PROJECT_VARIANT_NAME + '/report/StdSW/**/*')

    println(
        '[StandardSoftware.groovy][archivingStandardSoftware] Archiving END')

    println(
        '[StandardSoftware.groovy][archivingStandardSoftware] Stashing START')

    stash([
        includes: env.PROJECT_VARIANT_NAME + '/report/StdSW/**/*',
        name: 'swflib_stdsw_report'
    ])

    println(
        '[StandardSoftware.groovy][archivingStandardSoftware] ' +
        'Stash swflib_stdsw_report for StdSW Reports done.')

    println(
        '[StandardSoftware.groovy][archivingStandardSoftware] Stashing END')
}

/**
 * standardSoftwareReport is analyzing a SW-Factory Lib project for usage of
 * Kostal Standard Software modules
 *
 * <p>
 * Following pipeline stages are created:
 * <ul>
 *  <li>Std-SW Report</li>
 * </ul>
 *
 * @param env The Jenkins build environment. It must contain
 *            the following variables:
 *  <ul>
 *    <li>PROJECT_NAME       - name of the project </li>
 *    <li>VARIANT_NAME       - name of the variant</li>
 *    <li>STD_SW_ANALYSIS    - Activation of Standard-SW Check [ON/OFF]</li>
 *    <li>MAKE_CORE_NO_STDSW - (optional) No of cores to run STDSW process.
 *                             Default: 16</li>
 *    <li>TOOLS_PATH_MAKE  - (optional) The relative path where the Tools are located</li>
 *    <li>SOURCES_PATH     - (optional) Relative path to the Sources folder, Default: Source</li>
 *    <li>ECLIPSE_VERSION  - The path where MSYS and MinGW are stored</li>
 *    <li>MAKE_VAR         - (optional) String buffer to add parameters to your build
 *                           process as defined in you make tool</li>
 *    <li>MAKE_TOOL        - (optional) used make tool. Default: Make.bat</li>
 *    <li>STD_SW_BUILD       - (optional) Build target for STD-SW build</li>
 *    <li>TOOLS_PATH   - (optional) The relative path where the Tools are located</li>
 *    <li>TARGET_PATH   -(optional)  The relative path where the Target folder are located</li>
 *    <li>STD_SW_HEADER_L    - (optional) Header List for STD-SW tooling,
 *                             Default: headerlist.mk </li>
 **/
void standardSoftwareReport(Object env = this.env) {
    Notifier notifier = new Notifier()
    SWFTools swfTools = new SWFTools()

    if (swfTools.checkON(env.STD_SW_ANALYSIS)) {
        stage('Std-SW Report') {
            // verify Configuration first
            if (!isStdSwConfigurationValid(env)) {
                // Config Error, throw error
                error 'StandardSoftware failed due to an incorrect configuration of the ' +
                'environment variables. -> Check the environment variables in the job configuration.'
            }
            try {
                println(
                    '[StandardSoftware.groovy][standardSoftwareReport] ' +
                    'Analyze Std-SW start.')

                makeStandardSoftware(env)
                buildReport(env)
                archivingStandardSoftware(env)

                println(
                    '[StandardSoftware.groovy][standardSoftwareReport] ' +
                    'Analyze Std-SW end.')
            } catch (e) {
                println(
                    '[StandardSoftware.groovy][standardSoftwareReport] ' +
                    'standardSoftwareReport failed.')
                notifier.onFailure(env, e)
            }
        }
    }
    else {
        //default OFF
        env['STD_SW_ANALYSIS'] = 'OFF'
        println(
            '[standardSoftwareReport.groovy][standardSoftwareReport] ' +
            'Standard-SW Check deactivated.')
    }
}
