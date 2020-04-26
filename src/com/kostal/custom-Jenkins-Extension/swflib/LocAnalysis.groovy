/*
 * SW-Factory Library module to analyze Lines Of Code in Source folder with cLoc tool
*/
package com.kostal.pipelineworks.swflib

import com.kostal.pipelineworks.CommandBuilder
import com.kostal.pipelineworks.CommonTools
import com.kostal.pipelineworks.CheckoutTools
import com.kostal.pipelineworks.Checkout

/**
 * isLocConfigurationValid is verifiying Loc config by checking env vars
 * and setting default values for optional env vars
 *
 * @param env The Jenkins build environment. It must contain
 *            the following variables:
 *  <ul>
 *    <li>COMPILE_NODE       - Server Node Name</li>
 *    <li>PROJECT_NAME       - name of the project, e.g.: P12345 </li>
 *    <li>VARIANT_NAME       - name of the variant, e.g.: BCM</li>
 *   <li>RECIPIENTS              - list of e-mail recipients </li>
 *  </ul>
 **/
boolean isLocConfigurationValid(Object env) {
    SWFTools swfTools = new SWFTools()
    println('[LocAnalysis.groovy][isLocConfigurationValid] ' +
        'Verify LocAnalysis Config START')

    boolean configStatus = swfTools.isConfigurationValid(env,
    ['COMPILE_NODE', 'PROJECT_NAME', 'VARIANT_NAME', 'RECIPIENTS'])

    //check Basic project environment
    swfTools.checkBasicProjectSetup()

    println('[LocAnalysis.groovy][isLocConfigurationValid] ' +
        'Verify LocAnalysis Config END')

    return configStatus
}

/**
 * getScmPath is building SCM Repository path for CLOC tool
 *
 * @param version Required version info: TRUNK, LATEST or tag name
 **/
String getScmPath(String version) {
    // SCM base path
    String clocRepository = 'https://debesvn001/kostal/lk_ae_internal/LK/CLOC/'
    String clocLatestVersion = 'CLOC_1_72'

    switch (version) {
        case 'TRUNK':
            //return trunk
            return clocRepository + 'trunk/dist'
        case 'LATEST':
            //return LATEST
            return clocRepository + 'tags/' + clocLatestVersion + '/dist'
        case 'CLOC_1_72':
            //return trunk
            return clocRepository + 'tags/CLOC_1_72/dist'
        default:
            println('[LocAnalysis.groovy][getScmPath] no valid version selected, return LATEST')
            return clocRepository + 'tags/' + clocLatestVersion + '/dist'
        }
}

/**
 * checkoutClocTool is getting CLOC tool from SCM into Project directory
 * [TOOLS_PATH]/CLOC
 *
 * @param env The Jenkins build environment. It must contain
 *            the following variables:
 *  <ul>
 *    <li>CLOC_VERSION    - (optional) CLOC Version (TRUNK, LATEST(Default) or
 *                             specific Version Info) </li>
 *    <li>PROJECT_NAME       - name of the project, e.g.: P12345 </li>
 *    <li>VARIANT_NAME       - name of the variant, e.g.: BCM</li>
 *  </ul>
 **/
void checkoutClocTool(Object env) {
    CheckoutTools checkoutTools = new CheckoutTools()
    SWFTools swfTools = new SWFTools()

    println('[LocAnalysis.groovy][checkoutClocTool] Checkout CLOC START')

    env['PROJECT_VARIANT_NAME'] = env.PROJECT_NAME + '_' + env.VARIANT_NAME
    env['CLOC_VERSION'] = swfTools.checkEnvVar('CLOC_VERSION', 'LATEST')

    // Get Repository from SCM
    env['CLOC_REPOSITORY'] = getScmPath(env.CLOC_VERSION)

    println(
        '[LocAnalysis.groovy][checkoutClocTool] Selected CLOC Version: ' +
        env.CLOC_VERSION)

    List checkoutList = []
    checkoutList << [
        env.CLOC_REPOSITORY, env.PROJECT_VARIANT_NAME + '/tools/CLOC/', 'UpdateWithCleanUpdater'
    ]

    checkoutTools.checkoutAll(Checkout.buildCheckout(env, '', checkoutList))

    println('[LocAnalysis.groovy][checkoutClocTool] Checkout CLOC END')
}

