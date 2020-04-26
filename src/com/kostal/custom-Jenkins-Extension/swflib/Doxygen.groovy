/*
 * SW-Factory Library module to analyze Compiler Warnings
*/
package com.kostal.pipelineworks.swflib

import com.kostal.pipelineworks.CommandBuilder
import com.kostal.pipelineworks.CommonTools
import com.kostal.pipelineworks.CheckoutTools
import com.kostal.pipelineworks.Checkout
import com.kostal.pipelineworks.Utility

/**
 * isDoxygenConfigurationValid is verifiying Doxygen config by checking env vars
 * and setting default values for optional env vars
 *
 * @param env The Jenkins build environment. It must contain
 *            the following variables:
 *  <ul>
 *   <li>PROJECT_NAME            - name of the project, e.g.: P12345 </li>
 *   <li>VARIANT_NAME            - name of the variant, e.g.: BCM</li>
 *   <li>DOXYGEN_VERSION         - (optional) Doxygen Version (TRUNK, LATEST(Default) or
 *                             specific Version Info) </li>
 *   <li>TOOLS_PATH_DOXYGEN      - (optional) The relative path where the
 *                               Tools are located in
 *                               project path. Default: Tools</li>
 *  </ul>
 **/
boolean isDoxygenConfigurationValid(Object env) {
    SWFTools swfTools = new SWFTools()

    println('[Doxygen.groovy][isDoxygenConfigurationValid] Verify Doxygen Config START')

    boolean configStatus = swfTools.isConfigurationValid(env,
        ['PROJECT_NAME', 'VARIANT_NAME'])

    //check Basic project environment
    swfTools.checkBasicProjectSetup()

    // verify optional parameter and set default values
    env['MAKE_VAR'] = swfTools.checkEnvVar('MAKE_VAR', '')
    env['COMPILE_NODE'] = swfTools.checkEnvVar('COMPILE_NODE', 'compile')

    println('[Doxygen.groovy][isDoxygenConfigurationValid] Verify Doxygen Config END')

    return configStatus
}

/**
 * getScmPath is building SCM Repository path for Doxygen tool
 *
 * @param version Required version info: TRUNK, LATEST or tag name
 **/
String getScmPath(String version) {
    // SCM base path
    String doxygenRepository = 'https://debesvn001.de.kostal.int/kostal/lk_ae_internal/LK/Doxygen/'
    String doxygenLatestVersion = 'doxygen_1_8_16_x64'

    switch (version) {
        case 'TRUNK':
            //return trunk
            return doxygenRepository + 'trunk/dist'
        case 'LATEST':
            //return LATEST
            return doxygenRepository + 'tags/' + doxygenLatestVersion + '/dist'
        case 'doxygen_1_8_16_x64':
            //return trunk
            return doxygenRepository + 'tags/doxygen_1_8_16_x64/dist'
        default:
            println('[Doxygen.groovy][getScmPath] no valid version selected, return LATEST')
            return doxygenRepository + 'tags/' + doxygenLatestVersion + '/dist'
        }
}

/**
 * checkoutDoxygenTool is getting Doxygen tool from SCM into Project directory
 * [TOOLS_PATH]/Doxygen
 *
 * @param env The Jenkins build environment. It must contain
 *            the following variables:
 *  <ul>
 *   <li>PROJECT_NAME            - name of the project, e.g.: P12345 </li>
 *   <li>VARIANT_NAME            - name of the variant, e.g.: BCM</li>
 *   <li>DOXYGEN_VERSION         - (optional) Doxygen Version (TRUNK, LATEST(Default) or
 *                             specific Version Info) </li>
 *   <li>TOOLS_PATH_DOXYGEN      - (optional) The relative path where the
 *                               Tools are located in
 *                               project path. Default: Tools</li>
 *  </ul>
 **/
