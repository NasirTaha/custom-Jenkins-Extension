/*
 * SW-Factory Library module to test and validate Dynamic Resources.
 * Required Toolchain is ET_CONNECT.
*/
package com.kostal.pipelineworks.swflib

import com.kostal.pipelineworks.CommandBuilder
import com.kostal.pipelineworks.CommonTools
import com.kostal.pipelineworks.CheckoutTools
import com.kostal.pipelineworks.Checkout
import com.kostal.pipelineworks.Utility

/**
 * isDynamicResourcesConfigurationValid is verifiying DynamicResources config by checking env vars
 * and setting default values for optional env vars
 *
 * @param env The Jenkins build environment. It must contain
 *            the following variables:
 * <ul>
 *   <li>CI_SLAVE_LABEL     - node ID of Dynamic Resource Testing
 *          workstation</li>
 *   <li>PROJECT_NAME            - name of the project, e.g.: P12345 </li>
 *   <li>VARIANT_NAME            - name of the variant, e.g.: BCM</li>
 *   <li>RECIPIENTS              - list of e-mail recipients </li>
 *   <li>TA_TOOLSUITE_NÒDE       - (optional) node of TA_TOOLSUITE, Default: debejenk004 </li>
 *   <li>ETCONNECT_VERSION    - (optional) ET_connect Version (TRUNK, LATEST(Default) or
 *                             specific Version Info) </li>
 *   <li>ETTRIGGER_VERSION    - (optional) ET_trigger Version (TRUNK, LATEST(Default) or
 *                             specific Version Info) </li>
 *   <li>ETBTFCONV_VERSION    - (optional) ET_BTFConv Version (TRUNK, LATEST(Default) or
 *                             specific Version Info) </li>
 *   <li>TA_TOOLSUITE_VERSION - (optional) TA Toolsuite Version, Default: 18.4</li>
 * </ul>
 **/
boolean isDynamicResourcesConfigurationValid(Object env) {
    SWFTools swfTools = new SWFTools()

    println('[DynamicResources.groovy][isDynamicResourcesConfigurationValid] Verify DynamicResources Config START')

    boolean configStatus = swfTools.isConfigurationValid(env,
        ['CI_SLAVE_LABEL', 'PROJECT_NAME', 'VARIANT_NAME', 'RECIPIENTS'])

    //check Basic project environment
    swfTools.checkBasicProjectSetup()

    // verify optional parameter and set default values
    env['TA_TOOLSUITE_NODE'] = swfTools.checkEnvVar('TA_TOOLSUITE_NODE', 'debejenk004')
    env['TA_TOOLSUITE_VERSION'] = swfTools.checkEnvVar('TA_TOOLSUITE_VERSION', 'TA_TOOLSUITE_18_4')
    env['TA_TOOLSUITE_BAT_VERSION'] = swfTools.checkEnvVar('TA_TOOLSUITE_BAT_VERSION', 'TA_TOOLSUITE_BAT_18_4')
    env['ETCONNECT_VERSION'] = swfTools.checkEnvVar('ETCONNECT_VERSION', 'LATEST')
    env['ETTRIGGER_VERSION'] = swfTools.checkEnvVar('ETTRIGGER_VERSION', 'LATEST')
    env['ETBTFCONV_VERSION'] = swfTools.checkEnvVar('ETBTFCONV_VERSION', 'LATEST')

    println('[DynamicResources.groovy][isDynamicResourcesConfigurationValid] Verify DynamicResources Config END')

    return configStatus
}

/**
 * getScmPathETBTFConv is building SCM Repository path for ET_BTFConv tool
 *
 * @param version Required version info: TRUNK, LATEST or tag name
 **/
String getScmPathETBTFConv(String version) {
    // SCM base path
    String etBtfConvRepository = 'https://debesvn001/kostal/lk_ae_internal/LK/EventTracer/ET_BTFConv/'
    String etBtfConvLatestVersion = 'ET_BTFConv_v0150'

    switch (version) {
        case 'TRUNK':
            //return trunk
            return etBtfConvRepository + 'trunk/dist'
        case 'LATEST':
            //return LATEST
            return etBtfConvRepository + 'tags/' + etBtfConvLatestVersion + '/dist'
        case 'ET_BTFConv_v0150':
            //return trunk
            return etBtfConvRepository + 'tags/ET_BTFConv_v0150/dist'
        default:
            println('[DynamicResources.groovy][getScmPathETBTFConv] no valid version selected, return LATEST')
            return etBtfConvRepository + 'tags/' + etBtfConvLatestVersion + '/dist'
        }
}

/**
 * getScmPathETConnect is building SCM Repository path for ET_connect tool
 *
 * @param version Required version info: TRUNK, LATEST or tag name
 **/
String getScmPathETConnect(String version) {
    // SCM base path
    String etConnectRepository = 'https://debesvn001/kostal/lk_ae_internal/LK/EventTracer/ET_connect/'
    String etConnectLatestVersion = 'ET_connect_v0200'

    switch (version) {
        case 'TRUNK':
            //return trunk
            return etConnectRepository + 'trunk/dist'
        case 'LATEST':
            //return LATEST
            return etConnectRepository + 'tags/' + etConnectLatestVersion + '/dist'
        case 'ET_connect_v0110':
            //return trunk
            return etConnectRepository + 'tags/ET_connect_v0110/dist'
        case 'ET_connect_v0120':
            //return trunk
            return etConnectRepository + 'tags/ET_connect_v0120/dist'
        case 'ET_connect_v0200':
            //return trunk
            return etConnectRepository + 'tags/ET_connect_v0200/dist'
        default:
            println('[DynamicResources.groovy][getScmPathETConnect] no valid version selected, return LATEST')
            return etConnectRepository + 'tags/' + etConnectLatestVersion + '/dist'
        }
}

