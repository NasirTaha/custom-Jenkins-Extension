/*
 * SW-Factory Library module to run MISRA/QAC and HIS metric check
*/
package com.kostal.pipelineworks.swflib

import com.kostal.pipelineworks.CommandBuilder
import com.kostal.pipelineworks.CommonTools
import com.kostal.pipelineworks.CITools
import com.kostal.pipelineworks.CheckoutTools
import com.kostal.pipelineworks.Checkout
import com.kostal.pipelineworks.Utility

/**
 * isQacConfigurationValid is verifiying QAC config by checking env vars
 * and setting default values for optional env vars
 *
 * @param env The Jenkins build environment. It must contain
 *            the following variables:
 * <ul>
 *   <li>ECLIPSE_VERSION         - The path where MSYS and MinGW are
 *                                 stored</li>
 *   <li>QAC_VERSION             - used QAC version [QAC/QAC9]</li>
 *   <li>PROJECT_NAME            - name of the project, e.g.: P12345 </li>
 *   <li>VARIANT_NAME            - name of the variant, e.g.: BCM</li>
 *   <li>COMPILER_PATH           - env var of compiler </li>
 *   <li>RECIPIENTS              - list of e-mail recipients </li>
 *   <li>QAC_PATH                - (optional) path to QAC7.2, Default: CI_QAC_72 </li>
 *   <li>QAC9_PATH               - (optional) path to QAC9, Default: C:/Tools/PRQA-Framework-2.1.0 </li>
 * </ul>
 **/
boolean isQacConfigurationValid(Object env) {
    SWFTools swfTools = new SWFTools()

    println('[QAC.groovy][isQacConfigurationValid] Verify QAC Config START')

    boolean configStatus = swfTools.isConfigurationValid(env,
        ['QAC_VERSION', 'PROJECT_NAME', 'VARIANT_NAME', 'ECLIPSE_VERSION', 'RECIPIENTS'])

    //check Basic project environment
    swfTools.checkBasicProjectSetup()

    // check, if QA9_PATH is set, otherwise add default
    if (env['QAC_PATH'] == null) {
        println('[QAC.groovy][isQacConfigurationValid] Set Default for QAC_PATH: ' +
            '$(CI_QAC_72)')
        env['QAC_PATH'] = '$(CI_QAC_72)'
    }

    // check, if QA9_PATH is set, otherwise add default
    if (env['QAC9_PATH'] == null) {
        println('[QAC.groovy][isQacConfigurationValid] Set Default for QAC9_PATH: ' +
            'C:/Tools/PRQA-Framework-2.1.0')
        env['QAC9_PATH'] = 'C:/Tools/PRQA-Framework-2.1.0'
    }

    println('[QAC.groovy][isQacConfigurationValid] Verify QAC Config END')

    return configStatus
}

/**
 * getScmPathHis is building SCM Repository path for HISReport tool
 *
 * @param version Required version info: TRUNK, LATEST or tag name
 **/
String getScmPathHis(String version) {
    // SCM base path
    String hisRepository = 'https://debesvn001/kostal/lk_ae_internal/LK/HISReport/'
    String hisLatestVersion = 'HISReport_v02_20'

    switch (version) {
        case 'TRUNK':
            //return trunk
            return hisRepository + 'trunk/dist'
        case 'LATEST':
            //return LATEST
            return hisRepository + 'tags/' + hisLatestVersion + '/dist'
        case 'HISReport_v02_00':
            //return trunk
            return hisRepository + 'tags/HISReport_v02_00/dist'
        case 'HISReport_v02_10':
            //return trunk
            return hisRepository + 'tags/HISReport_v02_10/dist'
        case 'HISReport_v02_20':
            //return trunk
            return hisRepository + 'tags/HISReport_v02_20/dist'
        default:
            println('[Doxygen.groovy][getScmPathHis] no valid version selected, return LATEST')
            return hisRepository + 'tags/' + hisLatestVersion + '/dist'
        }
}

/**
 * checkoutHisTool is getting HISReport tool from SCM into Project directory
 * [TOOLS_PATH]/HISReport
 *
 * @param env The Jenkins build environment. It must contain
 *            the following variables:
 *  <ul>
 *    <li>HISREPORT_VERSION    - (optional) HISReport Version (TRUNK, LATEST(Default) or
 *                             specific Version Info) </li>
 *   <li>PROJECT_NAME            - name of the project, e.g.: P12345 </li>
 *   <li>VARIANT_NAME            - name of the variant, e.g.: BCM</li>
 *  </ul>
 **/
void checkoutHisTool(Object env) {
    CheckoutTools checkoutTools = new CheckoutTools()
    SWFTools swfTools = new SWFTools()

    println('[QAC.groovy][checkoutHisTool] Checkout HISReport START')

    env['PROJECT_VARIANT_NAME'] = env.PROJECT_NAME + '_' + env.VARIANT_NAME

    //check Basic project environment
    swfTools.checkBasicProjectSetup()
    env['HISREPORT_VERSION'] = swfTools.checkEnvVar('HISREPORT_VERSION', 'LATEST')

    // Get Repository from SCM
    env['HISREPORT_REPOSITORY'] = getScmPathHis(env.HISREPORT_VERSION)

    println(
        '[QAC.groovy][checkoutHisTool] Selected HISReport Version: ' +
        env.HISREPORT_VERSION)

    List checkoutList = []
    checkoutList << [
        env.HISREPORT_REPOSITORY, env.PROJECT_VARIANT_NAME + '/tools/HISReport/', 'UpdateWithCleanUpdater'
    ]

    checkoutTools.checkoutAll(Checkout.buildCheckout(env, '', checkoutList))

    println('[QAC.groovy][checkoutHisTool] Checkout HISReport END')
}

