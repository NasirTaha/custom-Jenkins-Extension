#!/usr/bin/env groovy
import com.kostal.customJenkinsExtension.Analytics

/**
* Override Jenkins built-in node step by adding some calls to external Rest 
* application and then return the original built-in step  execution
*
* @param name: node name 
* @param body: the closure script to execute inside node
* @return original built-in steps.stage with it's closure
*/
def call(String name, Closure body) {
    println("node.groovys: env.NODE_NAME: ${env.NODE_NAME}")
    Analytics analytics = new Analytics()

    boolean allowStageAnalytics = env.ANALYTICS_STAGE == 'ON'
    boolean debugMode = env.ANALYTICS_DEBUG == 'ON'
    /*Check ANALYTICS_STAGE flag:
    * if flag is ON: continue override execuation
    * if flag is OFF or net set in project's settings: execute only the buit-in functionality
    */
    boolean validConfig = analytics.isConfigurationValid(['ANALYTICS_ELK_SWF_URL', 'ANALYTICS_ELK_URL', 
    'ANALYTICS_API_ERROR_URL', 'ANALYTICS_API_JOB_URL', 'ANALYTICS_API_STAGE_URL'])

    if (!allowStageAnalytics || !validConfig) {
        if (name == null || name == '') {
            return steps.node{
                body()
            }
        }
        else {
             return steps.node(name){
                body()
            }
        }
    }

    if(debugMode){
        println("Override node: $name")
    }
    if (name == null || name == '') {
        analytics.sendNodeDataToRestApi('master', env.ANALYTICS_API_JOB_URL)
        return steps.node{
            body()
        }
    }
    else{
        analytics.sendNodeDataToRestApi(name, env.ANALYTICS_API_JOB_URL)
        return steps.node(name){
            body()
        }
    }
}

def call(Closure body) {
    println("node.groovys: env.NODE_NAME: ${env.NODE_NAME}")
    Analytics analytics = new Analytics()

    boolean allowStageAnalytics = env.ANALYTICS_STAGE == 'ON'
    boolean debugMode = env.ANALYTICS_DEBUG == 'ON'
    /*Check ANALYTICS_STAGE flag:
    * if flag is ON: continue override execuation
    * if flag is OFF or net set in project's settings: execute only the buit-in functionality
    */
    boolean validConfig = analytics.isConfigurationValid(['ANALYTICS_ELK_SWF_URL', 'ANALYTICS_ELK_URL', 
    'ANALYTICS_API_ERROR_URL', 'ANALYTICS_API_JOB_URL', 'ANALYTICS_API_STAGE_URL'])

    if (!allowStageAnalytics || !validConfig) {
        return steps.node{
            body()
        }
    }

    if(debugMode){
        println("Override empty node")
    }
    analytics.sendNodeDataToRestApi('master', env.ANALYTICS_API_JOB_URL)
    
    return steps.node{
        body()
    }
}