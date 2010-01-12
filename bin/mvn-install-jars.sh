export VERSION="1.3.0"
export SDK=$1
export LIB="${SDK}/lib"

mvn install:install-file -Dfile="${LIB}/user/appengine-api-1.0-sdk-${VERSION}.jar" -DgroupId=com.google.appengine -DartifactId=appengine-api-1.0-sdk -Dversion=${VERSION} -Dpackaging=jar -DgeneratePom=true
mvn install:install-file -Dfile="${LIB}/impl/appengine-api-stubs.jar" -DgroupId=com.google.appengine -DartifactId=appengine-api-stubs -Dversion=${VERSION} -Dpackaging=jar -DgeneratePom=true

 