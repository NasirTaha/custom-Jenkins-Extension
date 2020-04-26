/*
 * SW-Factory Library module to build Kostal-RTE files.
*/
package com.kostal.pipelineworks.swflib

import com.kostal.pipelineworks.CommandBuilder
import com.kostal.pipelineworks.CommonTools
import com.kostal.pipelineworks.Utility

/**
 * isKaRteConfigurationValid is verifiying KaRte config by checking env vars
 * and setting default values for optional env vars
 *
 * @param env The Jenkins build environment. It must contain
 *            the following variables:
 * <ul>
 *   <li>ECLIPSE_VERSION         - The path where MSYS and MinGW are
 *                                 stored</li>
 *   <li>PROJECT_NAME            - name of the project, e.g.: P12345 </li>
 *   <li>VARIANT_NAME            - name of the variant, e.g.: BCM</li>
 *   <li>COMPILER_PATH           - env var of compiler </li>
 *   <li>COMPILER_NODE           - env var of compiler node </li>
 *   <li>RECIPIENTS              - list of e-mail recipients </li>
 * </ul>
 **/
boolean isKaRteConfigurationValid(Object env) {
    SWFTools swfTools = new SWFTools()

    println('[KaRte.groovy][isKaRteConfigurationValid] Verify KaRte Config START')

    boolean configStatus = swfTools.isConfigurationValid(env,
        ['PROJECT_NAME', 'VARIANT_NAME', 'COMPILER_PATH', 'COMPILE_NODE', 'ECLIPSE_VERSION', 'RECIPIENTS'])

    //check Basic project environment
    swfTools.checkBasicProjectSetup()

    println('[KaRte.groovy][isKaRteConfigurationValid] Verify KaRte Config END')

    return configStatus
}

/**
 * makeKaRte is executing the KA-RTE make call
 * <p>
 * make tool needs to support 'Kostal_RTE' target and toolchain must be
 * available inside project in path ...Tools/KA_RTE
 * @param env The Jenkins build environment. It must contain
 *  the following variables:
 *  <ul>
 *    <li>TOOLS_PATH_MAKE    - (optional) The relative path where the Tools are
 *                             located</li>
 *    <li>SOURCES_PATH       - (optional) Relative path to the Sources folder, Default: Source</li>
 *    <li>ECLIPSE_VERSION    - The path where MSYS and MinGW are stored</li>
 *    <li>MAKE_TOOL          - (optional) used make tool.
 *                             Default: Make.bat</li>
 *    <li>KARTE_BUILD        - (optional) Buildmode for KA-RTE.
 *                             Default: release</li>
 *    <li>MAKE_CORE_NO_KARTE - (optional) No of cores to run KA-RTE build
 *                             process. Default: 8</li>
 *  </ul>
**/
@SuppressWarnings('BuilderMethodWithSideEffects') // Not a builder method
void makeKaRte(Object env) {
    CommandBuilder commandBuilder = new CommandBuilder()
    SWFTools swfTools = new SWFTools()
    Utility swfUtility = new Utility()

    //check make tool call
    env['MAKE_TOOL'] = swfTools.checkEnvVar('MAKE_TOOL', 'make.bat')
    env['KARTE_BUILD'] = swfTools.checkEnvVar('KARTE_BUILD', 'release')
    //check MAKE_CORE_NO_KARTE
    env['MAKE_CORE_NO_KARTE'] = swfTools.checkEnvVar(
        'MAKE_CORE_NO_KARTE', '8')

    //check Basic project environment
    swfTools.checkBasicProjectSetup()
    env['TOOLS_PATH_MAKE'] = swfUtility.toWindowsPath(swfTools.checkEnvVar('TOOLS_PATH_MAKE', env['TOOLS_PATH']))

    /**
    * Execution of the AutoConfigCreator target
    * defined as environment variable (SOURCES) is the location of the
    * analysed sources
    **/
    println(
        '[KaRte.groovy][makeKaRte] call KA-RTE make build with target ' +
        'Kostal_RTE')

    bat commandBuilder.buildBat([
        '@echo off',
        "@set MINGW=%${env.ECLIPSE_VERSION}%/mingw/bin",
        "@set MSYS=%${env.ECLIPSE_VERSION}%/msys64/usr/bin",
        '@set PATH=%MINGW%;%MSYS%',
        '@set WORKSPACE=%cd%',
        "@set SOURCES=%WORKSPACE%\\SWF_Project\\${env.SOURCES_PATH}",
        "cd %WORKSPACE%\\SWF_Project\\${env.TOOLS_PATH_MAKE}\\make",
        "@echo ${env.MAKE_TOOL} ${env.KARTE_BUILD} VERBOSE_ON -j" +
            "${env.MAKE_CORE_NO_KARTE} Kostal_RTE 2>&1 | \"%MSYS%/tee\" " +
            "${env.PROJECT_VARIANT_NAME}_${env.KARTE_BUILD}_KA_RTE.log",

        "${env.MAKE_TOOL} ${env.KARTE_BUILD} VERBOSE_ON " +
            "-j${env.MAKE_CORE_NO_KARTE} Kostal_RTE 2>&1 | \"%MSYS%/tee\" " +
            "${env.PROJECT_VARIANT_NAME}_${env.KARTE_BUILD}_KA_RTE.log"
    ])

    println('[KaRte.groovy][makeKaRte] call KA-RTE make done.')
}

