/*
 * SW-Factory Library module to analyze Compiler Warnings
*/
package com.kostal.pipelineworks.swflib

/**
 * isTaskScannerConfigurationValid is verifiying TaskScanner config by checking env vars
 * and setting default values for optional env vars
 *
 * @param env The Jenkins build environment. It must contain
 *            the following variables:
 *  <ul>
 *    <li>COMPILE_NODE       - Server Node Name</li>
 *    <li>RECIPIENTS         - list of e-mail recipients</li>
 *  </ul>
 **/
boolean isTaskScannerConfigurationValid(Object env) {
    SWFTools swfTools = new SWFTools()

    println('[TaskScanner.groovy][isTaskScannerConfigurationValid] Verify TaskScanner Config START')

    boolean configStatus = swfTools.isConfigurationValid(env,
        ['COMPILE_NODE', 'RECIPIENTS'])

    //check Basic project environment
    swfTools.checkBasicProjectSetup()

    println('[TaskScanner.groovy][isTaskScannerConfigurationValid] Verify TaskScanner Config END')

    return configStatus
}

/**
 * configScan is configuring taskScanner
 *
 * @param env The Jenkins build environment. It must contain
 *            the following variables:
 * <ul>
 *   <li>HIGH_TAGS       - (optional) List of high tags for Task Scanner
 *                             Default: 'TODO*'</li>
 *   <li>NRM_TAGS        - (optional) List of normal tags for Task Scanner
 *                             Default: 'PRQA*'</li>
 *   <li>LOW_TAGS        - (optional) List of low tags for Task Scanner
 *                             Default: 'RTE_E_UNCONNECTED'</li>
 *   <li>TS_INCLUDE      - (optional) File include pattern
 *                             Default: '**\*.c,**\*.h'</li>
 * </ul>
 **/
void configScan(Object env) {
    //import library tools
    SWFTools swfTools = new SWFTools()

    println(
        '[TaskScanner.groovy][configScan] Config TaskScanner START.')

    env['HIGH_TAGS'] = swfTools.checkEnvVar('HIGH_TAGS', 'TODO*')
    env['LOW_TAGS'] = swfTools.checkEnvVar('LOW_TAGS', 'RTE_E_UNCONNECTED')
    env['NRM_TAGS'] = swfTools.checkEnvVar('NRM_TAGS', 'PRQA*')
    env['TS_INCLUDE'] = swfTools.checkEnvVar('TS_INCLUDE', '**/*.c,**/*.h')

    println(
        '[TaskScanner.groovy][configScan] Config TaskScanner END.')
}

/**
 * callScan is calling recordIssues tool
 *
 * @param env The Jenkins build environment. It must contain
 *            the following variables:
 * <ul>
 *   <li>HIGH_TAGS       - (optional) List of high tags for Task Scanner
 *                             Default: 'TODO*'</li>
 *   <li>NRM_TAGS        - (optional) List of normal tags for Task Scanner
 *                             Default: 'PRQA*'</li>
 *   <li>LOW_TAGS        - (optional) List of low tags for Task Scanner
 *                             Default: 'RTE_E_UNCONNECTED'</li>
 *   <li>TS_INCLUDE      - (optional) File include pattern
 *                             Default: '**\*.c,**\*.h'</li>
 * </ul>
 **/
void callScan(Object env) {
    //call recordIssues
    println(
        '[TaskScanner.groovy][callScan] Call TaskScanner START.')
    recordIssues(
        sourceCodeEncoding: 'UTF-8',
        tools: [
            taskScanner(
                highTags: env.HIGH_TAGS,
                normalTags: env.NRM_TAGS,
                lowTags: env.LOW_TAGS,
                ignoreCase: true,
                includePattern: env.TS_INCLUDE,
            )
        ]
    )

    println(
        '[TaskScanner.groovy][callScan] Call TaskScanner END.')
}

/**
 * scanProject is analyzing SW-Factory-Lib project to find specific tags
 *
 * @param env The Jenkins build environment. It must contain
 *            the following variables:
 * <ul>
 *   <li>TASK_SCAN       - Activation of Task Scanning
 *                                 [ON/OFF]</li>
 *   <li>HIGH_TAGS       - (optional) List of high tags for Task Scanner
 *                             Default: 'TODO*'</li>
 *   <li>NRM_TAGS        - (optional) List of normal tags for Task Scanner
 *                             Default: 'PRQA*'</li>
 *   <li>LOW_TAGS        - (optional) List of low tags for Task Scanner
 *                             Default: 'RTE_E_UNCONNECTED'</li>
 *   <li>TS_INCLUDE      - (optional) File include pattern
 *                             Default: '**\*.c,**\*.h'</li>
 * </ul>
 **/
void scanProject(Object env = this.env) {
    //import library tools
    Notifier notifier = new Notifier()
    SWFTools swfTools = new SWFTools()

    if (swfTools.checkON(env.TASK_SCAN)) {
        // add seperate Stage for checkout
        stage('Task Scanning') {
            // verify Configuration first
            if (!isTaskScannerConfigurationValid(env)) {
                // Config Error, throw error
                error 'TaskScanner failed due to an incorrect configuration of the ' +
                'environment variables. -> Check the environment variables in the job configuration.'
            }
            try {
                println(
                    '[TaskScanner.groovy] Analyze TaskScanner START.')

                // configure TaskScanner
                configScan(env)

                // call TaskScanner
                callScan(env)

                println(
                    '[TaskScanner.groovy] Analyze TaskScanner END.')
            } catch (e) {
                //Print error message
                println('[TaskScanner.groovy] TaskScanner failed.')
                notifier.onFailure(env, e)
            }
        }
    }
    else {
        //default is release
        env['TASK_SCAN'] = 'OFF'
        println('[TaskScanner.groovy] TaskScanner deactivated.')
    }
}