void checkoutDoxygenTool(Object env) {
    CheckoutTools checkoutTools = new CheckoutTools()
    SWFTools swfTools = new SWFTools()
    Utility swfUtility = new Utility()

    println('[Doxygen.groovy][checkoutDoxygenTool] Checkout Doxygen START')

    env['PROJECT_VARIANT_NAME'] = env.PROJECT_NAME + '_' + env.VARIANT_NAME

    //check Basic project environment
    swfTools.checkBasicProjectSetup()
    env['TOOLS_PATH_DOXYGEN'] = swfUtility.toWindowsPath(swfTools.checkEnvVar('TOOLS_PATH_DOXYGEN', env['TOOLS_PATH']))
    env['DOXYGEN_VERSION'] = swfTools.checkEnvVar('DOXYGEN_VERSION', 'LATEST')

    // Get Repository from SCM
    env['DOXYGEN_REPOSITORY'] = getScmPath(env.DOXYGEN_VERSION)

    println(
        '[Doxygen.groovy][checkoutDoxygenTool] Selected Doxygen Version: ' +
        env.DOXYGEN_VERSION)

    List checkoutList = []
    checkoutList << [
        env.DOXYGEN_REPOSITORY, env.PROJECT_VARIANT_NAME + '/tools/Doxygen/', 'UpdateWithCleanUpdater'
    ]

    checkoutTools.checkoutAll(Checkout.buildCheckout(env, '', checkoutList))

    println('[Doxygen.groovy][checkoutDoxygenTool] Checkout Doxygen END')
}

/**
 * copyTool to copy Doxygen tool into Project directory
 * [TOOLS_PATH]/Doxygen
 *
 * @param env The Jenkins build environment. It must contain
 *            the following variables:
 *  <ul>
 *   <li>PROJECT_NAME            - name of the project, e.g.: P12345 </li>
 *   <li>VARIANT_NAME            - name of the variant, e.g.: BCM</li>
 *   <li>TOOLS_PATH_DOXYGEN      - (optional) The relative path where the
 *                               Tools are located in
 *                               project path. Default: Tools</li>
 *  </ul>
 **/
void copyTool(Object env) {
    CommandBuilder commandBuilder = new CommandBuilder()
    SWFTools swfTools = new SWFTools()
    Utility swfUtility = new Utility()

    env['PROJECT_VARIANT_NAME'] = env.PROJECT_NAME + '_' + env.VARIANT_NAME

    //check Basic project environment
    swfTools.checkBasicProjectSetup()
    env['TOOLS_PATH_DOXYGEN'] = swfUtility.toWindowsPath(swfTools.checkEnvVar('TOOLS_PATH_DOXYGEN', env['TOOLS_PATH']))

    println(
        '[Doxygen.groovy][copyTool] Copy Doxygen Tool into project')

    bat commandBuilder.buildBat([
        "XCOPY \"${env.PROJECT_VARIANT_NAME}\\tools\\Doxygen\\*\" ^",
        "\"%WORKSPACE%\\SWF_Project\\${env.TOOLS_PATH_DOXYGEN}\\Doxygen\" /s /i /Y"
    ])
}

/**
 * initDoxygen is preparing Doxygen
 *
 * @param env The Jenkins build environment. It must contain
 *            the following variables:
 * <ul>
 *    <li>TOOLS_PATH_DOXYGEN - (optional) The relative path
 *                     where the Tools are located in
 *                     project path. Default: Tools</li>
 * </ul>
 **/
void initDoxygen(Object env) {
    //import library tools
    CommandBuilder commandBuilder = new CommandBuilder()
    SWFTools swfTools = new SWFTools()
    Utility swfUtility = new Utility()

    println(
        '[Doxygen.groovy][initDoxygen] Init Doxygen START.')

    //check Basic project environment
    swfTools.checkBasicProjectSetup()
    env['TOOLS_PATH_DOXYGEN'] = swfUtility.toWindowsPath(swfTools.checkEnvVar('TOOLS_PATH_DOXYGEN', env['TOOLS_PATH']))

    bat commandBuilder.buildBat([
        '@echo on',
        "cd \"%WORKSPACE%\\SWF_Project\\${env.TOOLS_PATH_DOXYGEN}\\Doxygen\\",
        'if exist .\\Doc (@rmdir /S /Q .\\Doc)',
        'mkdir .\\Doc'
    ])

    println(
        '[Doxygen.groovy][initDoxygen] Init Doxygen END.')
}