/**
 * getScmPathETTrigger is building SCM Repository path for ET_connect tool
 *
 * @param version Required version info: TRUNK, LATEST or tag name
 **/
String getScmPathETTrigger(String version) {
    // SCM base path
    String etTriggerRepository = 'https://debesvn001/kostal/lk_ae_internal/LK/EventTracer/ET_Trigger/'
    String etTriggerLatestVersion = 'ET_trigger_v0200'

    switch (version) {
        case 'TRUNK':
            //return trunk
            return etTriggerRepository + 'trunk/dist'
        case 'LATEST':
            //return LATEST
            return etTriggerRepository + 'tags/' + etTriggerLatestVersion + '/dist'
        case 'ET_trigger_v0100':
            //return trunk
            return etTriggerRepository + 'tags/ET_trigger_v0100/dist'
        case 'ET_trigger_v0200':
            //return trunk
            return etTriggerRepository + 'tags/ET_trigger_v0200/dist'
        default:
            println('[DynamicResources.groovy][getScmPathETTrigger] no valid version selected, return LATEST')
            return etTriggerRepository + 'tags/' + etTriggerLatestVersion + '/dist'
        }
}

/**
 * checkoutETBTFConvTool is getting ET_BTFConv tool from SCM into Project directory
 * [PROJECT_VARIANT_NAME]/tools/ET_BTFConv
 *
 * @param env The Jenkins build environment. It must contain
 *            the following variables:
 *  <ul>
 *   <li>PROJECT_NAME            - name of the project, e.g.: P12345 </li>
 *   <li>VARIANT_NAME            - name of the variant, e.g.: BCM</li>
 *   <li>ETCONNECT_VERSION    - (optional) ET_connect Version (TRUNK, LATEST(Default) or
 *                             specific Version Info) </li>
 *  </ul>
 **/
void checkoutETBTFConvTool(Object env) {
    CheckoutTools checkoutTools = new CheckoutTools()

    println('[DynamicResources.groovy][checkoutETBTFConvTool] Checkout DynamicResources ET_BTFConv START')

    env['PROJECT_VARIANT_NAME'] = env.PROJECT_NAME + '_' + env.VARIANT_NAME

    // Get Repository from SCM
    env['ETBTFCONV_REPOSITORY'] = getScmPathETBTFConv(env.ETBTFCONV_VERSION)

    println(
        '[DynamicResources.groovy][checkoutETBTFConvTool] Selected ET_BTFConv Version: ' +
        env.ETBTFCONV_VERSION)

    List checkoutList = []
    checkoutList << [
        env.ETBTFCONV_REPOSITORY, env.PROJECT_VARIANT_NAME + '/tools/ET_BTFConv/', 'UpdateWithCleanUpdater'
    ]

    checkoutTools.checkoutAll(Checkout.buildCheckout(env, '', checkoutList))

    println('[DynamicResources.groovy][checkoutETBTFConvTool] Checkout DynamicResources ET_BTFConv END')
}

/**
 * checkoutETConnectTool is getting ET_connect tool from SCM into Project directory
 * [PROJECT_VARIANT_NAME]/tools/ET_connect
 *
 * @param env The Jenkins build environment. It must contain
 *            the following variables:
 *  <ul>
 *   <li>PROJECT_NAME            - name of the project, e.g.: P12345 </li>
 *   <li>VARIANT_NAME            - name of the variant, e.g.: BCM</li>
 *   <li>ETCONNECT_VERSION    - (optional) ET_connect Version (TRUNK, LATEST(Default) or
 *                             specific Version Info) </li>
 *  </ul>
 **/
void checkoutETConnectTool(Object env) {
    CheckoutTools checkoutTools = new CheckoutTools()

    println('[DynamicResources.groovy][checkoutETConnectTool] Checkout DynamicResources ET_connect START')

    env['PROJECT_VARIANT_NAME'] = env.PROJECT_NAME + '_' + env.VARIANT_NAME

    // Get Repository from SCM
    env['ETCONNECT_REPOSITORY'] = getScmPathETConnect(env.ETCONNECT_VERSION)

    println(
        '[DynamicResources.groovy][checkoutETConnectTool] Selected ET_connect Version: ' +
        env.ETCONNECT_VERSION)

    List checkoutList = []
    checkoutList << [
        env.ETCONNECT_REPOSITORY, env.PROJECT_VARIANT_NAME + '/tools/ET_connect/', 'UpdateWithCleanUpdater'
    ]

    checkoutTools.checkoutAll(Checkout.buildCheckout(env, '', checkoutList))

    println('[DynamicResources.groovy][checkoutETConnectTool] Checkout DynamicResources ET_connect END')
}

