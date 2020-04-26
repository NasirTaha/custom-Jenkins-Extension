/*
 * SW-Factory Library module to test and validate Static Resources by
 * analyzing map-File
*/
package com.kostal.pipelineworks.swflib

import com.kostal.pipelineworks.CommandBuilder
import com.kostal.pipelineworks.CommonTools
import com.kostal.pipelineworks.CheckoutTools
import com.kostal.pipelineworks.Checkout
import com.kostal.pipelineworks.Utility

/**
 * isStaticResourcesConfigurationValid is verifiying StaticResources config by checking env vars
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
boolean isStaticResourcesConfigurationValid(Object env) {
    SWFTools swfTools = new SWFTools()

    println('[StaticResources.groovy][isStaticResourcesConfigurationValid] Verify StaticResources Config START')

    boolean configStatus = swfTools.isConfigurationValid(env,
        ['PROJECT_NAME', 'VARIANT_NAME', 'COMPILE_NODE', 'RECIPIENTS'])

    //check Basic project environment
    swfTools.checkBasicProjectSetup()

    println('[StaticResources.groovy][isStaticResourcesConfigurationValid] Verify StaticResources Config END')

    return configStatus
}

/**
 * getScmPath is building SCM Repository path for Resource tool
 *
 * @param version Required version info: TRUNK, LATEST or tag name
 **/
String getScmPath(String version) {
    // SCM base path
    String staticResourcesRepository = 'https://debesvn001/kostal/lk_ae_internal/LK/Resource/'
    String staticResourcesLatestVersion = 'Resource_v4_0_0'

    switch (version) {
        case 'TRUNK':
            //return trunk
            return staticResourcesRepository + 'trunk/dist'
        case 'LATEST':
            //return LATEST
            return staticResourcesRepository + 'tags/' + staticResourcesLatestVersion + '/dist'
        case 'Resource_v3_4_0':
            //return trunk
            return staticResourcesRepository + 'tags/Resource_v3_4_0/dist'
        case 'Resource_v3_5_0':
            //return trunk
            return staticResourcesRepository + 'tags/Resource_v3_5_0/dist'
        case 'Resource_v3_6_0':
            //return trunk
            return staticResourcesRepository + 'tags/Resource_v3_6_0/dist'
        case 'Resource_v3_7_0':
            //return trunk
            return staticResourcesRepository + 'tags/Resource_v3_7_0/dist'
        case 'Resource_v3_8_0':
            //return trunk
            return staticResourcesRepository + 'tags/Resource_v3_8_0/dist'
        case 'Resource_v3_9_0':
            //return trunk
            return staticResourcesRepository + 'tags/Resource_v3_9_0/dist'
        case 'Resource_v4_0_0':
            //return trunk
            return staticResourcesRepository + 'tags/Resource_v4_0_0/dist'
        default:
            println('[StaticResources.groovy][getScmPath] no valid version selected, return LATEST')
            return staticResourcesRepository + 'tags/' + staticResourcesLatestVersion + '/dist'
        }
}

/**
 * checkoutStaticResourcesTool is getting Resource tool from SCM into Project directory
 * [TOOLS_PATH]/Resource
 *
 * @param env The Jenkins build environment. It must contain
 *            the following variables:
 *  <ul>
 *    <li>RESOURCE_VERSION    - (optional) StaticResources Version (TRUNK, LATEST(Default) or
 *                             specific Version Info) </li>
 *    <li>PROJECT_NAME       - name of the project, e.g.: P12345 </li>
 *    <li>VARIANT_NAME       - name of the variant, e.g.: BCM</li>
 *  </ul>
 **/
