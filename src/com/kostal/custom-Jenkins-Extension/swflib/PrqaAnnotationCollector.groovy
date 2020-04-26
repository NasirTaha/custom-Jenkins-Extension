/*
 * SW-Factory Library module to analyze Compiler Warnings
*/
package com.kostal.pipelineworks.swflib

import com.kostal.pipelineworks.CommandBuilder
import com.kostal.pipelineworks.CommonTools
import com.kostal.pipelineworks.CheckoutTools
import com.kostal.pipelineworks.Checkout
import com.kostal.pipelineworks.CITools
import com.kostal.pipelineworks.Utility

/**
 * isPrqaAnColConfigurationValid is verifiying PrqaAnnotationCollector tool config by checking env vars
 *
 * @param env The Jenkins build environment. It must contain
 *            the following variables:
 *  <ul>
 *    <li>PROJECT_NAME            - name of the project, e.g.: P12345 </li>
 *    <li>VARIANT_NAME            - name of the variant, e.g.: BCM</li>
 *    <li>PRQA_ANNOTATION_COLLECTOR_VERSION    - PrqaAnnotationCollector Version (TRUNK, LATEST(Default) or
 *                             specific Version Info) </li>
 *    <li>TOOLS_PATH_PRQA_ANNOTATION_COLLECTOR - The relative path where the
 *                     Tools are located in
 *                     project path. Default: Tools</li>
 *  </ul>
 **/
boolean isPrqaAnColConfigurationValid(Object env) {
    SWFTools swfTools = new SWFTools()

    println('[PrqaAnnotationCollector.groovy][isPrqaAnColConfigurationValid] Verify ' +
        'PrqaAnnotationCollector Config START')

    boolean configStatus = swfTools.isConfigurationValid(env,
        ['PROJECT_NAME', 'VARIANT_NAME'])

    //check Basic project environment
    swfTools.checkBasicProjectSetup()
    env['TOOLS_PATH_PRQA_ANNOTATION_COLLECTOR'] = swfTools.checkEnvVar('TOOLS_PATH_PRQA_ANNOTATION_COLLECTOR',
                                                                        env['TOOLS_PATH'])
    env['PRQA_ANNOTATION_COLLECTOR_VERSION'] = swfTools.checkEnvVar('PRQA_ANNOTATION_COLLECTOR_VERSION', 'LATEST')

    println('[PrqaAnnotationCollector.groovy][isPrqaAnColConfigurationValid] Verify PrqaAnnotationCollector Config END')

    return configStatus
}

/**
 * getScmPath is building SCM Repository path for PrqaAnnotationCollector tool
 *
 * @param version Required version info: TRUNK, LATEST or tag name
 **/
String getScmPath(String version) {
    // SCM base path
    String prqaAnnotationCollectorRepository = 'https://debesvn001/kostal/lk_ae_internal/LK/PrqaAnnotationCollector/'
    String prqaAnnotationCollectorLatestVersion = 'V5.0.2'

    switch (version) {
        case 'TRUNK':
            //return trunk
            return prqaAnnotationCollectorRepository + 'trunk/dist'
        case 'LATEST':
            //return LATEST
            return prqaAnnotationCollectorRepository + 'tags/' + prqaAnnotationCollectorLatestVersion + '/dist'
        case 'V5.0.0':
            //return trunk
            return prqaAnnotationCollectorRepository + 'tags/V5.0.0/dist'
        case 'V5.0.1':
            //return trunk
            return prqaAnnotationCollectorRepository + 'tags/V5.0.1/dist'
        case 'V5.0.2':
            //return trunk
            return prqaAnnotationCollectorRepository + 'tags/V5.0.2/dist'
        default:
            println('[PrqaAnnotationCollector.groovy][getScmPath] no valid version selected, return LATEST')
            return prqaAnnotationCollectorRepository + 'tags/' + prqaAnnotationCollectorLatestVersion + '/dist'
        }
}

