#!groovy

timestamps {
    node ('docker-1') {
        stage ('VC Checkout') {
                  }
        
        stage ('Generate Groovydoc') {
            
        }

        stage ('Publish Groovydoc') {
            publishHTML([
                allowMissing:          false,
                alwaysLinkToLastBuild: false,
                keepAll:               true,
                reportDir:             'doc',
                reportFiles:           'index.html',
                reportName:            'Documentation',])
        }
        
        stage ('Archive') {
            
        }

        stage ('Static code analysis') {
           
        }

        stage ('Publish HTML report') {
          
        }
    }
}
