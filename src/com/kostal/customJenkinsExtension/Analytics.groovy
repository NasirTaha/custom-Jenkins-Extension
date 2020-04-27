package com.kostal.customJenkinsExtension

/*****************************************************************************
 * Helper class used by global variables to collect analytics data and
 * send it to backend Rest Api ar Elastic search indexes
 ****************************************************************************/

import com.kostal.customJenkinsExtension.logging.Logger
import java.text.SimpleDateFormat
import groovy.json.JsonBuilder

/**
* sendNodeDataToRestApi send node analytics data to backend REST Api
* It will run when node step overrided
* @param nodeName: Jenkins node where the job started
* @param url: REST Api URL
*/

void sendNodeDataToRestApi(String labelName, String url) {
    (debugMode, formatedCurrentTime) = getInitialData()
    println("labelName: ${labelName}")
    println("env.NODE_NAME: ${env.NODE_NAME}")
    Map<String, String> requestData = prepareRequestData(formatedCurrentTime)
    requestData['build_id'] = env.BUILD_NUMBER
    requestData['label_name'] = labelName
    requestData['build_url'] = env.BUILD_URL
    requestData['start_time'] = formatedCurrentTime
    requestData['build_duration'] = getBuildDuration()
    String jsonBody = new JsonBuilder(requestData)
    try {
        (statusCode, responseText) = sendRequest(url, jsonBody)

        if (debugMode) {
            printToConsole(statusCode, responseText)
        }
    }
    catch (UnsupportedOperationException | IOException ex) {
        if (debugMode) {
            println('Refuse')
            println("Exception:  ${ex.toString()}")
        }
        Logger.warning("[Analytics.groovy][sendNodeDataToRestApi] Exception: ${ex.toString()}")
    }
}

/**
* sendErrorDataToRestApi send error analytics data to backend REST Api
* It will run when error step overrided
* @param errorMessage: Jenkins error message
* @param url: REST Api URL
*/
void sendErrorDataToRestApi(String errorMessage, String url) {
    (debugMode, formatedCurrentTime) = getInitialData()

    Map<String, String> requestData = prepareRequestData(formatedCurrentTime)
    requestData['console_url'] = "${env.BUILD_URL}consoleFull"
    requestData['error_message'] = errorMessage
    String jsonBody = new JsonBuilder(requestData)
    try {
        (statusCode, responseText) = sendRequest(url, jsonBody)

        if (debugMode) {
            printToConsole(statusCode, responseText)
        }
    }
    catch (UnsupportedOperationException | IOException ex) {
        if (debugMode) {
            println('Refuse')
            println("Exception:  ${ex.toString()}")
        }
        Logger.warning("[Analytics.groovy][sendErrorDataToRestApi] Exception: ${ex.toString()}")
    }
}

/**
* sendStageDataToRestApi send stage analytics data to backend REST Api
* It will run when error step overrided
* @param projectName: Jenkins project name
* @param stageName: stage name
* @param flag: indecates  wether the this call is executae before or after stage
* @param url: REST Api URL
*/
void  sendStageDataToRestApi(String stageName, String flag, String url ) {
    (debugMode, formatedCurrentTime) = getInitialData()

    Map<String, String> requestData = prepareRequestData(formatedCurrentTime)
    requestData['duration'] = '0'
    requestData['stage_name'] = stageName
    requestData['flag'] = flag
    requestData['build_duration'] = getBuildDuration()
    String jsonBody = new JsonBuilder(requestData)
    try {
        (statusCode, responseText) = sendRequest(url, jsonBody)

        if (debugMode) {
            printToConsole(statusCode, responseText)
        }
    }
    catch (UnsupportedOperationException | IOException ex) {
        if (debugMode) {
            println('Refuse')
            println("Exception:  ${ex.toString()}")
        }
        Logger.warning("[Analytics.groovy][sendStageDataToRestApi] Exception: ${ex.toString()}")
    }
}

