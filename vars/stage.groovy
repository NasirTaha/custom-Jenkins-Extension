#!/usr/bin/env groovy
import com.kostal.custom-Jenkins-Extension.Analytics

/**
* Override Jenkins built-in stage step by adding some calls to external Rest 
* application and then return the original built-in step  execution
*
* @param name: stage name 
* @param body: the closure script to execute inside stage
* @return original built-in steps.stage with it's closure
*/
def call(String name, Closure body) {
    println("stage.groovys: env.NODE_NAME: ${env.NODE_NAME}")
    Analytics analytics = new Analytics()
    String stageAnalyticsFlag = env.ANALYTICS_STAGE
    String debugMode = env.ANALYTICS_DEBUG
    /*Check ANALYTICS_STAGE flag:
    * if flag is ON: continue override execuation
    * if flag is OFF or net set in project's settings
    * or URLs are not vaild: execute only the buit-in functionality
    */
    boolean configStatus = analytics.isConfigurationValid(['ANALYTICS_ELK_SWF_URL', 'ANALYTICS_ELK_URL', 
    'ANALYTICS_API_ERROR_URL', 'ANALYTICS_API_JOB_URL', 'ANALYTICS_API_STAGE_URL'])
    
    if (stageAnalyticsFlag != 'ON' || !configStatus) {
        return steps.stage("$name") {
            body()
        }
    }
    if (debugMode == 'ON') {
        println("Override stage: $name")
    }

    analytics.sendStageDataToRestApi(name, 'start', env.ANALYTICS_API_STAGE_URL)
    
    return steps.stage("$name") {
        body()

        analytics.sendStageDataToRestApi(name, 'end', env.ANALYTICS_API_STAGE_URL)
        analytics.sendStageDataToElk(name, env.ANALYTICS_ELK_URL)
    }
}
