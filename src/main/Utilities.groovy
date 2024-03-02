package main

class Utilities implements Serializable {

  def steps

  // Constructor
  Utilities(steps) {this.steps = steps}

  def sayHelloFromLib() {
    steps.echo "Hello from lib!"
  }
}