/**
 * callcLoc is executing Lines Of Code in SOURCES_PATH with tool cloc-1.72.exe
 *
 * @param env The Jenkins build environment. It must contain
 *            the following variables:
 *  <ul>
 *    <li>SOURCES_PATH - (optional) Relative path to the Source folder,
 *        Default: Source</li>
 *    <li>PROJECT_NAME       - name of the project, e.g.: P12345 </li>
 *    <li>VARIANT_NAME       - name of the variant, e.g.: BCM</li>
 *  </ul>
**/
void callcLoc(Object env) {
    CommandBuilder commandBuilder = new CommandBuilder()
    SWFTools swfTools = new SWFTools()

    //check Basic project environment
    swfTools.checkBasicProjectSetup()

    println('[LocAnalysis.groovy][callcLoc] Config LoC analysis.')
    println('[LocAnalysis.groovy][callcLoc] Call LoC analysis.')

    // Start the Lines Of Code analysis
    bat commandBuilder.buildBat([
        '@set  WORKSPACE=%cd%',
        "@set  SOURCE=%WORKSPACE%\\SWF_Project\\${env.SOURCES_PATH}\\",
        "@cd   %cd%\\SWF_Project\\${env.SOURCES_PATH}",
        "@call %WORKSPACE%\\${env.PROJECT_VARIANT_NAME}\\tools\\CLOC\\cloc.exe ^",
        '%SOURCE% --by-file --xml --exclude-dir=.metadata ' +
            '--xsl=cloc2sloccount.xsl -out=cloc_full.xml.'
    ])

    println('[LocAnalysis.groovy][callcLoc] Publish LoC analysis.')
    sloccountPublish([
        encoding: '',
        numBuildsInGraph: 10,
        pattern: 'SWF_Project/' + env.SOURCES_PATH + '/cloc_full.xml'
    ])
    println('[LocAnalysis.groovy][callcLoc] LoC done.')
}

/**
 * archivingLocAnalysis is archiving data of loc stage
 *
 * Following files are included:
 * <ul>
 *  <li>[PROJECT_VARIANT_NAME]/data/LoC/*</li>
 * </ul>
 * @param env The Jenkins build environment. It must contain
 *            the following variables:
 *  <ul>
 *    <li>SOURCES_PATH - (optional) Relative path to the Source folder,
 *        default: Source</li>
 *    <li>PROJECT_NAME - name of the project, e.g.: P12345 </li>
 *    <li>VARIANT_NAME - name of the variant, e.g.: BCM</li>
 *  </ul>
 **/
void archivingLocAnalysis(Object env) {
    CommonTools commonTools = new CommonTools()
    CommandBuilder commandBuilder = new CommandBuilder()
    SWFTools swfTools = new SWFTools()

    //check Basic project environment
    swfTools.checkBasicProjectSetup()
    env['PROJECT_VARIANT_NAME'] = env.PROJECT_NAME + '_' + env.VARIANT_NAME

    //copy results to temp workspace
    //copy report
    println(
        '[SWFLibTools_stdsw.groovy][archivingLocAnalysis] Copy Reports START')
    bat commandBuilder.buildBat([
        '@echo off',
        "XCOPY \"%WORKSPACE%\\SWF_Project\\${env.SOURCES_PATH}\\" +
            'cloc_full.xml" ^',
        "\"%WORKSPACE%\\${env.PROJECT_VARIANT_NAME}\\data\\LoC\\" +
            'cloc_full.xml*" /Y'
     ])

    println(
        '[SWFLibTools_stdsw.groovy][archivingLocAnalysis] Copy Reports END')

    //Archive artifacts
    //Archivation of output files.
    println('[SWFLibTools_stdsw.groovy][archivingLocAnalysis] Archiving START')
    commonTools.archiveArtifacts(env.PROJECT_VARIANT_NAME + '/data/LoC/**/*')
    println('[SWFLibTools_stdsw.groovy][archivingLocAnalysis] Archiving END')
}

/**
 * callLocAnalysis is analyzing Lines of Code inside a SW-Factory Lib project.
 * <p>
 * Module is using cloc tool to analyze project
 * @param env The Jenkins build environment. It must contain
 *            the following variables:
 *  <ul>
 *    <li>SOURCES_PATH - (optional) Relative path to the Source folder,
 *        default: Source</li>
 *    <li>PROJECT_NAME - name of the project, e.g.: P12345 </li>
 *    <li>VARIANT_NAME - name of the variant, e.g.: BCM</li>
 *    <li>LOC_ANALYSIS - Activation of Lines of Code [ON/OFF]</li>
 *  </ul>
 **/
void callLocAnalysis(Object env = this.env ) {
    Notifier notifier = new Notifier()
    SWFTools swfTools = new SWFTools()

    if (swfTools.checkON(env.LOC_ANALYSIS)) {
        stage('LoC Report') {
            // verify Configuration first
            if (!isLocConfigurationValid(env)) {
                // Config Error, throw error
                error 'Loc failed due to an incorrect configuration of ' +
                'environment variables. -> Check the environment variables in the job configuration.'
            }
            try {
                println('[LocAnalysis.groovy] Analyze LoC start.')
                //checkout Tool from SCM
                checkoutClocTool(env)
                // call C-Loc tooling
                callcLoc(env)
                // archive C-LOC analysis
                archivingLocAnalysis(env)
                println('[LocAnalysis.groovy] Analyze LoC end.')
            } catch (e) {
                println('[LocAnalysis.groovy] callLocAnalysis failed.')
                notifier.onFailure(env, e)
            }
        }
    }
    else {
        env['LOC_ANALYSIS'] = 'OFF'
        println('[LocAnalysis.groovy] Lines of Code deactivated.')
    }
}
