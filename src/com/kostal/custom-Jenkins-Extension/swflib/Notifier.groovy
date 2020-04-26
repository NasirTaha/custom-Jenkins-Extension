/**
 * SW-Factory Library module to notify RECIPIENTS about errors in Jenkins
 * build process
**/
package com.kostal.pipelineworks.swflib

/**
 * onFailure function is sending a message to mail RECIPIENTS from failing
 * SW-Factory Lib builds
 * <p>
 * Several informations are included in mail:
 * <ul>
 *  <li>JOB_NAME</li>
 *  <li>BUILD_NUMBER</li>
 *  <li>BUILD_URL</li>
 * </ul>
 * currentBuild.result is set to 'FAILED' and build is aborted. Function is
 * also throwing error message from parameter 'e'
 *
 * @param env The Jenkins build environment. It must contain
 *            the following variables:
 *  <ul>
 *    <li>RECIPIENTS   - mail recipients for this notifier message.
 *                       Add them as comma separated </li>
 *    <li>JOB_NAME     - Jenkins job name set by Jenkins environment </li>
 *    <li>BUILD_NUMBER - Jenkins build number set by Jenkins environment </li>
 *    <li>BUILD_URL    - Jenkins build URL set by Jenkins environment  </li>
 *  </ul>
 * @param e    error message out of Jenkins environment
 **/
void onFailure(Object env, Object e) {
    //import pipelineworks notifier
    SWFTools swfTools = new SWFTools()
    Object mailTo = env.RECIPIENTS
    String mailSubject = (
        "ITSW Build FAILED: Job '${env.JOB_NAME} [${env.BUILD_NUMBER}]'")
    String mailBody = """FAILED: Job '${env.JOB_NAME} [${env.BUILD_NUMBER}]':
         For details please see Console log of Job [${env.BUILD_NUMBER}],
         ${env.BUILD_URL}
    """

    //sending mail
    String defaultSender = 'jenkins@kostal.com'
    mail (
        bcc: '',
        body: mailBody,
        cc: '',
        from: defaultSender,
        replyTo: defaultSender,
        subject: mailSubject,
        to: mailTo,
        mimeType: 'text/html'
    )
    println('[Notifier.groovy][onFailure] Send mail to: ' + env.RECIPIENTS)

    // check, if activated with 'ON'
    if (swfTools.checkON(env.NOTIFIER_TEST)) {
        //substitute DYN_RESOURCES_BUILD env var
        println('[Notifier.groovy][onFailure] Test functionality active')
    }
    else {
        //default is Integration
        env['NOTIFIER_TEST'] = 'OFF'
        currentBuild.result = 'FAILED'
        throw e
    }
}

/**
 * onSuccess function is sending a message to mail RECIPIENTS from successful
 * finished SW-Factory Lib builds
 * <p>
 * Several informations are included in mail:
 * <ul>
 *  <li>JOB_NAME</li>
 *  <li>BUILD_NUMBER</li>
 *  <li>BUILD_URL</li>
 * </ul>
 *
 * currentBuild.result is set to 'FAILED' and build is aborted. Function is
 * also throwing error message from parameter 'e'
 *
 * @param env The Jenkins build environment. It must contain
 *            the following variables:
 *  <ul>
 *    <li>RECIPIENTS     - mail recipients for this notifier message.
 *                         Add them as comma separated </li>
 *    <li>JOB_NAME       - Jenkins job name set by Jenkins environment </li>
 *    <li>BUILD_NUMBER   - Jenkins build number set by Jenkins environment</li>
 *    <li>BUILD_URL      - Jenkins build URL set by Jenkins environment </li>
 *    <li>MAIL_ON_SUCCESS - mail on success activation [ON|OFF] </li>
 *  </ul>
 **/
void onSuccess(Object env = this.env) {
    SWFTools swfTools = new SWFTools()
    if (swfTools.checkON(env.MAIL_ON_SUCCESS)) {
        Object mailTo = env.RECIPIENTS
        String mailSubject = (
            "ITSW Build SUCCESS: Job '${env.JOB_NAME} [${env.BUILD_NUMBER}]'")
        String mailBody = """SUCCESS: Job '${env.JOB_NAME} [${env.BUILD_NUMBER}]':
        For details please see Console log of Job [${env.BUILD_NUMBER}],
        ${env.BUILD_URL}
        """

        //sending mail
        String defaultSender = 'jenkins@kostal.com'

        mail (
            bcc: '',
            body: mailBody,
            cc: '',
            from: defaultSender,
            replyTo: defaultSender,
            subject: mailSubject,
            to: mailTo,
            mimeType: 'text/html'
        )

        println('[Notifier.groovy][onSuccess] Send mail to: ' + env.RECIPIENTS)
    } else {
        println ('[SWFLib.jenkinsfile] MAIL_ON_SUCCESS deactivated.')
    }
}