/**
 * getScmPathQarConverter is building SCM Repository path for QARConverter tool
 *
 * @param version Required version info: TRUNK, LATEST or tag name
 **/
String getScmPathQarConverter(String version) {
    // SCM base path
    String qarConverterRepository = 'https://debesvn001/kostal/lk_ae_internal/LK/QARConverter/'
    String qarConverterLatestVersion = 'QARConverter_v01_10'

    switch (version) {
        case 'TRUNK':
            //return trunk
            return qarConverterRepository + 'trunk/dist'
        case 'LATEST':
            //return LATEST
            return qarConverterRepository + 'tags/' + qarConverterLatestVersion + '/dist'
        case 'QARConverter_v01_00':
            //return trunk
            return qarConverterRepository + 'tags/QARConverter_v01_00/dist'
        case 'QARConverter_v01_10':
            //return trunk
            return qarConverterRepository + 'tags/QARConverter_v01_10/dist'
        default:
            println('[Doxygen.groovy][getScmPathQarConverter] no valid version selected, return LATEST')
            return qarConverterRepository + 'tags/' + qarConverterLatestVersion + '/dist'
        }
}

/**
 * checkoutQarConverterTool is getting QARConverter tool from SCM into Project directory
 * [TOOLS_PATH]/QARConverter
 *
 * @param env The Jenkins build environment. It must contain
 *            the following variables:
 *  <ul>
 *    <li>QARCONVERTER_VERSION  - (optional) QARConverter Version (TRUNK, LATEST(Default) or
 *                             specific Version Info) </li>
 *   <li>PROJECT_NAME            - name of the project, e.g.: P12345 </li>
 *   <li>VARIANT_NAME            - name of the variant, e.g.: BCM</li>
 *  </ul>
 **/
void checkoutQarConverterTool(Object env) {
    CheckoutTools checkoutTools = new CheckoutTools()
    SWFTools swfTools = new SWFTools()

    println('[QAC.groovy][checkoutQarConverterTool] Checkout QARConverter START')

    env['PROJECT_VARIANT_NAME'] = env.PROJECT_NAME + '_' + env.VARIANT_NAME

    //check Basic project environment
    swfTools.checkBasicProjectSetup()
    env['QARCONVERTER_VERSION'] = swfTools.checkEnvVar('QARCONVERTER_VERSION', 'LATEST')

    // Get Repository from SCM
    env['QARCONVERTER_REPOSITORY'] = getScmPathQarConverter(env.QARCONVERTER_VERSION)

    println(
        '[QAC.groovy][checkoutQarConverterTool] Selected QARConverter Version: ' +
        env.QARCONVERTER_VERSION)

    List checkoutList = []
    checkoutList << [
        env.QARCONVERTER_REPOSITORY, env.PROJECT_VARIANT_NAME + '/tools/QARConverter/', 'UpdateWithCleanUpdater'
    ]

    checkoutTools.checkoutAll(Checkout.buildCheckout(env, '', checkoutList))

    println('[QAC.groovy][checkoutQarConverterTool] Checkout QARConverter END')
}

/**
 * makeQac is calling qac make script via bat command.
 *
 * <p>
 * Typical make call is:
 * <ul>
 *  <li>[TOOLS_PATH]/make/[MAKE_TOOL_QAC] QAC [MAKE_VAR] -j[MAKE_CORE_NO_QAC]
 *      [buildTarget]</li>
 * </ul>
 * log file is stored to:
 * <ul>
 *  <li>[PROJECT_VARIANT_NAME]_QAC_[buildTarget].log</li>
 * </ul>
 * @param env The Jenkins build environment. It must contain
 *            the following variables:
 *  <ul>
 *   <li>ECLIPSE_VERSION         - The path where MSYS and MinGW are
 *                                 stored</li>
 *   <li>QAC_VERSION             - used QAC version [QAC/QAC9]</li>
 *   <li>PROJECT_NAME            - name of the project, e.g.: P12345 </li>
 *   <li>VARIANT_NAME            - name of the variant, e.g.: BCM</li>
 *   <li>COMPILER_PATH           - env var of compiler </li>
 *   <li>RECIPIENTS              - list of e-mail recipients </li>
 *   <li>QAC_PATH                - (optional) path to QAC7.2, Default: CI_QAC_72 </li>
 *   <li>QAC9_PATH               - (optional) path to QAC9, Default: C:/Tools/PRQA-Framework-2.1.0 </li>
 *   <li>TOOLS_PATH_MAKE         - (optional) The relative path where the Tools are located</li>
 *   <li>MAKE_VAR                - (optional) String buffer to add parameters to your build
 *                                 process as defined in you make tool</li>
 *   <li>MAKE_TOOL_QAC           - (optional) used make tool. Default:
 *                                 Make_qac.bat</li>
 *   <li>MAKE_CORE_NO_QAC        - (optional) No of cores to run QAC process.
 *                                 Default: 16</li>
 *  </ul>
 * @param buildTarget    build target parameter
 **/
