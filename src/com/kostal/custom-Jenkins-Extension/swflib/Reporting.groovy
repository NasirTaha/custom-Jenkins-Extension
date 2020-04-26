/*
 * SW-Factory Library module to report Results of SW-Factory Lib
 * pipeline project
*/
package com.kostal.pipelineworks.swflib

import com.kostal.pipelineworks.CITools

/**
 * isReportingConfigurationValid is verifiying Reporting config by checking env vars
 * and setting default values for optional env vars
 *
 * @param env The Jenkins build environment. It must contain
 *            the following variables:
 * <ul>
 *   <li>PROJECT_NAME            - name of the project, e.g.: P12345 </li>
 *   <li>VARIANT_NAME            - name of the variant, e.g.: BCM</li>
 *   <li>RECIPIENTS              - e-mail recipients list </li>
 * </ul>
 **/
boolean isReportingConfigurationValid(Object env) {
    SWFTools swfTools = new SWFTools()

    println('[Reporting.groovy][isReportingConfigurationValid] Verify Reporting Config START')

    boolean configStatus = swfTools.isConfigurationValid(env,
        ['PROJECT_NAME', 'VARIANT_NAME', 'RECIPIENTS'])

    println('[Reporting.groovy][isReportingConfigurationValid] Verify Reporting Config END')

    return configStatus
}

/**
 * initReports is getting report data from SW-Factory lib modules
 * <p>
 * SW-Factory Lib modules are copying their report data into a temporary
 * workspace. This data is then stashed to be usable in this reporting stage.
 * This is a list of stashed sources:
 *  <ul>
 *    <li>STATIC_RESOURCES  - 'swflib_statresource_report'</li>
 *    <li>DYNAMIC_RESOURCES - 'swflib_dynresource_report'</li>
 *    <li>STD_SW_ANALYSIS   - 'swflib_stdsw_report'</li>
 *    <li>QAC_ANALYSIS      - 'swflib_qac_report'</li>
 *    <li>TESSY_ANALYSIS    - 'swflib_tessy_report'</li>
 *    <li>DOXYGEN           - 'swflib_doxygen_report'</li>
 *  </ul>
 * <p>
 *
 * To activate a reporting from a test stage, you have to set following
 * parameter to ON.
 *
 * @param env The Jenkins build environment. It must contain
 *            the following variables:
 *  <ul>
 *    <li>STATIC_RESOURCES  - reporting for Static Resource Validation
 *                            activated [ON/OFF]</li>
 *    <li>DYNAMIC_RESOURCES - reporting for Dynamic Resource Validation
 *                            activated [ON/OFF]</li>
 *    <li>STD_SW_ANALYSIS   - reporting for Std-SW Validation activated
 *                            [ON/OFF]</li>
 *    <li>QAC_ANALYSIS      - reporting for MISRA/QAC Validation activated
 *                            [ON/OFF]</li>
 *    <li>PRQA_ANNOTATION_COLLECTOR - reporting for PRQA Annotation Collector activated
 *                            [ON/OFF]</li>
 *    <li>TESSY_ANALYSIS    - reporting for Tessy Validation activated
 *                            [ON/OFF]</li>
 *    <li>DOXYGEN           - Activation of Doxygen
 *                            [ON/OFF]</li>
 *  </ul>
**/
void initReports(Object env) {
    SWFTools swfTools = new SWFTools()
    String unstashingMessage = '[Reporting.groovy][initReports] unstashing '

    println('[Reporting.groovy][initReports] unstashing Report data START')

    if (swfTools.checkON(env.STATIC_RESOURCES)) {
        unstash 'swflib_statresource_report'
        println(unstashingMessage + 'swflib_statresource_report done.')
    }

    if (swfTools.checkON(env.STD_SW_ANALYSIS)) {
        unstash 'swflib_stdsw_report'
        println(unstashingMessage + 'swflib_stdsw_report done.')
    }

    if (swfTools.checkON(env.QAC_ANALYSIS)) {
        unstash 'swflib_qac_report'
        println(unstashingMessage + 'swflib_qac_report done.')
    }

    if (swfTools.checkON(env.PRQA_ANNOTATION_COLLECTOR)) {
        unstash 'swflib_PrqaAnnotationCollector_report'
        println(unstashingMessage + 'swflib_PrqaAnnotationCollector_report done.')
    }

    if (swfTools.checkON(env.DOXYGEN)) {
        unstash 'swflib_doxygen_report'
        println(unstashingMessage + 'swflib_doxygen_report done.')
    }

    if (swfTools.checkON(env.TESSY_ANALYSIS)) {
        unstash 'swflib_tessy_report'
        println(unstashingMessage + 'swflib_tessy_report done.')
    }

    if (swfTools.checkON(env.DYNAMIC_RESOURCES)) {
        unstash 'swflib_dynresource_report'
        println(unstashingMessage + 'swflib_dynresource_report done.')
    }

    if (swfTools.checkON(env.PSBF_ANALYSIS)) {
        unstash 'swflib_PsBf_Analysis_report'
        println(unstashingMessage + 'swflib_PsBf_Analysis_report done.')
    }

    println('[Reporting.groovy][initReports] unstashing Report data END')
}

