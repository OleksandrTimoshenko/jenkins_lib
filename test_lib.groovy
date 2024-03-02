@Library('pipeline_name_in_jenkins_settings@master') import main.Utilities
lib = new Utilities(this)

pipeline {
    stages {
        stage("Test Lib") {
            steps {
                script {
                    lib.sayHelloFromLib()
                }
            }
        }
    }
}