/**
 * checkoutETTriggerTool is getting ET_trigger tool from SCM into Project directory
 * [PROJECT_VARIANT_NAME]/tools/ET_trigger
 *
 * @param env The Jenkins build environment. It must contain
 *            the following variables:
 *  <ul>
 *   <li>PROJECT_NAME            - name of the project, e.g.: P12345 </li>
 *   <li>VARIANT_NAME            - name of the variant, e.g.: BCM</li>
 *   <li>ETTRIGGER_VERSION    - (optional) ET_trigger Version (TRUNK, LATEST(Default) or
 *                             specific Version Info) </li>
 *  </ul>
 **/
void checkoutETTriggerTool(Object env) {
    CheckoutTools checkoutTools = new CheckoutTools()

    println('[DynamicResources.groovy][checkoutETTriggerTool] Checkout DynamicResources ET_trigger START')

    env['PROJECT_VARIANT_NAME'] = env.PROJECT_NAME + '_' + env.VARIANT_NAME

    // Get Repository from SCM
    env['ETTRIGGER_REPOSITORY'] = getScmPathETTrigger(env.ETTRIGGER_VERSION)

    println(
        '[DynamicResources.groovy][checkoutETTriggerTool] Selected ET_trigger Version: ' +
        env.ETTRIGGER_VERSION)

    List checkoutList = []
    checkoutList << [
        env.ETTRIGGER_REPOSITORY, env.PROJECT_VARIANT_NAME + '/tools/ET_Trigger/', 'UpdateWithCleanUpdater'
    ]

    checkoutTools.checkoutAll(Checkout.buildCheckout(env, '', checkoutList))

    println('[DynamicResources.groovy][checkoutETTriggerTool] Checkout DynamicResources ET_trigger END')
}

/**
 * initTestNode inits a Dynamic Resources local node. Node is given by label
 * CI_SLAVE_LABEL
 *
 * <p>
 * Module is using SW-Factory Lib module 'swflibtools_checkout' to checkout
 * source to local node. ET_Connect and ET_Trigger tool are copied as well
 *
 * @param env  The Jenkins build environment. It must contain
 *             the following variables:
 * <ul>
 *   <li>PROJECT_NAME       - name of the project, e.g.: P12345 </li>
 *   <li>VARIANT_NAME       - name of the variant, e.g.: BCM</li>
 *   <li>CI_SLAVE_LABEL     - node ID of Dynamic Resource Testing
 *          workstation</li>
 *   <li>TOOLS_PATH_EVENTTRACER - (optional) Project Path to EventTrace, Default: Tools</li>
 * </ul>
 *
 **/
void initTestNode(Object env) {
    //import library tools
    CommandBuilder commandBuilder = new CommandBuilder()
    SWFCheckout swflibCheckout = new SWFCheckout()
    SWFTools swfTools = new SWFTools()
    Utility swfUtility = new Utility()

    println('[DynamicResources.groovy][initTestNode] Init test node START')

    //set local environment
    env['PROJECT_VARIANT_NAME'] = env.PROJECT_NAME + '_' + env.VARIANT_NAME

    //check Basic project environment
    swfTools.checkBasicProjectSetup()
    env['TOOLS_PATH_EVENTTRACER'] = swfUtility.toWindowsPath(
        swfTools.checkEnvVar('TOOLS_PATH_EVENTTRACER', env['TOOLS_PATH']))

    //check iSystem SDK to find specific Python+SDK version
    swfTools.checkEnvVar('ISYSTEM_SDK', '')
    if (env.ISYSTEM_SDK != null) {
        //add _
        env['ISYSTEM_SDK'] = '_' + env.ISYSTEM_SDK
    }

    //start
    println(
        '[DynamicResources.groovy][initTestNode] Checkout Sources to node: ' +
        env.CI_SLAVE_LABEL)

    //checkout SW to local node
    swflibCheckout.checkout(env)

    //add binaries for flashing test device
    unstash 'swflib_dynresource_bin'

    println(
        '[DynamicResources.groovy][initTestNode] Add binaries of ' +
        'swflib_dynresource_bin done.')

    //Prepare ET_connect Tooling
    println('[DynamicResources.groovy][initTestNode] Copy EventTracer Tools START')

    // Checkout ET_connect from SCM
    checkoutETConnectTool(env)

    println(
        '[DynamicResources.groovy][initTestNode] Copy ET_Connect START')
    //ET_CONNECT available
    bat commandBuilder.buildBat([
        '@echo on',
        '@set WORKSPACE=%cd%',
        "XCOPY \"${env.PROJECT_VARIANT_NAME}\\tools\\ET_connect\\*${env.ISYSTEM_SDK}.exe\" ^",
        "\"%WORKSPACE%\\SWF_Project\\${env.TOOLS_PATH_EVENTTRACER}\\EventTracer\\" +
            'ET_connect\\ET_Connect.exe*" /Y'
    ])

    println(
        '[DynamicResources.groovy][initTestNode] Copy ET_Connect END')

    // Checkout ET_trigger from SCM
    checkoutETTriggerTool(env)

    println(
        '[DynamicResources.groovy][initTestNode] Copy ET_Trigger START')
    bat commandBuilder.buildBat([
        '@echo on',
        '@set WORKSPACE=%cd%',
        "XCOPY \"${env.PROJECT_VARIANT_NAME}\\tools\\ET_Trigger\\*${env.ISYSTEM_SDK}.exe\" ^",
        "\"%WORKSPACE%\\SWF_Project\\${env.TOOLS_PATH_EVENTTRACER}\\EventTracer\\" +
            'ET_connect\\ET_Trigger.exe*" /Y'
     ])

    println(
        '[DynamicResources.groovy][initTestNode] Copy ET_Trigger END')

    println('[DynamicResources.groovy][initTestNode] Copy EventTracer Tools END')

    println('[DynamicResources.groovy][initTestNode] Init test node END')
}

