# custom-Jenkins-Extension

Installation steps:

1- Add the folder to a git repository

2- From Jenkins configuration, add new shared library and point the settings to git repository

3- Provice the git credintials

4- Load it implicitly

5- In Jenkins configuration setup the following environment variables:

#========== Activate or deactivate built-in steps ==================
ANALYTICS_STAGE=ON
ANALYTICS_ERROR=ON
ANALYTICS_DEBUG=ON
ANALYTICS_SWF=ON
ANALYTICS_ELK_SWF_URL=http://localhost:8686/jenkins-swf/jenkins/
ANALYTICS_ELK_URL=http://localhost:8686/jenkins-metrics/jenkins/
ANALYTICS_API_ERROR_URL=http://localhost:7007/errorinfo
ANALYTICS_API_JOB_URL=http://localhost:7007/jobinfo/
ANALYTICS_API_STAGE_URL =http://localhost:7007/stageinfo/
ANALYTICS_REST_TIMEOUT=250
ANALYTICS_ELK_TIMEOUT=350

6- Ensure the REST API application is running and all end-points are active.

7- Ensure PostgreSQL Database is setup and ready.

8- Ensure Elastic search is setup.