void checkoutStaticResourcesTool(Object env) {
    CheckoutTools checkoutTools = new CheckoutTools()
    SWFTools swfTools = new SWFTools()

    println('[StaticResources.groovy][checkoutStaticResourcesTool] Checkout StaticResources START')

    env['PROJECT_VARIANT_NAME'] = env.PROJECT_NAME + '_' + env.VARIANT_NAME
    env['RESOURCE_VERSION'] = swfTools.checkEnvVar('RESOURCE_VERSION', 'LATEST')

    // Get Repository from SCM
    env['RESOURCE_REPOSITORY'] = getScmPath(env.RESOURCE_VERSION)

    println(
        '[StaticResources.groovy][checkoutStaticResourcesTool] Selected Resource Version: ' +
        env.RESOURCE_VERSION)

    List checkoutList = []
    checkoutList << [
        env.RESOURCE_REPOSITORY, env.PROJECT_VARIANT_NAME + '/tools/Resource/', 'UpdateWithCleanUpdater'
    ]

    checkoutTools.checkoutAll(Checkout.buildCheckout(env, '', checkoutList))

    println('[StaticResources.groovy][checkoutStaticResourcesTool] Checkout StaticResources END')
}

/**
 * copyTool to copy Toolchain for Static Resources into Project directory
 * [TOOLS_PATH_RESOURCE]/Resource
 *
 * @param env The Jenkins build environment. It must contain
 *            the following variables:
 *  <ul>
 *    <li>RESOURCE_VERSION    - (optional) StaticResources Version (TRUNK, LATEST(Default) or
 *                             specific Version Info) </li>
 *    <li>TOOLS_PATH_RESOURCE - The relative path where the Tools are located in
 *                     project path. Default: Tools</li>
 *  </ul>
 **/
void copyTool(Object env) {
    CommandBuilder commandBuilder = new CommandBuilder()
    SWFTools swfTools = new SWFTools()
    Utility swfUtility = new Utility()

    //check Basic project environment
    swfTools.checkBasicProjectSetup()
    env['TOOLS_PATH_RESOURCE'] = swfUtility.toWindowsPath(
        swfTools.checkEnvVar('TOOLS_PATH_RESOURCE', env['TOOLS_PATH']))

    println(
        '[StaticResources.groovy][copyTool] Copy Resource Tool: ' +
        env.RESOURCE_VERSION)

    bat commandBuilder.buildBat([
        "XCOPY \"${env.PROJECT_VARIANT_NAME}\\tools\\Resource\\resource.exe\" ^",
        "\"%WORKSPACE%\\SWF_Project\\${env.TOOLS_PATH_RESOURCE}\\Resource\\" +
            'resource.exe*" /Y'
    ])
}

/**
 * copyMap to copy map-file for Static Resources into Project directory
 * [TOOLS_PATH_RESOURCE]/Resource/map
 *
 * <p>
 * copyMap is unstashing 'swflib_statresource_file'
 * @param env The Jenkins build environment. It must contain
 *            the following variables:
 *  <ul>
 *    <li>PROJECT_NAME - name of the project, e.g.: P12345 </li>
 *    <li>VARIANT_NAME - name of the variant, e.g.: BCM</li>
 *    <li>TOOLS_PATH_RESOURCE   - (optional) The relative path where the Tools are located in
 *                       project path. Default: Tools</li>
 *  </ul>
 **/
void copyMap(Object env) {
    CommandBuilder commandBuilder = new CommandBuilder()
    SWFTools swfTools = new SWFTools()
    Utility swfUtility = new Utility()

    env['PROJECT_VARIANT_NAME'] = env.PROJECT_NAME + '_' + env.VARIANT_NAME

    //check Basic project environment
    swfTools.checkBasicProjectSetup()
    env['TOOLS_PATH_RESOURCE'] = swfUtility.toWindowsPath(
        swfTools.checkEnvVar('TOOLS_PATH_RESOURCE', env['TOOLS_PATH']))

    //unstash map file
    println(
        '[StaticResources.groovy][copyMap] Unstash file ' +
        'swflib_statresource_file for StaticResources.')
    unstash 'swflib_statresource_file'

    println(
        '[StaticResources.groovy][copyMap] Copy map file from folder: ' +
        env.PROJECT_VARIANT_NAME + '/data/Resource/input')

    bat commandBuilder.buildBat([
        "XCOPY \"%WORKSPACE%\\${env.PROJECT_VARIANT_NAME}\\data\\" +
            'Resource\\input\\*.map" ^',
        "\"%WORKSPACE%\\SWF_Project\\${env.TOOLS_PATH_RESOURCE}\\" +
            'Resource\\map" /s /i /Y'
    ])
}