/**
 * checkoutPrqaAnnotationCollectorTool is getting PrqaAnnotationCollector tool from SCM into Project directory
 * [TOOLS_PATH]/PrqaAnnotationCollector
 *
 * @param env The Jenkins build environment. It must contain
 *            the following variables:
 *  <ul>
 *    <li>PROJECT_NAME            - name of the project, e.g.: P12345 </li>
 *    <li>VARIANT_NAME            - name of the variant, e.g.: BCM</li>
 *    <li>PRQA_ANNOTATION_COLLECTOR_VERSION    - PrqaAnnotationCollector Version (TRUNK, LATEST(Default) or
 *                             specific Version Info) </li>
 *  </ul>
 **/
void checkoutPrqaAnnotationCollectorTool(Object env) {
    CheckoutTools checkoutTools = new CheckoutTools()

    println('[PrqaAnnotationCollector.groovy][checkoutPrqaAnnotationCollectorTool] ' +
        'Checkout PrqaAnnotationCollector START')

    env['PROJECT_VARIANT_NAME'] = env.PROJECT_NAME + '_' + env.VARIANT_NAME

    // Get Repository from SCM
    env['PRQA_ANNOTATION_COLLECTOR_REPOSITORY'] = getScmPath(env.PRQA_ANNOTATION_COLLECTOR_VERSION)

    println(
        '[PrqaAnnotationCollector.groovy][checkoutPrqaAnnotationCollectorTool] Selected PrqaAnnotationCollector ' +
        ' Version: ' + env.PRQA_ANNOTATION_COLLECTOR_VERSION)

    List checkoutList = []
    checkoutList << [
        env.PRQA_ANNOTATION_COLLECTOR_REPOSITORY, env.PROJECT_VARIANT_NAME + '/tools/PrqaAnnotationCollector/',
        'UpdateWithCleanUpdater'
    ]

    checkoutTools.checkoutAll(Checkout.buildCheckout(env, '', checkoutList))

    println('[PrqaAnnotationCollector.groovy][checkoutPrqaAnnotationCollectorTool] Checkout ' +
        'PrqaAnnotationCollector END')
}

/**
 * copyTool to copy PrqaAnnotationCollector tool into Project directory
 * [TOOLS_PATH]/PrqaAnnotationCollector
 *
 * @param env The Jenkins build environment. It must contain
 *            the following variables:
 *  <ul>
 *    <li>TOOLS_PATH_PRQA_ANNOTATION_COLLECTOR - The relative path where the
 *                     Tools are located in
 *                     project path. Default: Tools</li>
 *  </ul>
 **/
void copyTool(Object env) {
    CommandBuilder commandBuilder = new CommandBuilder()
    SWFTools swfTools = new SWFTools()

    //check Basic project environment
    swfTools.checkBasicProjectSetup()
    env['TOOLS_PATH_PRQA_ANNOTATION_COLLECTOR'] = swfTools.checkEnvVar('TOOLS_PATH_PRQA_ANNOTATION_COLLECTOR',
                                                                        env['TOOLS_PATH'])

    println(
        '[PrqaAnnotationCollector.groovy][copyTool] Copy PrqaAnnotationCollector Tool into project')

    bat commandBuilder.buildBat([
        "XCOPY \"${env.PROJECT_VARIANT_NAME}\\tools\\PrqaAnnotationCollector\\core\\*\" ^",
        "\"%WORKSPACE%\\SWF_Project\\${env.TOOLS_PATH_PRQA_ANNOTATION_COLLECTOR}\\PrqaAnnotationCollector\" /s /i /Y"
    ])
}

/**
 * initPrqaAnnotationCollector is preparing PrqaAnnotationCollector
 *
 * @param env The Jenkins build environment. It must contain
 *            the following variables:
 * <ul>
 *    <li>TOOLS_PATH_PRQA_ANNOTATION_COLLECTOR - (optional) The relative path
 *                     where the Tools are located in
 *                     project path. Default: Tools</li>
 * </ul>
 **/
