/*
 * Module to store global tools for SW-Factory Library
*/
package com.kostal.custom-Jenkins-Extension.swflib


import com.kostal.custom-Jenkins-Extension.Analytics
import groovy.json.JsonBuilder
import hudson.EnvVars


/**
 * reportToBuildAnalytics is calling subfunctionality to report SW-Factory environment data to
 * Build Analytics
 *
 **/
void reportToBuildAnalytics(Object env = this.env) {
    Analytics analytics = new Analytics()
    println(
        '[SWFTools.groovy][RTBA] Report to ' +
        'Build Analytics START')

    // add call to report to Build Analytics here
    boolean swfAnalytics = env.ANALYTICS_SWF == 'ON'
    boolean configStatus = analytics.isConfigurationValid(['ANALYTICS_ELK_SWF_URL', 'ANALYTICS_ELK_URL',
    'ANALYTICS_API_ERROR_URL', 'ANALYTICS_API_JOB_URL', 'ANALYTICS_API_STAGE_URL'])

    if (swfAnalytics && configStatus) {
        Map<String, String> requestData = [:]
        try {
            (debugMode, formatedCurrentTime, now) = analytics.getInitialData()
            EnvVars envList = env.getEnvironment()
            if (envList.size() == 0) {
                println ('No environment variables available')
            }
            requestData['@timestamp'] = "$formatedCurrentTime"
            envList.eachWithIndex { entry, i ->
                if (entry.key != null && !entry.key.isEmpty() && entry.value != null && !entry.value.isEmpty()) {
                    requestData[entry.key.replace('\"', '')] = entry.value.replace('\"', '')
                }
            }
            String jsonBody = new JsonBuilder(requestData)
            if (debugMode) {
                println('Send to sfinfo')
                print(jsonBody)
            }

            String url = env.ANALYTICS_ELK_SWF_URL
            (statusCode, responseText) = analytics.sendRequest(url, jsonBody)
            if (debugMode) {
                analytics.printToConsole(statusCode, responseText)
            }
        }
        catch (UnsupportedOperationException | IOException ex) {
            println("Exception: ${ex.toString()}")
        }
    }

    println(
        '[SWFTools.groovy][reportToBuildAnalytics] Report to Analytics END')
}