/**
 * copySettings to copy setting-files for Static Resources into project
 * directory [TOOLS_PATH]/Resource/settings
 * <p>
 * copySettings is unstashing 'swflib_statresource_settings'
 * @param env The Jenkins build environment. It must contain
 *            the following variables:
 *  <ul>
 *    <li>PROJECT_NAME - name of the project, e.g.: P12345 </li>
 *    <li>VARIANT_NAME - name of the variant, e.g.: BCM</li>
 *    <li>TOOLS_PATH_RESOURCE   - (optional) The relative path where the Tools are located in
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
    env['TOOLS_PATH_RESOURCE'] = swfUtility.toWindowsPath(
        swfTools.checkEnvVar('TOOLS_PATH_RESOURCE', env['TOOLS_PATH']))

    println(
        '[StaticResources.groovy][copySettings] Unstash file ' +
        'swflib_statresource_settings for StaticResources.')
    unstash 'swflib_statresource_settings'

    println(
        '[StaticResources.groovy][copySettings] Copy settings from folder: ' +
        env.PROJECT_VARIANT_NAME + '/data/Resource/input/settings')

    bat commandBuilder.buildBat([
        "XCOPY \"%WORKSPACE%\\${env.PROJECT_VARIANT_NAME}\\data\\" +
            'Resource\\input\\settings\\*" ^',
        "\"%WORKSPACE%\\SWF_Project\\${env.TOOLS_PATH_RESOURCE}\\" +
            'Resource\\settings" /s /i /Y'
    ])
}

/**
 * stashingOutput copies StatRes output to Project template folder SWF_Project
 * <p>
 * Copied files are:
 * <ul>
 *  <li>files from [TOOLS_PATH_RESOURCE]/output/* </li>
 * </ul>
 *
 * The following folders are stashed:
 * <ul>
 *  <li>swflib_statresource_output: [PROJECT_VARIANT_NAME]/data/Resourse/output/*</li>
 * </ul>
 * @param env The Jenkins build environment. It must contain
 *            the following variables:
 *  <ul>
 *    <li>PROJECT_NAME - name of the project, e.g.: P12345 </li>
 *    <li>VARIANT_NAME - name of the variant, e.g.: BCM</li>
 *    <li>TOOLS_PATH_RESOURCE   - (optional) The relative path where the Tools are located in
 *                       project path. Default: Tools</li>
 *  </ul>
 **/