void initPrqaAnnotationCollector(Object env) {
    //import library tools
    CommandBuilder commandBuilder = new CommandBuilder()
    SWFTools swfTools = new SWFTools()

    println(
        '[PrqaAnnotationCollector.groovy][initPrqaAnnotationCollector] Init PrqaAnnotationCollector START.')

    //check Basic project environment
    swfTools.checkBasicProjectSetup()
    env['TOOLS_PATH_PRQA_ANNOTATION_COLLECTOR'] = swfTools.checkEnvVar('TOOLS_PATH_PRQA_ANNOTATION_COLLECTOR',
                                                                        env['TOOLS_PATH'])

    bat commandBuilder.buildBat([
        '@echo on',
        "cd \"%WORKSPACE%\\SWF_Project\\${env.TOOLS_PATH_PRQA_ANNOTATION_COLLECTOR}\\PrqaAnnotationCollector\\",
        'if exist .\\report (@rmdir /S /Q .\\report)',
        'mkdir .\\report'
    ])

    println(
        '[PrqaAnnotationCollector.groovy][initPrqaAnnotationCollector] Init PrqaAnnotationCollector END.')
}

/**
 * callPrqaAnnotationCollector is calling PrqaAnnotationCollector executable tool
 *
 * @param env The Jenkins build environment. It must contain
 *            the following variables:
 * <ul>
 *    <li>TOOLS_PATH_PRQA_ANNOTATION_COLLECTOR - The relative path where the
 *                     Tools are located in
 *                     project path. Default: Tools</li>
 *    <li>SOURCES_PATH         - (optional) Relative path to the Sources folder</li>
 *    <li>PRQA_ANNOTATION_COLLECTOR_PAR_l      - (optional) PRQA AnnotationCollector Parameter l,
 *            Default: 20</li>
 *    <li>PRQA_ANNOTATION_COLLECTOR_BLACKLIST  - (optional) Blacklist to use with PRQA Toolchain, Default:
 *            ..\\[TOOLS_PATH_MAKE]\\config\\qac_blacklist.mk </li>
 * </ul>
 **/
void callPrqaAnnotationCollector(Object env) {
    //import library tools
    CommandBuilder commandBuilder = new CommandBuilder()
    SWFTools swfTools = new SWFTools()
    Utility swfUtility = new Utility()

    println(
        '[PrqaAnnotationCollector.groovy][callPrqaAnnotationCollector] Call PrqaAnnotationCollector START.')

    //check Basic project environment
    swfTools.checkBasicProjectSetup()
    env['TOOLS_PATH_MAKE'] = swfUtility.toWindowsPath(swfTools.checkEnvVar('TOOLS_PATH_MAKE', env['TOOLS_PATH']))
    env['TOOLS_PATH_PRQA_ANNOTATION_COLLECTOR'] = swfUtility.toWindowsPath(
        swfTools.checkEnvVar('TOOLS_PATH_PRQA_ANNOTATION_COLLECTOR', env['TOOLS_PATH']))
    env['PRQA_ANNOTATION_COLLECTOR_PAR_l'] = swfTools.checkEnvVar('PRQA_ANNOTATION_COLLECTOR_PAR_l', '20')

    env['PRQA_ANNOTATION_COLLECTOR_BLACKLIST'] = swfUtility.toWindowsPath(
        swfTools.checkEnvVar('PRQA_ANNOTATION_COLLECTOR_BLACKLIST',
            "../../${env.TOOLS_PATH_MAKE}/Make/config/qac_blacklist.mk"))

    bat commandBuilder.buildBat([
        '@echo on',
        "cd \"%WORKSPACE%\\SWF_Project\\${env.TOOLS_PATH_PRQA_ANNOTATION_COLLECTOR}\\PrqaAnnotationCollector",
        'PrqaAnnotationCollector.exe ' +
        "-r ..\\..\\${env.SOURCES_PATH} " +
        "-b ${env.PRQA_ANNOTATION_COLLECTOR_BLACKLIST} " +
        '-c 0 ' +
        "-l ${env.PRQA_ANNOTATION_COLLECTOR_PAR_l} " +
        '-w 1 ' +
        '-o report\\QacAnnotationOverview.xlsx >PrqaAnnotationCollectorLog.txt'
    ])

    println(
        '[PrqaAnnotationCollector.groovy][callPrqaAnnotationCollector] Call PrqaAnnotationCollector END.')
}

