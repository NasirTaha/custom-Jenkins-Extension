/*
 * SW-Factory Library module to analyze Compiler Warnings
*/
package com.kostal.pipelineworks.swflib

import com.kostal.pipelineworks.CITools
import com.kostal.pipelineworks.Utility

/**
 * isCompilerWarningsConfigurationValid is verifiying CompilerWarnings config by checking env vars
 * and setting default values for optional env vars
 *
 * @param env The Jenkins build environment. It must contain
 *            the following variables:
 * <ul>
 *   <li>PROJECT_NAME            - name of the project, e.g.: P12345 </li>
 *   <li>VARIANT_NAME            - name of the variant, e.g.: BCM</li>
 *   <li>COMPILE_PARSER_NAME     - env var of compiler parser </li>
 *   <li>RECIPIENTS              - e-mail recipients list </li>
 * </ul>
 **/
boolean isCompilerWarningsConfigurationValid(Object env) {
    SWFTools swfTools = new SWFTools()
    println('[CompilerWarnings.groovy][isCompilerWarningsConfigurationValid] ' +
        'Verify CompilerWarnings Config START')

    boolean configStatus = swfTools.isConfigurationValid(env,
    ['COMPILE_PARSER_NAME', 'PROJECT_NAME', 'VARIANT_NAME', 'RECIPIENTS'])

    println('[CompilerWarnings.groovy][isCompilerWarningsConfigurationValid] ' +
        'Verify CompilerWarnings Config END')

    return configStatus
}

/**
 * compilerWarningsCall is calling standard Jenkins "warnings" plugin.
 *
 * @param env The Jenkins build environment. It must contain
 *            the following variables:
 * <ul>
 *   <li>COMPILE_PARSER_NAME - name of the compiler parser </li>
 *   <li>COMP_WARN_LOG       - warning log file </li>
 * </ul>
 **/
void compilerWarningsCall(Object env) {
    CITools ciTools = new CITools()
    ciTools.checkForCompilerWarnings(env.COMP_WARN_LOG, env.COMPILE_PARSER_NAME)
}

/**
 * compilerWarningsConfig is building a configuration for CompilerWarnings
 * analysis.
 *
 * <p>
 * Module is unstashing 'swflib_compwarnings_file' from SW-Factory build
 * process, which has COMPILER_WARNINGS = ON as requirement
 *
 * @param env The Jenkins build environment. It must contain the following
 * variables:
 * <ul>
 *   <li>PROJECT_NAME            - name of the project, e.g.: P12345 </li>
 *   <li>VARIANT_NAME            - name of the variant, e.g.: BCM</li>
 *   <li>COMPILE_PARSER_NAME     - name of the compiler parser </li>
 *   <li>COMPILER_WARNINGS_BUILD - (optional) build target for compiler
 *                                 warnings, default: 'release' </li>
 * </ul>
 **/
void compilerWarningsConfig(Object env) {
    SWFTools swfTools = new SWFTools()
    Utility swfUtility = new Utility()

    env['PROJECT_VARIANT_NAME'] = env.PROJECT_NAME + '_' + env.VARIANT_NAME
      //check make tool call
    env['COMPILER_WARNINGS_BUILD'] = swfTools.checkEnvVar(
        'COMPILER_WARNINGS_BUILD', 'release')

    env['COMP_WARN_LOG'] = swfUtility.toWindowsPath(
        env.PROJECT_VARIANT_NAME + '/data/CompWarnings/input/' +
        env.PROJECT_VARIANT_NAME + '_' + env.COMPILER_WARNINGS_BUILD +
        '_compile.log')

    println(
        '[CompilerWarnings.groovy][compilerWarningsConfig] File Input ' +
        'for Analyze CompilerWarnings: ' + env.COMP_WARN_LOG)
    println(
        '[CompilerWarnings.groovy][compilerWarningsConfig] Compiler Parser: ' +
        env.COMPILE_PARSER_NAME)

    println(
        '[CompilerWarnings.groovy][compilerWarningsConfig] Unstash ' +
        'swflib_compwarnings_file for Analyze CompilerWarnings.')

    unstash 'swflib_compwarnings_file'
}

/**
 * compilerWarnings is analyzing CompilerWarnings for a SW-Factory-Lib project.
 *
 * <p>
 * CompilerWarnings are stored inside Project Temp Workspace by SWFLib_build,
 * if COMPILER_WARNINGS = ON
 *
 * @param env The Jenkins build environment. It must contain
 *            the following variables:
 * <ul>
 *   <li>PROJECT_NAME            - name of the project, e.g.: P12345 </li>
 *   <li>VARIANT_NAME            - name of the variant, e.g.: BCM</li>
 *   <li>COMPILE_PARSER_NAME     - name of the compiler parser </li>
 *   <li>COMPILER_WARNINGS_BUILD - (optional) build target for compiler
 *                                 warnings, default: 'release' </li>
 *   <li>COMPILER_WARNINGS       - Activation of Compiler Warnings
 *                                 [ON/OFF]</li>
 * </ul>
 **/
void compilerWarnings(Object env = this.env) {
    //import library tools
    Notifier notifier = new Notifier()
    SWFTools swfTools = new SWFTools()

    if (swfTools.checkON(env.COMPILER_WARNINGS)) {
        // add seperate Stage for checkout
        stage('Compiler Warnings') {
            // verify Configuration first
            if (!isCompilerWarningsConfigurationValid(env)) {
                // Config Error, throw error
                error 'CompilerWarnings failed due to an incorrect configuration of ' +
                'environment variables. -> Check the environment variables in the job configuration.'
            }
            try {
                println(
                    '[CompilerWarnings.groovy] Analyze CompilerWarnings start.')

                compilerWarningsConfig(env)

                //run Compiler Warnings analysis
                compilerWarningsCall(env)

                println(
                    '[CompilerWarnings.groovy] Analyze CompilerWarnings end.')
            } catch (e) {
                //Print error message
                println('[CompilerWarnings.groovy] compilerWarnings failed.')
                notifier.onFailure(env, e)
            }
        }
    }
    else {
        //default is release
        env['COMPILER_WARNINGS'] = 'OFF'
        println('[CompilerWarnings.groovy] CompilerWarnings deactivated.')
    }
}