void stashingOutput(Object env) {
    CommandBuilder commandBuilder = new CommandBuilder()
    SWFTools swfTools = new SWFTools()
    Utility swfUtility = new Utility()

    env['PROJECT_VARIANT_NAME'] = env.PROJECT_NAME + '_' + env.VARIANT_NAME

    //check Basic project environment
    swfTools.checkBasicProjectSetup()
    env['TOOLS_PATH_RESOURCE'] = swfUtility.toWindowsPath(
        swfTools.checkEnvVar('TOOLS_PATH_RESOURCE', env['TOOLS_PATH']))

    println(
        '[StaticResources.groovy][stashingOutput] Copy output of ' +
        'Resource tool : SWF_Project/' + env.TOOLS_PATH_RESOURCE + '/Resource/output')

    bat commandBuilder.buildBat([
        "XCOPY \"%WORKSPACE%\\SWF_Project\\${env.TOOLS_PATH_RESOURCE}\\" +
            'Resource\\output\\*_segment_stat.txt" ^',

        "\"%WORKSPACE%\\${env.PROJECT_VARIANT_NAME}\\data\\" +
            "Resource\\output\\${env.PROJECT_VARIANT_NAME}_segment" +
            '_stat.txt*" /s /i /Y',

        "XCOPY \"%WORKSPACE%\\SWF_Project\\${env.TOOLS_PATH_RESOURCE}\\" +
            'Resource\\output\\*_component_stat.txt" ^',

        "\"%WORKSPACE%\\${env.PROJECT_VARIANT_NAME}\\data\\" +
            "Resource\\output\\${env.PROJECT_VARIANT_NAME}_component" +
            '_stat.txt*" /s /i /Y',

        "XCOPY \"%WORKSPACE%\\SWF_Project\\${env.TOOLS_PATH_RESOURCE}\\" +
            'Resource\\output\\*_component_stat.csv" ^',

        "\"%WORKSPACE%\\${env.PROJECT_VARIANT_NAME}\\data\\" +
            "Resource\\output\\${env.PROJECT_VARIANT_NAME}_component" +
            '_stat.csv*" /s /i /Y',

        "XCOPY \"%WORKSPACE%\\SWF_Project\\${env.TOOLS_PATH_RESOURCE}\\" +
            'Resource\\output\\*_junit_testreport.xml" ^',

        "\"%WORKSPACE%\\${env.PROJECT_VARIANT_NAME}\\data\\" +
            "Resource\\output\\${env.PROJECT_VARIANT_NAME}_junit" +
            '_testreport.xml*" /s /i /Y'
    ])

    // stash map file
    stash([
        includes: env.PROJECT_VARIANT_NAME + '/data/Resource/output/**/*',
        name: 'swflib_statresource_output'
    ])
    println(
        '[StaticResources.groovy][stashingOutput] Stashing output of ' +
        'Resource tool in swflib_statresource_output done')
}

/**
 * copyReport copies StatRes report data to Project template folder SWF_Project
 * <p>
 * Copied files are:
 * <ul>
 *  <li>files from [PROJECT_VARIANT_NAME]/data/Resource/output/
 *      [PROJECT_VARIANT_NAME]_segment_stat.html </li>
 *  <li>files from [PROJECT_VARIANT_NAME]/data/Resource/output/
 *      [PROJECT_VARIANT_NAME]_segment_stat.txt </li>
 *  <li>files from [PROJECT_VARIANT_NAME]/data/Resource/output/
 *      [PROJECT_VARIANT_NAME]_component_stat.html </li>
 *  <li>files from [PROJECT_VARIANT_NAME]/data/Resource/output/
 *      [PROJECT_VARIANT_NAME]_component_stat.txt </li>
 *  <li>files from [PROJECT_VARIANT_NAME]/data/Resource/output/
 *      [PROJECT_VARIANT_NAME]_component_stat.csv </li>
 *  <li>files from [PROJECT_VARIANT_NAME]/data/Resource/output/
 *      [PROJECT_VARIANT_NAME]_junit_testreport.xml </li>
 * </ul>
 * @param env The Jenkins build environment. It must contain
 *            the following variables:
 *  <ul>
 *    <li>PROJECT_NAME - name of the project, e.g.: P12345 </li>
 *    <li>VARIANT_NAME - name of the variant, e.g.: BCM</li>
 *  </ul>
 **/