/**
 * copyReport copies PrqaAnnotationCollector report data to Project template folder SWF_Project
 * <p>
 * Copied files are:
 * <ul>
 *  <li>files from SWF_Project/[TOOLS_PATH_PRQA_ANNOTATION_COLLECTOR]/PrqaAnnotationCollector/report/* </li>
 * </ul>
 * @param env The Jenkins build environment. It must contain
 *            the following variables:
 *  <ul>
 *    <li>PROJECT_NAME - name of the project, e.g.: P12345 </li>
 *    <li>VARIANT_NAME - name of the variant, e.g.: BCM</li>
 *    <li>TOOLS_PATH_PRQA_ANNOTATION_COLLECTOR   - The relative path where the
 *                       Tools are located in
 *                       project path. Default: Tools</li>
 *  </ul>
 **/
void copyReport(Object env) {
    CommandBuilder commandBuilder = new CommandBuilder()
    SWFTools swfTools = new SWFTools()

    env['PROJECT_VARIANT_NAME'] = env.PROJECT_NAME + '_' + env.VARIANT_NAME

    //check Basic project environment
    swfTools.checkBasicProjectSetup()
    env['TOOLS_PATH_PRQA_ANNOTATION_COLLECTOR'] = swfTools.checkEnvVar('TOOLS_PATH_PRQA_ANNOTATION_COLLECTOR',
                                                                        env['TOOLS_PATH'])

    println(
        '[PrqaAnnotationCollector.groovy][copyReport] zip and copy doc: ' +
        'SWF_Project/' + env.TOOLS_PATH_PRQA_ANNOTATION_COLLECTOR + '/PrqaAnnotationCollector/report/*')

    zip zipFile: env.PROJECT_VARIANT_NAME + '/report/PrqaAnnotationCollector/PrqaAnnotationCollectorReport.zip',
        archive: false,
        dir: 'SWF_Project/' + env.TOOLS_PATH_PRQA_ANNOTATION_COLLECTOR + '/PrqaAnnotationCollector/report'

    bat commandBuilder.buildBat([
        "XCOPY \"%WORKSPACE%\\${env.PROJECT_VARIANT_NAME}\\report\\PrqaAnnotationCollector\\" +
        'PrqaAnnotationCollectorReport.zip\" ^',
        "\"%WORKSPACE%\\${env.PROJECT_VARIANT_NAME}\\data\\PrqaAnnotationCollector\\output\\" +
        'PrqaAnnotationCollectorReport.zip*\" /Y'
    ])

    println(
        '[PrqaAnnotationCollector.groovy][copyReport] Zip and copy PrqaAnnotationCollector doc done.')
}

/**
 * copyLog copies PrqaAnnotationCollector log data to Project template folder SWF_Project
 * <p>
 * Copied files are:
 * <ul>
 *  <li>files from SWF_Project/[TOOLS_PATH_PRQA_ANNOTATION_COLLECTOR]/PrqaAnnotationCollector/
 *          PrqaAnnotationCollectorLog.txt </li>
 * </ul>
 * @param env The Jenkins build environment. It must contain
 *            the following variables:
 *  <ul>
 *    <li>PROJECT_NAME - name of the project, e.g.: P12345 </li>
 *    <li>VARIANT_NAME - name of the variant, e.g.: BCM</li>
 *    <li>TOOLS_PATH_PRQA_ANNOTATION_COLLECTOR   - The relative path where the
 *                       Tools are located in
 *                       project path. Default: Tools</li>
 *  </ul>
 **/