@SuppressWarnings('BuilderMethodWithSideEffects') // Not a builder method
void makeQac(Object env, String buildTarget) {
    CommandBuilder commandBuilder = new CommandBuilder()
    SWFTools swfTools = new SWFTools()
    Utility swfUtility = new Utility()

    //check Basic project environment
    swfTools.checkBasicProjectSetup()
    env['TOOLS_PATH_MAKE'] = swfUtility.toWindowsPath(swfTools.checkEnvVar('TOOLS_PATH_MAKE', env['TOOLS_PATH']))

    env['PROJECT_VARIANT_NAME'] = env.PROJECT_NAME + '_' + env.VARIANT_NAME
    env['MAKE_TOOL_QAC'] = swfTools.checkEnvVar('MAKE_TOOL_QAC', 'make_qac.bat')
    env['MAKE_CORE_NO_QAC'] = swfTools.checkEnvVar('MAKE_CORE_NO_QAC', '16')
    env['MAKE_VAR'] = swfTools.checkEnvVar('MAKE_VAR', '')

    println('[Qac.groovy][makeQac] call QAC with following parameter')
    println(
        '[Qac.groovy][makeQac] Project Variant: ' + env.PROJECT_VARIANT_NAME)
    println('[Qac.groovy][makeQac] WORKSPACE: ' + env.WORKSPACE)
    println('[Qac.groovy][makeQac] Eclipse: ' + env.ECLIPSE_VERSION)
    println('[Qac.groovy][makeQac] Tools: ' + env.TOOLS_PATH_MAKE)
    println('[Qac.groovy][makeQac] make tool: ' + env.MAKE_TOOL_QAC)
    println('[Qac.groovy][makeQac] build mode: QAC')
    println('[Qac.groovy][makeQac] build target: ' + buildTarget)
    println('[Qac.groovy][makeQac] make var: ' + env.MAKE_VAR)
    println(
        '[SWFLibTools_build.groovy][swflib_make] make cores: ' +
        env.MAKE_CORE_NO_QAC)
    println('[Qac.groovy][makeQac] QAC 9 path: ' + env.QAC9_PATH)
    println('[Qac.groovy][makeQac] QAC 7.2 path: ' + env.QAC_PATH)
    println('[Qac.groovy][makeQac] QAC Version: ' + env.QAC_VERSION)

    bat commandBuilder.buildBat([
        '@echo on',
        "@set MINGW=%${env.ECLIPSE_VERSION}%/mingw/bin",
        "@set MSYS=%${env.ECLIPSE_VERSION}%/msys64/usr/bin",
        "@set PATH=%MINGW%;%MSYS%;%${env.QAC9_PATH}%",
        "@set USE_QAC_VERSION=${env.QAC_VERSION}",
        '@set CI_',
        '@set FLEXLM_TIMEOUT',
        '@set FLEXLM_BATCH',
        '@set WORKSPACE=%cd%\\SWF_Project',
        '@set MAKE_VERBOSE=VERBOSE_OFF',
        "cd %cd%\\SWF_Project\\${env.TOOLS_PATH_MAKE}\\make",
        "@echo @call ${env.MAKE_TOOL_QAC} QAC ${env.MAKE_VAR} " +
            "-j${env.MAKE_CORE_NO_QAC} ${buildTarget} 2>&1 > " +
        "${env.PROJECT_VARIANT_NAME}_QAC_${buildTarget}.log",
            "@call ${env.MAKE_TOOL_QAC} QAC ${env.MAKE_VAR} " +
            "-j${env.MAKE_CORE_NO_QAC} ${buildTarget} 2>&1 > " +
        "${env.PROJECT_VARIANT_NAME}_QAC_${buildTarget}.log"
    ])

    println(
        '[Qac.groovy][makeQac] QAC log written to: ' +
        env.PROJECT_VARIANT_NAME + '_QAC_' + buildTarget + '.log')

    copyOutputBuild(env, buildTarget)
}

/**
 * runQacReportBuildingToolchain is calling qac make script to build all
 * targets necessary for a QAC report
 *
 * <p>
 * Typical make script call is:
 * <ul>
 *  <li>-qac_all_rebuild</li>
 *  <li>-qac_report_all</li>
 * </ul>
 *
 * @param env The Jenkins build environment. Get necessary data from makeQac
 *
 **/
void runQacReportBuildingToolchain(Object env) {
    println(
        '[Qac.groovy][runQacReportBuildingToolchain] call qac ' +
        'make qac_all_rebuild')
    makeQac(env, 'qac_all_rebuild')

    println(
        '[Qac.groovy][runQacReportBuildingToolchain] call ' +
        'qac make qac_report_all')

    makeQac(env, 'qac_report_all')
}

/**
 * copyOutputBuild copies outputs of make call to Project template folder
 * <p>
 * Copied files are:
 * <ul>
 *  <li>QAC log file
 *      [TOOLS_PATH_MAKE]/Make/[PROJECT_VARIANT_NAME]_QAC_[buildTarget].log</li>
 * </ul>
 * @param env The Jenkins build environment. It must contain
 *            the following variables:
 *  <ul>
 *    <li>TOOLS_PATH_MAKE   - (optional) The relative path where the Tools are located</li>
 *    <li>PROJECT_NAME - name of the project, e.g.: P12345 </li>
 *    <li>VARIANT_NAME - name of the variant, e.g.: BCM</li>
 *  </ul>
 * @param buildTarget    build target parameter
 **/
