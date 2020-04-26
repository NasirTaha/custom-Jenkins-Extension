/*
 * SW-Factory Library module to build and archivate a SW. Module is also
 * building binaries for DynamicResource Testing and stashing
 * data for CompilerWarnings and StaticResources Analysis
*/
package com.kostal.pipelineworks.swflib

import com.kostal.pipelineworks.CommandBuilder
import com.kostal.pipelineworks.CommonTools
import com.kostal.pipelineworks.Utility

/**
 * isBuildConfigurationValid is verifiying build config by checking env vars
 * and setting default values for optional env vars
 *
 * @param env The Jenkins build environment. It must contain
 *            the following variables:
 * <ul>
 *   <li>ECLIPSE_VERSION         - The path where MSYS and MinGW are
 *                                 stored</li>
 *   <li>PROJECT_NAME            - name of the project, e.g.: P12345 </li>
 *   <li>VARIANT_NAME            - name of the variant, e.g.: BCM</li>
 *   <li>MAKE_VAR                - (optional) String buffer to add parameters to your
 *                                 build process as defined in you make
 *                                 tool</li>
 *   <li>COMPILER_PATH           - env var of compiler</li>
 *   <li>COMPILE_NODE            - (optional) server node, where script is executed, Default: compile</li>
 *   <li>RECIPIENTS              - list of e-mail recipients</li>
 * </ul>
 **/
boolean isBuildConfigurationValid(Object env) {
    SWFTools swfTools = new SWFTools()

    println('[Build.groovy][isBuildConfigurationValid] Verify Build Config START')

    boolean configStatus = swfTools.isConfigurationValid(env,
        ['COMPILER_PATH', 'PROJECT_NAME', 'VARIANT_NAME', 'ECLIPSE_VERSION', 'RECIPIENTS'])

    //check Basic project environment
    swfTools.checkBasicProjectSetup()

    // verify optional parameter and set default values
    env['MAKE_VAR'] = swfTools.checkEnvVar('MAKE_VAR', '')
    env['COMPILE_NODE'] = swfTools.checkEnvVar('COMPILE_NODE', 'compile')

    println('[Build.groovy][isBuildConfigurationValid] Verify Build Config END')

    return configStatus
}

/**
 * make is calling make script via bat command required by Kostal standard
 * make tooling
 *
 * <p>
 * Typical make call is:
 * <ul>
 *  <li>[TOOLS_PATH_MAKE]/make/[MAKE_TOOL] [buildMode] [MAKE_VAR]
 *      -j[MAKE_CORE_NO_BUILD] all_rebuild</li>
 * </ul>
 * log file is stored to:
 * <ul>
 *  <li>[PROJECT_VARIANT_NAME]_[buildMode]_compile.log</li>
 * </ul>
 *
 * @param env The Jenkins build environment. It must contain
 *            the following variables:
 * <ul>
 *     <li>ECLIPSE_VERSION    - The path where MSYS and MinGW are stored</li>
 *     <li>TOOLS_PATH_MAKE    - (optional) The relative path where the Tools are located
 *     in project path. Default: Tools</li>
 *     <li>WORKSPACE          - Workspace folder of Jenkins build. Env var is
 *     set by Jenkins itself.</li>
 *     <li>PROJECT_NAME       - name of the project, e.g.: P12345 </li>
 *     <li>VARIANT_NAME       - name of the variant, e.g.: BCM</li>
 *     <li>MAKE_VAR           - String buffer to add parameters to your build
 *     process as defined in you make tool</li>
 *     <li>MAKE_TOOL          - (optional) used make tool. Default:
 *     Make.bat</li>
 *     <li>MAKE_CORE_NO_BUILD - (optional) No of cores to run build process.
 *     Default: 16</li>
 * </ul>
 * @param buildMode build target parameter as string [release, debug,
 * integration, ...]
 **/