/**
 * This function is building SWF_Report html page and publish it
 * <p>
 * To activate a reporting from a test stage, you have to set following
 * parameter to ON.
 * @param env The Jenkins build environment. It must contain
 *            the following variables:
 *  <ul>
 *    <li>PROJECT_NAME      - name of the project, e.g.: P12345 </li>
 *    <li>VARIANT_NAME      - name of the variant, e.g.: BCM</li>
 *    <li>STATIC_RESOURCES  - reporting for Static Resource Validation
 *                            activated [ON/OFF]</li>
 *    <li>DYNAMIC_RESOURCES - reporting for Dynamic Resource Validation
 *                            activated [ON/OFF]</li>
 *    <li>STD_SW_ANALYSIS   - reporting for Std-SW Validation activated
 *                            [ON/OFF]</li>
 *    <li>QAC_ANALYSIS      - reporting for MISRA/QAC Validation activated
 *                            [ON/OFF]</li>
 *    <li>TESSY_ANALYSIS    - reporting for Tessy Validation activated
 *                            [ON/OFF]</li>
 *    <li>QAC_VERSION       - used QAC version [QAC/QAC9]</li>
 *    <li>DOXYGEN           - Activation of Doxygen
 *                            [ON/OFF]</li>
 *  </ul>
**/
@SuppressWarnings('BuilderMethodWithSideEffects') // Not a builder method
void buildReports(Object env) {
    CITools ciTools = new CITools()
    SWFTools swfTools = new SWFTools()

    env['PROJECT_VARIANT_NAME'] = env.PROJECT_NAME + '_' + env.VARIANT_NAME

    static final String CONST_HEALTH_SCALE_FACTOR = '1.0'

    println('[Reporting.groovy][buildReports] building Report data START')
    println('[Reporting.groovy][buildReports] Configure Report data')

    //Configure Reports page
    String reportName = "SWF Reports ${env.BUILD_NUMBER}"
    String reportTitle = ''
    String reportDirectory = './' + env.PROJECT_VARIANT_NAME + '/report'
    String reportFiles = ''

    //Static Resources
    if (swfTools.checkON(env.STATIC_RESOURCES)) {
        if (reportFiles == '') {
            reportFiles = (
                'Resource/' + env.PROJECT_VARIANT_NAME +
                    '_segment_stat.html,' +
                'Resource/' + env.PROJECT_VARIANT_NAME +
                    '_component_stat.html,' +
                'Resource/' + env.PROJECT_VARIANT_NAME +
                    '_Resource_Analysis_Seg_Report.html,' +
                'Resource/' + env.PROJECT_VARIANT_NAME +
                    '_Resource_Analysis_Comp_Report.html'
            )

            reportTitle = 'Res-Seg, Res-Comp, Res-Seg Graph, Res-Comp Graph'
        }
        else {
            reportFiles +=  (
                ',Resource/' + env.PROJECT_VARIANT_NAME +
                    '_segment_stat.html,' +
                'Resource/' + env.PROJECT_VARIANT_NAME +
                    '_component_stat.html,' +
                'Resource/' + env.PROJECT_VARIANT_NAME +
                    '_Resource_Analysis_Seg_Report.html,' +
                'Resource/' + env.PROJECT_VARIANT_NAME +
                    '_Resource_Analysis_Comp_Report.html'
            )

            reportTitle += ',Res-Seg, Res-Comp, Res-Seg Graph, Res-Comp Graph'
        }

        junit([
            allowEmptyResults: true,
            healthScaleFactor: CONST_HEALTH_SCALE_FACTOR,
            testResults: (
                env.PROJECT_VARIANT_NAME + '/report/Resource/' +
                env.PROJECT_VARIANT_NAME + '_junit_testreport.xml')
        ])
    }

    if (swfTools.checkON(env.STD_SW_ANALYSIS)) {
        if (reportFiles == '') {
            reportFiles = 'StdSW/StdSW_report.html'
            reportTitle = 'StdSW'
        }
        else {
            reportFiles += ',StdSW/StdSW_report.html'
            reportTitle += ',StdSW'
        }
    }

    // MISRA/QAC Report
    if (swfTools.checkON(env.QAC_ANALYSIS)) {
        if (env.QAC_VERSION == 'QAC') {
            println(
                '[Reporting.groovy][buildReports] INFO: No reports ' +
                'available with option: QAC')
        }
        else if (env.QAC_VERSION == 'QAC9') {
            if (reportFiles == '') {
                reportFiles = (
                    'QAC/prqaconfig_CRR.html,QAC/prqaconfig_RCR.html,' +
                    'QAC/prqaconfig_SUR.html,QAC/HISMetrics.html')
                reportTitle = 'QAC_CRR,QAC_RCR,QAC_SUR,HISReport'
            }
            else {
                reportFiles += (
                    ',QAC/prqaconfig_CRR.html,QAC/prqaconfig_RCR.html,' +
                    'QAC/prqaconfig_SUR.html,QAC/HISMetrics.html')
                reportTitle += ',QAC_CRR,QAC_RCR,QAC_SUR,HISReport'
            }
        }
    }

    if (swfTools.checkON(env.DOXYGEN)) {
        //unzip doc
        unzip zipFile: env.PROJECT_VARIANT_NAME + '/report/Doxygen/DoxygenDoc.zip',
              dir: env.PROJECT_VARIANT_NAME + '/report/Doxygen'
        //
        if (reportFiles == '') {
            reportFiles = 'Doxygen/html/index.html'
            reportTitle = 'Doxygen'
        }
        else {
            reportFiles += ',Doxygen/html/index.html'
            reportTitle += ',Doxygen'
        }
    }

    // Polyspace Bugfinder Analysis
    if (swfTools.checkON(env.PSBF_ANALYSIS)) {
        if (reportFiles == '') {
            reportFiles = (
                    'Polyspace/BugFinder/polyspace_report.html')
            reportTitle = 'Polyspace Bugfinder Analysis'
        }
        else {
            reportFiles += (
                    ',Polyspace/BugFinder/polyspace_report.html')
            reportTitle += ',Polyspace Bugfinder Analysis'
        }
    }

    //Tessy Report
    if (swfTools.checkON(env.TESSY_ANALYSIS)) {
        if (reportFiles == '') {
            reportFiles = 'Tessy/TESSY_OverviewReport.html'
            reportTitle = 'Tessy'
        }
        else {
            reportFiles += ',Tessy/TESSY_OverviewReport.html'
            reportTitle += ',Tessy'
        }
    }

    if (swfTools.checkON(env.DYNAMIC_RESOURCES)) {
        if (reportFiles == '') {
            reportFiles = (
                'DynResource/' + env.PROJECT_VARIANT_NAME +
                '_Report_TA_Release_html.html')
            reportTitle = 'DynResource'
        }
        else {
            reportFiles += (
                ',DynResource/' + env.PROJECT_VARIANT_NAME +
                '_Report_TA_Release_html.html')
            reportTitle += ',DynResource'
        }

        junit([
            allowEmptyResults: true,
            healthScaleFactor: CONST_HEALTH_SCALE_FACTOR,
            testResults: (
                env.PROJECT_VARIANT_NAME + '/report/DynResource/' +
                env.PROJECT_VARIANT_NAME + '_Report_TA_Release_xml.*')
        ])
    }

    if (reportFiles != '') {
        println('[Reporting.groovy][buildReports] Publish Report data')
        println(
            '[Reporting.groovy][buildReports] available reports: ' +
            reportFiles)

        ciTools.publishHTML(
            reportDirectory, reportFiles, reportName, reportTitle)
    }
    else {
        println(
            '[Reporting.groovy][buildReports] INFO: no Reports available.')
    }

    println('[Reporting.groovy][buildReports] building Report data END')
}