void copyOutputBuild(Object env, String buildTarget) {
    CommandBuilder commandBuilder = new CommandBuilder()
    SWFTools swfTools = new SWFTools()
    Utility swfUtility = new Utility()

    env['PROJECT_VARIANT_NAME'] = env.PROJECT_NAME + '_' + env.VARIANT_NAME

    //check Basic project environment
    swfTools.checkBasicProjectSetup()
    env['TOOLS_PATH_MAKE'] = swfUtility.toWindowsPath(swfTools.checkEnvVar('TOOLS_PATH_MAKE', env['TOOLS_PATH']))

    println('[Qac.groovy][swflib_qac_copyoutput] copy files to temp workspace')

    bat commandBuilder.buildBat([
        '@echo off',
        "XCOPY \"%WORKSPACE%\\SWF_Project\\${env.TOOLS_PATH_MAKE}\\Make\\" +
            "${env.PROJECT_VARIANT_NAME}_QAC_${buildTarget}.log\" ^",

        "\"%WORKSPACE%\\${env.PROJECT_VARIANT_NAME}\\log\\QAC\\" +
            "${env.PROJECT_VARIANT_NAME}_QAC_${buildTarget}.log*\" /Y",

        "XCOPY \"%WORKSPACE%\\SWF_Project\\${env.TOOLS_PATH_MAKE}\\Make\\" +
            "${env.PROJECT_VARIANT_NAME}_QAC_${buildTarget}.log\" ^",

        "\"%WORKSPACE%\\${env.PROJECT_VARIANT_NAME}\\data\\QAC\\" +
            "input\\${env.PROJECT_VARIANT_NAME}_QAC_${buildTarget}.log*\" /Y"
    ])

    println(
        '[Qac.groovy][swflib_qac_copyoutput] copy files to temp ' +
        'workspace done')
}

/**
 * parseOutput control module to call MISRA/QAC and HIS parser sub-modules
 **/
void parseOutput(Object env) {
    println('[Qac.groovy][parseOutput] START parsing')
    parseHis(env)
    parseMisraQac(env)
    println('[Qac.groovy][parseOutput] END parsing')
}

/**
 * convertOutput is controlling converting processes of MISRA/QAC output files
 * @param env The Jenkins build environment. It must contain
 *            the following variables:
 *  <ul>
 *    <li>TOOLS_PATH_PRQA   - (optional) The relative path where the Tools are located, Default: Tools</li>
 *    <li>QAC_VERSION       - used QAC version [QAC/QAC9]</li>
 *  </ul>
 **/
void convertOutput(Object env) {
    SWFTools swfTools = new SWFTools()
    Utility swfUtility = new Utility()
    println('[Qac.groovy][convertOutput] START converting')

    //check Basic project environment
    swfTools.checkBasicProjectSetup()
    env['TOOLS_PATH_PRQA'] = swfUtility.toWindowsPath(swfTools.checkEnvVar('TOOLS_PATH_PRQA', env['TOOLS_PATH']))

    if (env.QAC_VERSION == 'QAC9') {
        String qarFile = (
            "SWF_Project\\${env.TOOLS_PATH_PRQA}\\prqaconfig\\prqa" +
            '\\output\\qac_cli_output.qar')
        convertQarReport(env, qarFile)
    }

    println('[Qac.groovy][convertOutput] END converting')
}

/**
 * parseMisraQac is calling MISRA/QAC parser
 * <p>
 * Following options are available with parameter QAC_VERSION:
 * <ul>
 *  <li>QAC:  QAC7.2, MISRA2004 > parser: LK_QAC_Parser_Priority</li>
 *  <li>QAC9:  QAC9, MISRA2012 > parser: LK_MISRA2012_QAC_Parser</li>
 * </ul>
 * <p>
 * @param env The Jenkins build environment. It must contain
 *            the following variables:
 *  <ul>
 *    <li>PROJECT_NAME - name of the project, e.g.: P12345 </li>
 *    <li>VARIANT_NAME - name of the variant, e.g.: BCM</li>
 *    <li>QAC_VERSION  - used QAC version [QAC/QAC9]</li>
 *    <li>TOOLS_PATH_PRQA   - (optional) The relative path where the Tools are located, Default: Tools</li>
 *  </ul>
 **/
void parseMisraQac(Object env) {
    CITools ciTools = new CITools()
    SWFTools swfTools = new SWFTools()
    Utility swfUtility = new Utility()
    env['PROJECT_VARIANT_NAME'] = env.PROJECT_NAME + '_' + env.VARIANT_NAME

    //check Basic project environment
    swfTools.checkBasicProjectSetup()
    env['TOOLS_PATH_PRQA'] = swfUtility.toWindowsPath(swfTools.checkEnvVar('TOOLS_PATH_PRQA', env['TOOLS_PATH']))

    println('[Qac.groovy][parseMisraQac] START parsing MISRA/QAC')

    if (env.QAC_VERSION == 'QAC') {
        String misraQacPattern = (
            "${env.PROJECT_VARIANT_NAME}/data/QAC/input/" +
            "${env.PROJECT_VARIANT_NAME}_QAC_qac_all_rebuild.log")

        println('[Qac.groovy][parseMisraQac] QAC: 7.2')
        println('[Qac.groovy][parseMisraQac] MISRA: 2004')
        println('[Qac.groovy][parseMisraQac] Parser: LK_QAC_Parser_Priority')
        println('[Qac.groovy][parseHis] file: ' + misraQacPattern)

        ciTools.checkForCompilerWarnings(
            misraQacPattern, 'LK_QAC_Parser_Priority')
    }
    else if (env.QAC_VERSION == 'QAC9') {
        String misraQacPattern = (
            "SWF_Project/${env.TOOLS_PATH_PRQA}/prqaconfig/prqa/output" +
            '/qac_cli_output.qar')

        println('[Qac.groovy][parseMisraQac] QAC: 9')
        println('[Qac.groovy][parseMisraQac] MISRA: 2012')
        println('[Qac.groovy][parseMisraQac] Parser: LK_MISRA2012_QAC_Parser')
        println('[Qac.groovy][parseHis] file: ' + misraQacPattern)

        ciTools.checkForCompilerWarnings(
            misraQacPattern, 'LK_MISRA2012_QAC_Parser')
    }
    else {
        println(
            '[Qac.groovy][parseMisraQac] WARNING: no valid MISRA/QAC ' +
            'parser configured! > check parameter QAC_VERSION')
    }

    println('[Qac.groovy][parseMisraQac] END parsing MISRA/QAC')
}

