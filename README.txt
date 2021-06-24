Note: Add path of installation os secugen lib to java.library.path:
a. By add the paths to Path variable in system variables.

Manually install the secugen sdk lib
mvn install:install-file -Dfile=FDxSDKPro.jar -DgroupId=com.secugen -DartifactId=fxsdpro -Dversion=1 -Dpackaging=jar