@SuppressWarnings('BuilderMethodWithSideEffects') // Not a builder method
void make(Object env, String buildMode) {
    CommandBuilder commandBuilder = new CommandBuilder()
    SWFTools swfTools = new SWFTools()

    env['PROJECT_VARIANT_NAME'] = env.PROJECT_NAME + '_' + env.VARIANT_NAME
    env['MAKE_TOOL'] = swfTools.checkEnvVar('MAKE_TOOL', 'make.bat')
    env['MAKE_CORE_NO_BUILD'] = swfTools.checkEnvVar('MAKE_CORE_NO_BUILD', '16')

    //check Basic project environment
    swfTools.checkBasicProjectSetup()
    env['TOOLS_PATH_MAKE'] = swfTools.checkEnvVar('TOOLS_PATH_MAKE', env['TOOLS_PATH'])

    println('[Build.groovy][make] call make with following parameter')
    println('[Build.groovy][make] Project Variant: ' + env.PROJECT_VARIANT_NAME)
    println('[Build.groovy][make] WORKSPACE: ' + env.WORKSPACE)
    println('[Build.groovy][make] Eclipse: ' + env.ECLIPSE_VERSION)
    println('[Build.groovy][make] Tools: ' + env.TOOLS_PATH_MAKE)
    println('[Build.groovy][make] make tool: ' + env.MAKE_TOOL)
    println('[Build.groovy][make] build mode: ' + buildMode)
    println('[Build.groovy][make] make var: ' + env.MAKE_VAR)
    println('[Build.groovy][make] make cores: ' + env.MAKE_CORE_NO_BUILD)

    bat commandBuilder.buildBat([
      '@echo on',
      "@set MINGW=%${env.ECLIPSE_VERSION}%/mingw/bin",
      "@set MSYS=%${env.ECLIPSE_VERSION}%/msys64/usr/bin",
      '@set PATH=%MINGW%;%MSYS%',
      '@set MAKE_CALL_RESULT=%WORKSPACE%\\make_call_result.tmp',
      '@echo > %MAKE_CALL_RESULT%',
      "cd %WORKSPACE%\\SWF_Project\\${env.TOOLS_PATH_MAKE}\\make",

      "(%WORKSPACE%\\SWF_Project\\${env.TOOLS_PATH_MAKE}\\make\\${env.MAKE_TOOL} ^",
          "${buildMode} " +
          "${env.MAKE_VAR} " +
          "-j${env.MAKE_CORE_NO_BUILD} " +
          'all_rebuild ' +
          '2>&1 && del %MAKE_CALL_RESULT%)| \"%MSYS%/tee" ' +
      "${env.PROJECT_VARIANT_NAME}_${buildMode}_compile.log",
      '@echo off',
      'if exist %MAKE_CALL_RESULT% (',
          'del %MAKE_CALL_RESULT%',
          'exit /b 1',
      ') else (',
          'exit /b 0',
      ')'
    ])
}

/**
 * copyOutput copies build output to Project template folder SWF_Project
 * <p>
 * Copied files are:
 * <ul>
 *  <li>compile log file </li>
 *  <li>files from [TARGET_PATH]/[buildMode]/[TARGET_BIN_PATH]/* </li>
 * </ul>
 * @param env  The Jenkins build environment. It must contain
 *             the following variables:
 * <ul>
 *     <li>TOOLS_PATH_MAKE - (optional) The relative path where the Tools are located in
 *                           project path. Default: Tools</li>
 *     <li>TARGET_PATH     - (optional) Relative path to the Target folder, Default:
 *                           Target</li>
 *     <li>TARGET_BIN_PATH - (optional) Relative path to binary folder inside
 *                           Target path, Default: bin</li>
 *     <li>WORKSPACE       - Workspace folder of Jenkins build. Env var is set
 *                           by Jenkins itself.</li>
 *     <li>PROJECT_NAME    - name of the project, e.g.: P12345 </li>
 *     <li>VARIANT_NAME    - name of the variant, e.g.: BCM</li>
 * </ul>
 * @param buildMode build target parameter [release, debug, integration, ...]
 **/