void copyReport(Object env) {
    CommandBuilder commandBuilder = new CommandBuilder()
    env['PROJECT_VARIANT_NAME'] = env.PROJECT_NAME + '_' + env.VARIANT_NAME

    println(
        '[StaticResources.groovy][copyReport] Copy report output of ' +
        'Resource tool: ' + env.PROJECT_VARIANT_NAME + '/data/Resource/output')

    String outputPrefix = (
        "XCOPY \"%WORKSPACE%\\${env.PROJECT_VARIANT_NAME}\\data\\" +
        "Resource\\output\\${env.PROJECT_VARIANT_NAME}")
    String reportPrefix = (
        "\"%WORKSPACE%\\${env.PROJECT_VARIANT_NAME}\\report\\Resource" +
        "\\${env.PROJECT_VARIANT_NAME}")

    bat commandBuilder.buildBat([
        "${outputPrefix}_segment_stat.html\" ^",
        "${reportPrefix}_segment_stat.html*\" /s /i /Y",
        "${outputPrefix}_segment_stat.txt\" ^",
        "${reportPrefix}_segment_stat.txt*\" /s /i /Y",
        "${outputPrefix}_component_stat.html\" ^",
        "${reportPrefix}_component_stat.html*\" /s /i /Y",
        "${outputPrefix}_component_stat.txt\" ^",
        "${reportPrefix}_component_stat.txt*\" /s /i /Y",
        "${outputPrefix}_component_stat.csv\" ^",
        "${reportPrefix}_component_stat.csv*\" /s /i /Y",
        "${outputPrefix}_junit_testreport.xml\" ^",
        "${reportPrefix}_junit_testreport.xml*\" /s /i /Y"
    ])

    println(
        '[StaticResources.groovy][copyReport] Copy report output of ' +
        'Resource tool done.')
}

/**
 * callTool is calling Resource tool 'resources.bat' for Testing Static
 * Resources in path [TOOLS_PATH]/Resource
 *
 * @param env The Jenkins build environment. It must contain
 *            the following variables:
 *  <ul>
 *    <li>PROJECT_NAME - name of the project, e.g.: P12345 </li>
 *    <li>VARIANT_NAME - name of the variant, e.g.: BCM</li>
 *    <li>RESOURCE_TOOL - (optional) bat file to call Resource Tool</li>
 *    <li>TOOLS_PATH_RESOURCE   - (optional) The relative path where the Tools are located in
 *                       project path. Default: Tools</li>
 *  </ul>
 **/
void callTool(Object env) {
    CommandBuilder commandBuilder = new CommandBuilder()
    SWFTools swfTools = new SWFTools()
    Utility swfUtility = new Utility()

    env['PROJECT_VARIANT_NAME'] = env.PROJECT_NAME + '_' + env.VARIANT_NAME

    //check Basic project environment
    swfTools.checkBasicProjectSetup()
    env['TOOLS_PATH_RESOURCE'] = swfUtility.toWindowsPath(
        swfTools.checkEnvVar('TOOLS_PATH_RESOURCE', env['TOOLS_PATH']))
    env['RESOURCE_TOOL'] = swfTools.checkEnvVar('RESOURCE_TOOL', 'resources.bat')

    println(
        '[StaticResources.groovy][callTool] Calling Resource Tool: ' +
        'SWF_Project/' + env.TOOLS_PATH_RESOURCE + '/Resource/' + env.RESOURCE_TOOL)

    println('[StaticResources.groovy][callTool] Copy RESOURCE_TOOL START')
    // copy RESOURCE_TOOL
    bat commandBuilder.buildBat([
        '@echo on',
        "XCOPY SWF_Project\\${env.TOOLS_PATH_RESOURCE}\\Resource\\${env.RESOURCE_TOOL} ^",
        "\"%WORKSPACE%\\${env.PROJECT_VARIANT_NAME}\\data\\Resource\\${env.RESOURCE_TOOL}*\" /Y"
    ])
    println('[StaticResources.groovy][callTool] Copy RESOURCE_TOOL END')

    println('[StaticResources.groovy][callTool] Call Resource Tool START')
    // call RESOURCE_TOOL
    bat commandBuilder.buildBat([
        '@echo on',
        "@cd SWF_Project\\${env.TOOLS_PATH_RESOURCE}\\Resource",
        "@call ${env.RESOURCE_TOOL}"
    ])
    println('[StaticResources.groovy][callTool] Call Resource Tool END')

    println('[StaticResources.groovy][callTool] Copy Resource Tool log.txt START')
    // copy log.txt
    bat commandBuilder.buildBat([
        '@echo on',
        "@cd SWF_Project\\${env.TOOLS_PATH_RESOURCE}\\Resource",
        'XCOPY \"log.txt\" ^',
        "\"%WORKSPACE%\\${env.PROJECT_VARIANT_NAME}\\log\\Resource\\" +
            "${env.PROJECT_VARIANT_NAME}_Resource_Tool.log*\" /Y"
    ])
    println('[StaticResources.groovy][callTool] Copy Resource Tool log.txt END')
}