/**
 * initValidationNode inits Dynamic Resources Validation node.
 * <p>
 * ET_BTFConv tool is copied into Project Temp Workspace, subfolder tool
 * <p>
 * Following packages are unstashed:
 * <ul>
 *  <li>-swflib_dynresource_test</li>
 *  <li>-swflib_dynresource_validation_input</li>
 * </ul>
 *
 * @param env The Jenkins build environment. It must contain
 *            the following variables:
 *  <ul>
 *    <li>PROJECT_NAME  - name of the project, e.g.: P12345 </li>
 *    <li>VARIANT_NAME  - name of the variant, e.g.: BCM</li>
 *    <li>ET_BTFCONV    - ET_BTFConv Version Info</li>
 *  </ul>
 *
 **/
void initValidationNode(Object env) {
    //set local environment
    env['PROJECT_VARIANT_NAME'] = env.PROJECT_NAME + '_' + env.VARIANT_NAME

    //start
    //unstash files for Dyn Resource validation
    unstash 'swflib_dynresource_test'
    unstash 'swflib_dynresource_validation_input'
    println(
        '[DynamicResources.groovy][initValidationNode] Add Dyn Resources ' +
        'Validation files of swflib_dynresource_test and ' +
        'swflib_dynresource_validation_input done.')

    // Checkout ET_BTFConv from SCM
    checkoutETBTFConvTool(env)

    println(
        '[DynamicResources.groovy][initValidationNode] Init ' +
        'Validation node done.')
}

/**
 * runTest is calling [TOOLS_PATH_EVENTTRACER]/EventTracer/ET_connect/ET_Connect.bat to
 * run Dynamic Resource test
 * <p>
 * Module also copies test data to temp workspace
 * [PROJECT_VARIANT_NAME]/data/DynResource
 * @param env The Jenkins build environment. It must contain
 *            the following variables:
 *  <ul>
 *    <li>PROJECT_NAME  - name of the project, e.g.: P12345 </li>
 *    <li>VARIANT_NAME  - name of the variant, e.g.: BCM</li>
 *    <li>TOOLS_PATH_EVENTTRACER    - (optional) The relative path where the Tools are located</li>
 *    <li>TOOLS_PATH_TATOOLSUITE    - (optional) The relative path where the Tools are located</li>
 *    <li>ETCONNECT_VERSION         - (optional) ET_connect Version (TRUNK, LATEST(Default) or
 *                             specific Version Info) </li>
 *  </ul>
 *
 **/
void runTest(Object env) {
    //import library tools
    CommandBuilder commandBuilder = new CommandBuilder()
    SWFTools swfTools = new SWFTools()
    Utility swfUtility = new Utility()

    //set local environment
    env['PROJECT_VARIANT_NAME'] = env.PROJECT_NAME + '_' + env.VARIANT_NAME

    //check Basic project environment
    swfTools.checkBasicProjectSetup()
    env['TOOLS_PATH_EVENTTRACER'] = swfUtility.toWindowsPath(
        swfTools.checkEnvVar('TOOLS_PATH_EVENTTRACER', env['TOOLS_PATH']))
    env['TOOLS_PATH_TATOOLSUITE'] = swfUtility.toWindowsPath(
        swfTools.checkEnvVar('TOOLS_PATH_TATOOLSUITE', env['TOOLS_PATH']))

    //Prepare ET_connect Tooling
    if ((env.ETCONNECT_VERSION != null) && (env.ETCONNECT_VERSION != '')) {
        println('[DynamicResources.groovy][runTest] Call ET_Connect tool')
        bat commandBuilder.buildBat([
            '@echo on',
            "@cd %cd%\\SWF_Project\\${env.TOOLS_PATH_EVENTTRACER}\\EventTracer\\ET_connect",
            '@call ET_connect.bat'
        ])

        println('[DynamicResources.groovy][runTest] ET_Connect done.')

        //copy to Temp Workspace
        //copy Trace
        bat commandBuilder.buildBat([
            '@echo on',
            '@set WORKSPACE=%cd%',
            "XCOPY \"SWF_Project\\${env.TOOLS_PATH_EVENTTRACER}\\EventTracer\\" +
                'ET_connect\\Output\\*" ^',

            "\"%WORKSPACE%\\${env.PROJECT_VARIANT_NAME}\\data\\DynResource" +
                '\\test\\output" /s /i /Y',

            "XCOPY \"SWF_Project\\${env.TOOLS_PATH_EVENTTRACER}\\EventTracer\\" +
                'ET_SetupTool\\data\\ET_ID.txt" ^',

            "\"%WORKSPACE%\\${env.PROJECT_VARIANT_NAME}\\data\\" +
                'DynResource\\test\\output\\ET_ID.txt*" /Y',

            "XCOPY \"SWF_Project\\${env.TOOLS_PATH_TATOOLSUITE}\\TA_ToolSuite\\" +
                'bat_mode\\*.tam" ^',

            "\"%WORKSPACE%\\${env.PROJECT_VARIANT_NAME}\\data\\" +
                'DynResource\\validation\\input" /s /i /Y'
        ])

        println('[DynamicResources.groovy][runTest] Copy Trace Data done.')

        //copy Log
        bat commandBuilder.buildBat([
            '@echo on',
            '@set WORKSPACE=%cd%',
            "XCOPY \"SWF_Project\\${env.TOOLS_PATH_EVENTTRACER}\\EventTracer\\" +
                'ET_connect\\ET_connect_log.txt" ^',
            "\"%WORKSPACE%\\${env.PROJECT_VARIANT_NAME}\\log\\" +
                'DynResource\\test\\ET_connect_log.txt*" /Y'
        ])

        println('[DynamicResources.groovy][runTest] Copy log Data done.')
    }
}