/**
 * archivingKaRte is archiving outputs of KA-RTE stage
 *
 * <p>
 * Following files are archived:
 * <ul>
 *  <li>-[PROJECT_VARIANT_NAME]/log/KaRTE/*</li>
 * </ul>
 * @param env The Jenkins build environment. It must contain
 *  the following variables:
 *  <ul>
 *    <li>TOOLS_PATH_MAKE   - (optional) The relative path where the Tools are located</li>
 *    <li>PROJECT_NAME - name of the project, e.g.: P12345 </li>
 *    <li>VARIANT_NAME - name of the variant, e.g.: BCM</li>
 *    <li>KARTE_BUILD  - (optional) Buildmode for KA-RTE. Default: release</li>
 *  </ul>
 **/
void archivingKaRte(Object env) {
    CommonTools commonTools = new CommonTools()
    CommandBuilder commandBuilder = new CommandBuilder()
    SWFTools swfTools = new SWFTools()
    Utility swfUtility = new Utility()

    env['PROJECT_VARIANT_NAME'] = env.PROJECT_NAME + '_' + env.VARIANT_NAME
    env['KARTE_BUILD'] = swfTools.checkEnvVar('KARTE_BUILD', 'release')

    //check Basic project environment
    swfTools.checkBasicProjectSetup()
    env['TOOLS_PATH_MAKE'] = swfUtility.toWindowsPath(swfTools.checkEnvVar('TOOLS_PATH_MAKE', env['TOOLS_PATH']))

    //copy results to temp workspace
    //copy log
    println('[KaRte.groovy][archivingKaRte] Copy logs START')
    bat commandBuilder.buildBat([
        '@echo off',
        "XCOPY \"%WORKSPACE%\\SWF_Project\\${env.TOOLS_PATH_MAKE}\\Make\\" +
            "${env.PROJECT_VARIANT_NAME}_${env.KARTE_BUILD}_KA_RTE.log\" ^",
        "\"%WORKSPACE%\\${env.PROJECT_VARIANT_NAME}\\log\\KaRTE\\" +
            "${env.PROJECT_VARIANT_NAME}_${env.KARTE_BUILD}_KA_RTE.log*\" /Y"
    ])
    println('[KaRte.groovy][archivingKaRte] Copy logs END')

    //Archive artifacts
    //Archivation of output files.
    println('[KaRte.groovy][archivingKaRte] Archiving START')
    commonTools.archiveArtifacts(env.PROJECT_VARIANT_NAME + '/log/KaRTE/**/*')
    println('[KaRte.groovy][archivingKaRte] Archiving END')
}

/**
 * buildKaRte is calling make tool with specific Kostal RTE parameter
 *
 * @param env The Jenkins build environment. It must contain
 *  the following variables:
 *  <ul>
 *    <li>KA_RTE_GEN         - Activation of Kostal RTE Gen build [ON/OFF]</li>
 *  </ul>
 **/
@SuppressWarnings('BuilderMethodWithSideEffects') // Not a builder method
void buildKaRte(Object env) {
    SWFTools swfTools = new SWFTools()

    if (swfTools.checkON(env.KA_RTE_GEN)) {
        println('[KaRte.groovy] KA-RTE build start.')
        makeKaRte(env)
        archivingKaRte(env)
        println('[KaRte.groovy] KA-RTE build end.')
    }
    else {
        env['KA_RTE_GEN'] = 'OFF'
        println('[KaRte.groovy] KA-RTE build deactivated.')
    }
}

/**
 * buildKaRteStage is calling make tool with specific Kostal RTE parameter
 * <p>
 * Following pipeline stages are created:
 * <ul>
 *  <li>- KA-RTE build</li>
 * </ul>
 *
 * @param env The Jenkins build environment. It must contain
 *  the following variables:
 *  <ul>
 *    <li>PROJECT_NAME       - name of the project </li>
 *    <li>VARIANT_NAME       - name of the variant</li>
 *    <li>TOOLS_PATH_MAKE    - (optional) The relative path where the Tools are
 *                             located</li>
 *    <li>SOURCES_PATH       - (optional) Relative path to the Sources folder</li>
 *    <li>KA_RTE_GEN         - Activation of Kostal RTE Gen build [ON/OFF]</li>
 *    <li>MAKE_TOOL          - (optional) used make tool.
 *                             Default: Make.bat</li>
 *    <li>KARTE_BUILD        - (optional) Buildmode for KA-RTE.
 *                             Default: release</li>
 *    <li>MAKE_CORE_NO_KARTE - (optional) No of cores to run KA-RTE build
 *                             process. Default: 8</li>
 *    <li>MAKE_CORE_NO_KARTE - (optional) No of cores to run KA-RTE build
 *                             process. Default: 8</li>
 **/
@SuppressWarnings('BuilderMethodWithSideEffects')  // Not a builder method
void buildKaRteStage(Object env = this.env) {
    //import library tools
    Notifier notifier = new Notifier()
    SWFTools swfTools = new SWFTools()

    if (swfTools.checkON(env.KA_RTE_GEN)) {
        // add seperate Stage for checkout
        stage('KA-RTE build') {
            // verify Configuration first
            if (!isKaRteConfigurationValid(env)) {
                // Config Error, throw error
                error 'KaRte failed due to an incorrect configuration of ' +
                'environment variables. -> Check the environment variables in the job configuration.'
            }
            try {
                println('[KaRte.groovy] KA-RTE build start.')
                // make KaRte buildmode
                makeKaRte(env)
                // archive build results
                archivingKaRte(env)
                println('[KaRte.groovy] KA-RTE build end.')
            } catch (e) {
                println('[KaRte.groovy] KA-RTE build failed.')
                notifier.onFailure(env, e)
            }
        }
    }
    else {
        //default is release
        env['KA_RTE_GEN'] = 'OFF'
        println('[KaRte.groovy] KA-RTE build deactivated.')
    }
}