void copyOutput(Object env, String buildMode) {
    //import library tools
    CommandBuilder commandbuilder = new CommandBuilder()
    SWFTools swfTools = new SWFTools()
    Utility swfUtility = new Utility()

    env['PROJECT_VARIANT_NAME'] = env.PROJECT_NAME + '_' + env.VARIANT_NAME
    //check Basic project environment
    swfTools.checkBasicProjectSetup()
    env['TOOLS_PATH_MAKE'] = swfUtility.toWindowsPath(swfTools.checkEnvVar('TOOLS_PATH_MAKE', env['TOOLS_PATH']))
    //check path to binary folder
    env['TARGET_BIN_PATH'] = swfUtility.toWindowsPath(swfTools.checkEnvVar('TARGET_BIN_PATH', 'bin'))

    println(
        '[Build.groovy][copyOutput] copy files to output, START buildMode: ' +
        buildMode)

    //copy log
    bat commandbuilder.buildBat(['@echo on',
        "XCOPY \"%WORKSPACE%\\SWF_Project\\${env.TOOLS_PATH_MAKE}\\Make\\" +
            "${env.PROJECT_VARIANT_NAME}_${buildMode}_compile.log\" ^",
        "\"%WORKSPACE%\\${env.PROJECT_VARIANT_NAME}\\log\\build\\" +
            "${env.PROJECT_VARIANT_NAME}_${buildMode}_compile.log*\" /Y"
    ])

    //copy bin
    bat commandbuilder.buildBat([
        '@echo on',
        "XCOPY \"%WORKSPACE%\\SWF_Project\\${env.TARGET_PATH}\\${buildMode}\\" +
            "${env.TARGET_BIN_PATH}\\*\" ^",
        "\"%WORKSPACE%\\${env.PROJECT_VARIANT_NAME}\\bin\\${buildMode}\" /s /i /Y"
    ])

    println(
        '[Build.groovy][copyOutput] copy files to output, END buildMode: ' +
        buildMode)
}

/**
 * configBuildList is setting up a build list and returning it.
 * <ul>
 *  <li>If no BUILD_LIST is set, build targets 'debug' and 'release' are set
 *      as default</li>
 *  <li>If no DYN_RESOURCES_BUILD is set, build targets 'integration' is set
 *      as default(only if DYNAMIC_RESOURCES is ON)</li>
 *  <li>If DYNAMIC_RESOURCES is set to ON, DYN_RESOURCES_BUILD is added to
 *      BUILD_LIST</li>
 * </ul>
 * @param env The Jenkins build environment. It must contain the following
 * variables:
 * <ul>
 *     <li>BUILD_LIST - (optional) list of build targets, e.g.
 *         BUILD_LIST = release,debug,integration</li>
 *     <li>DYNAMIC_RESOURCES - (optional) Activation of Dynamic Resource
 *         Testing [ON/OFF]</li>
 *     <li>DYN_RESOURCES_BUILD - (optional) build target to use with Dynamic
 *         Resource Testing</li>
 * </ul>
 **/
List configBuildList(Object env) {
    // import library tools
    List buildList = []

    // check BUILD_LIST env
    if ((env.BUILD_LIST != null) && (env.BUILD_LIST != '')) {
        List buildListImport = env.BUILD_LIST.split(',')

        //substitute BUILD_LIST env var
        for (items in buildListImport) {
            println(
                '[Build.groovy][configBuildList] Custom build target: ' +
                items.toString())
            buildList.add(items)
        }
        println('[Build.groovy][configBuildList] Custom build list set!')
    }
    else {
        // default is release and debug
        buildList.add('release')
        buildList.add('debug')
        println(
            '[Build.groovy][configBuildList] No build list defined, set ' +
            'default list instead!')
    }

    // check for DYNAMIC_RESOURCES env
    if ((env.DYNAMIC_RESOURCES != null) && (env.DYNAMIC_RESOURCES == 'ON')) {
        // add 'integration' build target
        if ((env.DYN_RESOURCES_BUILD != null) &&
            (env.DYN_RESOURCES_BUILD != '')) {
            // substitute DYN_RESOURCES_BUILD env var
            buildList.add(env.DYN_RESOURCES_BUILD)
            println(
                '[Build.groovy][configBuildList] DYNAMIC_RESOURCES activated! ' +
                    'Add build target ' + env.DYN_RESOURCES_BUILD + '.')
        }
        else {
            // default is release
            buildList.add('integration')
            println('[Build.groovy][configBuildList] DYNAMIC_RESOURCES ' +
                'activated! Add build target integration.')
        }
    }

    // set build var list
    println(
        '[Build.groovy][configBuildList] build list: ' + buildList.toString())

    return buildList
}