/**
 * archivingTest archivate outputs of Dyn Resource Testing stage
 *
 * <p>
 * Following files are archived:
 * <ul>
 *  <li>-[PROJECT_VARIANT_NAME]/data/DynResource/test/*</li>
 *  <li>-[PROJECT_VARIANT_NAME]/log/DynResource/test/*</li>
 *  <li>-[PROJECT_VARIANT_NAME]/data/DynResource/validation/input/*</li>
 * </ul>
 * Following files are included in stash:
 * <ul>
 *  <li>swflib_dynresource_test:
 *      [PROJECT_VARIANT_NAME]/data/DynResource/test/*</li>
 *  <li>swflib_dynresource_validation_input:
 *      [PROJECT_VARIANT_NAME]/data/DynResource/validation/input/*</li>
 * </ul>
 *
 * @param env The Jenkins build environment. It must contain
 *            the following variables:
 *  <ul>
 *    <li>PROJECT_NAME  - name of the project, e.g.: P12345 </li>
 *    <li>VARIANT_NAME  - name of the variant, e.g.: BCM</li>
 *  </ul>
 **/
void archivingTest(Object env) {
    //import library tools
    CommonTools commonTools = new CommonTools()
    //set local environmant
    env['PROJECT_VARIANT_NAME'] = env.PROJECT_NAME + '_' + env.VARIANT_NAME

    //Archive artifacts
    //Archivation of output files.
    println('[DynamicResources.groovy][archivingTest] Archiving START')

    commonTools.archiveArtifacts(
        env.PROJECT_VARIANT_NAME + '/log/DynResource/test/**/*')
    commonTools.archiveArtifacts(
        env.PROJECT_VARIANT_NAME + '/data/DynResource/test/**/*')
    commonTools.archiveArtifacts(
        env.PROJECT_VARIANT_NAME + '/data/DynResource/validation/input/**/*')

    println('[DynamicResources.groovy][archivingTest] Archiving END')

    println('[DynamicResources.groovy][archivingTest] Stashing START')

    // Stashing process for the generated files to be used in the compile node
    // by the BTF Tool

    stash([
        includes: env.PROJECT_VARIANT_NAME + '/data/DynResource/test/**/*',
        name: 'swflib_dynresource_test'
    ])
    println(
        '[SWFLibTools_build.groovy][swflib_build_stashing] Stash ' +
        'swflib_dynresource_test for Dyn Resource Testing done.')

    stash([
        includes: (
            env.PROJECT_VARIANT_NAME +
            '/data/DynResource/validation/input/**/*'),
        name: 'swflib_dynresource_validation_input'
    ])

    println(
        '[SWFLibTools_build.groovy][swflib_build_stashing] Stash ' +
        'swflib_dynresource_validation_input for Dyn Resource ' +
        'Validation done.')
    println('[DynamicResources.groovy][archivingTest] Stashing END')
}

/**
 * archivingValidation archivate outputs of Dyn Resource Validation stage
 *
 * <p>
 * Following files are archived:
 * <ul>
 *  <li>-[PROJECT_VARIANT_NAME]/report/DynResource/*</li>
 *  <li>-[PROJECT_VARIANT_NAME]/data/DynResource/validation/output/*</li>
 * </ul>
 * Following files are included in stash:
 * <ul>
 *  <li>swflib_dynresource_report:
 *      [PROJECT_VARIANT_NAME]/report/DynResource/*</li>
 * </ul>
 *
 * @param env The Jenkins build environment. It must contain
 *            the following variables:
 *  <ul>
 *    <li>PROJECT_NAME  - name of the project, e.g.: P12345 </li>
 *    <li>VARIANT_NAME  - name of the variant, e.g.: BCM</li>
 *  </ul>
 **/