/**
 * copySettings copies Doxygen settings data to Project template folder SWF_Project
 * <p>
 * Copied files are:
 * <ul>
 *  <li>files from SWF_Project/[TOOLS_PATH_DOXYGEN]/Doxygen/doxygen.txt </li>
 * </ul>
 * @param env The Jenkins build environment. It must contain
 *            the following variables:
 *  <ul>
 *    <li>PROJECT_NAME - name of the project, e.g.: P12345 </li>
 *    <li>VARIANT_NAME - name of the variant, e.g.: BCM</li>
 *    <li>TOOLS_PATH_DOXYGEN   - (optional) The relative path where the
 *                       Tools are located in
 *                       project path. Default: Tools</li>
 *  </ul>
 **/
void copySettings(Object env) {
    CommandBuilder commandBuilder = new CommandBuilder()
    SWFTools swfTools = new SWFTools()
    Utility swfUtility = new Utility()

    env['PROJECT_VARIANT_NAME'] = env.PROJECT_NAME + '_' + env.VARIANT_NAME

    //check Basic project environment
    swfTools.checkBasicProjectSetup()
    env['TOOLS_PATH_DOXYGEN'] = swfUtility.toWindowsPath(swfTools.checkEnvVar('TOOLS_PATH_DOXYGEN', env['TOOLS_PATH']))

    println(
        '[Doxygen.groovy][copySettings] Copy settings output of ' +
        'Doxygen tool: SWF_Project/' + env.TOOLS_PATH_DOXYGEN + '/Doxygen/doxygen.txt')

    bat commandBuilder.buildBat([
        "XCOPY \"%WORKSPACE%\\SWF_Project\\${env.TOOLS_PATH_DOXYGEN}\\Doxygen\\doxygen.txt\" ^",
        "\"%WORKSPACE%\\${env.PROJECT_VARIANT_NAME}\\data\\Doxygen\\input\\doxygen.txt*\" /Y"
    ])

    println(
        '[Doxygen.groovy][copySettings] Copy settings output of ' +
        'Doxygen tool done.')
}

/**
 * callDoxygen is calling Doxygen executable tool
 *
 * @param env The Jenkins build environment. It must contain
 *            the following variables:
 * <ul>
 *    <li>TOOLS_PATH_DOXYGEN - (optional) The relative path where the
 *                     Tools are located in
 *                     project path. Default: Tools</li>
 * </ul>
 **/
void callDoxygen(Object env) {
    //import library tools
    CommandBuilder commandBuilder = new CommandBuilder()
    SWFTools swfTools = new SWFTools()
    Utility swfUtility = new Utility()

    println(
        '[Doxygen.groovy][callDoxygen] Call Doxygen START.')

    //check Basic project environment
    swfTools.checkBasicProjectSetup()
    env['TOOLS_PATH_DOXYGEN'] = swfUtility.toWindowsPath(swfTools.checkEnvVar('TOOLS_PATH_DOXYGEN', env['TOOLS_PATH']))

    bat commandBuilder.buildBat([
        '@echo off',
        "cd \"%WORKSPACE%\\SWF_Project\\${env.TOOLS_PATH_DOXYGEN}\\Doxygen\\",
        'doxygen.exe doxygen.txt'
    ])

    println(
        '[Doxygen.groovy][callDoxygen] Call Doxygen END.')
}