/**
 * archivingBuild is archiving outputs of build stage from temp project
 * workspace
 * <p>
 * Following folders are included:
 * <ul>
 *  <li>[PROJECT_VARIANT_NAME]/log/build/*</li>
 *  <li>[PROJECT_VARIANT_NAME]/bin/*</li>
 * </ul>
 * @param env The Jenkins build environment. It must contain
 *            the following variables:
 * <ul>
 *     <li>PROJECT_NAME - name of the project, e.g.: P12345 </li>
 *     <li>VARIANT_NAME - name of the variant, e.g.: BCM</li>
 * </ul>
 **/
void archivingBuild(Object env) {
    //import library tools
    CommonTools commonTools = new CommonTools()
    //set local environmant
    env['PROJECT_VARIANT_NAME'] = env.PROJECT_NAME + '_' + env.VARIANT_NAME

    //Archive artifacts
    //Archivation of output files.
    println('[Build.groovy][archivingBuild] Archiving START')
    commonTools.archiveArtifacts(env.PROJECT_VARIANT_NAME + '/log/build/**/*')
    commonTools.archiveArtifacts(env.PROJECT_VARIANT_NAME + '/bin/**/*')
    println('[Build.groovy][archivingBuild] Archiving END')
}

/**
 * stashingBuild is stashing outputs of build stage from temp project workspace
 *
 * <p>
 * Following folders are included:
 * <ul>
 *  <li>swflib_build_log: [PROJECT_VARIANT_NAME]/log/build/*</li>
 *  <li>swflib_build_bin: [PROJECT_VARIANT_NAME]/bin/*</li>
 * </ul>
 *
 * Module is also calling stashing of compiler warnings, static resources and
 * dynamic resources
 *
 * @param env The Jenkins build environment. It must contain
 *            the following variables:
 * <ul>
 *     <li>PROJECT_NAME - name of the project, e.g.: P12345 </li>
 *     <li>VARIANT_NAME - name of the variant, e.g.: BCM</li>
 * </ul>
 **/
void stashingBuild(Object env) {
    //set local environmant
    env['PROJECT_VARIANT_NAME'] = env.PROJECT_NAME + '_' + env.VARIANT_NAME

    //Stashing make results, archiving is done in reporting stage
    println('[Build.groovy][stashingBuild] stashing build output START')
    stash([
        includes: env.PROJECT_VARIANT_NAME + '/log/build/**/*',
        name: 'swflib_build_log'
    ])
    println(
        '[Build.groovy][stashingBuild] Stash swflib_build_log for ' +
        'build process log done.')

    stash([
        includes: env.PROJECT_VARIANT_NAME + '/bin/**/*',
        name: 'swflib_build_bin'
    ])
    println(
        '[Build.groovy][stashingBuild] Stash swflib_build_bin for build ' +
        'output done.')

    //stashing for further stages
    stashingBuildCompilerWarnings(env)
    stashingBuildStaticResources(env)
    stashingBuildDynamicResources(env)

    println('[Build.groovy][stashingBuild] stashing build output END')
}

/**
 * stashingBuildCompilerWarnings is stashing outputs of build stage from temp
 * project workspace
 *
 * <p>
 * Following files are included:
 * <ul>
 *  <li>swflib_compwarnings_file: [PROJECT_VARIANT_NAME]/data/CompWarnings/
 *      input/[PROJECT_VARIANT_NAME]_[COMPILER_WARNINGS_BUILD]_compile.log</li>
 * </ul>
 *
 * @param env The Jenkins build environment. It must contain
 *            the following variables:
 * <ul>
 *   <li>PROJECT_NAME            - name of the project, e.g.: P12345 </li>
 *   <li>VARIANT_NAME            - name of the variant, e.g.: BCM</li>
 *   <li>COMPILER_WARNINGS       - Activation of Compiler Warnings
 *       [ON/OFF]</li>
 *   <li>COMPILER_WARNINGS_BUILD - (optional) build target for compiler
 *       warnings, default: 'release' </li>
 *   <li>TOOLS_PATH_MAKE    - (optional) The relative path where the Tools are located
 *     in project path. Default: Tools</li>
 * </ul>
 **/