void archivingValidation(Object env) {
    //import library tools
    CommonTools commonTools = new CommonTools()
    CommandBuilder commandBuilder = new CommandBuilder()
    //set local environmant
    env['PROJECT_VARIANT_NAME'] = env.PROJECT_NAME + '_' + env.VARIANT_NAME

    println(
        '[DynamicResources.groovy][archivingValidation] Copy report output ' +
        'of DynResource tool: ' + env.PROJECT_VARIANT_NAME +
        '/data/DynResource/validation/output')

    bat commandBuilder.buildBat([
        '@echo on',
        '@set WORKSPACE=%cd%',
        "XCOPY \"%WORKSPACE%\\${env.PROJECT_VARIANT_NAME}\\data\\" +
            'DynResource\\validation\\output\\*" ^',
        "\"%WORKSPACE%\\${env.PROJECT_VARIANT_NAME}\\report\\" +
            'DynResource" /s /i /Y'
    ])

    //Archive artifacts
    //Archivation of output files.
    println('[DynamicResources.groovy][archivingValidation] Archiving START')

    commonTools.archiveArtifacts(
        env.PROJECT_VARIANT_NAME + '/data/DynResource/validation/output/**/*')
    commonTools.archiveArtifacts(
        env.PROJECT_VARIANT_NAME + '/report/DynResource/**/*')
    println('[DynamicResources.groovy][archivingValidation] Archiving END')

    println('[DynamicResources.groovy][archivingValidation] Stashing START')

    // Stashing process for the generated files to be used in the compile
    // node by the BTF Tool
    stash([
        includes: env.PROJECT_VARIANT_NAME + '/report/DynResource/**/*',
        name: 'swflib_dynresource_report'
    ])

    println(
        '[DynamicResources.groovy][archivingValidation] Stash ' +
        'swflib_dynresource_report for Dyn Resource Validation done.')
    println('[DynamicResources.groovy][archivingValidation] Stashing END')
}

/**
 * archivingBTFConv archivate outputs of Dyn Resource BTF convertion stage
 *
 * <p>
 * Following files are archived:
 * <ul>
 *  <li>-[PROJECT_VARIANT_NAME]/log/DynResource/btfconv/*</li>
 *  <li>-[PROJECT_VARIANT_NAME]/data/DynResource/btfconv/output/*</li>
 * </ul>
 * Following files are included in stash:
 * <ul>
 *  <li>swflib_dynresource_btfconv_output:
 *      [PROJECT_VARIANT_NAME]/data/DynResource/btfconv/output/*</li>
 * </ul>
 * @param env The Jenkins build environment. It must contain
 *            the following variables:
 *  <ul>
 *    <li>PROJECT_NAME  - name of the project, e.g.: P12345 </li>
 *    <li>VARIANT_NAME  - name of the variant, e.g.: BCM</li>
 *  </ul>
 **/
void archivingBTFConv(Object env) {
    //import library tools
    CommonTools commonTools = new CommonTools()
    //set local environmant
    env['PROJECT_VARIANT_NAME'] = env.PROJECT_NAME + '_' + env.VARIANT_NAME

    //Archive artifacts
    //Archivation of output files.
    println('[DynamicResources.groovy][archivingBTFConv] Archiving START')
    commonTools.archiveArtifacts(
        env.PROJECT_VARIANT_NAME + '/log/DynResource/btfconv/**/*')
    commonTools.archiveArtifacts(
        env.PROJECT_VARIANT_NAME + '/data/DynResource/btfconv/output/**/*')
    println('[DynamicResources.groovy][archivingBTFConv] Archiving END')

    println('[DynamicResources.groovy][archivingBTFConv] Stashing START')

    // Stashing process for the generated files to be used in the compile
    // node by the BTF Tool
    stash([
        includes: (
            env.PROJECT_VARIANT_NAME + '/data/DynResource/btfconv/output/**/*'),
        name: 'swflib_dynresource_btfconv_output'
    ])
    println(
        '[DynamicResources.groovy][archivingBTFConv] Stash ' +
        'swflib_dynresource_btfconv_output for BTF Conv done.')
    println('[DynamicResources.groovy][archivingBTFConv] Stashing END')
}

