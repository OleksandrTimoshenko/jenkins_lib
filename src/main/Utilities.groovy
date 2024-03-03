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

}