/**
 * reportStaticResourcesHtml is building html Reports for Resource tool
 * <p>
 * Following files need to be available in path:
 * <ul>
 *  <li>[PROJECT_VARIANT_NAME]_segment_stat.html:
 *      [PROJECT_VARIANT_NAME]/data/Resource/output/
 *      [PROJECT_VARIANT_NAME]_segment_stat.txt </li>
 *  <li>[PROJECT_VARIANT_NAME]_component_stat.html:
 *      [PROJECT_VARIANT_NAME]/data/Resource/output/
 *      [PROJECT_VARIANT_NAME]_component_stat.txt </li>
 * </ul>
 * @param env The Jenkins build environment. It must contain
 *            the following variables:
 *  <ul>
 *    <li>PROJECT_NAME - name of the project, e.g.: P12345 </li>
 *    <li>VARIANT_NAME - name of the variant, e.g.: BCM</li>
 *  </ul>
 **/
void reportStaticResourcesHtml(Object env) {
    env['PROJECT_VARIANT_NAME'] = env.PROJECT_NAME + '_' + env.VARIANT_NAME
    println(
        '[StaticResources.groovy][reportStaticResourcesHtml] Build html ' +
        'Reports for StaticResources: ' + env.PROJECT_VARIANT_NAME +
        '/data/Resource/output/' + env.PROJECT_VARIANT_NAME +
        '_component_stat.html')

    /*
     Creates an index.html file template then it gets the project name and
     the project variant from the resource_config.xml file with those
     parameters it replace the name of the generated
     component_stat.txt file Ex. P00000_RdW_component_stat.txt
    */
    powershell '''
    echo Start report creation
    cd .\\''' + env.PROJECT_VARIANT_NAME + '''\\data\\Resource\\output\\

    If (-Not ( Test-Path ".\\''' + env.PROJECT_VARIANT_NAME + '''_component_stat.html" ))
    {
        New-Item .\\''' + env.PROJECT_VARIANT_NAME + '''_component_stat.html -ItemType "file" | out-null
        Set-Content .\\''' + env.PROJECT_VARIANT_NAME + '''_component_stat.html -Value @"
        <!DOCTYPE html>
        <html>
        <head>
        </head>
        <style>
        html {
            font-family: Helvetica, Arial, sans-serif;
            font-size: 100%;
            background: #FFFFFF;
        }

        #report{
            width: 1200px;
            min-height: 2500px;
            overflow: hidden;
        }
        </style>

        <body style="overflow-x:auto;height: 100%">
        <object id="report" type="text/plain" data="''' + env.PROJECT_VARIANT_NAME + '''_component_stat.txt"></object>
        </body>
        </html>
"@
        }
    '''

    println(
        '[StaticResources.groovy][reportStaticResourcesHtml] Build html ' +
        'Reports for StaticResources: ' + env.PROJECT_VARIANT_NAME +
        '/data/Resource/output/' + env.PROJECT_VARIANT_NAME +
        '_segment_stat.html')

    /*
     Creates an index.html file template then it gets the project name and the
     project variant from the resource_config.xml file with those parameters
     it replace the name of the generated segment_stat.txt file
     ex. P00000_RdW_segment_stat.txt
    */
    powershell '''
    echo Start report creation
    cd .\\''' + env.PROJECT_VARIANT_NAME + '''\\data\\Resource\\output\\

    If (-Not ( Test-Path ".\\''' + env.PROJECT_VARIANT_NAME + '''_segment_stat.html" ))
    {
        New-Item .\\''' + env.PROJECT_VARIANT_NAME + '''_segment_stat.html -ItemType "file" | out-null
        Set-Content .\\''' + env.PROJECT_VARIANT_NAME + '''_segment_stat.html -Value @"
        <!DOCTYPE html>
        <html>
        <head>
        </head>
        <style>
        html {
            font-family: Helvetica, Arial, sans-serif;
            font-size: 100%;
            background: #FFFFFF;
        }

        #report{
            width: 1200px;
            min-height: 2500px;
            overflow: hidden;
        }
        </style>

        <body style="overflow-x:auto;height: 100%">
        <object id="report" type="text/plain" data="''' + env.PROJECT_VARIANT_NAME + '''_segment_stat.txt"></object>
        </body>
        </html>
"@
        }
    '''
    println(
        '[StaticResources.groovy][reportStaticResourcesHtml] Build ' +
        'html Reports for StaticResources done.')
}