/**
 * parseHis is calling HIS parser
 * <p>
 * Following options are available with parameter QAC_VERSION:
 * <ul>
 *  <li>- QAC:  QAC7.2, MISRA2004 > parser: LK_QAC9_HIS_Metric_Parser</li>
 *  <li>- QAC9:  QAC9, MISRA2012 > parser: LK_QAC9_HIS_Metric_Parser</li>
 * </ul>
 * <p>
 * @param env The Jenkins build environment. It must contain
 *            the following variables:
 *  <ul>
 *    <li>PROJECT_NAME - name of the project, e.g.: P12345 </li>
 *    <li>VARIANT_NAME - name of the variant, e.g.: BCM</li>
 *    <li>QAC_VERSION  - used QAC version [QAC/QAC9]</li>
 *    <li>TOOLS_PATH_PRQA - The relative path where the Tools are located</li>
 *  </ul>
 **/
void parseHis(Object env) {
    CITools ciTools = new CITools()
    SWFTools swfTools = new SWFTools()
    Utility swfUtility = new Utility()
    env['PROJECT_VARIANT_NAME'] = env.PROJECT_NAME + '_' + env.VARIANT_NAME

    //check Basic project environment
    swfTools.checkBasicProjectSetup()
    env['TOOLS_PATH_PRQA'] = swfUtility.toWindowsPath(swfTools.checkEnvVar('TOOLS_PATH_PRQA', env['TOOLS_PATH']))

    println('[Qac.groovy][parseHis] START parsing HIS')

    if (env.QAC_VERSION == 'QAC') {
        String hisPattern = (
            "${env.PROJECT_VARIANT_NAME}/data/QAC/input/" +
            "${env.PROJECT_VARIANT_NAME}_QAC_qac_all_rebuild.log")

        println('[Qac.groovy][parseHis] QAC: 7.2')
        println('[Qac.groovy][parseHis] MISRA: 2004')
        println('[Qac.groovy][parseHis] Parser: LK_QAC9_HIS_Metric_Parser')
        println('[Qac.groovy][parseHis] file: ' + hisPattern)

        ciTools.checkForCompilerWarnings(
            hisPattern, 'LK_QAC9_HIS_Metric_Parser')
    }
    else if (env.QAC_VERSION == 'QAC9') {
        String hisPattern = (
            "SWF_Project/${env.TOOLS_PATH_PRQA}/prqaconfig/prqa/" +
            'output/qac_cli_output.qar')

        println('[Qac.groovy][parseHis] QAC: 9')
        println('[Qac.groovy][parseHis] MISRA: 2012')
        println('[Qac.groovy][parseHis] Parser: LK_QAC9_HIS_Metric_Parser')
        println('[Qac.groovy][parseHis] file: ' + hisPattern)

        ciTools.checkForCompilerWarnings(
            hisPattern, 'LK_QAC9_HIS_Metric_Parser')
    }
    else {
        println(
            '[Qac.groovy][parseHis] WARNING: no valid MISRA/QAC ' +
            'parser configured! > check parameter QAC_VERSION')
    }
    println('[Qac.groovy][parseHis] END parsing HIS')
}

/**
 * parseHis is calling HIS violation parser
 * <p>
 * Following options are available with parameter QAC_VERSION:
 * <ul>
 *  <li>QAC:  not available</li>
 *  <li>QAC9: HISReport</li>
 * </ul>
 * <p>
 * @param env The Jenkins build environment. It must contain
 *            the following variables:
 *  <ul>
 *    <li>PROJECT_NAME - name of the project, e.g.: P12345 </li>
 *    <li>VARIANT_NAME - name of the variant, e.g.: BCM</li>
 *    <li>QAC_VERSION  - used QAC version [QAC/QAC9]</li>
 *  </ul>
 **/
void parseHisViolation(Object env) {
    env['PROJECT_VARIANT_NAME'] = env.PROJECT_NAME + '_' + env.VARIANT_NAME

    if (env.QAC_VERSION == 'QAC') {
        println(
            '[Qac.groovy][parseHisViolation] No HIS vioaltion Report ' +
            'available with option: QAC')
    }
    else if (env.QAC_VERSION == 'QAC9') {
        String hisPattern = (
            "${env.PROJECT_VARIANT_NAME}\\data\\QAC\\" +
            'output\\prqaconfig_MDR.xml')

        println('[Qac.groovy][parseHisViolation] START parsing HIS violation')
        println('[Qac.groovy][parseHisViolation] file: ' + hisPattern)

        buildHisReport(env, hisPattern)
        println('[Qac.groovy][parseHisViolation] END parsing HIS violation')
    }
    else {
        println(
            '[Qac.groovy][parseHisViolation] WARNING: no valid MISRA/QAC ' +
            'parser configured! > check parameter QAC_VERSION')
    }
}

