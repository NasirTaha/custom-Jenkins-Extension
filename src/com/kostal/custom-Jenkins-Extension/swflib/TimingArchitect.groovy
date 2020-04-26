/*
 * SW-Factory Library module to run BTF Trace Validation with TimingArchitect
 * Toolsuite bat-mode
*/
package com.kostal.pipelineworks.swflib

import com.kostal.pipelineworks.CommandBuilder

/**
 * validatePyMode is validating BTF file with TA 17.4 python script
 * @param taVersion  TA Toolsuite version
 * @param taBatVersion TA Toolsuite bat version
 * @param project     Project Variant Name
 * @param btfFile    BTF Trace file input
 * @param reqFile    Requirement file input
**/
void validatePyMode(
    String taVersion, String taBatVersion, String project,
    String btfFile, String reqFile) {

    // include external libs
    CommandBuilder commandBuilder = new CommandBuilder()
    println(
        '[TimingArchitect.groovy][validatePyMode] Calling TA Toolsuite ' +
        'Validation with Python Script')

    bat commandBuilder.buildBat([
        '@echo on',
        '@set WORKSPACE=%cd%',
        "XCOPY \"${btfFile}\" ^",
        "\"%${taBatVersion}%\\input\\ET_TraceData.btf*\" /Y",
        "XCOPY \"${reqFile}\" ^",
        "\"%${taBatVersion}%\\input\\Requirements.tam*\" /Y"
    ])

    println('[TimingArchitect.groovy][validatePyMode] Copy files done.')

    bat commandBuilder.buildBat([
        '@echo on',
        '@set WORKSPACE=%cd%',
        "%CI_PYTHON27%\\python %${taBatVersion}%\\" +
            'ta_trace_to_junit_report.py ^',

        "--output %WORKSPACE%\\${project}\\data\\DynResource\\" +
            "validation\\output\\${project}_Report_TA_Release_html.html ^",

        "--requirements_file %${taBatVersion}%\\input\\Requirements.tam ^",
        "--trace %${taBatVersion}%\\input\\ET_TraceData.btf ^",
        "--ta_tool_suite_dir %${taVersion}% ^",
        '--report_type html'
    ])

    println('[TimingArchitect.groovy][validatePyMode] html report done.')

    bat commandBuilder.buildBat([
        '@echo on',
        '@set WORKSPACE=%cd%',
        "%CI_PYTHON27%\\python %${taBatVersion}%\\" +
            'ta_trace_to_junit_report.py ^',
        "--output %WORKSPACE%\\${project}\\data\\DynResource\\" +
            "validation\\output\\${project}_Report_TA_Release_xml.xml ^",
        "--requirements_file %${taBatVersion}%\\input\\Requirements.tam ^",
        "--trace %${taBatVersion}%\\input\\ET_TraceData.btf ^",
        "--ta_tool_suite_dir %${taVersion}% ^",
        '--report_type junit_xml'
    ])

    println('[TimingArchitect.groovy][validatePyMode] xml report done.')
    println(
        '[TimingArchitect.groovy][validatePyMode] TA Toolsuite ' +
        'Validation done')
}

/**
 * validateWfxMode is validating BTF file with TA > v18.x wfx files
 * @param taVersion  TA Toolsuite version
 * @param project     Project Variant Name
 * @param btfFile    BTF Trace file input
 * @param reqFile    Requirement file input
 *
**/
void validateWfxMode(
    String taVersion, String project, String btfFile, String reqFile) {
    // include external libs
    CommandBuilder commandBuilder = new CommandBuilder()
    println(
        '[TimingArchitect.groovy][validateWfxMode] Calling TA ' +
        'Toolsuite Validation with wfx mode')

    bat commandBuilder.buildBat([
        '@echo on',
        '@set WORKSPACE=%cd%',
        "XCOPY \"${btfFile}\" ^",
        "\"%${taVersion}%\\workspace\\ta_trace_to_junit_report\\test_files" +
            '\\ET_TraceData.btf*" /Y',

        "XCOPY \"${reqFile}\" ^",
        "\"%${taVersion}%\\workspace\\ta_trace_to_junit_report\\test_files" +
            '\\Requirements.tam*" /Y',

        "@call %${taVersion}%\\CI_workflow_xml.bat",
        "XCOPY \"%${taVersion}%\\workspace\\ta_trace_to_junit_report\\" +
            'report_files\\Report.html" ^',

        "\"%WORKSPACE%\\${project}\\data\\DynResource\\validation\\" +
            "output\\${project}_Report_TA_Release_xml.xml*\" /Y",

        "@call %${taVersion}%\\CI_workflow_html.bat",
        "XCOPY \"%${taVersion}%\\workspace\\ta_trace_to_junit_report\\" +
            'report_files\\Report.html" ^',

        "\"%WORKSPACE%\\${project}\\data\\DynResource\\validation\\" +
            "output\\${project}_Report_TA_Release_html.html*\" /Y"
     ])

    println(
        '[TimingArchitect.groovy][validateWfxMode] TA Toolsuite ' +
        'Validation done')
}

/**
 * generateReport is calling BTF validation with TA Toolsuite bat mode.
 * bat mode is controlled by env var. TA_TOOLSUITE_BAT_VERSION
 *
 * @param env The Jenkins build environment. It must contain
 *            the following variables:
 * <ul>
 *   <li>PROJECT_NAME             - name of the project, e.g.: P12345 </li>
 *   <li>VARIANT_NAME             - name of the variant, e.g.: BCM</li>
 *   <li>TA_TOOLSUITE_BAT_VERSION - TA Toolsuite Bat Version</li>
 *   <li>TA_TOOLSUITE_VERSION     - TA Toolsuite Version</li>
 * </ul>
 *
 * @param btfFile BTF Trace file input
 * @param reqFile Requirement file input
 *
**/
void generateReport(Object env, String btfFile, String reqFile) {
    env['PROJECT_VARIANT_NAME'] = env.PROJECT_NAME + '_' + env.VARIANT_NAME

    println(
        '[TimingArchitect.groovy][generateReport] Calling TA ' +
        'Toolsuite Validation')

    println(
        '[TimingArchitect.groovy][generateReport] TA: ' +
        env.TA_TOOLSUITE_BAT_VERSION)

    println('[TimingArchitect.groovy][generateReport] BTF file: ' + btfFile)
    println('[TimingArchitect.groovy][generateReport] Req file: ' + reqFile)

    switch (env.TA_TOOLSUITE_BAT_VERSION) {
        case 'TA_TOOLSUITE_BAT_17_4':
            validatePyMode(
                env.TA_TOOLSUITE_VERSION, env.TA_TOOLSUITE_BAT_VERSION,
                env.PROJECT_VARIANT_NAME, btfFile, reqFile)
            break
        case 'TA_TOOLSUITE_BAT_18_3':
            validateWfxMode(
                env.TA_TOOLSUITE_BAT_VERSION, env.PROJECT_VARIANT_NAME,
                btfFile, reqFile)
            break
        case 'TA_TOOLSUITE_BAT_18_4':
            validateWfxMode(
                env.TA_TOOLSUITE_BAT_VERSION, env.PROJECT_VARIANT_NAME,
                btfFile, reqFile)
            break
        default:
            println(
                '[TimingArchitect.groovy][generateReport] WARNING: ' +
                'unsupported TA Version > ' + env.TA_TOOLSUITE_BAT_VERSION)
            break
    }

    println(
        '[TimingArchitect.groovy][generateReport] TA Toolsuite ' +
        'Validation done')
}
