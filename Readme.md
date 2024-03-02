# Jenkins library test
## Add it to jenkins:
```
Go to 'Manage Jenkins', 'System', 'Global Pipeline Libraries'
add Library name (pipeline_name_in_jenkins_settings), Default version, Git repo and Credentials (if required)
```
[Additional info](https://www.jenkins.io/doc/book/pipeline/shared-libraries/)
## Test your Jenkins lib
1. Create test job
2. Copy `test_lib.groovy`
3. Replace `pipeline_name_in_jenkins_settings` to real lib name
4. Try to run job

