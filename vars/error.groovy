#!/usr/bin/env groovy
import com.kostal.custom-Jenkins-Extension.Analytics

/**
* Override Jenkins built-in error step by adding some calls to external Rest 
* application and then return the original built-in  error step  execution
*
* @param message: error message 
* @return original built-in steps.error with it's closure
*/
def call(String message) {
    Analytics analytics = new Analytics()
    String debugMode =   env.ANALYTICS_DEBUG
    String errorOverrideFlag =   env.ANALYTICS_ERROR
    /*Check ANALYTICS_ERROR flag:
    * if flag is ON: continue override execuation
    * if flag is OFF or net set in project's settings: execute only the buit-in functionality
    */
    boolean configStatus = analytics.isConfigurationValid(['ANALYTICS_ELK_SWF_URL', 'ANALYTICS_ELK_URL', 
    'ANALYTICS_API_ERROR_URL', 'ANALYTICS_API_JOB_URL', 'ANALYTICS_API_STAGE_URL'])

    if (errorOverrideFlag != 'ON' || !configStatus) {
        return steps.error(message)
    }
    if (debugMode == 'ON') {
        println("Error Analytics: $message")
    }
    echo "Error: $message"
    analytics.sendErrorDataToRestApi(message, env.ANALYTICS_API_ERROR_URL)
    
    return steps.error(message)
}