/**
 * buildHisReport is building HIS-Violation report out of PRQA MDR-Report
 *
 * @param env The Jenkins build environment. It must contain
 *            the following variables:
 *  <ul>
 *    <li>PROJECT_NAME - name of the project, e.g.: P12345 </li>
 *    <li>VARIANT_NAME - name of the variant, e.g.: BCM</li>
 *  </ul>
 * @param prqaReportFile PRQA MDR-Report file
 *
**/
@SuppressWarnings('BuilderMethodWithSideEffects') // Not a builder method
void buildHisReport(Object env, String prqaReportFile) {
    CommandBuilder commandBuilder = new CommandBuilder()

    env['PROJECT_VARIANT_NAME'] = env.PROJECT_NAME + '_' + env.VARIANT_NAME

    checkoutHisTool(env)

    println('[Qac.groovy][buildHisReport] Copy files to HIS Tool')
    bat commandBuilder.buildBat([
        '@echo on',
        '@set WORKSPACE=%cd%',
        "XCOPY \"%WORKSPACE%\\${prqaReportFile}\" ^",
        "\"%WORKSPACE%\\${env.PROJECT_VARIANT_NAME}\\tools\\" +
            'HISReport\\input\\prqaconfig_MDR.xml*" /s /i /Y'
    ])

    println('[Qac.groovy][buildHisReport] Call HIS Tool')
    bat commandBuilder.buildBat([
        '@echo on',
        "@set WORKSPACE=${env.PROJECT_VARIANT_NAME}\\tools\\HISReport",
        '@echo %WORKSPACE%',
        "@echo '${env.PROJECT_VARIANT_NAME}\\tools\\" +
            'HISReport\\input\\prqaconfig_MDR.xml',
        "@echo '${env.PROJECT_VARIANT_NAME}\\tools\\" +
            'HISReport\\output\\HISMetrics.html',
        "@call ${env.PROJECT_VARIANT_NAME}\\tools\\HISReport\\HISReport.exe " +
            "--i ${env.PROJECT_VARIANT_NAME}\\tools\\HISReport\\" +
            'input\\prqaconfig_MDR.xml' + ' ' +
            "--o ${env.PROJECT_VARIANT_NAME}\\tools\\HISReport\\" +
            'output\\HISMetrics.html'
    ])

    println('[Qac.groovy][buildHisReport] Get Report files from HIS Tool')

    bat commandBuilder.buildBat([
        '@echo on',
        '@set WORKSPACE=%cd%',
        '@echo %WORKSPACE%',
        "XCOPY \"${env.PROJECT_VARIANT_NAME}\\tools\\HISReport\\output\\*\" ^",
        "\"%WORKSPACE%\\${env.PROJECT_VARIANT_NAME}\\report\\QAC\" /s /I /Y"
    ])
}

/**
 * convertQarReport is converting qar file Reports to csv by usage of
 * QARConverter tools
 * @param env The Jenkins build environment. It must contain
 *            the following variables:
 *  <ul>
 *    <li>PROJECT_NAME  - name of the project, e.g.: P12345 </li>
 *    <li>VARIANT_NAME  - name of the variant, e.g.: BCM</li>
 *  </ul>
 * @param qarFile qar file
 *
**/
void convertQarReport(Object env, String qarFile) {
    CommandBuilder commandBuilder = new CommandBuilder()

    env['PROJECT_VARIANT_NAME'] = env.PROJECT_NAME + '_' + env.VARIANT_NAME

    // checkout QARConverter into tools folder
    checkoutQarConverterTool(env)

    println('[Qac.groovy][convertQarReport] Copy files to QARConverter tool')

    bat commandBuilder.buildBat([
        '@echo on',
        '@set WORKSPACE=%cd%',
        "XCOPY \"%WORKSPACE%\\${qarFile}\" ^",
        "\"${env.PROJECT_VARIANT_NAME}\\tools\\QARConverter\\input\\qac_cli_output.qar*\" /s /i /Y"
    ])

    println('[Qac.groovy][convertQarReport] Call QARConverter Tool')

    bat commandBuilder.buildBat([
        '@echo on',
        '@set WORKSPACE=%cd%',
        '@echo %WORKSPACE%',
        "@echo '${env.PROJECT_VARIANT_NAME}\\tools\\QARConverter\\input\\qac_cli_output.qar'",
        "@echo '${env.PROJECT_VARIANT_NAME}\\tools\\QARConverter\\output\\HIS.csv'",
        "@echo '${env.PROJECT_VARIANT_NAME}\\tools\\QARConverter\\output\\QAC.csv'",
        "@call ${env.PROJECT_VARIANT_NAME}\\tools\\QARConverter\\QARConverterHIS.exe -S " +
            "${env.PROJECT_VARIANT_NAME}\\tools\\QARConverter\\input\\qac_cli_output.qar -O " +
            "${env.PROJECT_VARIANT_NAME}\\tools\\QARConverter\\output\\HIS.csv",

        "@call ${env.PROJECT_VARIANT_NAME}\\tools\\QARConverter\\QARConverterQAC.exe -S " +
            "${env.PROJECT_VARIANT_NAME}\\tools\\QARConverter\\input\\qac_cli_output.qar -O " +
            "${env.PROJECT_VARIANT_NAME}\\tools\\QARConverter\\output\\QAC.csv"
    ])

    println(
        '[Qac.groovy][convertQarReport] Get converted files from ' +
        'QARConverter Tool')

    bat commandBuilder.buildBat([
        '@echo on',
        '@set WORKSPACE=%cd%',
        '@echo %WORKSPACE%',
        "XCOPY \"${env.PROJECT_VARIANT_NAME}\\tools\\QARConverter\\output\\*\" ^",
        "\"%WORKSPACE%\\${env.PROJECT_VARIANT_NAME}\\report\\QAC\" /s /I /Y"
    ])
}