/**
* sendStageDataToElk send stage analytics data to backend Elastic search
* It will run when stage step overrided
* @param projectName: Jenkins project name
* @param stageName: stage name
* @param startTime: stage start time
* @param url: REST Api URL
*/
void sendStageDataToElk(String stageName, String url) {
    (debugMode, formatedCurrentTime) = getInitialData()

    Map<String, String> requestData = prepareRequestData(formatedCurrentTime)
    requestData['duration'] = '0'
    requestData['stage_name'] = stageName
    String jsonBody = new JsonBuilder(requestData)
    try {
        (statusCode, responseText) = sendRequest(url, jsonBody)

        if (debugMode) {
            printToConsole(statusCode, responseText)
        }
    }
    catch (UnsupportedOperationException | IOException | IllegalArgumentException ex) {
        println('Refuse')
        println("Exception:  ${ex.toString()}")
        Logger.warning("[Analytics.groovy][sendStageDataToElk]Exception: ${ex.toString()}")
    }
}

/**
* sendRequest send POST request the variuos Api

* @param url: REST Api URL
* @param message: POST body
* @param timeout: timeout used to wait for Api to get a response
*/
@SuppressWarnings('UnnecessarySetter')
Tuple sendRequest(String url, String message) {
    int timeout = 0
    try {
        timeout = Integer.parseInt(env.ANALYTICS_REST_TIMEOUT)
    }
    catch (NumberFormatException ex) {
        timeout =  1000
    }
    HttpURLConnection httpConnection = new URL(url).openConnection()
    httpConnection.setRequestMethod('POST')
    httpConnection.setConnectTimeout(timeout)
    httpConnection.setDoOutput(true)
    httpConnection.setRequestProperty('Content-Type', 'application/json')
    httpConnection.getOutputStream().write(message.getBytes('UTF-8'))
    int statusCode = httpConnection.getResponseCode()
    String responseText = httpConnection.getInputStream().getText()
    return [statusCode, responseText]
}

Tuple getInitialData() {
    SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS", Locale.getDefault())
    simpleDateFormat.setTimeZone(TimeZone.getTimeZone('UTC'))
    String formatedCurrentTime = simpleDateFormat.format(new Date())
    boolean debugMode = env.ANALYTICS_DEBUG == 'ON'
    return [debugMode, formatedCurrentTime]
}

void printToConsole(int statusCode, String responseText) {
    println "Response code: $statusCode"
    if (statusCode == HttpURLConnection.HTTP_OK || statusCode == HttpURLConnection.HTTP_CREATED) {
        println('Success')
    }
    else {
        println("Response: $responseText")
        println('Fail')
    }
}

Map<String, String> prepareRequestData(String formatedCurrentTime) {
    Map<String, String> requestData = [:]
    requestData['timestamp'] = formatedCurrentTime
    requestData['@timestamp'] = formatedCurrentTime
    requestData['endTime'] = formatedCurrentTime
    requestData['project_name'] = env.JOB_NAME
    requestData['jenkins_url'] = env.JENKINS_URL
    requestData['build_id'] = env.BUILD_NUMBER
    requestData['node_name'] = env.NODE_NAME
    requestData['build_url'] = env.BUILD_URL
    requestData['source_host'] = env.BUILD_URL
    requestData['executor_number'] = env.EXECUTOR_NUMBER
    requestData['source'] = 'jenkins'
    requestData['@version'] = '1'
    return requestData
}

boolean isConfigurationValid(List<String> variablesToCheck) {
    boolean debugMode = env.ANALYTICS_DEBUG == 'ON'
    for (String variable : variablesToCheck) {
        if (env[variable] == null || env[variable] == '') {
            if (debugMode) {
                println("AnalyticsEnvironment var ${variable} is missing!")
            }
            return false
        }
    }
    return true
}

@NonCPS
int getBuildDuration() {
    final int CHARS_TO_CUT = 2
    int duration
    Item currentProject = Jenkins.instance.getItemByFullName(env.JOB_NAME)
    String description = currentProject.getDescription()

    if (description == null) {
        return Integer.MIN_VALUE
    }

    try {
        if (description.count('_') == CHARS_TO_CUT && description.indexOf('=') != -1) {
            int firstIndex = description.indexOf('_') + 1
            int lastIndex = description.lastIndexOf('_') - 1
            String durationPhrase = description[firstIndex..lastIndex]
            String durationPart = durationPhrase.split('=')[1]

            if (durationPart.length() > 1 && durationPart.charAt(durationPart.length() - 1) == 'h') {
                int unitIndex = durationPart.length() - CHARS_TO_CUT
                duration = Integer.parseInt(durationPart[0..unitIndex])
            }
        }
    }
    catch (NumberFormatException ex) {
        duration = Integer.MIN_VALUE
    }
    return duration
}
