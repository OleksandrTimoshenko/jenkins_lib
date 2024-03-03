package main

class Utilities implements Serializable {

  def steps

  // Constructor
  Utilities(steps) {this.steps = steps}

  def sayHelloFromLib() {
    steps.echo "Hello from lib!"
  }

  def scaleJenkinsWorkers(String CONTAINER_NAME, String WORKERS_NUMBER) {
    steps.sh "docker service scale $CONTAINER_NAME=$WORKERS_NUMBER"
  }

  def getAdminUsers(){
    return ['admin']
  }

  // TODO: rewrite this func: printMessage(message, color="")
  def printMessage(color, message) {
      def ANSI_RESET = '\u001B[0m'
      def ANSI_YELLOW = '\u001B[33m'
      def ANSI_RED = '\u001B[31m'

      switch (color) {
          case 'WARNING':
              steps.echo "${ANSI_YELLOW}${message}${ANSI_RESET}"
              break
          case 'ERROR':
              steps.echo "${ANSI_RED}${message}${ANSI_RESET}"
              break
          default:
              steps.echo "${ANSI_RESET}${message}${ANSI_RESET}"
              break
      }
  }

  def getPRInfo() {
    def PR_INFO = steps.sh(returnStdout: true, script: """curl \
                -X GET -L \
                -H \"Accept: application/json\" \
                -H \"Authorization: Bearer ${steps.env.BEARER_AUTH}\" \
                https://api.bitbucket.org/2.0/repositories/${steps.env.BB_WORKSPACE}/${steps.env.BB_REPO}/pullrequests/${steps.params.PR_ID}""").trim()
    return PR_INFO
  }

  def getPRState(String PR_INFO) {
      if (PR_INFO.contains('"state": "OPEN"')) {
        printMessage("", "The pull request is open and can be merged...")
      }
      else {
        printMessage('ERROR', "ERROR: The pull request with ID ${steps.params.PR_ID} is closed, does not exist, or you don't have access to it!")
        steps.error("There is an issue with the current pull request...")
        }
  }

  def checkForceMerge() {
    def FORCE_MERGE_APPROVED = false
    if (steps.params.FORCE_MERGE == true) {
        def BUILD_TRIGGER_BY = steps.currentBuild.getBuildCauses()[0].userId
        def WHO_CAN_MERGE_WITHOUT_TESTS = getAdminUsers()
        if (BUILD_TRIGGER_BY in WHO_CAN_MERGE_WITHOUT_TESTS) {
            FORCE_MERGE_APPROVED = true
            printMessage ("", "Force merge activated, triggered by user ${BUILD_TRIGGER_BY}.")
            printMessage('WARNING', "WARNING: Force merge!")
        }
        else {
            printMessage('ERROR', "ERROR: User ${BUILD_TRIGGER_BY} does not have force merge permissions!!!")
            steps.error("Starting a force merge for this user is not allowed.")
          }
      }
    return FORCE_MERGE_APPROVED
  }

  def checkout(String PR_INFO) {
    // Use the 'withCredentials' step to bind SSH credentials
    steps.withCredentials([sshUserPrivateKey(credentialsId: 'BB_SSH_KEY', keyFileVariable: 'SSH_PRIVATE_KEY')]) {
      // Set up Git configuration with the SSH key
      steps.sh """
        rm -rf ~/workspace/Merge_BB_PR/test-for-ci
        rm -rf ~/.ssh
        mkdir -p ~/.ssh
        cp $SSH_PRIVATE_KEY ~/.ssh/id_rsa
        chmod 600 ~/.ssh/id_rsa
        echo 'Host *\n\tStrictHostKeyChecking no\n\n' >> ~/.ssh/config
      """

      // Checkout the Git repository
      def prInfo = readJSON text: PR_INFO
      def sourceBranch = prInfo.source.branch.name
      steps.sh "git clone -b ${sourceBranch} ${steps.env.GIT_REPO}"
    }
  }

  def runTests() {
    def TESTS_RES = steps.sh(script: 'python3 ~/workspace/Merge_BB_PR/test-for-ci/tests.py', returnStdout: true).trim()
    steps.echo "${TESTS_RES}"
    if (TESTS_RES != "Tests passed!") {
      printMessage('ERROR', "Tests failed!")
      steps.error("Tests failed!")
    }
    else {
      TESTS_OK = true
    }
    return TESTS_OK
  }

  def chechApproves(String PR_INFO) {
    def NOT_APPROVED_USERS = []
    def prInfo = readJSON text: PR_INFO
    def APPROVERS = prInfo.reviewers.display_name
    def PARTISIPANTS = prInfo.participants
    if (APPROVERS == []) {
      printMessage('ERROR', "ERROR: It seems like this pull request doesn't have any approver...")
      steps.error("It seems like this pull request doesn't have any approver...")
    }
    else {
      printMessage('', "Found reviewer(s) for this pull request: ${APPROVERS}")
    }
    PARTISIPANTS.each { participant ->
      if (participant.role == 'REVIEWER' && participant.approved == false) {
        NOT_APPROVED_USERS <<  participant.user.display_name
        }
      }
    if (NOT_APPROVED_USERS.size() != 0) {
      printMessage('ERROR', "ERROR: The approver(s) haven't approved the pull request yet: ${NOT_APPROVED_USERS}")
      steps.error("Doesn't have approval from all approvers.")
    }
  }

  def merge() {
    def BUILD_TRIGGER_BY = steps.currentBuild.getBuildCauses()[0].userId
    steps.sh(script: """curl \
                  --request POST \
                  --url 'https://api.bitbucket.org/2.0/repositories/${steps.env.BB_WORKSPACE}/${steps.env.BB_REPO}/pullrequests/${steps.params.PR_ID}/merge' \
                  --header 'Authorization: Bearer ${steps.env.BEARER_AUTH}' \
                  --header 'Accept: application/json' \
                  --header 'Content-Type: application/json' \
                  --data '{
                          "type": "string",
                          "message": "PR was merged via Jenkins pipeline by user ${BUILD_TRIGGER_BY}",
                          "close_source_branch": ${steps.params.CLOSE_SOURCE_BRANCH},
                          "merge_strategy": "${steps.params.MERGE_STRATEGY}"
                  }'""")
  }
}