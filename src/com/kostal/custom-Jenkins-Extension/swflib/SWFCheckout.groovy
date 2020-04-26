/*
 * SW-Factory Library module is checking out a project from SCM system and
 * building up a parallel temporary workspace to control SW-Factory data
*/
package com.kostal.pipelineworks.swflib

import com.kostal.pipelineworks.CommandBuilder
import com.kostal.pipelineworks.CheckoutTools
import com.kostal.pipelineworks.Checkout
import com.kostal.pipelineworks.Utility

/**
 * isSWFCheckoutConfigurationValid is verifiying SWFCheckout config by checking env vars
 * and setting default values for optional env vars
 *
 * @param env The Jenkins build environment. It must contain
 *            the following variables:
 *  <ul>
 *    <li>PROJECT_NAME       - name of the project, e.g.: P12345 </li>
 *    <li>VARIANT_NAME       - name of the variant, e.g.: BCM</li>
 *    <li>REPOSITORY_URL     - SVN repository URL (example:
 * https://debesvn001/kostal/lk_ae_internal/AEP/AEP5/CI_Template/Appl/)</li>
 *    <li>REPOSITORY_DEV_REP - (otpional) SVN Development repository [default: trunk]</li>
 *    <li>REPOSITORY_TOOL    - (otpional) SW Repository Tool, default: SVN [SVN/GIT]</li>
 *    <li>COMPILE_NODE       - Server Node Name</li>
 *    <li>RECIPIENTS         - list of e-mail recipients</li>
 *  </ul>
 **/
boolean isSWFCheckoutConfigurationValid(Object env) {
    SWFTools swfTools = new SWFTools()

    println('[SWFCheckout.groovy][isSWFCheckoutConfigurationValid] Verify SWFCheckout Config START')

    boolean configStatus = swfTools.isConfigurationValid(env,
        ['REPOSITORY_URL', 'PROJECT_NAME', 'VARIANT_NAME', 'COMPILE_NODE', 'RECIPIENTS'])

    //check Basic project environment
    env['REPOSITORY_DEV_REP'] = swfTools.checkEnvVar('REPOSITORY_DEV_REP', 'trunk')
    env['REPOSITORY_TOOL'] = swfTools.checkEnvVar('REPOSITORY_TOOL', 'SVN')

    println('[SWFCheckout.groovy][isSWFCheckoutConfigurationValid] Verify SWFCheckout Config END')

    return configStatus
}

/**
 * cleanWorkspace cleans up temporary project workspace
 * <p>
 * Folder: [WORKSPACE]/[PROJECT_VARIANT_NAME]/*
 *
 * @param env The Jenkins build environment. It must contain
 *            the following variables:
 * <ul>
 *     <li>PROJECT_NAME - name of the project, e.g.: P12345 </li>
 *     <li>VARIANT_NAME - name of the variant, e.g.: BCM</li>
 * </ul>
 **/
void cleanWorkspace(Object env) {
    //import library tools
    CommandBuilder commandBuilder = new CommandBuilder()
    Utility swfUtility = new Utility()

    env.PROJECT_VARIANT_NAME = env['PROJECT_NAME'] + '_' + env['VARIANT_NAME']
    String tmpWorkspace = swfUtility.toWindowsPath(env.WORKSPACE + '/' + env.PROJECT_VARIANT_NAME)
    // Clean output folder
    println(
        '[SWFCheckout.groovy][cleanWorkspace] Clean up SWF ' +
        'project workspace: ' + tmpWorkspace)

    // subdir report
    tmpWorkspace = swfUtility.toWindowsPath(env.WORKSPACE + '/' + env.PROJECT_VARIANT_NAME + '/report')
    bat commandBuilder.buildBat([
        "IF EXIST ${tmpWorkspace} RMDIR " +
        "/S/Q ${tmpWorkspace}"
    ])
    // subdir data
    tmpWorkspace = swfUtility.toWindowsPath(env.WORKSPACE + '/' + env.PROJECT_VARIANT_NAME + '/data')
    bat commandBuilder.buildBat([
        "IF EXIST ${tmpWorkspace} RMDIR " +
        "/S/Q ${tmpWorkspace}"
    ])
    // subdir log
    tmpWorkspace = swfUtility.toWindowsPath(env.WORKSPACE + '/' + env.PROJECT_VARIANT_NAME + '/log')
    bat commandBuilder.buildBat([
        "IF EXIST ${tmpWorkspace} RMDIR " +
        "/S/Q ${tmpWorkspace}"
    ])
    println(
        '[SWFCheckout.groovy][cleanWorkspace] Clean up SWF ' +
        'project workspace END')
}

/**
 * doCheckout is doing the checkout from SCM with logic
 * 'UpdateWithCleanUpdater'
 *
 * <p>
 * Following path is checked out: [REPOSITORY_URL]/[REPOSITORY_DEV_REP]
 * <p>
 * Repository tool can be configured with REPOSITORY_TOOL. Available Tools are
 * SVN or GIT
 *
 * @param env The Jenkins build environment. It must contain
 *            the following variables:
 *  <ul>
 *    <li>REPOSITORY_URL     - SVN repository URL (example:
 * https://debesvn001/kostal/lk_ae_internal/AEP/AEP5/CI_Template/Appl/)</li>
 *    <li>REPOSITORY_DEV_REP - (optional) SVN Development repository [default: trunk]</li>
 *    <li>REPOSITORY_TOOL    - (optional) SW Repository Tool, default: SVN [SVN/GIT]</li>
 *  </ul>
 **/
