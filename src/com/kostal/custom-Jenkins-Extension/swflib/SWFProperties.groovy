/*
 * SW-Factory Library module to read Properties file
*/
package com.kostal.pipelineworks.swflib

import com.kostal.pipelineworks.CommandBuilder
import com.kostal.pipelineworks.CommonTools

/**
 * isSWFPropertiesValid is verifiying SWFProperties config by checking env vars
 * and setting default values for optional env vars
 *
 * @param env The Jenkins build environment. It must contain
 *            the following variables:
 *  <ul>
 *    <li>COMPILE_NODE       - Server Node Name</li>
 *    <li>RECIPIENTS         - list of e-mail recipients</li>
 *  </ul>
 **/
boolean isSWFPropertiesValid(Object env) {
    SWFTools swfTools = new SWFTools()

    println('[SWFProperties.groovy][isSWFPropertiesValid] Verify SWFProperties Config START')

    boolean configStatus = swfTools.isConfigurationValid(env,
        ['COMPILE_NODE', 'RECIPIENTS'])

    env['PROPERTIES_FILE'] = swfTools.checkEnvVar('PROPERTIES_FILE', 'SWFProperties.properties')

    println('[SWFProperties.groovy][isSWFPropertiesValid] Verify SWFProperties Config END')

    return configStatus
}

/**
 * readPropertiesFile is reading SWFProperties from given path
 *
 * @param env The Jenkins build environment. It must contain
 *            the following variables:
 *
 * <ul>
 *   <li>PROPERTIES_FILE - (optional) Path to File for PROPERTIES</li>
 * </ul>
 *
 **/
void readPropertiesFile(Object env) {
    println('[SWFProperties.groovy][readPropertiesFile] Read SWFProperties File START')
    try {
        load "${env.WORKSPACE}\\\\SWF_Project\\\\${env.PROPERTIES_FILE}"
        println('[SWFProperties.groovy][readPropertiesFile] Read SWFProperties File END')

        // copy SWFProperties
        copySWFProperties(env.PROPERTIES_FILE)

        // archiving
        archivingSWFProperties(env)
    } catch (e) {
        println('[SWFProperties.groovy][readPropertiesFile] WARNING: Read SWFProperties failed!!! ' +
            "Missing file: ${env.PROPERTIES_FILE}")
        println('[SWFProperties.groovy][readPropertiesFile] WARNING: this job is running with ' +
            'settings from job Properties Content only. -> UNSTABLE')
        currentBuild.result = 'UNSTABLE'
    }
}

/**
 * copySWFProperties copies Properties file to Project template folder SWF_Project
 * <p>
 * Copy files into [PROJECT_VARIANT_NAME]/data/SWFProperties
 *
 * @param propertiesFile Path to Properties file in WORKSPACE
 **/
void copySWFProperties(String propertiesFile) {
    CommandBuilder commandBuilder = new CommandBuilder()
    env['PROJECT_VARIANT_NAME'] = env.PROJECT_NAME + '_' + env.VARIANT_NAME

    println(
        '[SWFProperties.groovy][copySWFProperties] Copy properties file ' +
         env.PROJECT_VARIANT_NAME + '/data/SWFProperties START')

    bat commandBuilder.buildBat([
        "XCOPY \"%WORKSPACE%\\SWF_Project\\${propertiesFile}\" ^",
        "\"%WORKSPACE%\\${env.PROJECT_VARIANT_NAME}\\data\\SWFProperties\\" +
            "${propertiesFile}*\" /Y"
    ])

    println(
        '[SWFProperties.groovy][copySWFProperties] Copy properties file END')
}

/**
 * archivingSWFProperties is archiving and stashing outputs of SWF Properties stage
 * <p>
 * Following folders are archived:
 * <ul>
 *  <li>[PROJECT_VARIANT_NAME]/data/SWFProperties/*</li>
 * </ul>
 * @param env The Jenkins build environment. It must contain
 *            the following variables:
 *  <ul>
 *    <li>PROJECT_NAME - name of the project, e.g.: P12345 </li>
 *    <li>VARIANT_NAME - name of the variant, e.g.: BCM</li>
 *  </ul>
 **/
void archivingSWFProperties(Object env) {
    CommonTools commonTools = new CommonTools()
    env['PROJECT_VARIANT_NAME'] = env.PROJECT_NAME + '_' + env.VARIANT_NAME

    println(
        '[SWFProperties.groovy][archivingSWFProperties] Archiving SWFProperties START')

    commonTools.archiveArtifacts(
        env.PROJECT_VARIANT_NAME + '/data/SWFProperties/**/*')

    println('[SWFProperties.groovy][archivingSWFProperties] Archiving SWFProperties END')
}

/**
 * readSWFProperties is reading a prperties file to support your pipeline with Properties
 * env vars
 *
 * @param env The Jenkins build environment. It must contain
 *            the following variables:
 * <ul>
 *   <li>PROPERTIES      - Activation of Properties Reading
 *                                 [ON/OFF]</li>
 *   <li>PROPERTIES_FILE - (optional) Path to File for PROPERTIES</li>
 * </ul>
 **/
void readSWFProperties(Object env = this.env) {
    //import library tools
    Notifier notifier = new Notifier()
    SWFTools swfTools = new SWFTools()

    if (!swfTools.checkON(env.PROPERTIES)) {
        //default is OFF
        env['PROPERTIES'] = 'OFF'
        println('[SWFProperties.groovy] SWFProperties deactivated.')
        return
    }

    // verify Configuration first
    if (!isSWFPropertiesValid(env)) {
        // Config Error, throw error
        error 'SWFProperties failed due to an incorrect configuration of the ' +
        'environment variables. -> Check the environment variables in the job configuration.'
    }

    // add seperate Stage for checkout
    stage('Read SWF Properties') {
        try {
            println(
                '[SWFProperties.groovy] Read SWFProperties START.')

            // configure SWFProperties
            readPropertiesFile(env)

            println(
                '[SWFProperties.groovy] Read SWFProperties END.')
        } catch (e) {
            //Print error message
            println('[SWFProperties.groovy] SWFProperties failed.')
            notifier.onFailure(env, e)
        }
    }
}