/**
 * copyOutputReports copies outputs of MISRA/QAC+HIS parser into Project Temp
 * workspace [PROJECT_VARIANT_NAME]/data/QAC
 *
 * @param env The Jenkins build environment. It must contain
 *            the following variables:
 *  <ul>
 *    <li>PROJECT_NAME - name of the project, e.g.: P12345 </li>
 *    <li>VARIANT_NAME - name of the variant, e.g.: BCM</li>
 *    <li>QAC_VERSION  - used QAC version [QAC/QAC9]</li>
 *    <li>TOOLS_PATH_QAC   - (optional) The relative path where the Tools are located</li>
 *    <li>TOOLS_PATH_PRQA   - (optional) The relative path where the Tools are located, Default: Tools</li>
 *  </ul>
 **/
void copyOutputReports(Object env) {
    CommandBuilder commandBuilder = new CommandBuilder()
    SWFTools swfTools = new SWFTools()
    Utility swfUtility = new Utility()

    env['PROJECT_VARIANT_NAME'] = env.PROJECT_NAME + '_' + env.VARIANT_NAME

    //check Basic project environment
    swfTools.checkBasicProjectSetup()
    env['TOOLS_PATH_QAC'] = swfUtility.toWindowsPath(swfTools.checkEnvVar('TOOLS_PATH_QAC', env['TOOLS_PATH']))
    env['TOOLS_PATH_PRQA'] = swfUtility.toWindowsPath(swfTools.checkEnvVar('TOOLS_PATH_PRQA', env['TOOLS_PATH']))

    println(
        '[Qac.groovy][copyOutputReports] START copy report files ' +
        'for MISRA/QAC+HIS')

    if (env.QAC_VERSION == 'QAC') {
        bat commandBuilder.buildBat([
            '@echo off',
            "XCOPY \"%WORKSPACE%\\SWF_Project\\${env.TOOLS_PATH_QAC}\\QAC\\" +
                'report\\*.qar" ^',

            "\"%WORKSPACE%\\${env.PROJECT_VARIANT_NAME}\\report\\QAC\" /I /Y",
            "XCOPY \"%WORKSPACE%\\SWF_Project\\${env.TOOLS_PATH_QAC}\\QAC\\" +
                'report\\*.qar" ^',

            "\"%WORKSPACE%\\${env.PROJECT_VARIANT_NAME}\\data\\QAC" +
                '\\output" /I /Y'
         ])
    }
    else if (env.QAC_VERSION == 'QAC9') {
        String prefix = (
            "XCOPY \"%WORKSPACE%\\SWF_Project\\${env.TOOLS_PATH_PRQA}\\" +
            'prqaconfig\\prqa\\reports\\prqaconfig_'
        )

        bat commandBuilder.buildBat([
            '@echo off',
            "${prefix}CRR_*.html\" ^",
            "\"%WORKSPACE%\\${env.PROJECT_VARIANT_NAME}\\report\\QAC" +
                '\\prqaconfig_CRR.html*" /Y',

            "${prefix}RCR_*.html\" ^",
            "\"%WORKSPACE%\\${env.PROJECT_VARIANT_NAME}\\report\\QAC" +
                '\\prqaconfig_RCR.html*" /Y',

            "${prefix}SUR_*.html\" ^",
            "\"%WORKSPACE%\\${env.PROJECT_VARIANT_NAME}\\report\\QAC\\" +
                'prqaconfig_SUR.html*" /Y',

            "${prefix}MDR_*.xml\" ^",
            "\"%WORKSPACE%\\${env.PROJECT_VARIANT_NAME}\\report\\QAC\\" +
                'prqaconfig_MDR.xml*" /Y',

            "XCOPY \"%WORKSPACE%\\SWF_Project\\${env.TOOLS_PATH_PRQA}\\" +
                'prqaconfig\\prqa\\output\\qac_cli_output.qar" ^',

            "\"%WORKSPACE%\\${env.PROJECT_VARIANT_NAME}\\report\\QAC" +
                '\\qac_cli_output.qar*" /Y',

            "${prefix}CRR_*.html\" ^",
            "\"%WORKSPACE%\\${env.PROJECT_VARIANT_NAME}\\data\\QAC\\" +
                'output\\prqaconfig_CRR.html*" /Y',

            "${prefix}RCR_*.html\" ^",
            "\"%WORKSPACE%\\${env.PROJECT_VARIANT_NAME}\\data\\QAC\\" +
                'output\\prqaconfig_RCR.html*" /Y',

            "${prefix}SUR_*.html\" ^",
            "\"%WORKSPACE%\\${env.PROJECT_VARIANT_NAME}\\data\\QAC\\" +
                'output\\prqaconfig_SUR.html*" /Y',

            "${prefix}MDR_*.xml\" ^",
            "\"%WORKSPACE%\\${env.PROJECT_VARIANT_NAME}\\data\\QAC\\" +
                'output\\prqaconfig_MDR.xml*" /Y',

            "XCOPY \"%WORKSPACE%\\SWF_Project\\${env.TOOLS_PATH_PRQA}\\" +
                'prqaconfig\\prqa\\output\\qac_cli_output.qar" ^',

            "\"%WORKSPACE%\\${env.PROJECT_VARIANT_NAME}\\data\\QAC\\" +
                'output\\qac_cli_output.qar*" /Y'
         ])
    }
    else {
        println(
            '[Qac.groovy][copyOutputReports] WARNING: no valid MISRA/QAC ' +
            'parser configured! > check parameter QAC_VERSION')
    }

    println(
        '[Qac.groovy][copyOutputReports] END copy report files ' +
        'for MISRA/QAC+HIS')
}