void doCheckout(Object env) {
    CheckoutTools checkoutTools = new CheckoutTools()
    SWFTools swfTools = new SWFTools()
    Utility swfUtility = new Utility()

    env.PROJECT_REPOSITORY = env.REPOSITORY_URL + env.REPOSITORY_DEV_REP + '/'
    env.REPOSITORY_TOOL = swfTools.checkEnvVar('REPOSITORY_TOOL', 'SVN')

    println(
        '[SWFCheckout.groovy][doCheckout] Checkout repository: ' +
        env.PROJECT_REPOSITORY)
    println(
        '[SWFCheckout.groovy][doCheckout] Checkout workspace: ' +
        swfUtility.toWindowsPath(env.WORKSPACE + '/SWF_Project'))

    if (env.REPOSITORY_TOOL == 'SVN') {
        List checkoutList = []
        checkoutList << [
            env.PROJECT_REPOSITORY, 'SWF_Project/', 'UpdateWithCleanUpdater'
        ]

        println('[SWFCheckout.groovy][doCheckout] Checkout Tool: SVN')

        checkoutTools.checkoutAll(Checkout.buildCheckout(env, '', checkoutList))
    }
    else if (env.REPOSITORY_TOOL == 'GIT') {
        println('[SWFCheckout.groovy][doCheckout] Checkout Tool: GIT')
        checkoutTools.checkoutAll([
            Checkout.buildGitCheckout(
                env, env.REPOSITORY_URL, 'SWF_Project/', env.REPOSITORY_DEV_REP
            )])
    }
    else {
        println(
            '[SWFCheckout.groovy][doCheckout] ERROR: Check configuration of ' +
            'parameter REPOSITORY_TOOL: ' + env.REPOSITORY_TOOL)
    }
}

/**
 * checkout is preparing workspace for a SW-Factory Pipeline.
 * <p>
 * Main features are:
 * <ul>
 *  <li>- Cleaning temp project workspace</li>
 *  <li>- Checking out repository from SCM [SVN/GIT]</li>
 * </ul>
 * @param env The Jenkins build environment. It must contain
 *            the following variables:
 *  <ul>
 *    <li>PROJECT_NAME       - name of the project, e.g.: P12345 </li>
 *    <li>VARIANT_NAME       - name of the variant, e.g.: BCM</li>
 *    <li>REPOSITORY_URL     - SVN repository URL (example:
 * https://debesvn001/kostal/lk_ae_internal/AEP/AEP5/CI_Template/Appl/)</li>
 *    <li>REPOSITORY_DEV_REP - (optional) SVN Development repository [default: trunk]</li>
 *    <li>REPOSITORY_TOOL    - (optional) SW Repository Tool, default: SVN [SVN/GIT]</li>
 *  </ul>
 **/
void checkout(Object env) {
    Notifier notifier = new Notifier()
    Utility utility = new Utility()

    try {
        // print info of running host
        println(
            '[SWFCheckout.groovy][checkout] SW Checkout START on node: ' +
            utility.getHostName())
        // verify Configuration first
        if (!isSWFCheckoutConfigurationValid(env)) {
            // Config Error, throw error
            error 'SWFCheckout failed due to an incorrect configuration of the environment variables. ' +
            '-> Check the environment variables in the job configuration.'
        }
        // Prints all the environment variables
        bat 'set'
        // clean workspace
        cleanWorkspace(env)
        // call checkout
        doCheckout(env)
        println('[SWFCheckout.groovy][checkout] SW Checkout END.')
    } catch (e) {
        println(
            '[SWFCheckout.groovy][checkout] checkout failed. ' +
            'Check Repository URL.')
        notifier.onFailure(env, e)
    }
}

/**
 * checkoutStage is adding stage and preparing workspace for a SW-Factory
 * pipeline by checking out data from SCM.
 *
 * <p>
 * Main features are:
 * <ul>
 *  <li>- Adding Stage "SCM Checkout"</li>
 *  <li>- Cleaning temp project workspace</li>
 *  <li>- Checking out repository from SCM [SVN/GIT]</li>
 * </ul>
 * @param env The Jenkins build environment. It must contain
 *            the following variables:
 *  <ul>
 *    <li>PROJECT_NAME       - name of the project, e.g.: P12345 </li>
 *    <li>VARIANT_NAME       - name of the variant, e.g.: BCM</li>
 *    <li>WORKSPACE          - Workspace folder</li>
 *    <li>REPOSITORY_URL     - SVN repository URL (example:
 * https://debesvn001/kostal/lk_ae_internal/AEP/AEP5/CI_Template/Appl/)</li>
 *    <li>REPOSITORY_DEV_REP - (optional) SVN Development repository [default: trunk]</li>
 *    <li>REPOSITORY_TOOL    - (optional) SW Repository Tool, default: SVN [SVN/GIT]</li>
 *  </ul>
 **/
void checkoutStage(Object env = this.env) {
    Notifier notifier = new Notifier()
    Utility utility = new Utility()

    // add seperate Stage for checkout
    try {
        stage('SCM Checkout') {
            // verify Configuration first
            if (!isSWFCheckoutConfigurationValid(env)) {
                // Config Error, throw error
                error 'SWFCheckout failed due to an incorrect configuration of the environment variables. ' +
                '-> Check the environment variables in the job configuration.'
            }
            // print info of running host
            println(
                '[SWFCheckout.groovy][checkoutStage] SW Checkout START on node: ' +
                utility.getHostName())
            // Prints all the environment variables
            bat 'set'
            // clean workspace
            cleanWorkspace(env)
            // call checkout
            doCheckout(env)
            println('[SWFCheckout.groovy][checkoutStage] SW Checkout END.')
        }
    } catch (e) {
        echo(
            '[SWFCheckout.groovy][checkoutStage] swflib_checkout failed. ' +
            'Check Repository URL.')
        notifier.onFailure(env, e)
    }
}