/**
 * runBTFConv is executing ET_BTFConv tool
 * <p>
 * BTF_Conf tool needs xml trace 'trace_file' and ET_ID.txt 'id_file' file
 * as input. BTF Trace is copied to path
 * [PROJECT_VARIANT_NAME]/data/DynResource/btfconv/output/
 * @param env The Jenkins build environment. It must contain
 *            the following variables:
 *  <ul>
 *    <li>PROJECT_NAME  - name of the project, e.g.: P12345 </li>
 *    <li>VARIANT_NAME  - name of the variant, e.g.: BCM</li>
 *  </ul>
 * @param traceFile     XML Trace file input
 * @param idFile        ET_ID file input
 *
**/
void runBTFConv(Object env, String traceFile, String idFile) {
    // import library tools
    CommandBuilder commandBuilder = new CommandBuilder()
    // set local environmant
    env['PROJECT_VARIANT_NAME'] = env.PROJECT_NAME + '_' + env.VARIANT_NAME

    println('[DynamicResources.groovy][runBTFConv] Building BTF Trace START')
    println('[DynamicResources.groovy][runBTFConv] XML Trace: ' + traceFile)
    println('[DynamicResources.groovy][runBTFConv] ET_ID: ' + idFile)

    // Copying process of the generated files by the ET_connect tool into
    // the ET_BTFConv Input folder
    println(
        '[DynamicResources.groovy][runBTFConv] Copy Trace file ' +
         'to Temp workspace')
    bat commandBuilder.buildBat([
        '@echo on',
        '@set WORKSPACE=%cd%',
        "XCOPY \"%WORKSPACE%\\${traceFile}\" ^",
        "\"%WORKSPACE%\\${env.PROJECT_VARIANT_NAME}\\tools\\" +
            'ET_BTFConv\\Input\\ET_Trace.xml*" /Y'
    ])

    println(
        '[DynamicResources.groovy][runBTFConv] Copy ID file ' +
         'to Temp workspace')
    bat commandBuilder.buildBat([
        '@echo on',
        '@set WORKSPACE=%cd%',
        "XCOPY \"%WORKSPACE%\\${idFile}\" ^",
        "\"%WORKSPACE%\\${env.PROJECT_VARIANT_NAME}\\tools\\" +
            'ET_BTFConv\\Input\\ET_ID.txt*" /Y'
    ])

    // Execution of the ET_BTFConv converter tool.
    println('[DynamicResources.groovy][runBTFConv] Call ET_BTFConv START')
    bat commandBuilder.buildBat([
        '@echo on',
         "@cd %WORKSPACE%\\${env.PROJECT_VARIANT_NAME}\\tools\\ET_BTFConv",
         "@call %WORKSPACE%\\${env.PROJECT_VARIANT_NAME}\\tools\\" +
            'ET_BTFConv\\ET_BTFConv.bat'
    ])
    println('[DynamicResources.groovy][runBTFConv] Call ET_BTFConv END')

    // copy to Temp Workspace
    // copy Log
    println(
        '[DynamicResources.groovy][runBTFConv] ET_BTFConv log ' +
        'to Temp workspace')
    bat commandBuilder.buildBat([
        '@echo on',
        '@set WORKSPACE=%cd%',
        "XCOPY \"%WORKSPACE%\\${env.PROJECT_VARIANT_NAME}\\tools\\" +
            'ET_BTFConv\\ET_BTFConv_log.txt" ^',
        "\"%WORKSPACE%\\${env.PROJECT_VARIANT_NAME}\\log\\" +
            'DynResource\\btfconv\\ET_BTFConv_log.txt*" /s /i /Y'
    ])

    //Finally copy output of ET_BTFConv
    println(
        '[DynamicResources.groovy][runBTFConv] copy ET_BTFConv output ' +
        'to Temp workspace')
    bat commandBuilder.buildBat([
        '@echo on',
        '@set WORKSPACE=%cd%',
        "XCOPY \"%WORKSPACE%\\${env.PROJECT_VARIANT_NAME}\\tools\\" +
            'ET_BTFConv\\Output\\*" ^',
        "\"%WORKSPACE%\\${env.PROJECT_VARIANT_NAME}\\data\\" +
            'DynResource\\btfconv\\output" /s /i /Y'
    ])

    println('[DynamicResources.groovy][runBTFConv] Building BTF Trace END')
}

/**
 * runBTFValidation is calling BTF validation tool
 * <p>
 * TA_Toolsuite is used as BTF vaidation tool
 * @param env The Jenkins build environment. It must contain
 *            the following variables:
 *  <ul>
 *    <li>PROJECT_NAME  - name of the project, e.g.: P12345 </li>
 *    <li>VARIANT_NAME  - name of the variant, e.g.: BCM</li>
 *    <li>TA_TOOLSUITE_BAT_VERSION - TA Toolsuite Version</li>
 *  </ul>
 *
 * @param btfFile    BTF Trace file input
 * @param reqFile    Requirement file input
 *
**/
void runBTFValidation(Object env, String btfFile, String reqFile) {
    //import library tools
    TimingArchitect timingArchitect = new TimingArchitect()
    //set local environmant
    env['PROJECT_VARIANT_NAME'] = env.PROJECT_NAME + '_' + env.VARIANT_NAME
    env['TA_TOOLSUITE_BAT_VERSION'] = env.TA_TOOLSUITE_BAT_VERSION

    println(
        '[DynamicResources.groovy][runBTFValidation] Validating BTF ' +
        'Trace START')

    println(
        '[DynamicResources.groovy][runBTFValidation] TA: ' +
        env.TA_TOOLSUITE_BAT_VERSION)
    println(
        '[DynamicResources.groovy][runBTFValidation] BTF Trace: ' + btfFile)
    println('[DynamicResources.groovy][runBTFValidation] Req: ' + reqFile)

    timingArchitect.generateReport(env, btfFile, reqFile)

    println(
        '[DynamicResources.groovy][runBTFValidation] Validating BTF Trace END')
}

/**
 * testing is testing Dynamic Resources on node CI_SLAVE_LABEL
 * <p>
 * CI_SLAVE_LABEL will be automatically initialized before running the test
 * by calling ET_Connect.bat
 * <p>
 * Following pipeline stages are created:
 * <ul>
 *  <li>- Dynamic Resources Init: [CI_SLAVE_LABEL]</li>
 *  <li>- Dynamic Resources Testing: [CI_SLAVE_LABEL]</li>
 * </ul>
 * @param env The Jenkins build environment. It must contain
 *            the following variables:
 *  <ul>
 *    <li>PROJECT_NAME  - name of the project, e.g.: P12345 </li>
 *    <li>VARIANT_NAME  - name of the variant, e.g.: BCM</li>
 *    <li>REPOSITORY_URL     - SVN repository URL (example:
 * https://debesvn001/kostal/lk_ae_internal/AEP/AEP5/CI_Template/Appl/)</li>
 *    <li>REPOSITORY_DEV_REP - SVN Development repository [default: trunk]</li>
 *    <li>REPOSITORY_TOOL    - SW Repository Tool, default: SVN [SVN/GIT]</li>
 *    <li>CI_SLAVE_LABEL     - node ID of Dynamic Resource Testing
 *                             workstation</li>
 *    <li>DYNAMIC_RESOURCES  - Activation of Dynamic Resources [ON/OFF]</li>
 *  </ul>
 *
 **/