void stashingBuildCompilerWarnings(Object env) {
    CommandBuilder commandBuilder = new CommandBuilder()
    SWFTools swfTools = new SWFTools()
    Utility swfUtility = new Utility()

    //set local environmant
    env['PROJECT_VARIANT_NAME'] = env.PROJECT_NAME + '_' + env.VARIANT_NAME
    //check Basic project environment
    swfTools.checkBasicProjectSetup()
    env['TOOLS_PATH_MAKE'] = swfUtility.toWindowsPath(swfTools.checkEnvVar('TOOLS_PATH_MAKE', env['TOOLS_PATH']))
    //check make tool call
    env['COMPILER_WARNINGS_BUILD'] = swfTools.checkEnvVar('COMPILER_WARNINGS_BUILD', 'release')

    //stash for Compiler Warnings
    //copy log
    if (swfTools.checkON(env.COMPILER_WARNINGS)) {
        println(
            '[Build.groovy][stashingBuildCompilerWarnings] Copy files for ' +
            'Analyze CompilerWarnings.')

        bat commandBuilder.buildBat([
            '@echo off',
            "XCOPY \"%WORKSPACE%\\SWF_Project\\${env.TOOLS_PATH_MAKE}\\Make\\" +
                "${env.PROJECT_VARIANT_NAME}_${env.COMPILER_WARNINGS_BUILD}_" +
                'compile.log" ^',
            "\"%WORKSPACE%\\${env.PROJECT_VARIANT_NAME}\\data\\CompWarnings\\" +
                "input\\${env.PROJECT_VARIANT_NAME}_" +
                "${env.COMPILER_WARNINGS_BUILD}_compile.log*\" /s /i /Y"
        ])

      //stash it
        stash([
            includes: (
                env.PROJECT_VARIANT_NAME + '/data/CompWarnings/input/**/*_' +
                    env.COMPILER_WARNINGS_BUILD + '_compile.log'),
            name: 'swflib_compwarnings_file'
        ])

        println(
            '[Build.groovy][stashingBuildCompilerWarnings] Stash ' +
                'swflib_compwarnings_file for Analyze CompilerWarnings done.')
    }
}

/**
 * stashingBuildStaticResources is stashing outputs of build stage from temp
 * project workspace. map file location is: [TARGET_PATH]/
 * [STATIC_RESOURCES_BUILD]/[TARGET_BIN_PATH]/*.map
 *
 * <p>
 * Following files are included:
 * <ul>
 *  <li>swflib_statresource_file:
 *     [PROJECT_VARIANT_NAME]/data/Resource/input/*.map</li>
 *  <li>swflib_statresource_settings:
 *      [PROJECT_VARIANT_NAME]/data/Resource/input/settings/*</li>
 * </ul>
 *
 * @param env The Jenkins build environment. It must contain
 *            the following variables:
 * <ul>
 *   <li>PROJECT_NAME           - name of the project, e.g.: P12345 </li>
 *   <li>VARIANT_NAME           - name of the variant, e.g.: BCM</li>
 *   <li>STATIC_RESOURCES       - Activation of Static Resources [ON/OFF]</li>
 *   <li>TOOLS_PATH_RESOURCE    - (optional) The relative pathe where the Tools are
 *                                located. Default: Tools</li>
 *   <li>TARGET_PATH            - (optional) Relative path to the Target folder,
 *                                Default: Target</li>
 *   <li>TARGET_BIN_PATH        - (optional) Relative path to binary folder</li>
 *                                nside Target path, Default: bin</li>
 *   <li>STATIC_RESOURCES_BUILD - (optional) build target for stat. resources,</li>
 *                                Default: 'release' </li>
 * </ul>
 **/