/**
 * copyReport copies Doxygen report data to Project template folder SWF_Project
 * <p>
 * Copied files are:
 * <ul>
 *  <li>files from SWF_Project/[TOOLS_PATH_DOXYGEN]/Doxygen/Doc/* </li>
 * </ul>
 * @param env The Jenkins build environment. It must contain
 *            the following variables:
 *  <ul>
 *    <li>PROJECT_NAME - name of the project, e.g.: P12345 </li>
 *    <li>VARIANT_NAME - name of the variant, e.g.: BCM</li>
 *    <li>TOOLS_PATH_DOXYGEN   - (optional) The relative path where the
 *                       Tools are located in
 *                       project path. Default: Tools</li>
 *  </ul>
 **/
void copyReport(Object env) {
    CommandBuilder commandBuilder = new CommandBuilder()
    SWFTools swfTools = new SWFTools()
    Utility swfUtility = new Utility()

    env['PROJECT_VARIANT_NAME'] = env.PROJECT_NAME + '_' + env.VARIANT_NAME

    //check Basic project environment
    swfTools.checkBasicProjectSetup()
    env['TOOLS_PATH_DOXYGEN'] = swfUtility.toWindowsPath(swfTools.checkEnvVar('TOOLS_PATH_DOXYGEN', env['TOOLS_PATH']))

    println(
        '[Doxygen.groovy][copyReport] Zip and copy doc: ' +
        'SWF_Project/' + env.TOOLS_PATH_DOXYGEN + '/Doxygen/Doc/*')

    zip zipFile: env.PROJECT_VARIANT_NAME + '/report/Doxygen/DoxygenDoc.zip',
        archive: false,
        dir: 'SWF_Project/' + env.TOOLS_PATH_DOXYGEN + '/Doxygen/Doc'

    bat commandBuilder.buildBat([
        "XCOPY \"%WORKSPACE%\\${env.PROJECT_VARIANT_NAME}\\report\\Doxygen\\DoxygenDoc.zip\" ^",
        "\"%WORKSPACE%\\${env.PROJECT_VARIANT_NAME}\\data\\Doxygen\\output\\DoxygenDoc.zip*\" /Y"
    ])

    println(
        '[Doxygen.groovy][copyReport] Zip and copy Doxygen doc done.')
}

/**
 * stashingOutput copies StatRes output to Project template folder SWF_Project
 * <p>
 * The following folders are stashed:
 * <ul>
 *  <li>swflib_doxygen_report: [PROJECT_VARIANT_NAME]/report/Doxygen/*</li>
 * </ul>
 * @param env The Jenkins build environment. It must contain
 *            the following variables:
 *  <ul>
 *    <li>PROJECT_NAME - name of the project, e.g.: P12345 </li>
 *    <li>VARIANT_NAME - name of the variant, e.g.: BCM</li>
 *  </ul>
 **/
void stashingOutput(Object env) {
    env['PROJECT_VARIANT_NAME'] = env.PROJECT_NAME + '_' + env.VARIANT_NAME

    println(
        '[Doxygen.groovy][stashingOutput] Stashing files for Doxygen START')

    // stash map file
    stash([
        includes: env.PROJECT_VARIANT_NAME + '/report/Doxygen/DoxygenDoc.zip',
        name: 'swflib_doxygen_report'
    ])

    println(
        '[Doxygen.groovy][stashingOutput] Stashing output of ' +
        'Doxygen tool in swflib_doxygen_report done')
}

/**
 * archivingDoxygen is archiving and stashing outputs of Doxygen stage
 * <p>
 * Following folders are archived:
 * <ul>
 *  <li>[PROJECT_VARIANT_NAME]/data/Doxygen/*</li>
 *  <li>[PROJECT_VARIANT_NAME]/report/Doxygen/*</li>
 * </ul>
 * @param env The Jenkins build environment. It must contain
 *            the following variables:
 *  <ul>
 *    <li>PROJECT_NAME - name of the project, e.g.: P12345 </li>
 *    <li>VARIANT_NAME - name of the variant, e.g.: BCM</li>
 *  </ul>
 **/