/**
 * archivingStaticResources is archiving and stashing outputs of Resource stage
 * <p>
 * Following folders are archived:
 * <ul>
 *  <li>[PROJECT_VARIANT_NAME]/data/Resource/*</li>
 *  <li>[PROJECT_VARIANT_NAME]/log/Resource/*</li>
 *  <li>[PROJECT_VARIANT_NAME]/report/Resource/*</li>
 * </ul>
 * Following folders are stashed:
 * <ul>
 *  <li>swflib_statresource_report:
 *      [PROJECT_VARIANT_NAME]/report/Resource/*</li>
 * </ul>
 * @param env The Jenkins build environment. It must contain
 *            the following variables:
 *  <ul>
 *    <li>PROJECT_NAME - name of the project, e.g.: P12345 </li>
 *    <li>VARIANT_NAME - name of the variant, e.g.: BCM</li>
 *  </ul>
 **/
void archivingStaticResources(Object env) {
    CommonTools commonTools = new CommonTools()
    env['PROJECT_VARIANT_NAME'] = env.PROJECT_NAME + '_' + env.VARIANT_NAME

    println(
        '[StaticResources.groovy][archivingStaticResources] Archiving START')

    commonTools.archiveArtifacts(
        env.PROJECT_VARIANT_NAME + '/data/Resource/**/*')
    commonTools.archiveArtifacts(
        env.PROJECT_VARIANT_NAME + '/log/Resource/**/*')
    commonTools.archiveArtifacts(
        env.PROJECT_VARIANT_NAME + '/report/Resource/**/*')

    println('[StaticResources.groovy][archivingStaticResources] Archiving END')

    println(
        '[StaticResources.groovy][archivingStaticResources] Stashing START')

    stash([
        includes: env.PROJECT_VARIANT_NAME + '/report/Resource/**/*',
        name: 'swflib_statresource_report'
    ])

    println(
        '[StaticResources.groovy][archivingStaticResources] Stash ' +
        'swflib_statresource_report for StaticResources Reports done.')
    println('[StaticResources.groovy][archivingStaticResources] Stashing END')
}

/**
 * callResourceReporter is calling Pipelineworks Resource Reporter
 *
 * <p>
 * First, folder [PROJECT_VARIANT_NAME]/data/Resource/output is copied
 * resursively into a temp workspace which is given then to Reporting tool to
 * build a continuous graph
 * @param env The Jenkins build environment. It must contain
 *            the following variables:
 *  <ul>
 *    <li>PROJECT_NAME - name of the project, e.g.: P12345 </li>
 *    <li>VARIANT_NAME - name of the variant, e.g.: BCM</li>
 *  </ul>
 **/