/**
 * archivingQac is archiving outputs of MISRA/QAC stage
 * <p>
 * Following folders are archived:
 * <ul>
 *  <li>-[PROJECT_VARIANT_NAME]/data/QAC/*</li>
 *  <li>-[PROJECT_VARIANT_NAME]/log/QAC/*</li>
 *  <li>-[PROJECT_VARIANT_NAME]/report/QAC/*</li>
 * </ul>
 * @param env The Jenkins build environment. It must contain
 *            the following variables:
 *  <ul>
 *    <li>PROJECT_NAME - name of the project, e.g.: P12345 </li>
 *    <li>VARIANT_NAME - name of the variant, e.g.: BCM</li>
 *  </ul>
 **/
void archivingQac(Object env) {
    CommonTools commonTools = new CommonTools()

    env['PROJECT_VARIANT_NAME'] = env.PROJECT_NAME + '_' + env.VARIANT_NAME

    println('[Qac.groovy][archivingQac] Archiving START')
    commonTools.archiveArtifacts(env.PROJECT_VARIANT_NAME + '/data/QAC/**/*')
    commonTools.archiveArtifacts(env.PROJECT_VARIANT_NAME + '/log/QAC/**/*')
    commonTools.archiveArtifacts(env.PROJECT_VARIANT_NAME + '/report/QAC/**/*')
    println('[Qac.groovy][archivingQac] Archiving END')
}

/**
 * stashingQac is stashing outputs of MISRA/QAC stage
 * Following folders are stashed:
 * <ul>
 *  <li>swflib_qac_report: [PROJECT_VARIANT_NAME]/report/QAC/*</li>
 * </ul>
 * @param env The Jenkins build environment. It must contain
 *            the following variables:
 *  <ul>
 *    <li>PROJECT_NAME - name of the project, e.g.: P12345 </li>
 *    <li>VARIANT_NAME - name of the variant, e.g.: BCM</li>
 *  </ul>
 **/
void stashingQac(Object env) {
    env['PROJECT_VARIANT_NAME'] = env.PROJECT_NAME + '_' + env.VARIANT_NAME

    println('[Qac.groovy][stashingQac] stashing qac output START')
    stash([
        includes: env.PROJECT_VARIANT_NAME + '/report/QAC/**/*',
        name: 'swflib_qac_report'
    ])
    println(
        '[Qac.groovy][stashingQac] Stash swflib_qac_report for ' +
        'MISRA/QAC+HIS reports done.')
    println('[Qac.groovy][stashingQac] stashing qac output END')
}

/**
 * buildMisraQacHis to build a SW-Factory Lib MISRA/QAC Report.
 * <p>
 * Following pipeline stages are created:
 * <ul>
 *  <li>- MISRA/QAC Report</li>
 * </ul>
 * <p>
 * @param env The Jenkins build environment. It must contain
 *            the following variables:
 *  <ul>
 *    <li>ECLIPSE_VERSION  - The path where MSYS and MinGW are stored</li>
 *    <li>TOOLS_PATH_QAC   - (optional) The relative path where the Tools are located</li>
 *    <li>TOOLS_PATH_PRQA  - (optional) The relative path where the Tools are located, Default: Tools</li>
 *    <li>TOOLS_PATH_MAKE  - (optional) The relative path where the Tools are located</li>
 *    <li>PROJECT_NAME     - name of the project, e.g.: P12345 </li>
 *    <li>VARIANT_NAME     - name of the variant, e.g.: BCM</li>
 *    <li>MAKE_VAR         - (optional) String buffer to add parameters to your build
 *                           process as defined in you make tool</li>
 *    <li>MAKE_TOOL_QAC    - (optional) used make tool. Default:
 *                           Make_qac.bat</li>
 *    <li>MAKE_CORE_NO_QAC - (optional) No of cores to run QAC process.
 *                           Default: 16</li>
 *    <li>QAC9_PATH        - (optional) path to QAC9 folder</li>
 *    <li>QAC_PATH         - (optional) path to QAC7.2 folder</li>
 *    <li>QAC_VERSION      - used QAC version [QAC/QAC9]</li>
 *    <li>QARCONVERTER_VERSION    -(optional)  QARConverter Version (TRUNK, LATEST(Default) or
 *                             specific Version Info) </li>
 *    <li>HISREPORT_VERSION    -(optional)  HISReport Version (TRUNK, LATEST(Default) or
 *                             specific Version Info) </li>
 *    <li>QAC_ANALYSIS     - Activation of MISRA/QAC [ON/OFF]</li>
 *  </ul>
 **/
@SuppressWarnings('BuilderMethodWithSideEffects') // Not a builder method
void buildMisraQacHis(Object env = this.env) {
    Notifier notifier = new Notifier()
    SWFTools swfTools = new SWFTools()

    if (swfTools.checkON(env.QAC_ANALYSIS)) {
        stage('MISRA/QAC Report') {
            // verify Configuration first
            if (!isQacConfigurationValid(env)) {
                // Config Error, throw error
                error 'Qac failed due to an incorrect configuration of the environment variables. ' +
                '-> Check the environment variables in the job configuration.'
            }
            try {
                println('[Qac.groovy][buildMisraQacHis] MISRA/QAC start.')
                runQacReportBuildingToolchain(env)
                parseOutput(env)
                copyOutputReports(env)
                convertOutput(env)
                parseHisViolation(env)
                archivingQac(env)
                stashingQac(env)
                println('[Qac.groovy][buildMisraQacHis] MISRA/QAC end.')
            } catch (e) {
                //Print error message
                println(
                    '[Qac.groovy][buildMisraQacHis] buildMisraQacHis failed.')
                notifier.onFailure(env, e)
            }
        }
    }
    else {
        //default is release
        env['QAC_ANALYSIS'] = 'OFF'
        println('[Qac.groovy][buildMisraQacHis] MISRA/QAC deactivated.')
    }
}