void archivingDoxygen(Object env) {
    CommonTools commonTools = new CommonTools()
    env['PROJECT_VARIANT_NAME'] = env.PROJECT_NAME + '_' + env.VARIANT_NAME

    println(
        '[Doxygen.groovy][archivingDoxygen] Archiving START')

    commonTools.archiveArtifacts(
        env.PROJECT_VARIANT_NAME + '/data/Doxygen/output/DoxygenDoc.zip')
    commonTools.archiveArtifacts(
        env.PROJECT_VARIANT_NAME + '/report/Doxygen/DoxygenDoc.zip')

    println('[Doxygen.groovy][archivingDoxygen] Archiving END')
}

/**
 * parseDoxygenReport is calling recordIssues tool
 *
 * @param env The Jenkins build environment. It must contain
 *            the following variables:
 * <ul>
 *    <li>TOOLS_PATH_DOXYGEN - (optional) The relative path where the
 *                     Tools are located in
 *                     project path. Default: Tools</li>
 * </ul>
 **/
void parseDoxygenReport(Object env) {
    SWFTools swfTools = new SWFTools()
    Utility swfUtility = new Utility()

    //check Basic project environment
    swfTools.checkBasicProjectSetup()
    env['TOOLS_PATH_DOXYGEN'] = swfUtility.toWindowsPath(swfTools.checkEnvVar('TOOLS_PATH_DOXYGEN', env['TOOLS_PATH']))

    println(
        '[Doxygen.groovy][parseDoxygenReport] Parse Doxygen report START.')
    // call record Issues
    recordIssues(
        tools: [
            doxygen(
                pattern: 'SWF_Project\\' + env.TOOLS_PATH_DOXYGEN +
                         '\\Doxygen\\Doc\\warn.txt'
            )
        ]
    )

    println(
        '[Doxygen.groovy][parseDoxygenReport] Parse Doxygen report END.')
}

/**
 * buildDoxygen is analyzing SW-Factory-Lib project to find doxygen tags
 *
 * @param env The Jenkins build environment. It must contain
 *            the following variables:
 * <ul>
 *    <li>DOXYGEN            - Activation of Doxygen
 *                                 [ON/OFF]</li>
 *    <li>DOXYGEN_VERSION    - Doxygen Version (TRUNK, LATEST(Default) or
 *                             specific Version Info) </li>
 *    <li>PROJECT_NAME       - name of the project, e.g.: P12345 </li>
 *    <li>VARIANT_NAME       - name of the variant, e.g.: BCM</li>
 *    <li>TOOLS_PATH_DOXYGEN - (optional) The relative path where the
 *                     Tools are located in
 *                     project path. Default: Tools</li>
 * </ul>
 **/
void buildDoxygen(Object env = this.env) {
    //import library tools
    Notifier notifier = new Notifier()
    SWFTools swfTools = new SWFTools()

    if (swfTools.checkON(env.DOXYGEN)) {
        // add seperate Stage for checkout
        stage('Doxygen') {
            // verify Configuration first
            if (!isDoxygenConfigurationValid(env)) {
                // Config Error, throw error
                error 'Doxygen failed due to an incorrect configuration of ' +
                'environment variables. -> Check the environment variables in the job configuration.'
            }
            try {
                println(
                    '[Doxygen.groovy] Analyze Doxygen START.')

                //checkout Tool from SCM
                checkoutDoxygenTool(env)

                //copy Tool to project workspace
                copyTool(env)

                // init Doxygen workspace
                initDoxygen(env)

                // copy Doxygen Settings
                copySettings(env)

                // call Doxygen
                callDoxygen(env)

                // copy Files to TempWorkspace
                copyReport(env)

                //parse warnings log File
                parseDoxygenReport(env)

                // stashing and archiving
                archivingDoxygen(env)
                stashingOutput(env)

                println(
                    '[Doxygen.groovy] Analyze Doxygen END.')
            } catch (e) {
                //Print error message
                println('[Doxygen.groovy] buildDoxygen failed.')
                notifier.onFailure(env, e)
            }
        }
    }
    else {
        //default is release
        env['DOXYGEN'] = 'OFF'
        println('[Doxygen.groovy] Doxygen deactivated.')
    }
}