void stashingBuildStaticResources(Object env) {
    CommandBuilder commandBuilder = new CommandBuilder()
    SWFTools swfTools = new SWFTools()
    Utility swfUtility = new Utility()

    env['PROJECT_VARIANT_NAME'] = env.PROJECT_NAME + '_' + env.VARIANT_NAME
    //check Basic project environment
    swfTools.checkBasicProjectSetup()
    env['TOOLS_PATH_RESOURCE'] = swfUtility.toWindowsPath(
        swfTools.checkEnvVar('TOOLS_PATH_RESOURCE', env['TOOLS_PATH']))
    env['STATIC_RESOURCES_BUILD'] = swfTools.checkEnvVar('STATIC_RESOURCES_BUILD', 'release')
    env['TARGET_BIN_PATH'] = swfUtility.toWindowsPath(swfTools.checkEnvVar('TARGET_BIN_PATH', 'bin'))

    //stash for Static Resources
    //copy map and settings
    if (swfTools.checkON(env.STATIC_RESOURCES)) {
        println(
            '[Build.groovy][stashingBuildStaticResources] Copy files for ' +
                'Analyze Static Resources.')

        bat commandBuilder.buildBat([
            '@echo off',
            "XCOPY \"%WORKSPACE%\\SWF_Project\\${env.TARGET_PATH}\\" +
                "${env.STATIC_RESOURCES_BUILD}\\" +
                "${env.TARGET_BIN_PATH}\\*.map\" ^",

            "\"%WORKSPACE%\\${env.PROJECT_VARIANT_NAME}\\data\\Resource\\" +
                'input" /s /i /Y',

            "XCOPY \"%WORKSPACE%\\SWF_Project\\${env.TOOLS_PATH_RESOURCE}\\Resource\\" +
                'settings\\*" ^',

            "\"%WORKSPACE%\\${env.PROJECT_VARIANT_NAME}\\data\\Resource\\" +
                'input\\settings" /s /i /Y'
        ])

        //stash it
        stash([
            includes: env.PROJECT_VARIANT_NAME + '/data/Resource/input/**/*.map',
            name: 'swflib_statresource_file'
        ])
        println(
            '[Build.groovy][stashingBuildStaticResources] Stash ' +
                'swflib_statresource_file for Analyze StaticResources done.')

        stash([
            includes: (
                env.PROJECT_VARIANT_NAME +
                    '/data/Resource/input/settings/**/*'),
            name: 'swflib_statresource_settings'])

        println(
            '[Build.groovy][stashingBuildStaticResources] Stash ' +
                'swflib_statresource_settings for Analyze StaticResources done.')
    }
}

/**
 * stashingBuildDynamicResources is stashing outputs of build stage from temp
 * project workspace
 *
 * <p>
 * DYNAMIC_RESOURCES must be set to ON. DYN_RESOURCES_BUILD is used as
 * optional build target for stashing
 *
 * <p>
 * Following files are included:
 * <ul>
 *  <li>swflib_dynresource_bin:
 *      [PROJECT_VARIANT_NAME]/data/DynResource/input/*</li>
 * </ul>
 *
 * @param env The Jenkins build environment. It must contain
 *            the following variables:
 * <ul>
 *   <li>PROJECT_NAME        - name of the project, e.g.: P12345 </li>
 *   <li>VARIANT_NAME        - name of the variant, e.g.: BCM</li>
 *   <li>DYNAMIC_RESOURCES   - Activation of Static Resources [ON/OFF]</li>
 *   <li>TARGET_PATH         - (optional) Relative path to the Target folder,
 *                             Default: Target</li>
 *   <li>TARGET_BIN_PATH     - (optional) Relative path to binary folder
 *                             inside Target path, Default: bin</li>
 *   <li>DYN_RESOURCES_BUILD - (optional) build target for dynamic resources,
 *                             Default: 'integration' </li>
 * </ul>
 **/
void stashingBuildDynamicResources(Object env) {
    //import library tools
    CommandBuilder commandBuilder = new CommandBuilder()
    SWFTools swfTools = new SWFTools()
    Utility swfUtility = new Utility()

    //set local environmant
    env['PROJECT_VARIANT_NAME'] = env.PROJECT_NAME + '_' + env.VARIANT_NAME

    //check make tool call
    env['DYN_RESOURCES_BUILD'] = swfTools.checkEnvVar('DYN_RESOURCES_BUILD', 'integration')
    //check path to binary folder
    env['TARGET_BIN_PATH'] = swfUtility.toWindowsPath(swfTools.checkEnvVar('TARGET_BIN_PATH', 'bin'))

    //stash for Dynamic Resources
    //copy binaries
    if (swfTools.checkON(env.DYNAMIC_RESOURCES)) {
        println(
            '[Build.groovy][stashingBuildDynamicResources] Copy files for ' +
                'Dynamic Resources Testing.')

        bat commandBuilder.buildBat([
            '@echo on',
            "XCOPY \"%WORKSPACE%\\SWF_Project\\${env.TARGET_PATH}\\" +
                "${env.DYN_RESOURCES_BUILD}\\${env.TARGET_BIN_PATH}\\*\" ^",
            "\"%WORKSPACE%\\${env.PROJECT_VARIANT_NAME}\\data\\" +
                'DynResource\\input" /s /i /Y'
        ])

      //stash it
        stash([
        includes: (
            'SWF_Project/' + env.TARGET_PATH + '/' +
                env.DYN_RESOURCES_BUILD + '/' + env.TARGET_BIN_PATH + '/**/*'),
        name: 'swflib_dynresource_bin'
        ])

        println(
        '[Build.groovy][stashingBuildDynamicResources] Stash ' +
            'swflib_dynresource_bin for Testing Dynamic Resources done.')
    }
}

