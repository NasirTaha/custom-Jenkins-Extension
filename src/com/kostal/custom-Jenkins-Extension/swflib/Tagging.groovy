/*
 * SW-Factory Library module to tag tested build (revision) in SCM system
 * like GIT or SVN
*/
package com.kostal.pipelineworks.swflib

import com.kostal.pipelineworks.Checkout

/**
 * isTaggingConfigurationValid is verifiying Tagging config by checking env vars
 * and setting default values for optional env vars
 *
 * @param env The Jenkins build environment. It must contain
 *            the following variables:
 *  <ul>
 *    <li>REPOSITORY_URL     - SVN repository URL (example:
 * https://debesvn001/kostal/lk_ae_internal/AEP/AEP5/CI_Template/Appl/)</li>
 *    <li>TAG_FOLDER      - (optional) specify a subfolder where tags
 *                          are stored</li>
 *    <li>TAG_NAME        - (optional) specify a name of the tag</li>
 *    <li>REPOSITORY_TOOL - (optional) SW Repository Tool, default:
 *                          SVN [SVN/GIT]</li>
 *   <li>RECIPIENTS              - list of e-mail recipients</li>
 *  </ul>
 **/
boolean isTaggingConfigurationValid(Object env) {
    SWFTools swfTools = new SWFTools()

    println('[Tagging.groovy][isTaggingConfigurationValid] Verify Tagging Config START')

    boolean configStatus = swfTools.isConfigurationValid(env,
        ['REPOSITORY_URL', 'RECIPIENTS'])

    // verify optional parameter and set default values
    env['REPOSITORY_TOOL'] = swfTools.checkEnvVar('REPOSITORY_TOOL', 'SVN')
    env['TAG_FOLDER'] = swfTools.checkEnvVar(env.TAG_FOLDER + '/', '')

    if ((env.TAG_NAME != null) && (env.TAG_NAME != '')) {
        env['TAG_NAME'] = env.TAG_NAME + '/'
    }
    else {
        env['TAG_NAME'] = 'SWF_BUILD_' + JOB_BASE_NAME + '_' + BUILD_ID
    }

    // break build, if Repo is GIT
    if (env.REPOSITORY_TOOL == 'GIT') {
        error '[Tagging.groovy][tagScm] ERROR: GIT tagging is not supported, ' +
        'contact B.Kraeher, AEP5.'
    }

    println('[Tagging.groovy][isTaggingConfigurationValid] Verify Tagging Config END')

    return configStatus
}

/**
 * addBuildDescription is writing description to current Jenkins build job.
 * @param env The Jenkins build environment. It must contain
 *            the following variables:
 *  <ul>
 *    <li>REPOSITORY_TOOL - (optional) SW Repository Tool, default:
 *                          SVN [SVN/GIT]</li>
 *    <li>TAG_FOLDER      - (optional) specify a subfolder where tags
 *                          are stored</li>
 *    <li>TAG_NAME        - (optional) specify a name of the tag</li>
 *  </ul>
**/
void addBuildDescription(Object env) {
    if ((env.TAG_FOLDER != null) && (env.TAG_NAME != null)) {
        currentBuild.description = (
            "#${BUILD_NUMBER}, ${env.REPOSITORY_TOOL} " +
            "tag: ${env.TAG_FOLDER}${env.TAG_NAME}")
    }
    else {
        println(
            '[Tagging.groovy][addBuildDescription] INFO: TAG_FOLDER ' +
            'and/or TAG_NAME are not set. > build description is not set.')
    }
}

/**
 * tagScm is tagging a build in REPOSITORY_TOOL. Module will add pipeline
 * stage 'SCM tag'.
 * @param env The Jenkins build environment. It must contain
 *            the following variables:
 *  <ul>
 *    <li>REPOSITORY_URL     - SVN repository URL (example:
 * https://debesvn001/kostal/lk_ae_internal/AEP/AEP5/CI_Template/Appl/)</li>
 *    <li>TAG_FOLDER      - (optional) specify a subfolder where tags
 *                          are stored</li>
 *    <li>TAG_NAME        - (optional) specify a name of the tag</li>
 *    <li>REPOSITORY_TOOL - (optional) SW Repository Tool, default:
 *                          SVN [SVN/GIT]</li>
 *  </ul>
 **/
void tagScm(Object env = this.env) {
    SWFTools swfTools = new SWFTools()
    if (swfTools.checkON(env.TAG_SCM)) {
        Notifier notifier = new Notifier()

        stage('SCM tag') {
            // verify Configuration first
            if (!isTaggingConfigurationValid(env)) {
                // Config Error, throw error
                error 'Tagging failed due to an incorrect configuration of the environment variables. ' +
                '-> Check the environment variables in the job configuration.'
            }
            try {
                if (env.REPOSITORY_TOOL == 'SVN') {
                    bat Checkout.tagSvn(env,
                        env['REPOSITORY_URL'],
                        env['REPOSITORY_URL'] + '/tags/' + env.TAG_FOLDER,
                        env['TAG_NAME'])

                    addBuildDescription(env)
                }
                println('[Tagging.groovy][tagScm] SCM tag end.')
            } catch (e) {
                println('[Tagging.groovy][tagScm] tagScm failed.')
                notifier.onFailure(env, e)
            }
        }
    } else {
        println ('[SWFLib.jenkinsfile] MAIL_ON_SUCCESS deactivated.')
    }
}