void copyLog(Object env) {
    CommandBuilder commandBuilder = new CommandBuilder()
    SWFTools swfTools = new SWFTools()

    env['PROJECT_VARIANT_NAME'] = env.PROJECT_NAME + '_' + env.VARIANT_NAME

    //check Basic project environment
    swfTools.checkBasicProjectSetup()
    env['TOOLS_PATH_PRQA_ANNOTATION_COLLECTOR'] = swfTools.checkEnvVar('TOOLS_PATH_PRQA_ANNOTATION_COLLECTOR',
                                                                        env['TOOLS_PATH'])

    println(
        '[PrqaAnnotationCollector.groovy][copyLog] zip and copy doc: ' +
        'SWF_Project/' + env.TOOLS_PATH_PRQA_ANNOTATION_COLLECTOR +
        '/PrqaAnnotationCollector/PrqaAnnotationCollectorLog.txt')

    // copy log into Tmp Workspace
    bat commandBuilder.buildBat([
        "XCOPY \"%WORKSPACE%\\SWF_Project\\${env.TOOLS_PATH_PRQA_ANNOTATION_COLLECTOR}\\PrqaAnnotationCollector\\" +
        'PrqaAnnotationCollectorLog.txt\" ^',
        "\"%WORKSPACE%\\${env.PROJECT_VARIANT_NAME}\\data\\PrqaAnnotationCollector\\output\\" +
        'PrqaAnnotationCollectorLog.txt*\" /Y'
    ])

    // copy log to log folder
    bat commandBuilder.buildBat([
        "XCOPY \"%WORKSPACE%\\SWF_Project\\${env.TOOLS_PATH_PRQA_ANNOTATION_COLLECTOR}\\PrqaAnnotationCollector\\" +
        'PrqaAnnotationCollectorLog.txt\" ^',
        "\"%WORKSPACE%\\${env.PROJECT_VARIANT_NAME}\\log\\PrqaAnnotationCollector\\" +
        'PrqaAnnotationCollectorLog.txt*\" /Y'
    ])

    // copy log to report folder
    bat commandBuilder.buildBat([
        "XCOPY \"%WORKSPACE%\\SWF_Project\\${env.TOOLS_PATH_PRQA_ANNOTATION_COLLECTOR}\\PrqaAnnotationCollector\\" +
        'PrqaAnnotationCollectorLog.txt\" ^',
        "\"%WORKSPACE%\\${env.PROJECT_VARIANT_NAME}\\report\\PrqaAnnotationCollector\\" +
        'PrqaAnnotationCollectorLog.txt*\" /Y'
    ])

    println(
        '[PrqaAnnotationCollector.groovy][copyLog] Copy Log of PrqaAnnotationCollector END.')
}

/**
 * compilerWarningsCall is calling standard Jenkins "warnings" plugin to
 * publish PRQA AnnotationCollector log
 *
 * @param env The Jenkins build environment. It must contain
 *            the following variables:
 *  <ul>
 *    <li>PROJECT_NAME - name of the project, e.g.: P12345 </li>
 *    <li>VARIANT_NAME - name of the variant, e.g.: BCM</li>
 *  </ul>
 **/
void compilerWarningsCall(Object env) {
    CITools ciTools = new CITools()

    env['PROJECT_VARIANT_NAME'] = env.PROJECT_NAME + '_' + env.VARIANT_NAME

    println(
        '[PrqaAnnotationCollector.groovy][compilerWarningsCall] Publish PrqaAnnotationCollector Warnings START.')

    //Configure Parameter
    String pattern =  env.PROJECT_VARIANT_NAME + '\\report\\PrqaAnnotationCollector\\' +
        'PrqaAnnotationCollectorLog.txt'
    String parserID = 'QAC_Annotations_Parser'

    // call Pipelineworks Compiler Parser
    ciTools.checkForCompilerWarnings(pattern, parserID)

    println(
        '[PrqaAnnotationCollector.groovy][compilerWarningsCall] Publish PrqaAnnotationCollector Warnings END.')
}

/**
 * stashingOutput copies StatRes output to Project template folder SWF_Project
 * <p>
 * The following folders are stashed:
 * <ul>
 *  <li>swflib_PrqaAnnotationCollector_report: [PROJECT_VARIANT_NAME]/report/PrqaAnnotationCollector/*</li>
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
        '[PrqaAnnotationCollector.groovy][stashingOutput] Stashing files for PrqaAnnotationCollector START')

    // stash map file
    stash([
        includes: env.PROJECT_VARIANT_NAME + '/report/PrqaAnnotationCollector/**/*',
        name: 'swflib_PrqaAnnotationCollector_report'
    ])

    println(
        '[PrqaAnnotationCollector.groovy][stashingOutput] Stashing output of ' +
        'PrqaAnnotationCollector tool in swflib_PrqaAnnotationCollector_report done')
}

