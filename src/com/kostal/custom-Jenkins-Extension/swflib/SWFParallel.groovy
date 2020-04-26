package com.kostal.pipelineworks.swflib

import com.kostal.pipelineworks.CommonTools
import org.jenkinsci.plugins.workflow.cps.EnvActionImpl
import java.util.function.Consumer

/**
 * Executes Dynamic Resources Testing and Validation one after another on specified nodes.
**/
void dynamicAnalysis() {
    DynamicResources dynamicResources = new DynamicResources()

    //call swflibDynresources to test Dynamic Resources
    dynamicResources.testing()

    //call swflibDynresources to test Dynamic Resources
    dynamicResources.validation()
}

/**
 * Read the configuration for the build from a file, checkouts from the SCM,
 * builds the project and executes analysis on specified COMPILE_NODE.
**/
void buildAndAnalysis() {
    SWFTools swfTools = new SWFTools()
    SWFCheckout swfCheckout = new SWFCheckout()
    KaRte swfKaRte = new KaRte()
    Qac swfQac = new Qac()
    Build swfBuild = new Build()
    TaskScanner swfTaskScanner = new TaskScanner()
    Doxygen swfDoxygen = new Doxygen()
    PrqaAnnotationCollector swfPrqaAnnotationCollector = new PrqaAnnotationCollector()
    StandardSoftware swfStandardSoftware = new StandardSoftware()
    CompilerWarnings swfCompilerWarnings = new CompilerWarnings()
    LocAnalysis swfLocAnalysis = new LocAnalysis()
    StaticResources swfStaticResources = new StaticResources()
    CommonTools cm = new CommonTools()

    List<Consumer<EnvActionImpl>> buildAnalysis = new ArrayList<Consumer<EnvActionImpl>>(Arrays.asList
        (swfDoxygen.&buildDoxygen, swfTaskScanner.&scanProject, swfLocAnalysis.&callLocAnalysis,
            swfPrqaAnnotationCollector.&buildPrqaAnnotationCollector, swfStandardSoftware.&standardSoftwareReport,
            this.&dynamicAnalysis, swfCompilerWarnings.&compilerWarnings, swfStaticResources.&staticResources))

    node (COMPILE_NODE) {
        //call SWFLibTools SW Library checkout
        swfCheckout.checkoutStage()

        // check, if BUILD is set, default: ON
        env['BUILD'] = swfTools.checkEnvVar('BUILD', 'ON')

        //call KA-RTE build
        swfKaRte.buildKaRteStage()

        //call SW Library build
        swfBuild.build()

        //build MisraQacHis report
        swfQac.buildMisraQacHis()

        //Parallel execution of prebuild analysis
        parallel cm.parallelize(buildAnalysis)
    }
}

/**
 * Generates Tessy report on the specified TESSY_NODE.
 * TESSY_NODE setting inside swfTessy.groovy!
**/
void tessyAnalysis() {
    Tessy swfTessy = new Tessy()
    swfTessy.generateReport(env)
}