@SuppressWarnings('GStringExpressionWithinString') // Justification: string
// interpolation in the called method.
void callResourceReporter(Object env){
    CommonTools commonTools = new CommonTools()
    CommandBuilder commandBuilder = new CommandBuilder()

    env['PROJECT_VARIANT_NAME'] = env.PROJECT_NAME + '_' + env.VARIANT_NAME

    commonTools.copyArtifactsRecursively(
        env['JOB_NAME'],
        env.PROJECT_VARIANT_NAME + '/data/Resource/output/*',
        env.PROJECT_VARIANT_NAME + '/data/Resource/output-archive/build-${id}/'
    )

    println(
        '[StaticResources.groovy][callResourceReporter] Run Resource ' +
        'Reporter START')

    bat commandBuilder.buildBat([
        "resource-reporter ${env.PROJECT_VARIANT_NAME}/data/" +
            'Resource/output-archive ^',

        "${env.PROJECT_VARIANT_NAME}/report/Resource/" +
            "${env.PROJECT_VARIANT_NAME}_Resource_Analysis_Seg_Report.html",

        "resource-reporter-comp ${env.PROJECT_VARIANT_NAME}/data/" +
            'Resource/output-archive ^',

        "${env.PROJECT_VARIANT_NAME}/report/Resource/" +
            "${env.PROJECT_VARIANT_NAME}_Resource_Analysis_Comp_Report.html"
    ])

    println(
        '[StaticResources.groovy][callResourceReporter] Run ' +
        'Resource Reporter END')
}

/**
 * staticResources is performing a full SW-Factory Lib Static Resources test.
 * <p>
 * Following pipeline stages are created:
 * <ul>
 *  <li>- Static Resources</li>
 * </ul>
 * <p>
 * staticResources is unstashing 'swflib_statresource_settings' from
 * SW-Factory lib build job.
 * @param env The Jenkins build environment. It must contain
 *            the following variables:
 *  <ul>
 *    <li>PROJECT_NAME     - name of the project, e.g.: P12345 </li>
 *    <li>VARIANT_NAME     - name of the variant, e.g.: BCM</li>
 *    <li>TOOLS_PATH_RESOURCE   - (optional) The relative path where the Tools are located in
 *                       project path. Default: Tools</li>
 *    <li>RESOURCE_VERSION    - (optional) Resourcew Version (TRUNK, LATEST(Default) or
 *                             specific Version Info) </li>
 *    <li>STATIC_RESOURCES - Activation of Static Resources [ON/OFF]</li>
 *  </ul>
 **/
void staticResources(Object env = this.env) {
    Object notifier = new Notifier()
    SWFTools swfTools = new SWFTools()

    if (swfTools.checkON(env.STATIC_RESOURCES)) {
        stage('Static Resources') {
            // verify Configuration first
            if (!isStaticResourcesConfigurationValid(env)) {
                // Config Error, throw error
                error 'StaticResources failed due to an incorrect configuration of the ' +
                'environment variables. -> Check the environment variables in the job configuration.'
            }
            try {
                println(
                    '[StaticResources.groovy][staticResources] Test ' +
                    'Static Resources START.')
                //checkout Tool from SCM
                checkoutStaticResourcesTool(env)
                // copy tool into project path
                copyTool(env)
                // get map file
                copyMap(env)
                // get tool config
                copySettings(env)
                // run tool
                callTool(env)
                // stash tool output
                stashingOutput(env)
                // build html report
                reportStaticResourcesHtml(env)
                // copy report to tmp workspace
                copyReport(env)
                // build history by calling ResourceReporter
                callResourceReporter(env)
                // archive output
                archivingStaticResources(env)

                println(
                    '[StaticResources.groovy][staticResources] Test ' +
                    'Static Resources END.')
            } catch (e) {
                //Print error message
                println(
                    '[StaticResources.groovy][staticResources] ' +
                    'staticResources failed.')
                notifier.onFailure(env, e)
            }
        }
    }
    else {
        env['STATIC_RESOURCES'] = 'OFF'
        println(
            '[staticResources.groovy][staticResources] Static ' +
            'Resources deactivated.')
    }
}