/**
 * archivingPrqaAnnotationCollector is archiving and stashing outputs of PrqaAnnotationCollector stage
 * <p>
 * Following folders are archived:
 * <ul>
 *  <li>[PROJECT_VARIANT_NAME]/data/PrqaAnnotationCollector/*</li>
 *  <li>[PROJECT_VARIANT_NAME]/report/PrqaAnnotationCollector/*</li>
 *  <li>[PROJECT_VARIANT_NAME]/log/PrqaAnnotationCollector/*</li>
 * </ul>
 * @param env The Jenkins build environment. It must contain
 *            the following variables:
 *  <ul>
 *    <li>PROJECT_NAME - name of the project, e.g.: P12345 </li>
 *    <li>VARIANT_NAME - name of the variant, e.g.: BCM</li>
 *  </ul>
 **/
void archivingPrqaAnnotationCollector(Object env) {
    CommonTools commonTools = new CommonTools()
    env['PROJECT_VARIANT_NAME'] = env.PROJECT_NAME + '_' + env.VARIANT_NAME

    println(
        '[PrqaAnnotationCollector.groovy][archivingPrqaAnnotationCollector] Archiving START')

    commonTools.archiveArtifacts(
        env.PROJECT_VARIANT_NAME + '/data/PrqaAnnotationCollector/output/**/*')
    commonTools.archiveArtifacts(
        env.PROJECT_VARIANT_NAME + '/log/PrqaAnnotationCollector/**/*')
    commonTools.archiveArtifacts(
        env.PROJECT_VARIANT_NAME + '/report/PrqaAnnotationCollector/**/*')

    println('[PrqaAnnotationCollector.groovy][archivingPrqaAnnotationCollector] Archiving END')
}

/**
 * buildPrqaAnnotationCollector is analyzing SW-Factory-Lib project to find PrqaAnnotationCollector tags
 *
 * @param env The Jenkins build environment. It must contain
 *            the following variables:
 * <ul>
 *   <li>PRQA_ANNOTATION_COLLECTOR         - Activation of PrqaAnnotationCollector
 *                                 [ON/OFF]</li>
 *    <li>PRQA_ANNOTATION_COLLECTOR_VERSION    - PrqaAnnotationCollector Version (TRUNK, LATEST(Default) or
 *                             specific Version Info) </li>
 *    <li>TOOLS_PATH_PRQA_ANNOTATION_COLLECTOR - The relative path where the
 *                     Tools are located in
 *                     project path. Default: Tools</li>
 * </ul>
 **/
void buildPrqaAnnotationCollector(Object env = this.env) {
    //import library tools
    Notifier notifier = new Notifier()
    SWFTools swfTools = new SWFTools()

    if (swfTools.checkON(env.PRQA_ANNOTATION_COLLECTOR)) {
        // add seperate Stage for checkout
        stage('PRQA Annotation Collector') {
            // verify Configuration first
            if (!isPrqaAnColConfigurationValid(env)) {
                // Config Error, throw error
                error 'PRQA Annotation Collector Configuration failed due to an incorrect configuration of the ' +
                'environment variables. -> Check the environment variables in the job configuration.'
            }
            try {
                println(
                    '[PrqaAnnotationCollector.groovy] Analyze PrqaAnnotationCollector START.')

                //checkout Tool from SCM
                checkoutPrqaAnnotationCollectorTool(env)

                //copy Tool to project workspace
                copyTool(env)

                // init PrqaAnnotationCollector workspace
                initPrqaAnnotationCollector(env)

                // call PrqaAnnotationCollector
                callPrqaAnnotationCollector(env)

                // copy Files to TempWorkspace
                copyReport(env)

                // copy log File to TempWorkspace
                copyLog(env)

                // publish PRQA Warnings
                compilerWarningsCall(env)

                // stashing and archiving
                archivingPrqaAnnotationCollector(env)
                stashingOutput(env)

                println(
                    '[PrqaAnnotationCollector.groovy] Analyze PrqaAnnotationCollector END.')
            } catch (e) {
                //Print error message
                println('[PrqaAnnotationCollector.groovy] buildPrqaAnnotationCollector failed.')
                notifier.onFailure(env, e)
            }
        }
    }
    else {
        //default is release
        env['PRQA_ANNOTATION_COLLECTOR'] = 'OFF'
        println('[PrqaAnnotationCollector.groovy] PrqaAnnotationCollector deactivated.')
    }
}