/**
 * Lib module to build Reporting stage for SWFLib
 * <p>
 * Module is adding stage 'Report' to pipeline, getting report data from test
 * stages and building html page 'SWF_Report'
 *
 * @param env The Jenkins build environment. It must contain
 *            the following variables:
 *  <ul>
 *    <li>PROJECT_NAME      - name of the project, e.g.: P12345 </li>
 *    <li>VARIANT_NAME      - name of the variant, e.g.: BCM</li>
 *    <li>STATIC_RESOURCES  - reporting for Static Resource Validation
 *                            activated [ON/OFF]</li>
 *    <li>DYNAMIC_RESOURCES - reporting for Dynamic Resource Validation
 *                            activated [ON/OFF]</li>
 *    <li>STD_SW_ANALYSIS   - reporting for Std-SW Validation activated
 *                            [ON/OFF]</li>
 *    <li>QAC_ANALYSIS      - reporting for MISRA/QAC Validation activated
 *                            [ON/OFF]</li>
 *    <li>TESSY_ANALYSIS    - reporting for Tessy Validation activated
 *                            [ON/OFF]</li>
 *    <li>QAC_VERSION       - used QAC version [QAC/QAC9]</li>
 *    <li>DOXYGEN           - Activation of Doxygen
 *                            [ON/OFF]</li>
 *   <li>RECIPIENTS         - e-mail recipients list </li>
 *  </ul>
 **/
void report(Object env = this.env) {
    Notifier notifier = new Notifier()

    stage('Report') {
        // verify Configuration first
        if (!isReportingConfigurationValid(env)) {
            // Config Error, throw error
            error 'Reporting failed due to an incorrect configuration of the environment variables. ' +
            '-> Check the environment variables in the job configuration.'
        }
        try {
            println('[Reporting.groovy][report] SWFLib Report start.')
            //getting report data
            initReports(env)
            //build report
            buildReports(env)
            //end
            println('[Reporting.groovy][report] SWFLib Report end.')
        } catch (e) {
            //Print error message
            println('[Reporting.groovy][report] report failed.')
            notifier.onFailure(env, e)
        }
    }
}