/**
 * build is building a SW-Factory Lib project.
 * <p>
 * Following pipeline stages are created:
 * <ul>
 *  <li>SW Build Init</li>
 *  <li>SW Build [build]</li>
 *  <li>SW Build Archiving</li>
 * </ul>
 * Module will build up a BUILD_LIST based on activated functionality in
 * SW-Factory Pipeline. BUILD_LIST is used to call standard make tooling.
 * Finally, all log files and binaries are archived and stashed for
 * later usage.
 *
 * @param env The Jenkins build environment. It must contain
 *            the following variables:
 * <ul>
 *   <li>ECLIPSE_VERSION         - The path where MSYS and MinGW are
 *                                 stored</li>
 *   <li>TOOLS_PATH_MAKE         - (optional) The relative path where the Tools are located
 *     in project path. Default: Tools</li>
 *   <li>TARGET_PATH             - (optional) Relative path to the Target folder,
 *                                 Default: Target</li>
 *   <li>TARGET_BIN_PATH         - (optional) Relative path to binary folder
 *                                 inside Target path, Default: bin</li>
 *   <li>PROJECT_NAME            - name of the project, e.g.: P12345 </li>
 *   <li>VARIANT_NAME            - name of the variant, e.g.: BCM</li>
 *   <li>MAKE_VAR                - (optional) String buffer to add parameters to your
 *                                 build process as defined in you make
 *                                 tool</li>
 *   <li>MAKE_TOOL               - (optional) used make tool.
 *                                 Default: Make.bat</li>
 *   <li>MAKE_CORE_NO_BUILD      - (optional) No of cores to run build process.
 *                                 Default: 16</li>
 *   <li>BUILD_LIST              - (optional) list of build targets, e.g.
 *                                 BUILD_LIST = release,debug,integration</li>
 *   <li>DYNAMIC_RESOURCES       - Activation of Static Resources [ON/OFF]</li>
 *   <li>DYN_RESOURCES_BUILD     - (optional) build target for dynamic
 *                                 resources, Default: 'integration' </li>
 *   <li>STATIC_RESOURCES        - Activation of Static Resources [ON/OFF]</li>
 *   <li>STATIC_RESOURCES_BUILD  - (optional) build target for static
 *                                 resources, Default: 'release' </li>
 *   <li>COMPILER_WARNINGS       - Activation of Compiler Warnings
 *                                 [ON/OFF]</li>
 *   <li>COMPILER_WARNINGS_BUILD - (optional) build target for compiler
 *                                 warnings, default: 'release' </li>
 * </ul>
 **/
@SuppressWarnings('BuilderMethodWithSideEffects') // Not a builder method
void build(Object env = this.env) {
    try {
        List buildList = []

        //Init build
        stage('SWF Build Init') {
            // verify Configuration first
            if (!isBuildConfigurationValid(env)) {
                // Config Error, throw error
                error 'Build failed due to an incorrect configuration of the environment variables. ' +
                '-> Check the environment variables in the job configuration.'
            }
            println('[Build.groovy][build] Init build list')
            buildList = configBuildList(env)
        }

        //substitute BUILD_LIST env var
        for (build in buildList) {
            stage('SWF Build: ' + build ) {
                println('[Build.groovy][build] calling build target: ' + build)
                make(env, build)
                copyOutput(env, build)
            }
        }

        // add seperate Stage for build
        stage('SWF Build Archiving') {
            stashingBuild(env)
            archivingBuild(env)
            println('[Build.groovy][build] SW Build end.')
        }
    } catch (e) {
        println('[Build.groovy][build] build failed.')
        new Notifier().onFailure(env, e)
    }
}