void testing(Object env = this.env) {
    // import library tools
    Notifier notifier = new Notifier()
    SWFTools swfTools = new SWFTools()

    if (!swfTools.checkON(env.DYNAMIC_RESOURCES)) {
        env['DYNAMIC_RESOURCES'] = 'OFF'
        println(
            '[DynamicResources.groovy][validation] DynResources deactivated.')
        return
    }

    // verify Configuration first
    if (!isDynamicResourcesConfigurationValid(env)) {
        // Config Error, throw error
        error 'DynamicResources failed due to an incorrect configuration of ' +
        'environment variables. -> Check the environment variables in the job configuration.'
    }

    node (CI_SLAVE_LABEL) {
        // add seperate Stage for Dynamic Resources Testing
        stage('Dynamic Resources Init: ' + env.CI_SLAVE_LABEL) {
            try {
                // start
                println(
                    '[DynamicResources.groovy][testing] Init Dynamic ' +
                    'Resources start.')
                //checkout SW to local node
                initTestNode(env)
                // end
                println(
                    '[DynamicResources.groovy][testing] Init Dynamic ' +
                    'Resources done')
            } catch (e) {
                //Print error message
                println('[DynamicResources.groovy][testing] testing failed.')
                notifier.onFailure(env, e)
            }
        }

        stage('Dynamic Resources Testing: ' + env.CI_SLAVE_LABEL) {
            try {
                // start
                println(
                    '[DynamicResources.groovy][testing] Test Dynamic ' +
                    'Resources start.')
                // run Dynamic Resource Test
                runTest(env)
                //Archive
                archivingTest(env)
                // end
                println(
                    '[DynamicResources.groovy][testing] Test Dynamic ' +
                    'Resources end.')
            } catch (e) {
                // rint error message
                println('[DynamicResources.groovy][testing] testing failed.')
                notifier.onFailure(env, e)
            }
        }
    }
}

/**
 * validation is validating Dynamic Resources BTF file
 * <p>
 * Function is converting trace xml file to BTF file first. Finally it is
 * using TA_Toolsuite to validate converted BTF file
 * <p>
 * Following pipeline stages are created:
 * <ul>
 *  <li>- Dynamic Resources Validation</li>
 * </ul>
 * @param env The Jenkins build environment. It must contain
 *            the following variables:
 *  <ul>
 *    <li>PROJECT_NAME  - name of the project, e.g.: P12345 </li>
 *    <li>VARIANT_NAME  - name of the variant, e.g.: BCM</li>
 *    <li>DYNAMIC_RESOURCES - Activation of Dynamic Resources [ON/OFF]</li>
 *  </ul>
 **/
void validation(Object env = this.env) {
    Notifier notifier = new Notifier()
    SWFTools swfTools = new SWFTools()

    if (!swfTools.checkON(env.DYNAMIC_RESOURCES)) {
        env['DYNAMIC_RESOURCES'] = 'OFF'
        println(
            '[DynamicResources.groovy][validation] DynResources deactivated.')
        return
    }

    // verify Configuration first
    if (!isDynamicResourcesConfigurationValid(env)) {
        // Config Error, throw error
        error 'DynamicResources failed due to an incorrect configuration of ' +
        'environment variables. -> Check the environment variables in the job configuration.'
    }

    // execute on TA specific node
    node (TA_TOOLSUITE_NODE) {
        // add seperate Stage for Dynamic Resources Validation
        stage('Dynamic Resources Validation') {
            try {
                // start
                println(
                    '[DynamicResources.groovy][validation] Validate ' +
                    'Dynamic Resources start.')
                // checkout SW to local node
                initValidationNode(env)

                //run BTF Converter
                String traceFile = (
                    env.PROJECT_VARIANT_NAME +
                    '\\data\\DynResource\\test\\output\\ET_Trace.xml')

                String idFile = (
                    env.PROJECT_VARIANT_NAME +
                    '\\data\\DynResource\\test\\output\\ET_ID.txt')

                runBTFConv(env, traceFile, idFile)
                // Archive BTF Convertion
                archivingBTFConv(env)

                //run validation
                String btfFile = (
                    env.PROJECT_VARIANT_NAME +
                    '\\data\\DynResource\\btfconv\\output\\ET_TraceData.btf')

                String reqFile = (
                    env.PROJECT_VARIANT_NAME +
                    '\\data\\DynResource\\validation\\input\\Requirements.tam')

                runBTFValidation(env, btfFile, reqFile)
                // Archive Validation
                archivingValidation(env)
                // end
                println(
                    '[DynamicResources.groovy][validation] Validate ' +
                    'Dynamic Resources end.')
            } catch (e) {
                println(
                    '[DynamicResources.groovy][validation] validation failed.')
                notifier.onFailure(env, e)
            }
        }
    }
}
